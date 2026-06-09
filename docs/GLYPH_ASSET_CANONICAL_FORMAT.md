# Glyph Asset Canonical Format

Date: 2026-06-07

GlyphHub's canonical Matrix asset format is app-owned JSON that represents exactly one 13x13 Glyph Matrix icon or animation.

## Goals

- Keep every asset editable and reviewable.
- Preserve frame duration and loop behavior.
- Normalize brightness without fighting system Glyph brightness.
- Enforce the Phone (4a) Pro circular safe area.
- Make import/export possible from external tools without letting external formats leak into runtime.

## Top-Level Fields

```json
{
  "id": "coin_spin_depth",
  "name": "Coin Spin Depth",
  "matrix": 13,
  "type": "animation",
  "loop": false,
  "fps": 24,
  "respectCircularMask": true,
  "normalizeBrightness": true,
  "frames": [],
  "metadata": {}
}
```

| Field | Required | Meaning |
|---|---:|---|
| `id` | Yes | Stable unique asset id. Use lowercase snake case. |
| `name` | Yes | Human-readable name. |
| `matrix` | Yes | Must be `13`. |
| `type` | Yes | `icon`, `animation`, `text`, `transition`, or `imported`. |
| `loop` | No | Whether playback loops. Defaults to `false`. |
| `fps` | No | Default frame rate when a frame omits `durationMs`. |
| `respectCircularMask` | No | If true, off-mask pixels are sanitized or rejected. Defaults to `true`. |
| `normalizeBrightness` | No | If true, intensities are clamped/normalized before output. Defaults to `true`. |
| `frames` | Yes | One or more frame objects. |
| `metadata` | No | Tooling/source/validation details. |

## Frame Fields

Frames may use compact row strings or explicit intensity rows.

```json
{
  "durationMs": 42,
  "pixels": [
    "0000000000000",
    "0000011100000",
    "0000111110000",
    "0001111111000",
    "0000111110000",
    "0000011100000",
    "0000000000000",
    "0000000000000",
    "0000000000000",
    "0000000000000",
    "0000000000000",
    "0000000000000",
    "0000000000000"
  ]
}
```

`pixels` is always 13 strings of length 13.

Supported compact pixel characters:

- `0` or `.`: off
- `1` through `9`: increasing relative intensity
- `A` through `Z`: reserved high-resolution intensity symbols for importers

Internally these values are normalized to 0..100. A binary asset should use only `0` and `1`.

## Metadata

Recommended metadata:

```json
{
  "category": "transition",
  "intendedUse": ["coin", "payment", "activation"],
  "source": "internal",
  "sourceTool": "GlyphHub",
  "validated": true,
  "allowAllOff": false,
  "allowFullMatrixFlash": false,
  "allowOffMaskPixels": false,
  "particleEffect": false,
  "directional": false,
  "notes": "Designed for 13x13 Phone (4a) Pro."
}
```

## Brightness

Canonical brightness is relative. It is not a hardware/global brightness setting.

- Internal range: 0..100.
- SDK mapping happens only in the renderer/controller layer.
- Importers must normalize accidental gradients unless metadata marks gradients as deliberate.
- Full-on binary designs must use one uniform non-zero level.

## Validation

An asset is not production-ready until it passes `GlyphFrameValidator`.

The validator checks:

- Matrix size.
- Circular mask.
- All-on/all-off mistakes.
- Stray pixels.
- Bounding box size.
- Centering.
- Intensity range and unique intensity count.
- Text renderer provenance when applicable.

## Example Animation Asset

```json
{
  "id": "coin_spin_depth",
  "name": "Coin Spin Depth",
  "matrix": 13,
  "type": "animation",
  "loop": false,
  "fps": 24,
  "respectCircularMask": true,
  "normalizeBrightness": true,
  "frames": [
    {
      "durationMs": 42,
      "pixels": [
        "0000000000000",
        "0000011100000",
        "0000111110000",
        "0001111111000",
        "0000111110000",
        "0000011100000",
        "0000000000000",
        "0000000000000",
        "0000000000000",
        "0000000000000",
        "0000000000000",
        "0000000000000",
        "0000000000000"
      ]
    }
  ],
  "metadata": {
    "category": "transition",
    "intendedUse": ["coin", "payment", "activation"],
    "validated": true
  }
}
```
