# Releasing a new version

Every version is committed, tagged, and pushed. The GitHub Actions **Release**
workflow builds the tagged source and publishes both jars automatically. The
same jars are uploaded to Modrinth.

1. Bump the version in both build files:
   - `gradle.properties` (`version=`)
   - `neoforge-probe/gradle.properties` (`mod_version=`)
   - `neoforge-probe/src/main/resources/META-INF/neoforge.mods.toml`
2. Update the versioned jar names in `README.md`, `MODRINTH.md`, and the
   compatibility notes.
3. Build and copy the artifacts:

   ```powershell
   .\gradlew.bat clean build
   .\gradlew.bat -p neoforge-probe clean build

   Copy-Item build\libs\fpsmaxx-<version>.jar "FPSMaxx-<version>-fabric-mc26.3.jar"
   Copy-Item neoforge-probe\build\libs\fpsmaxx-neoforge-<version>.jar "FPSMaxx-<version>-neoforge-mc26.3.jar"
   Copy-Item "FPSMaxx-<version>-fabric-mc26.3.jar" dist\mods\
   Copy-Item "FPSMaxx-<version>-neoforge-mc26.3.jar" dist\mods\
   Copy-Item README.md dist\README.md
   ```

   Keep the matching `fabric-api-<version>+26.3.jar` in `dist/mods/` for offline
   installs. NeoForge does not need Fabric API.

4. Move the previous release jars from `dist/mods/` into `dist/versions/` and add
   them to `dist/versions/README.md`.
5. Commit everything:

   ```powershell
   git add -A
   git commit -m "FPSMaxx <version>"
   ```

6. Tag and push:

   ```powershell
   git tag -a v<version> -m "FPSMaxx <version>"
   git push origin main --tags
   ```

The `Release` workflow runs for tags matching `v*`. It builds both loaders with
Java 25, uploads `FPSMaxx-<version>-fabric-mc26.3.jar` and
`FPSMaxx-<version>-neoforge-mc26.3.jar` to the GitHub release, and then publishes
them to Modrinth when the `MODRINTH_TOKEN` Actions secret is configured. The
upload script is `scripts/publish-modrinth.sh`; it skips cleanly if the secret is
absent or the project is still under review.

## Modrinth

Project: <https://modrinth.com/mod/fpsmaxx>  
Project ID: `kA3JYFQu`

Use `MODRINTH.md` as the project body and `modrinth-icon.png` as the icon. The
release workflow uploads both jars automatically. To update the live project
summary and body, run the **Update Modrinth metadata** workflow from the Actions
tab; it uses `scripts/update-modrinth-metadata.sh` and the same `MODRINTH_TOKEN`
secret. If uploading manually, create two versions:

| Field | Fabric version | NeoForge version |
|---|---|---|
| Version number | `<version>-fabric` | `<version>-neoforge` |
| File | `FPSMaxx-<version>-fabric-mc26.3.jar` | `FPSMaxx-<version>-neoforge-mc26.3.jar` |
| Game version | `26.3` | `26.3` |
| Loader | `fabric` | `neoforge` |
| Environment | `client_only` | `client_only` |
| Version type | `release` | `release` |
| Required dependency | Fabric API (`P7dR8mSH`) | none |
| Optional dependency | Sodium (`AANobbMI`) | Sodium (`AANobbMI`) |

The project is submitted for Modrinth review after the first version. Do not
commit the Modrinth token or any API response scratch files.

## Archived versions

- `1.0.0` is archived at `dist/versions/FPSMaxx-1.0.0-mc26.2.jar`. It is published on Modrinth and as a GitHub release. Its tag points to the `archive/1.0.0` branch because the original source snapshot was not preserved.
- `1.1.1` is archived at `dist/versions/FPSMaxx-1.1.1-mc26.2.jar`. It was the last release before the low-end optimization pass.
- `1.2.0` is archived at `dist/versions/FPSMaxx-1.2.0-mc26.2.jar`. It added the `/fpsmaxx lowend` preset, particle culling/budgeting, first-run low-end defaults, and broader vanilla graphics reductions.
- `1.3.0` is archived at `dist/versions/FPSMaxx-1.3.0-mc26.2.jar`. It added the `/fpsmaxx potato` preset, chunk-update budgeting, removal of far particles, stronger low-end graphics reductions, and the `/fpsmaxx sectionbudget` command.
- `1.4.0` is archived at `dist/versions/FPSMaxx-1.4.0-mc26.2.jar`. It added the hidden **FpsMaxx** graphics-preset position, automatic **Fresh & Smooth** visual-safe mode, AsyncParticles/Particle Core awareness, and Iris-safe fog handling.
- `1.4.1` is archived at `dist/versions/FPSMaxx-1.4.1-mc26.2.jar`. It was the final Fabric 26.2 build and added the searchable **FPSMaxx** Sodium option page with an **Open FPSMaxx preset screen** button.
- `1.5.0` is the current release. It adds Fabric and NeoForge builds for Minecraft 26.3 from a shared core, with Sodium integration on both loaders.
- The old 1.1.0 label and the old 1.2.0 release are retired. The current `1.5.0` tag points to this source release.

## Notes

- Do not commit `build/`, `run/`, `.gradle/`, or log files; they are ignored.
- The repository includes the current root jar and `dist/` jars so a clone has the
  same ready-to-install artifacts as the release.
- Sodium is optional and is not bundled. The release jars have only FPSMaxx and
  compile-only references to Sodium.
- Sodium NeoForge `0.9.2` is broken on 26.3. Test NeoForge with
  `0.9.3-beta.1+mc26.3` or newer.
- Before tagging, test `/fpsmaxx lowend`, `/fpsmaxx potato`, `/fpsmaxx balanced`,
  and `/fpsmaxx status` on a small-heap instance. The status output should show
  low-end state, particle cull/cap, section budget, and the adaptive particle
  percentage.
- Open Video Settings and drag the Graphics Preset slider through **FpsMaxx,
  Fast, Fancy, Fabulous and Custom**. Confirm the FpsMaxx label appears at the
  far left, the flag persists in `config/fpsmaxx.json`, and vanilla presets reset
  the mod profile. With Sodium installed, confirm **Video Settings -> FPSMaxx**
  opens the same slider on both loaders.
- If a new version is added, move the previous root/`dist/mods` jars into
  `dist/versions/` and add them to `dist/versions/README.md`.
