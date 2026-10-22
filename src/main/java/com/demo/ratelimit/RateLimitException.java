package com.demo.ratelimit;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}