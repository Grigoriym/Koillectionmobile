# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed
entries get deleted — see /finalize.

- 2026-11-14 — `sed -e 's#...#...#'` failed with "unknown option to `s'" because the replacement
  text held a `#` comment. Use `|` as the delimiter when the text may contain `#`.
- 2026-11-15 — A KDoc holding `` `application/*+json` `` opened a nested `/*` comment; the
  compiler reported "Unclosed comment" and an unresolved name far below it.
- 2026-11-15 — `failFast = true` (`Quality.kt`) stopped `jvmTest` at the first failure, so the
  XML report listed 4 of 14 tests and looked like the others were missing.
- 2026-11-15 — `failFast` again (M1.4): two mutations in one `jvmTest` run showed only the first
  failure. Ran one mutation per run with `--tests`, plus a clean-tree run of each filter, since a
  filter that matches nothing also fails.
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
- 2026-11-15 — `git rm` of a module's only source file removed its empty directories again
  (M2.1), and five heredoc writes failed "No such file or directory". Second time; a third
  makes it a `CLAUDE.md` line.
- 2026-11-15 — Emulator login form (M2.2): tapped each field by its pre-keyboard coordinate,
  though the skill says to use `KEYCODE_TAB`; the keyboard moved the fields and all three values
  went into the URL field.
- 2026-11-15 — A Python heredoc with Kotlin raw strings (`"""`) in a `"""`-delimited Python string failed with a
  SyntaxError again (M2.6). Second time; `'''` delimiters in the Python fixed it.
- 2026-11-15 — A Python string-replace that put the `material.icons` imports above `material3.CircularProgressIndicator`
  (M2.8) broke ktlint's import order in `ItemScreen.kt`, which also imports `material3.Button`. Insert by sort order, or run `ktlintFormat`.
- 2026-11-15 — Piped a hung `./gradlew :composeApp:smokeWriteRun` through `| tail -40` (M3.1): `tail` prints
  only at the end, so 10 minutes passed with no output. Redirect to a file under `timeout` instead.
- 2026-11-15 — `pkill -f LiveWriteSmokeKt` (M3.1) also matched the Bash tool's own shell, whose command
  line held that text, and killed it (exit 144). Kill by PID from `pgrep`.
- 2026-11-15 — `io.ktor.http.contentType(this, ...)` in a debug edit (M3.1) failed "Unresolved reference":
  it is an extension, as the `CLAUDE.md` gotcha says. Used `header("Content-Type", ...)`.
- 2026-11-15 — `./gradlew :core:image:jvmTest --rerun -q | grep PASSED` (M3.2) printed nothing: `-q` hides the
  test-logging events, so the run looked empty. Drop `-q` when the PASSED lines are the proof.
- 2026-11-15 — `:androidApp:installDebug` failed "ambiguous": the app has store flavors. Use
  `installGplayDebug` (or `installFdroidDebug`).
- 2026-11-15 — `installGplayDebug` failed "SDK location not found": no `ANDROID_HOME` and no
  `local.properties`. Prefixed the command with `ANDROID_HOME=$HOME/Android/Sdk`.
