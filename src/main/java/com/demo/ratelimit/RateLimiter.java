package com.demo.ratelimit;

public interface RateLimiter {
    boolean tryAcquire();
}