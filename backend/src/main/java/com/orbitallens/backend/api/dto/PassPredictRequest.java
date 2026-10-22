package com.orbitallens.backend.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for predicting satellite passes over an observer location.
 */
public record PassPredictRequest(
        String satelliteId,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,

        String startTime,

        @Min(value = 1, message = "windowHours must be at least 1")
        @Max(value = 48, message = "windowHours cannot exceed 48")
        Integer windowHours,

        @DecimalMin(value = "0.0", message = "minimumElevationDegrees must be at least 0")
        @DecimalMax(value = "80.0", message = "minimumElevationDegrees cannot exceed 80")
        Double minimumElevationDegrees
) {
    public PassPredictRequest {
        if (satelliteId == null || satelliteId.isBlank()) {
            satelliteId = "25544";
        }
        if (windowHours == null) {
            windowHours = 24;
        }
        if (minimumElevationDegrees == null) {
            minimumElevationDegrees = 10.0;
        }
    }
}
