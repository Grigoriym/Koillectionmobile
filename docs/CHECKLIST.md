# koillectionmobile checklist

**Current step:** M3.6. M3.5 done 2026-11-15, M3.4 done 2026-11-15, M3.3 done 2026-11-15, M3.2 done 2026-11-15, M3.1 done 2026-11-15; M3 broken into steps 2026-11-15. M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6 and 7 are settled.

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

## M3 — add item with photos

Pick a collection, take or pick photos (main image + page images), text fields, tags. Matches
the book workflow in plan §1; form choices are plan §5 decision 7 (user, 2026-11-15). Every write
goes to the local test instance (`docs/local-info.md`), never the real server. Lessons from
`add_book.py` (`~/claude/books/00service/`): downscale to 2400px JPEG q85, fields get `position`
in order, and a failure halfway must not lead to a second item on retry.

- [x] **M3.1** — Live write smoke against the local test instance: extend M1.11's JVM run (or add a
  second task) that creates an item in `Books`, two text fields, an image field, uploads a main
  image and a field image, sets two existing tags, reads the item back, then deletes it. This is
  the first time M1.9/M1.10's writes reach a server. Record the answers in plan §2.
  **Verify:** the run prints the created item read back (fields, image URLs, tags) and the delete;
  a `GET` of the item after the delete gives 404.
  Note: a second task, `./gradlew :composeApp:smokeWriteRun` (`LiveWriteSmoke.kt`); the delete
  goes through the app's `HttpClient`, as the repositories have no delete. Every write worked as
  sent; answers in plan §2. First, `smokeRun` hung with M1.11's expired stored JWT: `bearerAuth`
  sent the bearer on the login at a root-path server, the server answered 401, and the re-login
  deadlocked on its own mutex. Fixed in a separate commit with a test (`794ffb2`).
- [x] **M3.2** — Image preparation (Android): read a content URI, apply its EXIF orientation plus
  an extra rotation (0/90/180/270, clockwise), downscale so the long side is at most 2400px,
  encode JPEG q85 to a `ByteArray`. Pure size and rotation math in `commonMain` with tests; the
  `Bitmap` part in `androidMain`. Pick the module in the step (a new `core:image`, or
  `feature:items:data`) and say why.
  **Verify:** `jvmTest --rerun` for the math; `build ktlintCheck` green. Device output is checked
  in M3.5.
  Note: a new `core:image` (`ImageMath.kt` in `commonMain`, `ImagePreparer` in `androidMain`), not
  `feature:items:data`: M3.5's preview in `feature:items:ui` needs it too. EXIF tags 2, 4, 5 and
  7 mirror as well as rotate. The platform `android.media.ExifInterface` reads a stream from API
  24 (= minSdk), so there is no new dependency. No DI binding yet: M3.3 or M3.5 adds it.
- [x] **M3.3** — Add-item use case (`feature:items`): create the item, then text fields in order,
  the main image, image fields (each created, then uploaded), then tags. It reports progress, and
  on a failure it keeps what exists on the server (item id, finished fields), so a retry continues
  from the failed part and never creates a second item.
  **Verify:** tests with hand-written fake repositories: the call order and positions, a failure
  at each stage, and a retry that sends only the remaining parts (`jvmTest --rerun`).
  Note: `AddItemUseCase` in `feature:items:domain`, which now depends on `feature:tags:domain`
  (`setItemTags`). It is stateless: a `Failure` returns an `AddItemState` (item id, fields done,
  image field ids), and M3.6's ViewModel passes it back on the retry. Positions: text fields from
  1, then the image fields. An empty tag list sends no call. Images come in as `ByteArray`. No DI
  binding yet: M3.6 adds it with its first caller. The build-logic stability plugin now adds
  compose-runtime to `commonTest` too: without it the iOS test compile failed ("requires the
  Compose Runtime to be on the class path").
- [x] **M3.4** — Add-item screen, text part: a nav key with the collection id, an "add" action on
  the collection screen, a form with the name, free label/value rows (add, remove) and a tag
  picker (existing tags only, grouped by category). Labels must be unique within the item
  (server rule, plan §2): the form says so before save.
  **Verify:** ViewModel tests (`jvmTest --rerun`); on the emulator the form opens from `Books`
  and shows the server's tags.
  Note: `AddItemRoute` + `AddItemScreen`/`AddItemViewModel` in `feature:items:ui`; a FAB "Add item"
  on the collection screen. The ViewModel does not take the collection id yet (only the route
  has it): M3.6's save adds it. Duplicate labels compare exactly, as the server does (`===` in
  `DatumLabelNotExistsInParentValidator`), so "Author" and "author" pass; blank labels are not
  flagged (the server's `NotBlank` is M3.6's concern). Tags: one group per category, sorted by
  label, tags without a known category last. No save button and no process-death state yet
  (M3.5, M3.6). Emulator: 30 tags in 4 categories, the same as `GET /api/tags`.
- [x] **M3.5** — Photos in the form: camera (system camera intent, `FileProvider`) and the photo
  picker (several at once). One photo is the main image; the rest are image fields with a label
  each, in order. Each photo has a rotate button and a preview that shows the result of M3.2.
  The form survives rotation and process death (photo URIs and labels kept).
  **Verify:** ViewModel tests; on the emulator a camera shot and two picked images show in the
  form, a rotate turns the preview, and `am kill` keeps them.
  Note: `PhotoSources.kt` (`expect`) in `feature:items:ui`, with the Android `actual` (camera
  intent, picker, preview) and empty JVM/iOS ones. The first photo is the main image; a "make
  main" button moves another to the front. Image-field labels join the duplicate-label check, as
  the server checks all data of an item. The preview runs `ImagePreparer.bitmap` (new, split out
  of `prepare`) at 512px, so it shows the upload's orientation. The whole form (name, rows,
  photos, picked tags) is a JSON draft in `SavedStateHandle`. A picked photo is copied to
  `cacheDir/photos/`, as camera shots are: the picker's URI dies with the process, and a persisted
  grant has a per-app limit (512), so the user chose the copy. `KoinGraphTest` now lists
  `SavedStateHandle` as externally supplied.
- [ ] **M3.6** — Save: run M3.3 from the form with a progress view, a retry on failure, and on
  success open the new item (the add screen leaves the back stack) and refresh the collection.
  An upload 500 names the disk quota as a likely cause (plan §2). Delete the photo files in
  `cacheDir/photos/` (M3.5) after a successful save, and when a photo is removed or the form is
  left (`docs/revisit.md`).
  **Verify:** ViewModel tests; on the emulator an item with two text fields, a main image, two
  page images and two tags is created on the local test instance, and the web UI shows the same
  item, upright and in order. A save with the server stopped halfway, then retried, gives one
  item, not two.
- [ ] **M3.7** — Device pass of the whole add flow, then the user confirms with screenshots. Fix
  what breaks, or put it in `docs/revisit.md` with evidence. Check `docs/revisit.md`'s M1.9 entry
  (IRIs without the server's subpath) against the user's real setup before M3 is called done:
  ask the user whether their server has a subpath; do not write to it.
  **Verify:** the flow passes on the emulator; the user confirms.

## M4 — release (rough scope)

Signing, F-Droid/IzzyOnDroid and/or Play per plan §5 decision 2.
