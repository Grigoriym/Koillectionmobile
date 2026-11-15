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
- 2026-11-15 — `request.url.encodedPath` on a `URLBuilder` failed "Unresolved reference": it is
  an extension and needs `import io.ktor.http.encodedPath`; the IDE-free compile gave no hint.
- 2026-11-15 — `failFast` again (M1.4): two mutations in one `jvmTest` run showed only the first
  failure. Ran one mutation per run with `--tests`, plus a clean-tree run of each filter, since a
  filter that matches nothing also fails.
- 2026-11-15 — The iOS backtick-name trap again (M1.5): a comma in a test name passed `jvmTest`
  and failed `compileTestKotlinIosArm64`. Second time; a third makes it a rule in `CLAUDE.md`.
- 2026-11-15 — Ktor `parseUrl("https:///sub")` returned a `Url` with a non-blank host, so a
  host check on the parsed URL passed a URL with no host. Read the host from the text instead.
