# koillectionmobile checklist

**Current step:** M6.6. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7 and 8 are settled.

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
M5 (edit and delete items) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## M6 — collections: create, edit, delete

Create a collection (top level or child), rename it, set, replace or remove its image, delete it
(with a confirm that counts what goes with it). Form choices are plan §5 decision 9 (user,
2026-11-16). Every write goes to the local test instance only (`docs/local-info.md`).

- [x] **M6.1** — Repository writes (`feature:collections`): create (`POST /api/collections`,
  `title` and an optional `parent` IRI), rename (`PATCH /api/collections/{id}`, `{"title"}`),
  upload the image (`POST /api/collections/{id}/image`, multipart), remove the image (`PATCH`
  with `deleteImage`), delete (`DELETE /api/collections/{id}`). Re-read `Collection.php` first,
  and copy the multipart part name and headers from `feature:items:data` (`CLAUDE.md`, API
  contract). A `PATCH` sends `application/merge-patch+json` and only the changed keys.
  **Verify:** mock-engine tests for each call (method, path, content type, body keys), with a
  root-path base URL case; mutation-check one body key and one method.
- [x] **M6.2** — Live collection smoke against the local test instance: extend `smokeWriteRun`
  (or add a task) to create a top-level collection and a child, rename the child, upload an
  image, replace it (the old file gives 404), remove it (the file gives 404 and `image` is null),
  add an item to the child, then delete the parent. After the delete, the child and the item give
  404. Also send a create with an empty title (expect 422). Record the answers in plan §2.
  **Verify:** the run prints each step and passes; plan §2 has the results.
  Note: a separate task, `./gradlew :composeApp:smokeCollectionRun` (`LiveCollectionSmoke.kt`),
  so the item run stays as it was. The empty-title 422 was also confirmed with `curl`, since
  `KoillectionError.Validation` covers 400 and 422.
- [x] **M6.3** — Delete collection: a delete action on the collection screen. The confirm dialog
  names the collection and counts the child collections (every level) and the items they hold;
  decide in the step how to count the items (for example `totalItems` of each
  `/api/collections/{id}/items`). On success the screen leaves the back stack, and the list or
  the parent collection reads its children again. A failure shows the error and keeps the screen.
  **Verify:** ViewModel tests (counts, success, failure); on the emulator a temporary collection
  with a child and an item is deleted, the list no longer shows it, and the web UI agrees.
  Note: the count is one `getCollections()` (descendants by `parentId`) plus a new
  `CollectionsRepository.countItems` per collection (`totalItems` of the first item page); the API
  has no counters (plan §2). A new `CollectionListChanges` signal makes the list or the parent read
  again. On the device a child (1 collection, 2 items) and then its parent (0, 0) were deleted.
- [x] **M6.4** — Create collection: a "new collection" action on the collection list (top level)
  and on the collection screen (child). A form with the title and an optional image (camera or
  picker, M3's photo preparation), saved with the M3 progress view and retry. On success the new
  collection opens, and the list or the parent shows it. The form survives rotation and process
  death. Delete the photo files in `cacheDir/photos/` as M3.6 does.
  **Verify:** ViewModel tests; on the emulator a top-level and a child collection are created,
  one with an image, and the web UI shows them.
  Note: the photo code moved to a new `core:photos` (plan §5 decision 4). The form has its own
  save view and messages in `feature:collections:ui`, a small copy of M3's. The list has a FAB;
  the collection screen has a "More actions" menu with "New collection". "Pick photo" opens the
  multi-select picker and keeps the first photo, as the edit form's "Replace" does.
- [x] **M6.5** — Edit collection: an "edit" action on the collection screen opens the M6.4 form,
  prefilled (title, image from its URL). The image can be replaced or removed. The save sends only
  what changed. On success the collection screen and the list read the collection again.
  **Verify:** ViewModel tests (each change alone, no change sends no call, a failure and the
  retry); on the emulator a rename, an image replace and an image remove show in the app and in
  the web UI.
  Note: its own `EditCollectionViewModel`; the screen shares the M6.4 frame, form and save view
  (`CollectionFormScaffold`, `CollectionForm`, `CollectionSaveView`). One call per change (rename,
  upload, image delete); the calls that went through are kept in `SavedStateHandle`, so a retry
  sends only the rest. "Remove image" drops a new photo and the server image both. A new
  `CollectionChanges` signal makes the collection screen read again; `CollectionListChanges` (the
  parent) does the list. Process death is covered by ViewModel tests only, not on the device.
- [ ] **M6.6** — Device pass of create, edit and delete, then the user confirms with screenshots.
  Fix what breaks, or put it in `docs/revisit.md` with evidence.
  **Verify:** the user confirms.

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
