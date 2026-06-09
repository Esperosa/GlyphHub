# Animation Pipeline Decision

Date: 2026-06-07

## Decision

GlyphHub will use an internal canonical 13x13 `GlyphAsset` / `GlyphAnimationAsset` / `GlyphFrameSequence` pipeline as the primary animation system.

External animation systems are optional import sources only:

- Glyph Matrix Editor JSON/pixel data: preferred external editor/import target.
- Bitmap/GIF/sprite sheet: preferred generic pixel-art import path.
- Lottie JSON: optional importer for simple high-contrast vector motion after dependency review.

No Toy should own an unrelated animation engine. Toys should use shared assets and primitives from:

- `GlyphAnimationLibrary`
- `GlyphIconLibrary`
- `GlyphTextRenderer`
- `GlyphCircularLayoutEngine`
- `GlyphMotionPrimitives`
- `GlyphFrameValidator`
- `GlyphFrameNormalizer`

Only validated, normalized 13x13 `GlyphFrame` sequences should reach `GlyphMatrixController`.

## Why This Pipeline

- The hardware target is tiny and circular. General animation formats can help author motion, but they cannot guarantee readable 13x13 frames.
- The Nothing SDK is the final renderer, but it is not an asset validation or authoring system.
- A canonical JSON format makes assets easy to diff, review, import, export, validate, and preview.
- A central validator prevents accidental all-on flashes, off-mask pixels, mixed-intensity artifacts, tiny unreadable icons, and stray pixels from being repeated in every Toy.
- Brightness can be normalized once before SDK mapping instead of handled by scattered Toy code.

## Runtime Architecture

1. Toy or UI requests an icon, animation, text, circular gauge, or motion primitive.
2. Shared library creates a `GlyphFrame` or `GlyphFrameSequence`.
3. `GlyphFrameNormalizer` clamps dimensions, circular mask, and relative intensity.
4. `GlyphFrameValidator` checks safety and quality rules.
5. `GlyphAnimationPlayer` chooses the current frame with stable timing.
6. `GlyphMatrixController` receives only safe frames.

## Canonical Format

- Matrix size: 13 only.
- Coordinates: row-major `x=0..12`, `y=0..12`.
- Internal brightness: normalized 0..100.
- `0` means off. Non-zero means relative intensity, not system brightness.
- Asset-level flags control circular mask, brightness normalization, loop behavior, and active-animation all-off policy.
- Metadata records category, intended use, validation state, source tool, and importer notes.

## Importer Policy

| Importer | Status | Decision |
|---|---|---|
| Bitmap/GIF/sprite importer | Feasible now | Implement minimal bitmap-frame interface and static bitmap downsampling first; GIF/sprite timing can follow |
| Glyph Matrix Editor importer | Feasible after schema sampling | Implement clean-room parser for exported JSON/pixel arrays when schema is confirmed |
| Lottie importer | Feasible but heavier | Create interface/stub now; add real dependency and rasterization only after app size/licensing/performance review |
| AnimatedVectorDrawable importer | Possible | Do not prioritize; Android XML authoring is weaker than Lottie/pixel tools |

## Brightness Decision

The system/device owns global Glyph brightness. GlyphHub only owns relative per-frame intensity.

- Do not expose brightness as a quick setting.
- Do not set global brightness every frame.
- Normalize internal intensity to 0..100.
- Map 0..100 to the SDK in one place only.
- If per-pixel/object brightness is unstable on device, quantize to on/off or a small number of safe buckets.

## Validation Decision

Every outgoing frame should be validated or sanitized. Production fallback is previous valid frame, never all-on/full-bright replacement.

Initial enforced rules:

- 13x13 dimensions.
- No accidental all-on frames.
- No accidental all-off frames during active animations unless allowed.
- No off-circular-mask pixels unless allowed.
- No full-matrix flash unless intentional and debug-enabled.
- No non-normalized intensities.
- Stray-pixel warnings.
- Too-small/off-center warnings.
- Text assets must come from registered text renderer, not ad-hoc row maps.

## Editor / Preview Decision

Add a debug-only Asset Quality / Animation Lab in the app. It should list canonical assets, preview 13x13 frames, show the safe circular mask, bounding box, center of mass, brightness histogram, and validator warnings.

The first implementation can be read-only and local-preview only. Import/export and real-device test buttons can be added after the core model is stable.

## Non-Goals For This Pass

- Full Lottie dependency integration.
- Full GIF decoder integration.
- Copying GPL editor code from Glyph Matrix Editor.
- Replacing the Nothing SDK render layer.
- Claiming production visual quality before physical Phone (4a) Pro validation.

## Consequences

- Some older local pixel maps can remain temporarily only if wrapped as validated assets or routed through shared primitives.
- More code will move from `com.pelikan.glyphhub.toys` into `com.pelikan.glyphhub.glyph.assets`, `glyph.animation`, and `glyph.validation`.
- Asset quality becomes testable and previewable instead of being judged only by manual toy activation.
