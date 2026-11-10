package com.demo.ratelimit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate redis;

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void check(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = rateLimit.key().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : rateLimit.key();

        RateLimiter limiter = limiters.computeIfAbsent(key, k -> createLimiter(k, rateLimit));

        if (!limiter.tryAcquire()) {
            throw new RateLimitException("too many requests, key=" + key);
        }
    }

    private RateLimiter createLimiter(String key, RateLimit anno) {
        String algo = anno.algorithm();
        if ("redis".equals(algo)) {
            return new RedisSlidingWindowLimiter(redis, key, anno.limit(), 1000);
        }
        if ("fixed".equals(algo)) {
            return new FixedWindowLimiter(anno.limit(), 1000);
        }
        if ("token".equals(algo)) {
            return new TokenBucketLimiter(anno.limit(), anno.limit());
        }
        if ("leaky".equals(algo)) {
            return new LeakyBucketLimiter(anno.limit(), anno.limit());
        }
        return new SlidingWindowLimiter(anno.limit(), 1000);
    }
}