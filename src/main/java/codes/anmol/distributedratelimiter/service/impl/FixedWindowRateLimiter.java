package codes.anmol.distributedratelimiter.component;

import codes.anmol.distributedratelimiter.service.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FixedWindowRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Long> redisTemplate;

    public FixedWindowRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        long currTime = System.currentTimeMillis()/1000;
        long window = currTime/windowSeconds;

        String redisKey = "rate:fixedWindow" + key + ":" + window;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        Long ttl = redisTemplate.getExpire(redisKey);
        System.out.println("TTL set: "+ttl);
        return count != null && count <= limit;
    }

}
