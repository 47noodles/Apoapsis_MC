# The work graph to 1.0

> Planned by Opus, at 47noodles' request (11 Oct 2026): *"a work graph, comprehensive, from now until full modpack. put it in the repo too, and HTML it."* It turns the staged roadmap (`../design/12-roadmap.md`) and the feature checklist (`../design/10-feature-checklist.md`) into 211 work items with dependencies, including the Lean MVP slice (stage 0.5).

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
- **Next on the critical path:** the Lean MVP slice (M.01 to M.12), which starts from the Stage 0 prototype and runs alongside the two Stage 0 spikes, the cube-sphere seam (0.18) and the Jolt physics spike (0.19), and the Stage 1 design pack (0.26 to 0.31). The spikes and the design pack feed the Stage 0 go/no-go gate (0.32), which still gates the heavy Stage 1 work.

## Lean MVP

The **Lean MVP** is a playable survival loop that uses all three pillars once, at the quality of the reference videos. It is stage `0.5` in the graph, between Stage 0 and Stage 1.

- **What it is:** a new world starts at Age 3 with vanilla tech and craftable ship parts. The player does First Flight (1,500 m, land intact), then the Age 4 firsts chain (suborbital, orbit, Pale flyby, Pale landing, crewed return), a Pale regolith sample that grants Age 5, a fuel refinery and canisters, parachutes and landing legs, landing aids, one maneuver node, a scripted return trip, multiplayer basics with ship ownership, an in-game guide, and the release (M.01 to M.12).
- **Why it comes first:** the lead wants a lean, playable product sooner. Stage 1's heavy systems (physics backend, grids, power, Mekanism, Lua) are weeks to months of work, and "first playable" was otherwise at 1.35, after all 36 of them. Stage 1 playable (1.35) now depends on the MVP release (M.12).
- **Single-stage rockets only:** no decouplers. Ship splitting stays in Stage 1 (1.05).
- **What waits:** round planets (0.18), walking on moving ships (1.10 and 1.22), grid power (1.13), Mekanism (1.12) and Lua (1.24) stay after the MVP. The map's smooth vector restyle is a post-MVP quality item (1.37).
- **Estimate:** 12 nodes, 12–23 person-weeks.

## Effort

| Stage | Items | Estimate (person-weeks) |
|---|---|---|
| 0 Foundations | 39 | 35–70 |
| 0.5 Lean MVP | 12 | 12–23 |
| 1 Engineers | 37 | 84–141 |
| 2 Orbits | 20 | 46–70 |
| 3 Round Worlds | 19 | 56–94 |
| 4 Landfall | 23 | 53–86 |
| 5 Together | 17 | 34–56 |
| 6 Expanse | 17 | 75–121 |
| 7 Beyond | 16 | 54–87 |
| 8 Launch | 11 | 18–31 |
| **Total** | **211** | **464–775 (≈107–179 person-months)** |

This bottom-up sum is higher than the top-down 80–130 person-months in `../design/02-feasibility.md`. Treat the gap as a risk signal. The biggest items are the cube-sphere worlds, grid physics, Kinetics and the 50-biome worlds with their life. Those are the items to cut or phase if time runs short.
