package com.orbitallens.backend.api.dto;

import java.util.List;

/**
 * Result of the pass prediction calculation.
 */
public record PassPredictResponse(
        String satelliteId,
        ObserverDto observer,
        String startTime,
        int windowHours,
        double minimumElevationDegrees,
        int passCount,
        List<PassResponseItem> passes,
        String note,
        TleInfo tle,
        String message
) {
}
