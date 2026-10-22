package com.orbitallens.backend.api.error;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when client query parameters, timestamps, or ranges violate business rules.
 */
public class BadRequestException extends RuntimeException {
    private final List<String> details;

    public BadRequestException(String message) {
        super(message);
        this.details = Collections.emptyList();
    }

    public BadRequestException(String message, List<String> details) {
        super(message);
        this.details = details != null ? details : Collections.emptyList();
    }

    public List<String> getDetails() {
        return details;
    }
}
