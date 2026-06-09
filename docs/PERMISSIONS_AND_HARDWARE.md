# Permissions And Hardware

Date: 2026-06-07

GlyphHub requests privacy-sensitive permissions only for features that need them.

| Permission / hardware | Used by | Status |
| --- | --- | --- |
| `RECORD_AUDIO` | Tuner, Sound Level Meter | Contextual request in Toy settings; microphone validation still required |
| `ACTIVITY_RECOGNITION` | Step / Motion Toy | Contextual request in Toy settings; step sensor validation still required |
| `INTERNET` | Open-Meteo Weather, web portal access from browser | Compiled; fetch/route tests still required |
| `ACCESS_NETWORK_STATE` | Network Status, portal diagnostics | Compiled; live connectivity validation required |
| Notification listener service | Notification Pulse | Service declared; user opt-in/settings flow and app filters incomplete |
| `RECEIVE_BOOT_COMPLETED` | Schedule/status restoration | Receiver declared; boot behavior not validated |
| Light sensor | Lux Meter | Real sensor path exists; live reaction validation required |
| Rotation vector / accelerometer / magnetometer | Compass, Level, Orientation, Maze, RPS | Real sensor path exists; calibration/manual validation required |

Future NFC and UsageStats wallet-active experiments are documented, but the app does not request those permissions until an actual trigger path is implemented.

No feature should be described as fully working until the matching permission, sensor, or hardware path is tested and recorded in `PHONE_TEST_RESULTS.md`.
