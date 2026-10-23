# FPSMaxx (Minecraft 26.2 / Fabric)

FPSMaxx raises the render-distance slider from 32 to **64 chunks**, keeps
simulation distance at **32 by default** (vanilla's maximum), and adds an
adaptive visibility governor that protects your frame rate. It is a
**client-side** mod: install it on the client, not on the server.

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
> - it keeps **simulation distance at 32** (the vanilla maximum, or the
>   memory-safe cap) so mob AI and ticking stay at full range while rendering
>   adapts. Simulation distance is deliberately **not** adaptive; lower it
>   manually on a phone.
>
> On a strong gaming PC you will sit at 64 chunks. On integrated graphics or a
> phone it will settle at a lower visible distance and stay playable. If a device
> cannot hit 65 FPS at the minimum distance (8 chunks by default), nothing can
> fix that.

## Requirements

| Thing | Version |
|---|---|
| Minecraft Java Edition | **26.2** |
| Fabric Loader | **0.19.3 or newer** |
| Fabric API | **0.161.0+26.2** (any 26.2 build works) |
| Java | **25** (required by 26.2) |
| RAM | **4 GB+ allocated** for 64 chunks (6 GB recommended) |
| CPU | 32 simulation distance is CPU-heavy; lower it on phones |
| Sodium (optional) | **0.9.2+ for 26.2** (recommended) |
| Mod | `FPSMaxx-1.2.0-mc26.2.jar` |

## Install — PC (official launcher / Prism / MultiMC)

1. Install Fabric Loader 0.19.3+ for Minecraft 26.2.
2. Put these two files in the instance's `mods` folder (both are in `dist/mods/`):
   - `FPSMaxx-1.2.0-mc26.2.jar`
   - `fabric-api-0.161.0+26.2.jar`
3. If you use **Sodium**, install **Sodium 0.9.2+ for 26.2** as well. FPSMaxx
   detects it automatically, applies the same adaptive radius to Sodium's
   terrain and fog, and extends Sodium's render-distance slider to 64.
4. Allocate **4 GB or more** to the instance (6 GB recommended). 64 chunks
   loads roughly 18,000 chunks on both the client and integrated server.
5. Launch the game, go to **Options → Video Settings**, and set **Render Distance**
   to 64. The slider now goes past 32.
6. Simulation distance is pinned to **32** when you enter a world (the memory
   guard may cap it lower). Change it with `/fpsmaxx simulation <5-32>` or the
   `simulationDistance` setting in `config/fpsmaxx.json`.
7. (Optional) In-game, run `/fpsmaxx status` to see the current visible distance,
   simulation distance and measured FPS.
8. Press **F6** to toggle the adaptive governor on/off.

## Install — PojavLauncher / MojoLauncher (Android)

1. Your launcher profile **must use Java 25**. Minecraft 26.2 will not start on
   Java 8/17/21. If the launcher does not offer Java 25 yet, the game itself
   cannot run 26.2 on that device — this is not a mod issue.
2. Install **Fabric Loader 0.19.3+** for **26.2** inside the launcher.
3. Copy `FPSMaxx-1.2.0-mc26.2.jar` **and** `fabric-api-0.161.0+26.2.jar` into
   `.minecraft/mods`. If your launcher/GPU supports Sodium 0.9.2+ for 26.2,
   install it too; FPSMaxx will detect it.
4. Allocate as much RAM as the device allows. 64 chunks is extremely heavy;
   start with the settings below. **Simulation 32 is too heavy for most phones**;
   the phone-safe config below sets it to 8.

### Phone-safe defaults

Edit `config/fpsmaxx.json` (created after the first launch) or use commands:

```json
{
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
  "blockEntityCullDistance": 48
}
```

Then set the render-distance slider to **32 or lower**. The slider can go to 64,
but on a phone that mostly wastes RAM and battery. The governor will keep the
frame rate playable either way. The PC default is `simulationDistance: 32`; 8-12
is the sensible phone range because 32 chunks of mob AI and block ticking will
eat the CPU headroom the renderer needs.

## Commands

| Command | What it does |
|---|---|
| `/fpsmaxx` or `/fpsmaxx status` | Show adaptive state, visible/simulation distance, FPS |
| `/fpsmaxx adaptive <true/false>` | Turn the governor on/off |
| `/fpsmaxx target <fps>` | Target FPS (30–1000, default **65**; VSync caps it) |
| `/fpsmaxx render <chunks>` | Set the requested render distance (2–64) |
| `/fpsmaxx mode <auto/visible/render>` | Choose the adaptive strategy (default **auto**) |
| `/fpsmaxx simulation <chunks>` | Pin simulation distance (5–32; default **32**, memory-capped) |
| `/fpsmaxx min <chunks>` | Lowest visible distance (default 8) |
| `/fpsmaxx entity <blocks>` | Entity cull distance, 0 = off (default 96) |
| `/fpsmaxx blockentity <blocks>` | Block-entity cull distance, 0 = off (default 96) |
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

## How the governor works

- Chunks are loaded at the distance you choose (up to 64) and stay loaded.
- `SectionOcclusionGraph` is told to only feed sections inside the current
  visible radius, so the GPU never draws the hidden ring.
- With Sodium, the same cap is applied to Sodium's terrain search distance and
  its fog parameters. Chunks still stay loaded; Sodium just stops submitting
  the ring outside the governed radius.
- Fog is moved to that same radius, so the world edge is never visible.
- Entities and block entities past their cull distances are not submitted.
- Simulation distance is pinned to `simulationDistance` (32 by default) when a
  world is entered. It is **not** part of the visible-radius governor because
  lowering it changes mob AI and ticking; that is a deliberate manual trade-off.
- Each second the mod reads the smoothed FPS. Below target it drops the visible
  radius quickly; above target (with a 5-second cooldown) it grows back.
- The starting radius is `initialVisibleDistance` (32 on PC), then it ramps up to
  your slider value when the FPS headroom allows it. That is why the first few
  seconds after joining are conservative.
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

That measurement used a lower simulation distance. With simulation distance now
pinned at 32 by default, a CPU-limited machine may settle a little lower; use
`/fpsmaxx simulation 12` or `8` if the visible radius keeps dropping.

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
- Leave the governor on. `target 65` is the default.
- Simulation distance is pinned to **32** by default. If the visible radius keeps
  dropping, lower it with `/fpsmaxx simulation 12` or `/fpsmaxx simulation 8`.
  It drives mob AI and block ticking, not rendering, but 32 chunks of simulation
  is still expensive.
- Install **Sodium 0.9.2+ for 26.2** when possible; FPSMaxx has a tested
  integration for it. Leave `sodiumIntegration` enabled. If `/fpsmaxx status`
  reports no Sodium hook, use `/fpsmaxx mode render` instead.
- If a resource pack or shader pack supplies custom fog, set
  `"matchFogToVisibleDistance": false` and `/fpsmaxx reload`.

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
overload a weak CPU, so lower it with `/fpsmaxx simulation` if needed.
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

The jar is produced at `build/libs/fpsmaxx-1.2.0.jar`; the ready-to-install copy
is `dist/mods/FPSMaxx-1.2.0-mc26.2.jar`. Toolchain: Gradle 9.7.1,
Loom 1.18.4, Java 25, Minecraft 26.2 (unobfuscated, Mojang names).

## License

CC0-1.0. Do whatever you want with it.

