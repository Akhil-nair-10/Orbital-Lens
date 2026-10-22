package com.orbitallens.backend.api.dto;

/**
 * An instantaneous sample taken during an orbital pass.
 */
public record PassSample(
        String time,
        double elevationDeg,
        double azimuthDeg,
        double latitudeDeg,
        double longitudeDeg
) {
}
