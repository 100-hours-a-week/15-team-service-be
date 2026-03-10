#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
export BACKEND_IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"
export ALLOY_IMAGE_URI="$(cat "${APP_DIR}/alloy-image-uri.txt")"
export SPRING_ACTIVE_PROFILE="prod"
export SPRING_CONFIG_FILE="${APP_DIR}/application-prod.yml"
export SPRING_CONFIG_BASENAME="application-prod.yml"
export ALLOY_CONFIG_FILE="${APP_DIR}/alloy/alloy.config"

TOKEN=$(curl -sS -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")

INSTANCE_ID=$(curl -sS -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/instance-id)

export ALLOY_INSTANCE_ID="$INSTANCE_ID"

docker compose -f "${APP_DIR}/docker-compose.yml" up -d --remove-orphans
