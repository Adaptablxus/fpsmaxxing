# FPSMaxx (Minecraft 26.2 / Fabric)

FPSMaxx is a **client-side Fabric optimization mod for Minecraft 26.2**, built
for **smooth gameplay on low-end PCs** as well as high-end machines. It raises
the render-distance slider from 32 to **64 chunks**, adds an adaptive
visibility governor, and cuts the work that usually causes stutter: distant
entities, block entities, particles, unnecessary chunk sections, oversized
graphics settings and bursty chunk-mesh rebuilds. Install it on the client, not
on the server.

On a weak PC, run **`/fpsmaxx lowend`** once. It applies a complete low-end
profile: Fast graphics, clouds off, fewer particles, no entity shadows or
smooth lighting, 16-chunk render distance, 8-chunk simulation distance, a
60 FPS target, tighter cull distances and a chunk-update budget. New installs
on detected low-end hardware start with the mod-side low-end defaults
automatically. If that is still not enough, **`/fpsmaxx potato`** goes further:
render 8, simulation 5, tiny particle and entity distances, and a 24-section
update budget.

FPSMaxx also detects Sodium, Iris, VulkanMod and common optimization mods. The
vanilla renderer and Sodium get the smooth visible-radius governor; VulkanMod
uses a real-render-distance fallback. See
[Compatibility](#compatibility-with-optimization-mods-and-resource-packs).

Latest release: <https://github.com/Adaptablxus/fpsmaxxing/releases>  
Modrinth: <https://modrinth.com/mod/fpsmaxx>

> **Read this first — the honest part**
>
> No mod can render 64 chunks at 65+ FPS on every device. That is a hardware
> limit, not a code limit. 64 chunks means roughly **16,600 loaded chunks** and
> up to **400,000 chunk sections**. A phone cannot hold that in RAM or draw it at
> 65 FPS, and a 60 Hz phone screen physically cannot display more than 60 FPS.
>
> FPSMaxx does the next best thing:
> - it lets you **select 64 chunks** and actually loads them in singleplayer;
> - it **holds your target FPS** by shrinking only the *visible* radius when the
>   frame rate falls, while chunks stay loaded (so moving never causes loading
>   stutter);
> - it grows the visible radius back automatically when you have headroom;
> - it keeps simulation distance configurable: **32 on a normal PC** and
>   **8 in the low-end preset**, so mob AI and ticking can be reduced when the
>   CPU is the bottleneck. Simulation distance is deliberately **not**
>   adaptive; lower it manually on a phone or weak laptop.
>
> On a strong gaming PC you will sit at 64 chunks. On integrated graphics or a
> phone it will settle at a lower visible distance and stay playable. If a device
> cannot hit 60-65 FPS at the minimum distance (4-8 chunks), nothing can fix
> that.

## Requirements

| Thing | Version |
|---|---|
| Minecraft Java Edition | **26.2** |
| Fabric Loader | **0.19.3 or newer** |
| Fabric API | **0.161.0+26.2** (any 26.2 build works) |
| Java | **25** (required by 26.2) |
| RAM | **4 GB+ allocated** for 64 chunks (6 GB recommended) |
| CPU | 32 simulation distance is CPU-heavy; use `/fpsmaxx lowend` on weak PCs |
| Sodium (optional) | **0.9.2+ for 26.2** (recommended) |
| Mod | `FPSMaxx-1.3.0-mc26.2.jar` |

## Low-end PC quick start

After installing, launch the game, join a world and run:

```text
/fpsmaxx lowend
```

This applies the full low-end profile immediately:

- Fast graphics preset, clouds off, cloud distance 2, particles minimal
- Weather radius 3, no vignette, improved transparency off, chunk fade 0
- Entity shadows and smooth lighting off, biome blend 0
- Mipmaps 0 and minimum anisotropic filtering
- Render distance 16, simulation distance 8, target 60 FPS
- Entity culling 48 blocks, block-entity culling 32 blocks
- Particle culling 24 blocks, particle cap 1200
- Entity distance scaling 0.5 and a 60 FPS framerate cap
- Chunk-update budget 64 sections per frame (32 when FPS is low)

If the result is still not smooth, try the potato preset:

```text
/fpsmaxx potato
```

It sets render 8, simulation 5, target 60, entity culling 24, block-entity
culling 16, particle culling 8, particle cap 400 and a 24-section update
budget. It also applies the same Fast graphics reductions as `/fpsmaxx lowend`.
If you only need a little more, use the individual commands instead:

```text
/fpsmaxx render 8
/fpsmaxx simulation 6
/fpsmaxx min 2
```

`/fpsmaxx balanced` restores the normal mod-side defaults (target 65, 32
simulation, wider culls, no section budget). It does **not** restore the vanilla
graphics or framerate options that `/fpsmaxx lowend` or `/fpsmaxx potato`
changed; set those back in Video Settings if you want them. New installs on
hardware detected as low-end (4 or fewer CPU cores, or 2 GB or less of allocated
heap) start with the mod-side low-end defaults automatically, but the command is
what applies the vanilla graphics reductions. You can force the first-run choice
with `-Dfpsmaxx.forceLowEnd=true` or `-Dfpsmaxx.forceBalanced=true`.

## Install — PC (official launcher / Prism / MultiMC)

1. Install Fabric Loader 0.19.3+ for Minecraft 26.2.
2. Put these two files in the instance's `mods` folder (both are in `dist/mods/`):
   - `FPSMaxx-1.3.0-mc26.2.jar`
   - `fabric-api-0.161.0+26.2.jar`
3. If you use **Sodium**, install **Sodium 0.9.2+ for 26.2** as well. FPSMaxx
   detects it automatically, applies the same adaptive radius to Sodium's
   terrain and fog, and extends Sodium's render-distance slider to 64.
4. Allocate **4 GB or more** to the instance for 64 chunks (6 GB recommended).
   A low-end PC with 2 GB can still play; it just loads fewer chunks.
5. Launch the game, go to **Options → Video Settings**, and set **Render Distance**
   to 64. The slider now goes past 32. On a low-end PC, leave it at 16 or lower
   and run `/fpsmaxx lowend` instead.
6. Simulation distance is pinned when you enter a world: **32 on a normal PC**,
   or **8 after `/fpsmaxx lowend`** (the memory guard may cap it lower). Change
   it with `/fpsmaxx simulation <5-32>` or the `simulationDistance` setting in
   `config/fpsmaxx.json`.
7. (Optional) In-game, run `/fpsmaxx status` to see the current visible distance,
   simulation distance and measured FPS.
8. Press **F6** to toggle the adaptive governor on/off.

## Install — PojavLauncher / MojoLauncher (Android)

1. Your launcher profile **must use Java 25**. Minecraft 26.2 will not start on
   Java 8/17/21. If the launcher does not offer Java 25 yet, the game itself
   cannot run 26.2 on that device — this is not a mod issue.
2. Install **Fabric Loader 0.19.3+** for **26.2** inside the launcher.
3. Copy `FPSMaxx-1.3.0-mc26.2.jar` **and** `fabric-api-0.161.0+26.2.jar` into
   `.minecraft/mods`. If your launcher/GPU supports Sodium 0.9.2+ for 26.2,
   install it too; FPSMaxx will detect it.
4. Allocate as much RAM as the device allows. 64 chunks is extremely heavy;
   start with the settings below. **Simulation 32 is too heavy for most phones**;
   the phone-safe config below sets it to 8.

### Phone-safe defaults

Edit `config/fpsmaxx.json` (created after the first launch) or use commands:

```json
{
  "lowEndMode": true,
  "targetFps": 60,
  "adaptiveEnabled": true,
  "initialVisibleDistance": 8,
  "minVisibleDistance": 4,
  "simulationDistance": 8,
  "forceSimulationDistance": true,
  "adaptiveMode": "auto",
  "sodiumIntegration": true,
  "sodiumConfigOverlay": true,
  "matchFogToVisibleDistance": true,
  "entityCullDistance": 48,
  "blockEntityCullDistance": 32,
  "particleCullDistance": 24,
  "maxParticles": 1200,
  "adaptiveParticles": true,
  "sectionUpdateBudget": 64
}
```

Then set the render-distance slider to **16 or lower**. The slider can go to 64,
but on a phone that mostly wastes RAM and battery. The governor will keep the
frame rate playable either way. The normal-PC default is `simulationDistance:
32`; 6-12 is the sensible phone/low-end range because 32 chunks of mob AI and
block ticking will eat the CPU headroom the renderer needs. On a phone, try
`/fpsmaxx potato` first; it uses render 8, simulation 5 and a 24-section update
budget.

## Commands

| Command | What it does |
|---|---|
| `/fpsmaxx` or `/fpsmaxx status` | Show adaptive state, visible/simulation distance, culls, particles, section budget, FPS |
| `/fpsmaxx lowend` | Apply the complete low-end PC profile |
| `/fpsmaxx potato` | Apply the extreme low-end profile (render 8, simulation 5) |
| `/fpsmaxx balanced` | Restore normal mod-side defaults (vanilla graphics unchanged) |
| `/fpsmaxx adaptive <true/false>` | Turn the governor on/off |
| `/fpsmaxx target <fps>` | Target FPS (30–1000; default **65**, **60** in low-end/potato mode) |
| `/fpsmaxx render <chunks>` | Set the requested render distance (2–64) |
| `/fpsmaxx mode <auto/visible/render>` | Choose the adaptive strategy (default **auto**) |
| `/fpsmaxx simulation <chunks>` | Pin simulation distance (5–32; default **32**, **8** low-end, **5** potato) |
| `/fpsmaxx min <chunks>` | Lowest visible distance (default **8**, **4** low-end, **2** potato) |
| `/fpsmaxx entity <blocks>` | Entity cull distance, 0 = off (default **96**, **48** low-end, **24** potato) |
| `/fpsmaxx blockentity <blocks>` | Block-entity cull distance, 0 = off (default **96**, **32** low-end, **16** potato) |
| `/fpsmaxx particles <blocks>` | Particle cull distance, 0 = off (default **64**, **24** low-end, **8** potato) |
| `/fpsmaxx maxparticles <count>` | Particle cap, 0 = vanilla limits only (default **4000**, **1200** low-end, **400** potato) |
| `/fpsmaxx sectionbudget <sections>` | Max chunk-section rebuilds per frame, 0 = vanilla (low-end **64**, potato **24**) |
| `/fpsmaxx reload` | Reload `config/fpsmaxx.json` and re-apply simulation distance |

`/fpsmaxx simulation <chunks>` also sets `forceSimulationDistance` to `true`, so
the value is re-applied on every world join. To let the vanilla video settings
control simulation distance instead, set `"forceSimulationDistance": false` in
`config/fpsmaxx.json` and run `/fpsmaxx reload`.

`adaptiveMode` controls how the governor reduces work:

- `auto` — visible-radius mode on vanilla and Sodium; render-distance fallback
  on VulkanMod (and when Sodium integration is disabled).
- `visible` — chunks stay loaded; only the rendered radius changes. This is the
  smoothest mode and the one used by default on vanilla/Sodium.
- `render` — changes the real render distance. Works with any renderer, but
  chunks load/unload as it moves, so expect some stutter. Useful as a fallback.

## How FPSMaxx optimizes

FPSMaxx is a **client-side rendering and scene optimization mod**, not a
server/AI optimizer. Its main win is refusing to submit chunk sections,
entities, block entities and particles that are outside the radius the GPU can
currently afford.

- **Adaptive chunk-section culling** — the largest saving. The client still
  loads chunks at the selected distance, but `SectionOcclusionGraph` only feeds
  sections inside the current visible radius to the renderer. Fewer visible
  sections means fewer terrain draw calls, less GPU work and less chunk-mesh
  submission work. Because chunks stay loaded, shrinking the radius does not
  cause chunk-loading stutter.
- **Sodium integration** — Sodium does not use vanilla's occlusion graph, so
  FPSMaxx caps Sodium's terrain search distance and fog parameters directly.
  This gives Sodium the same adaptive radius without unloading chunks.
- **Entity and block-entity culling** — entities and block entities beyond the
  configured cull distance are skipped before extraction/submission. This matters
  at 64 chunks because the server keeps far more entities loaded.
- **Particle culling and budgeting** — particles beyond the cull distance are
  rejected before they enter the tick/render queues, and particles that drift
  outside the radius are removed instead of ticking forever. A live-particle cap
  stops rain, fires, explosions and large farms from flooding the client. When
  FPS drops, the governor reduces the particle budget down to 50%; when FPS
  recovers, it grows back.
- **Chunk-update budgeting** — chunk mesh rebuilds are bursty. When a large
  batch of sections becomes dirty, FPSMaxx schedules only the nearest budget
  (64 in low-end mode, 24 in potato mode) and re-queues the rest for the next
  frame. This smooths stutter without leaving chunks permanently unbuilt. The
  budget applies to the vanilla chunk renderer; Sodium schedules its own chunk
  builds.
- **Low-end preset** — one command applies Fast graphics, clouds off, cloud
  distance 2, minimal particles, weather radius 3, no vignette, no improved
  transparency, zero chunk fade, no entity shadows or smooth lighting, no biome
  blend, mipmaps 0, minimum anisotropic filtering, entity distance scaling 0.5,
  render 16, simulation 8 and a 60 FPS cap. These are the settings that usually
  make the biggest difference on integrated graphics and old laptops.
- **Potato preset** — the same reductions plus render 8, simulation 5, entity
  culling 24, block-entity culling 16, particle culling 8, particle cap 400 and
  a 24-section update budget for phones and very old integrated GPUs.
- **Fog matching** — fog is moved to the same radius as the visible terrain. This
  is a visual fix (it hides the edge); it is not itself a large FPS gain.
- **Memory guard** — on small heaps the mod caps loaded chunks and simulation
  distance before they cause out-of-memory crashes or GC thrashing.
- **Chunk retention** — the governor changes only the visible radius in
  `visible` mode, so moving around does not repeatedly load/unload chunks. The
  `render` fallback is for renderers without a visible-radius hook and does
  trade smoothness for compatibility.

What it does **not** do: it does not speed up mob AI, block ticking, world
generation or the server. Simulation distance 32 is expensive; the low-end
preset lowers it to 8 and the potato preset to 5. Unlocking 64 chunks also
increases load. The governor is the trade-off: it keeps the requested distance
loaded and reduces what is drawn. If the FPS target still cannot be met, lower
simulation distance and the minimum visible distance.

## How the governor works

- Chunks are loaded at the distance you choose (up to 64) and stay loaded.
- `SectionOcclusionGraph` is told to only feed sections inside the current
  visible radius, so the GPU never draws the hidden ring.
- With Sodium, the same cap is applied to Sodium's terrain search distance and
  its fog parameters. Chunks still stay loaded; Sodium just stops submitting
  the ring outside the governed radius.
- Fog is moved to that same radius, so the world edge is never visible.
- Entities, block entities and particles past their cull distances are not
  submitted, and far particles are removed from the tick list.
- Simulation distance is pinned to `simulationDistance` when a world is entered:
  **32 on a normal PC**, **8 in low-end mode**, **5 in potato mode** (or the
  memory-safe cap). It is **not** part of the visible-radius governor because
  lowering it changes mob AI and ticking; that is a deliberate manual trade-off.
- Each second the mod reads the smoothed FPS. Below target it drops the visible
  radius quickly and reduces the particle budget; above target (with a
  5-second cooldown) it grows both back. A section budget also halves while the
  frame rate is below target, so mesh rebuilds do not keep the GPU/CPU pinned.
- The starting radius is `initialVisibleDistance` (**32** on a normal PC,
  **8** in low-end mode, **4** in potato mode), then it ramps up to your slider
  value when the FPS headroom allows it. That is why the first few seconds after
  joining are conservative.
- The governor intentionally pauses while a menu is open and for a couple of
  seconds after every change, because chunk-mesh compilation itself causes a
  short FPS dip. Without that, the mod would shrink the world every time it
  grew it.

### Measured example

On the machine this was developed on (i3-12100, Intel UHD 730, 8 GB RAM,
superflat test world), 64 chunks loaded and rendered. The governor settled
around **40–46 visible chunks** while holding 65+ FPS, and dropped to ~24 during
the initial chunk-generation storm before climbing back. A normal world with
terrain, mobs and block entities will settle lower. A high-end dedicated GPU
will hold 64 much more comfortably. This is exactly what the adaptive mode is
for: the requested distance is 64, the GPU only draws what it can afford.

That measurement used a lower simulation distance. A CPU-limited machine with
simulation 32 may settle lower; use `/fpsmaxx lowend` or
`/fpsmaxx simulation 12` / `8` if the visible radius keeps dropping.

## Settings for the "65 FPS" goal on PC

- Set the slider to 64. If you just changed the graphics preset, apply the
  preset **first**, then set the render distance (or run `/fpsmaxx render 64`).
  Vanilla presets overwrite the slider: Fancy resets it to 16, Fabulous to 32.
- Set the game's max framerate to **unlimited** or at least **120**, and turn
  **VSync off**. A 60 Hz monitor with VSync physically cannot show 65 FPS.
  While VSync is on, FPSMaxx automatically uses the monitor refresh rate as the
  target (e.g. 60) instead of shrinking the world forever chasing an impossible
  number. `/fpsmaxx status` shows this as "VSync cap".
- Use the **Vulkan** graphics backend if your GPU supports it (26.2 feature);
  the OpenGL backend is much slower on some systems.
- Leave the governor on. `target 65` is the normal default; `/fpsmaxx lowend`
  sets a more realistic **60 FPS** target for weak hardware.
- Simulation distance is **32** on a normal PC, **8** in low-end mode and **5**
  in potato mode. It drives mob AI and block ticking, not rendering, but 32
  chunks of simulation is still expensive. If the visible radius keeps dropping,
  lower it with `/fpsmaxx simulation 12` or `8`.
- On a low-end PC, use `/fpsmaxx lowend` first, then `/fpsmaxx potato`, then
  tune `/fpsmaxx render 8`, `/fpsmaxx simulation 6` and `/fpsmaxx min 2` only if
  needed.
- If chunk loading causes stutter, lower `/fpsmaxx sectionbudget` (for example
  to 32). A lower budget smooths frame time but makes newly loaded areas take
  longer to appear. Set it to 0 to use vanilla's unlimited behavior.
- Install **Sodium 0.9.2+ for 26.2** when possible; FPSMaxx has a tested
  integration for it. Leave `sodiumIntegration` enabled. If `/fpsmaxx status`
  reports no Sodium hook, use `/fpsmaxx mode render` instead.
- If a resource pack or shader pack supplies custom fog, set
  `"matchFogToVisibleDistance": false` and `/fpsmaxx reload`.

## If you need even more performance

These are the remaining high-impact levers, in order:

1. `/fpsmaxx potato` — render 8, simulation 5, tiny cull distances.
2. Lower the **window resolution** or play in a smaller window. Fill rate is
   usually the biggest GPU cost on integrated graphics, and no mod can beat a
   lower pixel count.
3. Disable shader packs. Iris shaders can cost more than the vanilla renderer
   saves, especially at high shadow resolution.
4. Use **Sodium 0.9.2+** and the **Vulkan** backend when your GPU supports it.
5. `/fpsmaxx simulation 5` or `6` and `/fpsmaxx render 8` if CPU time is the
   bottleneck. Simulation distance changes mob AI and ticking; render distance
   changes what is loaded.
6. `/fpsmaxx sectionbudget 32` if chunk loading still stutters. This trades
   faster terrain appearance for smoother frame times.
7. Close background applications, browsers and overlays. On a 2-4 GB machine
   they compete for the same RAM and CPU as Minecraft.

## Compatibility with optimization mods and resource packs

FPSMaxx does not bundle or hard-depend on any optimization mod. Its optional
hooks are non-fatal: if another mod changes a target class, that hook disables
itself instead of crashing the game. `/fpsmaxx status` shows whether the Sodium
hooks are active.

| Mod / content | What to expect |
|---|---|
| **Sodium 0.9.2+** | Tested. Adaptive terrain radius, matching fog, and a 64-chunk Sodium slider. Chunks stay loaded. |
| **Iris** | Should work on top of Sodium; not individually tested here. Shader packs can replace fog; if the edge is visible or the shader's fog is overridden, set `matchFogToVisibleDistance` to `false`. |
| **VulkanMod** | `adaptiveMode: auto` selects the real-render-distance fallback because VulkanMod does not expose Sodium's culling hook. The fallback is tested in vanilla/Sodium, not individually on VulkanMod. Expect chunk load/unload stutter. |
| **Lithium / C2ME** | Server-side mixins are best-effort. C2ME's own render-distance uncap takes priority and FPSMaxx skips its Sodium slider overlay when C2ME is present. |
| **FerriteCore / ModernFix / ImmediatelyFast / EntityCulling / Bobby / Nvidium** | No hard dependencies; optional hooks apply only if their target methods still exist. Not individually tested here; entity culling may overlap harmlessly. |
| **Distant Horizons** | Sodium's terrain cap still applies. Distant Horizons LODs are rendered by its own system and are not controlled by FPSMaxx's visible-radius cap. |
| **Resource packs** | Textures, models, UI, sounds and language files are untouched. Resource packs that define custom fog can win by setting `matchFogToVisibleDistance` to `false`. |
| **Shader packs** | Same fog setting as resource packs. Shader performance still depends on the pack and the 64-chunk request. |

If Sodium integration reports `no-hook` in `/fpsmaxx status`, your Sodium build
is not exposing the expected method. Use `/fpsmaxx mode render`; that path works
with any renderer. If another render-distance mod already raises Sodium's
slider, set `sodiumConfigOverlay` to `false` to avoid two overlays fighting over
the same option.

## Memory guard

Loading 64 chunks can easily use several gigabytes. FPSMaxx checks the JVM's
maximum heap and lowers the *loaded* distance automatically:

| Allocated heap | Maximum loaded render distance | Simulation cap |
|---|---|---|
| 4 GB or more | 64 chunks | 32 chunks |
| 3 GB | 48 chunks | 32 chunks |
| 2 GB | 32 chunks | 32 chunks |
| 1.5 GB | 24 chunks | 24 chunks |
| below 1.5 GB | 16 chunks | 16 chunks |

The server and the client are told the same capped render distance, so you never
get holes in the world. The simulation cap is `min(32, loaded cap)`. This is a
**memory** guard, not a performance guarantee: 32 simulation chunks can still
overload a weak CPU, so use `/fpsmaxx lowend` or `/fpsmaxx simulation` if needed.
`/fpsmaxx status` and the log show both caps. Allocate 6 GB for comfortable
64-chunk play. If you accept the crash risk, advanced users can add
`-Dfpsmaxx.ignoreMemoryLimit=true` to the JVM arguments to force 64 render and
32 simulation.

## Why singleplayer needs server-side mixins

Minecraft's integrated server clamps its view distance to 32 chunks and its
player ticket tracker to a 32-chunk radius. FPSMaxx raises both clamps from the
client side, so 64 chunks can actually be generated and sent in singleplayer.
Simulation distance already maxes out at 32 in vanilla; FPSMaxx raises the
low-memory slider cap from 16 to 32 and applies the configured value when a
world is entered. The integrated server reads the client option every tick, so
`/fpsmaxx simulation` takes effect immediately.

On a real multiplayer server the server's own view distance and simulation
distance still apply; the mod cannot force a server to send chunks or tick
entities it does not have.

## Build from source

```bash
./gradlew build
```

The jar is produced at `build/libs/fpsmaxx-1.3.0.jar`; the ready-to-install copy
is `dist/mods/FPSMaxx-1.3.0-mc26.2.jar`. Toolchain: Gradle 9.7.1,
Loom 1.18.4, Java 25, Minecraft 26.2 (unobfuscated, Mojang names).

## License

CC0-1.0. Do whatever you want with it.

