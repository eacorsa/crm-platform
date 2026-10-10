package mx.cec.crm.service;
import mx.cec.crm.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordResetRateLimiterTest {
    @Test void emailLimitAppliesAcrossDifferentIps() {
        var limiter = new PasswordResetRateLimiter();
        for (int i = 0; i < 3; i++) limiter.acquireRequest("same@example.com", "ip-" + i);
        assertThrows(RateLimitExceededException.class, () -> limiter.acquireRequest("same@example.com", "another-ip"));
    }
    @Test void ipLimitAppliesAcrossDifferentEmailsAndResetHasSeparateBudget() {
        var limiter = new PasswordResetRateLimiter();
        for (int i = 0; i < 10; i++) limiter.acquireRequest("person" + i + "@example.com", "same-ip");
        assertThrows(RateLimitExceededException.class, () -> limiter.acquireRequest("another@example.com", "same-ip"));
        for (int i = 0; i < 10; i++) limiter.acquireReset("same-ip");
        assertThrows(RateLimitExceededException.class, () -> limiter.acquireReset("same-ip"));
    }
}

