package com.orbitallens.backend.api.dto;

/**
 * A single coordinate point along the satellite ground track.
 */
public record GroundTrackPoint(
        String time,
        double latitudeDeg,
        double longitudeDeg
) {
}
