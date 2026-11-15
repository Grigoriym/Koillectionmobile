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
