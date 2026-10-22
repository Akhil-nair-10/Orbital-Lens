/**
 * Geographic calculation utilities for mapping and display.
 */

const COMPASS_POINTS = [
  "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
  "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
];

/**
 * Converts azimuth in degrees (0..360) into a 16-point compass direction string.
 */
function compassFromDegrees(degrees) {
  if (degrees == null || isNaN(degrees)) return "--";
  const normalized = ((degrees % 360) + 360) % 360;
  const index = Math.floor((normalized + 11.25) / 22.5) % 16;
  return COMPASS_POINTS[index];
}

/**
 * Splits a list of coordinate points into segments whenever longitude crosses
 * the antimeridian (180th meridian). Prevents Leaflet from drawing horizontal
 * lines spanning across the entire world map.
 *
 * Accepts either [{ latitudeDeg, longitudeDeg }, ...] or [[lat, lon], ...]
 * Returns an array of segment coordinate arrays: [[[lat, lon], ...], [[lat, lon], ...]]
 */
function splitAtDateLine(points) {
  if (!points || points.length === 0) return [];

  const segments = [];
  let currentSegment = [];

  for (let i = 0; i < points.length; i++) {
    const pt = points[i];
    const lat = pt.latitudeDeg !== undefined ? pt.latitudeDeg : pt[0];
    const lon = pt.longitudeDeg !== undefined ? pt.longitudeDeg : pt[1];

    if (currentSegment.length > 0) {
      const prevPt = currentSegment[currentSegment.length - 1];
      const prevLon = prevPt[1];

      // If the longitude jumped by more than 180 degrees, it crossed the date line
      if (Math.abs(lon - prevLon) > 180) {
        segments.push(currentSegment);
        currentSegment = [];
      }
    }

    currentSegment.push([lat, lon]);
  }

  if (currentSegment.length > 0) {
    segments.push(currentSegment);
  }

  return segments;
}

const geo = {
  compassFromDegrees,
  splitAtDateLine,
};

export default geo;
