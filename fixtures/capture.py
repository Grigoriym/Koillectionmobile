#!/usr/bin/env python3
"""Capture API fixtures from the local Koillection test instance (docs/local-info.md).

Writes into fixtures/koillection-<version>/. GET only, plus two failed logins for the 401
bodies. Never point this at the real server: the repo is public.

Usage:
    python3 -I fixtures/capture.py              # every fixture
    python3 -I fixtures/capture.py inventories  # only the named groups (here: the M12.1 files)
    python3 -I fixtures/capture.py logs         # the M13.1 files

Capture only the group you need: a full run rewrites the older fixtures, which came from an
older copy of the catalogue with other IDs (docs/local-info.md), and their tests read them.
"""
import json, os, sys, urllib.error, urllib.request

BASE = "http://127.0.0.1:8290"
USER, PASS = "tester", "koillection-test"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "koillection-1.8.5")


def req(method, path, body=None, headers=None):
    r = urllib.request.Request(BASE + path, data=body, method=method, headers=headers or {})
    try:
        resp = urllib.request.urlopen(r)
        return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def login(user, password):
    return req("POST", "/api/authentication_token", json.dumps({"username": user, "password": password}).encode(),
               {"Content-Type": "application/json"})


def save(name, status, text, expect):
    if status != expect:
        raise SystemExit(f"{name}: expected {expect}, got {status}: {text[:300]}")
    try:
        text = json.dumps(json.loads(text), indent=2, ensure_ascii=False) + "\n"
    except ValueError:
        pass
    with open(os.path.join(OUT, name), "w") as f:
        f.write(text)
    print(f"{name}: {status}")


def main():
    os.makedirs(OUT, exist_ok=True)
    token = json.loads(login(USER, PASS)[1])["token"]

    def get(path, accept="application/ld+json", auth=True):
        h = {"Accept": accept}
        if auth:
            h["Authorization"] = "Bearer " + token
        return req("GET", path, headers=h)

    def body(path):
        return json.loads(get(path)[1])

    known = {"core", "inventories", "logs"}
    groups = sys.argv[1:] or sorted(known)
    if set(groups) - known:
        raise SystemExit(f"unknown group: {set(groups) - known}")

    if "core" in groups:
        capture_core(get, body)
    if "inventories" in groups:
        capture_inventories(get, body)
    if "logs" in groups:
        capture_logs(get, body)


def capture_core(get, body):
    save("openapi.json", *get("/api/docs", accept="application/vnd.openapi+json"), 200)
    save("login_401_wrong_password.json", *login(USER, "wrong-password"), 401)
    save("login_401_unknown_user.json", *login("no-such-user", "x"), 401)
    save("collections_401_no_token.json", *get("/api/collections", auth=False), 401)

    save("collections_page.json", *get("/api/collections"), 200)
    coll = body("/api/collections")["member"][0]["@id"]
    save("collection_one.json", *get(coll), 200)
    save("collection_children_page.json", *get(coll + "/children"), 200)
    items = body(coll + "/items")
    save("collection_items_page_1.json", *get(coll + "/items"), 200)
    last = items["view"]["last"]
    save("collection_items_page_last.json", *get(last), 200)
    # the first item of page 1 has text and image fields, a main image, and tags
    item = items["member"][0]["@id"]
    save("item_one.json", *get(item), 200)
    save("item_data.json", *get(item + "/data"), 200)
    save("item_tags.json", *get(item + "/tags"), 200)
    save("tags_page.json", *get("/api/tags"), 200)
    save("tag_categories.json", *get("/api/tag_categories"), 200)


def capture_inventories(get, body):
    # Needs the inventories from `fixtures/seed.py`: one over "Comics" (no items of its own)
    # and its two children, with ticked items, and one over "Board games".
    save("inventories_page.json", *get("/api/inventories"), 200)
    named = {i["name"]: i["@id"] for i in body("/api/inventories")["member"]}
    if "Comics shelf check" not in named:
        raise SystemExit("no 'Comics shelf check' inventory: run fixtures/seed.py first")
    save("inventory_one.json", *get(named["Comics shelf check"]), 200)


def capture_logs(get, body):
    # The API sends logs oldest first (plan §2). The app reads `view.last`, then each `previous`.
    save("logs_page_first.json", *get("/api/logs"), 200)
    last = body("/api/logs")["view"]["last"]
    save("logs_page_last.json", *get(last), 200)
    # the page before the last one has both `previous` and `next`
    middle = body(last)["view"]["previous"]
    save("logs_page_middle.json", *get(middle), 200)
    # the newest log: the last row of the last page
    save("log_one.json", *get(body(last)["member"][-1]["@id"]), 200)


if __name__ == "__main__":
    main()
