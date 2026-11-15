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
- 2026-11-15 — `failFast` again (M1.4): two mutations in one `jvmTest` run showed only the first
  failure. Ran one mutation per run with `--tests`, plus a clean-tree run of each filter, since a
  filter that matches nothing also fails.
- 2026-11-15 — The iOS backtick-name trap again (M1.5): a comma in a test name passed `jvmTest`
  and failed `compileTestKotlinIosArm64`. Second time; a third makes it a rule in `CLAUDE.md`.
- 2026-11-15 — Ktor `parseUrl("https:///sub")` returned a `Url` with a non-blank host, so a
  host check on the parsed URL passed a URL with no host. Read the host from the text instead.
- 2026-11-15 — `git rm` of a module's only source file (M1.7) also removed its empty directories,
  so the next heredoc writes failed "No such file or directory". Run `mkdir -p` after it.
- 2026-11-15 — `../../koillection` has no `vendor/` (M1.7), so API Platform internals (default
  list order) cannot be read from source; the plan records that claim as unchecked.
- 2026-11-15 — Mutation restore with `git checkout -- <file>` (M1.8) failed ("pathspec did not match") on new,
  untracked files, so four mutations stacked up. Undid them by hand; copy the file to the
  scratchpad first and restore from the copy.
- 2026-11-15 — Resolved `../../koillection` from the wrong directory (M1.9) and got "No such file or
  directory"; from the repo root it is `/home/gregory/proj/koillection`.
- 2026-11-15 — A Python heredoc that spliced Kotlin raw strings (`"""`) into a test file failed with
  a SyntaxError (M1.9). Write the Kotlin to a scratchpad file and have Python read it.
- 2026-11-15 — `../../koillection` resolved from the wrong directory again (M1.10): typed
  `/home/gregory/proj/grappim/koillection`. Second time; a third makes the absolute path a
  `CLAUDE.md` line.
