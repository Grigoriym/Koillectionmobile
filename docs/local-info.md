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

## Fixtures

`fixtures/capture.py` writes the JSON fixtures in `fixtures/koillection-1.8.5/` from this
instance. It refuses nothing on its own: never change its `BASE` to the real server.

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

Both runs keep their session in `composeApp/build/smoke-home`. A run more than an hour after the
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
duplicate-label bug in M5.2 (plan §2).

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
