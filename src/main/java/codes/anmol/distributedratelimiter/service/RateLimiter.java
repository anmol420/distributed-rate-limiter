package codes.anmol.distributedratelimiter.repository;

public interface RateLimiter {
    boolean isAllowed(String key, int limit, int windowSeconds);
}
