#!/usr/bin/env python3
"""Seed the local Koillection test instance (docs/local-info.md) with sample data for every
feature the app shows: choice lists, a template with every typed field, collections with child
collections, items with typed fields, images and tags, loans, wishlists with wishes, and albums
with photos. Inventories are not seeded: Koillection 1.8.5 has no API `Post` for them.

Request shapes are copied from the `feature:*:data` repositories and `feature:*:dto` classes.
Never point BASE at the real server: it holds the user's working catalogue.

Usage:
    python3 -I fixtures/seed.py          # seed; refuses if a seed root already exists
    python3 -I fixtures/seed.py --clean  # delete everything the seed made (found by name)
"""
import io, json, sys, urllib.error, urllib.request, uuid

from PIL import Image, ImageDraw, ImageFont

BASE = "http://127.0.0.1:8290"
USER, PASS = "tester", "koillection-test"

# The seed's roots. --clean finds everything by these names; deleting a root deletes its children.
COLLECTIONS = ["Comics", "Board games"]
WISHLIST = "Books to find"
ALBUM = "Library shelves"
TEMPLATE = "Comic book"
CHOICE_LISTS = ["Condition", "Format"]
TAG = "Comics"

TOKEN = None


def req(method, path, body=None, content_type="application/ld+json"):
    headers = {"Accept": "application/ld+json"}
    if TOKEN:
        headers["Authorization"] = "Bearer " + TOKEN
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = content_type
    r = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        resp = urllib.request.urlopen(r)
        text = resp.read().decode()
        return json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        raise SystemExit(f"{method} {path}: {e.code} {e.read().decode()[:400]}")


def post(path, body):
    return req("POST", path, body)


def patch(path, body):
    return req("PATCH", path, body, "application/merge-patch+json")


def all_members(path):
    out, page = [], path
    while page:
        r = req("GET", page)
        out += r.get("member", [])
        page = r.get("view", {}).get("next")
    return out


def upload(path, part, title, color):
    """`POST` of one multipart part, as the app's `uploadImage` sends it (`file` or `fileImage`)."""
    img = Image.new("RGB", (900, 1200), color)
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 72)
    except OSError:
        font = ImageFont.load_default()
    draw.rectangle([40, 40, 860, 1160], outline="white", width=12)
    y = 450
    for line in title.split("\n"):
        w = draw.textlength(line, font=font)
        draw.text(((900 - w) / 2, y), line, fill="white", font=font)
        y += 100
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=85)
    boundary = uuid.uuid4().hex
    body = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"{part}\"; filename=\"seed.jpg\"\r\n"
        f"Content-Type: image/jpeg\r\n\r\n"
    ).encode() + buf.getvalue() + f"\r\n--{boundary}--\r\n".encode()
    r = urllib.request.Request(BASE + path, data=body, method="POST", headers={
        "Authorization": "Bearer " + TOKEN,
        "Accept": "application/ld+json",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    })
    try:
        stored = json.loads(urllib.request.urlopen(r).read())
    except urllib.error.HTTPError as e:
        raise SystemExit(f"upload {path}: {e.code} {e.read().decode()[:400]}")
    # A wrong part name answers 200 and stores nothing (CLAUDE.md): check that it stored.
    if not stored.get("image"):
        raise SystemExit(f"upload {path}: 200 but no image stored")


def iri_id(iri):
    return iri.rsplit("/", 1)[-1]


# --- seed -------------------------------------------------------------------------------------

def seed():
    existing = {c["title"] for c in all_members("/api/collections")}
    if existing & set(COLLECTIONS):
        raise SystemExit(f"already seeded ({existing & set(COLLECTIONS)}); run --clean first")

    condition = post("/api/choice_lists", {"name": "Condition",
                                           "choices": ["Mint", "Very good", "Good", "Fair", "Poor"]})
    fmt = post("/api/choice_lists", {"name": "Format", "choices": ["Single issue", "Trade paperback", "Hardcover"]})

    template = post("/api/templates", {"name": TEMPLATE})
    fields = [
        ("Writer", "text", None), ("Artist", "text", None), ("Issue", "number", None),
        ("Published", "date", None), ("Country", "country", None), ("Condition", "choice-list", condition),
        ("Format", "choice-list", fmt), ("Rating", "rating", None), ("Signed", "checkbox", None),
        ("Price paid", "price", None), ("Characters", "list", None), ("Link", "link", None),
        ("Notes", "textarea", None), ("Back cover", "image", None),
    ]
    for pos, (name, type_, cl) in enumerate(fields, start=1):
        body = {"name": name, "position": pos, "type": type_, "template": template["@id"]}
        if cl:
            body["choiceList"] = cl["@id"]
        post("/api/fields", body)

    tag = post("/api/tags", {"label": TAG})
    tags_by_label = {t["label"]: t["@id"] for t in all_members("/api/tags")}

    comics = post("/api/collections", {"title": "Comics"})
    upload(f"/api/collections/{comics['id']}/image", "file", "Comics", (180, 40, 40))
    patch(f"/api/collections/{comics['id']}", {"itemsDefaultTemplate": template["@id"]})
    marvel = post("/api/collections", {"title": "Superheroes", "parent": comics["@id"]})
    upload(f"/api/collections/{marvel['id']}/image", "file", "Superheroes", (30, 70, 160))
    bd = post("/api/collections", {"title": "Franco-Belgian", "parent": comics["@id"]})
    upload(f"/api/collections/{bd['id']}/image", "file", "Franco-\nBelgian", (30, 130, 80))

    def item(coll, name, color, data, tags, image_datum=True):
        it = post("/api/items", {"name": name, "collection": coll["@id"]})
        upload(f"/api/items/{it['id']}/image", "file", name.replace(": ", ":\n"), color)
        pos = 1
        for label, type_, value, cl in data:
            body = {"item": it["@id"], "type": type_, "label": label, "position": pos, "value": value}
            if cl:
                body["choiceList"] = cl["@id"]
            post("/api/data", body)
            pos += 1
        if image_datum:
            d = post("/api/data", {"item": it["@id"], "type": "image", "label": "Back cover", "position": pos})
            upload(f"/api/data/{d['id']}/image", "fileImage", "Back cover", tuple(c // 2 for c in color))
        patch(f"/api/items/{it['id']}", {"tags": [tags_by_label[t] for t in tags if t in tags_by_label]})
        return it

    def comic(writer, artist, issue, date, country, cond, form, rating, signed, price, chars, link, notes):
        return [
            ("Writer", "text", writer, None), ("Artist", "text", artist, None),
            ("Issue", "number", issue, None), ("Published", "date", date, None),
            ("Country", "country", country, None), ("Condition", "choice-list", json.dumps([cond]), condition),
            ("Format", "choice-list", json.dumps([form]), fmt), ("Rating", "rating", rating, None),
            ("Signed", "checkbox", signed, None), ("Price paid", "price", price, None),
            ("Characters", "list", json.dumps(chars), None), ("Link", "link", link, None),
            ("Notes", "textarea", notes, None),
        ]

    i1 = item(marvel, "Night Watch #1", (40, 40, 110), comic(
        "A. Writer", "B. Artist", "1", "1986-03-15", "US", "Very good", "Single issue", "8", "1", "12.50",
        ["Night Watch", "The Captain"], "https://example.com/night-watch-1",
        "First issue.\nSmall crease on the spine."), ["Comics", "English"])
    i2 = item(marvel, "Night Watch #2", (60, 30, 120), comic(
        "A. Writer", "C. Inker", "2", "1986-04-15", "US", "Good", "Single issue", "7", "0", "9.00",
        ["Night Watch"], "https://example.com/night-watch-2", "Second issue."), ["Comics", "English"])
    i3 = item(marvel, "Night Watch: Collected", (90, 20, 90), comic(
        "A. Writer", "B. Artist", "1", "1990-01-01", "GB", "Mint", "Trade paperback", "9", "1", "25",
        ["Night Watch", "The Captain", "Dr. Lune"], "https://example.com/night-watch-tpb",
        "Collects issues 1 to 6."), ["Comics", "English", "Collected works"])
    i4 = item(bd, "Les Voyageurs, tome 1", (20, 110, 60), comic(
        "D. Auteur", "E. Dessinateur", "1", "1972-09-01", "BE", "Fair", "Hardcover", "6", "0", "18.00",
        ["Lucie", "Le Capitaine"], "https://example.com/voyageurs-1", "Dust jacket missing."),
        ["Comics", "Children's book"])
    i5 = item(bd, "Les Voyageurs, tome 2", (30, 140, 90), comic(
        "D. Auteur", "E. Dessinateur", "2", "1974-02-01", "FR", "Good", "Hardcover", "7", "1", "22.00",
        ["Lucie"], "https://example.com/voyageurs-2", "Signed by the artist."), ["Comics"])

    games = post("/api/collections", {"title": "Board games"})
    upload(f"/api/collections/{games['id']}/image", "file", "Board games", (150, 110, 20))
    g1 = item(games, "River Traders", (170, 120, 30), [
        ("Players", "text", "2-4", None), ("Playing time", "text", "60 min", None),
        ("Year", "number", "2015", None), ("Rating", "rating", "8", None),
        ("Complete", "checkbox", "1", None), ("Notes", "textarea", "All pieces counted 2026-11-01.", None),
    ], [], image_datum=False)
    item(games, "Tower Siege", (120, 90, 20), [
        ("Players", "text", "2", None), ("Playing time", "text", "30 min", None),
        ("Year", "number", "2019", None), ("Rating", "rating", "6", None),
        ("Complete", "checkbox", "0", None),
    ], [], image_datum=False)
    item(games, "Star Cartographers", (90, 70, 140), [
        ("Players", "text", "1-5", None), ("Year", "number", "2021", None), ("Rating", "rating", "9", None),
    ], [], image_datum=False)

    # Loans (`NewLoanDto`; dates as `LoanMapper.toApi`, ISO dates): two open, two returned.
    post("/api/loans", {"item": i1["@id"], "lentTo": "Anna", "lentAt": "2026-10-02"})
    old = post("/api/loans", {"item": i1["@id"], "lentTo": "Ben", "lentAt": "2026-03-10"})
    patch(f"/api/loans/{old['id']}", {"returnedAt": "2026-04-01"})
    post("/api/loans", {"item": i4["@id"], "lentTo": "Clara", "lentAt": "2026-11-20"})
    ret = post("/api/loans", {"item": g1["@id"], "lentTo": "David", "lentAt": "2026-08-15"})
    patch(f"/api/loans/{ret['id']}", {"returnedAt": "2026-09-01"})

    # Wishlists and wishes (`NewWishlistDto`, `NewWishDto`).
    wl = post("/api/wishlists", {"name": WISHLIST})
    upload(f"/api/wishlists/{wl['id']}/image", "file", "Books\nto find", (120, 30, 100))
    gifts = post("/api/wishlists", {"name": "Gift ideas", "parent": wl["@id"]})
    wishes = [
        (wl, "Night Watch #3", "https://example.com/night-watch-3", "8.00", "EUR", "Completes the run.", (50, 40, 120)),
        (wl, "Les Voyageurs, tome 3", "https://example.com/voyageurs-3", "25.00", "EUR",
         "Hardcover first print only.", (40, 120, 70)),
        (wl, "Old town map", None, None, None, "Any edition before 1950.", None),
        (gifts, "Reading lamp", "https://example.com/lamp", "39.90", "EUR", None, (200, 160, 40)),
        (gifts, "Bookends", None, "15", "USD", "Cast iron.", None),
    ]
    for parent, name, url, price, currency, comment, color in wishes:
        body = {"name": name, "wishlist": parent["@id"]}
        for k, v in (("url", url), ("price", price), ("currency", currency), ("comment", comment)):
            if v is not None:
                body[k] = v
        w = post("/api/wishes", body)
        if color:
            upload(f"/api/wishes/{w['id']}/image", "file", name.replace(", ", ",\n"), color)

    # Albums and photos (`NewAlbumDto`, `NewPhotoDto`). A photo needs an image in the app.
    al = post("/api/albums", {"title": ALBUM})
    upload(f"/api/albums/{al['id']}/image", "file", "Library\nshelves", (70, 70, 70))
    fairs = post("/api/albums", {"title": "Book fairs", "parent": al["@id"]})
    photos = [
        (al, "Living room shelf", "The fiction shelf after sorting.", "Home", (110, 80, 50)),
        (al, "Comics box", "Long boxes, sorted by issue.", "Basement", (150, 40, 40)),
        (al, "Reading corner", None, "Home", (60, 100, 120)),
        (fairs, "Antiquarian fair 2025", "Found Ut mine Stromtid here.", "Hamburg", (40, 90, 60)),
        (fairs, "Comic con stall", "Signed tome 2.", "Brussels", (130, 50, 130)),
    ]
    for album, title, comment, place, color in photos:
        body = {"title": title, "album": album["@id"]}
        if comment:
            body["comment"] = comment
        if place:
            body["place"] = place
        p = post("/api/photos", body)
        upload(f"/api/photos/{p['id']}/image", "file", title.replace(" ", "\n", 1), color)

    print("seeded: 2 choice lists, 1 template (14 fields), 1 tag, 4 collections, 8 items,"
          " 4 loans, 2 wishlists, 5 wishes, 2 albums, 5 photos")


# --- clean ------------------------------------------------------------------------------------

def clean():
    n = 0
    for c in all_members("/api/collections"):
        if c["title"] in COLLECTIONS and not c.get("parent"):
            req("DELETE", c["@id"]); n += 1
    for w in all_members("/api/wishlists"):
        if w["name"] == WISHLIST and not w.get("parent"):
            req("DELETE", w["@id"]); n += 1
    for a in all_members("/api/albums"):
        if a["title"] == ALBUM and not a.get("parent"):
            req("DELETE", a["@id"]); n += 1
    for t in all_members("/api/templates"):
        if t["name"] == TEMPLATE:
            req("DELETE", t["@id"]); n += 1
    # Choice lists after the items that used them: an item still using one blocks the delete.
    for cl in all_members("/api/choice_lists"):
        if cl["name"] in CHOICE_LISTS:
            req("DELETE", cl["@id"]); n += 1
    for t in all_members("/api/tags"):
        if t["label"] == TAG:
            req("DELETE", t["@id"]); n += 1
    print(f"deleted {n} seed roots")


def main():
    global TOKEN
    TOKEN = req("POST", "/api/authentication_token", {"username": USER, "password": PASS},
                "application/json")["token"]
    clean() if "--clean" in sys.argv else seed()


if __name__ == "__main__":
    main()
