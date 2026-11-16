#!/usr/bin/env bash
# Prepara una EC2 Ubuntu nueva como servidor backend de TimeToGame: Java,
# Docker, cluster RabbitMQ, secretos, servicios systemd y compilacion.
# Se ejecuta UNA vez en la EC2, como el usuario ubuntu:
#
#   curl -fsSLO https://raw.githubusercontent.com/panda2048-max/CloudNativeGrupo7/main/infra/aws/backend/setup-backend.sh
#   RDS_ENDPOINT=database-1.xxxx.us-east-1.rds.amazonaws.com \
#   ORIGEN_FRONTEND=https://<ip-frontend>.nip.io \
#   KAFKA_BOOTSTRAP=<ip-privada-kafka>:9092,<ip-privada-kafka>:9093,<ip-privada-kafka>:9094 \
#   bash setup-backend.sh
#
# Pide la contrasena de RDS por teclado (no queda en el historial). Las de
# RabbitMQ las genera al azar. Todo secreto queda en /etc/timetogame (solo root)
# y en ~/app/infra/rabbitmq/.env (solo ubuntu).
set -euo pipefail

: "${RDS_ENDPOINT:?Definir RDS_ENDPOINT}"
: "${ORIGEN_FRONTEND:?Definir ORIGEN_FRONTEND (ej. https://1.2.3.4.nip.io)}"
: "${KAFKA_BOOTSTRAP:?Definir KAFKA_BOOTSTRAP (brokers de la EC2 de Kafka)}"
REPO_URL="${REPO_URL:-https://github.com/panda2048-max/CloudNativeGrupo7.git}"
RAMA="${RAMA:-main}"
APP=/home/ubuntu/app

echo "== Paquetes"
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  openjdk-25-jdk-headless git mysql-client docker.io docker-compose-v2 >/dev/null
sudo systemctl enable --now docker >/dev/null

echo "== Swap de 2 GB (t3.small: 2 JVM + 2 nodos RabbitMQ)"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null && sudo swapon /swapfile
  echo "/swapfile none swap sw 0 0" | sudo tee -a /etc/fstab >/dev/null
fi

echo "== Codigo"
[ -d "$APP/.git" ] || git clone -q -b "$RAMA" "$REPO_URL" "$APP"
git -C "$APP" config core.fileMode false
chmod +x "$APP"/timetogame-backend/mvnw "$APP"/timetogame-rabbitmq-admin/mvnw "$APP"/infra/rabbitmq/crear-usuario.sh

echo "== Secretos"
sudo mkdir -p /etc/timetogame && sudo chmod 700 /etc/timetogame
if ! sudo test -f /etc/timetogame/backend.env; then
  read -r -s -p "Contrasena del usuario 'timetogame' de RDS: " DB_PASS; echo
  printf "DB_USERNAME=timetogame\nDB_PASSWORD=%s\n" "$DB_PASS" | sudo tee /etc/timetogame/backend.env >/dev/null
fi
for app in backend admin; do
  if ! sudo test -f /etc/timetogame/rabbitmq-$app.env; then
    printf "RABBITMQ_USERNAME=timetogame-%s\nRABBITMQ_PASSWORD=%s\n" "$app" "$(openssl rand -hex 24)" \
      | sudo tee /etc/timetogame/rabbitmq-$app.env >/dev/null
  fi
done
sudo sh -c "chmod 600 /etc/timetogame/*.env"

echo "== Cluster RabbitMQ (docker-compose, 2 nodos)"
cd "$APP/infra/rabbitmq"
if [ ! -f .env ]; then
  (umask 077; printf "RABBITMQ_ERLANG_COOKIE=%s\nRABBITMQ_DEFAULT_USER=timetogame-ops\nRABBITMQ_DEFAULT_PASS=%s\n" \
    "$(openssl rand -hex 32)" "$(openssl rand -hex 20)" > .env)
fi
sudo docker compose up -d
for i in $(seq 1 40); do
  [ "$(sudo docker inspect -f '{{.State.Health.Status}}' rabbitmq-2 2>/dev/null)" = healthy ] && break
  sleep 3
done
sudo sed -n "s/^RABBITMQ_PASSWORD=//p" /etc/timetogame/rabbitmq-backend.env | tr -d "\n" \
  | sudo ./crear-usuario.sh timetogame-backend
sudo sed -n "s/^RABBITMQ_PASSWORD=//p" /etc/timetogame/rabbitmq-admin.env | tr -d "\n" \
  | sudo ./crear-usuario.sh timetogame-admin management

echo "== Servicios systemd"
for unit in timetogame-backend timetogame-rabbitmq-admin; do
  sed -e "s|<ENDPOINT_RDS>|$RDS_ENDPOINT|" -e "s|<ORIGEN_FRONTEND>|$ORIGEN_FRONTEND|" \
    -e "s|<KAFKA_BOOTSTRAP>|$KAFKA_BOOTSTRAP|" \
    "$APP/infra/aws/backend/$unit.service" | sudo tee /etc/systemd/system/$unit.service >/dev/null
done
sudo systemctl daemon-reload

echo "== Compilacion"
(cd "$APP/timetogame-backend" && ./mvnw -q clean package -DskipTests)
(cd "$APP/timetogame-rabbitmq-admin" && ./mvnw -q clean package -DskipTests)

echo "== Arranque (uno tras otro para no saturar la CPU)"
sudo systemctl enable --now timetogame-backend
sleep 40
sudo systemctl enable --now timetogame-rabbitmq-admin
sleep 25
systemctl is-active timetogame-backend timetogame-rabbitmq-admin
curl -s -o /dev/null -w "backend :8000 -> %{http_code} (401 = OK sin token)\n" localhost:8000/api/videojuegos
curl -s -o /dev/null -w "admin   :8001 -> %{http_code} (401 = OK sin token)\n" localhost:8001/api/admin/queues
