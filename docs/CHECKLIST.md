# koillectionmobile checklist

**Current step:** M7.6. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9 and 10 are settled.

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

## M7 — search and tags

Search items by name, browse tags and the items of a tag, create a tag from the add-item form.
Choices are plan §5 decision 10 (user, 2026-11-17). Every write goes to the local test instance
only (`docs/local-info.md`).

- [x] **M7.1** — Filter in a collection: a search field on the collection screen filters the items
  it already loaded by name (substring, ignoring case). No new API call. The query survives
  rotation and process death. An empty result shows a "no match" text, not the empty-collection
  text.
  **Verify:** ViewModel tests (match, no match, empty query shows all, case); on the emulator a
  query on the 445-item collection narrows the list and clearing it shows all items again.
  Note: the filter covers items only; child collections stay listed. A blank query counts as empty.
- [x] **M7.2** — Global item search. `ItemsRepository.getAllItems()` reads `GET /api/items`, all
  pages. A search action on the collection list opens a search screen. It reads all items once on
  open (loading and error states, with retry) and then filters by name in the app, as M7.1 does.
  A result row shows the item name and its collection title; a tap opens the item. Re-read
  `Item.php` first.
  **Verify:** mock-engine test (path, every page, root-path base URL case); ViewModel tests
  (load, failure and retry, filter); on the emulator a query finds an item and opens it.
  Note: the screen lives in `feature:collections:ui` (it needs both domains) and reads
  `getCollections()` in parallel for the titles, as an item names its collection by IRI only. It
  reads everything again on any `ItemListChanges` signal, so a deleted item leaves the results.
- [x] **M7.3** — Tag repository calls. `ItemsRepository.getTagItems(tagId)` reads
  `GET /api/tags/{id}/items`, all pages (in `feature:items`, because `feature:items:domain`
  already depends on `feature:tags:domain`). `TagsRepository.createTag(label)` sends
  `POST /api/tags` with `{"label"}` only. Re-read `Tag.php` and `Item.php` first.
  **Verify:** mock-engine tests for each call (method, path, content type, body keys), with a
  root-path base URL case; mutation-check the body key and one path.
  Note: `createTag` returns the new `Tag`. The mutations were the body key (`label` → `name`) and
  the `getTagItems` path (`tags` → `tag`); each failed its test.
- [x] **M7.4** — Live tag smoke against the local test instance: a task (as
  `smokeCollectionRun`) creates a tag, sets it on an item next to the item's existing tags, reads
  `/api/tags/{id}/items` (the item is there), takes it off the item again, then deletes the tag.
  Also send a create with an empty label (expect 422) and a create with a label that already
  exists (record what the server does). Record the answers in plan §2.
  **Verify:** the run prints each step and passes; the item's tags end as they started; plan §2
  has the results.
  Note: the task is `smokeTagRun`. The app has no tag delete call, so the run sends
  `DELETE /api/tags/{id}` through the app's own `HttpClient`. A duplicate label creates a second tag.
- [x] **M7.5** — Tag list and tag items (`feature:tags:ui`, the placeholder today). A tags action
  on the collection list opens the tag list: one section per tag category with its colour, sorted
  by label, then the tags without a category; tags sorted by label inside a section, ignoring case.
  A tap opens the tag's items (M7.3); a tap on an item opens the item. Loading, empty and error
  states with retry on both screens. Reuse the add form's grouping (`groupTags`,
  `feature:items:ui`); move it if the new screen needs it.
  **Verify:** ViewModel tests (grouping and order, empty, failure and retry) for both screens; on
  the emulator the list matches the web UI's tags and one tag shows its items.
  Note: `TagGroup`, `groupTags` and `loadTagGroups` moved to `feature:tags:domain`; `TagGroup` got
  the category `color`. The tag items screen lives in `feature:tags:ui` (now on
  `feature:items:domain`), keeps the server's order and reads again on `ItemListChanges`. Its route
  carries the tag label for the title. The device check compared with `GET /api/tags` and
  `/api/tag_categories` (30 tags, 4 categories, none without one), not the web page itself.
- [ ] **M7.6** — Create a tag from the add-item form: a "new tag" action in `TagPicker` takes a
  label. A label that exists already (ignoring case) selects the existing tag and creates nothing.
  A new tag is created on Save, before the item's tags are set, so an abandoned form leaves no
  tag; a retry does not create it twice. The pending tag survives rotation and process death. The
  edit form shares `TagPicker`: ask the user at the start of the step whether it gets the action
  too.
  **Verify:** ViewModel and use-case tests (new label, existing label, failure at the tag create
  and the retry); on the emulator an item is saved with a new tag, and the web UI shows the tag on
  the item.
- [ ] **M7.7** — Device pass of search, the tag screens and tag creation, then the user confirms
  with screenshots. Fix what breaks, or put it in `docs/revisit.md` with evidence. Delete the test
  tags and items afterwards.
  **Verify:** the user confirms.

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
