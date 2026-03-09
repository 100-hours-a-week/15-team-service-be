#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
BACKEND_IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"
REGION="ap-northeast-2"
ALLOY_ECR_REPOSITORY="commitme/alloy"
ALLOY_ECR_TAG="latest"

echo "Logging in to ECR..."
# Extract registry URL from BACKEND_IMAGE_URI (everything before the first slash)
REGISTRY_URL=$(echo "$BACKEND_IMAGE_URI" | cut -d'/' -f1)
ALLOY_IMAGE_URI="${REGISTRY_URL}/${ALLOY_ECR_REPOSITORY}:${ALLOY_ECR_TAG}"

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "$REGISTRY_URL"

echo "Pulling backend image: ${BACKEND_IMAGE_URI}"
docker pull "${BACKEND_IMAGE_URI}"

echo "Pulling alloy image: ${ALLOY_IMAGE_URI}"
docker pull "${ALLOY_IMAGE_URI}"

echo "${ALLOY_IMAGE_URI}" > "${APP_DIR}/alloy-image-uri.txt"
