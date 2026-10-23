# Releasing a new version

Every version is committed, tagged, and pushed. The GitHub Actions **Release**
workflow builds the tagged source and publishes the jar automatically.

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

The `Release` workflow runs for tags matching `v*`. It builds with Java 25 and
uploads `FPSMaxx-<version>-mc26.2.jar` to the GitHub release.

## Notes

- Do not commit `build/`, `run/`, `.gradle/`, or log files; they are ignored.
- The repository includes the current root jar and `dist/` jar so a clone has the
  same ready-to-install artifacts as the release.
- Sodium is optional and is not bundled. The release jar has only FPSMaxx and
  compile-only references to Sodium.
