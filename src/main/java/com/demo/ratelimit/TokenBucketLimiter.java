package com.demo.ratelimit;

public class TokenBucketLimiter implements RateLimiter {

    private final int capacity;
    private final double tokensPerMs;
    private double tokens;
    private long lastRefillMs;

    public TokenBucketLimiter(int capacity, int permitsPerSecond) {
        this.capacity = capacity;
        this.tokensPerMs = permitsPerSecond / 1000.0;
        this.tokens = capacity;
        this.lastRefillMs = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        tokens = Math.min(capacity, tokens + (now - lastRefillMs) * tokensPerMs);
        lastRefillMs = now;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }
}