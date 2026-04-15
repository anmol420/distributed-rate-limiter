package codes.anmol.distributedratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfigEntity {

    private String identifier;

    private int limit;

    private int windowSeconds;

    private String algorithm;

    private boolean active;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;
}
