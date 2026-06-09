# Animation Tooling Research

Date: 2026-06-07

Scope: Nothing Phone (4a) Pro 13x13 Glyph Matrix output through the Nothing Glyph Matrix SDK / Glyph Toy service. External animation systems are evaluated only as editors, previewers, import sources, or app-logic helpers. The final runtime output must be validated 13x13 frame data.

## Key SDK Constraints

- Phone (4a) Pro uses `Glyph.DEVICE_25111p`, matrix length 13, no Glyph Touch, and AOD-only toys according to the official Glyph Matrix Developer Kit.
- The official SDK says `Common.getDeviceMatrixLength()` should be used at runtime.
- Toy lifecycle is system-owned: the system binds/unbinds the service; `onBind()` starts work and `onUnbind()` must stop it cleanly.
- Phone (4a) Pro toys need `com.nothing.glyph.toy.aod_support=1`.
- `GlyphMatrixManager.setAppMatrixFrame(...)` is the official app-display path. The docs warn that `setMatrixFrame(...)` can conflict with active Glyph Toys when used for app-based control.
- Glyph Toy output has higher display priority than third-party app matrix usage.
- SDK object brightness is 0..255 per `GlyphMatrixObject`, but this is object/frame brightness, not a global brightness authority. GlyphHub should normalize relative intensity internally and avoid fighting system Glyph brightness.
- Raw `setMatrixFrame(int[])` is documented as expecting a 25x25 array. For Phone (4a) Pro, prefer structured frames or the existing project's proven 13x13 controller path unless the AAR behavior is explicitly verified on device.

## Official Nothing Resources

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| Glyph Matrix Developer Kit | https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit and local `artifacts/research/GlyphMatrix-Developer-Kit` | Nothing Glyph SDK EULA; closed-source SDK; commercial use requires permission | Official SDK, device IDs, matrix length, service metadata, lifecycle, frame/object APIs, priority notes | Yes, via AAR already in `app/libs` | Required for final output | No asset format beyond Android resources/bitmaps/SDK frames | Already integrated | Closed source; app-control path can conflict with active toys if wrong method is used; brightness semantics are easy to misuse | Use as required runtime output layer |
| Glyph Matrix Example Project | https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project | MIT according to repository page | Kotlin toy demos, SDK wrapper pattern, lifecycle examples, animation demo | Yes | Useful for service/lifecycle comparison | Android resources/code | Low | Example is not an asset pipeline; demos are simple | Use as reference only |
| Nothing Playground | https://playground.nothing.tech/toys | Official marketplace, asset licenses vary by toy | Shows current toy ecosystem and expected quality categories | No, website/marketplace | Useful for visual benchmarking and categories | Upload pipeline appears asset-oriented; exact format not fully documented | Low for research | Marketplace assets may not be open source; do not copy visuals | Use for benchmark/reference only |
| Nothing Phone (4a) Pro product docs | https://intl.nothing.tech/products/phone-4a-pro | Proprietary marketing/docs | Confirms user-facing Glyph Matrix uses: clock, battery, solar path, timer, camera countdown, selfie mirror, progress trackers | N/A | Useful for expected built-in visual quality | No | Low | Marketing details are not API contracts | Use for target UX expectations |

## Android Animation And Rendering Tools

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| Canvas / `android.graphics` | https://developer.android.com/reference/android/graphics/Canvas | Android SDK, Apache 2.0 platform components | Draws bitmaps, paths, circles, text, lines into `Bitmap`; ideal for preview and raster import/downsample | Yes | Strong fit for preview, importer rasterization, bitmap thresholding | Can load/export bitmaps; JSON handled separately | Low | Need deterministic scaling/threshold rules to avoid noisy pixels | Use |
| Custom View drawing | https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing | Android SDK | View-based editor/preview drawing through `onDraw()` | Yes | Useful if the app moves away from Compose for the editor | No native JSON | Medium | Current app is Compose; mixed UI can add complexity | Maybe |
| Jetpack Compose UI + animation | https://developer.android.com/jetpack/androidx/releases/compose-animation | AndroidX, Apache 2.0 | Smooth app-side preview, playback controls, debug UI, editor interactions | Yes, already used | Good for the Animation Lab UI, not direct Matrix output | No direct asset format | Low | Compose animation timing must not be confused with hardware frame timing | Use for preview/UI only |
| VectorDrawable | https://developer.android.com/reference/android/graphics/drawable/VectorDrawable | Android SDK | Static vector assets drawable by Android | Yes | Can be rasterized through Canvas to 13x13 | XML vector assets | Medium | Thin strokes/details often collapse badly at 13x13 | Maybe importer/source only |
| AnimatedVectorDrawable | https://developer.android.com/reference/android/graphics/drawable/AnimatedVectorDrawable | Android SDK | Native Android vector property animation | Yes | Can be rendered offscreen and sampled into frames, but tool support is weaker than Lottie/pixel editors | XML vector + animator resources | Medium/high | Limited format; authoring is cumbersome; frame extraction needs careful scheduling | Maybe, not primary |

## Lottie / Bodymovin

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| Lottie Android | https://github.com/airbnb/lottie-android | Apache 2.0 | Parses After Effects / Bodymovin JSON and renders vector animations natively | Yes | Good intermediate importer: render to bitmap frames, downsample to 13x13, threshold, validate | Lottie JSON, image assets | Medium | Adds dependency; many Lottie features collapse poorly at 13x13; must not drive Glyph directly | Maybe as secondary importer |
| Lottie JSON / Bodymovin workflow | https://airbnb.io/lottie/ and https://lottiefiles.github.io/lottie-docs/ | Format/docs open; asset licenses vary | Designer-friendly vector animation exchange | Not a runtime library by itself | Good source format for simple high-contrast icons/motion | JSON | Medium | Rich animations produce unreadable 13x13 results; needs strict import preview | Maybe |
| LottieFiles / dotLottie Android | https://github.com/LottieFiles/dotlottie-android | Check repo before adoption; dotLottie ecosystem open-source | `.lottie` package format and renderer | Yes | Useful only if `.lottie` import becomes needed | `.lottie`, Lottie JSON/assets | Medium | Extra format and dependency without immediate value | Do not use now |
| Glaxnimate | https://glaxnimate.mattbas.org/ and project listings | GPLv3 | Open-source vector animation editor; supports Lottie/animated SVG | Desktop/editor, not Android runtime | Good external editor if assets are exported to Lottie | Lottie, SVG, internal JSON | Low for external workflow | GPL editor is fine externally, but do not copy GPL code into app | Maybe external-only |

## Pixel / Matrix Animation Tools

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| Glyph Matrix Editor by pauwma | https://pauwma.com/projects/glyph-matrix-editor and https://github.com/pauwma/GlyphMatrixEditor | GPL-3.0 | Purpose-built Nothing Phone (3)/(4a) Pro editor, frame timeline, onion skin, image/GIF/video import, text-to-pixel, exports JSON/pixel data/JS arrays/Lottie | Web app, not Android runtime | Excellent external editor and import target for 13x13 | JSON, pixel data, JS array, image/video exports | Medium for importer | GPL code cannot be copied into this app without license consequences; export schema must be inspected and supported explicitly | Use as external editor; implement clean-room JSON importer if schema is stable |
| Toyph | https://nothing.community/en/d/56750-toyph-an-open-source-glyph-toy-for-nothing-phone-4a-pro-just-in-time-for-may-the-4th and https://github.com/antonvidishchev/toyph | MIT | JSON-driven 13x13 Glyph Toy, custom glyphs, scroll text, effects, filters, font conversion | Android project | Strong architectural reference for JSON-driven assets/effects | `toy-config.json`, editor JSON | Medium | Community project format may drift; avoid bundling IP-adjacent demo glyphs | Maybe reference/import |
| Toyph notification editor | https://antonvidishchev.github.io/nothing-toyph-notifications/ | Need repo/license verification | Browser editor for 13x13 glyph notification images | Web app | Useful external drawing source | JSON/export strings | Medium | Format may not match final animation assets | Maybe importer |
| Piskel | https://www.piskelapp.com/ | MIT according to project docs/search result | Browser pixel-art and sprite animation editor with GIF/PNG/spritesheet export | Web app, not Android runtime | Good external sprite source | GIF, PNG, spritesheets, `.piskel` | Low for bitmap/GIF importer | Not circular-mask-aware by default | Use as external source through bitmap/sprite importer |
| Pix2D | https://pix2d.com/ | Open-source; verify repo license before code reuse | Pixel art and animation editor across desktop/web/mobile | Has Android app, but as external tool | Good external source for sprites | Multiple image/sprite formats | Low for image import | Not Nothing-specific | Maybe external source |
| Aseprite | https://www.aseprite.org/docs/save and https://aseprite.org/api/command/ExportSpriteSheet | Source-available/proprietary distribution; asset output usable | Mature pixel art editor; exports GIF, PNG sprite sheets, JSON metadata | Desktop, not Android runtime | Good professional external sprite source | GIF, PNG, sprite sheet JSON | Low for importer | App cannot bundle Aseprite; not free/open in the simple MIT sense | Maybe external source |
| LibreSprite | https://github.com/LibreSprite/LibreSprite | GPLv2 | Open-source Aseprite fork for pixel animations | Desktop | Good external source through spritesheet/GIF | Sprite sheets/GIF | Low | GPL code reuse restrictions; not circular-aware | Maybe external source |
| LED Matrix Studio | Various community sources; verify current repo before use | Varies | LED matrix animation editing/export | Desktop | Conceptually useful for LED frames | Varies | Medium | Not Nothing-specific; format/license uncertainty | Do not use now |
| 13x13/16x16 icon sets | NounProject, open icon repos, pixel-font repos, etc. | Varies | Static icons/fonts | Asset-only | Useful only if license is permissive and icons survive 13x13 | PNG/SVG/fonts | Medium | Most icons are unreadable at 13x13 without hand tuning | Maybe, asset-by-asset |

## Existing Nothing Community Projects

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| Glyph Matrix Editor | https://github.com/pauwma/GlyphMatrixEditor | GPL-3.0 | Purpose-built editor for Phone (3)/(4a) Pro; exports JSON/pixel data/Lottie | External web app | Best community editor for current problem | Yes | Medium | GPL; import only clean exported data, no copied code | Use external + importer |
| Toyph | https://github.com/antonvidishchev/toyph | MIT | JSON-driven 13x13 toy, effects, filters, scrolling text | Android | Strong evidence that JSON-driven 13x13 pipeline is viable | Yes | Medium | Demo assets may be IP-adjacent; schema differs from GlyphHub canonical format | Maybe |
| Nothing Playground community toys | https://playground.nothing.tech/toys | Varies | Examples: Pomodoro, Wattage Indicator, Decibel, Metronome, Dice, Leveller, Pendulum, Star Map | Runtime toys, not libraries | Useful quality/feature benchmark | Marketplace upload, not necessarily source | Low | Assets/code mostly unavailable | Reference only |
| Dot Hub | https://dothub.nostream.tech and Nothing Community posts | Proprietary/community app; license not verified | Multi-function AOD toy with animation styles and 4a Pro support | Android app | Useful feature benchmark | Not verified | Low | Not open/source; brightness concerns reported by user comments | Reference only |
| Glyph Matrix Lab | https://github.com/alex-1121/glyph-matrix-lab/releases/tag/1.0.0 | Verify repo; likely community app | Sandbox for custom 4a Pro Matrix images | Android app | Useful benchmark for custom static images | Not verified | Medium | License/format not verified | Maybe reference only |
| AOD GeekBox | https://github.com/danissomo/GlyphMatrix-AODGeekBox | Verify repo | AOD toy collection for 4a Pro | Android | Useful reference for apps/widgets | Not verified | Medium | License/quality unknown | Maybe reference only |
| Glyph Matrix Simulator | https://glyph.andreibanu.com/ | License not verified | Browser simulator matching official SDK concepts | Web | Useful preview reference | Not verified | Low | Simulator is not hardware; license unknown | Maybe reference only |
| GlyphMatrix Tasker Plugin | https://glyphmatrix.kunbot.org/ | License not verified | Tasker integration for Phone (3) matrix | Android | Mostly 25x25 Phone (3), less relevant to 13x13 4a Pro | Pattern variables | Low | Different device/matrix size | Do not use now |

## Tool-Specific Libraries For Toy Logic

| Name | URL / Source | License | What it can do | Runs in Android app | Helps 13x13 Matrix | JSON/assets | Difficulty | Risks | Recommendation |
|---|---|---|---|---|---|---|---|---|---|
| TarsosDSP | https://github.com/JorenSix/TarsosDSP | GPL-3.0 | Java DSP framework; pitch detection algorithms include YIN, McLeod, dynamic wavelet; filters/effects | JVM/Android possible | Helps Tuner input logic, not matrix drawing | No | Medium | GPL is incompatible with many app distribution goals unless whole app licensing accepts it; custom Maven repo | Do not integrate as dependency now; reference algorithms only |
| Android-TarsosDSP forks | https://github.com/H-Fontaine/Android-TarsosDSP | GPL-derived; verify | Android-oriented TarsosDSP usage | Android | Helps tuner | No | Medium | Same GPL concern | Do not use now |
| YIN algorithm | Original paper/known algorithm; implementations vary | Algorithm not tied to one license; code license depends on implementation | Robust monophonic pitch detection | Yes if implemented locally | Helps Tuner | No | Medium | Need careful confidence/RMS gating to avoid release-note pitch drops | Use, implement locally or find permissive implementation |
| Autocorrelation pitch detection | Standard DSP technique | Algorithmic technique | Simpler pitch detection baseline | Yes | Helps Tuner | No | Medium | Less robust than YIN for noisy microphone input | Maybe as fallback |
| aubio | https://aubio.org/ and https://github.com/aubio/aubio | GPL | C audio analysis library with pitch methods including YIN/YINFFT | Native Android possible with NDK | Helps Tuner | No | High | GPL and native build overhead | Do not use |
| Android `AudioRecord` + RMS | Android SDK | Android SDK | Microphone capture and amplitude/RMS for tuner/sound meter/metronome input | Yes | Drives sound meter/tuner visuals | No | Medium | Permission, noise floor, approximate calibration | Use |
| Android rotation vector sensor | https://source.android.com/docs/core/interaction/sensors/sensor-types and https://developer.android.com/develop/sensors-and-location/sensors/sensors_position | Android SDK/AOSP docs | Fused orientation from accelerometer/gyro/magnetometer; preferred over deprecated orientation sensor | Yes | Drives Compass/Level/Maze smoother inputs | No | Low/medium | Heading still needs calibration/accuracy handling | Use |
| Low-pass/complementary filters | Android docs + local `accuracy/SmoothingFilters.kt` | Local/app code | Smooth noisy sensor values and reduce jitter | Yes | Helps Compass/Level/Maze/Tuner gauges | No | Low | Too much smoothing adds lag | Use |
| Open-Meteo | https://open-meteo.com/ | API data CC BY 4.0 with attribution; server AGPLv3 if self-hosted | Free/no-key weather forecast/current JSON API for non-commercial use | Yes via HTTP client | Helps Weather toy content | JSON API | Low | Rate limits; attribution; commercial use constraints | Use as default weather provider abstraction |
| MET Norway Locationforecast | https://api.met.no/weatherapi/locationforecast/2.0/documentation | Free data terms; requires identifying User-Agent | Weather forecasts for coordinates | Yes | Good fallback/provider option | JSON API | Low | Strict User-Agent; data semantics differ | Maybe |
| WeatherAPI.com | https://www.weatherapi.com/docs | Commercial API terms; free key tier | Current/forecast/weather search | Yes | Optional user-key provider | JSON/XML API | Low | Requires API key, paid tiers | Maybe user-key provider |
| Retrofit | https://github.com/square/retrofit | Apache 2.0 | Type-safe HTTP client for Android/JVM | Yes | Useful for weather/web APIs | JSON via converters | Low | Adds dependency; current project has minimal deps | Maybe |
| OkHttp | https://github.com/square/okhttp | Apache 2.0 | HTTP client/cache | Yes | Useful for weather caching and web portal client calls | No | Low | Adds dependency | Maybe |
| NanoHTTPD | https://github.com/NanoHttpd/nanohttpd | BSD-3-Clause / Modified BSD | Tiny embeddable Java HTTP server, commonly used in Android | Yes | Useful for optional Wi-Fi web portal editor/import/export | HTTP assets/JSON | Medium | Security, LAN exposure, auth, lifecycle/battery | Maybe for web portal |
| Ktor server | https://ktor.io/ | Apache 2.0 | Kotlin async server/client framework | Android possible with caveats | Web portal option | JSON/HTTP | High | Heavy for current app; embedded Android server complexity/API floor issues | Do not use now for portal |

## Findings

- The strongest immediate path is not a heavy external runtime; it is a small internal canonical 13x13 JSON/frame model plus validators, normalizers, circular layout helpers, shared primitives, and importers.
- Lottie is valuable as an optional importer only. It should not be the primary runtime animation model because many high-detail vector animations will not survive 13x13 downsampling.
- The Glyph Matrix Editor is the best discovered purpose-built external editor for 13x13 assets. Because its code is GPL-3.0, GlyphHub should not copy implementation code unless the app license strategy explicitly changes. A clean importer for exported JSON/pixel data is acceptable if the exported data schema is handled independently.
- Pixel editors such as Piskel, Pix2D, Aseprite, and LibreSprite are useful sources through GIF/PNG/spritesheet import. They are not circular-mask-aware, so validation and preview remain mandatory.
- TarsosDSP and aubio are technically useful for pitch detection but GPL-licensed. For this app, a local YIN/autocorrelation implementation or a permissively licensed pitch library is safer.
- Open-Meteo is the best weather default because it is JSON, no-key for non-commercial use, and easy to cache. A provider abstraction should allow user-key alternatives.
- NanoHTTPD is the best lightweight web-portal candidate if a Wi-Fi editor returns; Ktor server is too heavy for this pass.

## Research Decision Inputs

- Primary runtime format should be app-owned, not Lottie-owned or editor-owned.
- Every Toy should produce or request `GlyphFrame` / `GlyphFrameSequence` through shared libraries.
- External tools are source/import workflows and preview aids.
- Only validated, normalized 13x13 frames should reach `GlyphMatrixController`.
- Brightness must be relative and normalized in one place; the app should not expose or repeatedly set global brightness.
