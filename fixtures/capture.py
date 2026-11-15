#!/usr/bin/env python3
"""Capture API fixtures from the local Koillection test instance (docs/local-info.md).

Writes into fixtures/koillection-<version>/. GET only, plus two failed logins for the 401
bodies. Never point this at the real server: the repo is public.

Usage: python3 -I fixtures/capture.py
"""
import json, os, urllib.error, urllib.request

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


if __name__ == "__main__":
    main()
