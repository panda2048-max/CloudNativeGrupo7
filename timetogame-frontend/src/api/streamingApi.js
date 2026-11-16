import { httpClient, rabbitAdminClient } from "./httpClient";

export function fetchHistorialReciente() {
  return httpClient.get("/historial/recientes");
}

/** 202: las vistas se encolan en Kafka; el ranking se actualiza solo por SSE. */
export function simularVistas(vistas) {
  return httpClient.post(`/streaming/simulacion?vistas=${encodeURIComponent(vistas)}`);
}

// ---- Administracion de Kafka (microservicio administrador)

const enc = encodeURIComponent;

export function fetchKafkaCluster() {
  return rabbitAdminClient.get("/kafka/cluster");
}

export function fetchTopicos() {
  return rabbitAdminClient.get("/kafka/topics");
}

export function fetchTopico(nombre) {
  return rabbitAdminClient.get(`/kafka/topics/${enc(nombre)}`);
}

export function createTopico(topico) {
  return rabbitAdminClient.post("/kafka/topics", topico);
}

export function updateTopico(nombre, cambios) {
  return rabbitAdminClient.patch(`/kafka/topics/${enc(nombre)}`, cambios);
}

export function deleteTopico(nombre) {
  return rabbitAdminClient.delete(`/kafka/topics/${enc(nombre)}`);
}

export function fetchGrupos() {
  return rabbitAdminClient.get("/kafka/consumer-groups");
}

export function fetchGrupo(groupId) {
  return rabbitAdminClient.get(`/kafka/consumer-groups/${enc(groupId)}`);
}
