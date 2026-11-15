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
- 2026-11-15 (M1.9) — Write bodies send IRIs without the server's subpath (`/api/collections/<id>`,
  `/api/items/<id>`, `/api/tags/<id>`): `ItemsRepositoryImpl.createItem`/`itemIri` and
  `TagsRepositoryImpl.setItemTags`. The server's own IRIs carry its prefix (M1.4: `next` links).
  Whether API Platform accepts an unprefixed IRI on a server behind a subpath is not checked:
  the source has no `vendor/`, and the local test instance runs at the root. `add_book.py` uses
  unprefixed IRIs against the user's server, but whether that server has a subpath is not
  recorded here. Check once against a subpath instance (or the user's setup) before M3 ships.
- 2026-11-15 (M2.5) — The error mapping and the error view exist twice, copied by hand:
  `Throwable.toMessage()` in `feature/collections/ui/.../CollectionListViewModel.kt:59` and
  `feature/items/ui/.../ItemViewModel.kt` (bottom), and `EmptyOrError` in
  `feature/collections/ui/.../CollectionListScreen.kt:86` against the inline error `Column` in
  `feature/items/ui/.../ItemScreen.kt`. Only the string resources differ. A third `ui` screen makes
  it worth one shared copy (a `core:ui` module, or a `KoillectionError` → message function that
  takes the strings).
