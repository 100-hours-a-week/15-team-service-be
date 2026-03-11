#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
export BACKEND_IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"
export ALLOY_IMAGE_URI="$(cat "${APP_DIR}/alloy-image-uri.txt")"
export SPRING_ACTIVE_PROFILE="staging"
export SPRING_CONFIG_FILE="${APP_DIR}/application-staging.yml"
export SPRING_CONFIG_BASENAME="application-staging.yml"
export ALLOY_CONFIG_FILE="${APP_DIR}/alloy/alloy.config"

docker compose -f "${APP_DIR}/docker-compose.yml" up -d --remove-orphans
