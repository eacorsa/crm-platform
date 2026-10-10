package mx.cec.crm.service;

import mx.cec.crm.config.PasswordResetSettings;
import mx.cec.crm.dto.ResetPasswordRequest;
import mx.cec.crm.entity.User;
import mx.cec.crm.repository.PasswordResetTokenRepository;
import mx.cec.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-reset;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PasswordResetService.class, PasswordResetIntegrationTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PasswordResetIntegrationTest {
    @TestConfiguration
    static class Config {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC); }
        @Bean PasswordResetSettings settings() { return new PasswordResetSettings(true, "noreply@example.com", "http://127.0.0.1:5173/reset-password", 30); }
        @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }
    }
    @Autowired PasswordResetService service;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired Clock clock;
    @MockBean JavaMailSender mail;
    private User user;
    @BeforeEach
    void setup() {
        tokens.deleteAll(); users.deleteAll();
        user = users.save(User.builder().name("Test").email("person@example.com").password(encoder.encode("old-password")).build());
    }
    private String issue() {
        reset(mail);
        service.sendRecoveryLink(user.getEmail());
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertArrayEquals(new String[]{user.getEmail()}, message.getValue().getTo());
        return message.getValue().getText().split("#token=")[1].split("\\n")[0];
    }
    @Test
    void deliversOnlyDigestPersistsAndTokenCanBeUsedOnce() {
        String token = issue();
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        var stored = tokens.findById(user.getId()).orElseThrow();
        assertEquals(PasswordResetService.hash(token), stored.getTokenHash());
        assertEquals(clock.instant().plusSeconds(1800), stored.getExpiresAt());
        service.resetPassword(new ResetPasswordRequest(token, "new-password"));
        assertTrue(encoder.matches("new-password", users.findById(user.getId()).orElseThrow().getPassword()));
        assertEquals(0, tokens.count());
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(token, "another-password")));
    }
    @Test
    void newRequestReplacesPreviousLink() {
        String old = issue(), fresh = issue();
        assertNotEquals(old, fresh); assertEquals(1, tokens.count());
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(old, "new-password")));
        service.resetPassword(new ResetPasswordRequest(fresh, "new-password"));
    }
    @Test
    void smtpFailureRollsBackRotationAndPreservesPreviousLink() {
        String old = issue();
        doThrow(new MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        assertThrows(MailSendException.class, () -> service.sendRecoveryLink(user.getEmail()));
        assertEquals(PasswordResetService.hash(old), tokens.findById(user.getId()).orElseThrow().getTokenHash());
        service.resetPassword(new ResetPasswordRequest(old, "new-password"));
    }
    @Test
    void expiryBoundaryRejectsWithoutChangingPassword() {
        String token = issue();
        var stored = tokens.findById(user.getId()).orElseThrow(); stored.setExpiresAt(clock.instant()); tokens.save(stored);
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(token, "new-password")));
        assertTrue(encoder.matches("old-password", users.findById(user.getId()).orElseThrow().getPassword()));
    }
    @Test
    void inactiveAndUnknownEmailsDoNotSendAndInactiveCannotReset() {
        service.sendRecoveryLink("unknown@example.com"); verifyNoInteractions(mail);
        String token = issue(); user.setActive(false); users.save(user); reset(mail);
        service.sendRecoveryLink(user.getEmail()); verifyNoInteractions(mail);
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(token, "new-password")));
    }
    @Test
    void unicodePasswordCannotExceedBcryptByteLimit() {
        String token = issue();
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(token, "é".repeat(37))));
        assertEquals(1, tokens.count());
    }
    @Test
    void simultaneousConsumersOnlyChangePasswordOnce() throws Exception {
        String token = issue();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> consume = () -> {
            start.await();
            try { service.resetPassword(new ResetPasswordRequest(token, "new-password")); return true; }
            catch (IllegalArgumentException e) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(consume), second = executor.submit(consume); start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(0, tokens.count());
        } finally { executor.shutdownNow(); }
    }
}

