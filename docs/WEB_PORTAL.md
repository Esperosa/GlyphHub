# Web Portal

Date: 2026-06-07

GlyphHub uses NanoHTTPD `2.3.1` for a disabled-by-default LAN portal.

## Security Model

- Disabled by default.
- Random token/PIN generated when enabled.
- Every route requires `?token=` or `x-glyphhub-token`.
- No arbitrary code upload.
- No plugin installation.
- Only settings, Matrix test frames, and canonical asset-like JSON are accepted.

## Compiled Routes

| Route | Method | Purpose | Status |
| --- | --- | --- | --- |
| `/` | GET | Minimal web UI with 13x13 editor | compiled; browser test pending |
| `/status` | GET | Current selected/active Toy and portal status | compiled; route test pending |
| `/toys` | GET | Toy list with categories/visibility | compiled; route test pending |
| `/activate?toy=id` | GET | Activate a known Toy | compiled; route test pending |
| `/deactivate` | GET | Stop active Toy | compiled; route test pending |
| `/asset/upload` | POST | Save safe 13x13 JSON rows to PixelArt custom asset | compiled; route test pending |
| `/asset/export` | GET | Export current PixelArt rows as JSON | compiled; route test pending |
| `/matrix/test-frame` | POST | Validate rows, save as PixelArt custom, activate PixelArt | compiled; route test pending |
| `/logs` | GET | Return recent GlyphHub debug logs | compiled; route test pending |
| `/settings/export` | GET | Export selected safe app settings | compiled; route test pending |
| `/settings/import` | POST | Import selected safe app settings | compiled; route test pending |

## Remaining Work

- Test all routes on device/LAN with token.
- Show actual local IP in app UI.
- Add a polished web editor UI beyond the minimal 13x13 grid.
- Add persistent named asset storage instead of only PixelArt custom rows.
