#!/usr/bin/env bash
set -euo pipefail

cd /home/ubuntu/apps/yagu

# 기존 컨테이너 내려주기(있다면)
if docker ps --format '{{.Names}}' | grep -q '^yagu-app$'; then
  docker compose -f docker-compose.ec2.yml down || true
fi
