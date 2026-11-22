# koillectionmobile checklist

**Current step:** M10.5. M10.4 done 2026-11-22. M10.3 done 2026-11-22. M10.2 done 2026-11-22. M10.1 done 2026-11-22. M10 broken into steps 2026-11-22. M9 done 2026-11-22. M9.8 done 2026-11-22. M9.7 done 2026-11-22. M9.6 done 2026-11-22. M9.5 done 2026-11-21. M9.4 done 2026-11-21. M9.3 done 2026-11-21. M9.2 done 2026-11-21. M9.1 done 2026-11-21. M9 broken into steps 2026-11-19. M8 done 2026-11-19. M8.9 done 2026-11-19. M8.8 done 2026-11-19. M8.7 done 2026-11-19. M8.6 done 2026-11-18. M8.5 done 2026-11-18. M8.4 done 2026-11-18. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12 and 13 are settled.

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

## M10 — loans

Full CRUD of the loans of one item, on the item screen. Choices are plan §5 decision 13 (user,
2026-11-22). Every write goes to the local test instance only (`docs/local-info.md`). The test
copy has no loans. Re-read `Loan.php` at the start of each step that touches the API. Model the
code on `feature:wishlists`.

- [x] **M10.1** — Repository calls (new `feature:loans:{data,domain,dto,mapper}`, the
  wallosmobile split). The loans of an item (`GET /api/items/{id}/loans`, all pages), create
  (`item` IRI, `lentTo`, `lentAt`), edit (`PATCH`, only the changed keys; a null `returnedAt`
  goes out as an explicit `null`), delete (`DELETE /api/loans/{id}`). The domain dates are
  `LocalDate`; the mapper takes the date part of the server's date-time. Take the keys from the
  entity `Groups`, and copy the headers from `feature:wishlists:data`. Add the new Koin modules'
  `includes` lines in `AppModule`.
  **Verify:** mock-engine tests per call (method, path, content type, body keys), with a
  root-path base URL case; mapper tests for the date part; `KoinGraphTest` resolves the new
  repository; mutation-check one body key and one path.
  Note: an edit is a `List<LoanChange>` (sealed: `LentTo`, `LentAt`, `ReturnedAt`), not a
  `Map<Field, String?>` as in wishes, because the values are a string and dates. Dates go out as
  `yyyy-MM-dd`; M10.2 checks what the server makes of that. Mutations caught: the `returnedAt`
  key (2 tests), the `/loans` list path (2 tests).
- [x] **M10.2** — Live loan smoke against the local test instance (a task, as
  `smokeWishlistRun`): on a throwaway item, create a loan with a date-only `lentAt` and record
  the value the server gives back (time and offset). Edit `lentTo`, set `returnedAt`, clear it
  with an explicit `null`. Send an empty `lentTo` and one without `lentAt` (expect 422 each).
  List the item's loans. Delete one loan (404 after), then delete the item: its other loan gives
  404. Record each answer in plan §2; if the date-only value shifts a day, fix M10.1's format in
  this step.
  **Verify:** the run prints each step and passes; the test instance ends as it started; plan §2
  has the results.
  Note: `./gradlew :composeApp:smokeLoanRun` (`LiveLoanSmoke.kt`). Every case answered as
  expected. A date-only `lentAt` comes back as midnight with the server's offset
  (`T00:00:00+01:00`) and the same day, so M10.1's format stays.
- [x] **M10.3** — Loans on the item screen (`feature:loans:ui`). A "Loans" section lists the
  item's loans: lent to, lent date, returned date or "not returned". Order: not returned first,
  then by lent date, newest first. Loading, empty and error states with retry for the section; the
  rest of the item screen does not wait for it. New Koin module: add its `includes` line in
  `AppModule`.
  **Verify:** ViewModel tests (order, empty, failure and retry).
  Note: `ItemScreen` takes a `loans` slot and `MainNavHost` passes `LoansSection`, so
  `feature:items:ui` does not depend on the loans. Dates show as `yyyy-MM-dd`. Not run on a device
  (M10.6 does). Mutation caught: the lent-date order (1 test).
- [x] **M10.4** — Loan form: create and edit. A "new loan" action in the Loans section; a tap on
  a loan opens it in the form. Fields: lent to (required, trimmed), lent date (date picker,
  default today), returned date (date picker, optional, a clear button). Edit opens prefilled and
  sends only what changed; no change sends no call. Saved with the progress view and retry; on
  success the section reads again. The form survives rotation and process death.
  **Verify:** ViewModel tests (create, each change alone, cleared returned date, no change,
  blank name blocked, failure and retry); mutation-check one.
  Note: one screen and ViewModel for both (`LoanFormRoute(itemId, loanId?)`). The new form has no
  returned date, as the web `LoanType`; the returned date is on the edit form only. The edit form
  reads the item's loans and takes the one with the route id (no single-loan call in the
  repository). `LoanChanges` (domain, bound in `LoansDataModule`) makes the section read again. A
  save that a process death stopped is not kept: Save sends it again. Not run on a device (M10.6
  does). Mutation caught: the `returnedAt` change (2 tests).
- [ ] **M10.5** — Mark returned and delete. Each loan that is not returned has a "mark returned"
  action (sets today). Each loan has a delete action that asks first. On success the section
  reads again; a failure shows the error and keeps the list.
  **Verify:** ViewModel tests (mark returned sends today's date, delete success and failure).
- [ ] **M10.6** — Device pass on the emulator (`emulator-testing` skill) against the local test
  instance: on an item, add two loans, edit one, mark one returned, clear its returned date, then
  delete both. Rotate on the open form once. Compare with the web UI's `/loans` page before the
  delete.
  **Verify:** each action works on the device; the web UI shows the same names and dates; the
  instance ends as it started.

## M11 — albums and photos (rough scope)

Full CRUD with images (user, 2026-11-17): albums (nested), photos, and the image upload of each
(`/api/albums/{id}/image`, `/api/photos/{id}/image`). Break into steps at the start.

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
