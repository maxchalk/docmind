import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach JWT token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('docmind_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 → redirect to login
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('docmind_token');
      localStorage.removeItem('docmind_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
