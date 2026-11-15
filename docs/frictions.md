# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed
entries get deleted — see /finalize.

- 2026-11-14 — `sed -e 's#...#...#'` failed with "unknown option to `s'" because the replacement
  text held a `#` comment. Use `|` as the delimiter when the text may contain `#`.
- 2026-11-15 — A KDoc holding `` `application/*+json` `` opened a nested `/*` comment; the
  compiler reported "Unclosed comment" and an unresolved name far below it.
- 2026-11-15 — `failFast = true` (`Quality.kt`) stopped `jvmTest` at the first failure, so the
  XML report listed 4 of 14 tests and looked like the others were missing.
- 2026-11-15 — A backtick test name with a comma compiled on JVM but failed
  `compileTestKotlinIosArm64` ("Name contains illegal characters"); only `build` caught it.
