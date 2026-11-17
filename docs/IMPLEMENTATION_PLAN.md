# koillectionmobile implementation plan

This is the *why* document. For the *what next*, see `docs/CHECKLIST.md`. If the two ever
disagree, `CLAUDE.md` (root) wins over both.

## 1. Goal

A mobile client for a user's own Koillection server. Browse collections and items, and add
items from the phone: camera photos, text fields, tags.

The phone adds what the web UI can't: the camera, scanning at a shelf, and a quick look
while away from the PC.

**First use case: the user's book catalogue.** Today the phone photos go to the PC, then a
script creates the item over the API. The app should do the "create item + text fields + main
image + page images + tags" part on the phone. The scripts (`koi.py`, `add_book.py`) are not
available; the user does not know them (2026-11-16). Where this plan cites them, it is history.

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
  (`core:api` `bearerAuth`, M1.3). The login request must go without the bearer header: with
  an expired JWT the server answers the login itself with 401 (checked 2026-11-15, M3.1).
- Proxy rejection (M4.3, checked 2026-11-16 against 1.8.5 and an nginx front). Every Koillection
  401 comes from the JWT layer (lexik `JWTAuthenticationFailureResponse`): `application/json`,
  `WWW-Authenticate: Bearer`, body `{"code":401,"message":...}`. nginx's `return 401` and its
  `auth_basic` 401 send an HTML page. The API does not redirect. So `core:api`
  (`isProxyRejection`) reads as a proxy: a 401 without that JSON body, any 407, and a 3xx to
  another host. It throws `KoillectionError.ProxyRejection`, and `bearerAuth` does not log in
  again on it. The client follows no redirects (`followRedirects = false`); a 3xx on the same
  host is a plain `KoillectionError.Http`. An `auth_basic` front over the whole server is not
  supported: the JWT also travels in `Authorization` (user, 2026-11-16).
- `PATCH` takes `application/merge-patch+json`.
- Resources with `#[ApiResource]`: Item, Collection, Datum (an item's custom field), Tag,
  TagCategory, Template, ChoiceList, Album, Photo, Wishlist, Wish, Loan, Inventory, Log,
  User, DisplayConfiguration, Field, Error.
- Uploads are `multipart/form-data` POSTs to a sub-path: `/api/items/{id}/image` (field
  `file`), `/api/data/{id}/image` (field `fileImage`), also `/data/{id}/file` and
  `/data/{id}/video`. Checked 2026-11-15 (M1.10): the server takes `image/png`, `image/jpeg`,
  `image/webp` and `image/avif` (`Assert\Image` on `Item::$file`, `Datum::$fileImage`), and the
  OpenAPI doc answers 201 with the updated resource. The server checks free space per user
  (`HasEnoughSpaceForUpload`). `feature:items` sends one Ktor `ByteArray` part; Ktor reads the
  bytes again on each send, so the `bearerAuth` repeat after a 401 sends the whole body (tested).
  Sent to the local test instance in M3.1 (see "Writes checked live" below). A full per-user quota (`diskSpaceAllowed`, 512 MiB by
  default) makes an image upload fail with an empty 500 while text fields still work; the user
  hit it on the real server on 2026-11-01 (`~/claude/books/00service/CLAUDE.md`, API gotchas).
- Collections nest, and `GET /api/collections` returns every level: `Collection.php` has no
  `#[ApiFilter]`, so there is no `parent` filter. A top-level list is a client-side filter on a
  null `parent` (an IRI, not an object). `image` is a path relative to the server root
  (`uploads/<user>/<file>`), as on items. Checked 2026-11-15 (M1.6). A collection has one image
  (resized to at most 200x200) and no thumbnails. `feature:collections` resolves the path to a
  full URL, as `feature:items` does (M2.3).
- Items (checked 2026-11-15, M1.7): `image`, `imageSmallThumbnail` and `imageLargeThumbnail`
  on an item and on a Datum are paths relative to the server root. The server serves `/uploads/*`
  as static files (`docker/Caddyfile`), so an image URL needs no JWT (a `curl` without a token
  got 200, M2.3). `feature:items` resolves
  the paths to full URLs. `GET /api/items/{id}/data` has no guaranteed `position` order: the
  `OrderBy` on `Item::$data` is a Doctrine collection setting, and API Platform reads the Datum
  rows with its own query (`Datum.php` sets no `order`). So `feature:items` sorts by `position`.
  Not checked against a server with reordered fields: the fixture is already in `position` order.
- `GET /api/items` (checked live 2026-11-17, M7.2) lists every item of the user: 30 per page, a
  `view.next` link per page, 15 pages for the 445 items of the test instance. Two full reads gave
  445 distinct ids in the same order. An item's `collection` is an IRI only, so the search screen
  reads `GET /api/collections` for the titles.
- Read side checked live 2026-11-15 (M1.11, `./gradlew :composeApp:smokeRun`, JVM, local test
  instance at the root path): login, all collections, 445 items of one collection over every page
  (matches the server's `totalItems`, so the `next` links work), one item and its 23 fields. A
  resolved image URL returned 200 `image/jpeg` without a JWT. Not checked: a server behind a
  subpath.
- Android HTTP engine (checked 2026-11-15, M2.2, emulator against the local test instance): Ktor's
  engine autodiscovery works. `HttpClient {}` without an engine picks OkHttp, the only engine on
  the Android classpath (`KmpNetworkConventionPlugin`). A wrong password got the server's 401 and a
  correct one logged in, over plain HTTP (`usesCleartextTraffic`).
- Tags (checked 2026-11-15, M1.8): `GET /api/tags`, `/api/tag_categories` and
  `/api/items/{id}/tags` are Hydra lists. A tag's `category` is an IRI or null. A tag has
  `image` and `imageSmallThumbnail` only (no large thumbnail); `feature:tags` does not read them.
  `TagCategory.label` is nullable in the entity, and `color` is hex without `#`.
  `GET /api/items/{id}/tags` is not sorted by label, though `Item::$tags` has `OrderBy(label)` and
  the web UI shows that order (same cause as `position` on Datum). The item screen sorts by label,
  ignoring case (M2.5). Whether Postgres sorts case-insensitively for the web UI is not checked.
- Feature repositories return `Result` (`resultOf` from grappim-kit `domain`, as in
  wallosmobile); the failure is the `KoillectionError` that `core:api` threw. `feature:auth` is
  the exception: its `LoginResult` names each login outcome.
- Item tags: the API takes only the full list, so changing tags means sending all of them.
- Writes (checked 2026-11-15 against the 1.8.5 source, M1.9; sent live in M3.1, see the next bullet):
  `POST /api/items` needs `name` and `collection` (`item:write`, both `NotBlank`).
  `POST /api/data` needs `item` (or `collection`), `type` and `label`; a label already used on
  the item is rejected (`DatumLabelNotExistsInParent`). An `image` field's `value` must be null
  (`Assert\IsNull`), so the app leaves the key out. `PATCH /api/items/{id}` with `{"tags": [...]}`
  replaces the tag set: `Item` has `addTag`/`removeTag`, so the serializer adds and removes to
  match the list. A body names other resources by IRI; the app sends `/api/...` without the
  server's subpath, as `add_book.py` does. Behind a subpath this is unchecked (`docs/revisit.md`).
  `POST /api/collections` needs only `title` (`collection:write`, `NotBlank`); `parent` is an
  optional IRI, and `ColorListener` picks the colour. The image goes up as multipart part `file`
  to `POST /api/collections/{id}/image`; `PATCH` with `{"deleteImage": true}` removes it
  (`UploadListener`). The `Assert\Image` MIME check on `Collection::$file` sits in group
  `colletion:image` (a typo in 1.8.5), so the server may not run it: an unchecked type can pass.
  Read from the 1.8.5 source in M6.1; sent live in M6.2, see the next bullet.
- Collection writes checked live 2026-11-16 (M6.2, `./gradlew :composeApp:smokeCollectionRun`,
  JVM, local test instance at the root path), through the M6.1 repository calls: a top-level
  collection and a child (`parent` IRI) were created; the server picked a `color` for each. The
  parent's `/children` listed the child. A rename was read back and kept the parent. A `jpeg`
  image upload got a file; a second upload got a new file name, and the old file gave 404.
  `PATCH` with `deleteImage` answered `image: null`, and the file gave 404. A `DELETE` of the
  parent took the child and an item in the child with it: all three gave 404. A create with an
  empty title got 422 (`title: This value should not be blank.`). Not checked: a server behind a
  subpath, and whether a non-image file passes the upload (the `colletion:image` group typo).
- Tag writes checked live 2026-11-17 (M7.4, `./gradlew :composeApp:smokeTagRun`, JVM, local test
  instance at the root path), through the M7.3 repository calls: `POST /api/tags` with `label` only
  answered a tag with no category. `PATCH /api/items/{id}` with the item's three tags plus the new
  one gave the item four tags, and `GET /api/tags/{id}/items` listed exactly that item. A `PATCH`
  with the three old tags took the new one off, and the tag's items were empty after. A create with
  an empty label got 422 (`label: This value should not be blank.`). A create with a label that
  exists already succeeded: the server made a second tag with the same label and a new id. `DELETE
  /api/tags/{id}` answered 204, and a read after it gave 404. Not checked: a server behind a subpath.
- Counting what a collection delete takes (M6.3, 1.8.5 source): the API has no counters.
  `Collection::$cachedValues` holds them but has no `Groups`, and `Item.php` has no `#[ApiFilter]`,
  so items cannot be counted for several collections in one call. The app reads all collections
  once, finds the descendants by `parentId`, and reads `totalItems` of the first page of
  `/api/collections/{id}/items` for each one. Checked on the device against the local test instance.
- Writes checked live 2026-11-15 (M3.1, `./gradlew :composeApp:smokeWriteRun`, JVM, local test
  instance at the root path): through the app's repositories, an item in `Books`, two text fields
  (positions 1 and 2), an image field (position 3), a main image upload and a field image upload
  (`image/jpeg`), and two existing tags. The read-back matched: fields in `position` order, the
  image URLs, both tags. An image narrower than the server's thumbnail width gets no thumbnails,
  so the thumbnail URLs equal the image URL (`ThumbnailGenerator` returns early). `DELETE
  /api/items/{id}` answered 204; after it the item, its fields and both uploaded files gave 404.
  Not checked: a server behind a subpath.
- Upload quota checked live 2026-11-15 (M3.6, emulator, local test instance): with
  `disk_space_allowed` at 1 byte, `POST /api/items` and the text fields worked and the main-image
  upload got HTTP 500 (the `HasEnoughSpaceForUpload` violation, not a 422). The add-item form names
  the quota for a 500 at an image upload only.
- Edit and delete (read in the 1.8.5 source 2026-11-16, start of M5; sent live in M5.2, see the
  next bullet):
  `Item` and `Datum` both have `Patch` and `Delete`. A new upload to `/api/items/{id}/image` or
  `/api/data/{id}/image` deletes the old file (`ImageHandler::upload` calls `removeOldFile`). A
  deleted item or field takes its files with it (`UploadListener::postRemove`); `Item::$data` has
  `orphanRemoval`. `Item`'s `#[Upload]` has no `deleteProperty`, so the API cannot remove a main
  image. `DatumLabelNotExistsInParentValidator` skips the field itself, so a `PATCH` that keeps
  a label passes; a label used by another field of the item fails. The compare is exact (`===`,
  re-read M5.4): `Author` and `author` are two labels. The test instance's items hold only `text`
  and `image` fields (4007 and 1577).
- Edits checked live 2026-11-16 (M5.2, `./gradlew :composeApp:smokeWriteRun`, JVM, local test
  instance at the root path), through the M5.1 repository calls: a rename, a field `label` and
  `value` change, and a `value`-only change were read back as sent. A new main image and a new
  field image each got a new file name; the old file gave 404 and the new one 200 (no JWT). After
  `DELETE /api/data/{id}` the field's file gave 404. `DELETE /api/items/{id}` answered 204 and the
  item then gave 404.
- Duplicate label (checked live 2026-11-16, M5.2): a `PATCH /api/data/{id}` that gives a field the
  label of another field of the item gets **HTTP 500 with an empty body**, not a 422. The server
  log shows the validator's `ValidationException` ("label is used multiple times"), then a
  `UniqueConstraintViolationException` on `koi_datum_pkey` while it handles that exception. The
  field keeps its old label. A `POST /api/data` with a duplicate label gets the normal 422 with
  `violations`. So the app cannot tell a label conflict on an edit from any other server failure:
  the edit form must catch duplicate labels before it sends.
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
   Confirmed at the start of M4 (user, 2026-11-16): both, as in the user's other apps.
   The launcher icon and the release keystores come from the user.
3. **TLS** — *answered 2026-11-15 (M1.1)*: the user's Tailscale `ts.net` server passes
   Python's default certificate checks, so `koi.py`'s check-off was not needed. The app uses
   normal platform TLS. No custom or self-signed certificate support until a user needs it.
4. **Module layout** — *decided 2026-11-14*: wallosmobile-style full split. `core:api`,
   `core:domain`, `core:storage` (decision 6), `strings`, and `feature:<x>:{data,domain,dto,mapper,ui}` for `auth`,
   `collections`, `items`, `tags`. The mapper exposes `dto` + `domain` as `api`; `data`
   reaches both through it. No local `uikit` module: `ui` modules use `grappim-kit-uikit`.
   M3.2 (2026-11-15) added `core:image`: photo preparation for upload. It is not in
   `feature:items:data` because M3.5's preview in `feature:items:ui` needs it too, and a `ui`
   module does not depend on a `data` module.
   M6.4 (2026-11-16) added `core:photos` (user's choice): the camera and picker launchers, the
   photo preview, `PhotoFiles` and the `FileProvider`, moved out of `feature:items:ui`, so the
   collection form uses them too. `core:image` stays free of Compose.
   M3.3 (2026-11-15): `feature:items:domain` depends on `feature:tags:domain`, because
   `AddItemUseCase` sets the new item's tags.
5. **MVP scope** — *decided 2026-11-15 (start of M2)*: login, collection list, item list, item
   detail, and log out (M2); "add item with photos" into a chosen collection (M3). The user
   accepted the proposal as it was and added log out, so a user can change server without
   clearing the app data. After the M2.7 device pass the user added a top-bar back arrow on the
  collection and item screens (M2.8, 2026-11-15).
   Pre-release scope extended (user, 2026-11-16): network and security parity with
   wallosmobile (M4), edit/delete items (M5), collections CRUD (M6), search and tags (M7), and the
   other Koillection areas (M8). The release is M9.
   Changed 2026-11-17 (user, start of M8): one milestone per area, in this order: templates (M8),
   wishlists and wishes (M9), loans (M10), albums and photos (M11), inventories (M12), logs (M13).
   The release is M14. Writable areas get full CRUD with images; inventories are view and delete;
   logs are view only.
6. **Session storage module** — *decided 2026-11-15 (M1.3)*: a separate `core:storage`, as in
   wallosmobile and TaigaMobileNova. It holds `SessionStorage` (server URL, username, password,
   JWT) and the `CustomHeadersStorage` binding, in one DataStore file with one `SecretCipher`.
   `core:api` depends on it, and M2's login and settings screens can read the session without
   the HTTP client. The DataStore path and the cipher are per platform (`expect class
   PlatformStorageModule`, TaigaMobileNova's pattern): Android Keystore on Android,
   `NoopSecretCipher` (plain text) on JVM and iOS, neither of which is shipped.
7. **Add-item form** — *decided 2026-11-15 (start of M3)*: photos come from the system camera
   (intent, no CameraX) and the Android photo picker. The app applies EXIF orientation, and each
   photo has a manual rotate button; uploads are downscaled to 2400px JPEG q85, as `add_book.py`
   does. Text fields are free label/value rows: nothing book-specific in the app. Tags are picked
   from the server's existing tags only; the form cannot create one.
   Changed 2026-11-16 (user): the form gets tag creation in M7. Built in M7.6, see decision 10.
8. **Edit-item form** — *decided 2026-11-16 (start of M5)*: the edit screen is the M3 add form,
   prefilled from the server, and the save sends only what changed. The main image can be
   replaced but not removed: Koillection 1.8.5's `Item` upload has no delete property, so the API
   cannot clear it (the web form can). Image fields can be removed. No field reordering in M5:
   new fields go after the last one. A label swap between two fields is allowed: the save moves
   one field to a temporary label first (M5.4).
   Built in M5.5 as its own `EditItemViewModel` and route, with the add form's parts shared
   (`ItemFormParts.kt`), not as a mode of `AddItemViewModel`: the two saves have nothing in common.
9. **Collection form** — *decided 2026-11-16 (start of M6)*: the form edits the title and the
   image only. The server picks the color (`ColorListener`); visibility and parent stay as they
   are. The image can be set, replaced and removed: `Collection` has a `deleteImage` property,
   unlike `Item`. A delete removes the child collections and their items too (`cascade: ['all']`
   on `children` and `items` in `Collection.php`), so the confirm dialog says how many child
   collections and items go with it.
10. **Search and tags** — *decided 2026-11-17 (start of M7)*: Koillection 1.8.5 declares no
   `ApiFilter` on any entity (re-checked 2026-11-17), so search runs in the app, by item name. Two
   places: a filter on the collection screen over the items it already loaded, and a global search
   screen that reads every item (`GET /api/items`, all pages; about 15 requests for 445 items) once
   when it opens. The tag list groups tags by category, with the tags without a category last. A
   tag created from the add-item form gets a label only. `Tag.label` is `NotBlank` but not unique
   on the server, so the form selects an existing tag with the same label (ignoring case) instead
   of creating a second one. The edit form gets the same action (user, 2026-11-17, M7.6).
   Built in M7.6: the form keeps a new label as pending (saved state) and the save creates it just
   before it sets the item's tags (`findOrCreateTags`, `feature:tags:domain`). That call reads
   `GET /api/tags` first and uses a tag with the label (ignoring case), so a retry after a create
   that went through does not create a second tag. The label field takes at most 255 characters:
   `Tag.label` is a Doctrine `string` with no length check.
11. **Templates** — *decided 2026-11-17 (start of M8)*: full CRUD of templates, their fields and
   choice lists. A template field keeps its type in the item forms, with a plain input per type:
   a number keyboard for `number` and `price`, a switch for `checkbox`, a multi-select for
   `choice-list`, a text box for the other text types (no date picker, rating stars or country
   picker). A collection's `itemsDefaultTemplate` is in scope: the add form in that collection
   applies it when it opens.
