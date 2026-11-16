# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed
entries get deleted — see /finalize.

- 2026-11-14 — `sed -e 's#...#...#'` failed with "unknown option to `s'" because the replacement
  text held a `#` comment. Use `|` as the delimiter when the text may contain `#`.
- 2026-11-15 — A KDoc holding `` `application/*+json` `` opened a nested `/*` comment; the
  compiler reported "Unclosed comment" and an unresolved name far below it.
- 2026-11-15 — Ktor `parseUrl("https:///sub")` returned a `Url` with a non-blank host, so a
  host check on the parsed URL passed a URL with no host. Read the host from the text instead.
- 2026-11-15 — `git rm` of a module's only source file (M1.7) also removed its empty directories,
  so the next heredoc writes failed "No such file or directory". Run `mkdir -p` after it.
- 2026-11-15 — `../../koillection` has no `vendor/` (M1.7), so API Platform internals (default
  list order) cannot be read from source; the plan records that claim as unchecked.
- 2026-11-15 — A Python heredoc that spliced Kotlin raw strings (`"""`) into a test file failed with
  a SyntaxError (M1.9). Write the Kotlin to a scratchpad file and have Python read it.
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
- 2026-11-15 — A Python `str.replace` on `KoinGraphTest.kt` (M3.5) matched nothing and changed nothing, with no
  error; only `git diff --stat` (1 line, not 3) showed it. Assert the old text is in the file before replacing.
- 2026-11-15 — Python-inserted `material.icons` and `lazy` imports in `AddItemScreen.kt` (M3.5) broke ktlint's
  import order again. Second time (first M2.8); `ktlintFormat` fixed it.
- 2026-11-15 — `grep -rl <name> feature strings | xargs sed -i` (M3.6) also rewrote generated files under `build/`.
  Harmless, but exclude `build/` from such a grep.
- 2026-11-16 — `adb exec-out screencap -p > s.png` on the API 37 AVD (M4.1) wrote the abort text
  ("Assertion failed: ...hasReadColorBufferDma") into the `.png` with exit 0; only `file` showed it.
- 2026-11-16 — On the crashing API 37 AVD, `nc` and `ping` from `adb shell` said "Network is
  unreachable" even for `1.1.1.1`, and an `adb install` printed "Success" but the app was gone after
  the next boot. Both were the crash loop, not a network block or an install bug.
- 2026-11-16 — `sdkmanager` installing the API 37 system image also updated the shared emulator
  binary (to 37.2.13) without asking. The already-running `koil36` kept the old binary in memory.
- 2026-11-16 — `pkill -f "fwd.py ..."` killed the Bash tool's own shell again (exit 144), as in M3.1.
  Second time; kill by PID from `pgrep` (a pattern like `fwd\.py` does not match its own command line).
- 2026-11-16 — The auto-mode classifier gave "no verdict (error)" on Bash calls in M4.1, M4.2 and
  M4.3 (three times in M4.3: a `find`, a `docker rm` + `grep`). A retry passed each time; Read kept
  working. Third session: harness-side. Promoted: `.claude/settings.json` allows read-only
  commands (user, 2026-11-16); delete this entry if it stops recurring.
- 2026-11-16 — `jq` is not installed on this machine; a `jq -e ... && next` chain skipped the next
  step and the rest ran anyway. Validate JSON with `python3 -c "import json; ..."`.
- 2026-11-16 — `./gradlew build` (M4.4) failed "SDK location not found": ran it before reading
  `docs/machine.local.md`, which says to prefix `ANDROID_HOME=$HOME/Android/Sdk`.
- 2026-11-16 — `npx --yes --package renovate -- renovate-config-validator` (M4.5) was denied by the
  auto-mode classifier ("Unauthorized Access"), as a package download. The user chose to skip it.
- 2026-11-16 — `./gradlew build` (M5.3) failed "SDK location not found" again: ran it before reading
  `docs/machine.local.md`. Second time (first M4.4); a third makes it a fix (`local.properties` or a `CLAUDE.md` line).
- 2026-11-16 — A Bash call (M5.5) that started with `cd` into a deep source folder got no verdict
  from the auto-mode classifier ("reviewed this call without its leading cd"); the identical retry passed.
