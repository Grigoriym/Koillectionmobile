# Local test instance

Deliberately in the repo, and safe: a Koillection container bound to `127.0.0.1`, not
reachable from other machines. Nothing here is a credential for anything else.

Use this instance for every test, fixture capture and on-device check. Never write to the
user's real server: it holds their working book catalogue.

    data + compose:    /home/gregory/data/koillection-test/
    start:             docker compose -f /home/gregory/data/koillection-test/compose.yaml up -d
    stop:              docker compose -f /home/gregory/data/koillection-test/compose.yaml stop
    from the host:     http://127.0.0.1:8290/
    from the emulator: http://10.0.2.2:8290
    username:          tester
    password:          koillection-test

Version: `koillection/koillection:1.8.5` + `postgres:17`, `restart: "no"`. It stays off
between sessions. The first start takes about a minute (database migrations). `JWT_PASSPHRASE`
is fixed in `compose.yaml`, so a restart does not break API login (plan §2 trap).

## Data

A copy of the real catalogue, made 2026-11-15 by
`/home/gregory/data/koillection-test/copy_from_real.py`. The script only reads the real server
(through `koi.py`) and writes to this instance. It copies tag categories, tags, collections,
items with their main image, all data fields with their images, and each item's tags. The test
copy has its own IDs and its own user, so no real ID appears in it.

To reset to a fresh copy: stop the instance, delete `postgres/`, `uploads/` and `state.json` in
the data folder, start it, create the `tester` user on `/first-connection` (username, e-mail
`tester@example.com`, password above), then raise its upload quota (the default stops the
copy after about 500 MB of images with a 422 "not enough space left on your account"):

    docker exec koillection-test-db psql -U koillection -d koillection \
      -c "update koi_user set disk_space_allowed = 1000000000000 where username='tester';"

Then run `python3 -I copy_from_real.py` in the data folder. It is resumable: run it again after
an error. A finished copy ends `copy.log` with `done`. Check that line, not the
exit status of a wrapping shell command: on 2026-11-15 the fixtures were first captured from a
copy that had died at item 102.

## Fixtures

`fixtures/capture.py` writes the JSON fixtures in `fixtures/koillection-1.8.5/` from this
instance. It refuses nothing on its own: never change its `BASE` to the real server.

## Smoke run

`./gradlew :composeApp:smokeRun` logs in to this instance and prints collections, items and one
item's fields through the app's own repositories (M1.11). It only reads. Start the instance first.
Its URL and credentials are constants in `composeApp/src/jvmTest/.../smoke/LiveSmoke.kt`: never
point them at the real server.

## Probes

An expired-JWT response without waiting an hour: send any token whose payload has `exp` 1. The
server checks expiry before the signature (checked 2026-11-15):

    curl -s -H 'Accept: application/ld+json' \
      -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjF9.c2ln' \
      http://127.0.0.1:8290/api/collections
    # {"code":401,"message":"Expired JWT Token"}
