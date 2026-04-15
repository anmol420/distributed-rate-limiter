package codes.anmol.distributedratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RateLimitConfigRequest {

    @NotBlank(message = "identifier should not be blank")
    private String identifier;

    @Min(value = 1, message = "limit must be least 1")
    private int limit;

    @Min(value = 1, message = "windowSeconds must be least 1")
    private int windowSeconds;

    @Pattern(
            regexp = "fixedWindow|tokenBucket|slidingWindow",
            message = "algorithm must be one of: fixedWindow, tokenBucket, slidingWindow"
    )
    private String algorithm = "fixedWindow";
}
