package com.orbitallens.backend.api.dto;

/**
 * Ground observer geographic coordinates.
 */
public record ObserverDto(
        double latitude,
        double longitude
) {
}
