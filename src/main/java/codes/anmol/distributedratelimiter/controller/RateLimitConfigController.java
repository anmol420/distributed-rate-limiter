package codes.anmol.distributedratelimiter.controller;

import codes.anmol.distributedratelimiter.dto.PagedResponse;
import codes.anmol.distributedratelimiter.dto.RateLimitConfigRequest;
import codes.anmol.distributedratelimiter.dto.RateLimitConfigResponse;
import codes.anmol.distributedratelimiter.model.RateLimitConfigEntity;
import codes.anmol.distributedratelimiter.service.RateLimitConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration", description = "Per-user and per-IP rate limit configuration")
public class RateLimitConfigController {

    private final RateLimitConfigService configService;

    public RateLimitConfigController(RateLimitConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "Create a new rate limit rule",
            description = "Creates a per-user or per-IP rate limit config stored in Redis.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Config created"),
            @ApiResponse(responseCode = "400", description = "Duplicate identifier or validation error")
    })
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

    @Operation(
            summary     = "List all configs (paginated)",
            description = "Supports filtering by algorithm and active status. "
                    + "page is 0-based. size max is 100."
    )
    @GetMapping
    public ResponseEntity<PagedResponse<RateLimitConfigResponse>> listAll(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Page size (max 100)", example = "10")
            @RequestParam(defaultValue = "100")
            int size,

            @Parameter(description = "Filter by algorithm", example = "tokenBucket")
            @RequestParam(required = false)
            String algorithm,

            @Parameter(description = "Filter active/inactive", example = "true")
            @RequestParam(required = false)
            Boolean activeOnly
    ) {
        size = Math.clamp(size, 1, 100);
        PagedResponse<RateLimitConfigResponse> configResponses = configService.findPaged(page, size, algorithm, activeOnly);
        return ResponseEntity.ok(configResponses);
    }

    @Operation(summary = "Get a single config by identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Config found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<RateLimitConfigResponse> getByIdentifier(
            @Parameter(description = "Identifier", example = "user:xyz")
            @PathVariable
            String identifier
    ) {
        return configService.findByIdentifier(identifier)
                .map(e -> ResponseEntity.ok(toResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update an existing config")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Not found or validation error")
    })
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

    @Operation(summary = "Delete a config")
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

    @Operation(summary = "Toggle a config active/inactive",
            description = "Flips the active flag without deleting the config.")
    @PatchMapping("/{identifier}/toggle")
    public ResponseEntity<RateLimitConfigResponse> toggle(
            @PathVariable
            String identifier
    ) {
        RateLimitConfigEntity toggle = configService.toggleActive(identifier);
        return ResponseEntity.ok(toResponse(toggle));
    }

    @Operation(summary = "Bulk delete configs by algorithm")
    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Object>> deleteBulk(
            @Parameter(description = "Algorithm to target", example = "slidingWindow")
            @RequestParam
            String algorithm
    ) {
        int count = configService.deleteByAlgorithm(algorithm);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk delete complete",
                "algorithm", algorithm,
                "deletedCount", count
        ));
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
