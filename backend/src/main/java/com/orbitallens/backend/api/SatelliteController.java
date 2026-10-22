package com.orbitallens.backend.api;

import com.orbitallens.backend.api.dto.GroundTrackResponse;
import com.orbitallens.backend.api.dto.SatellitePositionResponse;
import com.orbitallens.backend.api.dto.SatelliteSummary;
import com.orbitallens.backend.api.dto.SatellitesResponse;
import com.orbitallens.backend.orbit.OrbitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for satellite metadata, real-time position, and ground track.
 */
@RestController
@RequestMapping("/api/satellites")
public class SatelliteController {

    private final OrbitService orbitService;

    public SatelliteController(OrbitService orbitService) {
        this.orbitService = orbitService;
    }

    @GetMapping
    public SatellitesResponse getSatellites() {
        return orbitService.getAllSatellites();
    }

    @GetMapping("/{id}")
    public SatelliteSummary getSatellite(@PathVariable String id) {
        return orbitService.getSatellite(id);
    }

    @GetMapping("/{id}/position")
    public SatellitePositionResponse getPosition(
            @PathVariable String id,
            @RequestParam(required = false) String at
    ) {
        return orbitService.getPosition(id, at);
    }

    @GetMapping("/{id}/ground-track")
    public GroundTrackResponse getGroundTrack(
            @PathVariable String id,
            @RequestParam(required = false) Integer pastMinutes,
            @RequestParam(required = false) Integer futureMinutes,
            @RequestParam(required = false) Integer stepSeconds
    ) {
        return orbitService.getGroundTrack(id, pastMinutes, futureMinutes, stepSeconds);
    }
}
