# Koillectionmobile — Emulator testing

Project-specific facts for the `emulator-testing` skill. Generic adb/uiautomator
technique lives in the skill itself, not here. This file holds only what is true about
this app.

## Device facts

- AVD: `Medium_Phone_API_36.1` (1080x2400; a `screencap` has the same size, so the scale
  factor is 1). Created 2026-11-15 in M2.1.
- Package ids: `com.grappim.koillectionmobile.debug` (`gplayDebug`, the day-to-day
  variant), `com.grappim.koillectionmobile.fdroid.debug` (`fdroidDebug`).
- Activity: `com.grappim.koillectionmobile.MainActivity` (fully qualified: the package id
  has a suffix, the class does not).
- Build and install: `./gradlew :androidApp:assembleGplayDebug`, then
  `adb -s emulator-5554 install -r androidApp/build/outputs/apk/gplay/debug/androidApp-gplay-debug.apk`.
- Backend: the local test instance (`docs/local-info.md`), `http://10.0.2.2:8290` from the
  emulator. Start it before any check that logs in.

## App-specific gotchas

- A fresh install has no session, so the app opens on the login side. The startup branch
  reads `SessionStorage.isLoggedIn` (a token and credentials that decrypt). `pm clear`
  returns the app to login.
- The login form is centered vertically, so the keyboard moves every field when it opens.
  Tap the server URL field once, then `KEYCODE_TAB` to the next field. A second tap by an
  old coordinate put all three values into the URL field (M2.2). `KEYCODE_ENTER` in the
  password field submits the form.
