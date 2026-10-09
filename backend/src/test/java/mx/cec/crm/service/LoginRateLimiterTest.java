package mx.cec.crm.service;

import mx.cec.crm.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-08T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test
    void blocksAfterLimitAndReportsRemainingWindow() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofMinutes(15), 10, clock);
        limiter.acquire("a"); limiter.acquire("a");
        clock.now = clock.now.plusSeconds(10);
        RateLimitExceededException error = assertThrows(RateLimitExceededException.class, () -> limiter.acquire("a"));
        assertEquals(890, error.getRetryAfterSeconds());
        limiter.acquire("b"); // Separate users have separate windows.
    }

    @Test
    void resetsAtExactExpirationAndReclaimsOtherExpiredBuckets() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofSeconds(60), 1, clock);
        limiter.acquire("a");
        assertThrows(RateLimitExceededException.class, () -> limiter.acquire("b"));
        clock.now = clock.now.plusSeconds(60);
        assertDoesNotThrow(() -> limiter.acquire("b"));
    }

    @Test
    void successfulLoginResetsItsBucket() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMinutes(15), 10, new MutableClock());
        limiter.acquire("a"); limiter.reset("a");
        assertDoesNotThrow(() -> limiter.acquire("a"));
    }

    @Test
    void concurrentReservationsNeverExceedTheLimit() throws Exception {
        LoginRateLimiter limiter = new LoginRateLimiter(5, Duration.ofMinutes(15), 10, new MutableClock());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        AtomicInteger accepted = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 40; i++) futures.add(executor.submit(() -> {
                try { limiter.acquire("a"); accepted.incrementAndGet(); }
                catch (RateLimitExceededException ignored) { }
            }));
            for (Future<?> future : futures) future.get(5, TimeUnit.SECONDS);
            assertEquals(5, accepted.get());
        } finally { executor.shutdownNow(); }
    }
}
