package com.orbitallens.backend.orbit;

import com.orbitallens.backend.api.dto.PassPredictRequest;
import com.orbitallens.backend.api.dto.PassPredictResponse;
import com.orbitallens.backend.api.dto.PassResponseItem;
import com.orbitallens.backend.api.dto.PassSample;
import com.orbitallens.backend.tle.FixedTleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PassTest {

    private static final String START_INSTANT = "2026-10-21T18:24:06.576Z";
    private static final double MUMBAI_LAT = 19.076;
    private static final double MUMBAI_LON = 72.8777;

    private OrbitService orbitService;

    @BeforeEach
    void setUp() {
        FixedTleProvider tleProvider = new FixedTleProvider();
        orbitService = new OrbitService(tleProvider, 720, 1000);
    }

    @Test
    @DisplayName("Pass predictions with min elevation 10 degrees yields 3 passes with verified characteristics")
    void verifiesPassPredictionsMinElevation10() {
        PassPredictRequest request = new PassPredictRequest(
                "25544",
                MUMBAI_LAT,
                MUMBAI_LON,
                START_INSTANT,
                24,
                10.0
        );

        PassPredictResponse response = orbitService.predictPasses(request);

        assertEquals(3, response.passCount(), "Expected exactly 3 passes for min elevation 10");
        assertEquals(3, response.passes().size());

        PassResponseItem pass1 = response.passes().get(0);
        PassResponseItem pass2 = response.passes().get(1);
        PassResponseItem pass3 = response.passes().get(2);

        // First rise about 2026-10-21T19:43:46.576Z (tolerance 2 s)
        Instant expectedRise = Instant.parse("2026-10-21T19:43:46.576Z");
        Instant actualRise = Instant.parse(pass1.riseTime());
        long diffSeconds = Math.abs(Duration.between(expectedRise, actualRise).toSeconds());
        assertTrue(diffSeconds <= 2, "First rise time should be within 2s of 2026-10-21T19:43:46.576Z, got " + pass1.riseTime());

        // Max elevation regression checks
        assertEquals(26.23, pass1.maxElevationDeg(), 0.05, "Pass 1 max elevation mismatch");
        assertEquals(13.45, pass2.maxElevationDeg(), 0.05, "Pass 2 max elevation mismatch");
        assertEquals(34.37, pass3.maxElevationDeg(), 0.05, "Pass 3 max elevation mismatch");

        // Verify timing and ordering invariants for every pass
        for (PassResponseItem pass : response.passes()) {
            Instant rise = Instant.parse(pass.riseTime());
            Instant tca = Instant.parse(pass.maxElevationTime());
            Instant set = Instant.parse(pass.setTime());

            assertTrue(set.isAfter(rise), "setTime must be strictly after riseTime");
            assertTrue(!tca.isBefore(rise) && !tca.isAfter(set), "maxElevationTime must be between rise and set");
            assertFalse(pass.samples().isEmpty(), "Pass must have samples");

            // Verify samples are ordered by time
            Instant previousTime = null;
            for (PassSample sample : pass.samples()) {
                Instant sampleTime = Instant.parse(sample.time());
                if (previousTime != null) {
                    assertFalse(sampleTime.isBefore(previousTime), "Samples must be monotonically increasing in time");
                }
                previousTime = sampleTime;
            }
        }
    }

    @Test
    @DisplayName("Pass predictions with min elevation 0 yields exactly 5 passes (dropping out-of-window 6th pass)")
    void verifiesPassPredictionsMinElevation0() {
        PassPredictRequest request = new PassPredictRequest(
                "25544",
                MUMBAI_LAT,
                MUMBAI_LON,
                START_INSTANT,
                24,
                0.0
        );

        PassPredictResponse response = orbitService.predictPasses(request);
        assertEquals(5, response.passCount(), "Expected exactly 5 passes when min elevation is 0 (dropping pass beyond 24h)");
    }

    @Test
    @DisplayName("Pass predictions with min elevation 80 yields 0 passes without exceptions")
    void verifiesPassPredictionsMinElevation80() {
        PassPredictRequest request = new PassPredictRequest(
                "25544",
                MUMBAI_LAT,
                MUMBAI_LON,
                START_INSTANT,
                24,
                80.0
        );

        PassPredictResponse response = orbitService.predictPasses(request);
        assertEquals(0, response.passCount());
        assertTrue(response.passes().isEmpty());
        assertNotNull(response.message());
    }
}
