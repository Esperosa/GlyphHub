# Third-Party Notices

GlyphHub keeps third-party code and visual ideas explicit. Only compatible sources are adapted into production code.

## Adapted MIT Sources

### GlyphMo

- Source: https://github.com/anamite/GlyphMo
- License: MIT
- Copyright: Copyright (c) 2024 Anand
- Use in GlyphHub: generic 13x13 animation ideas were rewritten into `CommunityGlyphVisuals.kt` for GlyphHub's `GlyphFrame` renderer and circular 13x13 physical LED mask. Adapted ideas include heartbeat, rings, equalizer bars, fire/noise, spinner, stars, rocket, and timer ring style visuals.
- Not copied as-is: Android `Bitmap` / `Canvas` rendering, 25x25 web-designer workflow, APK packaging, notification rule implementation, and IP-specific arcade-style visuals.

### Toyph

- Source: https://github.com/antonvidishchev/toyph
- License: MIT
- Copyright: Copyright (c) 2026 Anton Vidishchev
- Use in GlyphHub: the Toyph single-glyph `gridSize: 13` + `brightness[]` JSON asset shape is supported by `GlyphAssetLoader.kt`.
- Not bundled as default production assets: Toyph's demo Star Wars-style glyphs are not included in the default GlyphHub preset library because the symbols themselves are trademark/IP-adjacent even though the repository code is MIT.

### Official Nothing Glyph Matrix Example

- Source: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project
- License: MIT
- Copyright: Copyright (c) 2025 Nothing Technology Limited
- Use in GlyphHub: reference for official service/SDK setup patterns only. SDK calls remain isolated behind GlyphHub's controller/service layer.

## Reference-Only Sources

### Matrix Lab

- Source: https://github.com/alex-1121/glyph-matrix-lab
- License: GPL-3.0
- Use in GlyphHub: product and testing reference only. No Matrix Lab code or assets are copied into GlyphHub.

## Maintenance Rule

If a new community visual, algorithm, or asset is copied or materially adapted, add the source, license, exact files, and adaptation notes here before shipping it.
