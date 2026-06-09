# Circular Matrix Layout

Date: 2026-06-07

The Phone (4a) Pro Matrix is a circular 13x13 LED space. GlyphHub should treat it as radial hardware, not a square bitmap with rounded corners.

## Coordinate System

- Matrix size: 13x13.
- Center: `(6, 6)`.
- Coordinates: `x=0..12`, `y=0..12`.
- Radius helpers use Euclidean distance from center.

## Safe Area

The circular safe mask is the set of LEDs close enough to the center to look physically part of the Matrix disc. Off-mask pixels should be rejected or cleared for normal assets.

## Shared Helpers

`GlyphCircularLayoutEngine` provides:

- `isInSafeCircle(x, y)`
- `radius(x, y)`
- `angleDegrees(x, y)`
- center point
- ring points
- 8-direction points
- 12-clock positions
- orbit points
- progress ring positions
- radial needles
- arcs
- circular fills
- radial meters and gauges

## Required Toy Usage

Use circular helpers for:

- Compass
- Level
- Timer
- Clock analog
- Battery when practical
- Tuner gauge
- Lux meter
- Metronome
- Sound meter
- Eye
- Payment/status animations
- Maze where appropriate

## Rule

Do not draw default square boxes as placeholders. Square frames are allowed only when the Toy specifically represents a square object and the result looks intentional.
