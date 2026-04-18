package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.dto.RateLimitRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitResponse;
import codes.anmol.distributedratelimiter.exception.RateLimitExceedException;
import codes.anmol.distributedratelimiter.service.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path="/api/v1/rateLimit")
@Tag(name = "Rate Limiting", description = "Core rate limit check endpoints")
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

    @Operation(
            summary     = "Check if a request is allowed",
            description = "Checks whether the given key has exceeded its rate limit. "
                    + "Returns 200 if allowed, 429 if limit exceeded. "
                    + "Choose algorithm per request via the 'algorithm' field."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request allowed"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded — retry after window resets"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rate limit check parameters",
                    content = @Content(
                            examples = @ExampleObject(value = """
                    {
                      "key": "user:xyz",
                      "limit": 100,
                      "windowSeconds": 60,
                      "algorithm": "tokenBucket"
                    }
                    """)
                    )
            )
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

    @Operation(
            summary     = "Get current usage for a key",
            description = "Returns remaining requests without consuming a request. Safe to poll."
    )
    @GetMapping("/status")
    public ResponseEntity<RateLimitResponse> getStatus(
            @Parameter(description = "Rate limit key", example = "user:alice")
            @RequestParam
            String key,

            @Parameter(description = "Request limit",  example = "100")
            @RequestParam
            int limit,

            @Parameter(description = "Window size in seconds", example = "60")
            @RequestParam
            int windowSeconds,

            @Parameter(description = "Algorithm name", example = "fixedWindow")
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

    @Operation(
            summary     = "List available algorithms",
            description = "Returns all supported rate limiting algorithms with descriptions."
    )
    @GetMapping("/algorithms")
    public ResponseEntity<Map<String, String>> listAlgorithms() {
        return ResponseEntity.ok(Map.of(
                "0", "fixedWindow",
                "1", "tokenBucket",
                "2", "slidingWindow"
        ));
    }
}
