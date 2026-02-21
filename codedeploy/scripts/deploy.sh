#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"
REGION="ap-northeast-2"

echo "Logging in to ECR..."
# Extract registry URL from IMAGE_URI (everything before the first slash)
REGISTRY_URL=$(echo "$IMAGE_URI" | cut -d'/' -f1)
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "$REGISTRY_URL"

docker pull "${IMAGE_URI}"