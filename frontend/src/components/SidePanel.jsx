import validationUtils from "../utils/validation.js";
import timeUtils from "../utils/time.js";

/**
 * Side panel providing live telemetry readouts and user controls
 * to configure observer coordinates and trigger pass prediction queries.
 */
function SidePanel({
  currentPosition,
  observer,
  onObserverChange,
  windowHours,
  onWindowHoursChange,
  minElevation,
  onMinElevationChange,
  onFindPasses,
  isLoadingPasses,
  tleStatus,
}) {
  const { isValid, latError, lonError } = validationUtils.validateObserver(
    observer.latitude,
    observer.longitude
  );

  const isExpired = tleStatus === "EXPIRED";
  const isSubmitDisabled = !isValid || isLoadingPasses || isExpired;

  const latDisplay = currentPosition
    ? `${currentPosition.latitudeDeg >= 0 ? "+" : ""}${currentPosition.latitudeDeg.toFixed(2)}°`
    : "--";
  const lonDisplay = currentPosition
    ? `${currentPosition.longitudeDeg >= 0 ? "+" : ""}${currentPosition.longitudeDeg.toFixed(2)}°`
    : "--";
  const altDisplay = currentPosition ? `${currentPosition.altitudeKm.toFixed(1)} km` : "--";
  const timeDisplay = currentPosition ? timeUtils.formatUtc(currentPosition.at) : "--";

  return (
    <aside className="side-panel">
      {/* Telemetry Card */}
      <div className="panel-card">
        <h2 className="card-title">ISS Telemetry</h2>
        <div className="telemetry-grid">
          <div className="telemetry-item">
            <div className="telemetry-label">Latitude</div>
            <div className="telemetry-value">{latDisplay}</div>
          </div>
          <div className="telemetry-item">
            <div className="telemetry-label">Longitude</div>
            <div className="telemetry-value">{lonDisplay}</div>
          </div>
          <div className="telemetry-item">
            <div className="telemetry-label">Altitude</div>
            <div className="telemetry-value">{altDisplay}</div>
          </div>
          <div className="telemetry-item">
            <div className="telemetry-label">Orbit</div>
            <div className="telemetry-value" style={{ fontSize: "13px" }}>LEO (~92 min)</div>
          </div>
          <div className="telemetry-item telemetry-time">
            <div className="telemetry-label">Evaluation Timestamp</div>
            <div className="telemetry-value" style={{ fontSize: "12px" }}>{timeDisplay}</div>
          </div>
        </div>
      </div>

      {/* Observer Form Card */}
      <div className="panel-card">
        <h2 className="card-title">Observer Location</h2>

        <div className="form-row">
          <div className="form-group">
            <label className="form-label" htmlFor="observer-lat">
              Latitude (&deg;)
            </label>
            <input
              id="observer-lat"
              type="number"
              step="any"
              className={`form-input ${latError ? "has-error" : ""}`}
              value={observer.latitude}
              onChange={(e) => onObserverChange("latitude", e.target.value)}
              placeholder="e.g. 19.076"
            />
            {latError && <p className="error-text">{latError}</p>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="observer-lon">
              Longitude (&deg;)
            </label>
            <input
              id="observer-lon"
              type="number"
              step="any"
              className={`form-input ${lonError ? "has-error" : ""}`}
              value={observer.longitude}
              onChange={(e) => onObserverChange("longitude", e.target.value)}
              placeholder="e.g. 72.8777"
            />
            {lonError && <p className="error-text">{lonError}</p>}
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label className="form-label" htmlFor="window-select">
              Forecast Window
            </label>
            <select
              id="window-select"
              className="form-select"
              value={windowHours}
              onChange={(e) => onWindowHoursChange(Number(e.target.value))}
            >
              <option value={6}>6 hours</option>
              <option value={12}>12 hours</option>
              <option value={24}>24 hours</option>
              <option value={48}>48 hours</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="el-select">
              Min Elevation
            </label>
            <select
              id="el-select"
              className="form-select"
              value={minElevation}
              onChange={(e) => onMinElevationChange(Number(e.target.value))}
            >
              <option value={0}>0&deg; (horizon)</option>
              <option value={5}>5&deg;</option>
              <option value={10}>10&deg;</option>
              <option value={20}>20&deg;</option>
              <option value={30}>30&deg;</option>
            </select>
          </div>
        </div>

        <button
          className="submit-btn"
          onClick={onFindPasses}
          disabled={isSubmitDisabled}
        >
          {isLoadingPasses ? "Calculating..." : "Find Passes"}
        </button>

        <p className="map-tip">
          Tip: Click anywhere on the world map to set your observer location.
        </p>
      </div>
    </aside>
  );
}

export default SidePanel;
