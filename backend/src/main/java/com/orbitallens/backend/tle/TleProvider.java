package com.orbitallens.backend.tle;

/**
 * Strategy interface providing the current TleSnapshot.
 * Allows switching between live CelesTrak fetching and deterministic test stubs.
 */
public interface TleProvider {
    TleSnapshot current();
}
