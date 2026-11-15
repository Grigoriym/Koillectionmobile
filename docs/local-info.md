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

A full copy of the real catalogue, made 2026-11-15 (second time, on the current machine) from
a database dump and the uploads folder. The real instance is the `koillection` container
(`koillection/koillection:1.8.4`, compose in `/home/gregory/claude/koillection/`). Only read
it: `pg_dump` and a file copy. The copy keeps the real IDs. The real user was renamed to
`tester`, with the e-mail `tester@example.com`, the password above and a large upload quota.
On the first start, 1.8.5 migrated the 1.8.4 data without errors.

The first copy (also 2026-11-15, on another machine) used an API script, `copy_from_real.py`.
That copy had its own IDs. The script is not on this machine, and the fixtures in
`fixtures/koillection-1.8.5/` came from that first copy.

To reset to a fresh copy (the real instance keeps running):

    cd /home/gregory/data/koillection-test
    docker compose down && sudo rm -rf postgres && docker compose up -d db
    docker exec koillection-db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > real.dump
    docker exec -i koillection-test-db pg_restore -U koillection -d koillection --no-owner < real.dump
    rsync -a --delete /home/gregory/claude/koillection/data/uploads/ uploads/
    docker compose up -d
    H=$(docker exec koillection-test php /app/public/bin/console security:hash-password \
      'koillection-test' 'App\Entity\User' -n | grep -oE '\$2y\$[^ ]+')
    echo "update koi_user set username='tester', email='tester@example.com', password=:'h',
      disk_space_allowed=1000000000000;" |
      docker exec -i koillection-test-db psql -U koillection -d koillection -v h="$H"

`psql` puts `:'h'` in only for SQL read from stdin or a file, not for `-c`.

Run `pg_restore` before the app container starts the first time. If the app starts on an empty
database first, it creates the schema and the restore conflicts with it.

## Fixtures

`fixtures/capture.py` writes the JSON fixtures in `fixtures/koillection-1.8.5/` from this
instance. It refuses nothing on its own: never change its `BASE` to the real server.

## Smoke run

`./gradlew :composeApp:smokeRun` logs in to this instance and prints collections, items and one
item's fields through the app's own repositories (M1.11). It only reads. Start the instance first.
Its URL and credentials are constants in `composeApp/src/jvmTest/.../smoke/LiveSmoke.kt`: never
point them at the real server.

`./gradlew :composeApp:smokeWriteRun` writes (M3.1): it creates one item in `Books` with two text
fields, an image field, a main image and two existing tags, prints it read back, then deletes it
and checks the 404. If it fails halfway, it still tries the delete; look for a leftover
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
