# OrbitalLens v1

OrbitalLens is a web application that tracks the International Space Station (ISS, NORAD catalog 25544) in real time, visualizes its past and future ground track on a world map, and predicts upcoming passes over any observer location on Earth.

---

## Screenshots 
<img width="1919" height="1079" alt="Screenshot 2026-10-23 195432" src="https://github.com/user-attachments/assets/24f374f6-3b70-4ede-b7be-4ada8b99f80c" />

---

<img width="1919" height="1079" alt="Screenshot 2026-10-23 195451" src="https://github.com/user-attachments/assets/0d60933f-7c9e-43c4-bc0c-bf0c9819f3ae" />

## Features

- **Live ISS Position**: Automatically tracks current sub-satellite coordinates (latitude, longitude, altitude) refreshed every 5 seconds.
- **Orbital Ground Track**: Displays propagated past orbital path (solid line) and future orbit projection (dashed line) refreshed every 2 minutes.
- **Pass Forecast Engine**: Predicts geometric passes over an observer's location with configurable forecast window (6–48 hours) and minimum elevation angle (0°–30°).
- **Elevation Profiles**: Interactive Chart.js elevation graph over local time for selected passes.
- **Antimeridian Splitting**: Automatically splits ground tracks crossing the ±180° date line to prevent rendering artifacts.
- **Intelligent Caching & Graceful Fallback**: Fetches fresh orbital elements from CelesTrak with 2-hour memory caching, 15-minute failure cooldowns, local disk persistence, and a bundled offline fallback.
- **Dual Time Representation**: Displays browser local time alongside UTC across the UI.

---

## Tech Stack

- **Backend**: Java 25, Spring Boot 4.0.8, `predict4java 1.2.2` (SGP4 orbital propagator), Maven Wrapper (`mvnw.cmd`).
- **HTTP & Networking**: `java.net.http.HttpClient` (no external HTTP libraries).
- **Frontend**: Vite 8, React 19, JavaScript (ES modules), Leaflet 1.9 (world map), Chart.js 4 (elevation charts), Axios (HTTP).
- **Styling**: Pure CSS with CSS variables, system font stack, and dark scientific theme.

---

## Architecture Diagram

```
+--------------------------------------------------------------------+
|                            BROWSER (UI)                            |
|                                                                    |
|  +-------------------+  +-------------------+  +----------------+  |
|  |    Header.jsx     |  |    Banner.jsx     |  |  SidePanel.jsx |  |
|  +-------------------+  +-------------------+  +----------------+  |
|  |    MapView.jsx    |  |    PassList.jsx   |  | ElevationChart |  |
|  |     (Leaflet)     |  |                   |  |   (Chart.js)   |  |
|  +-------------------+  +-------------------+  +----------------+  |
|                           ^            |                           |
+---------------------------|------------|---------------------------+
                            | Axios      | Vite Dev Proxy (/api)
                            v            |
+----------------------------------------v---------------------------+
|                      SPRING BOOT 4 BACKEND                         |
|                                                                    |
|  [REST Controllers]                                                |
|  - HealthController      (/api/health)                             |
|  - SatelliteController   (/api/satellites/25544/...)               |
|  - PassController        (/api/passes/predict)                     |
|                                                                    |
|  [Business & Orbit Layer]                                          |
|  - OrbitService: coordinates propagation, ground track & passes    |
|  - GeoMath: radian/degree conversions, normalize lon, compass rose |
|                                                                    |
|  [TLE Management Layer]                                            |
|  - CelesTrakTleProvider: in-memory cache, cooldowns, file cache    |
|  - TleParser: line validation, catalog 25544 check, checksums      |
|                                                                    |
|  [External Propagator Engine]                                      |
|  - predict4java (SGP4 propagation model)                           |
+--------------------------------------------------------------------+
           |                                       |
    (HTTP GET, 2h TTL)                      (Local Fallback)
           v                                       v
   [CelesTrak API]                  [data/iss-tle-cache.txt]
                                    [sample-iss-tle.txt]
```

---

## How Data Flows

1. **TLE Acquisition**: When an endpoint is called, `CelesTrakTleProvider` checks if the in-memory TLE is younger than 120 minutes. If older and outside the 15-minute failure cooldown, it downloads fresh elements from CelesTrak. If downloading fails, it gracefully falls back to the disk cache or bundled sample.
2. **Freshness Assessment**: Epoch age is computed. Data under 72h is `FRESH`, 72h–336h is `STALE` (warning displayed), and beyond 336h is `EXPIRED` (returns 503).
3. **Position Calculation**: `OrbitService` creates an SGP4 satellite instance via `predict4java`, evaluates the sub-satellite point (station 0,0,0) at the target timestamp, and converts radians to geographic degrees.
4. **Pass Prediction**: `PassPredictor` computes all horizon passes for the observer. Passes outside the window or below minimum elevation are dropped. Remaining passes are sampled at 10-second intervals from rise to set.
5. **Frontend Presentation**: React components update telemetry, plot Leaflet polylines split across the antimeridian, and render Chart.js elevation curves.

---

## How It Works: Orbital Mechanics Simplified

- **Two-Line Element Set (TLE)**: A 2-line standard format encoding a satellite's orbit at a specific moment in time (epoch). It describes orbital inclination, eccentricity, mean motion (orbits per day), and RAAN.
- **SGP4 (Simplified General Perturbations 4)**: A mathematical algorithm used by space agencies to calculate satellite coordinates at any past or future time based on a TLE. It models Earth's gravitational irregularities (oblateness) and atmospheric friction.
- **Sub-Satellite Point & Ground Track**: The geographic point directly under the satellite. As the satellite travels in orbit and the Earth rotates eastward beneath it, the sub-satellite point traces the "ground track" on a world map.
- **Elevation Angle**: The angle in degrees of the satellite above the observer's horizon (0° = horizon, 90° = overhead).
- **Pass**: A period during which the satellite rises above the local horizon (AOS), reaches peak elevation (TCA), and sets back below the horizon (LOS).

---

## Windows Run Instructions

### Prerequisites
- Java 25 installed and available on PATH
- Node.js (v18+) and npm installed

### 1. Run Backend
Open PowerShell:
```powershell
cd D:\BCA_9\BCAproject\orbitallens\backend
.\mvnw.cmd spring-boot:run
```
The backend starts at `http://127.0.0.1:8080`.

### 2. Run Frontend
Open a separate PowerShell window:
```powershell
cd D:\BCA_9\BCAproject\orbitallens\frontend
npm install
npm run dev
```
Open your browser at `http://localhost:5173`.

---

## Running Backend Tests

In PowerShell:
```powershell
cd D:\BCA_9\BCAproject\orbitallens\backend
.\mvnw.cmd test
```

---

## REST API Documentation

All timestamps returned by the API are ISO-8601 UTC strings ending with `Z`.

### 1. Health Check
- **`GET /api/health`**
- **Response**:
```json
{
  "status": "UP",
  "time": "2026-10-22T10:23:25.154Z"
}
```

### 2. Supported Satellites
- **`GET /api/satellites`**
- **`GET /api/satellites/25544`**
- Any ID other than `25544` returns HTTP 404 with error code `SATELLITE_NOT_SUPPORTED`.

### 3. Real-Time Position
- **`GET /api/satellites/25544/position`**
- **Query Parameter** (optional): `at` (ISO-8601 timestamp; note that "+" in timezone offsets must be URL-encoded as `%2B`, e.g. `2026-10-22T00:00:00%2B05:30`).
- **Response**:
```json
{
  "satelliteId": "25544",
  "name": "ISS (ZARYA)",
  "at": "2026-10-22T10:23:47.271Z",
  "latitudeDeg": 7.93,
  "longitudeDeg": 17.29,
  "altitudeKm": 422.34,
  "tle": {
    "source": "CELESTRAK",
    "retrievedAt": "2026-10-22T10:23:47.271Z",
    "epoch": "2026-10-21T01:08:59.469Z",
    "ageHours": 33.2,
    "status": "FRESH",
    "message": "TLE data is up to date."
  }
}
```

### 4. Ground Track
- **`GET /api/satellites/25544/ground-track?pastMinutes=45&futureMinutes=90&stepSeconds=60`**
- **Limits**: `pastMinutes` (0–180), `futureMinutes` (0–360), `stepSeconds` (10–300).
- **Response**:
```json
{
  "satelliteId": "25544",
  "generatedAt": "2026-10-22T10:24:02.326Z",
  "stepSeconds": 60,
  "past": [{ "time": "...", "latitudeDeg": 35.70, "longitudeDeg": -8.38 }],
  "future": [{ "time": "...", "latitudeDeg": 7.17, "longitudeDeg": 17.84 }],
  "tle": { ... }
}
```

### 5. Pass Prediction
- **`POST /api/passes/predict`**
- **Request Body**:
```json
{
  "satelliteId": "25544",
  "latitude": 19.076,
  "longitude": 72.8777,
  "startTime": "2026-10-22T10:00:00Z",
  "windowHours": 24,
  "minimumElevationDegrees": 10.0
}
```
- **Response**:
```json
{
  "satelliteId": "25544",
  "observer": { "latitude": 19.076, "longitude": 72.8777 },
  "startTime": "2026-10-22T10:00:00Z",
  "windowHours": 24,
  "minimumElevationDegrees": 10.0,
  "passCount": 3,
  "passes": [
    {
      "riseTime": "...",
      "maxElevationTime": "...",
      "setTime": "...",
      "durationSeconds": 614,
      "maxElevationDeg": 26.2,
      "riseAzimuthDeg": 197.0,
      "setAzimuthDeg": 53.0,
      "samples": [
        { "time": "...", "elevationDeg": 0.0, "azimuthDeg": 197.0, "latitudeDeg": 6.4, "longitudeDeg": 71.4 }
      ]
    }
  ],
  "note": "Geometric passes only: the ISS is above the horizon. Seeing it also needs darkness at your location and sunlight on the ISS.",
  "tle": { ... }
}
```

---

## Data Source & Attribution

Orbital elements are obtained from [CelesTrak](https://celestrak.org/) (Dr. T.S. Kelso).
CelesTrak NORAD GP URL: `https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE`.
Map tiles are provided by &copy; [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.

---

## Limitations

- **Estimates**: Positions and passes are mathematical approximations calculated from public TLE sets via SGP4, not real-time telemetry from ISRO/NASA ground networks.
- **Data Age**: As TLEs age, atmospheric friction slows the ISS down, introducing time errors of several seconds after a few days.
- **Geometric vs Visual Passes**: Pass predictions indicate when the ISS is geometrically above the observer's horizon. Actually seeing the ISS with the naked eye requires optical visibility: the observer must be in darkness (night/dawn/dusk) while the ISS at ~420 km altitude remains illuminated by direct sunlight.
- **Single Satellite**: Version 1 exclusively tracks the ISS (NORAD 25544).
- **No Frontend Test Framework**: To prevent adding extra dependencies, frontend testing is omitted.

---

## Done vs Planned

### Done in v1
- Complete Spring Boot 4 REST API with SGP4 propagation and validation.
- Leaflet map with ISS marker, observer marker, antimeridian-safe ground tracks, and map click location picking.
- Chart.js elevation curves with threshold lines.
- CelesTrak TLE caching, failure cooldown, disk caching, and bundled fallback.
- Comprehensive JUnit 5 and MockMvc tests.

### Planned for Future Versions
- Multi-satellite support (Tiangong space station, Hubble, Landsat).
- Visual pass filtering (sunlight vs eclipse illumination calculation).
