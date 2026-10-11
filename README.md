# Apoapsis

**Space Engineers-style building, KSP-style orbits and SevTech-style ages: a fully open-source Minecraft mod and modpack for the latest Minecraft Java (26.3).**

> **Status: design phase.** No code yet. This repository currently holds the first full design draft (October 2026) and the research behind it.

## What it is

You crash-land on **Verdance**, a round world circling the star **Meridian**, beside the wreck of the colony ship *Arkwright*. You start with flint and fire and climb through seven Ages: bronze, steam, brass, steel, electricity, rocketry and beyond. You build ships **block by block** that fly on real physics. You launch them into **real Keplerian orbits** (patched conics, Δv, transfer windows, aerobraking). Then you explore a star system of very different worlds: a tidally locked furnace planet, a cold desert where fuel comes from the air, an ocean world with crushing abyssal vents, a gas giant you skim with floating aerostats, three moons, and a hollow Precursor world where gravity points outward.

## Read the design

Start with **[docs/design/README.md](docs/design/README.md)**. Highlights:

- [Feasibility](docs/design/02-feasibility.md): porting Sable to the latest version, effort, risks, MVP
- [Architecture](docs/design/03-architecture.md): our own physics engine on Jolt, round planets via cube-spheres with "ghost-halo" seams, multiplayer time warp
- [The Meridian system](docs/design/07-star-system-and-planets.md): every planet, moon and biome
- [Open-source charter](docs/design/09-open-source-and-dependencies.md): what we keep, fork or rebuild
- [Visual design](docs/design/13-art-direction.md): UI, title screen, HUD and game feel, so it plays like a polished game rather than a mod
- [Staged roadmap](docs/design/12-roadmap.md): 0.1 Engineers → 0.2 Orbits (MVP) → … → 1.0
- [Master feature checklist](docs/design/10-feature-checklist.md)

The raw research notes the design draws on are in [docs/research](docs/research/README.md).

Every version ships patch notes: see the [changelog](CHANGELOG.md) and [releases](docs/releases/README.md).

## Open source, absolutely everything

- **Code:** [MIT](LICENSES/MIT.txt)
- **Docs, art, models, sounds:** [CC BY 4.0](LICENSES/CC-BY-4.0.txt)
- Nothing closed or source-available is bundled in the pack. Core systems that aren't open get rebuilt from scratch as original work. See [CONTRIBUTING.md](CONTRIBUTING.md) for the clean-room rules.

*Apoapsis is an independent fan project. It is inspired by, and the design docs compare it with, other games and mods, including Space Engineers, Kerbal Space Program, SevTech: Ages, Factorio, Stationeers, Outer Wilds and others. All game and mod names and trademarks belong to their owners and are used only to describe and compare. Apoapsis is not affiliated with or endorsed by any of them, or by Mojang Studios or Microsoft. Minecraft is a trademark of Mojang Synergies AB.*

**What "open source" covers:** everything Apoapsis authors is MIT (code) or CC BY 4.0 (docs and art). The modpack also uses other open-source mods under their own licences (MIT and LGPL), plus optional extras that the launcher downloads from their official pages. Details: [licence audit](docs/design/09-open-source-and-dependencies.md#98-licence-audit-of-the-whole-plan-can-it-really-be-mit--cc-by).

## People

Started by **47noodles** and **CallMeCarlos**. Design draft written with Claude (Anthropic).
