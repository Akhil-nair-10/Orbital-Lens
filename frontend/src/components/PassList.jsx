import timeUtils from "../utils/time.js";
import geoUtils from "../utils/geo.js";

/**
 * Tabular list of predicted visible passes over the observer's horizon.
 * Selecting a pass highlights its track on the map and renders its elevation chart.
 */
function PassList({
  passes,
  selectedPass,
  onSelectPass,
  isLoading,
  error,
  minElevation,
  windowHours,
}) {
  if (isLoading) {
    return (
      <div className="panel-card pass-list-card">
        <h2 className="card-title">Upcoming Passes</h2>
        <div className="empty-state">
          <span>Computing orbital passes...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="panel-card pass-list-card">
        <h2 className="card-title">Upcoming Passes</h2>
        <div className="empty-state" style={{ color: "var(--error-red)" }}>
          <span>Failed to load passes: {error}</span>
        </div>
      </div>
    );
  }

  if (!passes || passes.length === 0) {
    return (
      <div className="panel-card pass-list-card">
        <h2 className="card-title">Upcoming Passes</h2>
        <div className="empty-state">
          <span>{`No passes of at least ${minElevation}° in the next ${windowHours} hours`}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="panel-card pass-list-card">
      <h2 className="card-title">
        Upcoming Passes <span className="num">({passes.length})</span>
      </h2>
      <div className="pass-table-wrapper">
        <table className="pass-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Rise &rarr; Set (Local / UTC)</th>
              <th>Max El</th>
              <th>Duration</th>
              <th>Direction</th>
            </tr>
          </thead>
          <tbody>
            {passes.map((pass, index) => {
              const isSelected = selectedPass === pass;
              const dateStr = pass.riseTime ? pass.riseTime.substring(0, 10) : "--";
              const riseLocal = timeUtils.formatLocal(pass.riseTime);
              const setLocal = timeUtils.formatLocal(pass.setTime);
              const riseUtc = timeUtils.formatUtc(pass.riseTime);
              const setUtc = timeUtils.formatUtc(pass.setTime);

              const riseDir = geoUtils.compassFromDegrees(pass.riseAzimuthDeg);
              const setDir = geoUtils.compassFromDegrees(pass.setAzimuthDeg);
              const direction = `${riseDir} &rarr; ${setDir}`;

              return (
                <tr
                  key={index}
                  className={`pass-row ${isSelected ? "selected" : ""}`}
                  onClick={() => onSelectPass(pass)}
                >
                  <td className="num">{dateStr}</td>
                  <td>
                    <div>
                      {riseLocal.split(" ")[1]} &rarr; {setLocal.split(" ")[1]}
                    </div>
                    <div className="time-sub">
                      UTC: {riseUtc.split(" ")[1]} &rarr; {setUtc.split(" ")[1]}
                    </div>
                  </td>
                  <td className="num" style={{ fontWeight: 600, color: "var(--accent-cyan)" }}>
                    {pass.maxElevationDeg.toFixed(1)}&deg;
                  </td>
                  <td className="num">{timeUtils.formatDuration(pass.durationSeconds)}</td>
                  <td dangerouslySetInnerHTML={{ __html: direction }} />
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default PassList;
