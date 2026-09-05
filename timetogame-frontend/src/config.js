export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8000/api";

export const OIDC_CONFIG = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY ?? "http://localhost:8080/realms/timetogame",
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? "timetogame-spa",
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? "http://localhost:5173/callback",
  post_logout_redirect_uri:
    import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI ?? "http://localhost:5173/",
};
