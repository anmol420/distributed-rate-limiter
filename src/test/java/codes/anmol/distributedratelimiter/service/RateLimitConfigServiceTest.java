package codes.anmol.distributedratelimiter.service;

import codes.anmol.distributedratelimiter.model.RateLimitConfigEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class RateLimitConfigServiceTest {

    @Autowired
    private RateLimitConfigService rateLimitConfigService;

    private final String PREFIX = "test-config:" + System.currentTimeMillis() + ":";

    @Test
    @DisplayName("Create config")
    void createConfig() {
        RateLimitConfigEntity entity = RateLimitConfigEntity.builder()
                .identifier(PREFIX + "user:abc")
                .limit(40)
                .windowSeconds(120)
                .algorithm("slidingWindow")
                .build();
        RateLimitConfigEntity savedEntity = rateLimitConfigService.create(entity);
        assertThat(savedEntity.getIdentifier()).isEqualTo(PREFIX + "user:abc");
        assertThat(savedEntity.getLimit()).isEqualTo(40);
        assertThat(savedEntity.isActive()).isTrue();
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Create duplicate identifier throws IllegalArgumentException")
    void createDuplicateThrows() {
        String identifier = PREFIX + "user:xyz";
        RateLimitConfigEntity entity = RateLimitConfigEntity.builder()
                .identifier(identifier).limit(20).windowSeconds(100).algorithm("fixedWindow").build();
        rateLimitConfigService.create(entity);
        assertThatThrownBy(() -> rateLimitConfigService.create(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("findByIdentifier returns correct entity")
    void findByIdentifierReturnsCorrectEntity() {
        String identifier = PREFIX + "user:zyx";
        rateLimitConfigService.create(RateLimitConfigEntity.builder()
                .identifier(identifier).limit(40).windowSeconds(60).algorithm("fixedWindow").build());
        Optional<RateLimitConfigEntity> exists = rateLimitConfigService.findByIdentifier(identifier);
        assertThat(exists).isPresent();
        assertThat(exists.get().getLimit()).isEqualTo(40);
        assertThat(exists.get().getAlgorithm()).isEqualTo("fixedWindow");
    }

    @Test
    @DisplayName("Find non-existing identifier returns empty")
    void findNonExistingIdentifierReturnsEmpty() {
        Optional<RateLimitConfigEntity> res = rateLimitConfigService.findByIdentifier(PREFIX + "ugh");
        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("Update config changes fields and updatedAt")
    void updateConfigChangesFieldsAndUpdateAt() throws InterruptedException {
        String identifier = PREFIX + "user:kek";
        rateLimitConfigService.create(RateLimitConfigEntity.builder()
                .identifier(identifier).limit(10).windowSeconds(60).algorithm("slidingWindow").build());
        Thread.sleep(5000);
        RateLimitConfigEntity updatedEntity = rateLimitConfigService.update(
                identifier,
                RateLimitConfigEntity.builder()
                        .identifier(identifier)
                        .limit(20)
                        .windowSeconds(40)
                        .algorithm("tokenBucket")
                        .build()
        );
        assertThat(updatedEntity.getLimit()).isEqualTo(20);
        assertThat(updatedEntity.getWindowSeconds()).isEqualTo(40);
        assertThat(updatedEntity.getAlgorithm()).isEqualTo("tokenBucket");
        assertThat(updatedEntity.getUpdatedAt()).isAfter(updatedEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Delete config")
    void deleteConfig() {
        String identifier = PREFIX + "user:mno";
        rateLimitConfigService.create(RateLimitConfigEntity.builder()
                .identifier(identifier).limit(10).windowSeconds(60).algorithm("slidingWindow").build());
        boolean entityDeleted = rateLimitConfigService.delete(identifier);
        assertThat(entityDeleted).isTrue();
        assertThat(rateLimitConfigService.findByIdentifier(identifier)).isEmpty();
    }

    @Test
    @DisplayName("Toggle works for active")
    void toggleWorksForActive() {
        String identifier = PREFIX + "user:klm";
        rateLimitConfigService.create(RateLimitConfigEntity.builder()
                .identifier(identifier).limit(10).windowSeconds(60).algorithm("slidingWindow").build());
        //noinspection OptionalGetWithoutIsPresent
        assertThat(rateLimitConfigService.findByIdentifier(identifier).get().isActive()).isTrue();

        rateLimitConfigService.toggleActive(identifier);
        //noinspection OptionalGetWithoutIsPresent
        assertThat(rateLimitConfigService.findByIdentifier(identifier).get().isActive()).isFalse();

        rateLimitConfigService.toggleActive(identifier);
        //noinspection OptionalGetWithoutIsPresent
        assertThat(rateLimitConfigService.findByIdentifier(identifier).get().isActive()).isTrue();
    }
}
