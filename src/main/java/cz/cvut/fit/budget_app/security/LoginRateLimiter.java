package cz.cvut.fit.budget_app.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, AttemptBucket> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptBucket bucket = attempts.get(key);
        if (bucket == null) {
            return false;
        }
        if (Instant.now().isAfter(bucket.windowStart.plus(WINDOW))) {
            attempts.remove(key);
            return false;
        }
        return bucket.count >= MAX_ATTEMPTS;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.windowStart.plus(WINDOW))) {
                return new AttemptBucket(1, now);
            }
            existing.count++;
            return existing;
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private static class AttemptBucket {
        private int count;
        private final Instant windowStart;

        private AttemptBucket(int count, Instant windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
