// In development, use the Vite proxy (relative URL)
// In production, use the full API URL
export const API_BASE_URL = import.meta.env.MODE === "production"
  ? "https://api.yourdomain.com"
  : ""; // Empty string means use relative URLs (Vite proxy)

export const API_VERSION = "/api/v1";

export const getApiUrl = (endpoint: string): string => {
  // Remove leading slash if present
  const cleanEndpoint = endpoint.startsWith("/") ? endpoint.slice(1) : endpoint;
  return `${API_BASE_URL}${API_VERSION}/${cleanEndpoint}`;
};
