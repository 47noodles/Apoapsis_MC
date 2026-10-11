# The work graph to 1.0

> Planned by Opus, at 47noodles' request (11 Oct 2026): *"a work graph, comprehensive, from now until full modpack. put it in the repo too, and HTML it."* It turns the staged roadmap (`../design/12-roadmap.md`) and the feature checklist (`../design/10-feature-checklist.md`) into 191 work items with dependencies.

**Open [work-graph.html](work-graph.html)** in a browser (download it, or clone the repo; GitHub shows HTML as source). It reads left to right: every item sits to the right of everything it needs, columns are centred, and each stage is a band. The first work is on the far left, and 1.0 is on the far right. Click a card to see what it needs, what it unblocks and when it counts as done. The stage links at the top jump along the page.

## Files

| File | What it is |
|---|---|
| `work-graph.json` | The source of truth: every item with its stage, track, kind, owner, estimate, status and dependencies |
| `work-graph.html` | The rendered page, generated from the JSON |
| `../../tools/plan/render_graph.py` | The renderer. `python tools/plan/render_graph.py docs/plan/work-graph.json` re-renders, and `--set <id> <status>` updates an item first |

Update the JSON as work moves, then re-render. Statuses: pending, running, review, redeploy, done, failed, self.

## How to read it

- **Kinds:**
  - **build:** code and data.
  - **design:** mockups and prototypes.
  - **content:** art, biomes, species, writing.
  - **research:** spikes.
  - **test:** playtests and exit checks.
  - **gate:** an approval.
  - **release:** a public version.
  - **decision:** an ADR.
- **Owners:**
  - **agents:** Haiku and Sonnet build agents under Opus review.
  - **opus:** design, architecture and integration.
  - **47noodles:** every gate and release.
  - **CallMeCarlos:** concept and block art.
  - **fable:** the licence and IP read.
  - **playtesters:** the exit playtests.
- **Gates** are the design rule from `12` §12.3. Each stage's screens and blocks are designed and approved one stage ahead, and no UI code starts until its gate passes. There are 10 gates in all: Stage 0's go/no-go, the Stage 1 to Stage 8 design gates, and the 1.0 sign-off.
- **Every stage ends the same way:** an exit playtest, a release with patch notes, then a version hop to the latest Minecraft drop with a tested save migration.

## Where things stand (11 Oct 2026)

- **Done:** the design draft, the UI concept pack, and the build setup with the module contract.
- **In progress:** the Stage 0 prototype on the `stage0-prototype` branch. It is a playable slice with ship blocks and assembly, flight physics, orbits with a transfer to Pale, Ages gating, the map and HUD, and a test save with a rocket already in orbit.
- **Next on the critical path:** the two Stage 0 spikes, the cube-sphere seam (0.18) and the Jolt physics spike (0.19), together with the Stage 1 design pack (0.26 to 0.31). All three feed the Stage 0 go/no-go gate (0.32).

## Effort

| Stage | Items | Estimate (person-weeks) |
|---|---|---|
| 0 Foundations | 32 | 31–62 |
| 1 Engineers | 36 | 82–138 |
| 2 Orbits | 20 | 46–70 |
| 3 Round Worlds | 19 | 56–94 |
| 4 Landfall | 23 | 53–86 |
| 5 Together | 17 | 34–56 |
| 6 Expanse | 17 | 75–121 |
| 7 Beyond | 16 | 54–87 |
| 8 Launch | 11 | 18–31 |
| **Total** | **191** | **446–741 (≈103–171 person-months)** |

This bottom-up sum is higher than the top-down 80–130 person-months in `../design/02-feasibility.md`. Treat the gap as a risk signal. The biggest items are the cube-sphere worlds, grid physics, Kinetics and the 50-biome worlds with their life. Those are the items to cut or phase if time runs short.
