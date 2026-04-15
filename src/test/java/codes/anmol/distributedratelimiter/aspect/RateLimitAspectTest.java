package codes.anmol.distributedratelimiter.aspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Request within limit should return 200")
    void requestWithinLimitShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redis").value("UP"));
    }

    @Test
    @DisplayName("Response inlcudes X-RateLimit headers")
    void responseIncludesRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Algorithm"));
    }

    @Test
    @DisplayName("Exceeding limits should return 429")
    void exceedingLimitsShouldReturn429() throws Exception {
        int res200 = 0;
        int res429 = 0;
        for (int i=0;i<20;i++) {
            int status = mockMvc.perform(get("/api/v1/health"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            if (status == 200) res200++;
            if (status == 429) res429++;
        }
        assertThat(res429).isGreaterThan(0);
    }
}
