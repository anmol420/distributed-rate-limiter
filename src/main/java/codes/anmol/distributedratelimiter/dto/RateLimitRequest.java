package codes.anmol.distributedratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RateLimitRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @Min(value = 1, message = "Must be least 1")
    private int limit;

    @Min(value = 1, message = "Must be least 1 second")
    private int windowSeconds;
}
