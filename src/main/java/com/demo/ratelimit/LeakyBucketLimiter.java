package com.demo.ratelimit;

public class LeakyBucketLimiter implements RateLimiter {

    private final int capacity;
    private final double leakPerMs;
    private double water;
    private long lastLeakMs;

    public LeakyBucketLimiter(int capacity, int permitsPerSecond) {
        this.capacity = capacity;
        this.leakPerMs = permitsPerSecond / 1000.0;
        this.water = 0;
        this.lastLeakMs = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        water = Math.max(0, water - (now - lastLeakMs) * leakPerMs);
        lastLeakMs = now;
        if (water + 1.0 <= capacity) {
            water += 1.0;
            return true;
        }
        return false;
    }
}