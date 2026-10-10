package mx.cec.crm.service;

import mx.cec.crm.config.PasswordResetSettings;
import mx.cec.crm.dto.ResetPasswordRequest;
import mx.cec.crm.entity.PasswordResetToken;
import mx.cec.crm.entity.User;
import mx.cec.crm.repository.PasswordResetTokenRepository;
import mx.cec.crm.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final PasswordResetSettings settings;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder, JavaMailSender mail,
                                PasswordResetSettings settings, Clock clock) {
        this.users = users; this.tokens = tokens; this.encoder = encoder;
        this.mail = mail; this.settings = settings; this.clock = clock;
        URI url = URI.create(settings.resetUrl());
        boolean localHttp = "http".equals(url.getScheme()) &&
                ("localhost".equals(url.getHost()) || "127.0.0.1".equals(url.getHost()));
        if (url.getHost() == null || url.getRawQuery() != null || url.getRawFragment() != null ||
                url.getUserInfo() != null || !("https".equals(url.getScheme()) || localHttp))
            throw new IllegalArgumentException("PASSWORD_RESET_URL debe usar HTTPS (HTTP solo en localhost), sin query ni fragmento.");
    }
    /** Lookup/delivery happens after the same response for registered and unknown emails. */
    @Async("passwordResetExecutor")
    @Transactional
    public void sendRecoveryLink(String normalizedEmail) {
        users.findActiveByEmailForUpdate(normalizedEmail).ifPresent(user -> {
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokens.saveAndFlush(PasswordResetToken.builder().userId(user.getId()).user(user)
                    .tokenHash(hash(token)).expiresAt(clock.instant().plus(Duration.ofMinutes(settings.ttlMinutes()))).build());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(settings.from()); message.setTo(user.getEmail());
            message.setSubject("CRM CEC — Recuperar contraseña");
            // Fragments stay out of HTTP access logs; frontend submits token only in POST body.
            message.setText("Solicitaste cambiar tu contraseña en CRM CEC.\n\n" + settings.resetUrl() + "#token=" + token +
                    "\n\nEl enlace vence en " + settings.ttlMinutes() + " minutos y solo se puede usar una vez. " +
                    "Si no hiciste esta solicitud, ignora este correo.");
            // Failed delivery rolls back rotation so the previous link remains usable.
            mail.send(message);
        });
    }
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe tener entre 8 caracteres y 72 bytes UTF-8.");
        String digest = hash(request.token());
        Long userId = tokens.findUserIdByTokenHash(digest).orElseThrow(PasswordResetService::invalidLink);
        // Issuing and consuming lock the same user first to serialize concurrent requests.
        User user = users.findByIdForUpdate(userId).filter(User::isActive).orElseThrow(PasswordResetService::invalidLink);
        PasswordResetToken stored = tokens.findByUserIdForUpdate(userId).orElseThrow(PasswordResetService::invalidLink);
        if (!stored.getTokenHash().equals(digest) || !clock.instant().isBefore(stored.getExpiresAt())) throw invalidLink();
        user.setPassword(encoder.encode(request.password()));
        users.save(user); tokens.delete(stored);
    }
    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 no disponible", e); }
    }
    private static IllegalArgumentException invalidLink() {
        return new IllegalArgumentException("El enlace es inválido o ha vencido. Solicita uno nuevo.");
    }
}

