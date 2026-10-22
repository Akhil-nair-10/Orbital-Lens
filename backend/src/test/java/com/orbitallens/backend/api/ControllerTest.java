package com.orbitallens.backend.api;

import com.orbitallens.backend.api.error.GlobalExceptionHandler;
import com.orbitallens.backend.orbit.OrbitService;
import com.orbitallens.backend.tle.FixedTleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControllerTest {

    private MockMvc mockMvc;
    private OrbitService normalOrbitService;
    private OrbitService expiredOrbitService;

    @BeforeEach
    void setUp() {
        FixedTleProvider normalProvider = new FixedTleProvider();
        normalOrbitService = new OrbitService(normalProvider, 720, 1000);

        // Expired service configured with zero thresholds so any age triggers EXPIRED
        FixedTleProvider expiredProvider = FixedTleProvider.withExpiredEpoch(Instant.now());
        expiredOrbitService = new OrbitService(expiredProvider, 1, 2);

        SatelliteController satelliteController = new SatelliteController(normalOrbitService);
        PassController passController = new PassController(normalOrbitService);
        HealthController healthController = new HealthController();
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        mockMvc = MockMvcBuilders
                .standaloneSetup(satelliteController, passController, healthController)
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    @DisplayName("latitude > 90 fails validation with HTTP 400")
    void rejectsLatitudeGreaterThan90() throws Exception {
        String json = """
                {
                    "latitude": 91.0,
                    "longitude": 72.8777
                }
                """;
        mockMvc.perform(post("/api/passes/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("windowHours = 0 fails validation with HTTP 400")
    void rejectsWindowHoursZero() throws Exception {
        String json = """
                {
                    "latitude": 19.076,
                    "longitude": 72.8777,
                    "windowHours": 0
                }
                """;
        mockMvc.perform(post("/api/passes/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Missing longitude fails validation with HTTP 400")
    void rejectsMissingLongitude() throws Exception {
        String json = """
                {
                    "latitude": 19.076
                }
                """;
        mockMvc.perform(post("/api/passes/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Unknown satellite ID returns HTTP 404 with SATELLITE_NOT_SUPPORTED")
    void returns404ForUnknownSatellite() throws Exception {
        mockMvc.perform(get("/api/satellites/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("SATELLITE_NOT_SUPPORTED")))
                .andExpect(jsonPath("$.message", containsString("Only the ISS (25544) is supported in v1.")));
    }

    @Test
    @DisplayName("startTime with timezone offset +05:30 is correctly parsed and returned as UTC ending in Z")
    void handlesTimezoneOffsetsCorrectly() throws Exception {
        String json = """
                {
                    "latitude": 19.076,
                    "longitude": 72.8777,
                    "startTime": "2026-10-22T00:00:00+05:30"
                }
                """;
        mockMvc.perform(post("/api/passes/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime", is("2026-10-21T18:30:00Z")));
    }

    @Test
    @DisplayName("Response timestamps always end with Z")
    void responseTimestampsEndWithZ() throws Exception {
        mockMvc.perform(get("/api/satellites/25544/position")
                        .param("at", "2026-10-21T18:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.at", endsWith("Z")))
                .andExpect(jsonPath("$.tle.epoch", endsWith("Z")));
    }

    @Test
    @DisplayName("EXPIRED snapshot returns HTTP 503 TLE_EXPIRED")
    void returns503ForExpiredTle() throws Exception {
        SatelliteController expiredSatController = new SatelliteController(expiredOrbitService);
        PassController expiredPassController = new PassController(expiredOrbitService);
        MockMvc expiredMvc = MockMvcBuilders
                .standaloneSetup(expiredSatController, expiredPassController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        expiredMvc.perform(get("/api/satellites/25544/position"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", is("TLE_EXPIRED")));
    }
}
