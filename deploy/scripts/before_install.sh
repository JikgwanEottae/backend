#!/usr/bin/env bash
set -euo pipefail

# 폴더 보장
mkdir -p /home/ubuntu/apps/yagu

# Docker 로그인 (필요시)
if [ -n "${DOCKERHUB_USERNAME:-}" ] && [ -n "${DOCKERHUB_TOKEN:-}" ]; then
  echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin
fi
