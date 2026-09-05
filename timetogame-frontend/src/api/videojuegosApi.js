import { httpClient } from "./httpClient";

export function fetchPublicVideojuegos() {
  return httpClient.get("/public/videojuegos", { auth: false });
}

export function fetchPublicVideojuego(id) {
  return httpClient.get(`/public/videojuegos/${id}`, { auth: false });
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
