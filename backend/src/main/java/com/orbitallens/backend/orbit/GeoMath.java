package com.orbitallens.backend.orbit;

/**
 * Geometric and geographic calculation utilities for satellite orbital positions.
 */
public final class GeoMath {

    private static final String[] COMPASS_POINTS = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    };

    private GeoMath() {
        // Utility class
    }

    /**
     * Converts radians to degrees.
     */
    public static double toDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * Normalizes longitude in degrees to the standard [-180.0, 180.0] range.
     */
    public static double normalizeLongitude(double lonDeg) {
        double lon = ((lonDeg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        // Keep 180.0 as positive 180.0 if the input was positive
        if (lon == -180.0 && lonDeg > 0) {
            return 180.0;
        }
        return lon;
    }

    /**
     * Normalizes an angle in degrees into [0.0, 360.0), typically used for azimuth.
     */
    public static double normalizeDegrees(double degrees) {
        return ((degrees % 360.0) + 360.0) % 360.0;
    }

    /**
     * Converts an azimuth angle in degrees (0-360) into a 16-point compass direction.
     * E.g., 0 -> "N", 53 -> "NE", 197 -> "SSW".
     */
    public static String toCompassDirection(double azimuthDegrees) {
        double normalized = normalizeDegrees(azimuthDegrees);
        int index = (int) Math.floor((normalized + 11.25) / 22.5) % 16;
        return COMPASS_POINTS[index];
    }
}
