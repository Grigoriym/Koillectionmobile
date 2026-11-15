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
