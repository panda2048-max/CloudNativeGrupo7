import { httpClient } from "./httpClient";

export function fetchPublicGeneros() {
  return httpClient.get("/public/generos", { auth: false });
}

export function fetchGeneros() {
  return httpClient.get("/generos");
}

export function createGenero(genero) {
  return httpClient.post("/generos", genero);
}

export function updateGenero(id, genero) {
  return httpClient.put(`/generos/${id}`, genero);
}

export function deleteGenero(id) {
  return httpClient.delete(`/generos/${id}`);
}
