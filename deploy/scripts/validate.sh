#!/usr/bin/env bash
set -euo pipefail

# 앱 헬스체크
for i in {1..30}; do
  if curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo "App is healthy."
    exit 0
  fi
  echo "Waiting for app to be healthy... ($i/30)"
  sleep 5
done

echo "Validation failed."
exit 1
