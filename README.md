# Apoapsis

**Space Engineers-style building, KSP-style orbits and SevTech-style ages: a fully open-source Minecraft mod and modpack for the latest Minecraft Java (26.3).**

> **Status: Stage 0 prototype (0.0.2-alpha.1).** The first playable slice is on the `stage0-prototype` branch. The design draft (October 2026) and the research behind it are here too.

## Try the prototype

The **Stage 0 prototype** (0.0.2-alpha.1) is the first playable slice, with placeholder art and UI. Build a ship and fly it, reach orbit, transfer to Pale and land. Ship parts unlock as you advance through the Ages. Patch notes: [docs/releases/0.0.2-alpha.1.md](docs/releases/0.0.2-alpha.1.md).

### From source (recommended)

You need Git and any JDK 21 or newer to start Gradle. Gradle fetches Java 25 itself. On a fresh machine, run these three commands in a terminal, one at a time:

```bash
git clone -b stage0-prototype https://github.com/47noodles/Apoapsis_MC.git
cd Apoapsis_MC
./gradlew runTestFlight
```

On Windows PowerShell, use `.\gradlew.bat runTestFlight` for the last command (the `./gradlew` form fails there). The first run downloads Minecraft and NeoForge, so it takes a while. The window opens at 1600x900.

It opens straight into the test save, **Apoapsis Test Flight**, with a Kestrel rocket already in orbit and a second one on a pad at spawn. To reset the test save, delete `run/saves/Apoapsis Test Flight`, or run `/apoapsis scenario orbit` to get the same start again. To start the normal game instead:

```bash
./gradlew runClient
```

### As a jar

Open the **Actions** tab on GitHub, pick the latest **Build** run on `stage0-prototype`, and download the `apoapsis-<short sha>` artifact. Unzip it and put the jar in the mods folder of a NeoForge 26.3.0.69-beta instance, for example one made with [Prism Launcher](https://prismlauncher.org/) or CurseForge. Then create a creative world with cheats on and run `/apoapsis scenario orbit` for the same start as the test save.

### Controls

| Key | Action | Notes |
|---|---|---|
| Right-click cockpit block | Board: assemble and sit | |
| Right-click a ship | Sit in its cockpit | |
| W / S | Pitch: tip the nose forward / back | Vanilla movement keys, read while piloting |
| A / D | Yaw: tip the nose left / right | |
| Q / E | Roll left / right | While piloting, Q's drop and E's inventory are suppressed |
| R / F | Throttle up / down | While piloting, F's swap-hands is suppressed |
| Z / X | Full / cut throttle | |
| G | Cycle SAS: Off, Hold, Prograde, Retrograde | Hold keeps the attitude. Prograde and Retrograde point the nose along or against your velocity. Not T: T is chat |
| B | Disassemble (landed and slow) | |
| M | System map | |
| . / , | Time warp up / down | Orbit only, engines off, singleplayer. Works on the map too |
| Shift | Leave seat | Vanilla dismount |

### A first flight

From orbit, in the Kestrel from the test save:

1. Press **M** to open the system map.
2. Wait for the transfer window. In orbit, **.** warps time forward, on the map as well.
3. Set SAS to **Prograde** with **G** (it cycles Off, Hold, Prograde, Retrograde), then burn about 190 m/s.
4. Coast until Pale's gravity takes over at its sphere of influence.
5. Set SAS to **Retrograde** with **G** to brake, then land. Touch down below about 10 m/s: between 10 and 12 m/s is a hard landing, and anything faster (or sliding into the ground above 30 m/s) destroys the ship.

From the pad, with the second Kestrel:

1. Full throttle (**Z**) and climb.
2. Tip over gradually to the east to build horizontal speed.
3. Aim for 15 km and about 710 m/s, which is the speed of a circular orbit at that height.

### Commands

The test save has cheats on, so these work there:

- `/apoapsis scenario pad`: builds a fuelled Kestrel on a pad next to you and raises you to Age 4.
- `/apoapsis scenario orbit`: puts a Kestrel in a 16 km circular orbit around Verdance, with you in the cockpit.
- `/apoapsis age`: shows your Age. `set` and `sandbox` need permission level 2.
- `/apoapsis guide`: the Custodian's guide shows each step once, in chat, as you reach it; this shows your current step again. `/apoapsis guide book` gives you the whole guide as a book.

### Known limits

- Flat worlds only. Round planets come in Stage 3.
- Simple physics. Real grid physics comes later.
- No spacewalks: you cannot leave a ship in orbit.
- Placeholder art and UI.
- Time warp works only in singleplayer.

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
