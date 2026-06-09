The official Nothing Glyph Matrix SDK AAR is installed here:

app/libs/glyph-matrix-sdk-2.0.aar

It was copied from the official Nothing Glyph Matrix Developer Kit. The AAR declares `minSdkVersion 33`, so the app module uses `minSdk = 33`.

The project also compiles without this AAR. `RealGlyphMatrixController` uses reflection and `GlyphMatrixController.create()` falls back to `FakeGlyphMatrixController` when SDK classes are absent.
