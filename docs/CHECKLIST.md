# koillectionmobile checklist

**Current step:** M0.5 (not started). M0.4 done 2026-11-14. Plan §5 decisions 1, 2 and 4 are settled.

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
- [ ] **M0.5** — Merge `../agentic-grappim/templates/CLAUDE.md.template` into root `CLAUDE.md`,
  keeping this project's facts.
- [ ] **M0.6** — CI workflow (port deskmate's). It must call `ktlintCheck` itself: `build`
  skips the root `:ktlintKotlinScriptCheck` (M0.4 note).
  **Verify:** CI green on the pushed commit.

## M1 — `core:api` (rough scope)

Ktor client, server URL + login (JWT), token storage (grappim-kit `storage`), Hydra list
paging, DTOs for Collection / Item / Datum / Tag, multipart image upload. Fixture-tested
against real JSON captured from the user's server.

## M2 — browse (rough scope)

Login screen, collection list, item list, item detail with images and fields.

## M3 — add item with photos (rough scope)

Pick a collection, take or pick photos (main image + page images), text fields, tags.
Matches the book workflow in plan §1.

## M4 — release (rough scope)

Signing, F-Droid/IzzyOnDroid and/or Play per plan §5 decision 2.
