package com.orbitallens.backend.api;

import com.orbitallens.backend.api.dto.PassPredictRequest;
import com.orbitallens.backend.api.dto.PassPredictResponse;
import com.orbitallens.backend.orbit.OrbitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for calculating upcoming visible passes over an observer position.
 */
@RestController
@RequestMapping("/api/passes")
public class PassController {

    private final OrbitService orbitService;

    public PassController(OrbitService orbitService) {
        this.orbitService = orbitService;
    }

    @PostMapping("/predict")
    public PassPredictResponse predict(@Valid @RequestBody PassPredictRequest request) {
        return orbitService.predictPasses(request);
    }
}
