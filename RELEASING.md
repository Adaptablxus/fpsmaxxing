# Releasing a new version

Every version is committed, tagged, and pushed. The GitHub Actions **Release**
workflow builds the tagged source and publishes the jar automatically. The same
jar is uploaded to Modrinth.

1. Bump `version` in `gradle.properties`.
2. Update the versioned jar names in `README.md` and the compatibility notes.
3. Build and copy the artifacts:

   ```powershell
   .\gradlew.bat clean build
   Copy-Item build\libs\fpsmaxx-<version>.jar FPSMaxx-<version>-mc26.2.jar
   Copy-Item build\libs\fpsmaxx-<version>.jar dist\mods\FPSMaxx-<version>-mc26.2.jar
   Copy-Item README.md dist\README.md
   ```

4. Commit everything:

   ```powershell
   git add -A
   git commit -m "FPSMaxx <version>"
   ```

5. Tag and push:

   ```powershell
   git tag -a v<version> -m "FPSMaxx <version>"
   git push origin main --tags
   ```

The `Release` workflow runs for tags matching `v*`. It builds with Java 25,
uploads `FPSMaxx-<version>-mc26.2.jar` to the GitHub release, and then publishes
the same jar to Modrinth when the `MODRINTH_TOKEN` Actions secret is configured.
The upload script is `scripts/publish-modrinth.sh`; it skips cleanly if the
secret is absent.

## Modrinth

Project: <https://modrinth.com/mod/fpsmaxx>  
Project ID: `kA3JYFQu`

Use `MODRINTH.md` as the project body and `modrinth-icon.png` as the icon. The
release workflow uploads the same `FPSMaxx-<version>-mc26.2.jar` automatically.
To update the live project summary and body, run the **Update Modrinth metadata**
workflow from the Actions tab; it uses `scripts/update-modrinth-metadata.sh` and
the same `MODRINTH_TOKEN` secret. If uploading manually, use:

- Game version: `26.2`
- Loader: `fabric`
- Environment: `client_only`
- Version type: `release`
- Required dependency: Fabric API (`P7dR8mSH`)
- Optional dependency: Sodium (`AANobbMI`)

The project is submitted for Modrinth review after the first version. Do not
commit the Modrinth token or any API response scratch files.

## Archived versions

- `1.0.0` is archived at `dist/versions/FPSMaxx-1.0.0-mc26.2.jar`. It is published on Modrinth and as a GitHub release. Its tag points to the `archive/1.0.0` branch because the original source snapshot was not preserved.
- `1.1.1` is archived at `dist/versions/FPSMaxx-1.1.1-mc26.2.jar`. It was the last release before the low-end optimization pass.
- `1.2.0` is archived at `dist/versions/FPSMaxx-1.2.0-mc26.2.jar`. It added the `/fpsmaxx lowend` preset, particle culling/budgeting, first-run low-end defaults, and broader vanilla graphics reductions.
- `1.3.0` is archived at `dist/versions/FPSMaxx-1.3.0-mc26.2.jar`. It added the `/fpsmaxx potato` preset, chunk-update budgeting, removal of far particles, stronger low-end graphics reductions, and the `/fpsmaxx sectionbudget` command.
- `1.4.0` is archived at `dist/versions/FPSMaxx-1.4.0-mc26.2.jar`. It added the hidden **FpsMaxx** graphics-preset position, automatic **Fresh & Smooth** visual-safe mode, AsyncParticles/Particle Core awareness, and Iris-safe fog handling.
- `1.4.1` is the current release. It replaces the non-searchable Sodium external page with a normal **FPSMaxx** option page and an **Open FPSMaxx preset screen** button.
- The old 1.1.0 label and the old 1.2.0 release are retired. The current `1.4.1` tag points to this source release.

## Notes

- Do not commit `build/`, `run/`, `.gradle/`, or log files; they are ignored.
- The repository includes the current root jar and `dist/` jar so a clone has the
  same ready-to-install artifacts as the release.
- Sodium is optional and is not bundled. The release jar has only FPSMaxx and
  compile-only references to Sodium.
- Before tagging, test `/fpsmaxx lowend`, `/fpsmaxx potato`, `/fpsmaxx balanced`,
  and `/fpsmaxx status` on a small-heap instance. The status output should show
  low-end state, particle cull/cap, section budget, and the adaptive particle
  percentage.
- Open Video Settings and drag the Graphics Preset slider through **FpsMaxx,
  Fast, Fancy, Fabulous and Custom**. Confirm the FpsMaxx label appears at the
  far left, the flag persists in `config/fpsmaxx.json`, and vanilla presets reset
  the mod profile. With Sodium installed, confirm **Video Settings -> FPSMaxx**
  opens the same slider.
- If a new version is added, move the previous root/`dist/mods` jar into
  `dist/versions/` and add it to `dist/versions/README.md`.
