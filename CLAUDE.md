# koillectionmobile

Unofficial mobile client (KMP/Compose) for [Koillection](https://github.com/benjaminjonard/koillection),
a self-hosted collection manager. Not affiliated with the Koillection project.

First real use case: the user's book catalogue. Photograph a book on the phone, file it as an
item with text fields, a main image and page images, without a PC in between.

## Where facts live

**Point, don't copy.** Where another file owns a fact, link it and stop.

- Status / progress → `docs/CHECKLIST.md` banner
- Goal, API facts, architecture, open decisions → `docs/IMPLEMENTATION_PLAN.md`
- The server's API → the Koillection source at `../../koillection` (`src/Entity/*.php`,
  `#[ApiResource]` attributes) and the server's own OpenAPI docs. If this repo's docs and the
  server disagree, the server wins.

## How work happens here

One checklist step per session: "read `docs/CHECKLIST.md` and do step `<N>`". Do exactly that
step, run its Verify line, tick it, update the banner, then run `/finalize`.

Step M0.5 replaces this file with a merge of `../agentic-grappim/templates/CLAUDE.md.template`
(same as deskmate's and wayprint's M0). Until then, this short version applies.
