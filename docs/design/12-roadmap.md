# 12 — Roadmap: implementation in stages

> Planned by Opus. Shaped by 47noodles (chat, 11 Oct 2026): *"implementation planned in stages… the space engineers side and space dimension… then KSP orbital mechanics, then next… so we can have an MVP rather than make the whole thing at once."*
>
> **Every stage ships something playable on its own.** Each stage ends with a public release, a scope freeze, and a version-hop decision: Minecraft drops are only adopted at stage boundaries (`03` §3.0). Durations assume a core team of 2–4 developers with orchestrated AI assistance, plus content contributors (`02` §2.3).

## 12.1 The stages at a glance

| Stage | Name | Duration | What you can play at the end | Release |
|---|---|---|---|---|
| 0 | Foundations | 2–3 months | Dev builds, plus the approved look: brand, UI kit and every Stage 1 screen and block designed | Dev snapshots |
| **1** | **Engineers** | 6–9 months | **Space Engineers in Minecraft**: build ships block by block, fly them, go to space, mine asteroids, dock | **0.1 (first playable)** |
| **2** | **Orbits** | 4–6 months | **Plus KSP**: launch from your world into real orbit and land on the Moon | **0.2 (SE + KSP MVP)** |
| 3 | Round Worlds | 4–6 months | Planets become round (cube-sphere), with huge view distances | 0.3 |
| 4 | Landfall | 3–5 months | The full progression from flint to the Moon: Ages 0–4, Kinetics, the *Arkwright* story | 0.4 (first full modpack) |
| 5 | Together | 4–6 months | The multiplayer layer: owned comms networks, radio voice, android proxies, hubs, crews | 0.5 |
| 6 | Expanse | 7–8 months | The inner system: Ochre, Cinder and Thalassa, each with 50+ biomes and its own life, plus the Shoals; Age 5 | 0.6 (beta) |
| 7 | Beyond | 6–7 months | The outer system, Hollow, jump drives, the finale. The game can be finished | 0.7 (release candidate) |
| 8 | Launch | 2–3 months | Final polish, hardening and the release kit | **1.0** |
| — | Design track | parallel from Stage 0, one stage ahead | UI, HUD, screens, block models and effects designed and approved before the code that needs them | — |
| — | Content track | parallel from Stage 3 | Biomes, species, art, audio and writing for upcoming worlds | — |

**Calendar:** the stages add up to 38–53 months back to back. With a team of 3–4 they overlap: Landfall runs alongside Round Worlds, and Together alongside Landfall and Expanse. That gives:
- **first playable release (0.1) in about 8–12 months**
- **SE + KSP MVP (0.2) in about 12–18 months**
- **1.0 in about 2.7–3.6 years**

## 12.2 Stage by stage

### Stage 0 — Foundations *(2–3 months)*

- **Build:** repos, CI, the licence gate (`09` §9.3), REUSE, test harness. The `orbit` library, pure Java with unit tests (`03` §3.4). A throwaway gameplay prototype on the VS2 unofficial 26.2 port to feel thrust and gyros early.
- **Design (no game code needed):** brand identity, UI kit v1 with every component and state, the helmet and cockpit HUD, and every Stage 1 screen (grid terminal, cockpit, inventories, machines) as approved clickable prototypes. Also the block-model style and the first functional block models (`13` §13.10).
- **Exit:** the go/no-go checks in `02` §2.4 (orbit-library accuracy, a prototype rocket to orbit, a cube-sphere seam proof of concept, an APC spike on OpenGL and Vulkan, and the approved Stage 1 design pack).

### Stage 1 — Engineers *(6–9 months)*: the Space Engineers layer and the space dimension

- **Build:**
  - **APC physics** (`03` §3.1): grids, collision, mass, joints, split and merge, damage, networking, persistence.
  - **SE systems** (`05`): welding, grinding, projectors, cable-less power in Mekanism Joules, conveyors, atmospheric and hydrogen thrusters, gyroscopes, cockpits, rotors, hinges and pistons, connectors and merge blocks, landing gear, wheels.
  - **Character:** 6DOF jetpack and mag-boots (`05` §5.5.1), airtight rooms, suit O₂ and H₂.
  - **Flight Computer** (Lua).
  - **The space dimension:** fly above the build limit into a void space dimension (bubbles, floating origin, `03` §3.3) with procedural asteroids and a derelict or two. Gravity uses a simple SE-style falloff near the world (R1's falloff formula). No orbits yet.
  - **Multiplayer basics:** server-authoritative grids, control locks, ownership and sharing.
- **Play:** the vanilla world plus Mekanism, with a complete SE-style survival loop.
- **Not yet:** orbits, round planets, Ages, proxies.
- **Exit:** in survival, build a ship from scratch, fly to space, mine an asteroid, dock with a station, and do it with 5 players at 20 TPS.

### Stage 2 — Orbits *(4–6 months)*: the KSP layer

- **Build:**
  - Swap free space for **patched conics**: rails and active switching, SOIs, background burns (`03` §3.4).
  - **Verdance becomes a planet.** The vanilla Overworld is mapped as a **cylinder world**: an equirectangular map the size of the planet, east–west wrap, polar storm walls. This is the planned "tier A" (`03` §3.2.5) used on purpose as a stepping stone.
  - **Pale** is the first moon, with its own small generator.
  - **Transition altitude** and the **Imprint orbit sphere** from the planet's own generator (`03` §3.3.2).
  - **Flight:** navball, map view, maneuver nodes, Δv and staging, engines with Isp curves, aero, re-entry heating, parachutes (`06`).
  - **Comms:** antennas, relays, Ground Station, drone control.
  - **Time:** singleplayer warp, consensus warp, cryo-link (`03` §3.5).
- **Play:** launch from your world, reach real orbit, transfer to Pale, land, come home. **This is the SE + KSP MVP.**
- **Not yet:** round worlds, Ages, other planets.
- **Exit:** a new player gets to orbit and back unaided. Pale landing and return on a 5-player server.

### Stage 3 — Round Worlds *(4–6 months)*

- **Build:**
  - **Planets:** cube-sphere topology, ghost-halo seams, Vertex Spires, curvature shader, latitude climate (`03` §3.2).
  - **Far terrain:** our own Distant Horizons LOD generator straight from planet noise (`17` §17.5).
  - **The round pair:** Verdance on vanilla generation, and Pale, both round. Pale gets its full 50-biome set from the content track.
- **Play:** a round homeworld and moon with enormous view distances.
- **Worlds:** round planets need new worlds. Stage 2 cylinder worlds stay supported as a world type.
- **Exit:** walk, build and fly across every edge type. Surface to orbit with no loading screen. DH LOD matches the Imprint sphere.

### Stage 4 — Landfall *(3–5 months)*: progression

- **Build:**
  - **Ages 0–4** with the `ages` gating engine and the Custodian Log quests (`04`).
  - **Apoapsis Kinetics** (`08` §8.2), Mekanism-gated industry, the component economy (`05` §5.6).
  - **Story and learning:** the *Arkwright* wreck chain, science and Survey Points, the Holo-Manual.
  - **Front end v1:** title screen, universe browser, world-creation wizard, Codex (`13` §13.10).
- **Play:** the first complete modpack journey, from flint to the Moon.
- **Exit:** a full playthrough from Landfall to Pale with no admin help.

### Stage 5 — Together *(4–6 months)*: the multiplayer layer

- **Build:**
  - **Networks and voice:** owned comms networks with permissions, closed by default (`15` §15.6), and the radio voice add-on.
  - **Proxies:** the Link system with the Standard frame and guest pods (`14` §14.1).
  - **Places:** the Verdance hub station, and claims through Open Parties and Claims.
  - **Crew play:** crew consoles and Mission Control, distress beacons.
  - **Servers:** server profiles (`15` §15.1).
- **Play:** friends spread across Verdance and Pale working together live.
- **Exit:** a 10-player playtest weekend with no blocking issues.

### Stage 6 — Expanse *(7–8 months)*: the inner system

- **Build:**
  - **Worlds:** Ochre (with Rook and Pawn), Cinder and Thalassa, **each with 50+ biomes and its own life**, plus the Shoals (asteroid classes) (`07`, `17`).
  - **Industry:** Age 5, ISRU, methalox, ion, nuclear thermal, fission.
  - **Presence:** proxy frames, the Ochre hub, player hubs and alliances, the Discovery Registry, derelict boarding.
  - **World life:** NPC factions and economy, weather states, the transfer planner, autonomy cores.
- **Exit:** Age 5 complete, and all three Precursor fragments obtainable.

### Stage 7 — Beyond *(6–7 months)*: the outer system and the finale

- **Build:**
  - **Worlds:** Tempest (aerostats, rings), Ember, Glacis, Murk, the Wanderer, Hollow.
  - **Endgame tech:** Age 6, fusion, antimatter, jump drives, gravity generators, torch transfers.
  - **The end:** megaprojects, and the Heart finale with its ending choice.
- **Exit:** the game can be finished, and server performance targets are met.

### Stage 8 — Launch *(2–3 months)*

- **Build:**
  - **Front end:** a final polish pass on every screen (v1 shipped in Stage 4, `13` §13.10), auto-generated credits.
  - **Hardening:** save migration, the API freeze and SDK.
  - **Release kit:** docs site, Docker image, trailer (`16`).
- **Exit:** the release checklist (`16` §16.11) passes, and 1.0 ships.

### After 1.0

The Reach (`14` §14.4), the Twinlight system, Atmospherics 2.0, colonists, career mode (`11`), and the NMS-style procedural fork option (`17` §17.6).

## 12.3 Design and content tracks (parallel)

**Design comes before code.** 47noodles (chat, 11 Oct 2026): *"designing UI and visuals should be done before implementation, otherwise implementation of good UI will be lazy."*
- **Design track (from Stage 0):** every stage's UI and visuals are designed one stage ahead of the code that needs them. Stage 0 designs Stage 1, Stage 1 designs Stage 2, and so on.
- **Design gate:** no screen, HUD element or functional block is implemented until its design is approved. *Approved* means:
  - a high-fidelity mockup or clickable prototype covering every state (empty, loading, error, disabled, gamepad focus);
  - a motion and sound spec, final copy, and the accessibility variants;
  - for blocks, the Blockbench model with its animations and damage states.
- **Design QA:** implementation is reviewed side by side against the approved design, and the approved mockups become the reference images for the screenshot tests (`13` §13.8).

**Content track:** from Stage 3, content runs a stage ahead of the code that needs it: biome composer data, creature and plant kits, block palettes, art, audio and writing for the next worlds (`16`, `17`). **Rule:** from Stage 6, a new major world ships only when it reaches 50 biomes and its species targets (smaller bodies follow `17` §17.0). Verdance already has vanilla's biomes. Pale starts in Stage 2 with a small generator and gets its full set in Stage 3.

**Visual design runs on the same track** (`13` §13.10): UI kit and HUD in Stage 1, cockpit and maps in Stage 2, skies and post-processing in Stage 3, the title screen and front end in Stage 4, and a final polish pass in Stage 8.

## 12.4 Team shape (suggested)

| Role | Owns |
|---|---|
| Physics engineer | APC, joints, buoyancy, performance |
| Rendering engineer | Grid rendering, planet LOD, scattering, sky, Vulkan path, Sodium/Iris and DH compat |
| Gameplay engineer | Grids and blocks, flight UI, Flight Computer, ages |
| World engineer | Cube-sphere, halos, worldgen for 12 bodies plus the Shoals and the Wanderer, Imprint service |
| Artist(s) | Style guide, textures, models, UI, sounds (CC BY 4.0), working with the AI art pipeline (`16` §16.2) |
| Content designer | Biome composer data, creature and plant kits (`17`) |
| Audio and writing | Sound design, music, Custodian voice, story bible (`16`) |
| Community / QA lead | Playtests, docs, server testing, blueprint hub |
| AI orchestration | Claude subagents for code, data, tests and art under the leads' review |

## 12.5 Version hops (planned)

| When | Hop |
|---|---|
| Start | 26.3 |
| End of Stage 1 | Latest drop |
| End of each later stage | Latest drop, with about 1–2 weeks budgeted per hop and a tested save migration (`16` §16.7) |

## 12.6 Patch notes for every version

Every release, from each stage's minor version down to hotfixes, ships **patch notes**:
- player-facing notes from the repo's release template (`docs/releases/TEMPLATE.md`), also shown in an in-game **What's new** screen (from 0.4) and posted on Modrinth and CurseForge;
- a technical **CHANGELOG.md** entry;
- upgrade notes for save migration, server config, API changes and Minecraft version hops;
- credits.

Versions use `MAJOR.MINOR.PATCH+MC` (e.g. `0.1.0+26.3`), with MINOR stepping once per stage. Details: `docs/releases/README.md` in the repo.
