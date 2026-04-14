package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.service.impl.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SlidingWindowRateLimiterTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private SlidingWindowRateLimiter rateLimiter;

    private final String KEY_PREFIX = "test:sliding:" + System.currentTimeMillis() + ":";

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("First request should be allowed")
    void firstRequestAllowed() {
        assertThat(rateLimiter.isAllowed(KEY_PREFIX + "u1", 5, 60)).isTrue();
    }

    @Test
    @DisplayName("Request within limit should be allowed")
    void requestWithinLimitShouldBeAllowed() {
        String key = KEY_PREFIX + "u2";
        int limit = 10;
        for (int i=0;i<limit;i++) {
            assertThat(rateLimiter.isAllowed(key, limit, 60))
                    .as("Request %d should be allowed", i + 1)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Request exceeding limit should be denied")
    void requestExceedingLimitDenied() {
        String key = KEY_PREFIX + "u3";
        int limit = 5;
        for (int i=0;i<limit;i++) {
            rateLimiter.isAllowed(key, limit, 60);
        }
        assertThat(rateLimiter.isAllowed(key, limit, 60)).isFalse();
    }

    @Test
    @DisplayName("Old requests should be removed")
    void oldRequestsShouldBeRemoved() throws InterruptedException {
        String key = KEY_PREFIX + "slide";
        int limit = 3;
        int windowSeconds = 5;
        for (int i=0;i<limit;i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }
        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds)).isFalse();

        Thread.sleep(5000);

        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds)).isTrue();
    }

    @Test
    @DisplayName("Remaining count is accurate")
    void remainingCountIsAccurate() {
        String key = KEY_PREFIX + "u4";
        int limit = 5;
        for (int i=0;i<2;i++) {
            rateLimiter.isAllowed(key, limit, 60);
        }
        assertThat(rateLimiter.getRemaining(key, limit, 60)).isEqualTo(3);
    }

    @Test
    @DisplayName("Different users have independent windows")
    void differentUsersHaveIndependentWindows() {
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
}
