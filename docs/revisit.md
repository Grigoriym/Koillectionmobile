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
