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

@RestController
@RequestMapping(path="/api/v1/rateLimit")
public class RateLimitController {

    private final RateLimiter fixedWindowRateLimiter;

    public RateLimitController(
            @Qualifier("fixedWindowRateLimiter")
            RateLimiter rateLimiter
    ) {
        this.fixedWindowRateLimiter = rateLimiter;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @Valid
            @RequestBody
            RateLimitRequest request
    ) {
        boolean allowed = fixedWindowRateLimiter.isAllowed(
                request.getKey(),
                request.getLimit(),
                request.getWindowSeconds()
        );
        long remaining = fixedWindowRateLimiter.getRemaining(
                request.getKey(),
                request.getLimit(),
                request.getWindowSeconds()
        );
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(allowed)
                .remaining(remaining)
                .limit(request.getLimit())
                .windowSeconds(request.getWindowSeconds())
                .key(request.getKey())
                .build();
        if (!allowed) {
            throw new RateLimitExceedException(
                    request.getKey(),
                    request.getLimit(),
                    request.getWindowSeconds()
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(request.getLimit()));
        headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.add("X-RateLimit-Window", String.valueOf(request.getWindowSeconds()));

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<RateLimitResponse> getStatus(
            @RequestParam
            String key,

            @RequestParam
            int limit,

            @RequestParam
            int windowSeconds
    ) {
        long remaining = fixedWindowRateLimiter.getRemaining(key, limit, windowSeconds);
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(remaining>0)
                .remaining(remaining)
                .limit(limit)
                .windowSeconds(windowSeconds)
                .key(key)
                .build();

        return ResponseEntity.ok(response);
    }
}
