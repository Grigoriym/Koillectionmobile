# Local test instance

Deliberately in the repo, and safe: a Koillection container bound to `127.0.0.1`, not
reachable from other machines. Nothing here is a credential for anything else.

Use this instance for every test, fixture capture and on-device check. Never write to the
user's real server: it holds their working book catalogue.

This file is the contract that every machine meets. Where the instance lives on a given
machine (folders, the real instance's container, the exact reset commands) is in that
machine's `docs/machine.local.md` (gitignored). Read it first. If it is missing, this machine
has no instance yet: build one to this contract and write the file.

    from the host:     http://127.0.0.1:8290/
    from the emulator: http://10.0.2.2:8290
    username:          tester
    password:          koillection-test

The smoke runs below and the emulator doc have these values as constants. A machine that
uses other values breaks them.

Version: `koillection/koillection:1.8.5` + `postgres:17`, bound to `127.0.0.1` only,
`restart: "no"`. It stays off between sessions. `JWT_PASSPHRASE` is fixed in the compose
file, so a restart does not break API login (plan §2 trap).

## Data

A full copy of the real catalogue: `Books` with 445 items and their images (2026-11-15).
Make it by reading the real instance only: a `pg_dump` and a copy of its uploads folder.
Then rename the copied user to `tester` (e-mail `tester@example.com`, the password above)
and raise its upload quota. A dump copy keeps the real IDs.

- Run `pg_restore` before the app container starts the first time. If the app starts on an
  empty database first, it creates the schema and the restore conflicts with it.
- Hash the password with `bin/console security:hash-password` in the app container.
- `psql -v h=...` puts `:'h'` in only for SQL read from stdin or a file, not for `-c`.
- A 1.8.4 dump migrates to 1.8.5 on the first start without errors (2026-11-15).

An older machine made its copy with an API script (`copy_from_real.py`) instead. That copy had
its own IDs, and the fixtures in `fixtures/koillection-1.8.5/` came from it.

## Sample data

`fixtures/seed.py` adds sample data for every feature the app shows, next to the copy of the
catalogue (2026-11-24). The copy has no choice lists, wishes, albums, photos or loans of its own.

- `Comics` (default template `Comic book`, every typed field type) with the children
  `Superheroes` and `Franco-Belgian`: 5 items with a main image, a `Back cover` image field,
  typed values and tags. `Board games`: 3 items with simple fields.
- 2 choice lists (`Condition`, `Format`) and the tag `Comics` (page 2 of `/api/tags`).
- 4 loans: on "Night Watch #1" one open and one returned, one open on "Les Voyageurs, tome 1", one
  returned on "River Traders".
- The wishlist `Books to find` with the child `Gift ideas` (5 wishes), and the album `Library
  shelves` with the child `Book fairs` (5 photos). The images are generated coloured JPEGs.
- 2 inventories: "Comics shelf check" over `Comics` and both children (4 or 5 of 5 items
  ticked), and "Board games check" (0 of 3). Koillection 1.8.5 has no API `Post` for `Inventory`,
  so the seed logs in to the web UI and sends the web form and the tick route. The server stores
  more ticks than the seed's 3, and how many depends on the item order (plan §2, "Inventory
  ticks"): read the counts from `GET /api/inventories` after each seed run.

`python3 -I fixtures/seed.py` refuses to run twice. `--clean` deletes the seed by its root names;
after it the counts match the unseeded copy, except the log count. Run it again after a reset.

## Fixtures

`fixtures/capture.py` writes the JSON fixtures in `fixtures/koillection-1.8.5/` from this
instance. It refuses nothing on its own: never change its `BASE` to the real server. Give it a
group name (`core`, `inventories`, `logs`) to capture only that group. A run with no name rewrites the
`core` files too, and those came from the older copy with other IDs. The `inventories` group
needs the seed.

## Smoke run

`./gradlew :composeApp:smokeRun` logs in to this instance and prints collections, items and one
item's fields through the app's own repositories (M1.11). It only reads. Start the instance first.
Its URL and credentials are constants in `composeApp/src/jvmTest/.../smoke/LiveSmoke.kt`: never
point them at the real server.

`./gradlew :composeApp:smokeWriteRun` writes (M3.1, M5.2): it creates one item in `Books` with two
text fields, an image field, a main image and two existing tags, and prints it read back. Then it
edits the item (rename, field changes, both images replaced, the image field deleted) and checks
each old file for 404. Last it deletes the item and checks the 404. If it fails halfway, it still tries the delete; look for a leftover
`M3.1 smoke <millis>` item in `Books` if the cleanup line reports an error. Its constants are in
`LiveWriteSmoke.kt`, next to `LiveSmoke.kt`: the same rule applies.

`./gradlew :composeApp:smokeCollectionRun` writes (M6.2): it creates a top-level collection and a
child, renames the child, uploads, replaces and removes the child's image, adds an item to the child
and deletes the parent. Last it sends a create with an empty title (422). If it fails halfway, it
still tries to delete the parent; look for a leftover `M6.2 smoke parent <millis>` collection if
the cleanup line reports an error. Its constants are in `LiveCollectionSmoke.kt`: the same rule
applies.

`./gradlew :composeApp:smokeTagRun` writes (M7.4): it creates a tag, puts it on the first item it
finds with tags, takes it off again and deletes it. It also sends an empty label (422) and a
duplicate label, and deletes the duplicate tag. If it fails halfway, it still puts the item's tags
back and deletes its tags; look for a leftover `M7.4 smoke <millis>` tag if a cleanup line reports
an error. Its constants are in `LiveTagSmoke.kt`: the same rule applies.

`./gradlew :composeApp:smokeTemplateRun` writes (M8.2): it creates a choice list, a template with
typed fields and a top-level collection. It sets and clears the collection's default template, adds
an item with typed fields, deletes the template while it is the default, and deletes the rest. It
also sends a `choice-list` field without a choice list (422), a duplicate field name and a `checkbox`
value `true` (422). It checks that the counts of templates, choice lists and collections end as they
started. If it fails halfway, it still deletes what it made; look for a leftover `M8.2 smoke
<millis>` collection, template or choice list if a cleanup line reports an error. Its constants are
in `LiveTemplateSmoke.kt`: the same rule applies.

`./gradlew :composeApp:smokeWishlistRun` writes (M9.2): it creates a top-level wishlist and a
child, renames the child, and uploads, replaces and removes the child's image. It creates a wish in
the child with every field, uploads and replaces its image, edits two fields and clears `url`. It
sends a currency `EURO`, an empty name and a wish without `wishlist` (each 422). Last it deletes the
parent and checks that the count of wishlists ends as it started. If it fails halfway, it still
deletes the parent; look for a leftover `M9.2 smoke parent <millis>` wishlist if a cleanup line
reports an error. Its constants are in `LiveWishlistSmoke.kt`: the same rule applies.

`./gradlew :composeApp:smokeLoanRun` writes (M10.2): in `Books` it creates an item and two loans.
It edits `lentTo`, sets and clears `returnedAt`, and sends an empty `lentTo` and a loan without
`lentAt` (each 422). It deletes one loan, then the item, and checks that the counts of loans and of
`Books` items end as they started. If it fails halfway, it still deletes the item (which takes its
loans); look for a leftover `M10.2 smoke <millis>` item if a cleanup line reports an error. Its
constants are in `LiveLoanSmoke.kt`: the same rule applies.

`./gradlew :composeApp:smokeAlbumRun` writes (M11.2): it creates a top-level album and a child,
renames the child, and uploads, replaces and removes the child's image. It creates a photo in the
child with every field, uploads and replaces its image, edits two fields and clears `comment`. It
sends an empty title and a photo without `album` (each 422). Last it deletes the parent and checks
that the count of albums ends as it started. If it fails halfway, it still deletes the parent; look
for a leftover `M11.2 smoke parent <millis>` album if a cleanup line reports an error. Its constants
are in `LiveAlbumSmoke.kt`: the same rule applies.

All eight runs keep their session in `composeApp/build/smoke-home`. A run more than an hour after the
last one starts with an expired JWT. That is the path that hung in M3.1, so do not delete the
directory to make a hang go away: find out why first.

## Probes

An expired-JWT response without waiting an hour: send any token whose payload has `exp` 1. The
server checks expiry before the signature (checked 2026-11-15):

    curl -s -H 'Accept: application/ld+json' \
      -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjF9.c2ln' \
      http://127.0.0.1:8290/api/collections
    # {"code":401,"message":"Expired JWT Token"}

The cause of an empty 500: the app container's log has the PHP exception as one JSON line per
request (`docker logs --since 5m <instance container> 2>&1 | grep -i exception`). It named the
duplicate-label bug in M5.2 (plan §2). A 500 from a web form (M13.4) was not in `docker logs`: read
the newest file in `/app/public/var/log/` inside the container and grep it for `CRITICAL`.

## Proxy front

A throwaway nginx in front of this instance, for the proxy rejection (M4.3). Each port shows
one case. Run it on the instance's Docker network and remove it after. The network and the
container names on a given machine are in `docs/machine.local.md`.

    printf 'proxy:%s\n' "$(openssl passwd -apr1 secret)" > htpasswd
    cat > nginx.conf <<'EOF'
    server {  # header check: the app's custom header passes it
        listen 8295;
        client_max_body_size 20m;
        location / {
            if ($http_x_proxy_token != "secret") { return 401; }
            proxy_set_header Host $http_host;
            proxy_pass http://<instance container>:80;
        }
    }
    server {  # auth_basic: evidence only, the app does not support it
        listen 8296;
        location / {
            auth_basic "test";
            auth_basic_user_file /etc/nginx/htpasswd;
            proxy_set_header Host $http_host;
            proxy_pass http://<instance container>:80;
        }
    }
    server { listen 8297; location / { return 407; } }
    server { listen 8298; location / { return 302 https://auth.example.com/login?rd=$request_uri; } }
    EOF
    docker run -d --name koillection-proxyfront --network <instance network> \
      -p 127.0.0.1:8295-8298:8295-8298 \
      -v $PWD/nginx.conf:/etc/nginx/conf.d/default.conf:ro \
      -v $PWD/htpasswd:/etc/nginx/htpasswd:ro nginx:alpine

From the emulator: `http://10.0.2.2:8295`, custom header `X-Proxy-Token: secret`. Without
the header the front answers 401 HTML; with it, Koillection answers. `docker logs
koillection-proxyfront` shows each request's status, also the app's (user agent
`ktor-client`). Remove it with `docker rm -f koillection-proxyfront`.
