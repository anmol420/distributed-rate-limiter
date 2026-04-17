package codes.anmol.distributedratelimiter.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MetricsServiceTest {

    @Autowired
    private MetricsService metricsService;

    @BeforeEach
    void setup() {
        metricsService.reset();
    }

    @Test
    @DisplayName("Record allowed request increments totalRequests and totalAllowed")
    void recordAllowed() {
        metricsService.record("fixedWindow", true, 2L, "user:test1");
        Map<String, Object> summary = metricsService.getSummary();
        assertThat((Long) summary.get("totalRequests")).isEqualTo(1L);
        assertThat((Long) summary.get("totalAllowed")).isEqualTo(1L);
        assertThat((Long) summary.get("totalDenied")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Record denied request increments totalDenied")
    void recordDenied() {
        metricsService.record("fixedWindow", false, 1L, "user:spammer");
        Map<String, Object> summary = metricsService.getSummary();
        assertThat((Long) summary.get("totalDenied")).isEqualTo(1L);
        assertThat((Long) summary.get("totalAllowed")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Average latency is calculated correctly")
    void averageLatency() {
        metricsService.record("fixedWindow", true, 2L, "u1");
        metricsService.record("fixedWindow", true, 4L, "u2");
        metricsService.record("fixedWindow", true, 6L, "u3");
        Map<String, Object> summary = metricsService.getSummary();
        double avg = (double) summary.get("averageLatencyMs");
        assertThat(avg).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Per-algorithm counters are tracked separately")
    void perAlgorithmCounters() {
        metricsService.record("fixedWindow",   true,  1L, "u1");
        metricsService.record("tokenBucket",   true,  1L, "u2");
        metricsService.record("tokenBucket",   false, 1L, "u3");
        metricsService.record("slidingWindow", true,  1L, "u4");
        Map<String, Object> summary = metricsService.getSummary();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Long>> byAlgo =
                (Map<String, Map<String, Long>>) summary.get("byAlgorithm");
        assertThat(byAlgo.get("fixedWindow").get("total")).isEqualTo(1L);
        assertThat(byAlgo.get("tokenBucket").get("total")).isEqualTo(2L);
        assertThat(byAlgo.get("tokenBucket").get("denied")).isEqualTo(1L);
        assertThat(byAlgo.get("slidingWindow").get("total")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Reset clears all counters")
    void resetClearsCounters() {
        metricsService.record("fixedWindow", true,  2L, "u1");
        metricsService.record("fixedWindow", false, 1L, "u2");
        metricsService.reset();
        Map<String, Object> summary = metricsService.getSummary();
        assertThat((Long) summary.get("totalRequests")).isEqualTo(0L);
        assertThat((Long) summary.get("totalAllowed")).isEqualTo(0L);
        assertThat((Long) summary.get("totalDenied")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Denied percent is formatted correctly")
    void deniedPercent() {
        metricsService.record("fixedWindow", true,  1L, "u1");
        metricsService.record("fixedWindow", true,  1L, "u2");
        metricsService.record("fixedWindow", true,  1L, "u3");
        metricsService.record("fixedWindow", false, 1L, "u4");
        Map<String, Object> summary = metricsService.getSummary();
        assertThat((String) summary.get("deniedPercent")).isEqualTo("25.00%");
    }

    @Test
    @DisplayName("Summary returns all expected keys")
    void summaryHasAllKeys() {
        Map<String, Object> summary = metricsService.getSummary();
        assertThat(summary).containsKeys(
                "totalRequests",
                "totalAllowed",
                "totalDenied",
                "deniedPercent",
                "averageLatencyMs",
                "totalMeasurements",
                "byAlgorithm",
                "topDeniedKeys",
                "hourlyBreakdown"
        );
    }
}
