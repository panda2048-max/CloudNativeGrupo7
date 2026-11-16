import { httpClient } from "./httpClient";

export function fetchPublicVideojuegos() {
  return httpClient.get("/public/videojuegos", { auth: false });
}

/**
 * Publico, pero si hay sesion manda el token: abrir la ficha cuenta como una
 * vista (Kafka) y, con usuario conocido, queda en su historial.
 */
export function fetchPublicVideojuego(id) {
  return httpClient.get(`/public/videojuegos/${id}`);
}

export function fetchVideojuegos() {
  return httpClient.get("/videojuegos");
}

export function fetchVideojuego(id) {
  return httpClient.get(`/videojuegos/${id}`);
}

export function createVideojuego(videojuego) {
  return httpClient.post("/videojuegos", videojuego);
}

export function updateVideojuego(id, videojuego) {
  return httpClient.put(`/videojuegos/${id}`, videojuego);
}

export function deleteVideojuego(id) {
  return httpClient.delete(`/videojuegos/${id}`);
}

export function fetchFichaVideojuego(id) {
  return httpClient.get(`/videojuegos/${id}/ficha`);
}

/** 202 Accepted: la ficha se genera de forma asincrona (RabbitMQ). */
export function solicitarFichaVideojuego(id) {
  return httpClient.post(`/videojuegos/${id}/ficha`);
}
