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
- Sleep 1 s after each `KEYCODE_TAB` before `input text`. Without it the username lost its
  first letter, and a later retype got Gboard's autocorrect ("tester" became "yesterday", M2.3).
  The fix was `pm clear` and a clean fill with the pauses.
- The test instance has one collection (`Books`), and it has no image. To check an image or a
  child collection, add a temporary one through the API on the local instance and delete it
  after (`DELETE /api/collections/<id>` also deletes its children and the uploaded file, M2.3).
- The app logs no HTTP requests to logcat, and the Koillection container logs no requests
  either. To time a screen load, poll `uiautomator dump` for a text the loaded screen shows
  (about 1 s per poll, M2.4). For a host-side baseline, page through the same endpoint with
  `curl` (`Books`' 15 item pages took 0.3 s from the host).
- `Books` has 445 items. To check that every page arrived, fling to the bottom (40 fast
  `input swipe 540 2000 540 300 60`) and compare the last rows with the last `member`s of
  `GET /api/collections/<id>/items?page=15` (M2.4).
- Item check (M2.5): `Books` → "Ut mine Stromtid" has a main image, 10 `text` fields, two `image`
  fields (Title page, Spine) and 3 tags. To compare with the web UI from the host, log in with
  `curl` (a cookie jar, `GET /` for `_csrf_token`, then `POST /` with `_login`, `_password` and the
  token) and read `/items/<id>`.
