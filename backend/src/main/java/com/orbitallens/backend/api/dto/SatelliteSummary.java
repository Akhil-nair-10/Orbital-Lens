package com.orbitallens.backend.api.dto;

/**
 * Summary information for a single satellite.
 */
public record SatelliteSummary(
        String id,
        String name,
        TleInfo tle
) {
}
