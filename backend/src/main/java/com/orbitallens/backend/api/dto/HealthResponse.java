package com.orbitallens.backend.api.dto;

/**
 * Health check response payload.
 */
public record HealthResponse(
        String status,
        String time
) {
}
