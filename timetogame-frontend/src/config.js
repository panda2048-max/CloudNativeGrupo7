export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8000/api";

// Microsoft Entra ID (Azure AD). TENANT_ID y CLIENT_ID_SPA salen del App
// registration "timetogame-spa"; API_SCOPE es el scope expuesto por el App
// registration "timetogame-api" (Expose an API > Add a scope).
export const AZURE_AD_CONFIG = {
  tenantId: import.meta.env.VITE_AZURE_TENANT_ID ?? "<TENANT_ID>",
  clientId: import.meta.env.VITE_AZURE_CLIENT_ID_SPA ?? "<CLIENT_ID_SPA>",
  redirectUri: import.meta.env.VITE_AZURE_REDIRECT_URI ?? "http://localhost:5173/callback",
  postLogoutRedirectUri: import.meta.env.VITE_AZURE_POST_LOGOUT_REDIRECT_URI ?? "http://localhost:5173/",
};

export const API_SCOPE =
  import.meta.env.VITE_AZURE_API_SCOPE ?? "api://<CLIENT_ID_API>/access_as_user";
