# koillectionmobile checklist

**Current step:** M13.2. M13.1 done 2026-11-25. M13 broken into steps 2026-11-25. M12 done 2026-11-25. M12.5 done 2026-11-25. M12.4 done 2026-11-25. M12.3 done 2026-11-25. M12.2 done 2026-11-25. M12.1 done 2026-11-25. M12 broken into steps 2026-11-25. The user's UX notes from checking the app become steps in the UX pass below when they send them; the final review of the whole app comes after M12 and M13. M11.15 done 2026-11-25. M11.14 done 2026-11-24. M11.13 done 2026-11-24. M11.12 done 2026-11-24. M11.11 done 2026-11-23. M11.10 done 2026-11-23. M11.9 done 2026-11-23. M11 done 2026-11-23. M11.8 done 2026-11-23. M11.7 done 2026-11-23. M11.6 done 2026-11-23. M11.5 done 2026-11-23. M11.4 done 2026-11-23. M11.3 done 2026-11-23. M11.2 done 2026-11-23. M11.1 done 2026-11-23. M11 broken into steps 2026-11-23. M10 done 2026-11-23. M10.6 done 2026-11-23. M10.5 done 2026-11-22. M10.4 done 2026-11-22. M10.3 done 2026-11-22. M10.2 done 2026-11-22. M10.1 done 2026-11-22. M10 broken into steps 2026-11-22. M9 done 2026-11-22. M9.8 done 2026-11-22. M9.7 done 2026-11-22. M9.6 done 2026-11-22. M9.5 done 2026-11-21. M9.4 done 2026-11-21. M9.3 done 2026-11-21. M9.2 done 2026-11-21. M9.1 done 2026-11-21. M9 broken into steps 2026-11-19. M8 done 2026-11-19. M8.9 done 2026-11-19. M8.8 done 2026-11-19. M8.7 done 2026-11-19. M8.6 done 2026-11-18. M8.5 done 2026-11-18. M8.4 done 2026-11-18. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 and 16 are settled.

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
M11 (albums and photos) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
The M11 follow-up (M11.9–M11.14, fixes from the user's review) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M12 (inventories) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## UX pass (from the user's check of the app, before M12)

Usability fixes the user asks for while checking the app (user, 2026-11-25). Each note gets a step.

- [x] **M11.15** — Tag list: each category is a section that opens and closes on a tap (option 4
  of four the user was shown). The header shows the colour, the label, the tag count and an arrow,
  with a divider between groups. All groups start closed; a lone group starts open. The open
  groups stay in `TagListViewModel`, so they last while the user visits a tag's items.
  **Verify:** `TagListViewModelTest` covers the start state, toggling and the reset on reload; on
  the emulator a group opens and closes, and stays open after a tag's items and Back.
  Note: done 2026-11-25. A mutation (toggle never closes) failed the toggle test.

## M13 — logs

View only (user, 2026-11-17), as a plain list (plan §5 decision 16, user 2026-11-25). Koillection
1.8.5 `Log.php` has `Get` and `GetCollection` only, with no filter and no order. The API sends the
oldest first, 30 per page; the test copy has 1040 rows on 35 pages (plan §2). The app shows the
newest first: it reads the `last` page, then each `previous` page on scroll. Logs need no seed: the
test copy has them. Re-read `Log.php` at the start of each step that touches the API. Model the
code on `feature:inventories`.

- [x] **M13.1** — Fixtures. Add a `logs` group to `fixtures/capture.py`: the first page
  (`/api/logs`, for `view.last`), the last page, one middle page (it has `previous` and `next`)
  and one log (`GET /api/logs/{id}`). Capture only that group.
  **Verify:** the captured pages show the `view` keys and the ascending order that plan §2 says;
  the older fixtures are unchanged (`git status` shows only the new files and `capture.py`).
  Note: done 2026-11-25. `logs_page_middle.json` is page 34, the `previous` of the last page (35,
  20 rows). The keys are plain `view`, `last`, `previous`, with no `hydra:` prefix. A run with no
  group name now captures `logs` too.
- [ ] **M13.2** — Repository calls (new `feature:logs:{data,domain,dto,mapper}`). Add `last` and
  `previous` to `core:api`'s `HydraViewDto`, each also read under its `hydra:` name. A newest-first
  page call: the first call reads `api/logs` and then its `view.last` (one request when there is no
  `view`); each later call reads the `previous` path it got. It returns the rows newest first and
  the next path to read (null at the end). The mapper turns `type` into create, delete or unknown,
  and `objectClass` into one of the 11 classes or unknown (from the short class name). Copy the
  headers from `feature:inventories:data`. Add the new Koin modules' `includes` lines in `AppModule`.
  **Verify:** mock-engine tests (paths for the first, a later and the last call, a list with no
  `view`, a root-path base URL case); mapper tests on the M13.1 fixtures (order, type, class, an
  unknown value); `KoinGraphTest` resolves the repository; mutation-check one path and one mapper
  field.
- [ ] **M13.3** — Logs section in the drawer and the list screen (`feature:logs:ui`).
  `DrawerDestination.Logs` with its own back stack. Each row: the action (created or deleted), the
  object type, the label and the date with the time. The next page loads when the user scrolls near
  the end; a row whose id is already in the list is dropped. Loading, empty and error states with
  retry. A failed later page keeps the rows and shows a retry row at the end.
  **Verify:** ViewModel tests (first load, next page, the end, a duplicate id, empty, failure and
  retry for the first and a later page); mutation-check one.
- [ ] **M13.4** — Device pass on the emulator (`emulator-testing` skill) against the local test
  instance: open Logs, compare the first rows with the web `/history` page, scroll until at least
  three pages have loaded, rotate once, then make an item on the web UI and restart the app.
  **Verify:** the newest rows match the web page in order and text; paging shows no duplicate and
  no gap across the page seams; the new item's log is on top after the restart; the test item is
  deleted at the end.

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
