package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.dto.RateLimitConfigRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String PREFIX = "integrationTest:" + System.currentTimeMillis() + ":";

    @Test
    @DisplayName("POST /config — creates config and returns 201")
    void createConfigReturns200() throws Exception {
        RateLimitConfigRequest request = buildRequest(PREFIX + "user:xyz", 500, 60, "tokenBucket");
        mockMvc.perform(
                post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value(PREFIX + "user:xyz"))
                .andExpect(jsonPath("$.limit").value(500))
                .andExpect(jsonPath("$.algorithm").value("tokenBucket"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /config — duplicate identifier returns 400")
    void createConfigDuplicateReturns400() throws Exception {
        RateLimitConfigRequest request = buildRequest(PREFIX + "user:uuu", 100, 50, "fixedWindow");
        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(
                post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    @Test
    @DisplayName("POST /config — missing required fields returns 400 with field errors")
    void createConfigMissingFieldsReturns400() throws Exception {
        String invalidBody = """
                {
                    "identifier": "",
                    "limit": 0,
                    "windowSeconds": 0
                }
                """;
        mockMvc.perform(post("/api/v1/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").isMap());
    }

    @Test
    @DisplayName("GET /config — returns paginated response with correct structure")
    void listConfigReturnsPaginatedResponse() throws Exception {
        for (int i=0;i<3;i++) {
            RateLimitConfigRequest request = buildRequest(
                    PREFIX + "page:user:" + i, 100 * i+1, 60, "fixedWindow"
            );
            mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/v1/config")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    @DisplayName("GET /config?algorithm={algorithm} — filters by algorithm")
    void listConfigFilterByAlgorithm() throws Exception {
        mockMvc.perform(post("/api/v1/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            buildRequest(PREFIX + "filter:tb", 100, 60, "tokenBucket")
                    )))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(PREFIX + "filter:fw", 100, 60, "fixedWindow")
                        )))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/config")
                    .param("algorithm", "tokenBucket")
                    .param("page", "0")
                    .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].algorithm", everyItem(equalTo("tokenBucket"))));
    }

    @Test
    @DisplayName("GET /config/{id} — returns correct config")
    void getOneReturnCorrectConfig() throws Exception {
        String identifier = PREFIX + "user:abc";
        mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(identifier, 250, 30, "slidingWindow"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/config/" + identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value(identifier))
                .andExpect(jsonPath("$.limit").value(250))
                .andExpect(jsonPath("$.algorithm").value("slidingWindow"));
    }

    @Test
    @DisplayName("GET /config/{id} — unknown identifier returns 404")
    void getConfigReturnUnknownIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/config/" + PREFIX + "obc"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /config/{id} — updates config correctly")
    void updateConfig() throws Exception {
        String identifier = PREFIX + "user:axy";
        mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(identifier, 100, 60, "fixedWindow"))))
                .andExpect(status().isCreated());
        RateLimitConfigRequest update = buildRequest(identifier, 9999, 120, "tokenBucket");
        mockMvc.perform(put("/api/v1/config/" + identifier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(9999))
                .andExpect(jsonPath("$.windowSeconds").value(120))
                .andExpect(jsonPath("$.algorithm").value("tokenBucket"));
    }

    @Test
    @DisplayName("DELETE /config/{id} — deletes and confirms removal")
    void deleteConfig() throws Exception {
        String identifier = PREFIX + "user:eyes";
        mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(identifier, 100, 60, "fixedWindow"))))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/config/" + identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("deleted")));
        mockMvc.perform(get("/api/v1/config/" + identifier))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /config/{id} — unknown identifier returns 404")
    void deleteUnknown_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/config/" + PREFIX + "ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /config/{id}/toggle — flips active flag")
    void toggleConfig() throws Exception {
        String identifier = PREFIX + "user:xyx";
        mockMvc.perform(post("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(identifier, 100, 60, "fixedWindow"))))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/api/v1/config/" + identifier + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch("/api/v1/config/" + identifier + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("DELETE /config/bulk?algorithm=X — deletes matching configs")
    void bulkDelete() throws Exception {
        for (int i=0;i<2;i++) {
            mockMvc.perform(post("/api/v1/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(PREFIX + "bulk:sw:" + i, 100, 60, "slidingWindow"))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(delete("/api/v1/config/bulk")
                        .param("algorithm", "slidingWindow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("slidingWindow"))
                .andExpect(jsonPath("$.deletedCount").isNumber());
    }

    private RateLimitConfigRequest buildRequest(String identifier, int limit, int window, String algorithm) {
        RateLimitConfigRequest req = new RateLimitConfigRequest();
        req.setIdentifier(identifier);
        req.setLimit(limit);
        req.setWindowSeconds(window);
        req.setAlgorithm(algorithm);
        return req;
    }
}
