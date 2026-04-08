package codes.anmol.distributedratelimiter.service;

public interface RateLimiter {
    boolean isAllowed(String key, int limit, int windowSeconds);
    long getRemaining(String key, int limit, int windowSeconds);
}
