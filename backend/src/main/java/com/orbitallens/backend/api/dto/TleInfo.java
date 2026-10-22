package com.orbitallens.backend.api.dto;

import com.orbitallens.backend.tle.TleFreshness;
import com.orbitallens.backend.tle.TleSource;

/**
 * Metadata about the orbital elements used to compute satellite positions and passes.
 */
public record TleInfo(
        TleSource source,
        String retrievedAt,
        String epoch,
        double ageHours,
        TleFreshness status,
        String message
) {
}
