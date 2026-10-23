import axios from "axios";

/**
 * Pre-configured Axios client targeting the local Spring Boot backend.
 * Uses a 15-second timeout and passes through Vite's dev proxy (/api -> http://127.0.0.1:8080).
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 15000,
});

async function getSatellite(id = "25544") {
  const response = await apiClient.get(`/satellites/${id}`);
  return response.data;
}

async function getPosition(id = "25544", at) {
  const params = at ? { at } : {};
  const response = await apiClient.get(`/satellites/${id}/position`, { params });
  return response.data;
}

async function getGroundTrack(id = "25544", pastMinutes = 45, futureMinutes = 90, stepSeconds = 60) {
  const response = await apiClient.get(`/satellites/${id}/ground-track`, {
    params: { pastMinutes, futureMinutes, stepSeconds },
  });
  return response.data;
}

async function predictPasses(requestData) {
  const response = await apiClient.post("/passes/predict", requestData);
  return response.data;
}

const issApi = {
  getSatellite,
  getPosition,
  getGroundTrack,
  predictPasses,
};

export default issApi;
