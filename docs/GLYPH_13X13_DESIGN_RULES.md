# Glyph 13x13 Design Rules

## Core Principle

The 13x13 Glyph Matrix on Nothing Phone (4a) Pro should be treated as a circular/dot field, not as a tiny square LCD.

## Rules

1. Prefer center, ring, arc, orbit, needle, and clustered-dot compositions.
2. Avoid square borders unless the Toy deliberately needs that shape.
3. Use the circular-safe masks from [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphDesignSystem.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphDesignSystem.kt).
4. Remove stray isolated pixels unless they are deliberate and visually meaningful.
5. Keep primary information near the center-safe area.
6. Use edge-ring space for progress, orientation, and emphasis rather than defaulting to a full rectangular frame.

## Helpers Added In This Pass

- `circularMask13`
- `softCircularMask13`
- `innerAreaMask`
- `edgeRingMask`
- `centerSafeArea`
- `drawPixelSafe`
- `drawDot`
- `drawSoftDot`
- `drawRing`
- `drawCircleApprox`
- `drawArcApprox`
- `drawNeedle`
- `drawGauge`
- `drawProgressRing`
- `drawCenteredIcon`
- `removeStrayPixels`
- `validateAgainstCircularMask`

## Applied This Pass

- Generic transitions now use circular-safe patterns instead of square/full-panel frames.
- Eye and Level now render against the circular/radial design language instead of flat placeholder geometry.