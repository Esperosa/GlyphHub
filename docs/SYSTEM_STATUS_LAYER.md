# System Status Layer

Date: 2026-06-07

GlyphHub now routes temporary system/status visuals through `GlyphStatusEventRouter` inside `GlyphHubToyService`.

## Ownership Rules

- Active Toy output remains the normal owner of the Matrix.
- Status events are temporary overlays/interruption frames with priority and TTL.
- Low-priority status events are ignored while an active Toy is running.
- No status feature may create its own Matrix render loop.
- All status frames still pass through `GlyphRenderScheduler` and frame validation before reaching the SDK.
- After a status event expires, the service returns to the previous active Toy or default-display path.

## Implemented Event Types

| Event | Source | Priority | Status |
| --- | --- | --- | --- |
| Charging | `GlyphStatusReceiver` power/battery broadcasts | medium/high | compiled; needs plug/unplug validation |
| Volume | `GlyphStatusReceiver` volume broadcast | low | compiled; needs live validation |
| Notification | `GlyphNotificationListenerService` | low | compiled; needs opt-in permission flow validation |
| Network | status event type and Network Status Toy | low | compiled; needs callback/manual validation |
| Payment visual | manual/status event type | medium/high | compiled visual only; no payment success detection |
| Beacon | Beacon Toy and status event type | high | compiled; needs manual validation |
| Wake/alarm | status event type | high | event type exists; custom wake scheduling still TODO |

## Schedule Interaction

`SchedulePolicy` blocks status events during the quiet window unless the event maps to an allowed exception such as timer, alarm, charging full, or payment. This is compiled but not yet time-window tested on hardware.

## Remaining Work

- Add UI triggers for debug status-event tests.
- Validate charging, volume, notification, network, beacon, and payment/manual events on the phone.
- Add a custom wake scheduler if the user enables a wake schedule.
- Keep Notification Pulse and Wallet/NFC triggers setup-gated until permissions and live data are confirmed.
