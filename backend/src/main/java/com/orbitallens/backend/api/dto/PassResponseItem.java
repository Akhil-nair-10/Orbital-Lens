package com.orbitallens.backend.api.dto;

import java.util.List;

/**
 * Details of an individual satellite pass over an observer's horizon.
 */
public record PassResponseItem(
        String riseTime,
        String maxElevationTime,
        String setTime,
        long durationSeconds,
        double maxElevationDeg,
        double riseAzimuthDeg,
        double setAzimuthDeg,
        List<PassSample> samples
) {
}
