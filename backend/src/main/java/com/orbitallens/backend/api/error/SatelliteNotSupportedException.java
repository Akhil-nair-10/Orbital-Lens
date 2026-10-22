package com.orbitallens.backend.api.error;

/**
 * Thrown when an unsupported satellite catalog ID is requested.
 * In v1, only the International Space Station (NORAD 25544) is supported.
 */
public class SatelliteNotSupportedException extends RuntimeException {
    public SatelliteNotSupportedException(String satelliteId) {
        super("Only the ISS (25544) is supported in v1. Requested: " + satelliteId);
    }

    public SatelliteNotSupportedException() {
        super("Only the ISS (25544) is supported in v1.");
    }
}
