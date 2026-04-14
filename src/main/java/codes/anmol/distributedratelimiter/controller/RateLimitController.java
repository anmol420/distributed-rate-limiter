package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.dto.RateLimitRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitResponse;
import codes.anmol.distributedratelimiter.exception.RateLimitExceedException;
import codes.anmol.distributedratelimiter.service.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path="/api/v1/rateLimit")
public class RateLimitController {

    private final RateLimiter fixedWindowRateLimiter;
    private final RateLimiter tokenBucketRateLimiter;
    private final RateLimiter slidingWindowRateLimiter;

    public RateLimitController(
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

    private RateLimiter resolveAlgo(String algorithm) {
        return switch (algorithm) {
            case "tokenBucket" -> tokenBucketRateLimiter;
            case "slidingWindow" -> slidingWindowRateLimiter;
            default -> fixedWindowRateLimiter;
        };
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @Valid
            @RequestBody
            RateLimitRequest request
    ) {
        RateLimiter limiter = resolveAlgo(request.getAlgorithm());
        boolean allowed = limiter.isAllowed(
                request.getKey(),
                request.getLimit(),
                request.getWindowSeconds()
        );
        long remaining = limiter.getRemaining(
                request.getKey(),
                request.getLimit(),
                request.getWindowSeconds()
        );
        if (!allowed) {
            throw new RateLimitExceedException(
                    request.getKey(),
                    request.getLimit(),
                    request.getWindowSeconds()
            );
        }
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(true)
                .remaining(remaining)
                .limit(request.getLimit())
                .windowSeconds(request.getWindowSeconds())
                .key(request.getKey())
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(request.getLimit()));
        headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.add("X-RateLimit-Window", String.valueOf(request.getWindowSeconds()));
        headers.add("X-RateLimit-Algorithm", request.getAlgorithm());

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<RateLimitResponse> getStatus(
            @RequestParam
            String key,

            @RequestParam
            int limit,

            @RequestParam
            int windowSeconds,

            @RequestParam(defaultValue = "fixedWindow")
            String algorithm
    ) {
        RateLimiter limiter = resolveAlgo(algorithm);
        long remaining = limiter.getRemaining(key, limit, windowSeconds);
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(remaining>0)
                .remaining(remaining)
                .limit(limit)
                .windowSeconds(windowSeconds)
                .key(key)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/algorithms")
    public ResponseEntity<Map<String, String>> listAlgorithms() {
        return ResponseEntity.ok(Map.of(
                "0", "fixedWindow",
                "1", "tokenBucket",
                "2", "slidingWindow"
        ));
    }
}
