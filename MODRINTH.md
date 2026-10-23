# FPSMaxx

FPSMaxx is a **client-side Fabric optimization mod for Minecraft 26.2** built
for **smooth gameplay on low-end PCs**. It combines a 64-chunk render-distance
unlock with an adaptive FPS governor, a one-command low-end profile, and
entity, block-entity and particle culling.

It is not a cheat and does not need to be installed on the server.

## Features

- **Low-end PC preset** - `/fpsmaxx lowend` applies Fast graphics, clouds off,
  minimal particles, no entity shadows or smooth lighting, smaller cull
  distances, render 16, simulation 8, a 60 FPS target and a 60 FPS cap.
- **64-chunk render distance** - raises the vanilla slider maximum from 32 to 64.
- **Configurable simulation distance** - 32 on normal PCs, 8 in low-end mode,
  memory-capped on small heaps.
- **Adaptive visible radius** - chunks stay loaded, but only the radius your GPU
  can afford is drawn. Fog follows the same radius.
- **Adaptive particle budget** - distant particles are rejected and the live
  particle cap shrinks automatically when FPS falls.
- **Sodium 0.9.2+ integration** - the same adaptive radius is applied to Sodium's
  terrain search distance and fog. Sodium's render-distance slider is extended to 64.
- **Iris-friendly** - works on top of Sodium; shader fog can be left alone.
- **VulkanMod fallback** - switches to a real-render-distance fallback when Sodium's
  culling hook is not available.
- **Entity and block-entity culling** - distant entities and block entities are skipped.
- **Memory guard** - 2 GB gets 32 chunks, 3 GB gets 48, 4 GB+ gets 64.
- **F6 toggle** and `/fpsmaxx` commands for live tuning.

## How it optimizes

FPSMaxx is a **client-side rendering and scene optimization mod**, not a server
or mob-AI optimizer.

- **Adaptive chunk-section culling** - chunks stay loaded, but only sections
  inside the affordable visible radius are submitted to the renderer.
- **Sodium integration** - caps Sodium's terrain search distance and fog to the
  same radius, without unloading chunks.
- **Entity and block-entity culling** - skips distant entities and block entities
  before extraction and submission.
- **Particle culling and budgeting** - rejects distant particles before they enter
  the tick/render queues and caps live particles. The cap falls to 50% when FPS
  drops, then recovers.
- **Low-end preset** - applies the vanilla Fast graphics profile plus clouds off,
  minimal particles, no entity shadows/smooth lighting, biome blend 0, smaller
  weather radius, entity distance 0.5, render 16, simulation 8 and 60 FPS cap.
- **Fog matching** - hides the edge of the reduced radius; this is visual, not a
  large FPS gain by itself.
- **Memory guard** - prevents small heaps from loading more chunks than they can
  safely hold.
- **Chunk retention** - visible mode avoids repeated chunk load/unload stutter.

It does **not** speed up mob AI, block ticking, world generation or the server.
Simulation distance 32 is CPU-heavy; `/fpsmaxx lowend` lowers it to 8. The
governor trades visible draw distance for frame rate while keeping the requested
chunks loaded.

## Install

1. Install **Fabric Loader 0.19.3+** for **Minecraft 26.2** with **Java 25**.
2. Install **Fabric API 0.161.0+26.2**.
3. Optional but recommended: install **Sodium 0.9.2+ for 26.2**.
4. Put the FPSMaxx jar in your `mods` folder. Delete any older FPSMaxx jar first.
5. Allocate **4 GB or more** of RAM for 64 chunks (6 GB recommended). A 2 GB
   low-end PC can still play with fewer chunks.
6. On a low-end PC, run `/fpsmaxx lowend`. Otherwise set **Render Distance** to
   64; simulation distance is applied automatically.

## Commands

| Command | Purpose |
|---|---|
| `/fpsmaxx status` | Show visible distance, simulation distance, culls, particle budget, target FPS, and Sodium hook status |
| `/fpsmaxx lowend` | Apply the complete low-end PC profile |
| `/fpsmaxx balanced` | Restore normal mod-side defaults (vanilla graphics unchanged) |
| `/fpsmaxx render <2-64>` | Set the requested render distance |
| `/fpsmaxx mode auto/visible/render` | Choose the adaptive strategy |
| `/fpsmaxx simulation <5-32>` | Pin simulation distance |
| `/fpsmaxx target <30-1000>` | Set the FPS target |
| `/fpsmaxx min <2-64>` | Set the minimum visible distance |
| `/fpsmaxx entity <0-512>` | Entity cull distance |
| `/fpsmaxx blockentity <0-512>` | Block-entity cull distance |
| `/fpsmaxx particles <0-512>` | Particle cull distance |
| `/fpsmaxx maxparticles <0-20000>` | Live particle cap |
| `/fpsmaxx adaptive <true/false>` | Toggle the adaptive governor |
| `/fpsmaxx reload` | Reload `config/fpsmaxx.json` |

`visible` mode is the smoothest: chunks stay loaded and only the drawn radius
changes. `render` mode changes the real render distance and works with any
renderer, but chunks load/unload as it moves.

## Compatibility

- **Sodium 0.9.2+** - tested; adaptive terrain radius, matching fog, and a 64-chunk slider.
- **Iris** - should work on top of Sodium. If a shader pack supplies its own fog,
  set `matchFogToVisibleDistance` to `false`.
- **Lithium / C2ME / FerriteCore / ModernFix / ImmediatelyFast / EntityCulling /
  Bobby / Nvidium** - optional hooks only; mixins fail safely if another mod
  changes a target. C2ME's own render-distance uncap takes priority.
- **Resource packs** - textures, models, UI, sounds and language files are not
  touched. Resource packs with custom fog can win with
  `matchFogToVisibleDistance: false`.
- **Distant Horizons** - Sodium's terrain cap still applies; DH LODs are separate.

If Sodium integration reports `no-hook` in `/fpsmaxx status`, use
`/fpsmaxx mode render`.

## Important limits

- A **60 Hz display cannot show 65 FPS**. VSync caps the governor target to the
  refresh rate.
- **Multiplayer servers control their own view and simulation distance.** FPSMaxx
  cannot force a server to send more chunks.
- **Simulation distance 32 is CPU-heavy.** Use `/fpsmaxx lowend` or
  `/fpsmaxx simulation 8` / `12` on weak hardware.
- **The low-end preset changes vanilla graphics options.** It does not restore
  them; use Video Settings or `/fpsmaxx balanced` (which only resets FPSMaxx
  settings) if you want a different balance.
- 64 chunks is roughly 18,000 loaded chunks on both client and integrated server;
  low-memory devices are capped by the memory guard.

## Links

- Source: <https://github.com/Adaptablxus/fpsmaxxing>
- Issues: <https://github.com/Adaptablxus/fpsmaxxing/issues>
- Releases: <https://github.com/Adaptablxus/fpsmaxxing/releases>
- License: CC0-1.0
