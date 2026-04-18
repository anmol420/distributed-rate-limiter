package codes.anmol.distributedratelimiter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title       = "Distributed Rate Limiter",
                version     = "1.0.0",
                description = "Production-grade distributed rate limiting service supporting "
                        + "Token Bucket, Fixed Window, and Sliding Window algorithms. "
                        + "Built with Spring Boot, Redis, and Docker. "
                        + "Handles 10,000+ req/min with sub-5ms overhead.",
                contact = @Contact(
                        name  = "GitHub",
                        url   = "https://github.com/anmol420/distributed-rate-limiter"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "http://localhost:8080", description = "Docker compose")
        }
)
public class DistributedRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedRateLimiterApplication.class, args);
    }

}
