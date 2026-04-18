package codes.anmol.distributedratelimiter.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/metrics/summary returns all expected fields")
    void summaryHasAllFields() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").isNumber())
                .andExpect(jsonPath("$.totalAllowed").isNumber())
                .andExpect(jsonPath("$.totalDenied").isNumber())
                .andExpect(jsonPath("$.deniedPercent").isString())
                .andExpect(jsonPath("$.averageLatencyMs").isNumber())
                .andExpect(jsonPath("$.byAlgorithm").isMap())
                .andExpect(jsonPath("$.byAlgorithm.fixedWindow").isMap())
                .andExpect(jsonPath("$.byAlgorithm.tokenBucket").isMap())
                .andExpect(jsonPath("$.byAlgorithm.slidingWindow").isMap())
                .andExpect(jsonPath("$.topDeniedKeys").isArray())
                .andExpect(jsonPath("$.hourlyBreakdown").isMap());
    }

    @Test
    @DisplayName("POST /api/v1/metrics/reset returns success message")
    void resetReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/metrics/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All metrics reset successfully"))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("Metrics accumulate after rate limit checks")
    void metricsAccumulate() throws Exception {
        mockMvc.perform(post("/api/v1/metrics/reset"))
                .andExpect(status().isOk());
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/health")).andReturn();
        }
        mockMvc.perform(get("/api/v1/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").isNumber());
    }
}
