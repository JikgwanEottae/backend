#!/usr/bin/env bash
set -euo pipefail

cd /home/ubuntu/apps/yagu

# 최신 이미지 풀
docker compose -f docker-compose.ec2.yml pull

# 기동
docker compose -f docker-compose.ec2.yml up -d

# 상태 출력
docker compose -f docker-compose.ec2.yml ps
