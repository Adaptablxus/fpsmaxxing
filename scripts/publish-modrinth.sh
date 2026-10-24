#!/usr/bin/env bash
set -euo pipefail

: "${VERSION:?VERSION is required}"
: "${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

PROJECT_ID="kA3JYFQu"
FABRIC_JAR="FPSMaxx-${VERSION}-fabric-mc26.3.jar"
NEOFORGE_JAR="FPSMaxx-${VERSION}-neoforge-mc26.3.jar"

for jar in "${FABRIC_JAR}" "${NEOFORGE_JAR}"; do
  if [[ ! -f "${jar}" ]]; then
    echo "Missing ${jar}" >&2
    exit 1
  fi
done

# The project is still under review. While that is true the public API returns
# 404 even with a valid token, so skip cleanly instead of failing the release.
PROJECT_STATUS=$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
  "https://api.modrinth.com/v2/project/${PROJECT_ID}" \
  -H "Authorization: Bearer ${MODRINTH_TOKEN}" \
  -H "User-Agent: FPSMaxx-release-workflow")
if [[ "${PROJECT_STATUS}" == "404" ]]; then
  echo "Modrinth project ${PROJECT_ID} is not publicly available yet; skipping upload."
  exit 0
fi

CHANGELOG="FPSMaxx ${VERSION} adds Fabric and NeoForge builds for Minecraft 26.3.

How it optimizes:
- Hidden FpsMaxx preset: drag the Graphics Preset slider all the way left. The FpsMaxx position applies the adaptive governor, all cull distances, the particle budget and the chunk-update budget together with the low-end graphics reductions.
- Low-end preset: /fpsmaxx lowend applies Fast graphics, clouds off, cloud distance 2, minimal particles, render 16, simulation 8 and a 60 FPS target.
- Potato preset: /fpsmaxx potato goes to render 8, simulation 5, tiny cull distances and a 24-section update budget.
- Adaptive chunk-section culling: chunks stay loaded, but only the affordable visible radius is submitted to the renderer.
- Chunk-update budgeting: caps mesh rebuilds per frame and re-queues the rest, smoothing stutter.
- Particle culling and budgeting: rejects and removes distant particles, and shrinks the live cap when FPS falls.
- Sodium integration: caps Sodium's terrain search distance and fog to the same radius without unloading chunks, and adds a searchable FPSMaxx page with an Open FPSMaxx preset screen button to Sodium's Video Settings.
- Fresh & Smooth aware: auto-detects the pack and keeps its shaders, particles and animations with a visual-safe profile.
- Particle optimizer aware: defers particle culling to AsyncParticles or Particle Core when installed.
- Entity and block-entity culling: skips distant entities before extraction and submission.
- Memory guard: prevents small heaps from loading more chunks than they can safely hold.
- Chunk retention: visible mode avoids repeated load/unload stutter.

Loader notes:
- Fabric: Sodium 0.9.2+ for 26.3 works. The jar also runs on Quilt.
- NeoForge: use Sodium NeoForge 0.9.3-beta.1+ for 26.3. Sodium NeoForge 0.9.2 has a broken LevelExtractor mixin on 26.3.

Full notes: https://github.com/Adaptablxus/fpsmaxxing/releases/tag/v${VERSION}"

upload_version() {
  local loader="$1"
  local jar="$2"
  local name="$3"
  local version_number="$4"
  local dependencies="$5"
  local data="version-${loader}.json"
  local response="version-${loader}-response.json"

  jq -n \
    --arg name "${name}" \
    --arg version_number "${version_number}" \
    --arg changelog "${CHANGELOG}" \
    --arg project_id "${PROJECT_ID}" \
    --argjson dependencies "${dependencies}" \
    --arg loader "${loader}" \
    '{
      name: $name,
      version_number: $version_number,
      changelog: $changelog,
      dependencies: $dependencies,
      game_versions: ["26.3"],
      version_type: "release",
      loaders: [$loader],
      featured: false,
      status: "listed",
      project_id: $project_id,
      file_parts: ["file"],
      primary_file: "file",
      environment: "client_only"
    }' > "${data}"

  local http_code
  http_code=$(curl --silent --show-error --retry 3 --retry-delay 2 \
    --output "${response}" --write-out '%{http_code}' \
    -X POST "https://api.modrinth.com/v2/version" \
    -H "Authorization: Bearer ${MODRINTH_TOKEN}" \
    -H "User-Agent: FPSMaxx-release-workflow" \
    -F "data=<${data};type=application/json" \
    -F "file=@${jar};type=application/java-archive")

  if [[ "${http_code}" != 2* ]]; then
    echo "Modrinth upload failed for ${loader} (HTTP ${http_code}):" >&2
    cat "${response}" >&2
    exit 1
  fi

  echo "Uploaded ${jar} to Modrinth as ${version_number}."
}

upload_version \
  "fabric" \
  "${FABRIC_JAR}" \
  "FPSMaxx ${VERSION} (Fabric 26.3)" \
  "${VERSION}-fabric" \
  '[{"project_id":"P7dR8mSH","dependency_type":"required"},{"project_id":"AANobbMI","dependency_type":"optional"}]'

upload_version \
  "neoforge" \
  "${NEOFORGE_JAR}" \
  "FPSMaxx ${VERSION} (NeoForge 26.3)" \
  "${VERSION}-neoforge" \
  '[{"project_id":"AANobbMI","dependency_type":"optional"}]'
