# Sankofa Catch — Android Crash Gallery

Launcher: **Main screen → "Open Catch Crash Gallery"**
Activity: `dev.sankofa.example.CrashGalleryActivity`
SDK init: `ExampleApplication.onCreate()` (calls `SankofaCatch.init(...)`).

Every card on the screen triggers one specific failure mode so the
dashboard, symbolicator, and breadcrumb ring buffer can all be
exercised from a single screen.

## How to run

```bash
cd example/sankofa_example_android
./gradlew installDebug
# open the "Sankofa Example" app, tap "Open Catch Crash Gallery"
```

After each unhandled scenario the OS will kill the process — relaunch
the app to continue.

## Scenarios

| # | Button                       | Outcome                          | Captured via                                    |
|---|------------------------------|----------------------------------|-------------------------------------------------|
| 1 | Throw NPE                    | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 2 | Throw CCE                    | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 3 | Throw IOOBE                  | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 4 | Divide by zero               | Process crash (ArithmeticException) | Chained `UncaughtExceptionHandler`           |
| 5 | Throw ISE                    | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 6 | Throw IAE                    | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 7 | Throw NFE                    | Process crash                    | Chained `UncaughtExceptionHandler`              |
| 8 | Infinite recursion           | Process crash (StackOverflowError) | Chained `UncaughtExceptionHandler`            |
| 9 | Trigger OOM                  | Process crash (OutOfMemoryError) | Chained `UncaughtExceptionHandler`              |
| 10 | Throw off main thread       | Process crash from worker        | Chained handler, non-main thread                |
| 11 | Capture payment decline     | No crash, handled event          | `SankofaCatch.captureException(e, options)`     |
| 12 | Capture warning message     | No crash, warning event          | `SankofaCatch.captureMessage(text, options)`    |
| 13 | Freeze main thread 6s       | System ANR dialog                | `CatchAnrWatcher` installed by `init`           |

## What to check in the dashboard

- Every event carries the ambient user (`user_demo_gallery_42`) and
  the tags `screen=crash_gallery`, `platform=android`, `demo=true`.
- Scenario 11 groups by `fingerprint = [payment_declined,
  insufficient_funds]`, not by stack trace.
- Every event includes a rich breadcrumb trail — at minimum a
  `navigation` crumb for entering the activity and a `user.click`
  crumb for the button press.
- Unhandled events carry `mechanism.handled = false`, manual ones
  carry `handled = true`.
- Scenario 13 surfaces as `type = "anr"` with `level = error`.
