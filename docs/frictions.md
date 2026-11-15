# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed
entries get deleted — see /finalize.

- 2026-11-14 — `sed -e 's#...#...#'` failed with "unknown option to `s'" because the replacement
  text held a `#` comment. Use `|` as the delimiter when the text may contain `#`.
- 2026-11-15 — `koillection/koillection:1.8.5` first start: the entrypoint ran migrations before Postgres accepted connections and the container exited (`set -e`). Fixed with a `pg_isready` healthcheck + `condition: service_healthy` in the test instance's `compose.yaml`.
- 2026-11-15 — Catalogue copy into the test instance died at item 102: 422 "not enough space left on your account" (Koillection per-user `disk_space_allowed`). Raised it in `koi_user` via psql; reset steps in `docs/local-info.md`.
