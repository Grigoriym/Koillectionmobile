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
- The drawer (M11.11) holds Collections, Tags, Templates, Wishlists, Albums, Settings and Log out.
  Open it with "Open the menu" (`content-desc`) on the collection list or Settings. Each label is
  two nodes (the icon's `content-desc` and the `text`), so tap the text: `ui.py tap Tags 1`. Tags and
  Templates, Wishlists and Albums have it too (M11.12, M11.13).
  An edge swipe is the system back on this AVD (gesture navigation), not a drawer swipe. Back at
  the root of any other section goes to Collections; back at the Collections root leaves the app.
- Log out (M2.6) is "Log out" in the drawer. After it the
  login form shows only the server URL. A collection made through the API shows in the list after a
  pull to refresh: `input swipe 540 400 540 1800 800`. A 300 ms swipe from y 600 did not refresh
  (M7.7). To prove that a re-login loads fresh data and does not
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
- Save (M3.6): "Save" is a text button in the top bar (`ui.py tap Save`), off while a label is
  blank ("A label is required." under it). A filled text field's "Label" node stays in the dump, as
  "Image label" does, so `ui.py tap Label 0` hits the filled row: tap the empty one by its fresh
  bounds. To fail a save halfway on purpose, set the test user's quota to 1 byte
  (`update koi_user set disk_space_allowed=1 where username='tester'` in `koillection-test-db`):
  the item and text fields go through, the first image upload gets HTTP 500. Put it back to
  `1000000000000` after. The collection list is in server (creation) order, so a new item is
  last; a refresh check reads the last rows after a fling to the bottom. Run it with no process
  death since the collection screen opened: a restore creates its ViewModel after the save, so
  the item shows even without the refresh signal. The photo cache is
  `run-as com.grappim.koillectionmobile.debug ls /data/data/com.grappim.koillectionmobile.debug/cache/photos`
  (a relative `cache/photos` fails under `run-as sh -c`).
- Field rows (M3.7): the "Value" field is multi-line, so `KEYCODE_TAB` types a tab into it instead
  of moving on. Tap each Label and Value field by its fresh bounds. Back (arrow or system) on a
  form with any input opens "Discard this item?": tap "Discard" to leave, "Keep editing" to stay.
  An empty form leaves at once.
- Delete item (M5.3): a trash icon in the item screen's top bar (`content-desc` "Delete item")
  opens "Delete this item?" with "Cancel" and "Delete". To check it without a fling through
  `Books`, create a temporary collection with two items through the API, delete one in the app,
  and delete the collection after. Airplane mode before "Delete" gives the failure dialog
  ("The item was not deleted", "OK"). After a delete, back from the collection goes to
  Collections: the item screen is off the back stack.
- Edit item (M5.5): a pencil in the item screen's top bar (`content-desc` "Edit item") opens "Edit
  item". Each image row has a "Replace" text button with a "Take photo" / "Pick photos" menu; a
  replaced photo gets a "Rotate" button. A tag chip's `checked` is on the chip's parent node, not
  its text node. Back with edits opens "Discard your changes?". To build a test item through the
  API, a field image goes up as the multipart part `fileImage` (the main image is `file`): with
  `file` the server answers 200 and stores nothing. A `GET` with `Accept: application/json` gives a
  plain array, not a Hydra `member` list. The Name field is at the top, so a tap on a lower field
  while the keyboard is open can miss: close the keyboard (`KEYCODE_BACK`) and dump first.
- Save the edit (M5.6): "Save" in the edit form's top bar. On success the form closes and the item
  screen shows the new values at once; back shows the new name in the collection. A tap on the
  Author value right after typing in Name (keyboard still open) landed in Name again: close the
  keyboard after every field. To build the test item, `/api/collections` takes `{"title"}`, and the
  tags go in a merge-patch `{"tags": [IRIs]}` on the item (`TagsRepositoryImpl.setItemTags`).
- Edit and delete pass (M5.7): a `number` field added through the API (`"type":"number"`) showed a
  read-only row in the edit form, and a new text row went below it. Since M8.6 every text type
  (`number` too) is an editable row; a `file`, `video` or `sign` field is still read-only. The tag chips sit below "Add
  field": the first dump of the form has none of them, so scroll before you read `checked`. The
  main image's Replace → "Pick photos" needs the photo media-scanned first (skill, Step 5).
- Delete collection (M6.3): a trash icon in the collection screen's top bar (`content-desc` "Delete
  collection") counts first, then opens "Delete this collection?" with "Cancel" and "Delete". The
  dialog's body (the counts) was not in `ui.py list` or a dump grep, though the screenshot showed it:
  read the counts from a screenshot. Build the test tree through the API (`POST /api/collections`
  with a `parent` IRI, `POST /api/items` with a `collection` IRI); the delete cleans it up.
- New collection (M6.4): the list's FAB (`content-desc` "New collection"), or on a collection
  screen "More actions" (`content-desc`) → "New collection". The form has "Title", "Take photo",
  "Pick photo", then "Rotate" and "Remove image" once a photo is set; "Save" in the top bar. On
  success the new collection opens, and back goes to the list or the parent. The server stores
  the image at most 200x200, so a rotation is checked in the form's preview, not on the server.
- Edit collection (M6.5): "More actions" → "Edit collection". The form is the M6.4 form, filled;
  the server image shows with "Remove image", and Save is off until something changed. On success
  the form closes onto the collection screen. To check the image on the server, read `image` from
  `GET /api/collections/<id>` and a pixel of that file; the replaced or removed file gives 404.
- Collection search (M7.1): the field above the items has the placeholder "Search items by name"
  (`ui.py tap`), and "Clear search" (`content-desc`) once it holds text. It shows only when the
  collection has items. With the keyboard closed, Back leaves the screen. The "No item name
  contains ..." line was not in `ui.py list` though the screenshot showed it: read it from a
  screenshot.
- Item search (M7.2): the magnifier in the collection list's top bar (`content-desc` "Search
  items") opens "Search items". Each row is the item name with its collection title below it. The
  field is the M7.1 field. After "Clear search" the list keeps the last match at the top (the lazy
  list keeps its scroll anchor by key), so the rows above it are off screen, not missing.
- Tags (M7.5): "Tags" in the drawer opens "Tags": one header per
  category (a colour dot and the label), the tag rows below it. A tap opens the tag's items, titled
  with the tag label. Compare with `GET /api/tags` and `/api/tag_categories` (bearer token, as in
  M3.4); "Fraktur" has 121 items over 5 pages, the last one "Brettspiel des Lebens". In airplane mode
  the list shows the network error and "Try again". The category dots are pale because the server's colours are
  (`F3E5F5`, `E8EAF6`, `E3F2FD`; M7.7): compare a pixel with `GET /api/tag_categories`, not with
  a guess.
- New tag (M7.6): "New tag" is a text button under the last tag group (scroll to the bottom of
  the form). It opens a dialog with "Tag name", "Cancel" and "Add". With the keyboard open, "Add"
  moves up (y 975 instead of 1385): tap it from a fresh dump. "Add" closes the keyboard with the
  dialog, so a `KEYCODE_BACK` after it opens "Discard this item?". Pending tags show under "New
  tags, created on save". To see that no tag exists before Save, count `GET /api/tags`
  (`totalItems`). The test instance has exactly 30 tags, one full page of `/api/tags`, so a new tag
  lands on page 2: read every page before a cleanup by label (M7.7 missed one).
- Templates (M8.9): "Templates" in the drawer; "Choice lists" (`content-desc`)
  in its top bar. In the template form the field type is a chip with the type's name ("Text"):
  `ui.py tap Text <n>` opens the 17-type menu. A `choice-list` type adds "Pick a choice list". In
  the item forms the `checkbox` switch and the `choice-list` chips have no label in the dump: find
  them by `checkable="true"` in a fresh dump (the chip's `checked` is on that node). The item
  screen shows the raw stored values (`1`, `["Novel"]`). A choice list that an item still uses
  cannot be deleted (500, the app names the cause): delete the item or its collection first.
- Local network permission (M4.1): `10.0.2.2` is the emulator's own gateway, so a check of
  `ACCESS_LOCAL_NETWORK` needs another private host address. The test instance listens on
  `127.0.0.1` only: forward a host-only private address to it for the check, not the Wi-Fi LAN
  address (the address and the forwarder on a given machine are in `docs/machine.local.md`). The
  permission matters only on API 37+, and no API 37 AVD has worked here yet (`docs/revisit.md`).
- Settings (M4.2): "Settings" in the drawer and a text button under "Log in" on
  the login form. The screen holds the custom headers ("Add header", "Header name", "Value",
  "Save"; Save is off until the rows differ from storage). The app writes no Ktor log on the
  device: to see the headers a request carries, point the server URL at a forwarder on the host
  that prints them (M4.2 used a small Python asyncio forwarder, `127.0.0.1:8291` → `8290`, which
  the emulator reaches as `http://10.0.2.2:8291`).
- Proxy rejection (M4.3): run the proxy front (`docs/local-info.md`, "Proxy front") and log in to
  `http://10.0.2.2:8295`. Its access log is the request proof, as the app logs none. Back on the
  login form with the keyboard already closed leaves the app; the relaunch shows an empty form.
  To make the header wrong while logged in, edit its value in Settings (tap the value field,
  `KEYCODE_MOVE_END`, a run of `KEYCODE_DEL`), save, and pull the list down to refresh.
- Cleartext warning (M4.4): an `http://` server URL adds a red line under the URL field, which
  moves Username, Password and Log in down. Tap fields from a dump taken after the URL is typed.
- Wishlists (M9.8): "Wishlists" in the drawer; the list's FAB is "New wishlist"
  (`content-desc`). On a wishlist screen, "More actions" holds "New wishlist", "New wish" and
  "Edit"; "Delete wishlist" is a top-bar icon. The wish screen has "Edit wish" and "Delete wish"
  icons. The wishlist row's tap target is its name text. Solid-colour test JPEGs (skill, Step 5)
  tell the picker thumbnails and the stored images apart. To compare with the web UI, log in with
  `curl` as in M2.5 and read `/wishlists/<id>`.
- Loans (M10.6): a "Loans" section at the bottom of the item screen, with "New loan" under it. A
  tap on the lender's name opens "Edit loan"; "Clear" sits next to the returned date. Each loan
  has "Mark returned" and "Delete" text buttons, so tap them by index from a fresh `list`. After
  each action the screen jumps up (the section reloads): scroll down and dump again. The delete
  dialog's body names the loan, but only a screenshot shows it, as in M6.3. The date picker's days
  are `text` nodes in `ui.py list` ("Tuesday, November 10, 2026"). Compare with the web UI's
  `/loans` page (curl login as in M2.5): returned loans are in a second table.
- Albums (M11.8): "Albums" in the drawer; the list's FAB is "New album" (`content-desc`). On
  an album screen, "More actions" holds "New album", "New photo" and "Edit"; "Delete album" is a
  top-bar icon. The photo screen has "Edit photo" and "Delete photo" icons. A photo cell's tap
  target is its title text. A new photo cannot be saved without an image ("A new photo needs an
  image."): pick a solid-colour test JPEG (skill, Step 5). The album form's save returns to the screen it came from, not to the
  new album. The web UI has no photo page (`/photos/<id>` gives 404): read the photo from its album
  page, `/albums/<id>`.
