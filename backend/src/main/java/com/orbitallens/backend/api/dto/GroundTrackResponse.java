package com.orbitallens.backend.api.dto;

import java.util.List;

/**
 * Propagated past and future ground track coordinates.
 */
public record GroundTrackResponse(
        String satelliteId,
        String generatedAt,
        int stepSeconds,
        List<GroundTrackPoint> past,
        List<GroundTrackPoint> future,
        TleInfo tle
) {
}
