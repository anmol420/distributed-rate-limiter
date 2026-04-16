package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.dto.PagedResponse;
import codes.anmol.distributedratelimiter.dto.RateLimitConfigResponse;
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

    public PagedResponse<RateLimitConfigResponse> findPaged(int page, int size, String algorithm, Boolean activeOnly) {
        List<RateLimitConfigEntity> allResponse = repository.findAll();
        if (algorithm != null && !algorithm.isBlank()) {
            allResponse = allResponse.stream()
                    .filter(e -> algorithm.equalsIgnoreCase(e.getAlgorithm()))
                    .toList();
        }
        if(activeOnly != null) {
            boolean active = activeOnly;
            allResponse = allResponse.stream()
                    .filter(e -> e.isActive() == active)
                    .toList();
        }
        long totalElements = allResponse.size();
        int totalPages = (int) Math.ceil((double) totalElements/size);
        int fromIndex = page*size;
        int toIndex = Math.min(fromIndex+size, (int) totalElements);
        List<RateLimitConfigResponse> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = List.of();
        } else {
            pageContent = allResponse.subList(fromIndex, toIndex)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return PagedResponse.<RateLimitConfigResponse>builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages-1)
                .build();
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

    public int deleteByAlgorithm(String algorithm) {
        List<RateLimitConfigEntity> matching = repository.findAll().stream()
                .filter(e -> algorithm.equalsIgnoreCase(e.getAlgorithm()))
                .toList();
        matching.forEach(e -> repository.deleteByIdentifier(e.getIdentifier()));
        return matching.size();
    }

    private RateLimitConfigResponse toResponse(RateLimitConfigEntity entity) {
        return RateLimitConfigResponse.builder()
                .identifier(entity.getIdentifier())
                .limit(entity.getLimit())
                .windowSeconds(entity.getWindowSeconds())
                .algorithm(entity.getAlgorithm())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
