package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.service.impl.TokenBuckerRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TokenBucketRateLimiterTest {

    @Autowired
    private RedisTemplate<String, Long> redisTemplate;

    private TokenBuckerRateLimiter rateLimiter;

    private final String KEY_PREFIX = "test:token:" + System.currentTimeMillis() + ":";

    @BeforeEach
    void setUp() {
        rateLimiter = new TokenBuckerRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("First request should be allowed")
    void firstRequestAllowed() {
        boolean res = rateLimiter.isAllowed(KEY_PREFIX + "u1", 10, 60);
        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("Burst of requests upto limit should be allowed")
    void burstUpToLimitAllowed() {
        String key = KEY_PREFIX + "u2";
        int limit = 5;
        for (int i=0;i<limit;i++) {
            assertThat(rateLimiter.isAllowed(key, limit, 60))
                    .as("Request %d should be allowed", i+1)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Request beyond capacity should be denied")
    void requestBeyondCapacityToBeDenied() {
        String key = KEY_PREFIX + "u3";
        int limit = 2;
        for (int i=0;i<limit;i++) {
            rateLimiter.isAllowed(key, limit, 60);
        }
        assertThat(rateLimiter.isAllowed(key, limit, 60)).isFalse();
    }

    @Test
    @DisplayName("Remaining token decrease as requests are made")
    void remainingTokenShouldDecrease() {
        String key = KEY_PREFIX + "u4";
        int limit = 5;
        rateLimiter.isAllowed(key, limit, 60);
        long remain = rateLimiter.getRemaining(key, limit, 60);
        assertThat(remain).isLessThan(limit);
    }

    @Test
    @DisplayName("Different keys are independent")
    void differentKeysAreIndependent() {
        String k1 = KEY_PREFIX + "x1";
        String k2 = KEY_PREFIX + "x2";
        int limit = 2;
        for (int i=0;i<limit;i++) {
            rateLimiter.isAllowed(k1, limit, 60);
        }
        boolean k1Denied = !rateLimiter.isAllowed(k1, limit, 60);
        boolean k2Allowed = rateLimiter.isAllowed(k2, limit, 60);
        assertThat(k1Denied).isTrue();
        assertThat(k2Allowed).isTrue();
    }

    @Test
    @DisplayName("Token refilling occurs")
    void tokenRefillingOccurs() throws InterruptedException {
        String key = KEY_PREFIX + "refill";
        int limit = 1;
        int windowSeconds = 2;
        rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds)).isFalse();

        Thread.sleep(2000);

        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds)).isTrue();
    }
}
