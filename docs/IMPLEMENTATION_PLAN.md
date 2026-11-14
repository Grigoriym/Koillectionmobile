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
  come back as Hydra collections: entries under `member` (or `hydra:member`), paginated.
- The server publishes OpenAPI docs (`jsonopenapi` docs format). Worth checking in M1
  whether generating DTOs from it beats writing them by hand.
- Login: `POST /api/authentication_token` with `{"username","password"}` → `{"token": JWT}`.
  Then `Authorization: Bearer <token>`.
- `PATCH` takes `application/merge-patch+json`.
- Resources with `#[ApiResource]`: Item, Collection, Datum (an item's custom field), Tag,
  TagCategory, Template, ChoiceList, Album, Photo, Wishlist, Wish, Loan, Inventory, Log,
  User, DisplayConfiguration, Field, Error.
- Uploads are `multipart/form-data` POSTs to a sub-path: `/api/items/{id}/image` (field
  `file`), `/api/data/{id}/image` (field `fileImage`), also `/data/{id}/file` and
  `/data/{id}/video`.
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
3. **TLS**: the user's server is reached over Tailscale. `koi.py` turns off certificate
   checks. Check whether that was needed at all (Tailscale `ts.net` certs are normally
   valid) before the app supports custom or self-signed certificates.
4. **Module layout** — *decided 2026-11-14*: wallosmobile-style full split. `core:api`,
   `core:domain`, `strings`, and `feature:<x>:{data,domain,dto,mapper,ui}` for `auth`,
   `collections`, `items`, `tags`. The mapper exposes `dto` + `domain` as `api`; `data`
   reaches both through it. No local `uikit` module: `ui` modules use `grappim-kit-uikit`.
5. **MVP scope** (start of M2): proposed minimum is login, collection list, item list, item
   detail, and "add item with photos" into a chosen collection.
