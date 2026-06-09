# Glyph Asset Format

GlyphHub asset JSON files describe 13x13 matrix animations.

```json
{
  "id": "dice_6",
  "name": "Dice 6",
  "matrix": 13,
  "loop": false,
  "frames": [
    {
      "durationMs": 120,
      "brightness": 80,
      "pixels": [
        "0000000000000",
        "0001111111000",
        "0010000000100",
        "0010100010100",
        "0010000000100",
        "0010001000100",
        "0010000000100",
        "0010100010100",
        "0010000000100",
        "0001111111000",
        "0000000000000",
        "0000000000000",
        "0000000000000"
      ]
    }
  ]
}
```

## Fields

- `id`: stable asset id.
- `name`: display name.
- `matrix`: expected matrix size. GlyphHub assets use `13`.
- `loop`: whether playback should loop.
- `frames`: ordered animation frames.
- `durationMs`: frame duration in milliseconds.
- `brightness`: frame brightness from `0` to `100`.
- `pixels`: exactly 13 strings, each exactly 13 characters. `1` means lit, `0` means off.

Transition files live in `app/src/main/assets/glyphs/transitions/`. The runtime transition engine also generates 13x13 transition frames programmatically so transitions remain available even if assets are not loaded.

## Toyph-Compatible Single Glyphs

`GlyphAssetLoader` also accepts the community Toyph single-glyph shape:

```json
{
  "device": "Phone4aPro",
  "gridSize": 13,
  "version": "0.1.0",
  "brightness": [0, 0, 4095]
}
```

Rules:

- `gridSize` must be `13`.
- `brightness` must contain exactly `169` values in row-major order.
- Values are treated as `0..4095` source intensity and converted to GlyphHub `0..100` per-pixel intensity.
- The frame is masked through the physical 13x13 circular LED layout before rendering.
- Toyph demo assets are supported as user/import assets, but trademark/IP-adjacent symbols are not bundled as GlyphHub defaults.
