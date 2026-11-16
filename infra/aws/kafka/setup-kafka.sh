#!/usr/bin/env bash
# Prepara una EC2 Ubuntu nueva como servidor de Kafka: Docker, swap y los
# compose del cluster (3 Zookeeper + 3 brokers) y de Kafka UI. Se ejecuta UNA
# vez en la EC2, como ubuntu:
#
#   curl -fsSLO https://raw.githubusercontent.com/panda2048-max/CloudNativeGrupo7/main/infra/aws/kafka/setup-kafka.sh
#   bash setup-kafka.sh
#
# Los brokers se anuncian con la IP privada de esta EC2: el backend (en otra
# EC2 de la misma VPC) se conecta a <ip-privada>:9092,9093,9094.
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/panda2048-max/CloudNativeGrupo7.git}"
RAMA="${RAMA:-main}"
APP=/home/ubuntu/app

echo "== Paquetes"
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq git docker.io docker-compose-v2 >/dev/null
sudo systemctl enable --now docker >/dev/null

echo "== Swap de 2 GB"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null && sudo swapon /swapfile
  echo "/swapfile none swap sw 0 0" | sudo tee -a /etc/fstab >/dev/null
fi

echo "== Codigo"
[ -d "$APP/.git" ] || git clone -q -b "$RAMA" "$REPO_URL" "$APP"

echo "== Cluster Kafka"
IP_PRIVADA=$(hostname -I | awk '{print $1}')
cd "$APP/infra/kafka"
[ -f .env ] || echo "KAFKA_HOST=$IP_PRIVADA" > .env
sudo docker compose up -d

echo "== Esperando a los 3 brokers"
for i in $(seq 1 60); do
  # </dev/null: en una terminal interactiva "compose exec" espera que se cierre
  # la entrada aunque el comando ya termino. timeout: si el broker aun no
  # responde, kafka-broker-api-versions espera para siempre.
  BROKERS=$(timeout 30 sudo docker compose exec -T kafka-1 \
    kafka-broker-api-versions --bootstrap-server kafka-1:29092 </dev/null 2>/dev/null \
    | grep -c "^kafka-" || true)
  [ "$BROKERS" -ge 3 ] && break
  sleep 5
done
echo "Brokers registrados: ${BROKERS:-0}"

echo "== Kafka UI"
cd "$APP/infra/kafka-ui"
sudo docker compose up -d </dev/null

echo
echo "Listo. En la EC2 del backend usar:"
echo "  KAFKA_BOOTSTRAP_SERVERS=$IP_PRIVADA:9092,$IP_PRIVADA:9093,$IP_PRIVADA:9094"
echo "Kafka UI: ssh -i <llave>.pem -L 8080:localhost:8080 ubuntu@<ip-publica-de-esta-ec2>  ->  http://localhost:8080"
