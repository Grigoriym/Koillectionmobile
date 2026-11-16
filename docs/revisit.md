# Revisit

Real problems found outside the current task. One entry each, with `file:line` or a link so a
cold session can act on it. Delete an entry when it's fixed.

- 2026-11-14 (M0.2) — Every Kotlin compile prints `Koin compiler plugin: Kotlin 2.4.21 is newer
  than the newest verified version (2.4.20)`. Catalog: `kotlin = "2.4.21"`, `koin-plugin =
  "1.2.3"` (`gradle/libs.versions.toml`). Harmless today; bump `koin-plugin` once a release
  lists 2.4.21 as verified (https://github.com/InsertKoinIO/koin-compiler-plugin/releases).
- 2026-11-14 (M0.3) — Every module on `koillectionmobile.kmp.library.stability` (`core:domain`,
  `feature:*:domain`) prints `Unsupported compileOnly Dependencies in Kotlin Targets` for
  `compose.runtime` on `iosArm64Main`/`iosSimulatorArm64Main`. Source:
  `build-logic/.../ComposeStabilityMarker.kt:22` (`compileOnly`, ported from wallosmobile, which
  has no iOS targets so never saw it). Build is green. Fix options: make it `implementation`
  (every consuming `ui` module carries compose-runtime anyway), or `compileOnly` for
  `androidMain`/`jvmMain` + `implementation` for `iosMain`. Decide before iOS is ever built.
- 2026-11-14 (M0.6) — CI run 43643554439 warns: `actions/upload-artifact@v4` targets Node.js 20,
  which GitHub forces onto Node.js 24. Source: `.github/workflows/ci.yml` (the "Upload Kover XML
  report" step, ported from deskmate, which has the same step). Fix: bump to the newest
  `upload-artifact` major that targets Node 24, then check CI stays green.
- 2026-11-15 (M1.6) — `FakeSessionStorage` now exists three times, copied by hand:
  `core/api/src/commonTest/.../core/api/FakeSessionStorage.kt`,
  `feature/auth/data/src/commonTest/.../auth/data/FakeSessionStorage.kt`,
  `feature/collections/data/src/jvmTest/.../collections/data/FakeSessionStorage.kt`. M1.7 and M1.8
  will add two more. Fix: one copy in a shared test module (or `core:storage` test fixtures) that
  the `data` modules take as a test dependency. M1.7 added the fourth:
  `feature/items/data/src/jvmTest/.../items/data/FakeSessionStorage.kt`.
  M1.8 added the fifth: `feature/tags/data/src/jvmTest/.../tags/data/FakeSessionStorage.kt`.
  M2.6 added `clearSession()` to `SessionStorage` and had to edit all five copies.
- 2026-11-15 (M1.9) — Write bodies send IRIs without the server's subpath (`/api/collections/<id>`,
  `/api/items/<id>`, `/api/tags/<id>`): `ItemsRepositoryImpl.createItem`/`itemIri` and
  `TagsRepositoryImpl.setItemTags`. The server's own IRIs carry its prefix (M1.4: `next` links).
  Whether API Platform accepts an unprefixed IRI on a server behind a subpath is not checked:
  the source has no `vendor/`, and the local test instance runs at the root. `add_book.py` uses
  unprefixed IRIs against the user's server, but whether that server has a subpath is not
  recorded here. Check once against a subpath instance (or the user's setup) before release (M9).
  User, 2026-11-15: their server is reached through Tailscale, with no subpath ("I presume").
  Deferred by the user (2026-11-15): not part of M3.7. Confirm it later from the server URL
  itself, read only.
- 2026-11-15 (M2.5) — The error mapping and the error view exist twice, copied by hand:
  `Throwable.toMessage()` in `feature/collections/ui/.../CollectionListViewModel.kt:74` and
  `feature/items/ui/.../ItemViewModel.kt` (bottom), and `EmptyOrError` in
  `feature/collections/ui/.../CollectionListScreen.kt:97` against the inline error `Column` in
  `feature/items/ui/.../ItemScreen.kt`. Only the string resources differ. A third `ui` screen makes
  it worth one shared copy (a `core:ui` module, or a `KoillectionError` → message function that
  takes the strings).
  M4.3 added a `KoillectionError.ProxyRejection` branch to all four mappings (login, collection
  list, item, add item); they share one string, `error_proxy_rejected`.
- 2026-11-15 — `docs/IMPLEMENTATION_PLAN.md:15` points to `~/claude/books/00service/` (`koi.py`,
  `add_book.py`) as the worked example of the API calls. That folder does not exist on the current
  machine. Ask the user where the scripts live now, then fix the path or drop the pointer.
- 2026-11-15 (M3.6) — Add-item photo files in `cacheDir/photos/` are deleted on photo remove, on
  save, and when the form leaves the back stack (`AddItemViewModel.onCleared`). A form that never
  comes back (the task swiped away, so the saved draft is gone and `onCleared` never runs) leaves
  its files behind: M3.6 found three such files from M3.5 on the emulator. Fix option: on app
  start, delete files in `cache/photos/` that no saved draft names (or older than a day).
- 2026-11-15 (M3.6) — A `createItem` whose response is lost (a timeout after the server created
  the item) leaves no item id in `AddItemState`, so "Try again" creates a second item.
  `AddItemUseCaseImpl.addItem` in `feature/items/domain/.../AddItemUseCase.kt`. The server has
  no idempotency key. Fix option: before a retry with no item id, look for a new item of the same
  name in the collection, or ask the user.
- 2026-11-16 (M4.1) — The local network permission is not checked on an API 37 device. The
  `koil37` AVD crashes (`docs/machine.local.md`), and the user chose to skip that pass. Open:
  denied at Log in gives `login_error_local_network_denied` and sends no request; allowed logs in;
  a revoke while logged in shows `LocalNetworkBanner` (`composeApp/.../LocalNetworkBanner.kt`),
  which leaves after a grant; the banner takes the status bar inset and the screen below does not
  add a second gap. Run it on a working API 37 AVD or an Android 17 phone.
- 2026-11-16 (M4.1) — Known limits carried over from wallosmobile (its `docs/revisit.md` §6):
  screens do not reload after a grant (the collection list needs Try again); the denied message at
  Log in has no "Open settings" action. `feature/auth/ui/.../permission/`.
- 2026-11-16 (M4.2) — No JVM gate catches a module missing from `AppModule`'s `includes`
  (`composeApp/src/commonMain/.../di/Koin.kt`). On the JVM the Koin plugin finds `@Configuration`
  modules on its own, so `KoinGraphTest` passes; Android does not, and `SettingsUiModule` crashed
  the app on device until it was added. Fix option: a `jvmTest` that lists every `@Configuration`
  module class in the build (or every `*UiModule`/`*DataModule` in `settings.gradle.kts` modules)
  and checks each is in `AppModule`'s `includes` (read the annotation by reflection). Effort: one
  test, if the annotation keeps runtime retention; otherwise a source-text check of `Koin.kt`.
- 2026-11-16 (M4.5) — MASVS-CODE-3: no advisory feed checks the dependencies yet. `renovate.json`
  sets `osvVulnerabilityAlerts`, but Renovate runs only after the user installs the Renovate
  GitHub app on `Grigoriym/Koillectionmobile`. GitHub's own vulnerability alerts are off
  (`gh api repos/Grigoriym/Koillectionmobile/vulnerability-alerts`: 404 "disabled"). The
  `renovate-config-validator` run was skipped (user, 2026-11-16: the bot checks the config on its
  first run). Close when the app is installed and its first run reports no config error.
- 2026-11-16 (M4.5) — MASVS-PRIVACY-4: log out does not clear Coil's image disk cache.
  `AuthRepositoryImpl.logOut` (`feature/auth/data/.../AuthRepositoryImpl.kt:57`) clears only the
  session keys; the app uses Coil's default singleton `ImageLoader` (no `diskCache` config). A log
  in to another server keeps the first server's images on the device. Fix option: clear the
  `ImageLoader`'s disk and memory cache on log out.
- 2026-11-16 (M4.5) — MASVS-PRIVACY-1: `ACCESS_NETWORK_STATE`
  (`androidApp/src/main/AndroidManifest.xml:5`) has no user in app code. Coil's network fetcher
  may use it. Check the merged release manifest (M9.2) and the libraries before removing it.
- 2026-11-16 (M4.5) — Images do not get the custom headers (M4.2). Coil loads them with its own
  Ktor client (`coil-network-ktor3`, `composeApp/build.gradle.kts:30`), not the app's
  `HttpClient`, so the `CustomHeaders` plugin does not run. Behind a header-checking proxy
  (`docs/local-info.md`, "Proxy front") the thumbnails and item images will fail. Not checked on
  device. Fix option: a singleton `ImageLoader` whose fetcher uses a client with the
  `CustomHeaders` plugin (server origin only).
