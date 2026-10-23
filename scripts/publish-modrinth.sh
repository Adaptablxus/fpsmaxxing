#!/usr/bin/env bash
set -euo pipefail

: "${VERSION:?VERSION is required}"
: "${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

PROJECT_ID="kA3JYFQu"
JAR_PATH="FPSMaxx-${VERSION}-mc26.2.jar"
VERSION_DATA="version-data.json"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Missing ${JAR_PATH}" >&2
  exit 1
fi

jq -n \
  --arg name "FPSMaxx ${VERSION}" \
  --arg version "${VERSION}" \
  --arg changelog "FPSMaxx ${VERSION}. See https://github.com/Adaptablxus/fpsmaxxing/releases/tag/v${VERSION}" \
  --arg project_id "${PROJECT_ID}" \
  '{
    name: $name,
    version_number: $version,
    changelog: $changelog,
    dependencies: [
      { project_id: "P7dR8mSH", dependency_type: "required" },
      { project_id: "AANobbMI", dependency_type: "optional" }
    ],
    game_versions: ["26.2"],
    version_type: "release",
    loaders: ["fabric"],
    featured: false,
    status: "listed",
    project_id: $project_id,
    file_parts: ["file"],
    primary_file: "file",
    environment: "client_only"
  }' > "${VERSION_DATA}"

curl --fail-with-body --retry 3 --retry-delay 2 \
  -X POST "https://api.modrinth.com/v2/version" \
  -H "Authorization: Bearer ${MODRINTH_TOKEN}" \
  -H "User-Agent: FPSMaxx-release-workflow" \
  -F "data=<${VERSION_DATA};type=application/json" \
  -F "file=@${JAR_PATH};type=application/java-archive"
