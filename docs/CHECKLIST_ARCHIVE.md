# koillectionmobile checklist archive

Finished milestones, moved here verbatim from `docs/CHECKLIST.md`.

## M0 — repo and module scaffolding

Model: deskmate's and wayprint's archived M0.

- [x] **M0.1** — `git init` (branch `master`). Port `../wallosmobile/build-logic`
  (`wallosmobile.*` → `koillectionmobile.*`, package `com.grappim.koillectionmobile.buildlogic`),
  keeping `KmpNetworkConventionPlugin`. Keep or drop targets and store flavors per plan §5
  decisions 1-2. Apply deskmate's M0.1 fixes for the hardcoded `:core:logger` / `:testing` /
  `:detekt-rules` injections (logger and testing come from `grappim-kit`). Root
  `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.editorconfig`,
  `.gitignore`, Gradle wrapper, LICENSE. Don't carry over suppressions; re-add one only for a
  real finding.
  **Verify:** `./gradlew :build-logic:convention:build` and `./gradlew help` succeed.
  **Note:** copied from wallosmobile (newer than deskmate's copy), then deskmate's `Quality.kt`
  fixes applied: `grappim-kit-testing` from the catalog, no `detekt-rules` plugin. Catalog is
  deskmate's trimmed list at wallosmobile's versions (`grappimKit = "0.1.8"`, on Maven Central),
  no datastore/glance/work, plus `ktor-client-darwin`. Wrapper is Gradle 9.9.1 (wallosmobile).
  **Note:** decision 1 is in `configureKmp()`: `jvm()`, `iosArm64()`, `iosSimulatorArm64()`.
  So the `withHostTestBuilder` block deskmate needed is gone; `commonTest` runs as `jvmTest`.
  The network plugin adds OkHttp to `jvmMain` and Darwin to `iosMain`, as Taiga does.
  **Note:** decision 2: `AppFlavors.kt` and the `store` dimension are kept, but wallosmobile's
  signing configs (its keystores, `WALLOS_*` env vars, the `fdroidDebug` variant hook) are
  dropped. Release signing comes in M4.
  **Note:** M0.2 still needs the three files deskmate's M0.1 note listed: root
  `build.gradle.kts`, `config/detekt/detekt.yml`, `config/compose/stability_config.conf`.
  `help` prints one Gradle 11 deprecation (`Configuration.setVisible`) from a third-party plugin
  in `build-logic`, not from our code.
  Confirmed: both green; the jar declares all seven `koillectionmobile.*` plugin ids.
- [x] **M0.2** — `androidApp` + `composeApp` skeletons with a placeholder screen.
  `applicationId com.grappim.koillectionmobile`. Root `build.gradle.kts`,
  `config/detekt/detekt.yml`, `config/compose/stability_config.conf` (deskmate M0.1 note).
  **Verify:** the debug APK builds and shows the placeholder on the emulator.
  **Note:** shape is deskmate's M0.2 (`KoillectionAppContent` "Koillection" text screen,
  manifest `INTERNET`, `ACCESS_NETWORK_STATE`, `usesCleartextTraffic="true"` for LAN servers).
  Root `build.gradle.kts` and both config files are deskmate's current copies; its `detekt.yml`
  already has the four Compose rules re-enabled, so no suppressions came along. No launcher
  icon yet. `local.properties` (gitignored) is needed locally for `sdk.dir`.
  **Note:** the `store` flavors mean the APK tasks are `assembleGplayDebug` /
  `assembleFdroidDebug` (ids `com.grappim.koillectionmobile.debug` / `.fdroid.debug`).
  **Note:** emulator half of Verify skipped by the user's call: no emulator runs for
  scaffolding-sized steps, only for real screens and flows (M2 on).
  Confirmed: both debug APKs build; `ktlintCheck`, `detekt`, `:composeApp:build` green.
- [x] **M0.3** — Module skeletons per plan §5 decision 4, wired into `settings.gradle.kts`.
  grappim-kit modules under one `grappimKit` key, after reading `grappim-kit/CONSUMING.md`.
  **Verify:** `./gradlew build` succeeds.
  **Note:** decision 4 settled by the user: wallosmobile-style full split (plan §5). 23 modules,
  each with one `internal object Placeholder`. Plugins: `dto` library + serialization; `domain`
  library + stability; `mapper` library + di (`api` dto + domain); `data` library + di + network;
  `ui` library + compose + di; `core:api` library + di + network + serialization; `core:domain`
  library + stability; `strings` library + compose (one `app_name` string).
  **Note:** catalog gains kit `coroutines`, `domain`, `storage`, `customheaders` (0.1.8, all 200
  on Maven Central). Wired so the build proves they resolve: `domain` as `api` in `core:domain`,
  the other three in `core:api`; `uikit` in every `feature:*:ui`. No `core:storage` yet; M1
  decides whether token storage needs its own module.
  Confirmed: `./gradlew build` green.
- [x] **M0.4** — Koin skeleton, `AppModule`, a `KoinGraphTest`.
  **Verify:** the test runs (forced with `--rerun`) and passes.
  **Note:** deskmate's shape: `composeApp/.../di/Koin.kt` (`AppModule` = `@Module @Configuration
  @ComponentScan`, empty `includes` until features define modules; `@KoinApplication object
  KoinApp`), `KoillectionApp : Application` calls `startKoin<KoinApp>` with `androidContext`.
  No injected-greeting proof on screen (not asked; no emulator for scaffolding).
  **Note:** `KoinGraphTest` is in `composeApp/src/jvmTest`, not `commonTest`: this project has
  `jvm` + iOS targets, and koin-test's `verify()` is JVM-only. `koin-test` is a `jvmTest` dep.
  **Note:** `ktlintCheck` failed on M0.3's trailing commas in `settings.gradle.kts`; fixed here.
  `build` never runs the root project's `:ktlintKotlinScriptCheck` (checked with `--dry-run`),
  so `settings.gradle.kts` and root `build.gradle.kts` are only linted by `ktlintCheck`. Run it
  alongside `build` from now on.
  Confirmed: `jvmTest --rerun` ran 1 test, 0 failures; a temporary `@Single class Broken(val
  missing: Missing)` failed it with `MissingKoinDefinitionException`, then was removed.
  `build ktlintCheck` green.
- [x] **M0.5** — Merge `../agentic-grappim/templates/CLAUDE.md.template` into root `CLAUDE.md`,
  keeping this project's facts.
  **Note:** shape is deskmate's merged file. Project parts: API contract (Koillection source
  wins), checklist workflow, commit format (`M<n>.<m>: …` + `docs: … finalize`), two Gradle
  verification rules (`ktlintCheck` with `build`, `--rerun` for tests), settled-decisions table
  built only from plan §5 and the ground rules. Reference projects point to plan §4.
  Confirmed: no `<PLACEHOLDER>` left (grep); M0.5 has no Verify line.
- [x] **M0.6** — CI workflow (port deskmate's). It must call `ktlintCheck` itself: `build`
  skips the root `:ktlintKotlinScriptCheck` (M0.4 note).
  **Verify:** CI green on the pushed commit.
  **Note:** `.github/workflows/ci.yml` is deskmate's file with two changes: `setup-java@v6`
  (wallosmobile's version) and a comment on why `ktlintCheck` stays. `:androidApp:assembleDebug`
  builds both store flavors. No signing secrets needed yet; Codecov upload not ported.
  Confirmed: same task set green locally with `--rerun-tasks`; CI run 43643554439 green (4m46s).

## M1 — API layer

Ktor client, server URL + login (JWT), token storage (grappim-kit `storage`), Hydra list
paging, DTOs for Collection / Item / Datum / Tag, multipart image upload. Fixture-tested
against real JSON captured from the local test instance (`docs/local-info.md`). No screens: those are M2.

Layout is plan §5 decision 4: shared plumbing in `core:api`, each resource in its
`feature:<x>:{dto,domain,mapper,data}`. Datum (an item's field) lives in `feature:items`.
Model for every step: `../wallosmobile/core/api` and its `feature:*:data` modules.

- [x] **M1.1** — Capture fixtures and settle two open questions. Fetch the server's OpenAPI
  doc and real responses for: one page of `/api/collections`, one collection, one page of
  `/api/collections/{id}/items`, one item, `/api/items/{id}/data`, one page of `/api/tags`,
  `/api/tag_categories`, and the login error bodies (wrong password → 401). Use `koi.py`
  (`~/claude/books/00service/`), GET only. Decide hand-written vs generated DTOs (plan §2) and
  answer plan §5 decision 3 (does the Tailscale cert validate without `koi.py`'s check-off?).
  **The GitHub repo is public:** ask the user how to trim or anonymise their data before any
  fixture is committed. Never commit a token.
  **Verify:** fixtures are in the repo, `grep` finds no JWT (`eyJ`) in them, and plan §2 / §5.3
  record both answers.
  Note: user asked for a test instance instead of capturing from the real server. A local
  Koillection 1.8.5 copy of the real catalogue (`docs/local-info.md`) is the source of the
  fixtures (`fixtures/koillection-1.8.5/`, script `fixtures/capture.py`), so no real ID is
  in the repo. Full responses kept, not trimmed.
- [x] **M1.2** — `core:api` client: Ktor `HttpClient` factory (wallosmobile `NetworkModule`),
  base-URL provider, `Json` (`ignoreUnknownKeys`, `application/ld+json` and
  `application/merge-patch+json`), redacting logger, custom headers (grappim-kit
  `customheaders`), and one error type that maps HTTP status and network failure. Koin module.
  **Verify:** `ktor-client-mock` tests pass (`jvmTest --rerun`), `KoinGraphTest` passes,
  `build ktlintCheck` green.
  Note: `configureKoillectionClient` (`HttpClientConfig` extension) holds the setup; the Koin
  provider applies it on Ktor's default engine, tests on `MockEngine`. Error type is
  `KoillectionError` in `core:domain`. `BaseUrlProvider` and `CustomHeadersStorage` have no
  binding yet: both need M1.3's storage. `KoinGraphTest` cannot see that gap (it reads
  `HttpClient`'s constructor, so `HttpClientEngine` is in its `extraTypes`, as in wallosmobile).
  Not run on a device: Ktor's engine autodiscovery on Android is first exercised in M1.11/M2.
- [x] **M1.3** — Session storage and bearer auth. Store server URL, username, password and JWT
  with grappim-kit `storage`; decide `core:api` vs a new `core:storage` module (M0.3 note) and
  write the answer in plan §5. Bind `core:api`'s `BaseUrlProvider` and grappim-kit
  `CustomHeadersStorage` (M1.2 left both unbound). Add `Authorization: Bearer`; on 401 log in again once with the
  stored password, then fail.
  **Verify:** tests prove the header is sent, one re-login on 401, and no loop on a second
  401. Hand-written fakes for storage.
  Note: new module `core:storage` (plan §5.6). `bearerAuth` (`core:api`) skips the login path;
  a failed re-login throws its own error, so a 500 there stays `Server` (plan §2 trap).
  Mutation-checked: a dropped header and a re-login loop each fail a `BearerAuthTest` case.
  `KoinGraphTest`'s `verify()` passed with `CustomHeadersStorage` unbound, so a second test
  now starts the JVM `KoinApp` and resolves `HttpClient`; it fails on that gap. Removing
  `PlatformStorageModule` from `AppModule.includes` did not fail it: the module's own
  `@Configuration` is found anyway on JVM. Android graph not checked (no device run).
- [x] **M1.4** — Hydra paging in `core:api`: a generic list DTO that reads `member` and
  `hydra:member`, `totalItems`, and the next-page link; a helper that follows pages.
  **Verify:** tests against the M1.1 list fixtures, including the last page (no next link).
  Note: `HydraCollectionDto<T>` and `HttpClient.getAllHydraPages` (`HydraCollection.kt`). The
  `next` link is requested as it is (an absolute path with the server's own prefix). Fixture
  tests are in `jvmTest` and read `../../fixtures/` from the module directory: `commonTest` has
  no file API. Mutation-checked one change at a time (`failFast` stops at the first failure).
- [x] **M1.5** — `feature:auth` dto/domain/mapper/data: `POST /api/authentication_token`,
  server-URL check, and distinct results for wrong credentials (401), server JWT failure (500,
  plan §2 trap) and unreachable server.
  **Verify:** mock-engine tests for each result; `KoinGraphTest` passes.
  Note: `AuthRepository.logIn` returns `LoginResult`. The server-URL check is a text check
  (`http(s)://` and a host) plus the login answer itself (404, 405 or no `token` →
  `NotKoillection`); no extra probe request, because it would carry the stored JWT to the typed
  server. The URL is saved before the request (custom headers go only to the stored origin) and
  put back on failure. `mapper` stays a placeholder: the token is a string, nothing to map.
  `LOGIN_PATH` (`core:api`) is now public. Mutation-checked: no URL restore, no 500 mapping and
  no `@Single` each fail a test.
- [x] **M1.6** — `feature:collections` dto/domain/mapper/data: list (paged), get one,
  children.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `CollectionsRepository` returns `Result` (`resultOf`, as in wallosmobile); the failure
  is the `KoillectionError`. `getCollections` returns every level: the server has no `parent`
  filter, so the caller keeps the ones without `parentId`. `imagePath` stays relative
  (`uploads/...`), not resolved. The fixtures hold one collection with no children, so paging
  and a set `parent` are tested with inline bodies. Mutation-checked: no `@Single`, a wrong
  children path and an unparsed parent IRI each fail a test.
- [x] **M1.7** — `feature:items` read side: items of a collection (paged), get one item, the
  item's data (Datum) list, image URLs resolved against the server URL.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `ItemsRepository` returns `Result`, as in M1.6. `ItemMapper.toDomain(dto, serverUrl)`
  resolves image and thumbnail paths to full URLs; the repository passes the stored URL through
  `normalizeBaseUrl` (`core:api`, now public). `getData` sorts by `position`: the server does not promise
  that order (plan §2). `Datum.type` stays the server's string. Mutation-checked: no `@Single`, no
  sort, no server URL in the image URL and an unnormalised server URL each fail a test.
- [x] **M1.8** — `feature:tags` dto/domain/mapper/data: list tags (paged), tag categories,
  an item's tags.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `TagsRepository` returns `Result`, as in M1.6. `Tag.categoryId` comes from the
  `category` IRI. The tag image fields are not read: no fixture has one and no step needs them
  yet. The tags fixture is one full page (30 of 30), so paging is tested with an inline body.
  Mutation-checked: no `@Single`, a wrong item-tags path, an unparsed category IRI and a
  dropped second page each fail a test.
- [x] **M1.9** — Write side: create item, create Datum (text and image type), set an item's
  tags (full list, `merge-patch+json`, plan §2). Request shapes follow `add_book.py`, checked
  against the entity `write` groups in the Koillection source.
  **Verify:** mock-engine tests assert method, path, content type and body for each call.
  Note: `ItemsRepository` gains `createItem`, `createTextDatum` and `createImageDatum`;
  `TagsRepository` gains `setItemTags` (`PATCH /api/items/{id}`, tag IRIs). Bodies name other
  resources by `/api/...` IRI without the server's subpath, as `add_book.py` does. An image field
  is sent without a `value` key. Not sent to a server: writes are checked on device in M3.
  Mutation-checked: a bare collection id, ld+json on the PATCH, `"value":null` on an image field
  and a wrong datum path each fail a test.
- [x] **M1.10** — Multipart upload: `POST /api/items/{id}/image` (field `file`) and
  `POST /api/data/{id}/image` (field `fileImage`). Bytes in, no platform file APIs in
  `commonMain`.
  **Verify:** mock-engine tests assert the multipart parts, field names and content type.
  M1.3 note: `bearerAuth` sends a request again after a 401 and a new login. Check that a
  multipart body is still complete on that second send.
  Note: `ItemsRepository` gains `uploadItemImage` and `uploadDatumImage` (bytes, file name, MIME
  type; `Result` of the updated `Item` / `Datum`). Ktor `submitFormWithBinaryData` with a
  `ByteArray` part reads the bytes again on each send, so the repeat after a 401 is complete.
  The tests parse the multipart body by hand. Not sent to a server: checked on device in M3.
  Mutation-checked: a wrong field name, no part `Content-Type` and a one-shot channel body each
  fail a test; only the 401 test catches the one-shot body.
- [x] **M1.11** — Live read-only smoke against the local test instance: a JVM run (not part of
  `build` or CI) that logs in and reads one collection, its items and one item's data through
  the real repositories. No writes; writes get checked on device in M3.
  **Verify:** the run prints real names from the test instance; the user confirms them.
  Note: `main` in `composeApp/src/jvmTest/.../smoke/LiveSmoke.kt`, run by
  `./gradlew :composeApp:smokeRun` (not wired into `build` or `check`). The task points `user.home`
  at `composeApp/build/smoke-home`, so the session stays out of `~/.koillectionmobile`. It printed
  collection `Books` with 445 items (the server's `totalItems` is 445, so every page was read) and
  the first item's 23 fields; the user confirmed the names on 2026-11-15.

## M2 — browse

Login screen, collection list, item list, item detail with images and fields, log out, a
top-bar back arrow (M2.8, user 2026-11-15). Scope is
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
- [x] **M2.7** — Device pass of the whole browse flow on the emulator: login, collection, item,
  back to the top, rotation and process death (back stack kept), log out. Open two different
  items in a row: before M2.6 the second showed the first one's data, and M2.5's device check
  opened only one. Fix what breaks, or put
  it in `docs/revisit.md` with evidence.
  **Verify:** the flow passes on the emulator; screenshots go to the user, and the user confirms.
  Note: nothing broke, so no code change and no `docs/revisit.md` entry. "Jörn Uhl" then
  "Die Pappenheimer" showed their own authors. Landscape on an item kept the item and the back
  stack. `am kill` on "Ut mine Stromtid" restored the item, then back went to `Books` and
  Collections. Log out left only the URL; a re-login worked. The user saw the screenshots and
  asked for a top-bar back arrow: that is M2.8.
- [x] **M2.8** — Back arrow. The collection screen and the item screen get a back arrow as the
  `TopAppBar` navigation icon. A tap does the same as system back (removes the top back stack
  entry). The collection list is the top screen and gets no arrow.
  **Verify:** `build ktlintCheck` green; on the emulator the arrow goes item → collection →
  collection list, and the collection list shows no arrow.
  Note: `CollectionScreen` and `ItemScreen` take an `onBack`; `MainNavHost` passes
  `backStack.removeLastOrNull()`, the same call as `NavDisplay`'s `onBack`. The icon is
  `Icons.AutoMirrored.Filled.ArrowBack` with the content description "Back" (`collection_back`,
  `item_back`). Device: "Ut mine Stromtid" → `Books` → Collections by the arrow; the list's dump
  has no "Back" node.

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
- [x] **M3.6** — Save: run M3.3 from the form with a progress view, a retry on failure, and on
  success open the new item (the add screen leaves the back stack) and refresh the collection.
  An upload 500 names the disk quota as a likely cause (plan §2). Delete the photo files in
  `cacheDir/photos/` (M3.5) after a successful save, and when a photo is removed or the form is
  left (`docs/revisit.md`).
  **Verify:** ViewModel tests; on the emulator an item with two text fields, a main image, two
  page images and two tags is created on the local test instance, and the web UI shows the same
  item, upright and in order. A save with the server stopped halfway, then retried, gives one
  item, not two.
  Note: `AddItemUseCase` and a new `ItemListChanges` (the refresh signal, `feature:items:domain`)
  are bound in `ItemsDataModule`. `PhotoFiles` (prepare + delete) is bound per platform by
  `PlatformPhotoModule` (`feature:items:ui`, expect/actual as `PlatformStorageModule`); the
  ViewModel takes the collection id by `@InjectedParam`. `AddItemProgress` now carries the
  `AddItemState`, so a save cut by process death resumes from saved state. Save is off while the
  name or a label is blank, or a label is duplicate. "Server stopped halfway" was done with the
  test user's quota at 1 byte: the item and text fields went through, the main-image upload got
  HTTP 500 and the quota message; after `am kill`, quota back and "Try again", the same item got
  the rest (one item, not two). The web UI showed it upright and in order; test items deleted.
- [x] **M3.7** — Device pass of the whole add flow, then the user confirms with screenshots. Fix
  what breaks, or put it in `docs/revisit.md` with evidence. The subpath check (`docs/revisit.md`,
  M1.9 entry) is deferred: the user's server likely has none, and they chose to check it later
  (user, 2026-11-15).
  **Verify:** the flow passes on the emulator; the user confirms.
  Note: the pass (camera main image, two picked page images with one rotated, two text fields,
  two tags, `am kill` mid-form, save) gave one item, in order and upright, in the API and the web
  UI; back went to `Books` with the item last. Found: back on a filled form left at once and lost
  it. Fixed with the user's OK: a "Discard this item?" dialog (`AddItemUiState.hasInput`,
  `NavigationBackHandler` from navigationevent-compose, already on the classpath). Test item
  deleted.

## M4 — network and security parity with wallosmobile

The user chose all four gaps found against `../Wallosmobile` (user, 2026-11-16). Model: wallosmobile
plan §4.5b (custom headers, proxy rejection), §4.5c (local network permission) and its cleartext
warning (plan §1.1, "Warn on non-HTTPS base URLs"). Not in scope: self-signed certificate trust
(TOFU). Plan §5 decision 3 still holds.

- [x] **M4.1** — Local network permission. Port wallosmobile 28.10: `ACCESS_LOCAL_NETWORK` in the
  manifest, `rememberLocalNetworkPermissionRequest` (`expect`/`actual`, from wallosmobile `uikit`
  `permission/`), asked at Log in only for a private, link-local or `fc00::/7` address on API 37+,
  a "denied" login message, and `LocalNetworkBanner` above `MainNavHost`. There is no local
  `uikit` (settled decisions): put the request in `feature:auth:ui` and the banner in `composeApp`
  (user, 2026-11-16: here for now; a move to `grappim-kit-uikit` may come later). Create an
  API 37 AVD (`system-images;android-37.0;google_apis;x86_64`) and record it in
  `docs/machine.local.md`.
  **Verify:** unit tests for the address check; on the API 37 AVD, against the test instance at
  the host's LAN address (not `10.0.2.2`): denied gives the message and sends no request, allowed
  logs in, and a revoke while logged in shows the banner, which leaves after a grant. The API 36
  AVD logs in with no prompt.
  Note (2026-11-16): unit tests (`LocalNetworkAddressTest`, `jvmTest`, on the JVM's parsed bytes;
  the address check is common code over `ByteArray`), `build` and `ktlintCheck` passed. **The
  device checks did not run.** The `koil37` AVD crashes in `surfaceflinger` on emulator 37.2.13
  (`docs/machine.local.md`); the user chose to skip the API 37 pass. The API 36 "no prompt" check
  was then blocked by a tooling outage, and the user chose to skip it too and check later. Both are
  open in `docs/revisit.md`. The banner wraps `MainNavHost` and takes the top inset (this app has no
  shell `Scaffold`, unlike wallosmobile).
- [x] **M4.2** — Settings screen with custom headers. A new `feature:settings:ui` module; port
  wallosmobile's `customheaders/` screen and ViewModel (`feature/settings/ui`). The storage,
  validator and Ktor plugin come from `grappim-kit-customheaders` and are already wired
  (`core/api/.../KoillectionHttpClient.kt`). The app opens Settings as a route from the
  collection list top bar. The login screen opens it as state, not a route (login has no back
  stack), as wallosmobile's `LoginBranch`. Keep Log out where it is unless the user asks.
  **Verify:** ViewModel tests; on the emulator, a header saved from login and from the app
  reaches the test instance (Ktor log shows the name, the value redacted), and survives `am kill`.
  Note (2026-11-16): ViewModel tests (`CustomHeadersViewModelTest`), `build` and `ktlintCheck`
  passed. Settings is one screen (`SettingsRoute`) whose only section is the custom headers; a
  "Settings" text button opens it from the collection list top bar and from login (`LoginBranch`).
  `Authorization` is a reserved name (`NetworkModule.provideCustomHeaderValidator`): the
  `CustomHeaders` plugin runs after `bearerAuth` and would replace the token. **Deviation in the
  Verify:** the debug app installs no `KitLogger`, so there is no Ktor log on the device. The
  redaction is a unit test (`KoillectionHttpClientTest`, "the log names a custom header but hides
  its value"). On `koil36`, a header-printing TCP forwarder (`127.0.0.1:8291` → `8290`) showed the
  header saved at login on `POST /api/authentication_token` and `GET /api/collections`, the value
  changed in the app on the next refresh, and the value after `force-stop` and after `am kill` on
  Settings (the route came back, and Back went to the list).
- [x] **M4.3** — Proxy rejection message. Port the idea of wallosmobile's `ProxyRejection.kt`, not
  its rule. Koillection itself answers 401 to a missing or expired JWT (`bearerAuth` re-logs in
  on it), so "every 401 is a proxy" does not hold here. Settle the rule from evidence: the body
  and content type of Koillection's own 401 against an nginx `auth_basic` 401, plus 407 and an
  off-host redirect. A proxy 401 must not start a re-login. Repro: a throwaway `auth_basic` front
  over the test instance (wallosmobile `docs/local-info.txt`, "Reverse-proxy header auth"); write
  the recipe for this project into `docs/local-info.md`.
  **Verify:** `MockEngine` tests for each case, with a root-path base URL as well; on the emulator
  through the front, no header gives the proxy message at login and in the app, and the M4.2
  header gets through.
  Note from M4.2 (2026-11-16): the app may not send a custom `Authorization` (reserved: it holds
  the bearer token). A plain `auth_basic` front reads `Authorization`, so the M4.2 header cannot
  pass it. For the "header gets through" case, make the front check another header (for example
  nginx `if ($http_x_proxy_token != ...)`): the user chose this (2026-11-16, "we can test on
  some other custom header"). Closed (user, 2026-11-16): an `auth_basic` front over the whole
  server breaks Koillection's own API for every client (its JWT also travels in
  `Authorization`), so the app does not try to support it.
  Note (2026-11-16): the rule, from evidence (plan §2, "Proxy rejection"): a 401 is Koillection's
  only with the JWT layer's JSON body `{"code":401,...}`; any other 401, any 407 and a 3xx to
  another host are a proxy (`isProxyRejection`, `KoillectionError.ProxyRejection`, one string
  `error_proxy_rejected` for login and the app screens). `bearerAuth` does not log in again on a
  proxy 401. **Behaviour change:** the client no longer follows redirects (`followRedirects =
  false`), so a same-host 3xx is now `KoillectionError.Http`, not a followed `GET`. Tests:
  `ProxyRejectionTest` (15, each case at `/sub` and at the root path; mutation-checked for the
  `bearerAuth` guard and `followRedirects`), plus one each in `AuthRepositoryImplTest`,
  `LoginViewModelTest`, `CollectionListViewModelTest`. `build` and `ktlintCheck` passed. On
  `koil36` through the front (`docs/local-info.md`, "Proxy front"): no header gave the proxy
  message at login; `X-Proxy-Token` saved from login Settings logged in (front log: 200 on the
  login and `GET /api/collections`). In the app the "no header" case was a wrong value, which
  the front treats the same: the refresh got one 401, no re-login `POST`, the proxy message, and
  the session stayed.
- [x] **M4.4** — Cleartext warning at login. When the server URL starts with `http://`, show that
  the password and the token travel unencrypted. Advisory only: it never disables Log in,
  because the test instance is plain HTTP. Only a literal `http://` prefix counts.
  **Verify:** ViewModel tests; on the emulator the warning shows for `http://` and not for
  `https://`.
  Note (2026-11-16): `LoginUiState.isCleartextWarningVisible` (the trimmed URL starts with
  `http://`, any case, as wallosmobile's); the line sits under the server URL field. Tests: five in
  `LoginViewModelTest` (http, capitals with spaces, https, no scheme, Log in stays on and sends);
  the `ignoreCase` mutation fails one. `build` and `ktlintCheck` passed. On `koil36`:
  `http://10.0.2.2:8290` shows the warning, `https://10.0.2.2:8290` does not.
- [x] **M4.5** — Security register and dependency alerts. Run `/masvs-review` to create
  `docs/security/masvs.md` (after M4.1–M4.4, so it reviews them). Add `renovate.json` with
  `osvVulnerabilityAlerts`, as wallosmobile's; keep only the package rules this build needs.
  Renovate runs only after the user installs the GitHub app on the repo: tell them.
  **Verify:** `masvs.md` covers every MASVS v2 control with a status; each gap is a
  `docs/revisit.md` entry or has the user's verdict; `renovate-config-validator` passes.
  Note (2026-11-16): `docs/security/masvs.md` gives all 24 controls a status, from source only
  (its last table lists the device and APK checks). Four deviations accepted by the user
  (`allowBackup`, no `FLAG_SECURE`, `minSdk` 24, no update check); three Open rows (CODE-3 until
  the Renovate app is installed, PRIVACY-4 image cache after log out, PRIVACY-1
  `ACCESS_NETWORK_STATE`), each in `docs/revisit.md`. `renovate.json` keeps the datetime and
  `java-jdk` rules (no `detekt-rules` here). **Deviation in the Verify:** `renovate-config-validator`
  did not run; the user chose to let the bot check the config on its first run. Only a JSON parse
  ran. **The user must install the Renovate GitHub app** for it to run at all.

## M5 — edit and delete items

Change an existing item's name, text fields, main image, image fields and tags; delete an item
(with a confirm). Uses `PATCH`/`DELETE` on `Item` and `Datum` (Koillection source). Form choices
are plan §5 decision 8 (user, 2026-11-16): the add form, prefilled; the main image is replaced,
never removed; no field reordering. Every write goes to the local test instance only
(`docs/local-info.md`).

- [x] **M5.1** — Repository writes (`feature:items`): rename an item (`PATCH /api/items/{id}`,
  `{"name"}`), change a field (`PATCH /api/data/{id}`, `label` and/or `value`), delete a field
  (`DELETE /api/data/{id}`), delete an item (`DELETE /api/items/{id}`). Re-read `Item.php` and
  `Datum.php` first. A `PATCH` sends `application/merge-patch+json` and only the changed keys.
  **Verify:** mock-engine tests for each call (method, path, content type, body keys), with a
  root-path base URL case (`CLAUDE.md`, Verification); mutation-check one body key and one method.
  Note: `updateDatum(id, label, value)` leaves a null argument out of the body. Mutations caught:
  a `value` key always sent (the label-only test), `deleteItem` sent as `POST` (the method test).
- [x] **M5.2** — Live edit smoke against the local test instance: extend `smokeWriteRun` (or add a
  task) to create an item with a text field, an image field and a main image, then rename it,
  change the field's label and value, replace the main image and the field image, delete the
  image field, and delete the item. After each replace, the old file gives 404; after the field
  delete, its file gives 404. Also try a `PATCH` that gives a field the label of another field
  of the item (expect 422). Record the answers in plan §2.
  **Verify:** the run prints each step and passes; plan §2 has the results.
  Note: the duplicate-label `PATCH` got an empty HTTP 500, not the expected 422 (server bug,
  plan §2); the run now expects the 500. A `POST` with a duplicate label still gets 422.
- [x] **M5.3** — Delete item: a delete action on the item screen with a confirm dialog. On
  success the item screen leaves the back stack and the collection reads its list again
  (`ItemListChanges`). A failure shows the error and keeps the item screen.
  **Verify:** ViewModel tests; on the emulator a test item is deleted, the collection list no
  longer shows it, and the web UI agrees.
  Note: the error shows in a dialog ("The item was not deleted"); the screen keeps the item.
  The device check used a temporary collection (deleted after) and airplane mode for the failure.
- [x] **M5.4** — Edit use case (`feature:items:domain`): from the item as loaded (name, fields,
  main image, tags) and the edited form, work out the calls and run them. Deletes first (a freed
  label can be reused), then changes, then new fields (positions after the current last), then a
  new main image, then the tags (only if the set changed). Fields the form cannot edit (types
  other than `text` and `image`) are never touched. A retry reloads the item and works out the
  calls again, so a call that already went through is not sent twice. A label swap between two
  fields needs a temporary label (server rule, plan §2): handle it, or reject it in the form
  with a message; decide in the step. A duplicate label in a `PATCH` gets an empty 500, not a
  422 (M5.2, plan §2), so a conflict must be caught before the calls are sent.
  **Verify:** use case tests with hand-written fakes: each kind of change alone, a mix, no change
  (no calls), a failure in the middle and the retry, a label swap, an untouched non-text field.
  Note: a label swap is handled: the use case moves one field to a temporary label
  (`<label> (moving 1)`), visible on the server only if the save stops there. A duplicate final
  label returns `EditItemResult.DuplicateLabel` before any call. The retry is `retry(original, edit)`;
  it reads the item again and matches a new field already created by its label. `EditItemUseCase`
  is bound in `ItemsDataModule`.
- [x] **M5.5** — Edit screen: an "edit" action on the item screen opens the M3 add form,
  prefilled from the server (name, text fields, the main image and image fields shown from their
  URLs, the item's tags). An existing photo can be replaced (camera or picker) or, for an image
  field, removed; its label can change. The main image has replace only. New text rows and new
  photos are added as in M3. Fields of other types show read only. The form survives rotation and
  process death (edits kept, not reloaded over).
  **Verify:** ViewModel tests; on the emulator the form opens with the item's values, and
  `am kill` keeps an edit.
  Note: a separate `EditItemViewModel`/`EditItemRoute` with the add form's parts (`ItemFormParts.kt`);
  the item screen's pencil opens it. The form has no Save yet (M5.6). "Replace" (camera or picker)
  is per image; the picker can return several photos, and only the first is used. The draft and
  the loaded values are in saved state as `EditItemDraft`; M5.6 can build the `ItemSnapshot`
  diff base from `original`, or read the item again.
- [x] **M5.6** — Save the edit: run M5.4 from the form with the M3 progress view and retry. On
  success return to the item screen, which reads the item again, and refresh the collection (a
  new name shows in the list). An upload 500 names the disk quota, as in M3.6. Delete the photo
  files in `cacheDir/photos/` as M3.6 does.
  **Verify:** ViewModel tests; on the emulator one edit renames the item, changes a text field,
  adds one, removes an image field, replaces the main image and changes the tags; the web UI on
  the local test instance shows the same.
  Note: the item screen reads the item again through a new `ItemChanges` signal (bound in
  `ItemsDataModule`). `EditItemResult.Failure` got `isUpload` for the quota message. The loaded
  `ItemSnapshot` is kept in saved state; the first save is `editItem`, every later one `retry`. A
  failure after a call went through offers only the retry; before that, "Back to the form" too.
  `SaveView` moved to `ItemFormParts.kt`. The device check used a temporary collection (deleted after).
- [x] **M5.7** — Device pass of edit and delete, then the user confirms with screenshots. Fix what
  breaks, or put it in `docs/revisit.md` with evidence.
  **Verify:** the user confirms.
  Note: nothing broke. One save renamed the item, changed a text value, added a field, removed an
  image field, replaced the main image (picker) and changed the tags; the API agreed and both old
  files gave 404. The edits survived `am kill` before Save. Temporary collection, deleted after.
  The user confirmed from the screenshots.

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
- [x] **M6.6** — Device pass of create, edit and delete, then the user confirms with screenshots.
  Fix what breaks, or put it in `docs/revisit.md` with evidence.
  **Verify:** the user confirms.
  Note: nothing broke. A top-level collection (camera photo) and a child were created, the child
  renamed, the top image replaced (picker; the old file gave 404) and removed (`image` null, file
  404). The delete dialog counted 1 collection and 1 item (item added through the API); after it
  the collection, the child and the item gave 404. The list showed a new image as a thumbnail. The
  user confirmed from the screenshots.
