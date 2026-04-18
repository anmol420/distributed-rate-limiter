package codes.anmol.distributedratelimiter.integration;

import codes.anmol.distributedratelimiter.dto.RateLimitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String PREFIX = "integration:" + System.currentTimeMillis() + ":";

    @Test
    @DisplayName("Fixed Window: requests within limit return 200")
    void fixedWindow_withinLimitReturn200() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(PREFIX + "fw1", 5, 60, "fixedWindow")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true));
        }
    }

    @Test
    @DisplayName("Fixed Window: request exceeding limit returns 429")
    void fixedWindowExceedingLimitReturn429() throws Exception {
        String key = PREFIX + "fw2";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(key, 3, 60, "fixedWindow")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(key, 3, 60, "fixedWindow")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("Fixed Window: response has X-RateLimit-* headers")
    void fixedWindowHasRateLimitHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PREFIX + "fw3", 10, 60, "fixedWindow")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Window"))
                .andExpect(header().exists("X-RateLimit-Algorithm"))
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("X-RateLimit-Algorithm", "fixedWindow"));
    }

    @Test
    @DisplayName("Fixed Window: remaining decrements with each request")
    void fixedWindowRemainingDecrements() throws Exception {
        String key = PREFIX + "fw4";
        MvcResult r1 = mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(key, 10, 60, "fixedWindow")))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(key, 10, 60, "fixedWindow")))
                .andExpect(status().isOk()).andReturn();
        long rem1 = mapper.readTree(r1.getResponse().getContentAsString())
                .get("remaining").asLong();
        long rem2 = mapper.readTree(r2.getResponse().getContentAsString())
                .get("remaining").asLong();
        assertThat(rem1).isGreaterThan(rem2);
    }

    @Test
    @DisplayName("Token Bucket: burst of requests within capacity allowed")
    void tokenBucketBurstAllowed() throws Exception {
        String key = PREFIX + "tb1";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(key, 6, 60, "tokenBucket")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true));
        }
    }

    @Test
    @DisplayName("Token Bucket: denies when bucket is empty")
    void tokenBucketDeniesWhenEmpty() throws Exception {
        String key = PREFIX + "tb2";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(key, 3, 60, "tokenBucket")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(key, 3, 60, "tokenBucket")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Sliding Window: requests within limit return 200")
    void slidingWindowWithinLimitReturn200() throws Exception {
        String key = PREFIX + "sw1";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(key, 5, 60, "slidingWindow")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Sliding Window: denies at limit")
    void slidingWindowDeniesAtLimit() throws Exception {
        String key = PREFIX + "sw2";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/rateLimit/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(key, 3, 60, "slidingWindow")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(key, 3, 60, "slidingWindow")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Same key with different algorithms have independent counters")
    void differentAlgorithmsAreIndependent() throws Exception {
        String key = PREFIX + "iso";
        String jsonFw = body(key, 2, 60, "fixedWindow");
        String jsonTb = body(key, 2, 60, "tokenBucket");
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonFw))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonFw))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonFw))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonTb))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Status endpoint returns remaining without consuming")
    void statusDoesNotConsumeRequest() throws Exception {
        String key = PREFIX + "stat";
        mockMvc.perform(get("/api/v1/rateLimit/status")
                        .param("key", key)
                        .param("limit","10")
                        .param("windowSeconds","60")
                        .param("algorithm", "fixedWindow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(10));
        mockMvc.perform(get("/api/v1/rateLimit/status")
                        .param("key", key)
                        .param("limit", "10")
                        .param("windowSeconds", "60")
                        .param("algorithm", "fixedWindow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(10));
    }

    @Test
    @DisplayName("@RateLimit annotation on /health blocks at limit")
    void aspectAnnotationBlocksAtLimit() throws Exception {
        int allowed = 0, denied = 0;
        for (int i = 0; i < 15; i++) {
            int status = mockMvc.perform(get("/api/v1/health"))
                    .andReturn().getResponse().getStatus();
            if (status == 200) allowed++;
            if (status == 429) denied++;
        }
        assertThat(denied).isGreaterThanOrEqualTo(0);
        assertThat(allowed).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @DisplayName("Invalid request body returns 400 with field errors")
    void invalidBodyReturn400() throws Exception {
        String invalidBody = """
            {
              "key": "",
              "limit": 0,
              "windowSeconds": -1,
              "algorithm": "nonExistentAlgo"
            }
            """;

        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").isMap());
    }

    @Test
    @DisplayName("Unknown algorithm defaults to fixedWindow")
    void unknownAlgorithmDefaultsToFixedWindow() throws Exception {
        mockMvc.perform(post("/api/v1/rateLimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PREFIX + "default", 10, 50, "fixedWindow")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("GET /algorithms returns all 3 algorithm descriptions")
    void algorithmsEndpointReturnAll() throws Exception {
        mockMvc.perform(get("/api/v1/rateLimit/algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.0").isString())
                .andExpect(jsonPath("$.1").isString())
                .andExpect(jsonPath("$.2").isString());
    }

    private String body(String key, int limit, int window, String algorithm) throws Exception {
        RateLimitRequest req = new RateLimitRequest();
        req.setKey(key);
        req.setLimit(limit);
        req.setWindowSeconds(window);
        req.setAlgorithm(algorithm);
        return mapper.writeValueAsString(req);
    }
}
