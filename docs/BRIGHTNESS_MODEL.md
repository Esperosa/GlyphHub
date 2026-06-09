# Brightness Model

## Goal

Nothing system brightness should remain the main authority. GlyphHub should not behave as if it owns a separate hardware-brightness domain that it can safely fight every frame.

## Current Direction

- System-level brightness remains authoritative.
- GlyphHub now treats frame brightness as internal relative intensity only.
- Normal app settings no longer expose the global Matrix intensity slider; it is debug-only.
- Generic transition animation relies on per-pixel intensity behavior and not brute-force full-panel brightness changes.
- `GlyphFrameNormalizer` clamps internal brightness and per-pixel intensity to 0..100.
- `RealGlyphMatrixController.mapRelativeIntensityToSdk()` is the single mapping point from app-relative intensity to SDK 0..255 values.
- Active frames with lit pixels and old stored brightness `0` are sanitized to a safe relative intensity so they do not silently render all-off.

## Problems Still Present

- Many Toy schemas still retain a stored `brightness` key for compatibility, but the schema system classifies it as advanced/debug rather than a quick control.
- The user-facing key names still need migration to `visualIntensity` or similar without breaking existing saved preferences.
- Physical phone-back comparison against Nothing OS built-in AOD brightness still needs manual validation.

## What Should Be True

- Quick settings should not pretend to override system brightness.
- If the app exposes a visual control, it should be described as contrast, dim effect, or visual intensity, not a fake hardware master brightness.
- Repeated per-frame attempts to fight system brightness should be avoided.

## Status In This Pass

- Partial, materially improved.
- The normal app-level Matrix intensity slider is hidden outside debug mode.
- Widget/app quick settings do not expose brightness.
- Every service-submitted frame now passes through central normalization/validation before reaching the controller.
- The remaining work is schema-key cleanup and physical brightness comparison against built-in Nothing Glyph behavior.
