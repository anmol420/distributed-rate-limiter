package codes.anmol.distributedratelimiter.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {

    private boolean allowed;
    private long remaining;
    private int limit;
    private int windowSeconds;
    private String key;
}
