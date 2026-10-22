package com.demo.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowLimiter implements RateLimiter {

    private final int limit;
    private final long windowMs;
    private volatile long windowStart;
    private final AtomicInteger count;

    public FixedWindowLimiter(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
        this.windowStart = System.currentTimeMillis();
        this.count = new AtomicInteger(0);
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        if (now - windowStart >= windowMs) {
            windowStart = now;
            count.set(0);
        }
        if (count.get() < limit) {
            count.incrementAndGet();
            return true;
        }
        return false;
    }
}