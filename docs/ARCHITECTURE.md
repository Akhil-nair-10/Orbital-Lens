# OrbitalLens Architecture & Interview Reference

This document provides a comprehensive technical overview of the OrbitalLens codebase, detailed request execution flows, and key orbital mechanics definitions for interview preparation.

---

## 1. File Catalog

### Backend (`backend/`)

- **`pom.xml`**: Maven project configuration defining Spring Boot 4.0.8, Java 25, dependencies (`predict4java`, `spring-boot-starter-webmvc`, `spring-boot-starter-validation`), and the crucial `slf4j-simple` exclusion.
- **`src/main/resources/application.properties`**: Core backend configuration including port 8080, binding address 127.0.0.1, CelesTrak TLE URL, caching TTLs, failure cooldowns, and freshness thresholds.
- **`src/main/resources/sample-iss-tle.txt`**: Bundled fallback Two-Line Element (TLE) text file for the ISS, used when network and local disk cache are unavailable.
- **`src/main/java/com/orbitallens/backend/BackendApplication.java`**: Spring Boot application entry point containing `public static void main(String[] args)`.

#### Package: `com.orbitallens.backend.tle`
- **`TleFreshness.java`**: Enum (`FRESH`, `STALE`, `EXPIRED`) describing the validity state of orbital elements based on their epoch age.
- **`TleSource.java`**: Enum (`CELESTRAK`, `CACHE_FILE`, `BUNDLED_SAMPLE`) recording where the active TLE was sourced.
- **`TleSnapshot.java`**: Immutable record storing the 3 TLE lines, satellite name, epoch instant, retrieval instant, and source origin.
- **`TleParser.java`**: Validates 69-character line lengths, line headers ("1 ", "2 "), catalog number 25544, modulo-10 checksums, and parses the epoch into an ISO UTC `Instant`.
- **`TleProvider.java`**: Strategy interface providing `TleSnapshot current()`.
- **`CelesTrakTleProvider.java`**: Thread-safe implementation of `TleProvider` implementing memory caching, failure cooldowns, local disk persistence (`data/iss-tle-cache.txt`), and graceful fallback to the bundled sample.

#### Package: `com.orbitallens.backend.orbit`
- **`GeoMath.java`**: Utility class providing radian-to-degree conversions, longitude normalization to `[-180.0, 180.0]`, angle modulo, and 16-point compass heading conversions.
- **`OrbitService.java`**: Core business service utilizing `predict4java` for SGP4 orbital propagation, past/future ground track generation, freshness evaluation, and pass predictions.

#### Package: `com.orbitallens.backend.api`
- **`HealthController.java`**: REST controller exposing `GET /api/health` for uptime checks.
- **`SatelliteController.java`**: REST controller exposing metadata, real-time position (`GET /api/satellites/25544/position`), and ground track (`GET /api/satellites/25544/ground-track`).
- **`PassController.java`**: REST controller exposing pass prediction (`POST /api/passes/predict`).

#### Package: `com.orbitallens.backend.api.dto`
- **`TleInfo.java`**: Record returning TLE metadata (source, retrievedAt, epoch, ageHours, status, message).
- **`HealthResponse.java`**: Record for service health status.
- **`SatelliteSummary.java`**: Record containing satellite ID, name, and TleInfo.
- **`SatellitesResponse.java`**: Record containing the list of supported satellites.
- **`SatellitePositionResponse.java`**: Record containing instantaneous latitude, longitude, altitude, and evaluation timestamp.
- **`GroundTrackPoint.java`**: Record containing a single timestamped coordinate along the orbit.
- **`GroundTrackResponse.java`**: Record returning past and future orbital ground track segments.
- **`ObserverDto.java`**: Record holding observer coordinates (latitude and longitude).
- **`PassSample.java`**: Record holding a 10-second sample of elevation, azimuth, and sub-satellite coordinates during a pass.
- **`PassResponseItem.java`**: Record representing a single pass with rise, TCA, set, max elevation, and sample series.
- **`PassPredictRequest.java`**: Validated record for pass forecast input parameters.
- **`PassPredictResponse.java`**: Record returning all predicted passes and observer metadata.

#### Package: `com.orbitallens.backend.api.error`
- **`InvalidTleDataException.java`**: Unchecked exception thrown when raw TLE data fails format or checksum verification.
- **`SatelliteNotSupportedException.java`**: Unchecked exception thrown when any satellite ID other than 25544 is requested (maps to HTTP 404).
- **`TleExpiredException.java`**: Unchecked exception thrown when TLE age exceeds expiration threshold (maps to HTTP 503).
- **`TleUnavailableException.java`**: Unchecked exception thrown when no TLE data is reachable from any source (maps to HTTP 503).
- **`BadRequestException.java`**: Unchecked exception thrown for invalid query parameters or dates (maps to HTTP 400).
- **`ErrorResponse.java`**: Standard JSON envelope for all API errors.
- **`GlobalExceptionHandler.java`**: `@RestControllerAdvice` mapping exceptions to standard `ErrorResponse` records without leaking stack traces.

---

### Frontend (`frontend/`)

- **`index.html`**: Single page application HTML shell with title "OrbitalLens".
- **`vite.config.js`**: Vite configuration containing React plugin and API proxy (`/api` -> `http://127.0.0.1:8080`).
- **`src/main.jsx`**: React root mounting `App` into `#root` and importing Leaflet CSS and application styles.
- **`src/App.jsx`**: Central state coordinator managing position polling (5s), ground-track polling (2m), observer coordinates, and pass queries.
- **`src/styles.css`**: Dark scientific design system using CSS variables, system typography, monospace numerical styling, and responsive layout rules.

#### Modules: `src/api` & `src/utils`
- **`src/api/issApi.js`**: Axios HTTP client exporting an object with methods for position, ground track, satellite info, and pass prediction.
- **`src/utils/geo.js`**: Functions for antimeridian polyline splitting (`splitAtDateLine`) and compass direction conversions (`compassFromDegrees`).
- **`src/utils/time.js`**: Functions formatting UTC strings, local browser times, durations (mm:ss), and relative TLE age strings.
- **`src/utils/validation.js`**: Client-side validation for observer latitude (-90..90) and longitude (-180..180).

#### Components: `src/components`
- **`Header.jsx`**: Header bar showing branding, ISS catalog tag, TLE freshness status badge, data source, and ticking UTC clock.
- **`Banner.jsx`**: Contextual banner alerting users to STALE or EXPIRED orbital data or network connection issues with a Retry button.
- **`MapView.jsx`**: Leaflet world map rendering ISS position circle marker, observer circle marker, past track, dashed future track, and active pass trajectory.
- **`SidePanel.jsx`**: Real-time telemetry cards (lat, lon, alt) and observer inputs with forecast window and minimum elevation selectors.
- **`PassList.jsx`**: Table listing upcoming passes with local and UTC times, maximum elevation, duration, and trajectory directions.
- **`ElevationChart.jsx`**: Chart.js canvas component rendering the elevation curve over time for the selected pass with an optional threshold line.

---

## 2. Request Flows

### A. Real-Time Position Flow (`GET /api/satellites/25544/position?at=...`)
1. **Controller**: `SatelliteController.getPosition` receives satellite ID and optional `at` timestamp query parameter.
2. **Validation**: Confirms ID is `"25544"`. Parses `at` using `OffsetDateTime.parse(...).toInstant()` (defaults to `Instant.now()`).
3. **TLE Acquisition**: `CelesTrakTleProvider.current()` evaluates cache freshness (TTL 120m). If expired, attempts CelesTrak HTTP GET with 10s timeout. On failure, falls back to in-memory, disk cache, or bundled sample.
4. **Freshness Guard**: `OrbitService.evaluateFreshness` checks elapsed hours between epoch and evaluation time. If > 336 hours, throws `TleExpiredException` (HTTP 503).
5. **Propagation**: Instantiates `uk.me.g4dpz.satellite.TLE` and creates a fresh `Satellite` via `SatelliteFactory`. Queries `sat.getPosition(new GroundStationPosition(0, 0, 0), date)`.
6. **Coordinate Transform**: Converts radians to degrees via `GeoMath.toDegrees` and normalizes longitude to `[-180.0, 180.0]`. Altitude is read directly in kilometers.
7. **Response**: Returns `SatellitePositionResponse` containing coordinates, altitude, evaluation time, and `TleInfo`.

### B. Ground Track Flow (`GET /api/satellites/25544/ground-track`)
1. **Controller**: `SatelliteController.getGroundTrack` receives `pastMinutes` (0-180), `futureMinutes` (0-360), and `stepSeconds` (10-300).
2. **Range Validation**: Verifies boundary parameters in code; throws `BadRequestException` if limits are violated.
3. **Propagation Loop**:
   - Creates a fresh `Satellite` instance.
   - Computes past track points stepping by `stepSeconds` from `now - pastMinutes` up to `now`.
   - Computes future track points stepping by `stepSeconds` from `now` to `now + futureMinutes`.
4. **Response**: Returns `GroundTrackResponse` containing ordered past/future points and `TleInfo`.
5. **Frontend Rendering**: `MapView.jsx` runs points through `geoUtils.splitAtDateLine` to split continuous paths when crossing the +-180° meridian, rendering past points as solid cyan and future points as dashed blue lines.

### C. Pass Prediction Flow (`POST /api/passes/predict`)
1. **Controller**: `PassController.predict` validates request body via `@Valid` (`@DecimalMin`, `@DecimalMax`, `@Min`, `@Max`).
2. **Observer Coordinates**: Creates `uk.me.g4dpz.satellite.GroundStationPosition(lat, lon, 0)`.
3. **Pass Predictor**: Instantiates `uk.me.g4dpz.satellite.PassPredictor` and calls `getPasses(startDate, windowHours, false)`.
4. **Filtering**:
   - Drops passes whose rise time occurs after `startDate + windowHours`.
   - Drops passes whose maximum elevation is lower than `minimumElevationDegrees`.
5. **Sampling Loop**: For each surviving pass, steps every 10 seconds from `pass.getStartTime()` to `pass.getEndTime()`, sampling elevation, azimuth, and sub-satellite coordinates at each step.
6. **Response**: Returns `PassPredictResponse` containing pass metadata, rise/set azimuths, durations, and detailed sample series.
7. **Frontend Selection**: `PassList` displays passes. Selecting a row updates `ElevationChart` to display the rise-to-set elevation curve and highlights the ground track on the map.

---

## 3. Glossary & Interview Concepts

- **TLE (Two-Line Element Set)**: A standardized data format developed by NORAD/NASA encoding orbital elements of Earth-orbiting satellites at a specific point in time (the epoch).
- **Epoch**: The exact instant at which the orbital parameters in a TLE were measured. Because Low Earth Orbit satellites experience atmospheric drag, TLEs lose accuracy over time.
- **SGP4 (Simplified General Perturbations 4)**: The mathematical propagation model used to calculate satellite position and velocity vectors from a TLE, taking into account Earth oblateness (geopotential harmonics J2, J3, J4) and atmospheric drag.
- **Sub-Satellite Point**: The point on the Earth's surface directly beneath the satellite (nadir), defined by geodetic latitude and longitude.
- **Ground Track**: The path projected on the Earth's surface directly beneath the satellite as it moves along its orbit while the Earth rotates underneath it.
- **Elevation**: The vertical angular distance of a satellite above the observer's local horizon (0° = horizon, 90° = directly overhead/zenith). Negative values mean the satellite is below the horizon.
- **Azimuth**: The horizontal compass direction of a satellite relative to true north, measured clockwise from 0° (North), 90° (East), 180° (South), to 270° (West).
- **AOS (Acquisition of Signal)**: The moment a satellite rises above the observer's horizon or minimum elevation mask.
- **LOS (Loss of Signal)**: The moment a satellite dips below the observer's horizon or minimum elevation mask.
- **TCA (Time of Closest Approach)**: The instant during a pass when the satellite reaches its highest elevation angle and shortest distance to the observer.
- **UTC (Coordinated Universal Time)**: The primary time standard by which the world regulates clocks. All astronomical calculations and backend APIs use UTC (ending with `Z`) to avoid daylight saving and timezone ambiguities.
