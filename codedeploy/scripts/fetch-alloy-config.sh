#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="ap-northeast-2"
APP_DIR="/home/ubuntu/deploy/be"
ALLOY_CONFIG_DIR="${APP_DIR}/alloy"
ALLOY_CONFIG_FILE="${ALLOY_CONFIG_DIR}/alloy.config"
ALLOY_CONFIG_S3_URI="s3://commit-me-deploy/be-server/alloy/alloy.config"

mkdir -p "${ALLOY_CONFIG_DIR}"
aws s3 cp --region "${AWS_REGION}" "${ALLOY_CONFIG_S3_URI}" "${ALLOY_CONFIG_FILE}"

echo "[OK] wrote ${ALLOY_CONFIG_FILE}"
