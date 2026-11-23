# koillectionmobile checklist

**Current step:** M11.5. M11.4 done 2026-11-23. M11.3 done 2026-11-23. M11.2 done 2026-11-23. M11.1 done 2026-11-23. M11 broken into steps 2026-11-23. M10 done 2026-11-23. M10.6 done 2026-11-23. M10.5 done 2026-11-22. M10.4 done 2026-11-22. M10.3 done 2026-11-22. M10.2 done 2026-11-22. M10.1 done 2026-11-22. M10 broken into steps 2026-11-22. M9 done 2026-11-22. M9.8 done 2026-11-22. M9.7 done 2026-11-22. M9.6 done 2026-11-22. M9.5 done 2026-11-21. M9.4 done 2026-11-21. M9.3 done 2026-11-21. M9.2 done 2026-11-21. M9.1 done 2026-11-21. M9 broken into steps 2026-11-19. M8 done 2026-11-19. M8.9 done 2026-11-19. M8.8 done 2026-11-19. M8.7 done 2026-11-19. M8.6 done 2026-11-18. M8.5 done 2026-11-18. M8.4 done 2026-11-18. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 and 14 are settled.

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
M6 (collections: create, edit, delete) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M7 (search and tags) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M8 (templates) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M9 (wishlists and wishes) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M10 (loans) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## M11 — albums and photos

Albums (nested, as collections) and photos: create, edit, delete, with images. Choices are plan §5
decision 14 (user, 2026-11-23). Every write goes to the local test instance only
(`docs/local-info.md`). The test copy has no albums and no photos. Re-read `Album.php` and
`Photo.php` at the start of each step that touches the API. Model the code on `feature:wishlists`.

- [x] **M11.1** — Repository calls (new `feature:albums:{data,domain,dto,mapper}`, the
  wallosmobile split). Albums: list (`GET /api/albums`, all pages), get, children
  (`/api/albums/{id}/children`), create (`title`, optional `parent` IRI), rename (`PATCH`,
  `{"title"}`), upload the image (`POST /api/albums/{id}/image`), remove the image (`PATCH` with
  `deleteImage`), delete. Photos: `GET /api/albums/{id}/photos` (all pages), get, create (`title`,
  `album` IRI, `comment`, `place`), edit (`PATCH`, only the changed keys), upload the image
  (`POST /api/photos/{id}/image`), delete. Take the keys from the entity `Groups`, and copy the
  multipart part name and headers from `feature:wishlists:data`. Add the new Koin modules'
  `includes` lines in `AppModule`.
  **Verify:** mock-engine tests per call (method, path, content type, body keys), with a
  root-path base URL case; `KoinGraphTest` resolves the new repository; mutation-check one body
  key and one path.
- [x] **M11.2** — Live album smoke against the local test instance (a task, as
  `smokeWishlistRun`): create a top-level album and a child, rename the child, upload an image,
  replace it (the old file gives 404), remove it (`image` is null). Create a photo in the child
  with every M11.1 field, upload its image, replace it, edit two fields, and send an explicit
  `null` for `comment` (record whether it clears). Record what `takenAt` and
  `imageSmallThumbnail` hold after the upload. Send a photo with an empty title (expect 422) and
  one without `album`. Record whether `GET /api/albums` lists the child albums too. Delete the
  parent; the child and the photo give 404. Record each answer in plan §2.
  **Verify:** the run prints each step and passes; the test instance ends as it started; plan §2
  has the results.
  Note: `./gradlew :composeApp:smokeAlbumRun` (`LiveAlbumSmoke.kt`). Every case answered as
  expected. `GET /api/albums` lists child albums too; `"comment": null` clears; `takenAt` stays null
  after an upload; `imageSmallThumbnail` equals `image` for an image 300 px wide or less.
- [x] **M11.3** — Album browse (`feature:albums:ui`). An "Albums" entry in the collection list's
  "More" menu. The album list shows the top-level albums; a tap opens the album screen with its
  child albums and a grid of its photos (`imageSmallThumbnail`, title). Loading, empty and error
  states with retry on both screens. New Koin module: add its `includes` line in `AppModule`.
  **Verify:** ViewModel tests (order, empty, failure and retry) for both screens.
  Note: the web UI lists photos in database order, which the API does not show, so the app sorts
  photos by title ignoring case, as it sorts albums. A photo cell has no tap yet (M11.6).
- [x] **M11.4** — Album form: create and edit. A "new album" action on the album list (top level)
  and on the album screen (child). The form edits the title and the image (set, replace, remove;
  camera or picker from `core:photos`), saved with the progress view and retry. Edit opens
  prefilled and sends only what changed. On success the list or the parent reads again. The form
  survives rotation and process death.
  **Verify:** ViewModel tests (create, each change alone, no change sends no call, failure and
  retry); mutation-check one.
  Note: the album screen's "More" menu holds "New album" and "Edit"; the list has a "New album"
  button. A save signals `AlbumChanges` (`feature:albums:domain`, bound in `:data`), which the list
  and the album screen collect. Mutation: dropping `trim()` from the title diff fails one test.
  No device run in this step (M11.8).
- [ ] **M11.5** — Delete album: a delete action on the album screen. The confirm names the album
  and counts the child albums (every level) and the photos they hold, as M9.5 counts wishlists.
  On success the screen leaves the back stack and the list or the parent reads again. A failure
  shows the error and keeps the screen.
  **Verify:** ViewModel tests (counts, success, failure).
- [ ] **M11.6** — Photo screen and delete. A tap on a photo opens its screen: the image, title,
  comment, place, taken date. A delete action asks first; on success the album screen reads
  again.
  **Verify:** ViewModel tests (load, failure and retry, delete success and failure).
- [ ] **M11.7** — Photo form: create and edit. A "new photo" action on the album screen and an
  "edit" action on the photo screen. Fields: title (required), comment, place, image (required
  for a new photo; set and replace, no remove, decision 14). Edit sends only what changed;
  clearing a field follows the M11.2 answer for an explicit `null`. The form survives rotation
  and process death.
  **Verify:** ViewModel tests (create, create without image blocked, each change alone, cleared
  field, failure and retry); mutation-check the image rule.
- [ ] **M11.8** — Device pass on the emulator (`emulator-testing` skill) against the local test
  instance: open Albums from the "More" menu, create an album with an image and a child, add a
  photo with every field, edit it, replace both images, remove the album image, then delete the
  photo and the parent album. Compare with the web UI before the delete.
  **Verify:** each action works on the device; the web UI shows the same values; the instance
  ends as it started.

## M12 — inventories (rough scope)

View and delete (user, 2026-11-17). Koillection 1.8.5 has no `Post` for `Inventory`, so the app
does not create one. Break into steps at the start.

## M13 — logs (rough scope)

View only (user, 2026-11-17): `Log` has `Get` and `GetCollection` only. The test copy has 610
rows: the list needs paging. Break into steps at the start.

## M14 — release

Runs after M4–M13 (user, 2026-11-16; renumbered from M9 on 2026-11-17). Both store flavors ship, as in the user's other apps (user, 2026-11-16): `fdroid` to
IzzyOnDroid first, `gplay` to Google Play. Model: `../Wallosmobile` (`build-logic`
`configureAppSigningConfigs`, `androidApp/build.gradle.kts` dependency-info block,
`.github/workflows/release*.yml`, `fastlane/metadata/`). No Firebase: wallosmobile's
`google-services.json` steps are not ported. Two inputs come from the user later: the launcher
icon (M14.4) and the release keystores (M14.5). Those steps wait for them; the others do not.

- [ ] **M14.1** — Release blockers. Check the subpath question read only, from the user's server
  URL (`docs/revisit.md`, M1.9 entry). Go through every `docs/revisit.md` entry with the user;
  each one is "fix before release" (gets a step here) or "after release".
  **Verify:** the M1.9 entry is closed or has a recorded answer; each revisit entry has the
  user's verdict, written in the entry.
- [ ] **M14.2** — R8 release build. `assembleFdroidRelease` and `assembleGplayRelease` build with
  minify and resource shrinking on (never run so far; `androidApp/proguard-rules.pro` is empty).
  Sign the `fdroidRelease` APK by hand with the debug key (`apksigner`, no build change) and run
  login, browse and add item on the emulator against the local test instance. Add a keep rule
  only for what R8 breaks.
  **Verify:** both release tasks pass; the hand-signed APK passes the flow on the emulator.
- [ ] **M14.3** — Store texts. `README.md` (what the app does, unofficial and not affiliated with
  Koillection, the Koillection version it was tested against, where to install it), a privacy
  policy (Play needs one; the app talks only to the user's own server), and
  `fastlane/metadata/android/en-US/` (title, short and full description, `changelogs/1.txt`,
  phone screenshots). Screenshots come from the local test instance, which is a copy of the
  user's real catalogue: the user approves what they show before they are committed.
  **Verify:** the user approves the texts and the screenshots.
- [ ] **M14.4** — Launcher icon, from the image the user provides (waits for it). Adaptive icon
  with a monochrome layer, plus the 512px `fastlane/.../images/icon.png`.
  **Verify:** the icon shows on the emulator launcher, light and dark.
- [ ] **M14.5** — Release signing (waits for the user's keystores). Port wallosmobile's
  per-flavor signing configs: keystore files at the repo root (gitignored), passwords from env
  vars. Turn off the APK dependency-info block, which F-Droid's scanner rejects. Confirm with the
  user whether wallosmobile's signed `fdroidDebug` channel is wanted here too.
  **Verify:** `apksigner verify --print-certs` shows the right certificate for each flavor; the
  `fdroid` APK has no extra signing block.
- [ ] **M14.6** — Release workflow. Port wallosmobile's `release.yml`: a `v*` tag builds the signed
  APKs and the `gplay` AAB and attaches them to a GitHub release. Ask the user before this step
  whether the branch flow switches now (ground rules: `dev`/`main` + PRs near release), because
  wallosmobile's `release-prepare`/`release-finalize` need it.
  **Verify:** a manual run on a pre-release tag attaches signed artifacts that pass `apksigner`.
- [ ] **M14.7** — First release `0.1.0` (version code 1). Tag it, then the IzzyOnDroid inclusion
  request and the Play upload (internal testing first). Each outward step needs the user's go.
  **Verify:** the GitHub release exists; the IzzyOnDroid request and the Play upload are done or
  handed to the user with what they need.
