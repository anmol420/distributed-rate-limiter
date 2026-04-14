package codes.anmol.distributedratelimiter.service.impl;

import codes.anmol.distributedratelimiter.service.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;

public class FixedWindowRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Long> redisTemplate;

    public FixedWindowRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        String redisKey = generateKey(key, windowSeconds);
        Long currCount = redisTemplate.opsForValue().increment(redisKey);
        if (currCount == null) {
            return false;
        }
        if (currCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        return currCount <= limit;
    }

    @Override
    public long getRemaining(String key, int limit, int windowSeconds) {
        String redisKey = generateKey(key, windowSeconds);
        Long currCount = redisTemplate.opsForValue().get(redisKey);
        if (currCount == null) {
            return limit;
        }
        long remaining = limit - currCount;
        return Math.max(0, remaining);
    }

    private String generateKey(String key, int window) {
        long bucket = Instant.now().getEpochSecond() / window;
        return "rate:fixed" + key + ":" + bucket;
    }

}
