package codes.anmol.distributedratelimiter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/")
@RequiredArgsConstructor
public class RateLimiterController {

    @GetMapping(path="/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.status(HttpStatus.OK).body("OK");
    }
}
