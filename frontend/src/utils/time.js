/**
 * Date and time formatting helpers for displaying both Local and UTC values clearly.
 */

function parseDate(isoString) {
  if (!isoString) return null;
  const d = new Date(isoString);
  return isNaN(d.getTime()) ? null : d;
}

/**
 * Formats an ISO-8601 string into UTC string: "YYYY-MM-DD HH:mm:ss UTC"
 */
function formatUtc(isoString) {
  const d = parseDate(isoString);
  if (!d) return "--";

  const year = d.getUTCFullYear();
  const month = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  const hours = String(d.getUTCHours()).padStart(2, "0");
  const minutes = String(d.getUTCMinutes()).padStart(2, "0");
  const seconds = String(d.getUTCSeconds()).padStart(2, "0");

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds} UTC`;
}

/**
 * Formats an ISO-8601 string into browser local time: "YYYY-MM-DD HH:mm:ss"
 */
function formatLocal(isoString) {
  const d = parseDate(isoString);
  if (!d) return "--";

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  const seconds = String(d.getSeconds()).padStart(2, "0");

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * Formats duration in seconds to "mm:ss".
 */
function formatDuration(seconds) {
  if (seconds == null || isNaN(seconds)) return "--:--";
  const s = Math.max(0, Math.round(seconds));
  const mins = Math.floor(s / 60);
  const remainingSecs = s % 60;
  return `${String(mins).padStart(2, "0")}:${String(remainingSecs).padStart(2, "0")}`;
}

/**
 * Produces user-friendly relative age string for TLE freshness.
 */
function relativeAge(hours) {
  if (hours == null || isNaN(hours)) return "age unknown";
  return `orbit data ${hours.toFixed(1)} h old`;
}

const time = {
  formatUtc,
  formatLocal,
  formatDuration,
  relativeAge,
};

export default time;
