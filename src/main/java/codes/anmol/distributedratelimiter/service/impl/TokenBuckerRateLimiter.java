package codes.anmol.distributedratelimiter.service.impl;

import codes.anmol.distributedratelimiter.service.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

public class TokenBuckerRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Long> redisTemplate;

    private static final String TOKEN_BUCKET_LUA_SCRIPT = """
                local key = KEYS[1]
                local maxToken = tonumber(ARGV[1])
                local refillRate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                local ttl = tonumber(ARGV[4])
            
                local currTokens = tonumber(redis.call('HGET', key, 'tokens'))
                local lastRefillTime = tonumber(redis.call('HGET', key, 'lastRefillTime'))
            
                if currTokens == nil then
                    currTokens = maxToken
                    lastRefillTime = now
                end
            
                local elapsedSec = (now - lastRefillTime)/1000.0
                local tokenToAdd = elapsedSec * refillRate
            
                currTokens = math.min(maxToken, currTokens+tokenToAdd)
            
                local allowed = 0
                if currTokens >= 1 then
                    currTokens = currTokens - 1
                    allowed = 1
                end
            
                redis.call('HSET', key, 'tokens', tostring(currTokens))
                redis.call('HSET', key, 'lastRefillTime', tostring(now))
                redis.call('EXPIRE', key, ttl)
            
                return {allowed, math.floor(currTokens)}
            """;

    public TokenBuckerRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        List<Long> result = executeLuaScript(key, limit, windowSeconds);
        return result != null && result.getFirst() == 1L;
    }

    @Override
    public long getRemaining(String key, int limit, int windowSeconds) {
        List<Long> result = executeLuaScript(key, limit, windowSeconds);
        if (result == null) return 0;
        return result.get(1);
    }

    private List<Long> executeLuaScript(String key, int limit, int windowSeconds) {
        String redisKey = buildKey(key);
        double refillRate = (double) limit / windowSeconds;
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setScriptText(TOKEN_BUCKET_LUA_SCRIPT);
        script.setResultType(getListLongClass());
        return redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                String.valueOf(limit),
                String.valueOf(refillRate),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowSeconds * 2)
        );
    }

    private String buildKey(String key) {
        return "rate:token:" + key;
    }

    @SuppressWarnings("unchecked")
    private Class<List<Long>> getListLongClass() {
        return (Class<List<Long>>) (Class<?>) List.class;
    }
}
