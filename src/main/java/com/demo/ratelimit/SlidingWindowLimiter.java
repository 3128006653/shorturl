package com.demo.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowLimiter implements RateLimiter {

    private final int limit;
    private final long windowMs;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public SlidingWindowLimiter(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
            timestamps.pollFirst();
        }
        if (timestamps.size() < limit) {
            timestamps.addLast(now);
            return true;
        }
        return false;
    }
}