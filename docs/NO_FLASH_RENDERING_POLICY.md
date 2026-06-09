# No-Flash Rendering Policy

This policy exists because the Glyph Matrix must never appear to full-flash during normal Toy updates, activation, deactivation, or transition playback.

## Rules

1. No ordinary render path may clear between normal frames.
2. No generic transition may use a full 13x13 all-on frame.
3. No helper may silently convert partial-intensity overlays into full-intensity pixels.
4. No transition may be designed as a rectangular LCD wipe that ignores the circular/dot character of the 13x13 field.
5. Accidental full-panel frames are suspicious by default.
6. Debug-only flash behavior must never be the default runtime behavior.

## Implemented In This Pass

- [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphFrame.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphFrame.kt)
  Overlay and shift now preserve incoming pixel intensity instead of collapsing everything to full-on pixels.
- [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphDesignSystem.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphDesignSystem.kt)
  Adds circular masks, cleanup helpers, and accidental-full-panel detection support.
- [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTransitionEngine.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTransitionEngine.kt)
  Generic transitions were rewritten away from square/all-on frames toward masked circular-safe patterns.
- [app/src/main/java/com/pelikan/glyphhub/glyph/validation/GlyphFrameValidator.kt](app/src/main/java/com/pelikan/glyphhub/glyph/validation/GlyphFrameValidator.kt)
  Adds central validation for 13x13 size, all-on, all-off, off-mask pixels, isolated pixels, tiny frames, centering, and normalized intensity.
- [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphRenderScheduler.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphRenderScheduler.kt)
  Final service frames now pass through the validator and keep the previous valid frame when unsafe output is rejected.

## Still Required

- Physically validate the new validator path on the phone-back Matrix during normal activation, deactivation, and Toy switching.
- Expand warning-to-fix migration for remaining Toy visuals that still report tiny/off-center/isolated-pixel warnings.

## Manual Validation Checklist

- Activate Dice, Clock, Battery, Eye, Timer, and Pixel Art from the phone.
- Deactivate each module back to default/AOD.
- Confirm there is no whole-panel flash at the start, middle, or end of each update.
- Confirm transitions no longer look like square LCD wipes.
- If a suspicious flash is observed, record the module, trigger, and whether it was activation, deactivation, or steady-state update.
