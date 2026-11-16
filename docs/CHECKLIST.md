# koillectionmobile checklist

**Current step:** M4.5. M4.4 done 2026-11-16. M4.3 done 2026-11-16. M4.2 done 2026-11-16. M4.1 done 2026-11-16 without its device checks (see its Note). The release moved to M9 (user, 2026-11-16). M3 done 2026-11-15, M2 done 2026-11-15, M1 done 2026-11-15, M0 done 2026-11-14 (all archived). Plan §5 decisions 1, 2, 4, 5, 6 and 7 are settled.

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

M0 (repo and module scaffolding) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M1 (API layer) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M2 (browse) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.
M3 (add item with photos) is done; its steps and notes are in `docs/CHECKLIST_ARCHIVE.md`.

## M4 — network and security parity with wallosmobile

The user chose all four gaps found against `../Wallosmobile` (user, 2026-11-16). Model: wallosmobile
plan §4.5b (custom headers, proxy rejection), §4.5c (local network permission) and its cleartext
warning (plan §1.1, "Warn on non-HTTPS base URLs"). Not in scope: self-signed certificate trust
(TOFU). Plan §5 decision 3 still holds.

- [x] **M4.1** — Local network permission. Port wallosmobile 28.10: `ACCESS_LOCAL_NETWORK` in the
  manifest, `rememberLocalNetworkPermissionRequest` (`expect`/`actual`, from wallosmobile `uikit`
  `permission/`), asked at Log in only for a private, link-local or `fc00::/7` address on API 37+,
  a "denied" login message, and `LocalNetworkBanner` above `MainNavHost`. There is no local
  `uikit` (settled decisions): put the request in `feature:auth:ui` and the banner in `composeApp`
  (user, 2026-11-16: here for now; a move to `grappim-kit-uikit` may come later). Create an
  API 37 AVD (`system-images;android-37.0;google_apis;x86_64`) and record it in
  `docs/machine.local.md`.
  **Verify:** unit tests for the address check; on the API 37 AVD, against the test instance at
  the host's LAN address (not `10.0.2.2`): denied gives the message and sends no request, allowed
  logs in, and a revoke while logged in shows the banner, which leaves after a grant. The API 36
  AVD logs in with no prompt.
  Note (2026-11-16): unit tests (`LocalNetworkAddressTest`, `jvmTest`, on the JVM's parsed bytes;
  the address check is common code over `ByteArray`), `build` and `ktlintCheck` passed. **The
  device checks did not run.** The `koil37` AVD crashes in `surfaceflinger` on emulator 37.2.13
  (`docs/machine.local.md`); the user chose to skip the API 37 pass. The API 36 "no prompt" check
  was then blocked by a tooling outage, and the user chose to skip it too and check later. Both are
  open in `docs/revisit.md`. The banner wraps `MainNavHost` and takes the top inset (this app has no
  shell `Scaffold`, unlike wallosmobile).
- [x] **M4.2** — Settings screen with custom headers. A new `feature:settings:ui` module; port
  wallosmobile's `customheaders/` screen and ViewModel (`feature/settings/ui`). The storage,
  validator and Ktor plugin come from `grappim-kit-customheaders` and are already wired
  (`core/api/.../KoillectionHttpClient.kt`). The app opens Settings as a route from the
  collection list top bar. The login screen opens it as state, not a route (login has no back
  stack), as wallosmobile's `LoginBranch`. Keep Log out where it is unless the user asks.
  **Verify:** ViewModel tests; on the emulator, a header saved from login and from the app
  reaches the test instance (Ktor log shows the name, the value redacted), and survives `am kill`.
  Note (2026-11-16): ViewModel tests (`CustomHeadersViewModelTest`), `build` and `ktlintCheck`
  passed. Settings is one screen (`SettingsRoute`) whose only section is the custom headers; a
  "Settings" text button opens it from the collection list top bar and from login (`LoginBranch`).
  `Authorization` is a reserved name (`NetworkModule.provideCustomHeaderValidator`): the
  `CustomHeaders` plugin runs after `bearerAuth` and would replace the token. **Deviation in the
  Verify:** the debug app installs no `KitLogger`, so there is no Ktor log on the device. The
  redaction is a unit test (`KoillectionHttpClientTest`, "the log names a custom header but hides
  its value"). On `koil36`, a header-printing TCP forwarder (`127.0.0.1:8291` → `8290`) showed the
  header saved at login on `POST /api/authentication_token` and `GET /api/collections`, the value
  changed in the app on the next refresh, and the value after `force-stop` and after `am kill` on
  Settings (the route came back, and Back went to the list).
- [x] **M4.3** — Proxy rejection message. Port the idea of wallosmobile's `ProxyRejection.kt`, not
  its rule. Koillection itself answers 401 to a missing or expired JWT (`bearerAuth` re-logs in
  on it), so "every 401 is a proxy" does not hold here. Settle the rule from evidence: the body
  and content type of Koillection's own 401 against an nginx `auth_basic` 401, plus 407 and an
  off-host redirect. A proxy 401 must not start a re-login. Repro: a throwaway `auth_basic` front
  over the test instance (wallosmobile `docs/local-info.txt`, "Reverse-proxy header auth"); write
  the recipe for this project into `docs/local-info.md`.
  **Verify:** `MockEngine` tests for each case, with a root-path base URL as well; on the emulator
  through the front, no header gives the proxy message at login and in the app, and the M4.2
  header gets through.
  Note from M4.2 (2026-11-16): the app may not send a custom `Authorization` (reserved: it holds
  the bearer token). A plain `auth_basic` front reads `Authorization`, so the M4.2 header cannot
  pass it. For the "header gets through" case, make the front check another header (for example
  nginx `if ($http_x_proxy_token != ...)`): the user chose this (2026-11-16, "we can test on
  some other custom header"). Closed (user, 2026-11-16): an `auth_basic` front over the whole
  server breaks Koillection's own API for every client (its JWT also travels in
  `Authorization`), so the app does not try to support it.
  Note (2026-11-16): the rule, from evidence (plan §2, "Proxy rejection"): a 401 is Koillection's
  only with the JWT layer's JSON body `{"code":401,...}`; any other 401, any 407 and a 3xx to
  another host are a proxy (`isProxyRejection`, `KoillectionError.ProxyRejection`, one string
  `error_proxy_rejected` for login and the app screens). `bearerAuth` does not log in again on a
  proxy 401. **Behaviour change:** the client no longer follows redirects (`followRedirects =
  false`), so a same-host 3xx is now `KoillectionError.Http`, not a followed `GET`. Tests:
  `ProxyRejectionTest` (15, each case at `/sub` and at the root path; mutation-checked for the
  `bearerAuth` guard and `followRedirects`), plus one each in `AuthRepositoryImplTest`,
  `LoginViewModelTest`, `CollectionListViewModelTest`. `build` and `ktlintCheck` passed. On
  `koil36` through the front (`docs/local-info.md`, "Proxy front"): no header gave the proxy
  message at login; `X-Proxy-Token` saved from login Settings logged in (front log: 200 on the
  login and `GET /api/collections`). In the app the "no header" case was a wrong value, which
  the front treats the same: the refresh got one 401, no re-login `POST`, the proxy message, and
  the session stayed.
- [x] **M4.4** — Cleartext warning at login. When the server URL starts with `http://`, show that
  the password and the token travel unencrypted. Advisory only: it never disables Log in,
  because the test instance is plain HTTP. Only a literal `http://` prefix counts.
  **Verify:** ViewModel tests; on the emulator the warning shows for `http://` and not for
  `https://`.
  Note (2026-11-16): `LoginUiState.isCleartextWarningVisible` (the trimmed URL starts with
  `http://`, any case, as wallosmobile's); the line sits under the server URL field. Tests: five in
  `LoginViewModelTest` (http, capitals with spaces, https, no scheme, Log in stays on and sends);
  the `ignoreCase` mutation fails one. `build` and `ktlintCheck` passed. On `koil36`:
  `http://10.0.2.2:8290` shows the warning, `https://10.0.2.2:8290` does not.
- [ ] **M4.5** — Security register and dependency alerts. Run `/masvs-review` to create
  `docs/security/masvs.md` (after M4.1–M4.4, so it reviews them). Add `renovate.json` with
  `osvVulnerabilityAlerts`, as wallosmobile's; keep only the package rules this build needs.
  Renovate runs only after the user installs the GitHub app on the repo: tell them.
  **Verify:** `masvs.md` covers every MASVS v2 control with a status; each gap is a
  `docs/revisit.md` entry or has the user's verdict; `renovate-config-validator` passes.

## M5 — edit and delete items (rough scope)

Change an existing item's name, text fields, main image, image fields and tags; delete an item
(with a confirm). Uses `PATCH`/`DELETE` on `Item` and `Datum` (Koillection source).

## M6 — collections: create, edit, delete (rough scope)

Create a collection (top level or child), rename it, set its image, delete it (with a confirm).

## M7 — search and tags (rough scope)

- Search items by name. Koillection 1.8.5 declares no `ApiFilter` on `Item` or `Collection`
  (checked 2026-11-16), so search runs in the app over loaded data. Settle the approach at the
  start of M7.
- A tag list screen (`feature:tags:ui` is a placeholder today) and the items of a tag
  (`/api/tags/{id}/items`).
- Create a tag from the add-item form. This reverses part of plan §5 decision 7 (user,
  2026-11-16).

## M8 — other Koillection areas (rough scope)

The user wants these before release too (2026-11-16): wishlists and wishes, albums and photos,
loans, templates (with fields and choice lists), inventories, logs. In 1.8.5, `Inventory` has
no `Post` and `Log` is read only. At the start of M8, settle the order and the scope of each
area with the user; split M8 into one milestone per area if it is too big for one.

## M9 — release

Runs after M4–M8 (user, 2026-11-16). Both store flavors ship, as in the user's other apps (user, 2026-11-16): `fdroid` to
IzzyOnDroid first, `gplay` to Google Play. Model: `../Wallosmobile` (`build-logic`
`configureAppSigningConfigs`, `androidApp/build.gradle.kts` dependency-info block,
`.github/workflows/release*.yml`, `fastlane/metadata/`). No Firebase: wallosmobile's
`google-services.json` steps are not ported. Two inputs come from the user later: the launcher
icon (M9.4) and the release keystores (M9.5). Those steps wait for them; the others do not.

- [ ] **M9.1** — Release blockers. Check the subpath question read only, from the user's server
  URL (`docs/revisit.md`, M1.9 entry). Go through every `docs/revisit.md` entry with the user;
  each one is "fix before release" (gets a step here) or "after release".
  **Verify:** the M1.9 entry is closed or has a recorded answer; each revisit entry has the
  user's verdict, written in the entry.
- [ ] **M9.2** — R8 release build. `assembleFdroidRelease` and `assembleGplayRelease` build with
  minify and resource shrinking on (never run so far; `androidApp/proguard-rules.pro` is empty).
  Sign the `fdroidRelease` APK by hand with the debug key (`apksigner`, no build change) and run
  login, browse and add item on the emulator against the local test instance. Add a keep rule
  only for what R8 breaks.
  **Verify:** both release tasks pass; the hand-signed APK passes the flow on the emulator.
- [ ] **M9.3** — Store texts. `README.md` (what the app does, unofficial and not affiliated with
  Koillection, the Koillection version it was tested against, where to install it), a privacy
  policy (Play needs one; the app talks only to the user's own server), and
  `fastlane/metadata/android/en-US/` (title, short and full description, `changelogs/1.txt`,
  phone screenshots). Screenshots come from the local test instance, which is a copy of the
  user's real catalogue: the user approves what they show before they are committed.
  **Verify:** the user approves the texts and the screenshots.
- [ ] **M9.4** — Launcher icon, from the image the user provides (waits for it). Adaptive icon
  with a monochrome layer, plus the 512px `fastlane/.../images/icon.png`.
  **Verify:** the icon shows on the emulator launcher, light and dark.
- [ ] **M9.5** — Release signing (waits for the user's keystores). Port wallosmobile's
  per-flavor signing configs: keystore files at the repo root (gitignored), passwords from env
  vars. Turn off the APK dependency-info block, which F-Droid's scanner rejects. Confirm with the
  user whether wallosmobile's signed `fdroidDebug` channel is wanted here too.
  **Verify:** `apksigner verify --print-certs` shows the right certificate for each flavor; the
  `fdroid` APK has no extra signing block.
- [ ] **M9.6** — Release workflow. Port wallosmobile's `release.yml`: a `v*` tag builds the signed
  APKs and the `gplay` AAB and attaches them to a GitHub release. Ask the user before this step
  whether the branch flow switches now (ground rules: `dev`/`main` + PRs near release), because
  wallosmobile's `release-prepare`/`release-finalize` need it.
  **Verify:** a manual run on a pre-release tag attaches signed artifacts that pass `apksigner`.
- [ ] **M9.7** — First release `0.1.0` (version code 1). Tag it, then the IzzyOnDroid inclusion
  request and the Play upload (internal testing first). Each outward step needs the user's go.
  **Verify:** the GitHub release exists; the IzzyOnDroid request and the Play upload are done or
  handed to the user with what they need.
