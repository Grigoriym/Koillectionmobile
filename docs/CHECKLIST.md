# koillectionmobile checklist

**Current step:** M8.4. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10 and 11 are settled.

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

## M8 — templates

Templates with their fields, choice lists, typed fields in the item forms, and a collection's
default template. Choices are plan §5 decision 11 (user, 2026-11-17). Every write goes to the
local test instance only (`docs/local-info.md`). Re-read `Template.php`, `Field.php`,
`ChoiceList.php` and `Datum.php` at the start of each step that touches the API.

- [x] **M8.1** — Repository calls (new `feature:templates:{data,domain,dto,mapper}`, the
  wallosmobile split). Templates: list, get, create (`{"name"}`), rename, delete. Fields:
  `GET /api/templates/{id}/fields` (all pages, sorted by `position` in the app), create
  (`name`, `position`, `type`, `template` IRI, `choiceList` IRI for `choice-list`), edit, delete.
  Choice lists: list, create and edit (`name`, `choices`), delete. Take the keys from the entity
  `Groups`, not from memory.
  **Verify:** mock-engine tests per call (method, path, content type, body keys), with a
  root-path base URL case; mutation-check one body key and one path.
  Note: done 2026-11-17. The test instance has no templates or choice lists, so the tests use
  inline bodies, not fixtures. `TemplatesDataModule` and `TemplatesMapperModule` are in
  `AppModule` now, and `KoinGraphTest` resolves `TemplatesRepository`. `editField` with a null
  `choiceListId` keeps the stored choice list: a merge patch cannot clear it without an explicit
  null, which the app does not send yet.
- [x] **M8.2** — Live template smoke against the local test instance (a task, as `smokeTagRun`):
  create a choice list, a template, one field of each type the item form will send (`text`,
  `number`, `checkbox`, `choice-list`), set the template as a collection's
  `itemsDefaultTemplate`, read `/api/collections/{id}/items_default_template`, add an item with a
  `checkbox` and a `choice-list` datum, then delete everything it made and clear the default.
  Also send a `choice-list` field without a `choiceList` and two fields with one name in one
  template; record each answer. Record in plan §2 what the server stores for each value (source:
  `DatumType.php` gives `checkbox` as `"1"`/`"0"`, `choice-list` as a JSON array, `date` as
  `Y-m-d`; confirm over the API) and what a template delete does to its fields.
  **Verify:** the run prints each step and passes; the test instance ends as it started; plan §2
  has the results.
  Note: done 2026-11-17 (`smokeTemplateRun`). It uses its own top-level collection, not `Books`.
  A second field with a name the template has already got 201: the server does not check field
  names, so M8.4 must. The run also checked a template delete while it is a collection's default
  (the default becomes null) and a `checkbox` value `true` (422).
- [x] **M8.3** — Template list and template screen (`feature:templates:ui`). An action on the
  collection list opens the template list (sorted by name, ignoring case); a tap shows the
  template's fields in position order with their type (and the choice list for `choice-list`).
  Loading, empty and error states with retry on both screens. New Koin module: add its
  `includes` line in `AppModule`.
  **Verify:** ViewModel tests (order, empty, failure and retry) for both screens.
  Note: done 2026-11-17. The template screen reads the fields and the choice lists in parallel
  (as the tag list does); a failure of either shows the error. The type shows as the server's
  value (`choice-list: Genres`); readable type labels wait for the M8.4 type picker. The
  "Templates" action is a fifth top-bar action: see `docs/revisit.md` (M8.3).
- [ ] **M8.4** — Template form: create, edit, delete. The form edits the name and the field
  rows (name, type, choice list for `choice-list`; add, remove, move up and down). The form blocks
  a duplicate field name: the server takes one (plan §2, M8.2). The save sends
  only what changed, as the item edit form does. Delete asks first. The form survives rotation
  and process death. Ask the user at the start of the step which types the type picker offers
  (all of `DatumTypeEnum::TYPES`, or the ones the item form can fill).
  **Verify:** ViewModel and use-case tests (create, rename, add and remove and move a field,
  failure part way, delete); mutation-check one.
- [ ] **M8.5** — Choice lists: list, create, edit (name and choices: add, remove, reorder),
  delete with a confirm. Entry point: the template list and the choice-list picker in the M8.4
  form. `ChoiceList::$choices` is `Assert\Unique`: the form blocks a duplicate choice.
  **Verify:** ViewModel tests (create, edit, duplicate choice, delete, failure and retry).
- [ ] **M8.6** — Typed fields in the add and edit item forms. A text row carries its type (and
  its choice list for `choice-list`). Input by type: a number keyboard for `number` and
  `price`, a switch for `checkbox`, a multi-select from the choice list for `choice-list`, a text
  box for every other text type. The values go to the server in the M8.2 formats. "Add field"
  still adds a `text` row. The edit form shows a field the server already has with its own
  input. Ask the user at the start whether a free row gets a type picker too.
  **Verify:** ViewModel and use-case tests (each input type, the value sent per type, edit of an
  existing `checkbox` and `choice-list` field); mutation-check the `checkbox` value.
- [ ] **M8.7** — Apply a template in the add form: a template picker adds the template's fields
  as empty typed rows after the rows the form has. A label the form already has is not added
  twice. Ask the user at the start what an `image`, `sign`, `file`, `video`, `section` or
  `blank-line` field does (default proposal: `image` adds a photo slot with that label, the rest
  are skipped and the form says which).
  **Verify:** ViewModel tests (apply, apply twice, label clash, skipped types).
- [ ] **M8.8** — Collection default template. The collection form picks a template or none
  (`itemsDefaultTemplate`, `collection:write`). The add form in that collection applies it when
  it opens (once: not again after rotation or process death); the user can still remove the rows.
  **Verify:** ViewModel and use-case tests (set, clear, add form opens with the rows, no second
  apply after a saved-state restore).
- [ ] **M8.9** — Device pass on the emulator (`emulator-testing` skill) against the local test
  instance: create a choice list and a template, set it as a collection's default, add an item
  with it (photo, `checkbox`, `choice-list`), edit the item, then delete the template and the
  choice list. Compare the item with the web UI.
  **Verify:** each action works on the device; the item shows the same values in the web UI.

## M9 — wishlists and wishes (rough scope)

Full CRUD with images (user, 2026-11-17): wishlists (nested, as collections), wishes, and the
image upload of each (`/api/wishlists/{id}/image`, `/api/wishes/{id}/image`). Break into steps
at the start.

## M10 — loans (rough scope)

Full CRUD of loans on an item (user, 2026-11-17). Break into steps at the start.

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
