# Revisit

Real problems found outside the current task. One entry each, with `file:line` or a link so a
cold session can act on it. Delete an entry when it's fixed.

- 2026-11-14 (M0.2) — Every Kotlin compile prints `Koin compiler plugin: Kotlin 2.4.21 is newer
  than the newest verified version (2.4.20)`. Catalog: `kotlin = "2.4.21"`, `koin-plugin =
  "1.2.3"` (`gradle/libs.versions.toml`). Harmless today; bump `koin-plugin` once a release
  lists 2.4.21 as verified (https://github.com/InsertKoinIO/koin-compiler-plugin/releases).
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.24.
- 2026-11-14 (M0.3) — Every module on `koillectionmobile.kmp.library.stability` (`core:domain`,
  `feature:*:domain`) prints `Unsupported compileOnly Dependencies in Kotlin Targets` for
  `compose.runtime` on `iosArm64Main`/`iosSimulatorArm64Main`. Source:
  `build-logic/.../ComposeStabilityMarker.kt:22` (`compileOnly`, ported from wallosmobile, which
  has no iOS targets so never saw it). Build is green. Fix options: make it `implementation`
  (every consuming `ui` module carries compose-runtime anyway), or `compileOnly` for
  `androidMain`/`jvmMain` + `implementation` for `iosMain`. Decide before iOS is ever built.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.24.
- 2026-11-14 (M0.6) — CI run 43643554439 warns: `actions/upload-artifact@v4` targets Node.js 20,
  which GitHub forces onto Node.js 24. Source: `.github/workflows/ci.yml` (the "Upload Kover XML
  report" step, ported from deskmate, which has the same step). Fix: bump to the newest
  `upload-artifact` major that targets Node 24, then check CI stays green.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.24.
- 2026-11-15 (M1.9) — Write bodies send IRIs without the server's subpath (`/api/collections/<id>`,
  `/api/items/<id>`, `/api/tags/<id>`): `ItemsRepositoryImpl.createItem`/`itemIri` and
  `TagsRepositoryImpl.setItemTags`. The server's own IRIs carry its prefix (M1.4: `next` links).
  Whether API Platform accepts an unprefixed IRI on a server behind a subpath is not checked:
  the source has no `vendor/`, and the local test instance runs at the root. `add_book.py` uses
  unprefixed IRIs against the user's server, but whether that server has a subpath is not
  recorded here. Check once against a subpath instance (or the user's setup) before release (M14).
  User, 2026-11-15: their server is reached through Tailscale, with no subpath ("I presume").
  Deferred by the user (2026-11-15): not part of M3.7. Confirm it later from the server URL
  itself, read only.
  Answer (M14.1, 2026-11-25, read only): the user's server is served by `tailscale serve` at path
  `/` on its own port, so it has no subpath. Other users may have one. Verdict (user): check a
  subpath instance before release, M14.12.
- 2026-11-15 (M2.5) — Hand-copied UI parts across `ui` modules. The error mapping and the error
  view are fixed (M14.10: `core:ui` `toMessage` and `EmptyOrError`). The forms and the nested
  delete are still copied:
  M11.4 copied the name-and-image form a third time: `FormPhoto`, the save status, the form
  scaffold, `FormImageSection` and `toSaveMessage` exist in `feature/collections/ui`,
  `feature/wishlists/ui` (`WishlistFormScreen.kt`, `NewWishlistViewModel.kt`) and
  `feature/albums/ui` (`AlbumFormScreen.kt`, `AlbumFormUiState.kt`, `AlbumMessages.kt`). Only the
  strings and the repository calls differ. M11.7 (photo form) reused the album form parts inside
  `feature/albums/ui` (made `internal`; `FormImageSection` got `isServerImageRemovable`, as in
  `feature/wishlists/ui`), but `NewPhotoViewModel` and `EditPhotoViewModel` copy
  `NewWishViewModel` and `EditWishViewModel`, and `PhotoFormValues` copies `WishFormValues`.
  Fix option: move the form parts to a shared UI module that `core:photos` could host.
  M11.5 copied the nested-delete parts from `feature/wishlists/ui` (M9.5): `descendantIds`,
  the count, `DeleteSummary` and `DeleteDialogs` in `AlbumViewModel.kt`, `AlbumUiState.kt` and
  `AlbumScreen.kt` differ only in types and strings. `descendantIds` could take `(id, parentId)`
  pairs and live in a shared module.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.10 (error mapping and view, done
  2026-11-26) and M14.11 (forms and nested delete).
- 2026-11-15 (M3.6) — Add-item photo files in `cacheDir/photos/` are deleted on photo remove, on
  save, and when the form leaves the back stack (`AddItemViewModel.onCleared`). A form that never
  comes back (the task swiped away, so the saved draft is gone and `onCleared` never runs) leaves
  its files behind: M3.6 found three such files from M3.5 on the emulator. Fix option: on app
  start, delete files in `cache/photos/` that no saved draft names (or older than a day).
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.21.
- 2026-11-15 (M3.6) — A `createItem` whose response is lost (a timeout after the server created
  the item) leaves no item id in `AddItemState`, so "Try again" creates a second item.
  `AddItemUseCaseImpl.addItem` in `feature/items/domain/.../AddItemUseCase.kt`. The server has
  no idempotency key. Fix option: before a retry with no item id, look for a new item of the same
  name in the collection, or ask the user.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.18.
- 2026-11-16 (M4.1) — The local network permission is not checked on an API 37 device. The
  `koil37` AVD crashes (`docs/machine.local.md`), and the user chose to skip that pass. Open:
  denied at Log in gives `login_error_local_network_denied` and sends no request; allowed logs in;
  a revoke while logged in shows `LocalNetworkBanner` (`composeApp/.../LocalNetworkBanner.kt`),
  which leaves after a grant; the banner takes the status bar inset and the screen below does not
  add a second gap. Run it on a working API 37 AVD or an Android 17 phone.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.23.
- 2026-11-16 (M4.1) — Known limits carried over from wallosmobile (its `docs/revisit.md` §6):
  screens do not reload after a grant (the collection list needs Try again); the denied message at
  Log in has no "Open settings" action. `feature/auth/ui/.../permission/`.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.22.
- 2026-11-16 (M4.5) — MASVS-PRIVACY-4: log out does not clear Coil's image disk cache.
  `AuthRepositoryImpl.logOut` (`feature/auth/data/.../AuthRepositoryImpl.kt:57`) clears only the
  session keys; the app uses Coil's default singleton `ImageLoader` (no `diskCache` config). A log
  in to another server keeps the first server's images on the device. Fix option: clear the
  `ImageLoader`'s disk and memory cache on log out.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.14.
- 2026-11-16 (M4.5) — MASVS-PRIVACY-1: `ACCESS_NETWORK_STATE`
  (`androidApp/src/main/AndroidManifest.xml:5`) has no user in app code. Coil's network fetcher
  may use it. Check the merged release manifest (M14.2) and the libraries before removing it.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.2.
- 2026-11-16 (M4.5) — Images do not get the custom headers (M4.2). Coil loads them with its own
  Ktor client (`coil-network-ktor3`, `composeApp/build.gradle.kts:30`), not the app's
  `HttpClient`, so the `CustomHeaders` plugin does not run. Behind a header-checking proxy
  (`docs/local-info.md`, "Proxy front") the thumbnails and item images will fail. Not checked on
  device. Fix option: a singleton `ImageLoader` whose fetcher uses a client with the
  `CustomHeaders` plugin (server origin only).
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.13.
- 2026-11-16 (M5.3) — A delete that gets HTTP 404 shows "The item was not deleted" and keeps the
  item screen. `ItemViewModel.delete` (`feature/items/ui/.../ItemViewModel.kt`) treats every
  failure the same. A 404 means the item is already gone: a retry after a lost answer, or a delete
  from the web UI. The user then sees a deleted item and has to go back by hand; the collection is
  not told to reload. Fix option: treat `KoillectionError.Http(404)` on delete as success.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.17.
- 2026-11-16 (M5.6) — Two edge cases of the edit save, left as they are.
  (1) "Back to the form" shows after a failure where no call reported success
  (`EditItemViewModel.fail`, `isPartial = hasSent`). If the failed call was an image upload whose
  answer was lost, the server has the new image; the next save is `retry`, which sees a changed
  image URL and skips that upload (`EditItemUseCaseImpl.plan`, "image URL is still the original
  one"). A second replacement of the same photo on the form is then not sent.
  (2) `EditItemResult.DuplicateLabel` from a `retry` (someone added a field with that label on
  the web meanwhile) after a partial save shows only "Try again", which gives the same answer each
  time; the user can only leave the form. Fix option for both: let `retry` diff against the server
  as reloaded, with a fresh `original`, once the form changes after a failure.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.19.
- 2026-11-17 (M6.6) — The collection screen does not show the collection's own image. Only the
  parent's child row and the collection list show it, as a thumbnail (`CollectionScreen.kt:112`
  puts only `uiState.title` in the top bar). After an edit that sets or replaces the image, the
  edited screen looks the same; the user sees the change only in the parent or the list. Not a
  bug against any step: M2 never asked for it. Decide in M14.1 whether the screen gets a header
  image.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.26.
- 2026-11-18 (M8.4) — Two edge cases of the template save, left as they are.
  (1) A field changed from `choice-list` to another type keeps its choice list on the server:
  `SaveTemplateUseCaseImpl.patch` sends no `choiceList`, and `editField` cannot send an explicit
  null (M8.1 note, `FieldPatchDto`). The web UI may still show the link. Fix option: a nullable
  `choiceList` that the patch DTO sends as `null` on purpose.
  (2) A new template whose `POST /api/templates` answer was lost has no known id, so the retry
  (`TemplateFormViewModel.runSave`, `record.createdId == null`) creates a second template. The
  new-collection form has the same gap, and so has the M8.5 choice list form
  (`ChoiceListFormViewModel.save`: a create stopped by a process death is sent again).
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.20 for (1), M14.18 for (2).
- 2026-11-18 (M8.6) — The item screen shows a typed value as the server stores it: a `checkbox` as
  `1` or `0`, a `choice-list` as a JSON array (`["Hardcover","Ebook"]`)
  (`feature/items/ui/.../ItemField.kt`, `Datum.toField`: every text type becomes
  `ItemField.Text` with the raw `value`). The item forms show them with their own input since
  M8.6. Fix option: a checkmark (or yes/no) for `checkbox` and the picks joined by commas for
  `choice-list`, with `pickedChoices` from `FieldInput.kt`. Seen on the device in M8.9: "Read" showed
  `1`, then `0`, and "Genre" showed `["Novel","Poem"]`; the web item page shows a cross and a list.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.15.
- 2026-11-23 (M10.6) — The item screen jumps up after each loan action (save, mark returned,
  delete). While the section reads again, `LoansContent` shows only a spinner in place of the list
  (`feature/loans/ui/.../LoansSection.kt:73`, `uiState.isLoading`). The page gets shorter, so
  the scroll position moves up and the loan just changed is off screen. Fix option: keep the old
  list on screen during a reload and show the spinner only when there is no list yet.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.16.
- 2026-11-25 (M12.5) — `fixtures/seed.py` does not make the same inventory ticks on each run.
  `seed_inventories` (`fixtures/seed.py:141`) ticks three items, and the server's tick regex also
  ticks every later item in `content` (plan §2, "Inventory ticks"). The item order in `content`
  comes from `InventoryHandler::buildCollection` (`$collection->getItems()`, no order), so it
  changes between runs: M12.1 stored 4 of 5 ticks, the M12.5 reseed stored 5 of 5 (no unticked
  item left in "Comics shelf check"). Fix option: read the order from `GET /api/inventories/{id}`
  after the create and tick only the last items in that order, so the server stores exactly what
  the seed sends.
  Verdict (user, 2026-11-25, M14.1): fix before release, M14.25.
