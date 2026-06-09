# Animation Engine

## Goal

Animations must be deterministic, non-blocking, and visually coherent on the 13x13 matrix.

## Changes In This Pass

- Generic transition shapes in [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTransitionEngine.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTransitionEngine.kt) were rewritten away from square/all-on patterns.
- The 2026-06-07 transition pass expanded `appear`, `burst`, `fade`, and `glitch` transitions toward the full 13x13 physical circular mask and lowered transition frame duration to 72 ms for smoother service-owned activation/deactivation.
- Community animation/editor projects were reviewed: Toyph and GlyphMo are useful MIT-compatible references for JSON/frame-driven effects, while Glyph Matrix Lab is GPLv3 and therefore reference-only unless licensing changes.
- Generic GlyphMo-inspired visuals are now rewritten in [app/src/main/java/com/pelikan/glyphhub/toys/CommunityGlyphVisuals.kt](app/src/main/java/com/pelikan/glyphhub/toys/CommunityGlyphVisuals.kt) and routed through the same 13x13 circular-mask renderer as the rest of GlyphHub.
- PixelArt no longer animates ordinary static icons by shifting the whole image left/right. That keeps custom icons, the heart preset, and widget previews centered; only explicit animated community presets animate.
- Eye now uses weighted behavior states instead of a fixed loop in [app/src/main/java/com/pelikan/glyphhub/toys/EyeToyModule.kt](app/src/main/java/com/pelikan/glyphhub/toys/EyeToyModule.kt).
- Dice now has settle/debounce behavior in [app/src/main/java/com/pelikan/glyphhub/toys/DiceToyModule.kt](app/src/main/java/com/pelikan/glyphhub/toys/DiceToyModule.kt).
- Coin now uses less abrupt phase changes and requested result-set semantics in [app/src/main/java/com/pelikan/glyphhub/toys/CoinToyModule.kt](app/src/main/java/com/pelikan/glyphhub/toys/CoinToyModule.kt).
- PixelArt no longer uses pulse-style default animation; built-in icon selection and custom rows render through one shared icon library.

## Still Missing

- A single explicit service-owned animation policy/validator for every rendered frame
- Shared easing/random-seed primitives exposed as first-class engine utilities
- Full direct-observation audit of every Toy animation against the no-flash policy on the physical phone-back Matrix
