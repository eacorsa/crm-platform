package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.cec.crm.dto.*;
import mx.cec.crm.entity.User;
import mx.cec.crm.repository.UserRepository;
import mx.cec.crm.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final JwtTokenProvider   tokenProvider;
    private final PasswordEncoder    passwordEncoder;

    @Value("${app.rate-limit.login-max-attempts}") private int maxAttempts;
    @Value("${app.rate-limit.login-window-minutes}") private int windowMinutes;

    // Rate limiting en memoria (suficiente para instancia única)
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public AuthResponse login(LoginRequest request) {
        String key = request.email().toLowerCase();
        checkRateLimit(key);

        User user = userRepository.findByEmailAndActiveTrue(key)
                .orElseThrow(() -> {
                    record(key);
                    return new BadCredentialsException("Credenciales incorrectas.");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            record(key);
            throw new BadCredentialsException("Credenciales incorrectas.");
        }

        buckets.remove(key); // reset en login exitoso
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    private void checkRateLimit(String key) {
        RateBucket b = buckets.get(key);
        if (b == null) return;
        if (b.isExpired(windowMinutes)) { buckets.remove(key); return; }
        if (b.attempts >= maxAttempts)
            throw new IllegalArgumentException(
                "Demasiados intentos. Espera " + windowMinutes + " minutos.");
    }

    private void record(String key) {
        buckets.merge(key, new RateBucket(), (existing, ignored) -> {
            existing.attempts++;
            return existing;
        });
    }

    private static class RateBucket {
        int attempts = 1;
        final Instant firstAttempt = Instant.now();
        boolean isExpired(int windowMinutes) {
            return Instant.now().isAfter(firstAttempt.plusSeconds(windowMinutes * 60L));
        }
    }
}
