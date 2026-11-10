package com.demo.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;

public class RedisSlidingWindowLimiter implements RateLimiter {

    private static final String LUA =
        "local key = KEYS[1] " +
        "local now = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local limit = tonumber(ARGV[3]) " +
        "local member = ARGV[4] " +
        "redis.call('ZREMRANGEBYSCORE', key, 0, now - window) " +
        "local count = redis.call('ZCARD', key) " +
        "if count < limit then " +
        "  redis.call('ZADD', key, now, member) " +
        "  redis.call('PEXPIRE', key, window) " +
        "  return 1 " +
        "end " +
        "return 0";

    private static final RedisScript<Long> SCRIPT = RedisScript.of(LUA, Long.class);

    private final StringRedisTemplate redis;
    private final String key;
    private final int limit;
    private final long windowMs;

    public RedisSlidingWindowLimiter(StringRedisTemplate redis, String key, int limit, long windowMs) {
        this.redis = redis;
        this.key = "ratelimit:" + key;
        this.limit = limit;
        this.windowMs = windowMs;
    }

    @Override
    public boolean tryAcquire() {
        try {
            Long result = redis.execute(
                SCRIPT,
                Collections.singletonList(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowMs),
                String.valueOf(limit),
                UUID.randomUUID().toString()
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            return true;
        }
    }
}