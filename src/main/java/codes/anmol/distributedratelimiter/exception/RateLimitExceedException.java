package codes.anmol.distributedratelimiter.exception;

import lombok.Getter;

public class RateLimitExceedException extends RuntimeException {

    @Getter
    private final String key;

    @Getter
    private final int limit;

    @Getter
    private final int windowSeconds;

    public RateLimitExceedException(String key, int limit, int windowSeconds) {
        super("Rate limit exceeded for key - " + key);
        this.key = key;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

}
