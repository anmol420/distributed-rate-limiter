package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.annotation.RateLimit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final RedisTemplate<String, Long> redisTemplate;

    public HealthController(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RateLimit(limit = 10, windowSeconds = 50, algorithm = "tokenBucket")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        String redis;
        try {
            assert redisTemplate.getConnectionFactory() != null;
            redisTemplate.getConnectionFactory().getConnection().ping();
            redis = "UP";
        } catch (Exception e) {
            redis = "DOWN - " + e.getMessage();
        }
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.toString(),
                "redis", redis,
                "message", "OK!"
        ));
    }
}
