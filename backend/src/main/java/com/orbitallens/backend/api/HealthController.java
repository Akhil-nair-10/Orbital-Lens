package com.orbitallens.backend.api;

import com.orbitallens.backend.api.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Endpoint for uptime and health monitoring.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse checkHealth() {
        return new HealthResponse("UP", Instant.now().toString());
    }
}
