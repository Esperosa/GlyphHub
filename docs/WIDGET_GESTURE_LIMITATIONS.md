# Widget Gesture Limitations

Android home-screen widgets are rendered through `RemoteViews`. Most launchers intercept direct swipe gestures for page navigation, workspace movement, widget resizing, or launcher-specific behavior. Because of that, true custom swipe handling inside a widget is not reliable across launchers.

GlyphHub uses one production-safe strategy:

1. The widget shows one centered Toy preview inside a compact card.
2. All interaction goes through five explicit `TextView` tap zones: top, bottom, left, right, and center.

The fallback zones are implemented with `RemoteViews`-allowed `TextView` hit targets. A plain `android.view.View` hit target was rejected by Nothing Launcher, so the widget layout intentionally avoids unsupported classes.

## Current Main Mapping

- Center tap: `ACTION_WIDGET_TOGGLE_SELECTED_TOY`
- Top tap zone: `ACTION_WIDGET_PREVIOUS_TOY`
- Bottom tap zone: `ACTION_WIDGET_NEXT_TOY`
- Left tap zone: `ACTION_WIDGET_OPEN_TOY_SETTINGS`, rendered as an inline Toy settings panel inside the widget
- Right tap zone: opens the full GlyphHub app on the app settings screen

The 2x2 card is treated as five practical touch regions: top, bottom, left, right, and center. Android `RemoteViews` does not support polygon or triangular hit paths, so GlyphHub uses non-custom `TextView` regions that approximate those sections while staying launcher-compatible. The hit targets are sized to avoid overlap on Nothing Launcher so side taps do not fall through to the center action.

## Inline Panel Mapping

The Toy quick-settings panel is not a separate Activity. It is another `RemoteViews` state inside the same 2x2 widget card.

- Center tap: accept the current panel state and close the panel
- Top tap zone: previous setting
- Bottom tap zone: next setting
- Left tap zone: decrease or previous value
- Right tap zone: increase or next value

The panel intentionally exposes only settings that can be adjusted safely through a widget: booleans, numeric values, and fixed choices. Free text editing and full app settings belong in the Compose app because `RemoteViews` does not provide a reliable inline keyboard text-input model for launcher widgets.

## Intended Gesture Mapping

- Swipe up: next Toy
- Swipe down: previous Toy
- Swipe left: current Toy settings
- Swipe right: app settings
- Tap: activate/deactivate current Toy

Long press is intentionally unused because Android launchers reserve long press for moving, resizing, or removing widgets.

## Overlay Limitation

Android widgets cannot create an arbitrary launcher-level overlay, cannot reliably detect taps outside the widget bounds, and cannot consume the system Back button like an Activity or dialog. GlyphHub therefore implements the requested small Toy settings panel as a fixed-size inline widget state. It opens and closes inside the widget card and keeps all controls reachable from the same five zones.

## Tested Status

- Nothing Launcher on `A069P` / `FroggerPro`: center, top, bottom, and left zones were tested on a 2x2 widget after the latest non-overlapping layout rewrite.
- Center toggled active state and showed the red `#E60012` active style.
- Top and bottom changed the carousel selection with wraparound repository logic.
- Left opened the inline Toy settings panel.
- Right is wired to open the full app settings screen through an Activity `PendingIntent`. The app-settings route itself was verified with UIAutomator; a clean physical right-zone tap still needs one final launcher-page validation because the last attempted tap landed after the launcher page changed.
- True custom swipe attempts on Nothing Launcher were intercepted by launcher page/app-drawer gestures.
- Left/right custom swipe remains unsupported by Android widget API, so settings use tappable zones.
- Larger fallback hit regions were tested through ADB taps on Nothing Launcher: `PREVIOUS_TOY`, `NEXT_TOY`, `OPEN_TOY_SETTINGS`, `CLOSE_PANEL`, and `TOGGLE_SELECTED_TOY` reached the intended path.
- Latest layout pass replaced the overlapping `FrameLayout` hit model with a non-overlapping `LinearLayout` tree. UIAutomator exposed exact child bounds on Nothing Launcher, and ADB taps against those bounds verified top = `PREVIOUS_TOY`, bottom = `NEXT_TOY`, left = `OPEN_TOY_SETTINGS`, and center = panel close/toggle behavior.
