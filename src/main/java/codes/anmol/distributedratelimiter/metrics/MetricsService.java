package codes.anmol.distributedratelimiter.metrics;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class MetricsService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration HOURLY_TTL  = Duration.ofHours(25);
    private static final Duration LATENCY_TTL = Duration.ofDays(7);

    public MetricsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void record(String algorithm, boolean allowed, long latencyMs, String key) {
        redisTemplate.opsForValue().increment("metrics:total:requests");
        if (allowed) {
            redisTemplate.opsForValue().increment("metrics:total:allowed");
        } else {
            redisTemplate.opsForValue().increment("metrics:total:denied");
            redisTemplate.opsForZSet().incrementScore("metrics:denied:keys", key, 1.0);
        }
        redisTemplate.opsForValue().increment("metrics:algo:" + algorithm + ":total");
        if (!allowed) {
            redisTemplate.opsForValue().increment("metrics:algo:" + algorithm + ":denied");
        }
        String hourKey = "metrics:hourly:" + Instant.now().toString().substring(0, 13)
                .replace("T", "-");
        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, HOURLY_TTL);

        redisTemplate.opsForValue().increment("metrics:latency:total", Long.parseLong(String.valueOf(latencyMs)));
        redisTemplate.opsForValue().increment("metrics:latency:count");
        redisTemplate.expire("metrics:latency:total", LATENCY_TTL);
        redisTemplate.expire("metrics:latency:count", LATENCY_TTL);
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        long totalRequests = getLong("metrics:total:requests");
        long totalAllowed  = getLong("metrics:total:allowed");
        long totalDenied   = getLong("metrics:total:denied");

        summary.put("totalRequests", totalRequests);
        summary.put("totalAllowed",  totalAllowed);
        summary.put("totalDenied",   totalDenied);
        summary.put("deniedPercent",
                totalRequests > 0
                        ? String.format("%.2f%%", (double) totalDenied / totalRequests * 100)
                        : "0.00%"
        );

        long latencySum   = getLong("metrics:latency:total");
        long latencyCount = getLong("metrics:latency:count");
        summary.put("averageLatencyMs", latencyCount > 0 ? (double) latencySum / latencyCount : 0.0);
        summary.put("totalMeasurements", latencyCount);

        Map<String, Map<String, Long>> algoStats = new HashMap<>();
        for (String algo : new String[]{"fixedWindow", "tokenBucket", "slidingWindow"}) {
            Map<String, Long> stats = new HashMap<>();
            stats.put("total",  getLong("metrics:algo:" + algo + ":total"));
            stats.put("denied", getLong("metrics:algo:" + algo + ":denied"));
            algoStats.put(algo, stats);
        }
        summary.put("byAlgorithm", algoStats);

        Set<String> topDenied = redisTemplate.opsForZSet()
                .reverseRange("metrics:denied:keys", 0, 4);
        summary.put("topDeniedKeys", topDenied);

        Map<String, Long> hourly = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            Instant hourInstant = Instant.now().minus(Duration.ofHours(i));
            String hourKey = "metrics:hourly:" + hourInstant.toString()
                    .substring(0, 13).replace("T", "-");
            String label = hourInstant.toString().substring(11, 13) + ":00";
            hourly.put(label, getLong(hourKey));
        }
        summary.put("hourlyBreakdown", hourly);

        return summary;
    }

    public void reset() {
        Set<String> keys = redisTemplate.keys("metrics:*");
        if (keys!=null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private long getLong(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
