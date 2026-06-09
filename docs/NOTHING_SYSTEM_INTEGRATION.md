# Nothing System Integration

Date: 2026-06-07

GlyphHub still uses one official Glyph Toy service path:

- Service: `com.pelikan.glyphhub.glyph.GlyphHubToyService`
- Final output target: Nothing Glyph Matrix SDK controller
- Final frame format: validated 13x13 `GlyphFrame`

## Current Rules

- The app does not set global Nothing/Glyph brightness every frame.
- Per-frame intensity is normalized internally and mapped in one place.
- Normal UI hides app-level Matrix intensity; debug-only intensity controls remain for diagnostics.
- Render ownership is centralized through the service and `GlyphRenderScheduler`.
- Status visuals are temporary events, not separate render loops.

## AOD / Default Display

Default display runs only when no Toy is active and app AOD/default-display settings allow it. Prior smoke tests verified service-side default-display logs, but the Nothing OS settings flow for selecting GlyphHub as the Always-on Glyph Toy still requires manual validation.

## Known Limits

- Physical LED smoothness and readability cannot be proven by compile/logcat alone.
- Notification, UsageStats, NFC, microphone, and activity-recognition features require user setup.
- Payment visuals are not transaction detection.
- System next-alarm integration is not claimed; only custom schedule policy is compiled in this pass.
