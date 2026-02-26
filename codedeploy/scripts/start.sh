#!/bin/bash
# start.sh
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"
CONFIG_FILE="${APP_DIR}/application-staging.yml"

# 예시 포트 8080, env는 EC2에 이미 존재(또는 SSM/Secrets)한다고 가정
docker run -d \
	--name be-api \
	--restart=always \
	-p 8080:8080 \
	-e SPRING_PROFILES_ACTIVE=staging \
    -e SPRING_CONFIG_ADDITIONAL_LOCATION="file:/config/" \
  -v "${CONFIG_FILE}:/config/application-staging.yml:ro" \
  "${IMAGE_URI}"