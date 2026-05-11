# Sankofa Android example

A runnable reference Android app that exercises the full Sankofa Android SDK surface — Analytics, Catch (Crashlytics + Sentry merged), Switch, Config, Pulse, Replay — against a local or remote Sankofa engine.

## Run

```bash
# Open in Android Studio (Hedgehog+)
# Run the `app` target on any emulator or device (API 24+).
```

Or from the CLI:

```bash
./gradlew :app:installDebug
adb shell am start -n dev.sankofa.example/.MainActivity
```

## Point at your engine

Edit `ExampleApplication.kt`:

```kotlin
Sankofa.init(
    context = this,
    apiKey = "sk_test_...",
    config = SankofaConfig(
        endpoint = "http://192.168.1.241:8080",  // or your local IP / staging URL
        // ...
    )
)
```

The default `http://192.168.1.241:8080` points at a developer-local Sankofa engine. Use `http://10.0.2.2:8080` if you're running on an emulator and the engine is on your host machine.

## What it demonstrates

### Crash Gallery (`CrashGalleryActivity.kt`)

15 scenarios across the full Catch API surface:

1. NullPointerException
2. ClassCastException
3. ArrayIndexOutOfBoundsException
4. NumberFormatException
5. ConcurrentModificationException
6. OutOfMemoryError
7. Thrown from a background thread
8. Caught Kotlin throw with `captureException(..., fingerprint = ...)`
9. `captureException` with custom fingerprint
10. Crashlytics-style breadcrumb log + manual capture
11. Payment-decline custom business error
12. `captureMessage` (warning, non-error signal)
13. ANR simulation (main thread sleep 6s — fires Android's ANR dialog AND a Sankofa `anr` event)
14. **Phase B — `withScope`** — tags + level + extras on ONE capture only
15. **Phase B — `beforeSend`** — fires events the hook drops or scrubs

### Phase B `beforeSend` (`ExampleApplication.kt`)

`SankofaConfig(beforeSend = { event -> ... })` is wired at init:

- Drops messages containing `"[noise]"` (framework warnings you can't fix).
- Scrubs `user_email` from `extra` so PII doesn't leak.

### Compose scroll-offset tagging (`ComposeStressActivity.kt`)

`LazyColumn` is wrapped with `Sankofa.tagScrollContainer { ... }` inside a `DisposableEffect` so heatmap touch attribution and replay frames carry the correct Y offset for below-the-fold taps.

### Flags + Config Lab (`FlagsLabActivity.kt`)

Subscribes to every demo flag + config key and renders a live decision table. Demonstrates `SankofaSwitch.onChange` / `SankofaRemoteConfig.onChange` listeners and bundled defaults that fall back when the handshake hasn't landed.

### Pulse Lab (`PulseLabActivity.kt`)

Triggers + previews the in-app survey runtime.

## Sticky context

`CrashGalleryActivity.onCreate` calls `Sankofa.setUser` / `Sankofa.setTags` / `Sankofa.setExtra` once so every captured event inherits the same user + tag set. Compare what the dashboard shows against this baseline to see how `withScope` overlays vs. how the global scope stays sticky.

## Documentation

Full Android SDK reference: [docs.sankofa.dev/sdks/android](https://docs.sankofa.dev/sdks/android/overview).
