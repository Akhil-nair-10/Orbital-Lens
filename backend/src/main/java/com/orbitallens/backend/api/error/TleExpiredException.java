package com.orbitallens.backend.api.error;

/**
 * Thrown when the current TLE epoch is older than the configured expiration threshold.
 * Propagating expired orbital elements produces inaccurate results, so requests are rejected.
 */
public class TleExpiredException extends RuntimeException {
    private final double ageHours;

    public TleExpiredException(double ageHours) {
        super(String.format("The orbit data is too old for reliable results (age: %.1f hours). Updates have expired.", ageHours));
        this.ageHours = ageHours;
    }

    public double getAgeHours() {
        return ageHours;
    }
}
