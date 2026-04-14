package codes.anmol.distributedratelimiter.config;

import codes.anmol.distributedratelimiter.service.RateLimiter;
import codes.anmol.distributedratelimiter.service.impl.FixedWindowRateLimiter;
import codes.anmol.distributedratelimiter.service.impl.SlidingWindowRateLimiter;
import codes.anmol.distributedratelimiter.service.impl.TokenBuckerRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimiterConfig {

    @Bean("fixedWindowRateLimiter")
    public RateLimiter fixedWindowRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        return new FixedWindowRateLimiter(redisTemplate);
    }

    @Bean("tokenBucketRateLimiter")
    public RateLimiter tokenBucketRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        return new TokenBuckerRateLimiter(redisTemplate);
    }

    @Bean("slidingWindowRateLimiter")
    public RateLimiter slidingWindowRateLimiter(StringRedisTemplate redisTemplate) {
        return new SlidingWindowRateLimiter(redisTemplate);
    }
}
