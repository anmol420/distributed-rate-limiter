package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.annotation.RateLimit;
import codes.anmol.distributedratelimiter.dto.RateLimitRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitResponse;
import codes.anmol.distributedratelimiter.exception.RateLimitExceedException;
import codes.anmol.distributedratelimiter.service.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final RateLimiter fixedWindowRateLimiter;
    private final RateLimiter tokenBucketRateLimiter;
    private final RateLimiter slidingWindowRateLimiter;

    public BenchmarkController(
            @Qualifier("fixedWindowRateLimiter")
            RateLimiter fixedWindowRateLimiter,

            @Qualifier("tokenBucketRateLimiter")
            RateLimiter tokenBucketRateLimiter,

            @Qualifier("slidingWindowRateLimiter")
            RateLimiter slidingWindowRateLimiter
    ) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.slidingWindowRateLimiter = slidingWindowRateLimiter;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "pong"));
    }

    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "fixedWindow", keyBy = "IP")
    @GetMapping("/fixed-window")
    public ResponseEntity<Map<String, Object>> fixedWindowBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "fixedWindow",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "tokenBucket", keyBy = "IP")
    @GetMapping("/token-bucket")
    public ResponseEntity<Map<String, Object>> tokenBucketBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "tokenBucket",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "slidingWindow", keyBy = "IP")
    @GetMapping("/sliding-window")
    public ResponseEntity<Map<String, Object>> slidingWindowBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "slidingWindow",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @GetMapping("/concurrent-test")
    public ResponseEntity<Map<String, Object>> concurrentTest(
            @RequestParam(defaultValue = "benchmark-user")
            String userID
    ) {
        long start = System.nanoTime();
        boolean allowed = fixedWindowRateLimiter.isAllowed(
                "concurrent:" + userID, 10000, 60
        );
        long remaining = fixedWindowRateLimiter.getRemaining(
                "concurrent:" + userID, 10000, 60
        );
        long latencyNs = System.nanoTime() - start;
        return ResponseEntity.ok(Map.of(
                "allowed",       allowed,
                "remaining",     remaining,
                "latencyNanos",  latencyNs,
                "latencyMs",     latencyNs / 1_000_000.0,
                "userID",        userID,
                "ts",            System.currentTimeMillis()
        ));
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> check(
            @RequestBody
            RateLimitRequest request
    ) {
        RateLimiter limiter = switch (request.getAlgorithm()) {
            case "tokenBucket"   -> tokenBucketRateLimiter;
            case "slidingWindow" -> slidingWindowRateLimiter;
            default              -> fixedWindowRateLimiter;
        };
        boolean allowed = limiter.isAllowed(
                request.getKey(), request.getLimit(), request.getWindowSeconds()
        );
        long remaining = limiter.getRemaining(
                request.getKey(), request.getLimit(), request.getWindowSeconds()
        );
        if (!allowed) {
            throw new RateLimitExceedException(
                    request.getKey(), request.getLimit(), request.getWindowSeconds()
            );
        }
        return ResponseEntity.ok(
                RateLimitResponse.builder()
                        .allowed(true)
                        .remaining(remaining)
                        .limit(request.getLimit())
                        .windowSeconds(request.getWindowSeconds())
                        .key(request.getKey())
                        .build()
        );
    }
}
