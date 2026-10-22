package com.orbitallens.backend.tle;

import java.time.Instant;

/**
 * Immutable snapshot of the orbital elements for the ISS.
 *
 * @param lines The 3 raw TLE lines (line 0: satellite name, line 1: TLE line 1, line 2: TLE line 2)
 * @param name The satellite name (e.g., "ISS (ZARYA)")
 * @param epoch The timestamp when these orbital elements were measured/published
 * @param retrievedAt The instant when OrbitalLens downloaded/cached this data (null for bundled sample)
 * @param source Origin of this snapshot (CELESTRAK, CACHE_FILE, or BUNDLED_SAMPLE)
 */
public record TleSnapshot(
        String[] lines,
        String name,
        Instant epoch,
        Instant retrievedAt,
        TleSource source
) {
}
