#!/usr/bin/env bash
set -euo pipefail

: "${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

PROJECT_ID="kA3JYFQu"
SUMMARY="Client-side Fabric and NeoForge optimization for Minecraft 26.3 with a hidden FpsMaxx graphics preset, 64-chunk render distance, adaptive FPS governor, culling, chunk budgeting, Fresh & Smooth support, and Sodium/Iris integration."
DATA_FILE="project-metadata.json"

jq -n \
  --arg description "${SUMMARY}" \
  --rawfile body MODRINTH.md \
  '{
    description: $description,
    body: $body
  }' > "${DATA_FILE}"

curl --fail-with-body --retry 3 --retry-delay 2 \
  -X PATCH "https://api.modrinth.com/v2/project/${PROJECT_ID}" \
  -H "Authorization: Bearer ${MODRINTH_TOKEN}" \
  -H "User-Agent: FPSMaxx-metadata-workflow" \
  -H "Content-Type: application/json" \
  --data-binary "@${DATA_FILE}"
