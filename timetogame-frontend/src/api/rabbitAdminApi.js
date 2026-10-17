import { rabbitAdminClient } from "./httpClient";

const enc = encodeURIComponent;

export function fetchColas() {
  return rabbitAdminClient.get("/queues");
}

export function createCola(cola) {
  return rabbitAdminClient.post("/queues", cola);
}

export function deleteCola(nombre, { soloSiSinConsumidores = false, soloSiVacia = false } = {}) {
  const params = new URLSearchParams({ soloSiSinConsumidores, soloSiVacia });
  return rabbitAdminClient.delete(`/queues/${enc(nombre)}?${params}`);
}

export function fetchExchanges() {
  return rabbitAdminClient.get("/exchanges");
}

export function createExchange(exchange) {
  return rabbitAdminClient.post("/exchanges", exchange);
}

export function deleteExchange(nombre, { soloSiSinBindings = false } = {}) {
  const params = new URLSearchParams({ soloSiSinBindings });
  return rabbitAdminClient.delete(`/exchanges/${enc(nombre)}?${params}`);
}

export function fetchBindings() {
  return rabbitAdminClient.get("/bindings");
}

export function createBinding(binding) {
  return rabbitAdminClient.post("/bindings", binding);
}

export function deleteBinding({ exchange, cola, routingKey }) {
  const params = new URLSearchParams({ routingKey: routingKey ?? "" });
  return rabbitAdminClient.delete(`/bindings/${enc(exchange)}/${enc(cola)}?${params}`);
}
