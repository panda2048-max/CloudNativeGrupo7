import { httpClient } from "./httpClient";

export function login(username, password) {
  return httpClient.post("/auth/login", { username, password }, { auth: false });
}
