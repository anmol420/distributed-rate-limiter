package codes.anmol.distributedratelimiter.repository;

import codes.anmol.distributedratelimiter.model.RateLimitConfigEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class RateLimitConfigRepository {

    private static final String CONFIG_KEY_PREFIX = "config:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitConfigRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    public RateLimitConfigEntity save(RateLimitConfigEntity rateLimitConfigEntity) {
        String key = buildKey(rateLimitConfigEntity.getIdentifier());
        String value = toJson(rateLimitConfigEntity);
        redisTemplate.opsForValue().set(key, value);
        return rateLimitConfigEntity;
    }

    public Optional<RateLimitConfigEntity> findByIdentifier(String identifier) {
        String key = buildKey(identifier);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        return Optional.of(fromJson(value));
    }

    public boolean existsByIdentifier(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(identifier)));
    }

    public boolean deleteByIdentifier(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.delete(buildKey(identifier)));
    }

    public List<RateLimitConfigEntity> findAll() {
        Set<String> keys = redisTemplate.keys(CONFIG_KEY_PREFIX + "*");
        List<RateLimitConfigEntity> configs = new ArrayList<>();

        if (keys == null) return configs;

        for (String key: keys) {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                configs.add(fromJson(value));
            }
        }
        return configs;
    }

    private String buildKey(String identifier) {
        return CONFIG_KEY_PREFIX + identifier;
    }

    private String toJson(RateLimitConfigEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize RateLimitConfigEntity", e);
        }
    }

    private RateLimitConfigEntity fromJson(String json) {
        try {
            return objectMapper.readValue(json, RateLimitConfigEntity.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize RateLimitConfigEntity", e);
        }
    }
}
