function token_bucket_atomic()
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
end