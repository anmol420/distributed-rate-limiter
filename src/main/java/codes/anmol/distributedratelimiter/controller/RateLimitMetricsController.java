package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.metrics.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
public class RateLimitMetricsController {

    private final MetricsService metricsService;

    public RateLimitMetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(metricsService.getSummary());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        metricsService.reset();
        return ResponseEntity.ok(Map.of(
                "message", "All metrics reset successfully",
                "status",  "OK"
        ));
    }
}
