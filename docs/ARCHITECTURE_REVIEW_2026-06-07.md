# Architecture Review

Date: 2026-06-07

## Scope

This pass reviewed the service-owned Nothing Glyph integration, Android manifest, Toy registry, runtime modules, settings, widget flow, and user-facing audit docs.

## Current Shape

- Glyph Matrix access is correctly centralized in `GlyphHubToyService` and `GlyphMatrixController`.
- `RealGlyphMatrixController` uses the official SDK classes through reflection and falls back to `FakeGlyphMatrixController` when the AAR is absent.
- Phone (4a) Pro constraints are reflected in the code path: `DEVICE_25111p`, 13x13 matrix detection, AOD support metadata, and no UI/widget direct SDK writes.
- Toy modules are modular and registered through `ToyRegistry`; hidden/default behavior is separated from visible carousel entries.
- Widget actions update settings and request service sync/activation instead of rendering to the Matrix directly.

## Cleanup Applied

- Removed manifest permissions for NFC and Usage Stats because those trigger paths are documented as future work and are not implemented.
- Removed manifest `BATTERY_CHANGED`; it is a sticky broadcast and should not be claimed as a manifest-delivered status path.
- Changed Glyph Toy `EVENT_CHANGE` handling to sync from settings instead of pretending it is a Toy interaction.
- Kept direct Toy interaction mapped only from explicit action/touch events and app/widget controls.
- Added a Weather refresh generation guard so stale network threads do not update module state after deactivation/reactivation.
- Centralized web portal start/stop side effects in `MainActivity`; the settings screen now only updates settings.
- Updated stale weather readiness wording to reflect the Open-Meteo provider.

## Remaining Risks

- Many Toys are compile-present but still need real phone-back visual validation before being called production-ready.
- Microphone, step, notification listener, schedule, boot, and network paths need live permission/device validation.
- Widget quick-panel setting adjustment still needs final launcher validation.
- Notification/status layer is scaffolded, but app filtering and full permission UX are not finished.
- `ToyRegistry` uses singleton module instances. This is acceptable for the current one-process app, but runtime state must stay out of preview rendering.

## Recommended Debug Order

1. Re-run build and install baseline.
2. Validate official Nothing AOD selection and `EVENT_AOD` sync.
3. Validate core AOD-safe Toys: Clock, Battery, Text Scroll, Pixel Art, Weather, Idle Default.
4. Validate sensor Toys: Compass, Level, Lux, Orientation, Motion, Maze, RPS.
5. Validate audio Toys after permission grant: Tuner, Sound Meter, Metronome.
6. Validate system/status Toys: Network, Payment Visual, Beacon, notification listener.
7. Record every physical LED result in `docs/PHONE_TEST_RESULTS.md`.
