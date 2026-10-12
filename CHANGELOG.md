# Changelog

All notable changes to Apoapsis are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow the scheme in [docs/releases/README.md](docs/releases/README.md). Player-facing patch notes for each game release live in [docs/releases/](docs/releases/README.md).

## [Unreleased]

### 0.0.2-alpha.1 — Stage 0 prototype

Unreleased, on the `main` branch. Placeholder art and UI.

#### Added
- **Orbit library** (`orbit/`, pure Java, no Minecraft classes): Kepler propagation, orbital elements, Meridian system bodies, patched-conic trajectory predictor, RK4 integrator.
- **Ship blocks and assembly** (`dev.apoapsis.grid`, `dev.apoapsis.ship`): hull plate, cockpit, gyroscope, fuel tank, Spark thruster, regolith and hydrolox canister. Right-click a cockpit to board; the blocks become one moving ship. Disassemble a landed ship back into blocks.
- **Flight physics:** surface flight with gravity, thrust and drag; rate-controlled rotation from gyroscopes with SAS; fuel burn from thrust and specific impulse.
- **Orbit and Pale dimensions** (`apoapsis:orbit`, `apoapsis:pale`): ships in orbit live in a starry void, and Pale is a dry regolith moon at 0.16 g. Flight regimes, dimension transitions, universal time, and time warp in singleplayer.
- **System map and flight HUD:** full-screen map of the Meridian system on M, and an in-flight HUD.
- **Ages gating:** Ages 0 to 4 (Landfall, Hearth, Steam and Brass, Current, Ascent) with milestones. Ship parts are locked by Age, with tooltips and an action-bar line. *First Flight* (1,500 m in a ship) raises you to Age 4. `/apoapsis age` and a per-world sandbox flag.
- **Test scenarios and test save:** `/apoapsis scenario pad` and `/apoapsis scenario orbit`, and `gradlew runTestFlight`, which opens the *Apoapsis Test Flight* save with a Kestrel in orbit and a second on a pad.
- **CI:** GitHub Actions workflow that builds on every push and pull request, runs the GameTests, and uploads the mod jar as an `apoapsis-<short sha>` artifact.
- **3D system map and maneuver node:** the map key now opens a 3D map with textured planets (Tab flips to the 2D map; `-Dapoapsis.map3d=false` makes the 2D map the default). Click the orbit line to place a node, drag one of six handles to set its delta-v, and read the orbit after the burn, its apoapsis and periapsis, the burn time and the time to ignition. Right click or Delete removes the node. Home and last-landing markers sit on the bodies. `gradlew runTestMap` drives the map by script.

## [Design draft 0.0.1] — 2026-10-11

The first full design draft. No game code yet.

### Added
- **Design docs** (`docs/design/`), sixteen sections:
  - Feasibility, including porting Sable to the latest Minecraft
  - Architecture: own physics engine on Jolt, cube-sphere planets with ghost-halo seams, multiplayer time model
  - Progression Ages; construction and grids (the Space Engineers layer); flight and orbits (the KSP layer)
  - The Meridian star system; materials on Mekanism plus Apoapsis Kinetics; the open-source charter and licence audit
  - The master feature checklist and expansion ideas
  - The staged roadmap (0.1 Engineers → 0.2 Orbits MVP → … → 1.0), with UI and visuals designed before code
  - Best of other games (android proxies, hub stations), multiplayer (owned comms networks, voice over radio)
  - Visual design and art direction (UI kit, title screen, HUD, game feel); production and polish; worlds, biomes and life
- **Concept art log** (`docs/design/13-art-direction.md` §13.8), starting with CallMeCarlos's thruster concept (small 1×1×2, large 3×3×4).
- **Research notes** (`docs/research/`): ten research reports with sources, checked during four design review rounds.
- **Orbit calculator** (`tools/calc/system.py`): reproduces the star system's derived figures (transfer times, capture speeds, tides, phase cycles).
- **Licences:** MIT for code and data (`LICENSE`, `LICENSES/MIT.txt`), CC BY 4.0 for docs and art (`LICENSES/CC-BY-4.0.txt`), REUSE metadata.
- **Contributing guide** with clean-room rules, and this changelog plus a release-notes template.
