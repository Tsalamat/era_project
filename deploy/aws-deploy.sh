#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TF_DIR="$ROOT_DIR/infra/aws"

DOMAIN_NAME="${DOMAIN_NAME:-invest-gold.asia}"
AWS_REGION="${AWS_REGION:-eu-central-1}"
CADDY_ACME_EMAIL="${CADDY_ACME_EMAIL:-admin@${DOMAIN_NAME}}"
DEMO_DATA_ENABLED="${DEMO_DATA_ENABLED:-false}"
BOOTSTRAP_ADMIN_EMAIL="${BOOTSTRAP_ADMIN_EMAIL:-admin@${DOMAIN_NAME}}"
BOOTSTRAP_ADMIN_FULL_NAME="${BOOTSTRAP_ADMIN_FULL_NAME:-Админ Тест Магистратура}"
EMAIL_VERIFICATION_ENABLED="${EMAIL_VERIFICATION_ENABLED:-}"
SMTP_HOST="${SMTP_HOST:-}"
SMTP_PORT="${SMTP_PORT:-}"
SMTP_USERNAME="${SMTP_USERNAME:-}"
SMTP_PASSWORD="${SMTP_PASSWORD:-}"
SMTP_FROM="${SMTP_FROM:-$SMTP_USERNAME}"

terraform -chdir="$TF_DIR" init
terraform -chdir="$TF_DIR" apply -auto-approve \
  -var "domain_name=$DOMAIN_NAME" \
  -var "aws_region=$AWS_REGION"

PUBLIC_IP="$(terraform -chdir="$TF_DIR" output -raw public_ip)"
KEY_PATH_RAW="$(terraform -chdir="$TF_DIR" output -raw private_key_path)"
DB_PASSWORD="$(terraform -chdir="$TF_DIR" output -raw db_password)"
JWT_SECRET="$(terraform -chdir="$TF_DIR" output -raw jwt_secret)"
BOOTSTRAP_ADMIN_PASSWORD="$(terraform -chdir="$TF_DIR" output -raw admin_password)"

if [[ "$KEY_PATH_RAW" = /* ]]; then
  KEY_PATH="$KEY_PATH_RAW"
else
  KEY_PATH="$TF_DIR/$KEY_PATH_RAW"
fi

SSH_TARGET="ubuntu@${PUBLIC_IP}"
SSH_OPTS=(
  -i "$KEY_PATH"
  -o StrictHostKeyChecking=accept-new
  -o ServerAliveInterval=20
  -o ServerAliveCountMax=12
)
RSYNC_SSH="ssh -i \"$KEY_PATH\" -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=20 -o ServerAliveCountMax=12"

echo "Waiting for Ubuntu server at ${PUBLIC_IP}..."
until ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "cloud-init status --wait >/dev/null 2>&1"; do
  sleep 10
done

if [[ -z "$SMTP_USERNAME" || -z "$SMTP_PASSWORD" || -z "$SMTP_FROM" ]]; then
  EXISTING_SMTP_ENV="$(ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "if [ -f /opt/test-magistratura/app/.env ]; then grep -E '^(EMAIL_VERIFICATION_ENABLED|SMTP_HOST|SMTP_PORT|SMTP_USERNAME|SMTP_PASSWORD|SMTP_FROM)=' /opt/test-magistratura/app/.env || true; fi")"
  EXISTING_EMAIL_VERIFICATION_ENABLED="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^EMAIL_VERIFICATION_ENABLED=//p' | tail -n 1)"
  EXISTING_SMTP_HOST="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^SMTP_HOST=//p' | tail -n 1)"
  EXISTING_SMTP_PORT="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^SMTP_PORT=//p' | tail -n 1)"
  EXISTING_SMTP_USERNAME="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^SMTP_USERNAME=//p' | tail -n 1)"
  EXISTING_SMTP_PASSWORD="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^SMTP_PASSWORD=//p' | tail -n 1)"
  EXISTING_SMTP_FROM="$(printf '%s\n' "$EXISTING_SMTP_ENV" | sed -n 's/^SMTP_FROM=//p' | tail -n 1)"
  EMAIL_VERIFICATION_ENABLED="${EMAIL_VERIFICATION_ENABLED:-$EXISTING_EMAIL_VERIFICATION_ENABLED}"
  SMTP_HOST="${SMTP_HOST:-$EXISTING_SMTP_HOST}"
  SMTP_PORT="${SMTP_PORT:-$EXISTING_SMTP_PORT}"
  SMTP_USERNAME="${SMTP_USERNAME:-$EXISTING_SMTP_USERNAME}"
  SMTP_PASSWORD="${SMTP_PASSWORD:-$EXISTING_SMTP_PASSWORD}"
  SMTP_FROM="${SMTP_FROM:-$EXISTING_SMTP_FROM}"
fi
EMAIL_VERIFICATION_ENABLED="${EMAIL_VERIFICATION_ENABLED:-false}"
SMTP_HOST="${SMTP_HOST:-smtp.yandex.ru}"
SMTP_PORT="${SMTP_PORT:-465}"
SMTP_FROM="${SMTP_FROM:-$SMTP_USERNAME}"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "mkdir -p /opt/test-magistratura/app"

rsync -az --delete \
  --exclude '.DS_Store' \
  --exclude '.git' \
  --exclude '.gradle' \
  --exclude '.terraform' \
  --exclude 'infra/aws/generated' \
  --exclude 'build' \
  --exclude 'bin' \
  --exclude 'data' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/.nuxt' \
  --exclude 'frontend/.output' \
  --exclude 'frontend/.cache' \
  --exclude 'frontend/npm-debug.log*' \
  -e "$RSYNC_SSH" \
  "$ROOT_DIR/" "$SSH_TARGET:/opt/test-magistratura/app/"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "cat > /opt/test-magistratura/app/.env" <<EOF
POSTGRES_DB=kta
POSTGRES_USER=kta
POSTGRES_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
PUBLIC_HOST=${DOMAIN_NAME}
CADDY_ACME_EMAIL=${CADDY_ACME_EMAIL}
DEMO_DATA_ENABLED=${DEMO_DATA_ENABLED}
BOOTSTRAP_ADMIN_EMAIL=${BOOTSTRAP_ADMIN_EMAIL}
BOOTSTRAP_ADMIN_PASSWORD=${BOOTSTRAP_ADMIN_PASSWORD}
BOOTSTRAP_ADMIN_FULL_NAME=${BOOTSTRAP_ADMIN_FULL_NAME}
EMAIL_VERIFICATION_ENABLED=${EMAIL_VERIFICATION_ENABLED}
SMTP_HOST=${SMTP_HOST}
SMTP_PORT=${SMTP_PORT}
SMTP_USERNAME=${SMTP_USERNAME}
SMTP_PASSWORD=${SMTP_PASSWORD}
SMTP_FROM=${SMTP_FROM}
EOF

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "cd /opt/test-magistratura/app && docker compose --env-file .env -f deploy/docker-compose.prod.yml up -d --build"

echo "IP: ${PUBLIC_IP}"
echo "URL: https://${DOMAIN_NAME}"
echo "DNS A record required: ${DOMAIN_NAME} -> ${PUBLIC_IP}"
