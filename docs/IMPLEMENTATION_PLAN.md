# koillectionmobile implementation plan

This is the *why* document. For the *what next*, see `docs/CHECKLIST.md`. If the two ever
disagree, `CLAUDE.md` (root) wins over both.

## 1. Goal

A mobile client for a user's own Koillection server. Browse collections and items, and add
items from the phone: camera photos, text fields, tags.

The phone adds what the web UI can't: the camera, scanning at a shelf, and a quick look
while away from the PC.

**First use case: the user's book catalogue.** Today it runs as Python scripts in
`~/claude/books/00service/` (`koi.py`, `add_book.py`, and that folder's `CLAUDE.md` for the
workflow). The phone photos go to the PC, then a script creates the item over the API. The
app should do the "create item + text fields + main image + page images + tags" part on
the phone. Those scripts prove the API supports it; read them as a worked example of the
calls, not as a spec.

Not in scope (for now): server administration, the web UI's statistics/history pages,
scrapers.

## 2. The API (Koillection 1.8.5, checked 2026-11-14)

The Koillection source at `../../koillection` is the source of truth. Re-check it at the
start of any step that touches the API. Facts that shape the design:

- API Platform. JSON-LD by default (`application/ld+json`), plain JSON also accepted. Lists
  come back as Hydra collections, 30 entries per page. On 1.8.5 the keys have no `hydra:`
  prefix: `member`, `totalItems`, and `view` with `first`/`last`/`previous`/`next` links.
  The last page has no `next`; a list that fits on one page has no `view` at all (checked
  2026-11-15 against the M1.1 fixtures). Older servers may still send `hydra:member`.
  `core:api`'s `HydraCollectionDto` reads both key sets; `getAllHydraPages` requests each `next`
  link as it is, because it is an absolute path that already holds the server's prefix.
- Error bodies come in two shapes. The JWT layer sends `{"code": <status>, "message": "..."}`:
  a wrong password and an unknown user both give 401 `Invalid credentials.`, a missing token
  gives 401 `JWT Token not found`. API Platform sends `application/problem+json` with
  `title`, `detail` and `description` (404 `Not Found`; 422 adds `violations`). Checked
  2026-11-15 against the local test instance; `core:api` reads `message`, then `detail`.
- Send `Accept: application/ld+json`. A plain `application/json` list is a bare array, without
  the Hydra paging keys. Ktor's client serializes a request body only when the request sets
  its content type, so every call with a body sets one (`core:api`'s `ContentTypeLdJson`, or
  `ContentTypeMergePatchJson` for `PATCH`).
- The server publishes OpenAPI 3.1 at `GET /api/docs` with `Accept:
  application/vnd.openapi+json` (copy in `fixtures/koillection-1.8.5/openapi.json`).
- **DTOs are hand-written** (decided M1.1, 2026-11-15). The OpenAPI doc is 3.1 (`type:
  ["string","null"]`, Hydra `allOf` wrappers), which Kotlin generators handle poorly, and the
  app reads a small subset of about six resources. Use the OpenAPI doc as the field
  reference instead.
- Login: `POST /api/authentication_token` with `{"username","password"}` → `{"token": JWT}`.
  Then `Authorization: Bearer <token>`. The body may be `application/json` or
  `application/ld+json`; the answer is plain `application/json`. A wrong subpath gets
  Koillection's own 404 (`problem+json`, `detail: Not Found`), and a `GET` gets 405 (checked
  2026-11-15 against the local test instance). `feature:auth` reads 404, 405 and a 2xx without
  `token` as "not a Koillection server".
- The JWT lives 1 hour (lexik's default `token_ttl`; Koillection's
  `config/packages/lexik_jwt_authentication.yaml` does not set it). There is no refresh token.
  An expired JWT gives 401 `{"code":401,"message":"Expired JWT Token"}` (checked 2026-11-15
  against the local test instance). So the app stores the password and logs in again on a 401
  (`core:api` `bearerAuth`, M1.3).
- `PATCH` takes `application/merge-patch+json`.
- Resources with `#[ApiResource]`: Item, Collection, Datum (an item's custom field), Tag,
  TagCategory, Template, ChoiceList, Album, Photo, Wishlist, Wish, Loan, Inventory, Log,
  User, DisplayConfiguration, Field, Error.
- Uploads are `multipart/form-data` POSTs to a sub-path: `/api/items/{id}/image` (field
  `file`), `/api/data/{id}/image` (field `fileImage`), also `/data/{id}/file` and
  `/data/{id}/video`.
- Collections nest, and `GET /api/collections` returns every level: `Collection.php` has no
  `#[ApiFilter]`, so there is no `parent` filter. A top-level list is a client-side filter on a
  null `parent` (an IRI, not an object). `image` is a path relative to the server root
  (`uploads/<user>/<file>`), as on items. Checked 2026-11-15 (M1.6).
- Items (checked 2026-11-15, M1.7): `image`, `imageSmallThumbnail` and `imageLargeThumbnail`
  on an item and on a Datum are paths relative to the server root. The server serves `/uploads/*`
  as static files (`docker/Caddyfile`), so an image URL needs no JWT. `feature:items` resolves
  the paths to full URLs. `GET /api/items/{id}/data` has no guaranteed `position` order: the
  `OrderBy` on `Item::$data` is a Doctrine collection setting, and API Platform reads the Datum
  rows with its own query (`Datum.php` sets no `order`). So `feature:items` sorts by `position`.
  Not checked against a server with reordered fields: the fixture is already in `position` order.
- Tags (checked 2026-11-15, M1.8): `GET /api/tags`, `/api/tag_categories` and
  `/api/items/{id}/tags` are Hydra lists. A tag's `category` is an IRI or null. A tag has
  `image` and `imageSmallThumbnail` only (no large thumbnail); `feature:tags` does not read them.
  `TagCategory.label` is nullable in the entity, and `color` is hex without `#`.
- Feature repositories return `Result` (`resultOf` from grappim-kit `domain`, as in
  wallosmobile); the failure is the `KoillectionError` that `core:api` threw. `feature:auth` is
  the exception: its `LoginResult` names each login outcome.
- Item tags: the API takes only the full list, so changing tags means sending all of them.
- Known server-side trap: Koillection's Docker entrypoint can regenerate `JWT_PASSPHRASE` on
  restart, which breaks API login with a 500 while the website keeps working. Upstream issues
  #1600, #1400, #1410. The app should show a clear message for a 500 at login, not a
  generic error.

## 3. Prior art

**Goshiki** (`github.com/HumboShot/koillection-android`), found 2026-11-14: a native Android
client, Kotlin, MIT, active (last commit 2026-10-18), released on Google Play. The user
chose to build this app anyway. Reasons: KMP (Android + iOS), F-Droid, grappim-kit, and the
book-photo workflow. Its README's JWT troubleshooting is a useful reference. Don't copy its
code without checking the licence terms are kept.

## 4. Reference projects

- **`../wallosmobile`**: the closest model, a client for another self-hosted service.
  Source of `build-logic`, the Ktor client setup, server-URL login, and the F-Droid/Play
  flavors.
- **`../deskmate`** and **`../wayprint`**: the most recent from-scratch bootstraps. Their
  `docs/CHECKLIST_ARCHIVE.md` M0 is the model for this app's M0.
- **`../grappim-kit`**: shared KMP library on Maven Central (`io.github.grigoriym:grappim-kit-*`).
  Read `grappim-kit/CONSUMING.md` before adding a module. Use one `grappimKit` version key
  for all of them. Likely modules: logger, coroutines, uikit, storage (the token and password),
  customheaders (reverse proxies), testing.
- **`../agentic-grappim`**: shared skills and `templates/CLAUDE.md.template`.

## 5. Open decisions (settle in the step that needs them)

1. **Targets** — *decided 2026-11-14*: same as TaigaMobileNova. Android is the shipped app;
   KMP library modules also declare `jvm()`, `iosArm64()`, `iosSimulatorArm64()` (in
   `configureKmp()`, as Taiga does) so `commonMain` stays platform-clean and `jvmTest` is the
   fast test/coverage target. No `iosApp` / desktop app until the user asks for one.
2. **Distribution** — *decided 2026-11-14*: both. Keep the `store` flavor dimension
   (`gplay`, `fdroid` with `.fdroid` suffix) from wallosmobile/Taiga; IzzyOnDroid first.
3. **TLS** — *answered 2026-11-15 (M1.1)*: the user's Tailscale `ts.net` server passes
   Python's default certificate checks, so `koi.py`'s check-off was not needed. The app uses
   normal platform TLS. No custom or self-signed certificate support until a user needs it.
4. **Module layout** — *decided 2026-11-14*: wallosmobile-style full split. `core:api`,
   `core:domain`, `core:storage` (decision 6), `strings`, and `feature:<x>:{data,domain,dto,mapper,ui}` for `auth`,
   `collections`, `items`, `tags`. The mapper exposes `dto` + `domain` as `api`; `data`
   reaches both through it. No local `uikit` module: `ui` modules use `grappim-kit-uikit`.
5. **MVP scope** (start of M2): proposed minimum is login, collection list, item list, item
   detail, and "add item with photos" into a chosen collection.
6. **Session storage module** — *decided 2026-11-15 (M1.3)*: a separate `core:storage`, as in
   wallosmobile and TaigaMobileNova. It holds `SessionStorage` (server URL, username, password,
   JWT) and the `CustomHeadersStorage` binding, in one DataStore file with one `SecretCipher`.
   `core:api` depends on it, and M2's login and settings screens can read the session without
   the HTTP client. The DataStore path and the cipher are per platform (`expect class
   PlatformStorageModule`, TaigaMobileNova's pattern): Android Keystore on Android,
   `NoopSecretCipher` (plain text) on JVM and iOS, neither of which is shipped.
