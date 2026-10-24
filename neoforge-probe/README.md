# FPSMaxx NeoForge 26.3 probe

This is the NeoForge build for FPSMaxx 1.5.0. It compiles the shared FPSMaxx
core against NeoForge 26.3 and adds the NeoForge-specific entrypoint, command
tree, and Sodium config page. The Fabric 26.3 build is in the repository root.

## Build

```powershell
.\gradlew -p neoforge-probe build
```

Output: `neoforge-probe/build/libs/fpsmaxx-neoforge-1.5.0.jar`

## Run

```powershell
.\gradlew -p neoforge-probe runClient
```

The dev run config uses Java 25, NeoForge `26.3.0.97`, and automatically joins
the `New World` test save. Sodium is optional; put a Sodium NeoForge jar in
`neoforge-probe/run/mods` to test the Sodium hooks and config page.

## Layout

- Shared core: `../src/main/java`
- NeoForge adapter: `src/main/java/dev/rishit/fpsmaxx/neoforge`
- NeoForge metadata: `src/main/resources/META-INF/neoforge.mods.toml`
- Fabric-only classes excluded from this build: `FPSMaxxClient`,
  `FPSMaxxCommands`, `FPSMaxxSodiumConfig`

## Known version-specific pieces

- `LevelExtractor#isEntityVisible` gained `float`/`long` parameters in 26.3.
  The shared mixin uses the 26.3 descriptor; the 26.2 Fabric branch keeps the
  older descriptor.
- Sodium NeoForge `0.9.2` has a broken `LevelExtractor` mixin on 26.3. Use
  `0.9.3-beta.1+mc26.3` or newer.
- The Fabric and NeoForge command trees are duplicated because the source types
  differ (`FabricClientCommandSource` vs `CommandSourceStack`). They can be
  collapsed into a generic builder later.
