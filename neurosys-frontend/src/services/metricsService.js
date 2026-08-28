import api from './api';

const API_BASE = '/api/v1';

/**
 * Resilient API Fetcher with Axios + Native Fetch fallback
 * Guarantees data arrays are cleanly unwrapped across all network conditions.
 */
export const fetchRealApi = async (endpoint) => {
  const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  
  // 1. Try Axios Service
  try {
    const res = await api.get(path);
    if (res && res.data !== undefined) {
      return res.data;
    }
    if (Array.isArray(res)) return res;
    if (res && typeof res === 'object') return res;
  } catch (axiosErr) {
    console.warn(`Axios call to ${path} failed, attempting direct native fetch...`, axiosErr);
  }

  // 2. Direct Native Fetch Fallback
  try {
    const rawRes = await fetch(`${API_BASE}${path}`);
    if (rawRes.ok) {
      const rawData = await rawRes.json();
      if (rawData && rawData.data !== undefined) {
        return rawData.data;
      }
      return rawData;
    }
  } catch (fetchErr) {
    console.error(`Native fetch to ${API_BASE}${path} failed`, fetchErr);
  }

  return null;
};

export const metricsService = {
  fetchRealApi,
  
  // Computer Management APIs
  getAllComputers: async () => fetchRealApi('/computers'),
  getPendingComputers: async () => fetchRealApi('/computers/pending'),
  getComputerById: async (id) => fetchRealApi(`/computers/${id}`),
  getComputersByLab: async (labName) => fetchRealApi(`/computers/lab/${labName}`),
  approveComputer: async (id) => api.put(`/computers/${id}/approve`),
  rejectComputer: async (id) => api.put(`/computers/${id}/reject`),

  // Telemetry History & Metrics APIs
  getMetricHistory: async (id, limit = 30) => fetchRealApi(`/agent/metrics/history/${id}?limit=${limit}`),
  getHealthScore: async (id) => fetchRealApi(`/health-score/${id}`),
  getProcesses: async (id, search = '', sortBy = 'cpu', page = 0, size = 10) =>
    fetchRealApi(`/computers/${id}/processes?search=${search}&sortBy=${sortBy}&page=${page}&size=${size}`),
  getFileAnalysis: async (id) => fetchRealApi(`/computers/${id}/file-analyzer/summary`),
  getLogs: async (id, logLevel = '', page = 0, size = 15) =>
    fetchRealApi(`/computers/${id}/logs?logLevel=${logLevel}&page=${page}&size=${size}`),

  // Alert Center APIs
  getAllAlerts: async () => fetchRealApi('/alerts'),
  getActiveAlerts: async () => fetchRealApi('/alerts'),
  getComputerAlerts: async (id) => fetchRealApi(`/alerts/computer/${id}`),
  acknowledgeAlert: async (id) => api.put(`/alerts/${id}/acknowledge`),
  resolveAlert: async (id) => api.put(`/alerts/${id}/resolve`),

  // Remote Power Management APIs
  lockComputer: async (computerId) => api.post(`/computers/${computerId}/lock`, {}),
  restartComputer: async (computerId) => api.post(`/computers/${computerId}/restart`, {}),
  shutdownComputer: async (computerId) => api.post(`/computers/${computerId}/shutdown`, {}),
  sendPowerCommand: async (computerId, commandType) => {
    const endpoint = (commandType || 'SHUTDOWN').toLowerCase();
    return api.post(`/computers/${computerId}/${endpoint}`, {});
  },
  remoteAction: async (computerId, commandType) => {
    const endpoint = (commandType || 'SHUTDOWN').toLowerCase();
    return api.post(`/computers/${computerId}/${endpoint}`, {});
  },
  getPowerAudits: async (computerId) => fetchRealApi(`/computers/${computerId}/power-audits`),

  // AI Performance, Prediction & Diagnosis APIs
  getCrashPrediction: async (id) => fetchRealApi(`/predictions/crash/${id}`),
  evaluateCrashRisk: async (id) => api.post(`/predictions/crash/${id}/evaluate`, {}),
  getAIDiagnosis: async (computerId) => fetchRealApi(`/diagnostics/${computerId}`),
  getAnalyticsSummary: async () => fetchRealApi('/analytics/summary'),
  askAiAssistant: async (message, computerId) => api.post('/ai-assistant/chat', { message, computerId }),
};
