# Schedule And Night Mode

Date: 2026-06-07

GlyphHub now has a custom schedule policy for quiet hours.

## Implemented

- App settings fields:
  - `scheduleEnabled`
  - `nightStart`
  - `nightEnd`
  - `scheduleExceptionIds`
- App settings UI for enabling the schedule and editing start/end times.
- `SchedulePolicy.matrixAllowed(...)` for quiet-window checks.
- Service/status-event integration for blocking non-exception status events during quiet time.

## Default Exceptions

- `timer`
- `alarm`
- `charging_full`
- `payment`

## Not Yet Implemented

- Custom wake alarm scheduler.
- System next-alarm lookup.
- Per-day schedule rules.
- Schedule test screen.

## Validation Required

- Test quiet-window blocking at exact configured times.
- Test allowed exceptions.
- Confirm active Toy/default-display behavior during the quiet window.
- Document exact-alarm limitations if custom wake scheduling is added.
