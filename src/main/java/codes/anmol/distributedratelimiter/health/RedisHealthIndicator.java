package codes.anmol.distributedratelimiter.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("redisCustom")
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            long start = System.currentTimeMillis();
            assert redisTemplate.getConnectionFactory() != null;
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            long latencyMs = System.currentTimeMillis() - start;
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail("ping",       "PONG")
                        .withDetail("latencyMs",  latencyMs)
                        .withDetail("checkedAt",  Instant.now().toString())
                        .build();
            }
            return Health.down()
                    .withDetail("ping",    pong)
                    .withDetail("reason",  "Unexpected ping response")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error",   e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetail("checkedAt", Instant.now().toString())
                    .build();
        }
    }
}
