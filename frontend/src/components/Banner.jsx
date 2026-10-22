/**
 * Alert banner for orbital element status (STALE, EXPIRED) and network request errors.
 */
function Banner({ tleInfo, requestError, onRetry }) {
  if (requestError) {
    return (
      <div className="banner banner-error">
        <span>Request Error: {requestError}</span>
        {onRetry && (
          <button className="banner-btn" onClick={onRetry}>
            Retry
          </button>
        )}
      </div>
    );
  }

  if (tleInfo?.status === "EXPIRED") {
    return (
      <div className="banner banner-error">
        <span>
          <strong>Warning: </strong>
          {tleInfo.message || "Orbital elements are expired (over 336 hours old). Predictions are disabled."}
        </span>
      </div>
    );
  }

  if (tleInfo?.status === "STALE") {
    return (
      <div className="banner banner-warning">
        <span>
          <strong>Notice: </strong>
          {tleInfo.message || "Orbital elements are older than 72 hours. Predictions may have reduced accuracy."}
        </span>
      </div>
    );
  }

  return null;
}

export default Banner;
