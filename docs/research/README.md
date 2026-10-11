# Research notes

These are the raw research reports the Apoapsis design was built on, written on 2026-10-11 by ten AI research agents (Claude Haiku) working under a reviewing orchestrator (Claude Opus).

**Read them as notes, not as gospel.** Each report tags its facts:
- **verified** or **[V]/[F]**: read on the source page
- **[S]/[A]**: taken from a search summary or aggregator
- **UNVERIFIED**: could not be confirmed

The facts the design relies on most (versions, licences, mod availability) were re-checked by the orchestrator against primary sources. Those are marked ✔ in the design docs. Anything else here should be checked before you build on it.

| File | Topic |
|---|---|
| [R1-space-engineers.md](R1-space-engineers.md) | Space Engineers 1 and 2: blocks, systems, gravity formula, planets, reception |
| [R2-sevtech-ages.md](R2-sevtech-ages.md) | SevTech: Ages, each Age's gates, the Game Stages family, the full mod list, lessons |
| [R3-ksp-orbital.md](R3-ksp-orbital.md) | KSP's simulation model, rocketry, aero, the Kerbol table, multiplayer time warp precedents, maths appendix |
| [R4-mc-physics-space-mods.md](R4-mc-physics-space-mods.md) | Minecraft physics and space mod landscape, what Sable is, name check |
| [R5-latest-mc-platform.md](R5-latest-mc-platform.md) | Minecraft 26.x: versions, deobfuscation, renderer changes, loaders, update cadence |
| [R6-sable-deep-dive.md](R6-sable-deep-dive.md) | Sable vs Valkyrien Skies 2 code measurements (line counts, mixin targets) for porting feasibility |
| [R7-spherical-planets-techniques.md](R7-spherical-planets-techniques.md) | Round planets in voxel games, precision, orbit-view rendering |
| [R8-adjacent-games.md](R8-adjacent-games.md) | Distinctive mechanics from about 19 other space and automation games |
| [R9-worldgen-dimension-tech.md](R9-worldgen-dimension-tech.md) | Worldgen, dimension types, entity attributes, runtime dimensions, fluids, performance on 26.x |
| [R10-mekanism-create-materials.md](R10-mekanism-create-materials.md) | Mekanism and Create materials, chains, licences and port status |

**Quoted material** (short quotes from wikis, licences, changelogs and articles, each linked to its source) belongs to its owners and is included for research and review. The CC BY 4.0 licence on this folder covers only our own text. Some tables restate factual data from community wikis under their own licences, cited inline: the Minecraft Wiki (minecraft.wiki, CC BY-NC-SA 3.0) and the Space Engineers wiki (spaceengineers.wiki.gg, CC BY-SA 4.0). Facts aren't copyrightable, but if you reuse those tables, credit the wikis and check their licences.

The Sable measurement (R6) only counted files and listed which vanilla classes its mixins target. No Sable code was copied, and the temporary clone was deleted (see the clean-room rules in `../design/09-open-source-and-dependencies.md`).
