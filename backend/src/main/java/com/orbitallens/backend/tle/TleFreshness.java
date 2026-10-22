package com.orbitallens.backend.tle;

/**
 * Indicates how fresh the orbital element set (TLE) is relative to the current time.
 * SGP4 predictions degrade as TLEs age due to atmospheric drag.
 */
public enum TleFreshness {
    FRESH,
    STALE,
    EXPIRED
}
