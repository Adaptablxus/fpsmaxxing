#!/usr/bin/env bash
set -euo pipefail

: "${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

PROJECT_ID="kA3JYFQu"
SUMMARY="Client-side Fabric/NeoForge optimization for MC 26.3: FpsMaxx preset, 64-chunk render, adaptive FPS/sim/cull/resolution governors, benchmark, GPU-aware profiles, culling, chunk budget, Fresh & Smooth and Sodium/Iris support."
DATA_FILE="project-metadata.json"

jq -n \
  --arg description "${SUMMARY}" \
  --rawfile body MODRINTH.md \
  '{
    description: $description,
    body: $body,
    requested_status: "approved"
  }' > "${DATA_FILE}"

RESPONSE_FILE="project-metadata-response.json"
HTTP_CODE=$(curl --silent --show-error --retry 3 --retry-delay 2 \
  --output "${RESPONSE_FILE}" --write-out '%{http_code}' \
  -X PATCH "https://api.modrinth.com/v2/project/${PROJECT_ID}" \
  -H "Authorization: Bearer ${MODRINTH_TOKEN}" \
  -H "User-Agent: FPSMaxx-metadata-workflow" \
  -H "Content-Type: application/json" \
  --data-binary "@${DATA_FILE}")

if [[ "${HTTP_CODE}" -lt 200 || "${HTTP_CODE}" -ge 300 ]]; then
  echo "::error::Modrinth PATCH failed (HTTP ${HTTP_CODE}): $(cat "${RESPONSE_FILE}")"
  exit 1
fi

echo "Modrinth project metadata updated (HTTP ${HTTP_CODE})."