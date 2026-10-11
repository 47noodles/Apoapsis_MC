# 02 — Feasibility: Sable on the latest Minecraft, and Apoapsis as a whole

> Assessment by Opus from R4, R5, R6, R10 and the orchestrator's own checks (✔ = verified against a primary source on 2026-10-11). Effort figures are judgement calls, stated as ranges, for experienced Minecraft modders. AI assistance (orchestrated Claude subagents writing code under review) is assumed to cut the *mechanical* share of the work, not the debugging and playtesting.

## 2.1 The platform today

| Fact | Value |
|---|---|
| Latest Minecraft Java | **26.3 "Wilderness Bound"**, released 2026-09-15 ✔. 26.4 is in snapshots (snapshot 3, 2026-10-06) |
| Versioning | year.drop.hotfix since 2026: 26.1 (Mar), 26.2 (Jun), 26.3 (Sep), with roughly four drops a year (R5) |
| Obfuscation | Gone since 26.1. Mojang names everywhere, no intermediary, Yarn discontinued (R5 §2) |
| Java | 25 |
| Renderer | Code `RenderPipeline` (1.21.5) → extract-then-render (26.1) → **Vulkan backend** (26.2, experimental; "Prefer Vulkan" is the default in 26.4 snapshots) → SDL plus the `renderpearl` package split (26.3) (R5) |
| Loaders on 26.3 | NeoForge 26.3.0.x (beta), Forge 66.0.x, Fabric Loader 0.19.5 (R5) |
| Big mods on 26.x | **No releases from Sable ✔, Create ✔, Mekanism ✔, Immersive Engineering or official VS2 ✔.** Mekanism's upstream default branch is already `26.3` (10.8.0, alpha ✔). JEI, Lithium, Distant Horizons, Sodium, Iris and AE2 (alpha) are on 26.3 |
| Share of Modrinth mods per version | 1.20.1 45%, 1.21.1 41.5%, 26.1 15%, 26.2 17.6%, 26.3 9.7% (R5) |

The content ecosystem is still mostly on **1.21.1**. "Latest version" means Apoapsis carries its own world and doesn't wait for anyone. That's achievable because Apoapsis rebuilds its core systems anyway (`09`) and its one big dependency, Mekanism, is MIT and can be ported by us.

## 2.2 Feasibility of bringing Sable to 26.3

### Technical

| Measure (R6, recounted ✔) | Sable (main, 1.21.1) |
|---|---|
| Code | 58.6k lines Java, 5.9k lines Rust (Rapier fork, JNI), ~840 lines GLSL, one bundled 9 MB native archive |
| Mixin files | 396 (228 common, 158 NeoForge, 10 Fabric), plus 1 mixin-plugin file |
| Mixins by subsystem | Rendering 56 · Collision and entity movement 47 · Chunk and world storage 35 · Block/pathing 22 · Networking 17 · Entities 16 · Worldgen 2 · **Third-party 171 files (Create: 116 targets; also Flywheel, Catnip, Jade, Iris, Sodium and others)** |
| Access wideners/transformers | 123-line AW + 71-line AT |
| Java target | 21 (26.3 needs 25) |

**What it would take.** Moving from 1.21.1 to 26.3 crosses every major internal rework since 2024:
- **Rendering:** 56 files in the subsystem rewritten three times, plus the Vulkan path (raw GL breaks).
- **Collision and entity movement:** signatures changed in 1.21.2.
- **World storage:** the 1.21.5 tickets and SavedData rework, plus 26.1's world-storage changes.
- **Networking:** packets changed to records and float rotations.
- **Third-party compatibility:** 171 of its mixin files hook other mods, mostly Create (116 targets), which has **no 26.x build yet** (its 26.1 port is "in progress" per R10).

**Estimate:** **3–6 person-months** for Sable core alone with an experienced porter, plus **1–2 months** for its Create layer once Create ships on 26.x. Rendering on the Vulkan backend is the largest unknown. **Precedent:** the unofficial VS2 port reached 26.2 ✔ (official VS2 has 417 mixin files, comparable to Sable; the port's own count wasn't measured), so this class of port *can* be done by a small team.

**Verdict: technically feasible, moderately hard.**

### Legal and charter

- Sable is **PolyForm Shield 1.0.0** ✔. It allows changes and distribution for any purpose *except* providing a product that competes with the software, or with products the licensor provides using it.
- A straight port *might* fit inside that. **Using it inside Apoapsis is risky**, because an SE-style ship mod could compete with Create Aeronautics, which ryanhcode provides on Sable.
- Shield isn't an open-source licence, so even a clean exception wouldn't meet 47noodles' open-source charter (`09`).

**Verdict: not suitable for Apoapsis.** Spend the same 26.x effort on **APC** (`03` §3.1). It needs the same porting know-how (entity motion, rendering, storage) but produces an **MIT engine with orbital frames built in**, with no upstream to wait on and no licence risk.

### Answers to the founding questions

- **"Adding KSP mechanics is fairly easy."** Mostly right. The maths (patched conics, Tsiolkovsky, drag) is textbook and lives in a small, testable library (`03` §3.4). The *hard* parts are the **multiplayer time model** (`03` §3.5) and **rendering planets from orbit**.
- **"Mapping a Minecraft world onto a sphere is the biggest issue."** Agreed. That's why `03` §3.2 puts the most design into it. **Ghost-halo seams** keep each face as plain vanilla terrain with vanilla gravity, so the sphere costs a server-side sync system rather than a rewrite of Minecraft's physics and renderer. **Vertex Spires** hide the eight corners, and a **cylinder-world fallback** exists if seams slip.
- **"Hardest part would be porting Sable to 1.20.1."** That task is no longer needed. Apoapsis targets 26.3 and doesn't port Sable at all. The hardest part is now **APC's entity-on-grid motion and rendering on 26.x**, which has to be done well whichever engine is used.

## 2.3 Feasibility of Apoapsis as a whole

### Work breakdown

| Work package | Person-months | Notes |
|---|---|---|
| `orbit` library (propagation, events, Lambert, presets) | 1–1.5 | Textbook maths, fully unit-testable |
| **APC physics** (sub-levels, collision, entity motion, joints, rendering, networking, persistence) | **8–14** | Critical path and highest risk |
| **Planets** (cube-sphere, ghost halos, Vertex Spires, 12 bodies' worldgen plus the Shoals and the Wanderer, Imprint LOD, sky bodies, scattering) | **8–12** | Second-highest risk |
| Grids and SE systems (~150 blocks, networks, welding, projector, damage, life support) | 8–12 | Lots of content, moderate risk |
| Flight UI (navball, map view, nodes, planner, autopilots, Flight Computer Lua) | 4–6 | |
| `ages`, quests, *Arkwright* story and structures | 4–6 | |
| Apoapsis Kinetics | 4–6 | |
| Mekanism 26.x port fork (only if upstream's 26.3 alpha lags) | 0–2 | MIT ✔. Upstream's default branch is `26.3` ✔ |
| Original art (700–1,000 textures, 150–250 models, sounds) | 6–10 (artist) | Community artists, CC BY 4.0 |
| Multiplayer hardening, performance, QA | 4–6 | |
| Link system and proxies | 3–5 | `14` §14.1 |
| Hubs, Discovery Registry, derelicts | 2–3 | `14` |
| Multiplayer systems (owned comms networks, voice add-on, claims, crew consoles, control locks) | 3–5 | `15` |
| Biome and life breadth (at least 50 biomes per major world, creature and plant kits, their art) | 10–16 | `17`, the largest content item |
| Audio (sound design, music) | 3–5 | `16` §16.3 |
| Story and writing | 2–4 | `16` §16.4 |
| Visual design and front end (UI kit, HUD and maps, title screen, world wizard, credits, post-processing, dynamic lights, animated block models, connected-texture port) | 5–8 | `13`, `16` §16.6 |
| API, SDK, docs site | 3–4 | `16` §16.8 |
| Distribution and DevOps | 1–2 | `16` §16.9 |
| QA automation and save migration | 2–3 | `16` §16.7 |
| **Total** | **≈ 80–130 person-months** (rows sum to 81–130.5) | |

Without AI help that's roughly **7–11 years solo**, or about **1.7–3.6 years of effort for a team of 3–4** working in parallel. The roadmap (`12`) sequences the stages around the work that can't run in parallel, with a parallel content track. It comes to **about 8–12 months to the first playable release (0.1 Engineers), about 12–18 months to the SE + KSP MVP (0.2 Orbits), and about 2.7–3.6 years to 1.0**, for a core team of 2–4 developers plus content contributors, with heavy AI assistance on code and art.

### MVP: built in stages (`12`)

The project ships as playable stages rather than all at once:
1. **0.1 "Engineers"** (Stages 0–1): the Space Engineers layer on the vanilla world. APC grids, welding, power, conveyors, thrusters, gyros, cockpits, 6DOF jetpack, and the space dimension with asteroids.
2. **0.2 "Orbits"** (Stage 2), **the SE + KSP MVP**: real orbits, map view, maneuver nodes, staging and Δv, re-entry. The vanilla world becomes a (cylinder) planet, and Pale is the first moon.
3. Everything after that (round worlds, Ages, multiplayer layer, new planets) is added stage by stage.

This is the smallest slice that proves all three pillars: SE building, KSP orbits and SevTech-style progression.

### Risk register

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| 1 | Entity-on-grid motion, 6DOF player orientation and grid rendering on 26.x/Vulkan prove harder than estimated | Medium | Very high | Prototype gameplay on the VS2 unofficial port (Stage 0; it's on 26.2, one drop behind, which is fine for a throwaway prototype). One `EntityGridMotion` facade. A dedicated rendering engineer |
| 2 | Version churn (about four drops a year) | High | Medium | Hop only at stage boundaries. Mixin facades per subsystem. Unobfuscated names make hops cheaper |
| 3 | Ghost-halo seams misbehave (block entities, redstone, mobs, Kinetics shafts across edges) | Medium | High | Machines don't tick on halos. Exhaustive seam test worlds. Cylinder fallback (tier A) |
| 4 | Players reject the multiplayer time model | Medium | High | The compact system keeps early trips real-time. Cryo-link plus consensus warp. Playtest at Stage 2 |
| 5 | Mekanism isn't usable on 26.x when Stage 1 needs it | Medium | Medium | Upstream's default branch is already `26.3` (10.8.0 alpha ✔, checked 2026-10-11). If it isn't usable in time, our own MIT port fork (`09` §9.7), budgeted above |
| 6 | Art volume overwhelms the team | High | Medium | Style guide early, community artists, CC BY credits, flat-colour placeholders |
| 7 | Scope creep | High | High | Staged releases (`12`), stage gates, expansion ideas parked in `11` |
| 8 | Server performance with many grids | Medium | High | Rails for idle grids, complexity budget, physics on its own threads, a perf CI scenario |
| 9 | Sodium/Iris break custom rendering | Medium | Medium | Compat module plus CI smoke tests with and without Sodium |
| 10 | Licence or IP challenge | Low | High | Charter and clean-room rules (`09` §9.3), CI licence gate, review before 1.0 |
| 11 | jolt-jni lacks a double-precision build or 26.x-friendly natives | Low–medium | Medium | Bubble-local float physics works anyway. Rapier backend as plan B |
| 12 | Biome and species breadth (50+ biomes per major world) outruns content capacity | High | Medium | Biome composer and creature/plant kits (`17`), AI-assisted art pipeline (`16` §16.2), per-world biome counts tracked at every stage |
| 13 | The polished-game look (UI kit, HUD, post-processing, animated models, own dynamic lights) costs more than planned or clashes with other mods' rendering | Medium | Medium | Designed and approved before implementation, one stage ahead (`12` §12.3). One UI kit built once and reused everywhere (`13` §13.10). Classic UI toggle. Per-effect sliders and a 1 ms post-processing budget. Golden-image tests on OpenGL and Vulkan (`13` §13.8) |

### Overall verdict

**Feasible as a staged, open-source community project.** It isn't a weekend mod. The three genuinely hard problems are APC on 26.x, sphere topology and multiplayer time, and each has a concrete design plus a fallback. Everything else is a large but well-understood amount of content. The open-source charter helps: it removes every upstream dependency that could stall the project and invites contributors.

## 2.4 Go/no-go checks for the first three months (Stage 0)

1. An orbit-library test suite passes: round-trip state ↔ elements, SOI transitions, and a 10-orbit propagation drift under 1 m.
2. On the VS2-port prototype, a player flies a staged rocket from a test world into a stable orbit (rails), coasts, and returns, all at 20 TPS.
3. A cube-sphere seam proof of concept: walk, build and fly across one face edge with mirrored chunks and no visible pop.
4. An APC spike: a 500-block grid with a player standing on it, moving and rotating, rendered through 26.3's pipeline on both the OpenGL and Vulkan backends.
5. The Stage 1 design pack is approved: brand, UI kit v1, HUD and every Stage 1 screen and block (`12` §12.3).

If 3 or 4 fails badly, fall back (cylinder worlds, or the VS2 port as a longer-term host) and re-plan before committing to Stage 1.
