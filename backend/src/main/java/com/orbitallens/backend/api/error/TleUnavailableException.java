package com.orbitallens.backend.api.error;

/**
 * Thrown when orbital element data cannot be retrieved from remote CelesTrak,
 * the local cache file, or the bundled fallback resource.
 */
public class TleUnavailableException extends RuntimeException {
    public TleUnavailableException(String message) {
        super(message);
    }

    public TleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
