package com.orbitallens.backend.api.dto;

/**
 * Instantaneous geographic sub-satellite position and altitude.
 */
public record SatellitePositionResponse(
        String satelliteId,
        String name,
        String at,
        double latitudeDeg,
        double longitudeDeg,
        double altitudeKm,
        TleInfo tle
) {
}
