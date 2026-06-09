# Additional Toys

This document records the newly added Toy surface from the 2026-06-06 continuation pass. These modules now exist in `ToyRegistry` and compile into the app.

## Newly Added Requested Toys

- `Level`: accelerometer-driven bubble level with axis selection, optional guides, and tap-to-recenter.
- `Timer`: countdown timer with tap pause/resume, optional shake reset, and a visible progress bar.
- `Eye`: animated eye with scanning/restless motion modes and tap-triggered blink.
- `RPS`: shake/tap rock-paper-scissors throw with animated cycling before the result.
- `Maze`: tilt-driven maze runner with reset on shake or back tap and an optional trail.
- `Lux Meter`: light-sensor-driven ambient brightness readout with optional peak hold.
- `Weather`: manual weather icon plus temperature display for sunny, cloudy, rain, storm, snow, and night modes.
- `School Class Timer`: class/break schedule timer with configurable block lengths and cycle count.
- `Tuner`: microphone tuner visualization with guitar, ukulele, and chromatic targets.

## Current Notes

- `Lux Meter` depends on `Sensor.TYPE_LIGHT`. If the device does not expose a light sensor, it falls back to a `NO` readout instead of inventing a value.
- `Weather` is intentionally manual in this pass. It does not fetch network weather data.
- `Timer`, `Level`, `Maze`, `RPS`, and `Lux Meter` are the main sensor-driven additions in this pass.
- `Tuner` uses the app microphone path and in-house YIN pitch detection. It remains `NEEDS_SETUP` until microphone permission is granted on the phone.

## Inventory Status

- Previous registry size: 8 modules
- Current registry size: 17 modules
- Requested core Toy status: 9 of 9 implemented in code, with hardware visual validation still outstanding
