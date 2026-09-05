import { API_BASE_URL } from "../config";
import { getAccessToken } from "../auth/oidcUserManager";

export class ApiError extends Error {
  constructor(status, message, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

let unauthorizedHandler = null;

export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

function defaultMessageFor(status) {
  switch (status) {
    case 400:
      return "La solicitud tiene datos invalidos.";
    case 401:
      return "Debes iniciar sesion para continuar.";
    case 403:
      return "No tienes permisos suficientes para esta accion.";
    case 404:
      return "El recurso solicitado no existe.";
    default:
      return "Ocurrio un error inesperado.";
  }
}

async function request(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };

  if (auth) {
    const token = await getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      0,
      "No se pudo conectar con el backend. Verifica que este disponible."
    );
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    const message = data?.mensaje ?? defaultMessageFor(response.status);
    throw new ApiError(response.status, message, data?.errores);
  }

  return data;
}

export const httpClient = {
  get: (path, opts) => request(path, { ...opts, method: "GET" }),
  post: (path, body, opts) => request(path, { ...opts, method: "POST", body }),
  put: (path, body, opts) => request(path, { ...opts, method: "PUT", body }),
  delete: (path, opts) => request(path, { ...opts, method: "DELETE" }),
};
