package com.orbitallens.backend.orbit;

import com.orbitallens.backend.api.dto.GroundTrackPoint;
import com.orbitallens.backend.api.dto.GroundTrackResponse;
import com.orbitallens.backend.api.dto.ObserverDto;
import com.orbitallens.backend.api.dto.PassPredictRequest;
import com.orbitallens.backend.api.dto.PassPredictResponse;
import com.orbitallens.backend.api.dto.PassResponseItem;
import com.orbitallens.backend.api.dto.PassSample;
import com.orbitallens.backend.api.dto.SatellitePositionResponse;
import com.orbitallens.backend.api.dto.SatelliteSummary;
import com.orbitallens.backend.api.dto.SatellitesResponse;
import com.orbitallens.backend.api.dto.TleInfo;
import com.orbitallens.backend.api.error.BadRequestException;
import com.orbitallens.backend.api.error.SatelliteNotSupportedException;
import com.orbitallens.backend.api.error.TleExpiredException;
import com.orbitallens.backend.tle.TleFreshness;
import com.orbitallens.backend.tle.TleProvider;
import com.orbitallens.backend.tle.TleSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.PassPredictor;
import uk.me.g4dpz.satellite.SatPassTime;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Core business service for SGP4 orbital propagation, ground track generation,
 * and observer pass predictions using predict4java.
 */
@Service
public class OrbitService {

    public static final String ISS_CATALOG_ID = "25544";

    private final TleProvider tleProvider;
    private final long staleAfterHours;
    private final long expiredAfterHours;

    public OrbitService(
            TleProvider tleProvider,
            @Value("${orbitallens.tle.stale-after-hours:72}") long staleAfterHours,
            @Value("${orbitallens.tle.expired-after-hours:336}") long expiredAfterHours
    ) {
        this.tleProvider = tleProvider;
        this.staleAfterHours = staleAfterHours;
        this.expiredAfterHours = expiredAfterHours;
    }

    /**
     * Determines the freshness tier based on elapsed hours between epoch and evaluation time.
     */
    public static TleFreshness evaluateFreshness(Instant epoch, Instant now, long staleHours, long expiredHours) {
        double ageHours = (double) Duration.between(epoch, now).toMillis() / 3_600_000.0;
        if (ageHours < staleHours) {
            return TleFreshness.FRESH;
        } else if (ageHours <= expiredHours) {
            return TleFreshness.STALE;
        } else {
            return TleFreshness.EXPIRED;
        }
    }

    public TleInfo buildTleInfo(TleSnapshot snapshot, Instant evaluationTime) {
        Instant now = evaluationTime != null ? evaluationTime : Instant.now();
        double ageHours = (double) Duration.between(snapshot.epoch(), now).toMillis() / 3_600_000.0;
        if (ageHours < 0) {
            ageHours = 0.0;
        }
        double roundedAge = Math.round(ageHours * 10.0) / 10.0;

        TleFreshness status = evaluateFreshness(snapshot.epoch(), now, staleAfterHours, expiredAfterHours);
        String message;
        switch (status) {
            case FRESH -> message = "TLE data is up to date.";
            case STALE -> message = String.format(
                    "TLE data is %.1f hours old (older than %d h). Ground track and passes may have reduced accuracy.",
                    roundedAge, staleAfterHours);
            case EXPIRED -> message = String.format(
                    "The orbit data is too old for reliable results (age: %.1f hours). Updates have expired.",
                    roundedAge);
            default -> message = "TLE data status: " + status;
        }

        String retrievedAtStr = snapshot.retrievedAt() != null ? snapshot.retrievedAt().toString() : null;
        return new TleInfo(snapshot.source(), retrievedAtStr, snapshot.epoch().toString(), roundedAge, status, message);
    }

    public SatellitesResponse getAllSatellites() {
        return new SatellitesResponse(List.of(getSatellite(ISS_CATALOG_ID)));
    }

    public SatelliteSummary getSatellite(String satelliteId) {
        validateSatelliteId(satelliteId);
        TleSnapshot snapshot = tleProvider.current();
        TleInfo tleInfo = buildTleInfo(snapshot, Instant.now());
        return new SatelliteSummary(ISS_CATALOG_ID, snapshot.name(), tleInfo);
    }

    public SatellitePositionResponse getPosition(String satelliteId, String atString) {
        validateSatelliteId(satelliteId);

        Instant atInstant = parseInstantOrNow(atString);
        TleSnapshot snapshot = tleProvider.current();
        TleInfo tleInfo = buildTleInfo(snapshot, atInstant);

        // When EXPIRED, reject calculation with 503
        if (tleInfo.status() == TleFreshness.EXPIRED) {
            throw new TleExpiredException(tleInfo.ageHours());
        }

        try {
            // A fresh Satellite instance is created per call as it is lightweight and not thread-safe
            TLE tle = new TLE(snapshot.lines());
            Satellite sat = SatelliteFactory.createSatellite(tle);

            // Ground station (0,0,0) allows querying the sub-satellite point directly
            SatPos pos = sat.getPosition(new GroundStationPosition(0.0, 0.0, 0.0), Date.from(atInstant));

            double latDeg = GeoMath.toDegrees(pos.getLatitude());
            double lonDeg = GeoMath.normalizeLongitude(GeoMath.toDegrees(pos.getLongitude()));
            double altKm = pos.getAltitude();

            return new SatellitePositionResponse(
                    ISS_CATALOG_ID,
                    snapshot.name(),
                    atInstant.toString(),
                    latDeg,
                    lonDeg,
                    altKm,
                    tleInfo
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to propagate satellite position: " + e.getMessage(), e);
        }
    }

    public GroundTrackResponse getGroundTrack(String satelliteId, Integer pastMinutes, Integer futureMinutes, Integer stepSeconds) {
        return getGroundTrack(satelliteId, pastMinutes, futureMinutes, stepSeconds, Instant.now());
    }

    public GroundTrackResponse getGroundTrack(String satelliteId, Integer pastMinutes, Integer futureMinutes, Integer stepSeconds, Instant referenceTime) {
        validateSatelliteId(satelliteId);

        int pastM = pastMinutes != null ? pastMinutes : 45;
        int futureM = futureMinutes != null ? futureMinutes : 90;
        int stepS = stepSeconds != null ? stepSeconds : 60;

        if (pastM < 0 || pastM > 180) {
            throw new BadRequestException("pastMinutes must be between 0 and 180 (received " + pastM + ")");
        }
        if (futureM < 0 || futureM > 360) {
            throw new BadRequestException("futureMinutes must be between 0 and 360 (received " + futureM + ")");
        }
        if (stepS < 10 || stepS > 300) {
            throw new BadRequestException("stepSeconds must be between 10 and 300 (received " + stepS + ")");
        }

        Instant now = referenceTime != null ? referenceTime : Instant.now();
        TleSnapshot snapshot = tleProvider.current();
        TleInfo tleInfo = buildTleInfo(snapshot, now);

        if (tleInfo.status() == TleFreshness.EXPIRED) {
            throw new TleExpiredException(tleInfo.ageHours());
        }

        try {
            TLE tle = new TLE(snapshot.lines());
            Satellite sat = SatelliteFactory.createSatellite(tle);
            GroundStationPosition center = new GroundStationPosition(0.0, 0.0, 0.0);

            // 1. Past track: ordered oldest to now (excluding now to avoid duplicate at boundary)
            List<GroundTrackPoint> pastList = new ArrayList<>();
            Instant pastStart = now.minusSeconds((long) pastM * 60);
            for (Instant t = pastStart; t.isBefore(now); t = t.plusSeconds(stepS)) {
                SatPos p = sat.getPosition(center, Date.from(t));
                pastList.add(new GroundTrackPoint(
                        t.toString(),
                        GeoMath.toDegrees(p.getLatitude()),
                        GeoMath.normalizeLongitude(GeoMath.toDegrees(p.getLongitude()))
                ));
            }

            // 2. Future track: starts AT now and runs forward
            List<GroundTrackPoint> futureList = new ArrayList<>();
            Instant futureEnd = now.plusSeconds((long) futureM * 60);
            for (Instant t = now; !t.isAfter(futureEnd); t = t.plusSeconds(stepS)) {
                SatPos p = sat.getPosition(center, Date.from(t));
                futureList.add(new GroundTrackPoint(
                        t.toString(),
                        GeoMath.toDegrees(p.getLatitude()),
                        GeoMath.normalizeLongitude(GeoMath.toDegrees(p.getLongitude()))
                ));
            }

            return new GroundTrackResponse(ISS_CATALOG_ID, now.toString(), stepS, pastList, futureList, tleInfo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to propagate ground track: " + e.getMessage(), e);
        }
    }

    public PassPredictResponse predictPasses(PassPredictRequest request) {
        validateSatelliteId(request.satelliteId());

        Instant startTime = parseInstantOrNow(request.startTime());
        TleSnapshot snapshot = tleProvider.current();
        TleInfo tleInfo = buildTleInfo(snapshot, startTime);

        if (tleInfo.status() == TleFreshness.EXPIRED) {
            throw new TleExpiredException(tleInfo.ageHours());
        }

        int windowHours = request.windowHours() != null ? request.windowHours() : 24;
        double minElevation = request.minimumElevationDegrees() != null ? request.minimumElevationDegrees() : 10.0;

        try {
            TLE tle = new TLE(snapshot.lines());
            GroundStationPosition gs = new GroundStationPosition(request.latitude(), request.longitude(), 0.0);
            PassPredictor passPredictor = new PassPredictor(tle, gs);

            // Predict passes over the requested window
            List<SatPassTime> rawPasses = passPredictor.getPasses(Date.from(startTime), windowHours, false);

            Instant windowEnd = startTime.plus(Duration.ofHours(windowHours));
            Satellite sat = SatelliteFactory.createSatellite(tle);
            GroundStationPosition subPosGs = new GroundStationPosition(0.0, 0.0, 0.0);

            List<PassResponseItem> filteredPasses = new ArrayList<>();

            if (rawPasses != null) {
                for (SatPassTime pass : rawPasses) {
                    Instant riseInstant = pass.getStartTime().toInstant();

                    // Drop passes whose rise time is after startTime + windowHours
                    if (riseInstant.isAfter(windowEnd)) {
                        continue;
                    }

                    // Drop passes whose max elevation is below the requested threshold
                    if (pass.getMaxEl() < minElevation) {
                        continue;
                    }

                    Instant setInstant = pass.getEndTime().toInstant();
                    Instant tcaInstant = pass.getTCA().toInstant();
                    long durationSeconds = Duration.between(riseInstant, setInstant).toSeconds();

                    // Sample every 10 seconds from rise to set
                    List<PassSample> samples = new ArrayList<>();
                    long riseMs = pass.getStartTime().getTime();
                    long setMs = pass.getEndTime().getTime();

                    for (long ms = riseMs; ms <= setMs; ms += 10_000L) {
                        Date sampleDate = new Date(ms);
                        SatPos obsPos = sat.getPosition(gs, sampleDate);
                        SatPos subPos = sat.getPosition(subPosGs, sampleDate);

                        samples.add(new PassSample(
                                sampleDate.toInstant().toString(),
                                GeoMath.toDegrees(obsPos.getElevation()),
                                GeoMath.normalizeDegrees(GeoMath.toDegrees(obsPos.getAzimuth())),
                                GeoMath.toDegrees(subPos.getLatitude()),
                                GeoMath.normalizeLongitude(GeoMath.toDegrees(subPos.getLongitude()))
                        ));
                    }

                    // Ensure final set point is included if last sample didn't hit it exactly
                    if (!samples.isEmpty()) {
                        String lastSampleTime = samples.get(samples.size() - 1).time();
                        if (!lastSampleTime.equals(setInstant.toString())) {
                            Date setDate = pass.getEndTime();
                            SatPos obsPos = sat.getPosition(gs, setDate);
                            SatPos subPos = sat.getPosition(subPosGs, setDate);
                            samples.add(new PassSample(
                                    setDate.toInstant().toString(),
                                    GeoMath.toDegrees(obsPos.getElevation()),
                                    GeoMath.normalizeDegrees(GeoMath.toDegrees(obsPos.getAzimuth())),
                                    GeoMath.toDegrees(subPos.getLatitude()),
                                    GeoMath.normalizeLongitude(GeoMath.toDegrees(subPos.getLongitude()))
                            ));
                        }
                    }

                    filteredPasses.add(new PassResponseItem(
                            riseInstant.toString(),
                            tcaInstant.toString(),
                            setInstant.toString(),
                            durationSeconds,
                            pass.getMaxEl(),
                            GeoMath.normalizeDegrees(pass.getAosAzimuth()),
                            GeoMath.normalizeDegrees(pass.getLosAzimuth()),
                            samples
                    ));
                }
            }

            String note = "Geometric passes only: the ISS is above the horizon. Seeing it also needs darkness at your location and sunlight on the ISS.";
            String message = filteredPasses.isEmpty() ? "No passes found in the requested window." : null;

            return new PassPredictResponse(
                    ISS_CATALOG_ID,
                    new ObserverDto(request.latitude(), request.longitude()),
                    startTime.toString(),
                    windowHours,
                    minElevation,
                    filteredPasses.size(),
                    filteredPasses,
                    note,
                    tleInfo,
                    message
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate pass predictions: " + e.getMessage(), e);
        }
    }

    private void validateSatelliteId(String satelliteId) {
        if (satelliteId == null || !ISS_CATALOG_ID.equals(satelliteId.trim())) {
            throw new SatelliteNotSupportedException(satelliteId);
        }
    }

    private Instant parseInstantOrNow(String text) {
        if (text == null || text.isBlank()) {
            return Instant.now();
        }
        try {
            // Parsing via OffsetDateTime allows offsets like +05:30 to convert cleanly to Instant UTC
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid ISO-8601 date-time string: '" + text + "'");
        }
    }
}
