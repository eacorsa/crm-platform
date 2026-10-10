package mx.cec.crm.service;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;

@Component
public class PasswordResetRateLimiter {
    private final LoginRateLimiter emails = new LoginRateLimiter(3, Duration.ofMinutes(15), 10000, Clock.systemUTC());
    private final LoginRateLimiter requestIps = new LoginRateLimiter(10, Duration.ofMinutes(15), 10000, Clock.systemUTC());
    private final LoginRateLimiter resetIps = new LoginRateLimiter(10, Duration.ofMinutes(15), 10000, Clock.systemUTC());
    public void acquireRequest(String email, String ip) {
        requestIps.acquire(ip);
        emails.acquire(email);
    }
    public void acquireReset(String ip) { resetIps.acquire(ip); }
}

