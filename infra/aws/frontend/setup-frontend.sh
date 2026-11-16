#!/usr/bin/env bash
# Prepara una EC2 Ubuntu nueva como servidor del frontend: Nginx + HTTPS con
# Let's Encrypt sobre <ip>.nip.io. Se ejecuta UNA vez en la EC2:
#
#   IP_ELASTICA=1.2.3.4 IP_BACKEND=5.6.7.8 CORREO=tu@correo.com bash setup-frontend.sh
#
# Antes: la IP elastica asociada a la instancia y el Security Group con 80 y
# 443 abiertos (certbot valida por el puerto 80). El build se sube despues
# desde el PC (ver infra/aws/README.md, paso "Frontend").
set -euo pipefail

: "${IP_ELASTICA:?Definir IP_ELASTICA}"
: "${IP_BACKEND:?Definir IP_BACKEND (IP elastica del backend, para el proxy SSE)}"
: "${CORREO:?Definir CORREO (avisos de vencimiento de Let's Encrypt)}"
DOMINIO="$IP_ELASTICA.nip.io"

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq nginx certbot python3-certbot-nginx >/dev/null
sudo mkdir -p /var/www/timetogame

sudo tee /etc/nginx/sites-available/timetogame >/dev/null <<EOF
server {
    listen 80;
    server_name $DOMINIO;

    root /var/www/timetogame;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Tendencias en vivo (SSE) directo al backend: el API Gateway no mantiene
    # conexiones abiertas.
    location /api/public/stream/ {
        proxy_pass http://$IP_BACKEND:8000;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 1h;
    }
}
EOF
sudo ln -sf /etc/nginx/sites-available/timetogame /etc/nginx/sites-enabled/timetogame
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

sudo certbot --nginx -d "$DOMINIO" --non-interactive --agree-tos --email "$CORREO" --redirect
echo "Listo: https://$DOMINIO (falta subir el build)"
