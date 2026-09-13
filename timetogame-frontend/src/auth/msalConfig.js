import { PublicClientApplication, LogLevel, EventType } from "@azure/msal-browser";
import { AZURE_AD_CONFIG, API_SCOPE } from "../config";

export const msalConfig = {
  auth: {
    clientId: AZURE_AD_CONFIG.clientId,
    authority: `https://login.microsoftonline.com/${AZURE_AD_CONFIG.tenantId}`,
    redirectUri: AZURE_AD_CONFIG.redirectUri,
    postLogoutRedirectUri: AZURE_AD_CONFIG.postLogoutRedirectUri,
    // Por defecto MSAL, despues de procesar la respuesta en redirectUri
    // (/callback), navega automaticamente de vuelta a la pagina que inicio
    // el login (/login) - eso se salta CallbackPage antes de que pueda leer
    // el resultado (exito o error). Lo desactivamos para que CallbackPage
    // sea quien decida a donde navegar.
    navigateToLoginRequestUrl: false,
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false,
  },
  system: {
    // Sin esto, msal-browser falla en silencio: ni la UI ni la consola
    // muestran nada cuando algo sale mal procesando el redirect o pidiendo
    // un token (config de tenant/scope invalida, redirect URI no
    // registrada, etc.).
    loggerOptions: {
      logLevel: LogLevel.Verbose,
      loggerCallback: (level, message, containsPii) => {
        if (containsPii) return;
        if (level === LogLevel.Error) console.error("[MSAL]", message);
        else if (level === LogLevel.Warning) console.warn("[MSAL]", message);
        else console.log("[MSAL]", message);
      },
    },
  },
};

export const loginRequest = {
  scopes: [API_SCOPE],
};

export const msalInstance = new PublicClientApplication(msalConfig);

// Listener global (no depende de que CallbackPage este montada) para ver el
// error real de un login/token fallido: el logger interno de MSAL solo
// traza mensajes internos, no el objeto Error que viaja en el evento.
msalInstance.addEventCallback((event) => {
  if (event.eventType === EventType.LOGIN_FAILURE || event.eventType === EventType.ACQUIRE_TOKEN_FAILURE) {
    console.error("[MSAL] fallo:", event.error);
  }
});

// httpClient.js no es un componente React y no puede usar el hook useMsal:
// necesita una forma imperativa de pedir el access token vigente.
export async function getAccessToken() {
  const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!account) return null;

  try {
    const result = await msalInstance.acquireTokenSilent({ ...loginRequest, account });
    return result.accessToken;
  } catch {
    return null;
  }
}
