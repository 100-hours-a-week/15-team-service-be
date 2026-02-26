#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="ap-northeast-2"
OUT_FILE="/home/ubuntu/deploy/be/application-staging.yml"
PARAM_BASE="/commitme/v2/prod/be-server"
PARAM_BASE_STAGING="/commitme/v2/staging/be-server"

umask 077

# SSM 값 조회 함수 (SecureString 복호화 포함)
get_ssm() {
  local name="$1"
  aws ssm get-parameter \
    --region "$AWS_REGION" \
    --name "$name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text
}

# 1) SSM에서 "변하는 값"만 읽기
DOMAIN="$(get_ssm "${PARAM_BASE_STAGING}/DOMAIN")"
DOMAIN_CSRF="$(get_ssm "${PARAM_BASE_STAGING}/DOMAIN_CSRF")"

DB_URL="$(get_ssm "${PARAM_BASE}/PROD_DB_NAME")"
DB_USER="$(get_ssm "${PARAM_BASE}/PROD_MYSQL_ID")"
DB_PASS="$(get_ssm "${PARAM_BASE}/PROD_MYSQL_PW")"

JWT_SECRET="$(get_ssm "${PARAM_BASE}/JWT_SECRET")"
ACCESS_TOKEN_KEY="$(get_ssm "${PARAM_BASE}/ACCESS_TOKEN_KEY")"

GITHUB_CLIENT_ID="$(get_ssm "${PARAM_BASE}/GITHUB_CLIENT_ID")"
GITHUB_CLIENT_SECRET="$(get_ssm "${PARAM_BASE}/GITHUB_CLIENT_SECRET")"
GITHUB_REDIRECT_URI="$(get_ssm "${PARAM_BASE_STAGING}/GITHUB_OAUTH_REDIRECT")"

APP_AUTH_REDIRECT_URI="$(get_ssm "${PARAM_BASE_STAGING}/FE_OAUTH_REDIRECT")"

S3_BUCKET_NAME="$(get_ssm "${PARAM_BASE}/S3_BUCKET_NAME")"
S3_ACCESS_KEY="$(get_ssm "${PARAM_BASE}/S3_ACCESS_KEY")"
S3_SECRET_KEY="$(get_ssm "${PARAM_BASE}/S3_SECRET_KEY")"

AI_BASE_URL="$(get_ssm "${PARAM_BASE}/AI_BASE_URL")"
AI_GENERATE_PATH="$(get_ssm "${PARAM_BASE}/AI_GENERATE_PATH")"
AI_EDIT_PATH="$(get_ssm "${PARAM_BASE}/AI_EDIT_PATH")"
AI_CALLBACK_PATH="$(get_ssm "${PARAM_BASE_STAGING}/AI_CALLBACK_PATH")"

REDIS_IP="$(get_ssm "${PARAM_BASE}/REDIS_IP")"
REDIS_PW="$(get_ssm "${PARAM_BASE}/REDIS_PW")"

# 2) application-prod.yml 생성 (prod에서 바뀌는 것만 override)
cat > "$OUT_FILE" <<YAML
server:
  port: 8080

spring:
  application:
    name: CommitMe
  output:
    ansi:
      enabled: always
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: "${DB_URL}"
    username: "${DB_USER}"
    password: "${DB_PASS}"
  data:
    redis:
      host: "${REDIS_IP}"
      port: 6379
      password: "${REDIS_PW}"
    
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
    show-sql: false
    
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: "20260204.0"
    
logging:
  level:
    org.hibernate.SQL: off
    org.hibernate.type.descriptor.sql: off
    net.ttddyy.dsproxy.listener: info

management:
  endpoints:
    web:
      exposure:
        include: health
        
app:
  auth:
    redirect-uri: "${APP_AUTH_REDIRECT_URI}"
  cors:
    allowed-origins: "${DOMAIN}"
    allowed-methods:
      - "GET"
      - "POST"
      - "PATCH"
      - "DELETE"
      - "OPTIONS"
    allowed-headers:
      - "*"
    exposed-headers:
      - "Set-Cookie"
    allow-credentials: true
    max-age: 8600
  s3:
    bucket: "${S3_BUCKET_NAME}"
    region: "ap-northeast-2"
    access-key: "${S3_ACCESS_KEY}"
    secret-key: "${S3_SECRET_KEY}"
    cdn-base-url: "https://cdn.commit-me.com"
    presign-duration-minutes: 30

security:
  jwt:
    secret: "${JWT_SECRET}"
    access-expiration: 1h
    refresh-expiration: 7d
  cookie:
    secure: true
  csrf:
    cookie-domain: "${DOMAIN_CSRF}"
  crypto:
    access-token-key: "${ACCESS_TOKEN_KEY}"

github:
  client-id: "${GITHUB_CLIENT_ID}"
  client-secret: "${GITHUB_CLIENT_SECRET}"
  redirect-uri: "${GITHUB_REDIRECT_URI}"
  scope: "read:user repo"
  
ai:
  base-url: "${AI_BASE_URL}"
  resume-generate-path: "${AI_GENERATE_PATH}"
  resume-edit-path: "${AI_EDIT_PATH}"
  resume-callback-url: "${AI_CALLBACK_PATH}"
  callback-secret: "dev-secret"
    
YAML

echo "[OK] wrote $OUT_FILE"