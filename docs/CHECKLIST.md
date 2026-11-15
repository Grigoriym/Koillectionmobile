# koillectionmobile checklist

**Current step:** M1.9 (M1.8 done 2026-11-15). M0 done 2026-11-14 (archived). Plan §5 decisions 1, 2, 4 and 6 are settled.

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

## M1 — API layer

Ktor client, server URL + login (JWT), token storage (grappim-kit `storage`), Hydra list
paging, DTOs for Collection / Item / Datum / Tag, multipart image upload. Fixture-tested
against real JSON captured from the local test instance (`docs/local-info.md`). No screens: those are M2.

Layout is plan §5 decision 4: shared plumbing in `core:api`, each resource in its
`feature:<x>:{dto,domain,mapper,data}`. Datum (an item's field) lives in `feature:items`.
Model for every step: `../wallosmobile/core/api` and its `feature:*:data` modules.

- [x] **M1.1** — Capture fixtures and settle two open questions. Fetch the server's OpenAPI
  doc and real responses for: one page of `/api/collections`, one collection, one page of
  `/api/collections/{id}/items`, one item, `/api/items/{id}/data`, one page of `/api/tags`,
  `/api/tag_categories`, and the login error bodies (wrong password → 401). Use `koi.py`
  (`~/claude/books/00service/`), GET only. Decide hand-written vs generated DTOs (plan §2) and
  answer plan §5 decision 3 (does the Tailscale cert validate without `koi.py`'s check-off?).
  **The GitHub repo is public:** ask the user how to trim or anonymise their data before any
  fixture is committed. Never commit a token.
  **Verify:** fixtures are in the repo, `grep` finds no JWT (`eyJ`) in them, and plan §2 / §5.3
  record both answers.
  Note: user asked for a test instance instead of capturing from the real server. A local
  Koillection 1.8.5 copy of the real catalogue (`docs/local-info.md`) is the source of the
  fixtures (`fixtures/koillection-1.8.5/`, script `fixtures/capture.py`), so no real ID is
  in the repo. Full responses kept, not trimmed.
- [x] **M1.2** — `core:api` client: Ktor `HttpClient` factory (wallosmobile `NetworkModule`),
  base-URL provider, `Json` (`ignoreUnknownKeys`, `application/ld+json` and
  `application/merge-patch+json`), redacting logger, custom headers (grappim-kit
  `customheaders`), and one error type that maps HTTP status and network failure. Koin module.
  **Verify:** `ktor-client-mock` tests pass (`jvmTest --rerun`), `KoinGraphTest` passes,
  `build ktlintCheck` green.
  Note: `configureKoillectionClient` (`HttpClientConfig` extension) holds the setup; the Koin
  provider applies it on Ktor's default engine, tests on `MockEngine`. Error type is
  `KoillectionError` in `core:domain`. `BaseUrlProvider` and `CustomHeadersStorage` have no
  binding yet: both need M1.3's storage. `KoinGraphTest` cannot see that gap (it reads
  `HttpClient`'s constructor, so `HttpClientEngine` is in its `extraTypes`, as in wallosmobile).
  Not run on a device: Ktor's engine autodiscovery on Android is first exercised in M1.11/M2.
- [x] **M1.3** — Session storage and bearer auth. Store server URL, username, password and JWT
  with grappim-kit `storage`; decide `core:api` vs a new `core:storage` module (M0.3 note) and
  write the answer in plan §5. Bind `core:api`'s `BaseUrlProvider` and grappim-kit
  `CustomHeadersStorage` (M1.2 left both unbound). Add `Authorization: Bearer`; on 401 log in again once with the
  stored password, then fail.
  **Verify:** tests prove the header is sent, one re-login on 401, and no loop on a second
  401. Hand-written fakes for storage.
  Note: new module `core:storage` (plan §5.6). `bearerAuth` (`core:api`) skips the login path;
  a failed re-login throws its own error, so a 500 there stays `Server` (plan §2 trap).
  Mutation-checked: a dropped header and a re-login loop each fail a `BearerAuthTest` case.
  `KoinGraphTest`'s `verify()` passed with `CustomHeadersStorage` unbound, so a second test
  now starts the JVM `KoinApp` and resolves `HttpClient`; it fails on that gap. Removing
  `PlatformStorageModule` from `AppModule.includes` did not fail it: the module's own
  `@Configuration` is found anyway on JVM. Android graph not checked (no device run).
- [x] **M1.4** — Hydra paging in `core:api`: a generic list DTO that reads `member` and
  `hydra:member`, `totalItems`, and the next-page link; a helper that follows pages.
  **Verify:** tests against the M1.1 list fixtures, including the last page (no next link).
  Note: `HydraCollectionDto<T>` and `HttpClient.getAllHydraPages` (`HydraCollection.kt`). The
  `next` link is requested as it is (an absolute path with the server's own prefix). Fixture
  tests are in `jvmTest` and read `../../fixtures/` from the module directory: `commonTest` has
  no file API. Mutation-checked one change at a time (`failFast` stops at the first failure).
- [x] **M1.5** — `feature:auth` dto/domain/mapper/data: `POST /api/authentication_token`,
  server-URL check, and distinct results for wrong credentials (401), server JWT failure (500,
  plan §2 trap) and unreachable server.
  **Verify:** mock-engine tests for each result; `KoinGraphTest` passes.
  Note: `AuthRepository.logIn` returns `LoginResult`. The server-URL check is a text check
  (`http(s)://` and a host) plus the login answer itself (404, 405 or no `token` →
  `NotKoillection`); no extra probe request, because it would carry the stored JWT to the typed
  server. The URL is saved before the request (custom headers go only to the stored origin) and
  put back on failure. `mapper` stays a placeholder: the token is a string, nothing to map.
  `LOGIN_PATH` (`core:api`) is now public. Mutation-checked: no URL restore, no 500 mapping and
  no `@Single` each fail a test.
- [x] **M1.6** — `feature:collections` dto/domain/mapper/data: list (paged), get one,
  children.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `CollectionsRepository` returns `Result` (`resultOf`, as in wallosmobile); the failure
  is the `KoillectionError`. `getCollections` returns every level: the server has no `parent`
  filter, so the caller keeps the ones without `parentId`. `imagePath` stays relative
  (`uploads/...`), not resolved. The fixtures hold one collection with no children, so paging
  and a set `parent` are tested with inline bodies. Mutation-checked: no `@Single`, a wrong
  children path and an unparsed parent IRI each fail a test.
- [x] **M1.7** — `feature:items` read side: items of a collection (paged), get one item, the
  item's data (Datum) list, image URLs resolved against the server URL.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `ItemsRepository` returns `Result`, as in M1.6. `ItemMapper.toDomain(dto, serverUrl)`
  resolves image and thumbnail paths to full URLs; the repository passes the stored URL through
  `normalizeBaseUrl` (`core:api`, now public). `getData` sorts by `position`: the server does not promise
  that order (plan §2). `Datum.type` stays the server's string. Mutation-checked: no `@Single`, no
  sort, no server URL in the image URL and an unnormalised server URL each fail a test.
- [x] **M1.8** — `feature:tags` dto/domain/mapper/data: list tags (paged), tag categories,
  an item's tags.
  **Verify:** mapper and repository tests against the M1.1 fixtures.
  Note: `TagsRepository` returns `Result`, as in M1.6. `Tag.categoryId` comes from the
  `category` IRI. The tag image fields are not read: no fixture has one and no step needs them
  yet. The tags fixture is one full page (30 of 30), so paging is tested with an inline body.
  Mutation-checked: no `@Single`, a wrong item-tags path, an unparsed category IRI and a
  dropped second page each fail a test.
- [ ] **M1.9** — Write side: create item, create Datum (text and image type), set an item's
  tags (full list, `merge-patch+json`, plan §2). Request shapes follow `add_book.py`, checked
  against the entity `write` groups in the Koillection source.
  **Verify:** mock-engine tests assert method, path, content type and body for each call.
- [ ] **M1.10** — Multipart upload: `POST /api/items/{id}/image` (field `file`) and
  `POST /api/data/{id}/image` (field `fileImage`). Bytes in, no platform file APIs in
  `commonMain`.
  **Verify:** mock-engine tests assert the multipart parts, field names and content type.
  M1.3 note: `bearerAuth` sends a request again after a 401 and a new login. Check that a
  multipart body is still complete on that second send.
- [ ] **M1.11** — Live read-only smoke against the local test instance: a JVM run (not part of
  `build` or CI) that logs in and reads one collection, its items and one item's data through
  the real repositories. No writes; writes get checked on device in M3.
  **Verify:** the run prints real names from the test instance; the user confirms them.

## M2 — browse (rough scope)

Login screen, collection list, item list, item detail with images and fields.

## M3 — add item with photos (rough scope)

Pick a collection, take or pick photos (main image + page images), text fields, tags.
Matches the book workflow in plan §1.

## M4 — release (rough scope)

Signing, F-Droid/IzzyOnDroid and/or Play per plan §5 decision 2.
