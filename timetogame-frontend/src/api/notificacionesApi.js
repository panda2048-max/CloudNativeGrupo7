import { httpClient } from "./httpClient";

export function fetchNotificaciones() {
  return httpClient.get("/notificaciones");
}

export function fetchMensajesFallidos() {
  return httpClient.get("/mensajes-fallidos");
}
