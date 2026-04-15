package codes.anmol.distributedratelimiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfigResponse {

    private String identifier;
    private int limit;
    private int windowSeconds;
    private String algorithm;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
