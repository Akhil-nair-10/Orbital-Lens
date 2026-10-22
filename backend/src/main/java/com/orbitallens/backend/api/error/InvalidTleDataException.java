package com.orbitallens.backend.api.error;

/**
 * Thrown when raw TLE data fails validation checks (invalid length, bad checksum,
 * incorrect line identifiers, or non-ISS catalog number).
 */
public class InvalidTleDataException extends RuntimeException {
    public InvalidTleDataException(String message) {
        super(message);
    }

    public InvalidTleDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
