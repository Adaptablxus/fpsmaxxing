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
  --arg changelog "FPSMaxx ${VERSION} is a client-side Fabric optimization mod for Minecraft 26.2, built for smooth gameplay on low-end PCs.

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

It is a rendering optimization mod, not a mob-AI or server optimizer. Simulation distance 32 is CPU-heavy; low-end lowers it to 8 and potato lowers it to 5.

Full notes: https://github.com/Adaptablxus/fpsmaxxing/releases/tag/v${VERSION}" \
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
