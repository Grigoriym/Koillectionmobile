# koillectionmobile checklist

**Current step:** M12.2. M12.1 done 2026-11-25. M12 broken into steps 2026-11-25. The user's UX notes from checking the app become steps in the UX pass below when they send them; the final review of the whole app comes after M12 and M13. M11.15 done 2026-11-25. M11.14 done 2026-11-24. M11.13 done 2026-11-24. M11.12 done 2026-11-24. M11.11 done 2026-11-23. M11.10 done 2026-11-23. M11.9 done 2026-11-23. M11 done 2026-11-23. M11.8 done 2026-11-23. M11.7 done 2026-11-23. M11.6 done 2026-11-23. M11.5 done 2026-11-23. M11.4 done 2026-11-23. M11.3 done 2026-11-23. M11.2 done 2026-11-23. M11.1 done 2026-11-23. M11 broken into steps 2026-11-23. M10 done 2026-11-23. M10.6 done 2026-11-23. M10.5 done 2026-11-22. M10.4 done 2026-11-22. M10.3 done 2026-11-22. M10.2 done 2026-11-22. M10.1 done 2026-11-22. M10 broken into steps 2026-11-22. M9 done 2026-11-22. M9.8 done 2026-11-22. M9.7 done 2026-11-22. M9.6 done 2026-11-22. M9.5 done 2026-11-21. M9.4 done 2026-11-21. M9.3 done 2026-11-21. M9.2 done 2026-11-21. M9.1 done 2026-11-21. M9 broken into steps 2026-11-19. M8 done 2026-11-19. M8.9 done 2026-11-19. M8.8 done 2026-11-19. M8.7 done 2026-11-19. M8.6 done 2026-11-18. M8.5 done 2026-11-18. M8.4 done 2026-11-18. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 and 14 are settled.

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

## UX pass (from the user's check of the app, before M12)

Usability fixes the user asks for while checking the app (user, 2026-11-25). Each note gets a step.

- [x] **M11.15** — Tag list: each category is a section that opens and closes on a tap (option 4
  of four the user was shown). The header shows the colour, the label, the tag count and an arrow,
  with a divider between groups. All groups start closed; a lone group starts open. The open
  groups stay in `TagListViewModel`, so they last while the user visits a tag's items.
  **Verify:** `TagListViewModelTest` covers the start state, toggling and the reset on reload; on
  the emulator a group opens and closes, and stays open after a tag's items and Back.
  Note: done 2026-11-25. A mutation (toggle never closes) failed the toggle test.

## M12 — inventories

View and delete (user, 2026-11-17). Koillection 1.8.5 `Inventory.php` has `Get`, `GetCollection`
and `Delete` only. The web UI makes an inventory (`/inventories/add`) and ticks its items
(`/inventories/{id}/check`); both are web routes, not API, so the app does neither. `content` is
JSON that `InventoryHandler::buildInventory` builds: a tree of collections (`id`, `title`,
`children`, `items`, `totalItems`, `totalCheckedItems`), each item `id`, `name`, `checked`. The
seed (`fixtures/seed.py`, M12.1) makes two inventories. Every write goes to the local test instance only
(`docs/local-info.md`). Re-read `Inventory.php` and `InventoryHandler.php` at the start of each
step that touches the API. Model the code on `feature:loans` and `feature:albums`.

- [x] **M12.1** — Test data and fixtures. Extend `fixtures/seed.py` to make two inventories through
  the web form (log in to the web UI, CSRF token from the form page): one over the seed's
  "Comics" root with its child collections, one over "Board games". Tick some items in the first
  with `/inventories/{id}/check`. `--clean` deletes them. Then add the inventory calls to
  `fixtures/capture.py` (list page, one inventory) and capture them.
  **Verify:** the seed run makes both, the API list shows them with the ticked counts, `--clean`
  removes them; the captured JSON shows the `content` tree as the API sends it (record its shape
  in plan §2, including a parent collection that has no items of its own).
  Note: done 2026-11-25. The tick route also ticks every later item in `content`: 3 ticks stored
  4 (plan §2). `capture.py` now takes a group name, so the inventory capture leaves the older
  fixtures alone.
- [ ] **M12.2** — Repository calls (new `feature:inventories:{data,domain,dto,mapper}`). The list
  (`GET /api/inventories`, all pages), one inventory (`GET /api/inventories/{id}`), delete
  (`DELETE /api/inventories/{id}`). The mapper turns `content` into a domain tree; a missing
  `children` or `items` key is an empty list. Copy the headers from `feature:loans:data`. Add the
  new Koin modules' `includes` lines in `AppModule`.
  **Verify:** mock-engine tests per call (method, path), with a root-path base URL case; mapper
  tests on the M12.1 fixtures (tree depth, counts, checked flags); `KoinGraphTest` resolves the
  repository; mutation-check one path and one mapper field.
- [ ] **M12.3** — Inventories section in the drawer and the list screen (`feature:inventories:ui`).
  `DrawerDestination.Inventories` with its own back stack. Each row: name, created date, and
  checked / total items. Loading, empty and error states with retry. A delete action per row
  that asks first; on success the list reads again, a failure shows the error and keeps the list.
  **Verify:** ViewModel tests (load, empty, failure and retry, delete success and failure);
  mutation-check one.
- [ ] **M12.4** — Inventory screen. A tap on a row opens it: the collection tree, each collection
  with its checked / total count, each item with a read-only checked mark. Collections open and
  close on a tap, as the M11.15 tag groups. Delete in the top bar menu, asks first, goes back to
  the list on success. Loading and error states with retry.
  **Verify:** ViewModel tests (load, failure and retry, toggle, delete success and failure);
  mutation-check one.
- [ ] **M12.5** — Device pass on the emulator (`emulator-testing` skill) against the local test
  instance, with the M12.1 seed: open the list, open each inventory, open and close a child
  collection, delete one inventory from the list and one from its screen. Rotate on the inventory
  screen once. Compare the counts with the web UI before the deletes.
  **Verify:** each action works on the device; the counts match the web UI; the instance ends as
  it started (seed rerun or `--clean`).

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
