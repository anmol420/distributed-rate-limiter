package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.dto.RateLimitConfigRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitConfigResponse;
import codes.anmol.distributedratelimiter.model.RateLimitConfigEntity;
import codes.anmol.distributedratelimiter.service.RateLimitConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/config")
public class RateLimitConfigController {

    private final RateLimitConfigService configService;

    public RateLimitConfigController(RateLimitConfigService configService) {
        this.configService = configService;
    }

    @PostMapping
    public ResponseEntity<RateLimitConfigResponse> create(
            @Valid
            @RequestBody
            RateLimitConfigRequest request
    ) {
        RateLimitConfigEntity entity = toEntity(request);
        RateLimitConfigEntity saved = configService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<RateLimitConfigResponse>> listAll() {
        List<RateLimitConfigResponse> configResponses = configService.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(configResponses);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<RateLimitConfigResponse> getByIdentifier(
            @PathVariable
            String identifier
    ) {
        return configService.findByIdentifier(identifier)
                .map(e -> ResponseEntity.ok(toResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<RateLimitConfigResponse> update(
            @PathVariable
            String identifier,

            @Valid
            @RequestBody
            RateLimitConfigRequest request
    ) {
        RateLimitConfigEntity updated = configService.update(identifier, toEntity(request));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable
            String identifier
    ) {
        boolean deleted = configService.delete(identifier);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "message", "Config deleted for identifier: " + identifier
        ));
    }

    @PatchMapping("/{identifier}/toggle")
    public ResponseEntity<RateLimitConfigResponse> toggle(
            @PathVariable
            String identifier
    ) {
        RateLimitConfigEntity toggle = configService.toggleActive(identifier);
        return ResponseEntity.ok(toResponse(toggle));
    }

    private RateLimitConfigEntity toEntity(RateLimitConfigRequest request) {
        return RateLimitConfigEntity.builder()
                .identifier(request.getIdentifier())
                .limit(request.getLimit())
                .windowSeconds(request.getWindowSeconds())
                .algorithm(request.getAlgorithm())
                .build();
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
