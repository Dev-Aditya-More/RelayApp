package com.relay.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counters kept in memory. Fine for a single instance; with several instances
 * each keeps its own counts, so move this to Redis before scaling out.
 */
@Component
public class RateLimiter {
    private record Entry(long start, long windowMs, int count) {
        boolean expired(long now) { return now >= start + windowMs; }
        long retryAfterSeconds(long now) { return Math.max(1, (start + windowMs - now + 999) / 1000); }
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    /** Counts one attempt. Returns 0 if it is allowed, otherwise the seconds until the window resets. */
    public long hit(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        if ((calls.incrementAndGet() & 1023) == 0) entries.values().removeIf(e -> e.expired(now));
        Entry e = entries.compute(key, (k, old) -> old == null || old.expired(now)
                ? new Entry(now, window.toMillis(), 1)
                : new Entry(old.start(), old.windowMs(), old.count() + 1));
        return e.count() > max ? e.retryAfterSeconds(now) : 0;
    }

    /** Without counting: seconds until the key may try again if it has already used up {@code max} attempts, else 0. */
    public long blockedFor(String key, int max) {
        long now = System.currentTimeMillis();
        Entry e = entries.get(key);
        return e != null && !e.expired(now) && e.count() >= max ? e.retryAfterSeconds(now) : 0;
    }

    public void reset(String key) { entries.remove(key); }

    public static String humanWait(long seconds) {
        long minutes = (seconds + 59) / 60;
        return minutes <= 1 ? "a minute" : minutes + " minutes";
    }
}
