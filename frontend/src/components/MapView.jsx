import { useEffect, useRef } from "react";
import L from "leaflet";
import geoUtils from "../utils/geo.js";

/**
 * Interactive world map component using plain Leaflet.
 * Renders the real-time ISS position, observer pin, ground track (past & future),
 * and active pass trajectory with antimeridian splitting.
 */
function MapView({
  currentPosition,
  groundTrack,
  observer,
  onSelectObserver,
  selectedPass,
}) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const issMarkerRef = useRef(null);
  const observerMarkerRef = useRef(null);
  const pastTrackGroupRef = useRef(null);
  const futureTrackGroupRef = useRef(null);
  const passTrackGroupRef = useRef(null);

  // Initialize Leaflet map once on mount, destroy on unmount (StrictMode safe)
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [20, 0],
      zoom: 2,
      minZoom: 2,
      worldCopyJump: true,
      zoomControl: true,
    });

    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
    }).addTo(map);

    // Layer groups for dynamic polyline paths
    pastTrackGroupRef.current = L.layerGroup().addTo(map);
    futureTrackGroupRef.current = L.layerGroup().addTo(map);
    passTrackGroupRef.current = L.layerGroup().addTo(map);

    // ISS circleMarker (avoids Vite asset loading issues with default image markers)
    const issMarker = L.circleMarker([0, 0], {
      radius: 8,
      fillColor: "#00e5ff",
      color: "#ffffff",
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9,
    }).addTo(map);
    issMarker.bindTooltip("ISS", { permanent: false, direction: "top" });
    issMarkerRef.current = issMarker;

    // Observer circleMarker
    const observerMarker = L.circleMarker([19.076, 72.8777], {
      radius: 7,
      fillColor: "#f59e0b",
      color: "#ffffff",
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9,
    }).addTo(map);
    observerMarker.bindTooltip("Observer", { permanent: false, direction: "top" });
    observerMarkerRef.current = observerMarker;

    // Map click handler to update observer position
    map.on("click", (e) => {
      // Normalize longitude to [-180, 180]
      let lon = ((e.latlng.lng + 180) % 360 + 360) % 360 - 180;
      let lat = e.latlng.lat;
      // Clamp latitude to [-90, 90]
      if (lat > 90) lat = 90;
      if (lat < -90) lat = -90;

      const roundedLat = Math.round(lat * 10000) / 10000;
      const roundedLon = Math.round(lon * 10000) / 10000;

      if (onSelectObserver) {
        onSelectObserver({ latitude: roundedLat, longitude: roundedLon });
      }
    });

    const handleResize = () => {
      map.invalidateSize();
    };
    window.addEventListener("resize", handleResize);

    mapInstanceRef.current = map;

    return () => {
      window.removeEventListener("resize", handleResize);
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // Update ISS position marker
  useEffect(() => {
    if (!issMarkerRef.current || !currentPosition) return;
    issMarkerRef.current.setLatLng([currentPosition.latitudeDeg, currentPosition.longitudeDeg]);
    issMarkerRef.current.setTooltipContent(
      `ISS<br/>Lat: ${currentPosition.latitudeDeg.toFixed(2)}&deg;<br/>Lon: ${currentPosition.longitudeDeg.toFixed(2)}&deg;`
    );
  }, [currentPosition]);

  // Update observer marker
  useEffect(() => {
    if (!observerMarkerRef.current || !observer) return;
    const lat = Number(observer.latitude);
    const lon = Number(observer.longitude);
    if (!isNaN(lat) && !isNaN(lon)) {
      observerMarkerRef.current.setLatLng([lat, lon]);
      observerMarkerRef.current.setTooltipContent(
        `Observer<br/>Lat: ${lat.toFixed(2)}&deg;<br/>Lon: ${lon.toFixed(2)}&deg;`
      );
    }
  }, [observer]);

  // Update ground track polylines
  useEffect(() => {
    if (!pastTrackGroupRef.current || !futureTrackGroupRef.current) return;

    pastTrackGroupRef.current.clearLayers();
    futureTrackGroupRef.current.clearLayers();

    if (groundTrack?.past) {
      const pastSegments = geoUtils.splitAtDateLine(groundTrack.past);
      pastSegments.forEach((segment) => {
        L.polyline(segment, {
          color: "#00e5ff",
          weight: 2,
          opacity: 0.8,
        }).addTo(pastTrackGroupRef.current);
      });
    }

    if (groundTrack?.future) {
      const futureSegments = geoUtils.splitAtDateLine(groundTrack.future);
      futureSegments.forEach((segment) => {
        L.polyline(segment, {
          color: "#38bdf8",
          weight: 2,
          opacity: 0.7,
          dashArray: "6, 8",
        }).addTo(futureTrackGroupRef.current);
      });
    }
  }, [groundTrack]);

  // Update highlighted selected pass track
  useEffect(() => {
    if (!passTrackGroupRef.current) return;

    passTrackGroupRef.current.clearLayers();

    if (selectedPass?.samples && selectedPass.samples.length > 0) {
      const passSegments = geoUtils.splitAtDateLine(selectedPass.samples);
      passSegments.forEach((segment) => {
        L.polyline(segment, {
          color: "#f59e0b",
          weight: 4,
          opacity: 0.95,
        }).addTo(passTrackGroupRef.current);
      });
    }
  }, [selectedPass]);

  return (
    <div className="map-container-wrapper">
      <div ref={mapContainerRef} className="leaflet-map" />
    </div>
  );
}

export default MapView;
