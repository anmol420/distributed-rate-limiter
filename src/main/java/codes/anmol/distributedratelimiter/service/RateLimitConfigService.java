package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.model.RateLimitConfigEntity;
import codes.anmol.distributedratelimiter.repository.RateLimitConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RateLimitConfigService {

    private final RateLimitConfigRepository repository;

    public RateLimitConfigService(RateLimitConfigRepository repository) {
        this.repository = repository;
    }

    public RateLimitConfigEntity create(RateLimitConfigEntity entity) {
        if (repository.existsByIdentifier((entity.getIdentifier()))) {
            throw new IllegalArgumentException("Config already exists for identifier: " + entity.getIdentifier() + ". Use PUT to update it.");
        }
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setActive(true);

        return repository.save(entity);
    }

    public Optional<RateLimitConfigEntity> findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier);
    }

    public RateLimitConfigEntity update(String identifier, RateLimitConfigEntity entity) {
        RateLimitConfigEntity existingEntity = repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No config found for identifier: " + identifier
                ));
        entity.setIdentifier(identifier);
        entity.setCreatedAt(existingEntity.getCreatedAt());
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity);
    }

    public boolean delete(String identifier) {
        return repository.deleteByIdentifier(identifier);
    }

    public List<RateLimitConfigEntity> findAll() {
        return repository.findAll();
    }

    public RateLimitConfigEntity toggleActive(String identifier) {
        RateLimitConfigEntity existingEntity = repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No config found for identifier: " + identifier
                ));
        existingEntity.setActive(!existingEntity.isActive());
        existingEntity.setUpdatedAt(Instant.now());
        return repository.save(existingEntity);
    }
}
