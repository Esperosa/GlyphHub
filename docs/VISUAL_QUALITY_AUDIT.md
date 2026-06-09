# Visual Quality Audit

Audit status for the current 2026-06-06 GlyphHub build on Nothing Phone (4a) Pro / `A069P` / 13x13 Glyph Matrix.

This document separates compile/runtime proof from physical visual judgement. Logcat can prove that frames are sent through the real Glyph SDK path, but final smoothness, flicker, and readability still need direct observation of the phone-back Matrix.

## Current Findings

| Module | Status | Current visual state | Remaining work |
| --- | --- | --- | --- |
| Dice | improved | App/widget preview and runtime d6 result now share the same larger square dice shell with single-LED pips; runtime roll animation no longer brightness-pulses. | Phone-back validation for roll cadence, shake debounce, and settle hold. |
| Coin | partial | Circular-ish shell and edge frames exist, but result faces still lean on centered text. | Replace more label-centric frames with clearer heads/tails glyphs and tune flip cadence. |
| Compass | improved | Thin circular outline, 8-way cardinal text, edge-dot mode, and needle mode are present; ring/needle now use the fuller 13x13 radius. | Add truthful calibration quality feedback and validate rotation smoothness on hardware. |
| Clock | improved | Analog circular mode exists; abstract/dot modes use circular composition and fuller outer ring radius rather than square framing. | Validate centering/flicker on physical LEDs and simplify modes that are not readable enough. |
| Battery | improved | App/widget preview is battery-shaped; runtime charging/low states no longer use hard brightness blinking and progress uses the fuller outer ring radius. | Tune charging/full/low state symbols and validate legibility on device. |
| Pixel Art | improved | Built-in icon library, custom rows, editor actions, and app/widget custom preview work. The heart preset is centered/full-size, static icons no longer shift during animation modes, and generic MIT community-inspired animated presets are available. | Add multiple named custom assets, import/export, and timeline preview. |
| Text Scroll | partial | Central text renderer and direction settings are wired. | Validate real readability and pacing on the phone back. |
| Idle / Default Display | partial | Clock/battery/date/custom/default-Toy paths exist in settings and service fallback. | Finish selected default Toy / last-active behavior polish and AOD validation. |
| Level | partial | Circular target/bubble concept exists. | Validate orientation, calibration, and haptic feel on phone. |
| Timer | partial | Functional countdown; completion no longer hard-blinks the whole frame. | Improve completion behavior with a service-owned non-flicker transition and better setup UI. |
| School Class Timer | partial | Basic schedule display exists. | Add timetable presets and clearer schedule visuals. |
| Eye | partial | Weighted state behavior exists instead of a fixed short loop. | Tune behavior on hardware and keep optional camera/person tracking out unless implemented properly. |
| Maze | experimental | Functional tilt maze, still rectangular in spirit. | Redesign around circular path/ring logic or keep experimental. |
| Weather | experimental | Manual icons exist, no real provider/cache/city integration. | Either implement real provider/cache or keep experimental. |
| Lux Meter | partial | Sensor-backed readout exists. | Redesign as circular gauge/needle and validate sensor fallback. |
| Rock Paper Scissors | experimental | Basic game/result frames exist; reveal animation no longer brightness-pulses. | Add polished countdown/reveal or keep out of primary surfaces. |
| Tuner | partial | Microphone-backed tuner exists and is setup-gated. | Validate pitch stability, note targeting, and Matrix readability on hardware. |

## Cross-Cutting Status

- Matrix target is 13x13-first. No production render path should assume a different matrix size.
- Real SDK calls are isolated in the glyph controller/service layer.
- Fake fallback compile path is verified by temporarily removing `app/libs/glyph-matrix-sdk-2.0.aar`.
- Widget visuals now render one centered LED-only preview with larger hit zones, not helper text.
- Widget/app previews now use stable per-Toy preview frames for every registered Toy instead of sampling animated runtime frames.
- The 2026-06-07 phone UI pass fixed blank Compose previews by rendering fixed-size LED grids and replaced helper-dependent preview icons with explicit 13x13 patterns. Launcher widget smoke confirmed Dice and Coin previews render as centered LED-only icons with unclipped labels.
- A later 2026-06-07 pass replaced misleading circular Dice/Battery previews with object-specific 13x13 previews and removed hard global brightness pulsing from normal Toy render loops.
- The 2026-06-07 icon/editor pass added a shared PixelArt icon library with 30+ original 13x13 presets and made custom editor output render in the app and launcher widget.
- The follow-up community visual pass added `CommunityGlyphVisuals`, adapted generic MIT GlyphMo-style visuals, added Toyph brightness-grid import support, kept GPL Matrix Lab reference-only, and documented all third-party decisions in `docs/THIRD_PARTY_NOTICES.md`.
- The same pass expanded generic transitions, progress rings, gauges, compass, clock, battery, level, and tuner circular elements toward the full physical 13x13 circular mask.
- Activation/deactivation animation is handled by `GlyphHubToyService` and `GlyphTransitionEngine`, not by widget animation.
- `GlyphRenderScheduler`, circular design helpers, and the updated fade/glitch transition paths reduce flash-like and square full-panel output, but physical no-flash validation is still required.

## Remaining High-Risk Items

1. Physical phone-back visual validation for flicker, stray pixels, and centering by direct observation.
2. Nothing OS AOD / Always-on Glyph Toy settings validation.
3. Broader circular redesign for Coin, Timer, Maze, Weather, Lux Meter, and Rock Paper Scissors.
4. More truthful quick-setting whitelists per Toy instead of broad schema-order exposure.
5. Long-run manual switching soak to catch rare Matrix glitches that short ADB activation tests may miss.
