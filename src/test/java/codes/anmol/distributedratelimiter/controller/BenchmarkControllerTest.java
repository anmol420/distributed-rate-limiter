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
public class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /benchmark/ping returns pong")
    void ping() throws Exception {
        mockMvc.perform(get("/benchmark/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pong"));
    }

    @Test
    @DisplayName("GET /benchmark/fixed-window returns 200")
    void fixedWindowBenchmark() throws Exception {
        mockMvc.perform(get("/benchmark/fixed-window"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("fixedWindow"))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("GET /benchmark/token-bucket returns 200")
    void tokenBucketBenchmark() throws Exception {
        mockMvc.perform(get("/benchmark/token-bucket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("tokenBucket"))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("GET /benchmark/sliding-window returns 200")
    void slidingWindowBenchmark() throws Exception {
        mockMvc.perform(get("/benchmark/sliding-window"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("slidingWindow"))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("GET /benchmark/concurrent-test returns latency fields")
    void concurrentTest() throws Exception {
        mockMvc.perform(get("/benchmark/concurrent-test")
                        .param("userId", "test-user-" + System.currentTimeMillis()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").isBoolean())
                .andExpect(jsonPath("$.latencyMs").isNumber())
                .andExpect(jsonPath("$.remaining").isNumber());
    }
}
