package com.orbitallens.backend.tle;

import com.orbitallens.backend.orbit.OrbitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreshnessTest {

    private static final long STALE_HOURS = 72;
    private static final long EXPIRED_HOURS = 336;

    @Test
    @DisplayName("Ages just under and over 72h and 336h map to FRESH, STALE, and EXPIRED")
    void verifiesFreshnessBoundaries() {
        Instant now = Instant.parse("2026-10-22T12:00:00Z");

        // 71.9 hours old -> FRESH
        Instant age71h54m = now.minus(71, ChronoUnit.HOURS).minus(54, ChronoUnit.MINUTES);
        assertEquals(TleFreshness.FRESH,
                OrbitService.evaluateFreshness(age71h54m, now, STALE_HOURS, EXPIRED_HOURS));

        // Exactly 72 hours old -> STALE
        Instant age72h = now.minus(72, ChronoUnit.HOURS);
        assertEquals(TleFreshness.STALE,
                OrbitService.evaluateFreshness(age72h, now, STALE_HOURS, EXPIRED_HOURS));

        // 72.1 hours old -> STALE
        Instant age72h6m = now.minus(72, ChronoUnit.HOURS).minus(6, ChronoUnit.MINUTES);
        assertEquals(TleFreshness.STALE,
                OrbitService.evaluateFreshness(age72h6m, now, STALE_HOURS, EXPIRED_HOURS));

        // 335.9 hours old -> STALE
        Instant age335h54m = now.minus(335, ChronoUnit.HOURS).minus(54, ChronoUnit.MINUTES);
        assertEquals(TleFreshness.STALE,
                OrbitService.evaluateFreshness(age335h54m, now, STALE_HOURS, EXPIRED_HOURS));

        // Exactly 336 hours old -> STALE
        Instant age336h = now.minus(336, ChronoUnit.HOURS);
        assertEquals(TleFreshness.STALE,
                OrbitService.evaluateFreshness(age336h, now, STALE_HOURS, EXPIRED_HOURS));

        // 336.1 hours old -> EXPIRED
        Instant age336h6m = now.minus(336, ChronoUnit.HOURS).minus(6, ChronoUnit.MINUTES);
        assertEquals(TleFreshness.EXPIRED,
                OrbitService.evaluateFreshness(age336h6m, now, STALE_HOURS, EXPIRED_HOURS));
    }
}
