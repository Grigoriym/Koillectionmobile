# koillectionmobile checklist

**Current step:** M14.13. M14.12 done 2026-11-26. M14.11 done 2026-11-26. M14.10 done 2026-11-26. M14.9 done 2026-11-25. M14.8 done 2026-11-25. M14.1 done 2026-11-25. M13 done 2026-11-25. M13.4 done 2026-11-25. M13.3 done 2026-11-25. M13.2 done 2026-11-25. M13.1 done 2026-11-25. M13 broken into steps 2026-11-25. M12 done 2026-11-25. M12.5 done 2026-11-25. M12.4 done 2026-11-25. M12.3 done 2026-11-25. M12.2 done 2026-11-25. M12.1 done 2026-11-25. M12 broken into steps 2026-11-25. The user's UX notes from checking the app become steps in the UX pass below when they send them; the final review of the whole app comes after M12 and M13. M11.15 done 2026-11-25. M11.14 done 2026-11-24. M11.13 done 2026-11-24. M11.12 done 2026-11-24. M11.11 done 2026-11-23. M11.10 done 2026-11-23. M11.9 done 2026-11-23. M11 done 2026-11-23. M11.8 done 2026-11-23. M11.7 done 2026-11-23. M11.6 done 2026-11-23. M11.5 done 2026-11-23. M11.4 done 2026-11-23. M11.3 done 2026-11-23. M11.2 done 2026-11-23. M11.1 done 2026-11-23. M11 broken into steps 2026-11-23. M10 done 2026-11-23. M10.6 done 2026-11-23. M10.5 done 2026-11-22. M10.4 done 2026-11-22. M10.3 done 2026-11-22. M10.2 done 2026-11-22. M10.1 done 2026-11-22. M10 broken into steps 2026-11-22. M9 done 2026-11-22. M9.8 done 2026-11-22. M9.7 done 2026-11-22. M9.6 done 2026-11-22. M9.5 done 2026-11-21. M9.4 done 2026-11-21. M9.3 done 2026-11-21. M9.2 done 2026-11-21. M9.1 done 2026-11-21. M9 broken into steps 2026-11-19. M8 done 2026-11-19. M8.9 done 2026-11-19. M8.8 done 2026-11-19. M8.7 done 2026-11-19. M8.6 done 2026-11-18. M8.5 done 2026-11-18. M8.4 done 2026-11-18. M8.3 done 2026-11-17. M8.2 done 2026-11-17. M8.1 done 2026-11-17. M8 broken into steps 2026-11-17; the other areas became M9–M13 and the release moved to M14 (user, 2026-11-17). M7 done 2026-11-17. M7.7 done 2026-11-17. M7.6 done 2026-11-17. M7.5 done 2026-11-17. M7.4 done 2026-11-17. M7.3 done 2026-11-17. M7.2 done 2026-11-17. M7.1 done 2026-11-17. M7 broken into steps 2026-11-17. M6 done 2026-11-17. M6.6 done 2026-11-17. M6.5 done 2026-11-16. M6.4 done 2026-11-16. M6.3 done 2026-11-16. M6.2 done 2026-11-16. M6.1 done 2026-11-16. M6 broken into steps 2026-11-16. M5 done 2026-11-16. M5.7 done 2026-11-16. M5.6 done 2026-11-16. M5.5 done 2026-11-16. M5.4 done 2026-11-16. M5.3 done 2026-11-16. M5.2 done 2026-11-16. M5.1 done 2026-11-16. M5 broken into steps 2026-11-16. M4 done 2026-11-16. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 and 16 are settled.

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
M13 (logs) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## UX pass (from the user's check of the app, before M12)

Usability fixes the user asks for while checking the app (user, 2026-11-25). Each note gets a step.

- [x] **M11.15** — Tag list: each category is a section that opens and closes on a tap (option 4
  of four the user was shown). The header shows the colour, the label, the tag count and an arrow,
  with a divider between groups. All groups start closed; a lone group starts open. The open
  groups stay in `TagListViewModel`, so they last while the user visits a tag's items.
  **Verify:** `TagListViewModelTest` covers the start state, toggling and the reset on reload; on
  the emulator a group opens and closes, and stays open after a tag's items and Back.
  Note: done 2026-11-25. A mutation (toggle never closes) failed the toggle test.

## M14 — release

Runs after M4–M13 (user, 2026-11-16; renumbered from M9 on 2026-11-17). Both store flavors ship, as in the user's other apps (user, 2026-11-16): `fdroid` to
IzzyOnDroid first, `gplay` to Google Play. Model: `../Wallosmobile` (`build-logic`
`configureAppSigningConfigs`, `androidApp/build.gradle.kts` dependency-info block,
`.github/workflows/release*.yml`, `fastlane/metadata/`). No Firebase: wallosmobile's
`google-services.json` steps are not ported. Two inputs come from the user later: the launcher
icon (M14.4) and the release keystores (M14.5). Those steps wait for them; the others do not.

- [x] **M14.1** — Release blockers. Check the subpath question read only, from the user's server
  URL (`docs/revisit.md`, M1.9 entry). Go through every `docs/revisit.md` entry with the user;
  each one is "fix before release" (gets a step here) or "after release".
  **Verify:** the M1.9 entry is closed or has a recorded answer; each revisit entry has the
  user's verdict, written in the entry.
  Note: done 2026-11-25. The user's server is served by `tailscale serve` at path `/` on its own
  port: no subpath. The user put every entry before release. The fixes are M14.8–M14.26 below;
  they run after M14.1 and before M14.2.
  `ACCESS_NETWORK_STATE` went into M14.2.
- [ ] **M14.2** — R8 release build. `assembleFdroidRelease` and `assembleGplayRelease` build with
  minify and resource shrinking on (never run so far; `androidApp/proguard-rules.pro` is empty).
  Sign the `fdroidRelease` APK by hand with the debug key (`apksigner`, no build change) and run
  login, browse and add item on the emulator against the local test instance. Add a keep rule
  only for what R8 breaks. Read the merged release manifest: remove `ACCESS_NETWORK_STATE` if no
  library needs it (`docs/revisit.md`, M4.5).
  **Verify:** both release tasks pass; the hand-signed APK passes the flow on the emulator; the
  merged manifest's permissions are each explained in the Note.
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

### Release blockers (from M14.1)

The user's verdicts on `docs/revisit.md` (2026-11-25). Run these in order after M14.1 and before
M14.2. Each step deletes its revisit entry (or its part of a shared entry) when it is done. The
shared code comes first, so the fixes after it change one copy, not three.

- [x] **M14.8** — Koin `includes` test (`docs/revisit.md`, M4.2). A `jvmTest` that finds every
  `@Configuration` module class and checks that `AppModule`'s `includes`
  (`composeApp/.../di/Koin.kt`) names it.
  **Verify:** the test passes; a mutation (one `includes` line removed) fails it.
  Note: done 2026-11-25. `AppModuleIncludesTest` scans the test classpath for
  `com.grappim.koillectionmobile` classes with `@Configuration` (runtime retention). Removing
  `SettingsUiModule` from `includes` failed it and named the class.
- [x] **M14.9** — One `FakeSessionStorage` (revisit M1.6). One copy in a shared test place that the
  `data` modules take as a test dependency; delete the five copies.
  **Verify:** `build` and `ktlintCheck` pass; `grep -rn "class FakeSessionStorage"` finds one.
  Note: done 2026-11-25. There were 11 copies, not five. The one copy is in the new module
  `core:testing`. Its `serverUrl` default is `""`; the three `core:api` tests that used the old
  `/sub` default now pass it.
- [x] **M14.10** — One error mapping and one error view (revisit M2.5, first part). One
  `KoillectionError` → message function and one `EmptyOrError` in a shared UI module, used by
  every `ui` module that has its own copy now.
  **Verify:** `build` and `ktlintCheck` pass; ViewModel tests that check messages still pass; a
  grep finds no other `toMessage` or `EmptyOrError` copy; on the emulator the network error and
  "Try again" still show on the collection list and the tag list (airplane mode).
  Note: done 2026-11-26. New module `core:ui`. The nine copies shared one text, so the
  `<area>_error_network`, `<area>_error_http` and `<area>_retry` strings became `error_network`,
  `error_http` and `retry` (`loans_retry` stays: forms use it). The shared view always scrolls,
  as the collection list's copy did. The item screen's inline error uses it too.
- [x] **M14.11** — One name-and-image form and one nested-delete (revisit M2.5, second part). Move
  `FormPhoto`, the save status, the form scaffold, `FormImageSection`, `toSaveMessage`,
  `descendantIds`, `DeleteSummary` and `DeleteDialogs` to a shared module. Collections,
  wishlists and albums use them.
  **Verify:** `build` and `ktlintCheck` pass; the moved tests pass; on the emulator a new and an
  edited collection, wishlist and album save, and a nested delete shows its counts.
  Note: done 2026-11-26. New module `core:form` for the form parts (it needs `core:photos`, coil
  and serialization); the nested delete went to `core:ui`. `DeleteSummary` has one `contentCount`,
  and `descendantIds` takes `(id, parentId)` pairs. `CollectionForm` now uses `FormImageSection`.
  New tests: `DeleteSummaryTest`, `FormSaveMessageTest`; a mutation (no deeper levels) failed
  `DeleteSummaryTest`. On the emulator, new and edited collection, wishlist and album saved with an
  image; the deletes showed 1 collection and 2 items, 2 wishlists and 3 wishes, 1 album and 3 photos.
- [x] **M14.12** — Subpath check (revisit M1.9). Run the proxy front (`docs/local-info.md`) with a
  location `/sub/` in front of the test instance. Log in at `http://10.0.2.2:<port>/sub`, browse,
  add an item with a tag and an image, edit it, delete it. Fix only what fails.
  **Verify:** the flow passes on the emulator; the front's access log shows every request under
  `/sub/`; the M1.9 entry is closed.
  Note: done 2026-11-26, no app change needed. The front also sends `X-Forwarded-Prefix`, and the
  instance ran with trusted-proxy env for the check: without it Koillection ignores the prefix
  (plan §2, "Subpath"). The M1.9 entry is deleted.
- [ ] **M14.13** — Images get the custom headers (revisit M4.5). A singleton `ImageLoader` whose
  fetcher uses a client with the `CustomHeaders` plugin, for the server origin only.
  **Verify:** a test that the image client sends the header to the server origin and not to another
  host; on the emulator, behind the proxy front's header port, the thumbnails and item images load.
- [ ] **M14.14** — Clear the image caches on log out (revisit M4.5, MASVS-PRIVACY-4).
  **Verify:** a test that log out clears both caches; on the emulator the app's `cache/` holds no
  image file after log out; update `docs/security/masvs.md`.
- [ ] **M14.15** — Typed values on the item screen (revisit M8.6). A `checkbox` shows yes or no
  (or a mark), a `choice-list` shows the picks joined by commas.
  **Verify:** mapper or UI-model tests for `1`, `0`, an empty and a filled choice list; on the
  emulator the seed's comic items show them, the same as the web item page.
- [ ] **M14.16** — Loans section keeps its list during a reload (revisit M10.6).
  **Verify:** a ViewModel test that the old list stays while loading; on the emulator the changed
  loan stays on screen after save, mark returned and delete.
- [ ] **M14.17** — A delete that gets 404 counts as done (revisit M5.3). Item delete first; apply
  the same to the other deletes that share the code path.
  **Verify:** ViewModel tests for a 404 and another error; mutation-check one.
- [ ] **M14.18** — A lost create answer does not make a second object (revisit M3.6 and M8.4 (2)).
  Item, collection, template and choice list. Agree the approach with the user first (look the
  object up by name before a retry, or ask the user).
  **Verify:** use-case or ViewModel tests with a create that times out after the server saved it;
  a retry makes no second object; mutation-check one.
- [ ] **M14.19** — Edit save edge cases (revisit M5.6 (1) and (2)): `retry` diffs against the server
  as reloaded, with a fresh `original`, once the form changes after a failure.
  **Verify:** use-case tests for both cases; mutation-check one.
- [ ] **M14.20** — Template field leaves its choice list (revisit M8.4 (1)). The patch sends
  `choiceList: null` on purpose when a field stops being `choice-list`.
  **Verify:** a DTO test that the null is in the JSON; against the local instance the field's
  `choiceList` is null after the save.
- [ ] **M14.21** — Orphan photo files (revisit M3.6). On app start, delete files in `cache/photos/`
  that no saved draft names.
  **Verify:** a test with a named and an orphan file; on the emulator an orphan file left by a
  swiped-away form is gone after the next start.
- [ ] **M14.22** — Local network permission UX (revisit M4.1, wallosmobile's limits). Screens
  reload after a grant; the denied message at Log in has an "Open settings" action.
  **Verify:** ViewModel tests for the reload; the device check is part of M14.23.
- [ ] **M14.23** — API 37 device check (revisit M4.1). Get a working API 37 AVD (the canary
  emulator, or another image; `docs/machine.local.md`) or ask the user for an Android 17 phone.
  Run the open checks from the revisit entry.
  **Verify:** each check in the entry passes on API 37, or the failure becomes a step.
- [ ] **M14.24** — Build noise (revisit M0.2, M0.3, M0.6). Bump `koin-plugin` if a release lists
  Kotlin 2.4.21 as verified (else record that none does); fix the `compileOnly` compose-runtime
  warning on the iOS targets; bump `actions/upload-artifact` to the major that targets Node 24.
  **Verify:** `build` prints neither warning; a CI run is green with no Node 20 warning.
- [ ] **M14.25** — Stable seed ticks (revisit M12.5). `fixtures/seed.py` reads the item order from
  `GET /api/inventories/{id}` and ticks only the last items in that order.
  **Verify:** two `--clean` + seed runs store the same tick counts.
- [ ] **M14.26** — Collection header image (revisit M6.6). The collection screen shows the
  collection's own image at the top, when it has one.
  **Verify:** on the emulator a collection with an image shows it, one without shows no empty
  space, and an edit that sets, replaces or removes the image shows the change on that screen at
  once.
