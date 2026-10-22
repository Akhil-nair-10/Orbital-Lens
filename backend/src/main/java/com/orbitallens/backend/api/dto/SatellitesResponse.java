package com.orbitallens.backend.api.dto;

import java.util.List;

/**
 * Response listing all supported satellites.
 */
public record SatellitesResponse(
        List<SatelliteSummary> satellites
) {
}
