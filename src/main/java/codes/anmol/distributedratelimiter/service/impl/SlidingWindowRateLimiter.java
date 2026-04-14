package codes.anmol.distributedratelimiter.service.impl;

import codes.anmol.distributedratelimiter.service.RateLimiter;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class SlidingWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public SlidingWindowRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        String redisKey = buildKey(key);
        long now = System.currentTimeMillis();
        long windowStartMs = now - (windowSeconds * 1000L);

        redisTemplate.opsForZSet().removeRangeByScore(
                redisKey,
                0,
                (double) windowStartMs
        );

        Long currCount = redisTemplate.opsForZSet().zCard(redisKey);
        if (currCount == null) currCount = 0L;

        if (currCount < limit) {
            String member = now + "-" + System.nanoTime();
            redisTemplate.opsForZSet().add(
                    redisKey,
                    member,
                    (double) now
            );
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds * 2L));
            return true;
        }
        return false;
    }

    @Override
    public long getRemaining(String key, int limit, int windowSeconds) {
        String redisKey = buildKey(key);
        long windowStartMs = System.currentTimeMillis() - (windowSeconds * 1000L);

        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, (double) windowStartMs);

        Long currCount = redisTemplate.opsForZSet().zCard(redisKey);
        if (currCount == null) return limit;

        return Math.max(0, limit - currCount);
    }

    private String buildKey(String key) {
        return "rate:sliding:" + key;
    }
}
