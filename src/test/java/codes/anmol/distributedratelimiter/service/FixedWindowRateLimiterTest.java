package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.service.impl.FixedWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FixedWindowRateLimiterTest {

    @Autowired
    private RedisTemplate<String, Long> redisTemplate;

    private FixedWindowRateLimiter rateLimiter;

    private final String TEST_KEY_PREFIX = "test:fixed" + System.currentTimeMillis() + ":";

    @BeforeEach
    void setUp() {
        rateLimiter = new FixedWindowRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("First request should be allowed")
    void firstRequestAllowed() {
        String key = TEST_KEY_PREFIX + "user1";
        boolean res = rateLimiter.isAllowed(key, 10, 60);
        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("Request within limit should be allowed")
    void requestWithinLimitShouldBeAllowed() {
        String key = TEST_KEY_PREFIX + "user2";
        int limit = 5;
        for(int i=0;i<limit;i++) {
            boolean res = rateLimiter.isAllowed(key, limit, 60);
            assertThat(res).isTrue();
        }
    }

    @Test
    @DisplayName("Request exceeding limit should be denied")
    void requestExceedingLimitShouldBeDenied() {
        String key = TEST_KEY_PREFIX + "user3";
        int limit = 3;
        for (int i=0;i<limit;i++) {
            rateLimiter.isAllowed(key, limit, 60);
        }
        boolean res = rateLimiter.isAllowed(key, limit, 60);
        assertThat(res).isFalse();
    }

    @Test
    @DisplayName("Remaining count should decrease with each request")
    void remainingCountShouldDecreaseWithEachRequest() {
        String key = TEST_KEY_PREFIX + "user4";
        int limit = 10;

        rateLimiter.isAllowed(key, limit, 60);
        long remaining = rateLimiter.getRemaining(key, limit, 60);
        assertThat(remaining).isEqualTo(9);

        rateLimiter.isAllowed(key, limit, 60);
        remaining = rateLimiter.getRemaining(key, limit, 60);
        assertThat(remaining).isEqualTo(8);
    }

    @Test
    @DisplayName("Remaining should never go below zero")
    void remainingShouldNeverGoBelowZero() {
        String key = TEST_KEY_PREFIX + "user5";
        int limit = 2;

        for (int i=0;i<4;i++) {
            rateLimiter.isAllowed(key, limit, 60);
        }

        long remaining = rateLimiter.getRemaining(key, limit, 60);
        assertThat(remaining).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Different key should have independent counters")
    void differentKeyShouldHaveIndependentCounters() {
        String k1 = TEST_KEY_PREFIX + "userA";
        String k2 = TEST_KEY_PREFIX + "userB";
        int limit = 2;

        rateLimiter.isAllowed(k1, limit, 60);
        rateLimiter.isAllowed(k1, limit, 60);
        boolean k1Denied = !rateLimiter.isAllowed(k1, limit, 60);

        boolean k2Allowed = rateLimiter.isAllowed(k2, limit, 60);

        assertThat(k1Denied).isTrue();
        assertThat(k2Allowed).isTrue();
    }
}
