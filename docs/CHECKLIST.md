# koillectionmobile checklist

**Current step:** M5.7. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7 and 8 are settled.

## How to use this

Each step is done in **one fresh session with zero memory of prior sessions**:

1. Start a fresh session and say "read `docs/CHECKLIST.md` and do step `<N>`."
2. Do exactly that step. Don't pull later steps forward.
3. Pass the step's **Verify** line before calling it done.
4. Tick the box, update the **Current step** banner, add a one-line `Note:` if anything
   deviated.
5. Commit, then run `/finalize`.
6. When a milestone is fully ticked, move it verbatim into `docs/CHECKLIST_ARCHIVE.md` in
   the same commit.

Ground rules:
- Root `CLAUDE.md` overrides this file if they conflict.
- The Koillection source (`../../koillection`) is the API contract. Re-read it at the start
  of any step that touches the API.
- A prior session's claim ("already checked X") gets re-verified, not trusted.
- Tests go in the same step as the logic. Hand-written fakes, no mocking library.
- Milestones after M0 get broken into steps at the start of that milestone. The rough scope
  below is not binding.
- Git workflow (decided 2026-11-14): commit and push straight to `master` until the user
  says the app is close to release; then switch to their usual `dev`/`main` branches + PRs.
  GitHub remote: `Grigoriym/Koillectionmobile`, public (user chose it in M0.1).

M0 (repo and module scaffolding) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M1 (API layer) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M2 (browse) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M3 (add item with photos) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M4 (network and security parity with wallosmobile) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## M5 — edit and delete items

Change an existing item's name, text fields, main image, image fields and tags; delete an item
(with a confirm). Uses `PATCH`/`DELETE` on `Item` and `Datum` (Koillection source). Form choices
are plan §5 decision 8 (user, 2026-11-16): the add form, prefilled; the main image is replaced,
never removed; no field reordering. Every write goes to the local test instance only
(`docs/local-info.md`).

- [x] **M5.1** — Repository writes (`feature:items`): rename an item (`PATCH /api/items/{id}`,
  `{"name"}`), change a field (`PATCH /api/data/{id}`, `label` and/or `value`), delete a field
  (`DELETE /api/data/{id}`), delete an item (`DELETE /api/items/{id}`). Re-read `Item.php` and
  `Datum.php` first. A `PATCH` sends `application/merge-patch+json` and only the changed keys.
  **Verify:** mock-engine tests for each call (method, path, content type, body keys), with a
  root-path base URL case (`CLAUDE.md`, Verification); mutation-check one body key and one method.
  Note: `updateDatum(id, label, value)` leaves a null argument out of the body. Mutations caught:
  a `value` key always sent (the label-only test), `deleteItem` sent as `POST` (the method test).
- [x] **M5.2** — Live edit smoke against the local test instance: extend `smokeWriteRun` (or add a
  task) to create an item with a text field, an image field and a main image, then rename it,
  change the field's label and value, replace the main image and the field image, delete the
  image field, and delete the item. After each replace, the old file gives 404; after the field
  delete, its file gives 404. Also try a `PATCH` that gives a field the label of another field
  of the item (expect 422). Record the answers in plan §2.
  **Verify:** the run prints each step and passes; plan §2 has the results.
  Note: the duplicate-label `PATCH` got an empty HTTP 500, not the expected 422 (server bug,
  plan §2); the run now expects the 500. A `POST` with a duplicate label still gets 422.
- [x] **M5.3** — Delete item: a delete action on the item screen with a confirm dialog. On
  success the item screen leaves the back stack and the collection reads its list again
  (`ItemListChanges`). A failure shows the error and keeps the item screen.
  **Verify:** ViewModel tests; on the emulator a test item is deleted, the collection list no
  longer shows it, and the web UI agrees.
  Note: the error shows in a dialog ("The item was not deleted"); the screen keeps the item.
  The device check used a temporary collection (deleted after) and airplane mode for the failure.
- [x] **M5.4** — Edit use case (`feature:items:domain`): from the item as loaded (name, fields,
  main image, tags) and the edited form, work out the calls and run them. Deletes first (a freed
  label can be reused), then changes, then new fields (positions after the current last), then a
  new main image, then the tags (only if the set changed). Fields the form cannot edit (types
  other than `text` and `image`) are never touched. A retry reloads the item and works out the
  calls again, so a call that already went through is not sent twice. A label swap between two
  fields needs a temporary label (server rule, plan §2): handle it, or reject it in the form
  with a message; decide in the step. A duplicate label in a `PATCH` gets an empty 500, not a
  422 (M5.2, plan §2), so a conflict must be caught before the calls are sent.
  **Verify:** use case tests with hand-written fakes: each kind of change alone, a mix, no change
  (no calls), a failure in the middle and the retry, a label swap, an untouched non-text field.
  Note: a label swap is handled: the use case moves one field to a temporary label
  (`<label> (moving 1)`), visible on the server only if the save stops there. A duplicate final
  label returns `EditItemResult.DuplicateLabel` before any call. The retry is `retry(original, edit)`;
  it reads the item again and matches a new field already created by its label. `EditItemUseCase`
  is bound in `ItemsDataModule`.
- [x] **M5.5** — Edit screen: an "edit" action on the item screen opens the M3 add form,
  prefilled from the server (name, text fields, the main image and image fields shown from their
  URLs, the item's tags). An existing photo can be replaced (camera or picker) or, for an image
  field, removed; its label can change. The main image has replace only. New text rows and new
  photos are added as in M3. Fields of other types show read only. The form survives rotation and
  process death (edits kept, not reloaded over).
  **Verify:** ViewModel tests; on the emulator the form opens with the item's values, and
  `am kill` keeps an edit.
  Note: a separate `EditItemViewModel`/`EditItemRoute` with the add form's parts (`ItemFormParts.kt`);
  the item screen's pencil opens it. The form has no Save yet (M5.6). "Replace" (camera or picker)
  is per image; the picker can return several photos, and only the first is used. The draft and
  the loaded values are in saved state as `EditItemDraft`; M5.6 can build the `ItemSnapshot`
  diff base from `original`, or read the item again.
- [x] **M5.6** — Save the edit: run M5.4 from the form with the M3 progress view and retry. On
  success return to the item screen, which reads the item again, and refresh the collection (a
  new name shows in the list). An upload 500 names the disk quota, as in M3.6. Delete the photo
  files in `cacheDir/photos/` as M3.6 does.
  **Verify:** ViewModel tests; on the emulator one edit renames the item, changes a text field,
  adds one, removes an image field, replaces the main image and changes the tags; the web UI on
  the local test instance shows the same.
  Note: the item screen reads the item again through a new `ItemChanges` signal (bound in
  `ItemsDataModule`). `EditItemResult.Failure` got `isUpload` for the quota message. The loaded
  `ItemSnapshot` is kept in saved state; the first save is `editItem`, every later one `retry`. A
  failure after a call went through offers only the retry; before that, "Back to the form" too.
  `SaveView` moved to `ItemFormParts.kt`. The device check used a temporary collection (deleted after).
- [ ] **M5.7** — Device pass of edit and delete, then the user confirms with screenshots. Fix what
  breaks, or put it in `docs/revisit.md` with evidence.
  **Verify:** the user confirms.

## M6 — collections: create, edit, delete (rough scope)

Create a collection (top level or child), rename it, set its image, delete it (with a confirm).

## M7 — search and tags (rough scope)

- Search items by name. Koillection 1.8.5 declares no `ApiFilter` on `Item` or `Collection`
  (checked 2026-11-16), so search runs in the app over loaded data. Settle the approach at the
  start of M7.
- A tag list screen (`feature:tags:ui` is a placeholder today) and the items of a tag
  (`/api/tags/{id}/items`).
- Create a tag from the add-item form. This reverses part of plan §5 decision 7 (user,
  2026-11-16).

## M8 — other Koillection areas (rough scope)

The user wants these before release too (2026-11-16): wishlists and wishes, albums and photos,
loans, templates (with fields and choice lists), inventories, logs. In 1.8.5, `Inventory` has
no `Post` and `Log` is read only. At the start of M8, settle the order and the scope of each
area with the user; split M8 into one milestone per area if it is too big for one.

## M9 — release

Runs after M4–M8 (user, 2026-11-16). Both store flavors ship, as in the user's other apps (user, 2026-11-16): `fdroid` to
IzzyOnDroid first, `gplay` to Google Play. Model: `../Wallosmobile` (`build-logic`
`configureAppSigningConfigs`, `androidApp/build.gradle.kts` dependency-info block,
`.github/workflows/release*.yml`, `fastlane/metadata/`). No Firebase: wallosmobile's
`google-services.json` steps are not ported. Two inputs come from the user later: the launcher
icon (M9.4) and the release keystores (M9.5). Those steps wait for them; the others do not.

- [ ] **M9.1** — Release blockers. Check the subpath question read only, from the user's server
  URL (`docs/revisit.md`, M1.9 entry). Go through every `docs/revisit.md` entry with the user;
  each one is "fix before release" (gets a step here) or "after release".
  **Verify:** the M1.9 entry is closed or has a recorded answer; each revisit entry has the
  user's verdict, written in the entry.
- [ ] **M9.2** — R8 release build. `assembleFdroidRelease` and `assembleGplayRelease` build with
  minify and resource shrinking on (never run so far; `androidApp/proguard-rules.pro` is empty).
  Sign the `fdroidRelease` APK by hand with the debug key (`apksigner`, no build change) and run
  login, browse and add item on the emulator against the local test instance. Add a keep rule
  only for what R8 breaks.
  **Verify:** both release tasks pass; the hand-signed APK passes the flow on the emulator.
- [ ] **M9.3** — Store texts. `README.md` (what the app does, unofficial and not affiliated with
  Koillection, the Koillection version it was tested against, where to install it), a privacy
  policy (Play needs one; the app talks only to the user's own server), and
  `fastlane/metadata/android/en-US/` (title, short and full description, `changelogs/1.txt`,
  phone screenshots). Screenshots come from the local test instance, which is a copy of the
  user's real catalogue: the user approves what they show before they are committed.
  **Verify:** the user approves the texts and the screenshots.
- [ ] **M9.4** — Launcher icon, from the image the user provides (waits for it). Adaptive icon
  with a monochrome layer, plus the 512px `fastlane/.../images/icon.png`.
  **Verify:** the icon shows on the emulator launcher, light and dark.
- [ ] **M9.5** — Release signing (waits for the user's keystores). Port wallosmobile's
  per-flavor signing configs: keystore files at the repo root (gitignored), passwords from env
  vars. Turn off the APK dependency-info block, which F-Droid's scanner rejects. Confirm with the
  user whether wallosmobile's signed `fdroidDebug` channel is wanted here too.
  **Verify:** `apksigner verify --print-certs` shows the right certificate for each flavor; the
  `fdroid` APK has no extra signing block.
- [ ] **M9.6** — Release workflow. Port wallosmobile's `release.yml`: a `v*` tag builds the signed
  APKs and the `gplay` AAB and attaches them to a GitHub release. Ask the user before this step
  whether the branch flow switches now (ground rules: `dev`/`main` + PRs near release), because
  wallosmobile's `release-prepare`/`release-finalize` need it.
  **Verify:** a manual run on a pre-release tag attaches signed artifacts that pass `apksigner`.
- [ ] **M9.7** — First release `0.1.0` (version code 1). Tag it, then the IzzyOnDroid inclusion
  request and the Play upload (internal testing first). Each outward step needs the user's go.
  **Verify:** the GitHub release exists; the IzzyOnDroid request and the Play upload are done or
  handed to the user with what they need.
