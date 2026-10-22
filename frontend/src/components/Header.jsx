import { useState, useEffect } from "react";
import timeUtils from "../utils/time.js";

/**
 * Top application header presenting application branding, active satellite ID,
 * TLE element freshness status, data source attribution, and a real-time UTC clock.
 */
function Header({ tleInfo }) {
  const [currentUtc, setCurrentUtc] = useState(timeUtils.formatUtc(new Date().toISOString()));

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentUtc(timeUtils.formatUtc(new Date().toISOString()));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const status = tleInfo?.status || "FRESH";
  const source = tleInfo?.source || "CELESTRAK";
  const ageText = tleInfo ? timeUtils.relativeAge(tleInfo.ageHours) : "orbit data loading...";

  let badgeClass = "badge-fresh";
  if (status === "STALE") {
    badgeClass = "badge-stale";
  } else if (status === "EXPIRED") {
    badgeClass = "badge-expired";
  }

  return (
    <header className="app-header">
      <div className="header-brand">
        <h1 className="header-title">OrbitalLens</h1>
        <span className="header-sat-tag">ISS (25544)</span>
      </div>

      <div className="header-meta">
        <div className={`badge ${badgeClass}`} title={tleInfo?.message || ""}>
          <span className="badge-dot"></span>
          <span>{status}</span>
          <span className="num">({ageText})</span>
        </div>

        <div className="header-source" style={{ fontSize: "12px", color: "var(--text-muted)" }}>
          Source: <strong style={{ color: "var(--text-main)" }}>{source}</strong>
        </div>

        <div className="utc-clock">
          <span>{currentUtc}</span>
        </div>
      </div>
    </header>
  );
}

export default Header;
