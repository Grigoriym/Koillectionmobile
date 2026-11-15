# Koillectionmobile — Emulator testing

Project-specific facts for the `emulator-testing` skill. Generic adb/uiautomator
technique lives in the skill itself, not here. This file holds only what is true about
this app.

## Device facts

- AVD: any 1080x2400 API 36 phone. A `screencap` has the same size, so the scale factor is 1.
  The AVD name on a given machine, and whether Gradle needs `ANDROID_HOME`, are in
  `docs/machine.local.md` (gitignored).
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
- Log out (M2.6) is the "Log out" text button in the collection list's top bar. After it the
  login form shows only the server URL. To prove that a re-login loads fresh data and does not
  show the old screen state, change the server while logged out: create a temporary collection
  through the API, log in, log out, delete it, log in again. The list must not show it.
- Two different items in a row (`Books` → "Jörn Uhl", back, "Die Pappenheimer") must show
  different authors (Gustav Frenssen, Julius Wolff). The same author twice means the entries
  share one ViewModel (the M2.6 bug).
- Full browse pass (M2.7): taps by text with the skill's `ui.py tap "<text>"`
  (`~/.claude/skills/emulator-testing/ui.py`), not by hand-parsed
  dumps. In landscape the item's main image fills the width, so the fields are below the fold and
  not in the dump: check the title, or scroll first. Process death: open `Books` → an item, then
  HOME, `am kill`, `am start -n`; the item comes back, and back goes to `Books`, then Collections.
- Back arrow (M2.8): the collection and item screens' arrow is a node with `content-desc` "Back"
  (`ui.py tap "Back"`). The collection list has none.
- Add-item form (M3.4): the collection screen's FAB has `content-desc` "Add item". A duplicate
  label adds an error line under both rows, which moves every node below them: tap the next
  target from a fresh dump. To compare the tag picker with the server, count `GET /api/tags`
  (`totalItems`) and `/api/tag_categories` with a bearer token from
  `POST /api/authentication_token` (30 tags in 4 categories on the test instance, 2026-11-15).
- Photos (M3.5): "Take photo" and "Pick photos" sit under the photo rows (camera and picker steps
  are in the skill). A filled "Image label" field shows its value as `text`, so `ui.py`'s index for
  "Image label" shifts after each fill: tap the next one from a fresh `list`. A picked photo still
  previews after `am kill` (a copy in `cache/photos/`, readable with `run-as`); a white top band on a test JPEG shows the rotation.
