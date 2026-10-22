package com.orbitallens.backend.orbit;

import com.orbitallens.backend.api.dto.GroundTrackPoint;
import com.orbitallens.backend.api.dto.GroundTrackResponse;
import com.orbitallens.backend.api.dto.SatellitePositionResponse;
import com.orbitallens.backend.tle.FixedTleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

// Regression values in this test are from our own manual run, NOT independent validation.
class OrbitServiceTest {

    private OrbitService orbitService;

    @BeforeEach
    void setUp() {
        FixedTleProvider tleProvider = new FixedTleProvider();
        // High thresholds ensure test snapshot is treated as FRESH
        orbitService = new OrbitService(tleProvider, 720, 1000);
    }

    @Test
    @DisplayName("Position regression test at instant 2026-10-21T18:24:06.576Z")
    void verifiesPositionRegressionValues() {
        // Instant: 2026-10-21T18:24:06.576Z
        String testInstant = "2026-10-21T18:24:06.576Z";
        SatellitePositionResponse pos = orbitService.getPosition("25544", testInstant);

        // latitudeDeg about 37.774 (tolerance 0.01)
        assertEquals(37.774, pos.latitudeDeg(), 0.01, "Latitude regression mismatch");

        // longitudeDeg about 125.062 (tolerance 0.01)
        assertEquals(125.062, pos.longitudeDeg(), 0.01, "Longitude regression mismatch");

        // altitudeKm about 424.89 (tolerance 0.1)
        assertEquals(424.89, pos.altitudeKm(), 0.1, "Altitude regression mismatch");

        // General orbital invariants
        assertTrue(Math.abs(pos.latitudeDeg()) <= 51.9, "ISS inclination is 51.6 degrees, latitude cannot exceed 51.9");
        assertTrue(pos.altitudeKm() >= 380.0 && pos.altitudeKm() <= 450.0, "ISS altitude should be between 380 and 450 km");
        assertTrue(pos.longitudeDeg() >= -180.0 && pos.longitudeDeg() <= 180.0, "Longitude must be normalized to [-180, 180]");
    }

    @Test
    @DisplayName("Ground track satisfies physical orbit invariants")
    void verifiesGroundTrackInvariants() {
        GroundTrackResponse track = orbitService.getGroundTrack("25544", 45, 90, 60, Instant.parse("2026-10-21T18:24:06.576Z"));

        assertNotNull(track);
        assertFalse(track.past().isEmpty());
        assertFalse(track.future().isEmpty());

        for (GroundTrackPoint pt : track.past()) {
            assertTrue(Math.abs(pt.latitudeDeg()) <= 51.9,
                    "Past ground track latitude exceeded 51.9: " + pt.latitudeDeg());
            assertTrue(pt.longitudeDeg() >= -180.0 && pt.longitudeDeg() <= 180.0,
                    "Past ground track longitude out of bounds: " + pt.longitudeDeg());
        }

        for (GroundTrackPoint pt : track.future()) {
            assertTrue(Math.abs(pt.latitudeDeg()) <= 51.9,
                    "Future ground track latitude exceeded 51.9: " + pt.latitudeDeg());
            assertTrue(pt.longitudeDeg() >= -180.0 && pt.longitudeDeg() <= 180.0,
                    "Future ground track longitude out of bounds: " + pt.longitudeDeg());
        }
    }
}
