import { useState, useEffect, useCallback } from "react";
import issApi from "./api/issApi.js";
import Header from "./components/Header.jsx";
import Banner from "./components/Banner.jsx";
import MapView from "./components/MapView.jsx";
import SidePanel from "./components/SidePanel.jsx";
import PassList from "./components/PassList.jsx";
import ElevationChart from "./components/ElevationChart.jsx";

/**
 * Root application component orchestrating state, background polling,
 * error handling, and visual layouts.
 */
function App() {
  // Telemetry & orbital data state
  const [currentPosition, setCurrentPosition] = useState(null);
  const [groundTrack, setGroundTrack] = useState(null);
  const [tleInfo, setTleInfo] = useState(null);

  // Observer coordinates and query controls
  const [observer, setObserver] = useState({ latitude: 19.076, longitude: 72.8777 });
  const [windowHours, setWindowHours] = useState(24);
  const [minElevation, setMinElevation] = useState(10);

  // Passes state
  const [passes, setPasses] = useState([]);
  const [selectedPass, setSelectedPass] = useState(null);
  const [isLoadingPasses, setIsLoadingPasses] = useState(false);
  const [passError, setPassError] = useState(null);

  // General error banner state
  const [requestError, setRequestError] = useState(null);

  // Fetch real-time position
  const fetchPosition = useCallback(async () => {
    try {
      const data = await issApi.getPosition();
      setCurrentPosition(data);
      if (data.tle) {
        setTleInfo(data.tle);
      }
      setRequestError(null);
    } catch (err) {
      console.error("Failed to fetch ISS position:", err);
      const msg = err.response?.data?.message || err.message || "Failed to reach backend server";
      setRequestError(`Position update failed: ${msg}`);
    }
  }, []);

  // Fetch ground track
  const fetchGroundTrack = useCallback(async () => {
    try {
      const data = await issApi.getGroundTrack();
      setGroundTrack(data);
      if (data.tle) {
        setTleInfo(data.tle);
      }
      setRequestError(null);
    } catch (err) {
      console.error("Failed to fetch ground track:", err);
      const msg = err.response?.data?.message || err.message || "Failed to reach backend server";
      setRequestError(`Ground track update failed: ${msg}`);
    }
  }, []);

  // Fetch pass predictions for current observer
  const fetchPasses = useCallback(async () => {
    setIsLoadingPasses(true);
    setPassError(null);
    try {
      const data = await issApi.predictPasses({
        satelliteId: "25544",
        latitude: Number(observer.latitude),
        longitude: Number(observer.longitude),
        windowHours: Number(windowHours),
        minimumElevationDegrees: Number(minElevation),
      });

      setPasses(data.passes || []);
      if (data.passes && data.passes.length > 0) {
        setSelectedPass(data.passes[0]);
      } else {
        setSelectedPass(null);
      }
      if (data.tle) {
        setTleInfo(data.tle);
      }
      setRequestError(null);
    } catch (err) {
      console.error("Failed to fetch passes:", err);
      const msg = err.response?.data?.message || err.message || "Failed to calculate passes";
      setPassError(msg);
      setRequestError(`Pass prediction error: ${msg}`);
    } finally {
      setIsLoadingPasses(false);
    }
  }, [observer, windowHours, minElevation]);

  // Initial loads and background timers
  useEffect(() => {
    // 1. Initial fetches
    fetchPosition();
    fetchGroundTrack();
    fetchPasses();

    // 2. Poll position every 5 seconds
    const posInterval = setInterval(fetchPosition, 5000);

    // 3. Poll ground track every 2 minutes
    const trackInterval = setInterval(fetchGroundTrack, 120000);

    return () => {
      clearInterval(posInterval);
      clearInterval(trackInterval);
    };
  }, [fetchPosition, fetchGroundTrack, fetchPasses]);

  const handleObserverFieldChange = (field, val) => {
    setObserver((prev) => ({ ...prev, [field]: val }));
  };

  const handleMapSelectObserver = (newCoords) => {
    setObserver(newCoords);
  };

  const handleRetry = () => {
    setRequestError(null);
    fetchPosition();
    fetchGroundTrack();
    fetchPasses();
  };

  return (
    <>
      <Header tleInfo={tleInfo} />

      <Banner
        tleInfo={tleInfo}
        requestError={requestError}
        onRetry={handleRetry}
      />

      <main className="app-main">
        {/* Top Section: Map & Side Panel */}
        <section className="top-section">
          <MapView
            currentPosition={currentPosition}
            groundTrack={groundTrack}
            observer={observer}
            onSelectObserver={handleMapSelectObserver}
            selectedPass={selectedPass}
          />

          <SidePanel
            currentPosition={currentPosition}
            observer={observer}
            onObserverChange={handleObserverFieldChange}
            windowHours={windowHours}
            onWindowHoursChange={setWindowHours}
            minElevation={minElevation}
            onMinElevationChange={setMinElevation}
            onFindPasses={fetchPasses}
            isLoadingPasses={isLoadingPasses}
            tleStatus={tleInfo?.status}
          />
        </section>

        {/* Bottom Section: Pass List & Elevation Chart */}
        <section className="bottom-section">
          <PassList
            passes={passes}
            selectedPass={selectedPass}
            onSelectPass={setSelectedPass}
            isLoading={isLoadingPasses}
            error={passError}
            minElevation={minElevation}
            windowHours={windowHours}
          />

          <ElevationChart
            selectedPass={selectedPass}
            minElevation={minElevation}
          />
        </section>
      </main>

      <footer className="app-footer">
        Positions and passes are estimates computed from public TLE data (CelesTrak).
        Not official ISRO/NASA telemetry. A listed pass means the ISS is above the horizon;
        seeing it also needs darkness at your location and sunlight on the ISS.
      </footer>
    </>
  );
}

export default App;
