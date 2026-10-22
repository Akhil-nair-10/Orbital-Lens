package com.orbitallens.backend.api.error;

import java.util.List;

/**
 * Standard error response envelope returned by all error handlers.
 *
 * @param error Machine-readable error code (e.g., BAD_REQUEST, SATELLITE_NOT_SUPPORTED)
 * @param message Human-readable summary of the problem
 * @param details Additional details or field validation failures
 * @param timestamp ISO-8601 UTC timestamp ending in Z
 */
public record ErrorResponse(
        String error,
        String message,
        List<String> details,
        String timestamp
) {
}
