package mx.cec.crm.controller;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import mx.cec.crm.entity.User;
import mx.cec.crm.repository.UserRepository;
import mx.cec.crm.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP + async transaction + JavaMail transport, with no external delivery or MySQL writes. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:mail-flow;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-secret-with-at-least-thirty-two-bytes", "app.password-reset.enabled=true",
        "app.password-reset.from=noreply@example.com", "app.password-reset.reset-url=http://127.0.0.1:5173/reset-password",
        "spring.mail.host=127.0.0.1", "spring.mail.username=", "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false", "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false"
})
class PasswordResetMailFlowTest {
    private static final SmtpCapture smtp = new SmtpCapture();
    @DynamicPropertySource
    static void mailPort(DynamicPropertyRegistry registry) { registry.add("spring.mail.port", smtp::port); }
    @AfterAll static void closeSmtp() throws IOException { smtp.close(); }
    @Autowired TestRestTemplate http;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;

    @Test
    void receiveEmailResetLoginAndRevokeOldSession() throws Exception {
        http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1).build()));
        User user = users.save(User.builder().name("Mail test").email("mail-test@example.com")
                .password(encoder.encode("old-password")).build());
        try {
            ResponseEntity<Map> initialLogin = http.postForEntity("/auth/login",
                    Map.of("email", user.getEmail(), "password", "old-password"), Map.class);
            assertEquals(HttpStatus.OK, initialLogin.getStatusCode());
            String oldJwt = (String) Objects.requireNonNull(initialLogin.getBody()).get("token");
            ResponseEntity<Map> request = http.postForEntity("/auth/forgot-password", Map.of("email", user.getEmail()), Map.class);
            assertEquals(HttpStatus.ACCEPTED, request.getStatusCode());
            String raw = smtp.messages.poll(10, TimeUnit.SECONDS);
            assertNotNull(raw, "SMTP did not receive the recovery message");
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
            assertEquals(user.getEmail(), message.getAllRecipients()[0].toString());
            String token = ((String) message.getContent()).split("#token=")[1].split("\\s")[0];
            assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
            // Receipt can precede transaction commit by a few milliseconds.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!tokens.existsById(user.getId()) && System.nanoTime() < deadline) Thread.sleep(20);
            ResponseEntity<Map> changed = http.postForEntity("/auth/reset-password",
                    Map.of("token", token, "password", "new-password"), Map.class);
            assertEquals(HttpStatus.OK, changed.getStatusCode());
            assertEquals(HttpStatus.BAD_REQUEST, http.postForEntity("/auth/reset-password",
                    Map.of("token", token, "password", "another-password"), Map.class).getStatusCode());
            HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(oldJwt);
            assertEquals(HttpStatus.UNAUTHORIZED, http.exchange("/tasks", HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getStatusCode());
            assertEquals(HttpStatus.UNAUTHORIZED, http.postForEntity("/auth/login",
                    Map.of("email", user.getEmail(), "password", "old-password"), Map.class).getStatusCode());
            assertEquals(HttpStatus.OK, http.postForEntity("/auth/login",
                    Map.of("email", user.getEmail(), "password", "new-password"), Map.class).getStatusCode());
        } finally { tokens.deleteAll(); users.deleteById(user.getId()); }
    }

    private static final class SmtpCapture implements Closeable {
        private final ServerSocket server;
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        SmtpCapture() {
            try { server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress()); }
            catch (IOException e) { throw new UncheckedIOException(e); }
            Thread thread = new Thread(this::serve, "test-smtp"); thread.setDaemon(true); thread.start();
        }
        int port() { return server.getLocalPort(); }
        private void serve() {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);
                    reply(output, "220 localhost test SMTP");
                    String command;
                    while ((command = input.readLine()) != null) {
                        if (command.startsWith("DATA")) {
                            reply(output, "354 Send data");
                            StringBuilder data = new StringBuilder(); String line;
                            while ((line = input.readLine()) != null && !line.equals("."))
                                data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                            messages.add(data.toString()); reply(output, "250 Accepted");
                        } else if (command.startsWith("QUIT")) { reply(output, "221 Bye"); break; }
                        else { reply(output, "250 OK"); }
                    }
                } catch (IOException e) { if (!server.isClosed()) messages.add("SMTP capture failed"); }
            }
        }
        private void reply(PrintWriter output, String line) { output.print(line + "\r\n"); output.flush(); }
        @Override public void close() throws IOException { server.close(); }
    }
}
