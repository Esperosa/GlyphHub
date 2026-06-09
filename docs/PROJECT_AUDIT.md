# Project Audit Map

Last cleanup pass: 2026-06-07.

## Source of Truth

- Android application source: `app/src/main/`
- Build configuration: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/`
- Development scripts: `scripts/`
- Architecture and validation docs: `docs/`
- Local Nothing SDK AAR location: `app/libs/glyph-matrix-sdk-2.0.aar`

## Generated or Local-Only Files

These are intentionally not source of truth:

- `.gradle/` caches except `.gradle/bootstrap/`, `.kotlin/`, `build/`, `app/build/`
- `local.properties`
- `artifacts/runs/`
- root-level screenshots, XML dumps, build output text files, and `todos.json`

Run `.\scripts\clean-workspace.ps1` to remove generated build/cache directories and archive root-level run output into `artifacts/runs/workspace-sweep-2026-06-07`. The script keeps `.gradle/bootstrap/` because this project uses a custom wrapper script that stores the downloaded Gradle distribution there.

Historical loose device evidence from `artifacts/` was archived to `artifacts/runs/legacy-device-evidence-2026-06-06`. Future loose files in `artifacts/` are moved to `artifacts/runs/loose-artifacts` by the cleanup script.

## Debugging Order

Use this order when auditing or debugging individual app areas:

1. Build baseline: `.\scripts\build-debug.ps1`
2. Service and Matrix path: `glyph/GlyphHubToyService.kt`, `glyph/RealGlyphMatrixController.kt`, `glyph/FakeGlyphMatrixController.kt`
3. Toy registry and selected Toy: `toys/ToyRegistry.kt`, then the specific `*ToyModule.kt`
4. Settings persistence: `settings/`
5. UI flow: `ui/screens/` and `ui/components/`
6. Widget flow: `widget/` and `app/src/main/res/layout/glyphhub_widget.xml`
7. Hardware validation: update `docs/PHONE_TEST_RESULTS.md`

## Current Audit Notes

- No `.git/` repository is present in this workspace, so cleanup was done conservatively.
- Historical device screenshots and XML dumps remain under `artifacts/` unless explicitly archived by the cleanup script.
- The codebase already has architecture, testing, implementation status, and TODO docs. Keep those updated after each module-level debug pass.
- Nothing Glyph Toy integration is intentionally service-owned. App/widget actions request service sync or activation; UI code should not call the SDK directly.
- Phone (4a) Pro is treated as a 13x13 AOD-first target. Glyph Toy `EVENT_CHANGE` is handled as a system selection/sync event; direct Toy interaction remains limited to explicit touch/action events and app/widget controls.
