# koillectionmobile checklist

**Current step:** M2.7 (M2.6 done 2026-11-15). M1 done 2026-11-15, M0 done 2026-11-14 (both archived). Plan §5 decisions 1, 2, 4, 5 and 6 are settled.

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

## M2 — browse

Login screen, collection list, item list, item detail with images and fields, log out. Scope is
plan §5 decision 5 (settled 2026-11-15). Every screen is checked on the emulator against the
local test instance (`docs/local-info.md`); the emulator reaches it at `http://10.0.2.2:8290`.

Model: `../wallosmobile` `composeApp` (`WallosAppContent`, `LoginBranch`, `nav/MainNavHost`) and
`feature/setup/ui` (`LoginViewModel` + its `commonTest`). Navigation 3, the lifecycle
ViewModel libraries and Coil 3 (since M2.3) are in the catalog. ViewModel tests use hand-written
fake repositories.

- [x] **M2.1** — App shell. A "logged in" flow on `SessionStorage` (a stored token and
  credentials). The startup branch is state on that flow, not navigation (wallosmobile
  `WallosAppContent`, including its `rememberSaveable` seed). The logged-in side is a Navigation 3
  `NavDisplay` with keys for collection list, collection (id) and item (id), each a placeholder
  screen for now. The logged-out side is a placeholder login screen. App theme from
  `grappim-kit-uikit`. First device run: create `docs/EMULATOR_TESTING.md` (`/emulator-testing`).
  **Verify:** `build ktlintCheck` green, `KoinGraphTest` passes (`--rerun`); on the emulator a
  fresh install shows the login placeholder.
  Note: the routes and placeholder screens live in the feature `ui` modules (`CollectionListRoute`,
  `CollectionRoute`, `ItemRoute`, `LoginScreen`), as in wallosmobile. One plain
  `rememberNavBackStack`, no `grappim-kit-navigation`: there are no drawer sections. `isLoggedIn`
  needs a username, and a password and a token that decrypt. First Android launch of the app
  with the real Koin graph: no crash.
- [x] **M2.2** — Login screen (`feature:auth:ui`): server URL, username, password, one message per
  `LoginResult`. The 500 message names the JWT passphrase trap (plan §2). Success swaps to the
  logged-in side through M2.1's flow, not through a callback.
  **Verify:** ViewModel tests for each `LoginResult` (`jvmTest --rerun`); on the emulator a wrong
  password shows its message, and a correct login shows the collection list placeholder. This is
  the first Android HTTP request: plan §2 records whether Ktor's engine autodiscovery worked.
  Note: `LoginViewModel` (`@KoinViewModel`, `AuthUiModule` in `AppModule`) has no navigation
  callback. No pre-filled server URL yet: that is M2.6. A `Failed` with an HTTP status names the
  status; any other `Failed` gets a generic message. Autodiscovery worked: OkHttp (plan §2).
- [x] **M2.3** — Collection list (`feature:collections:ui`): top-level collections (no
  `parentId`), name and image, loading / error / empty states, pull to refresh. Add Coil 3 (as
  in wallosmobile's catalog). Resolve collection image URLs as `feature:items` does: this fixes
  the `docs/revisit.md` entry from M1.7.
  **Verify:** ViewModel and mapper tests (`jvmTest --rerun`); on the emulator the list shows the
  test instance's collections with their images.
  Note: the test instance's only collection (`Books`) has no image, so the device check added a
  temporary top-level collection with an image and a child under it, then deleted both. The image
  loaded, the child was filtered out, and a pull to refresh dropped the deleted one. A row without
  an image shows the title's first letter. Tapping a row opens `CollectionRoute` (needed by M2.4's
  Verify). Coil: `coil-compose` in `feature:collections:ui`, and `coil-network-ktor3` in
  `:composeApp`, found by service loader; uploads are public files, so images need no token.
- [x] **M2.4** — Collection screen: its child collections, then its items (name and small
  thumbnail). Tapping a child opens that collection; tapping an item opens the item placeholder.
  `ItemsRepository.getItems` reads every page before it returns (445 items = 15 requests on the
  test instance). Measure the wait on the emulator; if it is too slow, record it in
  `docs/revisit.md` rather than adding paging UI in this step.
  **Verify:** ViewModel tests (`jvmTest --rerun`); on the emulator `Books` shows all its items,
  and back returns to the collection list.
  Note: `CollectionViewModel` reads the collection, its children and its items in parallel; a
  load fills all three or none, with a retry button on error (no pull to refresh). The screen
  stays in `feature:collections:ui`, which now depends on `feature:items:domain`. Wait for `Books`
  on the emulator: under 2 s (one `uiautomator dump` poll); the host reads all 15 pages in 0.3 s.
  Not recorded in `docs/revisit.md`. The list ends with the server's last item (445 in total).
  Tapping a child collection was not checked on the device: the test instance has none.
- [x] **M2.5** — Item detail (`feature:items:ui`): main image (large thumbnail, full image on
  tap), fields in `position` order (text-like types as text, `image` fields as images), and the
  item's tags. A field type the screen does not know shows its label and a "not shown" mark, not
  a crash.
  **Verify:** ViewModel tests (`jvmTest --rerun`); on the emulator one book shows its fields,
  page images and tags, and they match the web UI for the same item.
  Note: `ItemViewModel` reads the item, its fields and its tags in parallel, all or none, with a
  retry button (as M2.4). `feature:items:ui` now depends on `feature:tags:domain`. Text types are
  `DatumTypeEnum::TEXT_TYPES` without `file`, shown raw (a `date` or `list` is not formatted).
  `image` and `sign` with an uploaded image show the large thumbnail. Everything else, and an image
  field without an image, shows "not shown". A tap on any image (main or field) opens the full
  image. Tags are sorted by label: the web UI does, the API does not (plan §2). Checked on
  "Ut mine Stromtid": 10 text fields, 2 page images, 3 tags, the same as the web page. The test
  instance has only `text` and `image` fields (4007 and 1577), so the other types were not seen on
  the device.
- [x] **M2.6** — Log out. A log-out action clears the session in `SessionStorage`, which returns
  the app to login through M2.1's flow. A failed re-login on a 401 (the stored password no longer
  works) also clears the session, so the user sees login and not an endless error.
  **Verify:** tests for both paths (`jvmTest --rerun`); on the emulator log out returns to login
  with empty fields except the server URL, and logging in again works.
  Note: "Log out" is a text button in the collection list's top bar. `CollectionListViewModel` calls
  `AuthRepository.logOut()` (`feature:collections:ui` now depends on `feature:auth:domain`), which
  calls the new `SessionStorage.clearSession()`: it removes the username, password and JWT and keeps
  the URL. The server has no API log-out route; the JWT stays valid until it expires. `BearerAuth`
  clears the session only on a 401 from its re-login; a 500, a network error or a second 401 after
  a good re-login keep it. The login screen fills in the stored URL, and a successful login clears
  the username too: the login ViewModel lives as long as the activity and is shown again after a
  log out. Fixed on the way: `MainNavHost` had no `ViewModelStore` per entry (dropped with
  `grappim-kit-navigation` in M2.1), and Koin keys a ViewModel by class only. So a second item showed
  the first item's data (A/B on the emulator: "Die Pappenheimer" showed "Jörn Uhl"'s author), and a
  re-login kept the old collection list. `NavDisplay` now has the saveable-state and ViewModel-store
  decorators. Device: a re-login showed the list without a collection deleted while logged out. The
  401 path was not checked on the device, only in `BearerAuthTest`.
- [ ] **M2.7** — Device pass of the whole browse flow on the emulator: login, collection, item,
  back to the top, rotation and process death (back stack kept), log out. Fix what breaks, or put
  it in `docs/revisit.md` with evidence.
  **Verify:** the flow passes on the emulator; screenshots go to the user, and the user confirms.

## M3 — add item with photos (rough scope)

Pick a collection, take or pick photos (main image + page images), text fields, tags.
Matches the book workflow in plan §1.

## M4 — release (rough scope)

Signing, F-Droid/IzzyOnDroid and/or Play per plan §5 decision 2.
