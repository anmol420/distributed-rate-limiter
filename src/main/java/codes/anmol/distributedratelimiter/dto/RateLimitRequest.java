package codes.anmol.distributedratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RateLimitRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @Min(value = 1, message = "Must be least 1")
    private int limit;

    @Min(value = 1, message = "Must be least 1 second")
    private int windowSeconds;

    @Pattern(
            regexp = "fixedWindow|tokenBucket|slidingWindow",
            message = "algorithm must be one of: fixedWindow, tokenBucket, slidingWindow"
    )
    private String algorithm = "fixedWindow";
}
