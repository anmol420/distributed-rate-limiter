package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.annotation.RateLimit;
import codes.anmol.distributedratelimiter.dto.RateLimitRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitResponse;
import codes.anmol.distributedratelimiter.exception.RateLimitExceedException;
import codes.anmol.distributedratelimiter.service.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/benchmark")
@Tag(name = "Benchmarking", description = "JMeter benchmark endpoints — do not use in production")
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

    @Operation(summary = "Baseline ping — no Redis, measures Spring overhead only")
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "pong"));
    }

    @Operation(summary = "Fixed Window benchmark endpoint — 600k req/min limit")
    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "fixedWindow", keyBy = "IP")
    @GetMapping("/fixed-window")
    public ResponseEntity<Map<String, Object>> fixedWindowBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "fixedWindow",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @Operation(summary = "Token Bucket benchmark endpoint — 600k req/min limit")
    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "tokenBucket", keyBy = "IP")
    @GetMapping("/token-bucket")
    public ResponseEntity<Map<String, Object>> tokenBucketBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "tokenBucket",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @Operation(summary = "Sliding Window benchmark endpoint — 600k req/min limit")
    @RateLimit(limit = 6000, windowSeconds = 60, algorithm = "slidingWindow", keyBy = "IP")
    @GetMapping("/sliding-window")
    public ResponseEntity<Map<String, Object>> slidingWindowBenchmark() {
        return ResponseEntity.ok(Map.of(
                "algorithm", "slidingWindow",
                "allowed",   true,
                "ts",        System.currentTimeMillis()
        ));
    }

    @Operation(
            summary     = "Concurrent test — rate limits by userId query param",
            description = "Use userId=shared-user in JMeter to force all 500 threads to hit the same key."
    )
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

    @Operation(summary = "Programmatic check — configurable algorithm and key")
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
