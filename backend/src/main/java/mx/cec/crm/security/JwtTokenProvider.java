package mx.cec.crm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, String encodedPassword) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("credentials", credentialsDigest(encodedPassword))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT inválido o expirado.");
            return false;
        }
    }

    public boolean matchesCredentials(String token, String encodedPassword) {
        String digest = parseClaims(token).get("credentials", String.class);
        return digest != null && MessageDigest.isEqual(
                digest.getBytes(StandardCharsets.UTF_8), credentialsDigest(encodedPassword).getBytes(StandardCharsets.UTF_8));
    }

    private String credentialsDigest(String encodedPassword) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(encodedPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("HMAC no disponible", e); }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
