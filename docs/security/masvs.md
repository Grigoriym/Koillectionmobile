# MASVS register

Profile: Android only (plan §5 decision 1; the iOS and JVM targets are not shipped) ·
self-hosted, user-supplied Koillection server · reviewed 2026-11-16 (M4.5), all MASVS v2
categories, from source only.

Sensitive assets: the Koillection password and JWT (stored, for the automatic re-login), the
custom header values (proxy credentials, M4.2), and the user's catalogue (images and text, read
from the server).

Out of scope:
- **MASVS-RESILIENCE** (1–4). Anti-tamper and obfuscation protect a vendor's assets against the
  device owner. This is a FOSS client for a server the same user owns, and the app holds no
  secret of its own (no client id, no API key in source or `gradle/libs.versions.toml`).
  Reproducible builds would matter more, and MASVS does not cover them.
- **iOS and JVM.** The JVM target is outside MASVS. iOS is not shipped; its storage is plain
  text today (see Accepted deviations).

Library code: `grappim-kit` 0.1.8 (`KeystoreSecretCipher`, `CustomHeaders`,
`CustomHeadersStorageImpl`) was read from the `../grappim-kit` checkout at `VERSION_NAME=0.1.8`,
not from the published jar. Gradle's cache holds no sources jar for it.

## Status per control

Status: **Pass (source)** = the code meets it, as far as source can show; **Accepted** = a
deviation in the next table; **Open** = in the Open table; **N/A** = does not apply, with the
reason. No row here is a claim of compliance: see "Needs a device or an APK".

| Control | Status | Evidence |
|---|---|---|
| MASVS-STORAGE-1 | Accepted | Password and JWT stored, Keystore-encrypted (`SessionStorageImpl.kt:52-65`). |
| MASVS-STORAGE-2 | Accepted | `allowBackup="true"`, no extraction rules. No logger in any build: `KitLogger` is never installed, so the Ktor log level is `NONE` (`NetworkModule.kt:63`); the logger also redacts the password and any JWT (`RedactingLogger.kt`). |
| MASVS-CRYPTO-1 | Pass (source) | AES/GCM/NoPadding, 128-bit tag, 12-byte IV from the cipher (`KeystoreSecretCipher.kt:41-46`). `setRandomizedEncryptionRequired(false)` is never called, so the Keystore enforces a fresh IV per encryption. Key size is the provider's default (not set in source). |
| MASVS-CRYPTO-2 | Pass (source) | Key generated in `AndroidKeyStore`, never exported; app-specific alias `koillection_session_key` (`PlatformStorageModule.android.kt:12,25`). |
| MASVS-AUTH-1 | Accepted | Koillection's own JWT login (`POST api/authentication_token`), no third-party login, no WebView. The stored password exists for the re-login on a 401 (`BearerAuth.kt:72-84`). |
| MASVS-AUTH-2 | N/A | The app has no app lock or biometric gate. |
| MASVS-AUTH-3 | N/A | No step-up operation. The server authorizes every request. |
| MASVS-NETWORK-1 | Accepted | Cleartext allowed for every host, with a warning at login (M4.4). Platform TLS only: no `TrustManager`, `HostnameVerifier` or `SSLContext` in any source set. |
| MASVS-NETWORK-2 | N/A | The server is user-supplied, not under the developer's control. |
| MASVS-PLATFORM-1 | Pass (source) | One exported component: the launcher `MainActivity`, which reads no intent data (`MainActivity.kt`). `FileProvider` is not exported and serves `cache/photos/` only (`photo_paths.xml`). The one `startActivity` is an explicit settings intent (`LocalNetworkPermissionRequest.android.kt:75`). No deep links. |
| MASVS-PLATFORM-2 | N/A | No `WebView` in any source set. |
| MASVS-PLATFORM-3 | Accepted | No `FLAG_SECURE`. The password field is always masked (`LoginScreen.kt:134`); there is no reveal toggle. |
| MASVS-CODE-1 | Accepted | `minSdk = "24"` (`gradle/libs.versions.toml:9`). |
| MASVS-CODE-2 | Accepted | No update check on either flavor. |
| MASVS-CODE-3 | Pass | `renovate.json` with `osvVulnerabilityAlerts` (M4.5). The user installed the Renovate GitHub app on 2026-11-16; its first run opened the Dependency Dashboard (issue #2) and PR #1, with no config error. GitHub's own vulnerability alerts are off. |
| MASVS-CODE-4 | Pass (source) | `Json { ignoreUnknownKeys = true }` (`NetworkModule.kt:42`). Error bodies that are not JSON read as no message (`ErrorMapping.kt:40`, `ProxyRejection.kt:35`). Server text is shown as plain `Text`; the app opens no server-supplied URL (no `LocalUriHandler`/`openUri`). Image URLs are the server URL plus the server's path (`CollectionMapper.kt:20`, `ItemMapper.kt`), so they stay on the server's host. |
| MASVS-RESILIENCE-1..4 | Out of scope | See above. |
| MASVS-PRIVACY-1 | Open | `ACCESS_NETWORK_STATE` is declared (`androidApp/src/main/AndroidManifest.xml:5`); no app code uses it. |
| MASVS-PRIVACY-2 | Pass (source) | No analytics, ad id, crash reporter or telemetry dependency (grep of `*.kts`/`*.toml`). The app talks to the user's server only. |
| MASVS-PRIVACY-3 | Pass (source) | Nothing is collected. The privacy policy is M14.3. |
| MASVS-PRIVACY-4 | Open | Log out clears the username, password and JWT (`SessionStorageImpl.kt:67-73`), and since M14.14 the image loader's memory and disk caches (`DrawerViewModel.logOut`; on the emulator only Coil's `journal` stays in `cache/coil3_disk_cache/`). Still open: a session that ends because the server rejects the re-login (`BearerAuth.kt:80`) keeps the image cache. |

## Accepted deviations

The STORAGE-2 (`allowBackup`), PLATFORM-3, CODE-1 and CODE-2 rows: accepted by the user, 2026-11-16 (M4.5).

| Control | What we do instead | Bound | Why |
|---|---|---|---|
| MASVS-STORAGE-1 | The password is stored, not only the JWT. | Keystore AES/GCM (`KeystoreSecretCipher`), as is the JWT. Cleared at log out and when the server rejects the re-login (`BearerAuth.kt:80`). | The JWT expires and Koillection has no refresh token; the stored password gives the silent re-login (plan §5 decision 6). |
| MASVS-STORAGE-1 | iOS and JVM store the password, JWT and custom headers as plain text (`NoopSecretCipher`, `PlatformStorageModule.ios.kt:38`, `PlatformStorageModule.jvm.kt:26`). | Neither target ships. The iOS file says a Keychain cipher must replace it before an iOS app exists (`PlatformStorageModule.ios.kt:18-19`). | Plan §5 decisions 1 and 6. |
| MASVS-STORAGE-2 | `android:allowBackup="true"` (`AndroidManifest.xml:12`), no `dataExtractionRules`/`fullBackupContent`. | The password, the JWT and the custom headers are ciphertext in the DataStore file; the Keystore key does not travel with a backup, so a restore decrypts to nothing and the user logs in again (`KeystoreSecretCipher.kt:70-74`). The backup carries the server URL and the username as plain text. The photo drafts and the image cache are in `cacheDir`, which backup skips. | The settings and session are one DataStore file, so a rule can only drop all of it; the secrets in it are already useless off the device. |
| MASVS-NETWORK-1 | `android:usesCleartextTraffic="true"` for every host (`AndroidManifest.xml:16`), no `networkSecurityConfig`. | The password (login body) and the JWT (header) do travel over plain HTTP to an `http://` server. The login screen warns for every `http://` URL (M4.4, `LoginUiState.isCleartextWarningVisible`). Custom headers go only to the server's own scheme, host and port (`CustomHeaders.isServerOrigin`), also for images: Coil's fetcher uses its own client with the same plugin (`ImageHttpClient.kt`, M14.13). The client follows no redirect (`KoillectionHttpClient.kt:52`), so neither the JWT nor a header leaves the server's origin. | Self-hosted Koillection often runs plain HTTP on a LAN; the test instance does. User's choice in M4.4 (advisory only). |
| MASVS-PLATFORM-3 | No `FLAG_SECURE`. | The password is always masked; no screen shows the JWT or a header value in clear except Settings, where the user typed the header. The recents thumbnail can show the catalogue and the custom header list. | The catalogue is the user's own and is not secret; `FLAG_SECURE` also blocks the user's own screenshots. |
| MASVS-CODE-1 | `minSdk = "24"` (Android 7.0). | Every API the app uses for secrets (`KeyGenParameterSpec`, AES/GCM in `AndroidKeyStore`) is API 23+. | Set in M0.1 (`6ed6e43`, the build-logic port) and never changed. No doc states a reason of its own; the user's verdict is the reason. |
| MASVS-CODE-2 | No forced or prompted update. | F-Droid and IzzyOnDroid users update through their store client; Play users through Play. The server API is versioned by Koillection, not by the app. | Self-hosted FOSS client. Revisit if a server change ever needs a minimum app version. |

## Open

| Control | Finding | Where | Severity |
|---|---|---|---|
| MASVS-PRIVACY-4 | The drawer's Log out clears the image caches (M14.14). A session that ends because the server rejects the silent re-login does not: the app shows the login screen, and a login to another server keeps the first server's images until Android clears the cache. | `BearerAuth.kt:80` (`core:api` has no `ImageLoader`) | Low |
| MASVS-PRIVACY-1 | `ACCESS_NETWORK_STATE` declared but unused by app code. A library may use it (Coil's network fetcher checks connectivity when the permission is granted): check the merged manifest before removing it. | `androidApp/src/main/AndroidManifest.xml:5` | Low |

Each Open row has a `docs/revisit.md` entry.

## Needs a device or an APK

| Control | Check | Why source can't answer it |
|---|---|---|
| MASVS-PRIVACY-1, PLATFORM-1, STORAGE-2 | The merged release manifest: permissions and components the libraries add, `allowBackup`, `exported`. | Manifest merging happens at build time (M14.2 builds the release APK). |
| MASVS-STORAGE-2 | A backup and restore (`bmgr`) logs the user out and carries no readable secret. | Backup contents need a device. |
| MASVS-STORAGE-2 | No app log lines in logcat from a release build. | Source shows no logger is installed; a library could still log. |
| MASVS-CRYPTO-1 | The generated key's size (`KeyInfo.getKeySize()`) and whether it is hardware-backed. | The provider default sets it, not the app. |
| MASVS-PLATFORM-3 | What the recents thumbnail shows on Login and Settings. | Needs a device screenshot. |
| MASVS-NETWORK-1 | TLS behaviour on the wire against an `https://` server with a bad certificate (expect a refusal). | Needs a TLS front with a bad certificate. |
| All | `grappim-kit` 0.1.8 as published equals the checkout read here. | No sources jar in the Gradle cache. |
| All | What R8 leaves in the release APK. | M14.2 builds it first. |
