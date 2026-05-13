import axios from 'axios';

const BASE = import.meta.env.VITE_PUBLIC_BACKEND_URL || 'https://helveticraft.com/api';

const api = axios.create({
  baseURL: BASE,
  headers: { 'Content-Type': 'application/json' }
});

// attach token automatically if present
api.interceptors.request.use((cfg) => {
  try {
    const token = localStorage.getItem('hc_token');
    if (token) cfg.headers = { ...cfg.headers, Authorization: `Bearer ${token}` };
  } catch (e) {
    // ignore
  }
  return cfg;
});

export default api;
