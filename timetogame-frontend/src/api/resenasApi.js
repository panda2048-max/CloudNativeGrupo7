import { httpClient } from "./httpClient";

/** Promedio, total y ultimas resenas de un videojuego (publico). */
export function fetchResenas(videojuegoId) {
  return httpClient.get(`/public/videojuegos/${videojuegoId}/resenas`, { auth: false });
}

/** Requiere sesion; el autor lo toma el backend del token. */
export function crearResena(videojuegoId, resena) {
  return httpClient.post(`/videojuegos/${videojuegoId}/resenas`, resena);
}
