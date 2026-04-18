package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.metrics.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Rate limiter observability — request counts and latency")
public class RateLimitMetricsController {

    private final MetricsService metricsService;

    public RateLimitMetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Operation(
            summary     = "Full metrics snapshot",
            description = "Returns total requests, allowed/denied counts, average latency, "
                    + "per-algorithm breakdown, top denied keys, and hourly breakdown."
    )
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(metricsService.getSummary());
    }

    @Operation(
            summary     = "Reset all metrics counters",
            description = "Deletes all metrics:* keys from Redis. Call before a clean benchmark run."
    )
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        metricsService.reset();
        return ResponseEntity.ok(Map.of(
                "message", "All metrics reset successfully",
                "status",  "OK"
        ));
    }
}
