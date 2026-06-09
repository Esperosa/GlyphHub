# Text Rendering

## Problem

Text was not readable enough on 13x13 and several settings claimed more than the renderer could honestly support.

## Implemented In This Pass

- Added [app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTextRenderer.kt](app/src/main/java/com/pelikan/glyphhub/glyph/GlyphTextRenderer.kt)
- Added transliteration/fallback handling for Czech diacritics into ASCII-friendly glyphs
- Added:
  - compact 3x5 text rendering
  - readable 4x5 digits
  - centered single 5x7 uppercase glyphs
- Updated [app/src/main/java/com/pelikan/glyphhub/toys/GlyphToyModule.kt](app/src/main/java/com/pelikan/glyphhub/toys/GlyphToyModule.kt) to use the central text renderer
- Updated [app/src/main/java/com/pelikan/glyphhub/toys/TextScrollToyModule.kt](app/src/main/java/com/pelikan/glyphhub/toys/TextScrollToyModule.kt) so direction, spacing, loop, and pause-at-edges are now materially wired

## Supported Directions In Code

- right to left
- left to right
- top to bottom
- bottom to top

## Remaining Work

- Real device readability still needs physical validation on the phone back
- Additional glyph polish for common labels is still needed
- The current widget quick-settings surface still cannot honestly provide free-form text entry