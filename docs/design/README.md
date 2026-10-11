# Apoapsis — design draft

*Space Engineers' building, KSP's orbits and SevTech's ages, on the latest Minecraft, fully open source.*

> **Status:** first full draft, 2026-10-11. Design and ideas by Opus. Research by ten Haiku subagents (`../research/R1–R10`), with key facts re-verified by the orchestrator. Codename "Apoapsis": no Modrinth project uses the name (R4). "Periapsis" is taken by a CurseForge modpack.

> *Other games and mods (Space Engineers, Kerbal Space Program, SevTech: Ages, Factorio, Stationeers, Outer Wilds, Create, Sable and more) are named only to describe and compare. Their names and trademarks belong to their owners, and Apoapsis isn't affiliated with or endorsed by any of them.*

## The pitch

You wake in an escape pod on **Verdance**, a round world circling the orange star **Meridian**, beside the scattered wreck of the colony ship *Arkwright*. You start with flint and fire. Over seven Ages you work through bronze, steam, brass, steel and electricity. You build ships **block by block** that fly on real physics: thrust, mass, gyros, conveyors, power, welding and damage. Then you launch them into **real Keplerian orbits**, with patched conics, Δv, transfer windows, aerobraking and maneuver nodes. Out there are a tidally locked furnace world, a cold desert where fuel comes from the air, an ocean planet with crushing abyssal vents, a gas giant you skim with floating aerostats, three very different moons, and finally a hollow Precursor world with gravity pointing outward. Everything is open source: code, art, quests and worldgen.

## Pillars

1. **Everything you fly, you built.** Ships are grids of real blocks with real mass, not rocket items.
2. **Orbits are real.** Kepler, not teleport menus. Space is continuous: you climb out of the atmosphere into orbit with no loading screen.
3. **Every world changes how you build.** Each planet has one signature mechanic: heat, dust and ISRU, pressure and buoyancy, radiation, buoyant air, spin gravity.
4. **Progress is earned.** SevTech-style Ages with an in-world reason (the wreck's sealed sections) and the best content last.
5. **Multiplayer-first.** A time model built for shared servers: real time, cryo-link and consensus warp.
6. **Open source, absolutely everything.** MIT code, CC BY 4.0 art. Rebuild whatever core piece isn't open (`09`).

## Documents

| # | File | What's in it |
|---|---|---|
| 02 | [Feasibility](02-feasibility.md) | **Sable on 26.3 (technically feasible, but incompatible with our open-source charter)**, whole-project effort and risks, MVP, go/no-go |
| 03 | [Architecture](03-architecture.md) | Platform, APC physics engine on Jolt, **cube-sphere with ghost-halo seams**, frames and bubbles, orbital engine, **multiplayer time model**, 26.x rendering, performance |
| 04 | [Progression: the Ages](04-progression-ages.md) | Seven Ages (Landfall → Beyond), the *Arkwright* story spine, gate types, quest book |
| 05 | [Construction and grids](05-construction-and-grids.md) | The SE layer: grids, networks, welding, damage, full block catalogue, components, Flight Computer, NPCs |
| 06 | [Flight and orbits](06-flight-and-orbits.md) | The KSP layer: control schemes, engines, staging and Δv, aero and re-entry, navigation, comms, science, jump drives, autopilots |
| 07 | [The Meridian system](07-star-system-and-planets.md) | Star system numbers, every planet and moon, their signature biomes |
| 08 | [Materials and industry](08-materials-and-industry.md) | Mekanism plus Apoapsis Kinetics, resource abundance per world, five new chemicals, fuel chains |
| 09 | [Open source and dependencies](09-open-source-and-dependencies.md) | The charter, licences, clean-room rules, dependency matrix (keep, fork, rebuild, extra) |
| 10 | [Master feature checklist](10-feature-checklist.md) | 180+ checkboxes |
| 11 | [Expansion ideas](11-expansion-ideas.md) | 35+ parked ideas: Lagrange stations, Kessler debris, terraforming, colonists, Twinlight binary system… |
| 12 | [Roadmap](12-roadmap.md) | **Implementation in stages**: 0.1 Engineers (SE layer + space dimension) → 0.2 Orbits (KSP layer, the MVP) → round worlds → Ages → multiplayer layer → inner system → outer system → 1.0 |
| 13 | [Visual design and art direction](13-art-direction.md) | **A polished game, not "a mod"**: brand, UI kit, title screen, HUD, maps, block models, post-processing, game feel |
| 14 | [Best of other games](14-best-of-other-games.md) | **Proxies** (android bodies over the comms network), hub stations, discovery, derelicts, The Reach |
| 15 | [Multiplayer](15-multiplayer.md) | "Nobody waits", server profiles, **owned comms networks**, voice over radio, crews, claims, netcode, scaling |
| 16 | [Production and polish](16-production-and-polish.md) | Art direction and the AI art pipeline, audio, story, controls, front end, quality, modding API, distribution, governance |
| 17 | [Worlds, biomes and life](17-worlds-biomes-and-life.md) | At least 50 biomes per major world, vanilla-derived generators, per-world colours, creature and plant kits, the NMS-style fork note |

## Key numbers

| | |
|---|---|
| Target | Minecraft Java **26.3**, Java 25, NeoForge first |
| Homeworld | Verdance: R = 64 km, 1 g, 20-min day, LEO period 11.6 min, ascent ~1.6 km/s |
| First trip | Verdance → Pale: **23 min real time**, 190 m/s injection |
| Inner planets | 1.4–4 h coasts (cryo-link or consensus warp) |
| Outer system | 8–24 h coasts (and jump drives in Age 6) |
| Effort | ≈ 80–130 person-months. Roadmap: first playable 0.1 in about 8–12 months, SE + KSP MVP in about 12–18 months, 1.0 in about 2.7–3.6 years, for a core team of 2–4 devs plus content contributors, with AI help |

## Decision log

| Decision | By | Where recorded |
|---|---|---|
| Latest Minecraft version, porting or forking dependencies up to it | 47noodles (original brief) | `03` §3.0 |
| Ideas by Opus, research by Haiku | 47noodles (chat) | — |
| Materials tie into Mekanism and Create, no SE ores | 47noodles (chat, 11 Oct 2026) | `08` |
| Open source absolutely everything; rebuild what isn't open | 47noodles (chat, 11 Oct 2026) | `09` |
| Sodium (source-available) is reused as an optional on-by-default extra, not rebuilt | Opus recommendation after 47noodles' question | `09` §9.4 |
| Own physics engine (APC on Jolt) instead of Sable or VS2 | Opus | `03` §3.1 |
| Cube-sphere with ghost-halo seams; cylinder fallback | Opus | `03` §3.2 |
| Real time + cryo-link + consensus warp | Opus | `03` §3.5 |
| Create's role becomes the original Apoapsis Kinetics module | Opus, following the open-source rule | `08` §8.2 |
| Android proxies you link into over the comms network | 47noodles (chat) | `14` §14.1 |
| Multiplayer is a core pillar; radio rides the same network as proxies | 47noodles (chat) | `15` |
| Comms networks are owned, independent and closed by default, easy to open | 47noodles (chat) | `15` §15.6 |
| Java Edition: keyboard and mouse first in every screen and key hint; gamepad support through Controlify | 47noodles (chat) | `13` §13.2 |
| UI and visuals are designed and approved before they're implemented (design gate, one stage ahead) | 47noodles (chat) | `12` §12.3, `13` §13.10 |
| Start on vanilla worldgen; every world gets its own vanilla-derived generator with at least 50 biomes on every major world and its own life | 47noodles (chat) | `17` |
| AI-created art with human review | 47noodles (chat) | `16` §16.2 |
| Visual design is a major pillar: feel like a polished game, not Minecraft (UI, menus, HUD) | 47noodles (chat) | `13` |
| NMS-style procedural galaxy and random spawns are a fork option, not core | 47noodles (chat) | `17` §17.6 |

## Open questions for 47noodles and CallMeCarlos

1. **Code licence:** MIT everywhere (default), or LGPL-3.0 for the physics engine so engine improvements stay open?
2. **Default universe scale:** `compact` (default: real-time moon trips) or `classic` (KSP-like, warp-heavy)?
3. **Weapons and PvP:** on by default, or a server opt-in?
4. **Loader:** NeoForge-only to start (recommended), or multiloader from day one?
5. **Name:** keep "Apoapsis"? (A trademark search before a public launch is cheap.)
6. **Team:** who takes physics, rendering, worlds and art? The roadmap assumes 2–4 people plus AI orchestration.
