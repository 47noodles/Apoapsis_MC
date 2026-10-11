# Changelog

All notable changes to Apoapsis are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow the scheme in [docs/releases/README.md](docs/releases/README.md). Player-facing patch notes for each game release live in [docs/releases/](docs/releases/README.md).

## [Unreleased]

Nothing yet.

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
