package mx.cec.crm.service;

import mx.cec.crm.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Fixed window per email, bounded in memory; intended for a single instance. */
@Component
public class LoginRateLimiter {
    private final int maxAttempts;
    private final int maxBuckets;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private Instant nextCleanup = Instant.MIN;

    @Autowired
    public LoginRateLimiter(@Value("${app.rate-limit.login-max-attempts}") int maxAttempts,
                            @Value("${app.rate-limit.login-window-minutes}") int windowMinutes,
                            @Value("${app.rate-limit.login-max-buckets:10000}") int maxBuckets) {
        this(maxAttempts, Duration.ofMinutes(windowMinutes), maxBuckets, Clock.systemUTC());
    }

    LoginRateLimiter(int maxAttempts, Duration window, int maxBuckets, Clock clock) {
        if (maxAttempts < 1 || maxBuckets < 1 || window.isZero() || window.isNegative())
            throw new IllegalArgumentException("Configuración de rate limit inválida.");
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.maxBuckets = maxBuckets;
        this.clock = clock;
    }

    // Reserve before authentication so concurrent attempts cannot exceed the limit.
    public synchronized void acquire(String key) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup) || buckets.size() >= maxBuckets) {
            buckets.values().removeIf(bucket -> !now.isBefore(bucket.expiresAt()));
            nextCleanup = now.plusSeconds(60);
        }
        Bucket bucket = buckets.get(key);
        if (bucket != null && !now.isBefore(bucket.expiresAt())) {
            buckets.remove(key);
            bucket = null;
        }
        if (bucket == null) {
            if (buckets.size() >= maxBuckets) {
                Instant earliest = buckets.values().stream().map(Bucket::expiresAt)
                        .min(Instant::compareTo).orElse(now.plus(window));
                throw new RateLimitExceededException(retryAfter(now, earliest));
            }
            buckets.put(key, new Bucket(1, now.plus(window)));
        } else {
            if (bucket.attempts() >= maxAttempts)
                throw new RateLimitExceededException(retryAfter(now, bucket.expiresAt()));
            buckets.put(key, new Bucket(bucket.attempts() + 1, bucket.expiresAt()));
        }
    }

    public synchronized void reset(String key) { buckets.remove(key); }

    private long retryAfter(Instant now, Instant expiry) {
        return Math.max(1, (Duration.between(now, expiry).toMillis() + 999) / 1000);
    }

    private record Bucket(int attempts, Instant expiresAt) {}
}
