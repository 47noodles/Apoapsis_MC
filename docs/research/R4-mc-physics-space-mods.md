# R4: Minecraft physics, ship, planet and space mod landscape

Research node R4 for the Apoapsis design. Date of research: 2026-10-11. Facts only; no design recommendations.

## Method and limits

- Modrinth API v2 queried directly (project, version, search, tag list, members). Sources are the URLs given in each row, e.g. https://api.modrinth.com/v2/project/sable.
- GitHub REST API used first. Unauthenticated rate limit (HTTP 403) was hit partway through; later GitHub reads used raw.githubusercontent.com file URLs and WebFetch on github.com pages.
- WebSearch was used for identity, lineage and discovery. The per-turn WebSearch budget (200) was exhausted before the final checks. The items that depended on those checks are marked UNVERIFIED: the identity of Krunch, separate searches for Escape Velocity, Starwright and Voidwright, forks of Ad Astra, the existence of a standalone Space Exploration mod, and Stellar Engineering.
- CurseForge: direct requests return HTTP 403 to automated clients (https://www.curseforge.com/minecraft/search?...&search=apoapsis, checked 2026-10-11). CurseForge facts below come only from search-result snippets and are marked as such.
- Latest Minecraft release: Java Edition 26.3 "Wilderness Bound", released 2026-09-15 (https://minecraft.wiki/w/Java_Edition_26.3, fetched). Latest snapshot: 26.4 Snapshot 3 (2026-10-06). Confirmed by https://api.modrinth.com/v2/tag/game_version.
- Nothing was downloaded, no sign-ins were made, nothing was posted, and nothing was written outside this file.

---

## 1. Sable identification

**Verdict:** One project fits. "Sable" is a Minecraft library mod by ryanhcode that adds moving "sub-levels" (block regions that move with physics) and powers the physics of the Create Aeronautics / Simulated suite. No other project titled "Sable" surfaced as a competitor; the many "Sable ..." projects are addons (see the dependents list below).

**Confidence:** High for author, purpose, license, source availability and Minecraft versions. Medium for the feature list, which comes from the Modrinth description and README rather than the code.

### Facts

| Item | Fact | Source |
|---|---|---|
| Author | ryanhcode (GitHub owner and Modrinth author). Modrinth team id 856jGBG5; members not fetched. | https://modrinth.com/mod/sable ; https://github.com/ryanhcode/sable |
| Credits | Dimforge (Rapier); Eriksonn (sub-level splitting region algorithm, floating blocks); Ocelot (sub-level renderer, optimization, API help); Cyvack and BeeIsYou (Create compatibility, lift math); KyanBirb (assets); Cake; Rhyguy1 | https://api.modrinth.com/v2/project/sable (body) |
| Purpose | Library mod that adds "sub-levels": moving regions of blocks, block entities and entities that remain interactive while assembled. Sub-levels are placed at a separate dynamic position and orientation within a level. | https://modrinth.com/mod/sable ; https://github.com/ryanhcode/sable (README) |
| Physics | Physics pipeline built on the Rapier physics engine written in Rust, compiled to native code (gradle task sable_rapier:buildRustNatives). Sub-levels react to changes in the world. | https://github.com/ryanhcode/sable (README); https://modrinth.com/mod/sable |
| Survival content | Adds no survival items and no survival way to build sub-levels. Assembly is done with the /sable command (assemble, modify, query). | https://modrinth.com/mod/sable |
| Compatibility | Describes itself as "an incredibly intrusive mod" that uses extensive mixins and is prone to compatibility issues. | https://modrinth.com/mod/sable ; https://github.com/ryanhcode/sable |
| Developer surface | Developer wiki at https://github.com/ryanhcode/sable/wiki. Optional compatibility helper "Sable Companion" at https://github.com/ryanhcode/sable-companion. | https://github.com/ryanhcode/sable (README) |
| Source | Public GitHub repository, default branch main. Created 2026-04-15, last push 2026-10-05, 494 stars, 1042 open issues (GitHub count includes PRs). Not archived. | https://api.github.com/repos/ryanhcode/sable (fetched 2026-10-11) |
| License | PolyForm Shield License 1.0.0 (LICENSE.md in repo). Modrinth id: LicenseRef-PolyForm-Shield-License-1.0.0. GitHub SPDX field: NOASSERTION ("Other"). This is source-available, not an OSI open-source license. | https://github.com/ryanhcode/sable/blob/main/LICENSE.md ; https://modrinth.com/mod/sable |
| License terms (quoted from LICENSE.md) | Copyright license to do anything that would otherwise infringe, for any permitted purpose; distribution and changes permitted under the distribution and "Changes and New Works" licenses. "Noncompete": "Any purpose is a permitted purpose, except for providing any product that competes with the software or any product the licensor or any of its affiliates provides using the software." "Competition": "Goods and services compete even when provided free of charge. If you market a product as a practical substitute for the software or another product, it definitely competes." Recipients of copies must also receive the terms. | https://github.com/ryanhcode/sable/blob/main/LICENSE.md |
| Minecraft versions | All 28 Modrinth versions list 1.21.1 only. Loaders: fabric and neoforge. No 26.x build found. | https://api.modrinth.com/v2/project/sable/version (fetched 2026-10-11) |
| Latest release | 2.0.6+mc1.21.1: NeoForge 2026-10-03T04:17:59Z, Fabric 2026-10-03T04:18:01Z. Previous: 2.0.5 (2026-08-18), 2.0.4 (2026-08-17), 2.0.3 (2026-06-17). GitHub release tags: mc1.21.1-2.0.6-neoforge and mc1.21.1-2.0.6-fabric. | https://github.com/ryanhcode/sable/releases ; https://modrinth.com/mod/sable |
| Second source (CurseForge) | Search snippet: 2.0.6 for 1.21.1, NeoForge and Fabric, dated 2026-10-03; over 5.5M downloads on CurseForge. Not opened directly (HTTP 403). | https://www.curseforge.com/minecraft/mc-mods/sable (search snippet) |
| Downloads | 6,185,766 on Modrinth (2026-10-11) | https://modrinth.com/mod/sable |
| Platform dates | Modrinth project "published" 2025-07-23; approved 2026-04-14; queued 2026-04-02; updated 2026-10-03. GitHub repo created 2026-04-15. Why the Modrinth project predates the public repo is UNVERIFIED. | https://api.modrinth.com/v2/project/sable ; GitHub API |
| Relationship to Create Simulated | Create Aeronautics / Simulated README says "powered by sable" and links to https://github.com/ryanhcode/sable. Create Aeronautics has a formal Modrinth dependency on Sable in versions 1.3.0 to 1.3.2. | https://github.com/Creators-of-Aeronautics/Simulated-Project (README); Modrinth dependency check |
| Port to newer MC | None found on Modrinth or GitHub. A GitHub repo Volodyamo4T/Sable-Fly (created 2026-08-26) describes itself as an "unofficial port of the Sable mod for newer Minecraft versions", but the repo page reads "This repository is empty." It cannot be used as evidence of a port. | https://github.com/Volodyamo4T/Sable-Fly (WebFetch 2026-10-11) |

### Mods that depend on Sable (Modrinth dependency metadata, checked versions)

Each name below has a Modrinth dependency on project id T9PomCSv (Sable) in at least one version, all for 1.21.1. Source for the whole list: https://api.modrinth.com/v2/project/{slug}/version (fetched 2026-10-11).

- Create Aeronautics (create-aeronautics): 1.3.0, 1.3.1, 1.3.2
- Create Propulsion: Simulated (create-propulsion-simulated): 1.0.0, 1.1.4
- Create: Lift n' Load (create-lift-n-load): 1.0a to 1.0c
- Create: Coasters Simulated (create-coasters-simulated): 0.1.3 to 0.1.5
- Sable Beyond (sable_beyond): Sable addon; MIT; 0.5.0
- CC: Sable (cc-sable), ssrd (Separate Sable Render Distance), sable-schematic-tool, sable-ragdolls, sable-ragdoll-corpse, drive-by-wire-sable, drive-by-sable, jade-sable-compat, waystones-sable, sable-explosions-fix, create-aeronautics-(sable)-mass-view, create-stuff-n-additions-x-sable-aeronautics-compat, sable-(create-aeronautics)-collision-damage, sable-cleanup, presence-footsteps-x-sable, touys, gravestone-and-sable-compatibility-patch, sable-far-and-wide, dimensional-sable, create-additions-sable-compat, sable-reforgedplay-compat, sable-true-impact, ship-trails-for-sable, sableedit, sable-pathfinder, ars-sable, sable-destructive, sable-cool-rain, sable-dynamic-lights, sable-new-sodium-compat, ftbchunksaerospace, sable-fix-mod, create-aeronautics-toolgun (0.0.1-beta), aero-science-program (1.21.1-1.0), create-sable-dynamic-lights
- No formal Sable dependency found in newest checked versions: create-simulated-additions, create-simulated-thrusters, create-droneworks-simulated, create-aeronautics-encased-fluid-pipes, create-aeronautics-transmission-linkage, vs-hose-connectors, sablecompat (Sable: Physics Compat). These may still integrate without a dependency entry (UNVERIFIED).

---

## 2. Landscape table

Columns: project (Modrinth slug), author, purpose, license (platform field, then source file if different), latest MC and loader (newest release, date), status. Sources are the Modrinth page (M) and the source repo (G) for each row. "Stale" means no release in the last 12 months as of 2026-10-11.

### 2a. Physics, ship, rocket and space mods

| Project (slug) | Author | Purpose | License | Latest MC + loader (date) | Status | Sources |
|---|---|---|---|---|---|---|
| Sable (sable) | ryanhcode | Sub-level physics library (Rapier) | PolyForm Shield 1.0.0 (source-available, non-OSI) | 1.21.1 NeoForge + Fabric, 2.0.6 (2026-10-03) | Active; repo pushed 2026-10-05 | M https://modrinth.com/mod/sable ; G https://github.com/ryanhcode/sable |
| Valkyrien Skies 2 (valkyrien-skies) | rubydesic (owner), StewStrong | "Physics in Minecraft": VS2 physics library for ships and airships | LGPL-3.0-only (Modrinth and GitHub API) | 1.20.1 Forge + Fabric, 2.4.11 (2026-04-10) | Active on GitHub (pushed 2026-10-10); no official 26.x build | M https://modrinth.com/mod/valkyrien-skies ; G https://github.com/ValkyrienSkies/Valkyrien-Skies-2 (default 1.20.1/main, 405 stars) |
| VS2 unofficial port to newer MC (valkyrien-skies-unnof-port) | LumixTeam (owner) | "Unofficial NeoForge/Fabric port of the Valkyrien Skies 2 physics engine to the newer Minecraft versions" | LGPL-3.0-only | NeoForge 26.2, 2.5.0-26.2 (2026-08-24); loaders listed fabric and neoforge | Unofficial; no 26.3 build listed | M https://modrinth.com/mod/valkyrien-skies-unnof-port |
| Eureka! Ships (eureka) | rubydesic, millennIumAMbiguity, StewStrong | VS2 add-on: build ships from ordinary blocks, place Ship Helm, assemble. Repo calls it "Basically Archimedes Ships, but with VS2 as the backend" | Apache-2.0 | 1.20.1 Forge + Fabric, 1.6.3 (2026-04-24) | Last release 2026-04-24; not on 1.21 officially | M https://modrinth.com/mod/eureka ; G https://github.com/ValkyrienSkies/Eureka (pushed 2026-04-24) |
| Eureka! unofficial port (eureka!-(unofficial-port)) | LumixTeam (owner) | Eureka ported to newer MC | Apache-2.0 | NeoForge 26.2, 1.6.3 beta (2026-08-24); Fabric 1.21.11, 1.5.3 (2026-08-24) | Unofficial | M https://modrinth.com/mod/eureka!-(unofficial-port) |
| Clockwork (create-clockwork) | rubydesic | VS2 add-on: Create contraptions (stress units) power ships; "mechanical contraptions meets physics" | Apache-2.0 (Modrinth; LICENSE file in repo is Apache 2.0) | 1.20.1 Fabric, 0.5.6 (2026-04-30); Modrinth lists fabric and forge, MC 1.18.2 to 1.20.1 | Active-ish; "Open Beta" per search snippet (UNVERIFIED) | M https://modrinth.com/mod/create-clockwork ; G https://github.com/ValkyrienSkies/Clockwork |
| Clockwork Additions (clockwork-additions) | GuyApooye | QoL for Clockwork/VS2: pedal, handlebar, constant velocity joint, flap bearing (per search snippet) | LicenseRef-All-Rights-Reserved | Fabric + Forge, last 2025-09-11; MC 1.18.2 to 1.20.1 | Stale | M https://modrinth.com/mod/clockwork-additions |
| Trackwork (trackwork) | Endalion | Wheels and tracks that drive around the world; VS2 + Create | MIT | 1.20.1 Forge, 1.20.1-1.2.3 (2026-01-07) | Stale-ish (last 2026-01) | M https://modrinth.com/mod/trackwork ; G https://github.com/Endalion/trackwork |
| Trackwork Refabricated (trackwork-refabricated) | zvikasdongre | Fabric port: "addon for VS2 and Create" | Modrinth MIT; GitHub SPDX NOASSERTION (conflict, UNVERIFIED which is right) | 1.20.1 Fabric, 1.2.2 (2026-01-01) | Stale-ish | M https://modrinth.com/mod/trackwork-refabricated ; G https://github.com/zvikasdongre/trackwork-fabric |
| Valkyrien Skies: Tournament (vs-tournament) | alex-s168 | VS2 add-on (Modrinth title) | Apache-2.0 | 1.20.4 to 1.20.6 Fabric + Forge (2024-05-22) | Stale | M https://modrinth.com/mod/vs-tournament |
| Carry On + VS2 compat (carryon-vs-compat) | copygirl | Compatibility patch | MIT | 1.20.1 Fabric + Forge (2026-04-19) | Active | M https://modrinth.com/mod/carryon-vs-compat |
| VS Wakes compat (vs-wakes-compat) | G_Mungus | Compatibility patch | CC0-1.0 | 1.20.1 (2024-10-20) | Stale | M https://modrinth.com/mod/vs-wakes-compat |
| VsBase (vsbase) | (not fetched) | Base for VS2 modpacks | MIT | 1.20.1 Fabric (2024-05-19) | Archived on Modrinth | M https://modrinth.com/mod/vsbase |
| Voxel Space Program | NythicalNorm | VS2 add-on for Forge 1.20.1: rockets with thrust-to-weight, delta-V, staging, time warp, maneuver nodes, solar-system map, planned multiplayer; "realistic orbital mechanics in a truly big solar system" | Apache-2.0 (GitHub sidebar via WebFetch; LICENSE file not read) | Forge 1.20.1 (README); no release data (UNVERIFIED) | "Beginning stages of work" (README); Modrinth not checked | G https://github.com/NythicalNorm/VoxelSpaceProgram (126 commits; WebFetch 2026-10-11) |
| Create (create) | simibubi (owner), IThundxr | Base contraption and building tech; everything in the Simulated/Sable stack sits on it | Code MIT, assets All Rights Reserved (LICENSE.md in repo); Modrinth LicenseRef-Create-Mod-License | 1.21.1 NeoForge, 6.0.10+mc1.21.1 (2026-04-21); no 26.x in Modrinth or GitHub branches (23 branches, newest mc1.21.1/*) | Active (pushed 2026-10-07; default branch mc1.21.1/dev) | M https://modrinth.com/mod/create ; G https://github.com/Creators-of-Create/Create ; LICENSE https://github.com/Creators-of-Create/Create/blob/HEAD/LICENSE.md |
| Create Aeronautics (create-aeronautics) | ryanhcode; repo org Creators-of-Aeronautics | Physics-contraption suite on Sable: planes, cars, airships; Simulated is the core | Code MIT, assets All Rights Reserved (LICENSE.md); Modrinth LicenseRef-Simulated-Project-License | 1.21.1 NeoForge, 1.3.2+mc1.21.1 (2026-08-29) | Active (repo pushed 2026-10-02; 918 stars) | M https://modrinth.com/mod/create-aeronautics ; G https://github.com/Creators-of-Aeronautics/Simulated-Project ; LICENSE https://github.com/Creators-of-Aeronautics/Simulated-Project/blob/main/LICENSE.md |
| Create Propulsion: Simulated (create-propulsion-simulated) | KyivSec (Modrinth); Propulsion-Team (repo) | Port of Create: Propulsion to NeoForge 1.21.1 with Sable and Aeronautics support (thrusters, torque) | MIT | 1.21.1 NeoForge, 1.1.5 (2026-07-23) | Active (repo pushed 2026-10-04) | M https://modrinth.com/mod/create-propulsion-simulated ; G https://github.com/Propulsion-Team/create-propulsion-simulated |
| Create: Lift n' Load (create-lift-n-load) | LopyLuna | Aeronautics/Simulated add-on "inspired by Scrap Mechanic" | Code MIT, art ARR (LicenseRef-MIT-Code-AND-ARR-Art; repo LICENSE) | 1.21.1 NeoForge, 1.0c beta (2026-06-24) | Beta | M https://modrinth.com/mod/create-lift-n-load ; G https://github.com/LopyLuna/Lift-n-Load |
| Create Big Cannons (create-big-cannons) | realRobotix (owner) | Large cannons built with Create | Code MIT; handmade textures and models CC BY-NC-SA 4.0 (LICENSE file); Modrinth LicenseRef-Create-Big-Cannons-License | 1.21.1 NeoForge, 5.11.7 (2026-06-22); 1.20.1 Fabric/Forge, 5.11.4 (2026-05-31) | Active (repo pushed 2026-08-23) | M https://modrinth.com/mod/create-big-cannons ; G https://github.com/Cannoneers-of-Create/CreateBigCannons |
| Archimedes' Ships | BalkondeurAlpha (per ModDex listing) | Forge sailing and airship builder with helm; legacy | UNVERIFIED (no license file located) | Listings say 1.7.10 era (2014); no 1.20+ build found; ModDex lists ArchimedesShips-1.7.1.jar (UNVERIFIED) | Legacy; status UNVERIFIED | https://moddex.gg/mod/archimedes-ships (search snippet, not opened) |
| Archimedes Mod (archimedes_mod) | INDOS_09 | NOT the ships mod: Forge 1.20.1 adventure content mod (Nether castles, pirate ships, treasure) | LicenseRef-All-Rights-Reserved | 1.20.1 Forge, 4.1.0 (2026-09-13) | Active; name collision only | M https://modrinth.com/mod/archimedes_mod |
| Immersive Portals (immersiveportals) | qouteall (owner), Nick1st | See-through portals with seamless teleport; "Dimension Stack" feature (vertically stacked dimensions linked by vertical portals) | Apache-2.0 (Modrinth and GitHub API) | Fabric 1.20.1, 5.2.0 (2025-11-21). NeoForge: immersive-portals-neoforge 6.0.7 on 1.21.1 (2025-06-18). Continued fork (immersive-portals-continued): Fabric 1.21.1, 6.0.7 (2025-02-10). | Upstream repo flagged archived=true on GitHub (pushed 2026-04-21); wiki still maintained | M https://modrinth.com/mod/immersiveportals ; G https://github.com/iPortalTeam/ImmersivePortalsMod ; wiki https://qouteall.fun/immptl/wiki/ ; dimension stack https://qouteall.fun/immptl/wiki/Dimension-Stack |
| Galacticraft-Legacy (galacticraft-legacy) | TeamGalacticraft | Classic space dimensions mod | MIT | 1.12.2 Forge, 4.0.6 (2023-12-09) | Legacy; no release since 2023 | M https://modrinth.com/mod/galacticraft-legacy ; G https://github.com/TeamGalacticraft/Galacticraft-Legacy (GitHub: fork=true; pushed 2025-11-16) |
| Galacticraft 5 (modern rewrite; repo TeamGalacticraft/Galacticraft) | TeamGalacticraft | "The classic Minecraft space mod, rewritten from the ground up for modern versions"; Fabric first, Forge later | MIT (GitHub SPDX) | No release. Pre-alpha builds after each commit to main. Latest MC: UNVERIFIED (unofficial wiki April 2025 snapshot said Fabric 1.21.1) | Pre-alpha; README: "There is currently no ETA for a release." | G https://github.com/TeamGalacticraft/Galacticraft (pushed 2026-10-10); README https://raw.githubusercontent.com/TeamGalacticraft/Galacticraft/HEAD/README.md ; wiki https://galacticraftunofficial.miraheze.org/wiki/Galacticraft_5 (search snippet) |
| Ad Astra (ad-astra) | Terrarium team (Facundo46, Kekie6, AlexNijjar, ThatGravyBoat) | Space/planets/rockets mod (purpose detail UNVERIFIED: Modrinth description is "Live long and prosper, Ad Astra!"; wiki not read) | Terrarium License v1: code (data, source, scripts, configs) MIT; non-code All Rights Reserved (LICENSE on 1.19 branch; current branch UNVERIFIED) | 1.21.1 Fabric and NeoForge, 1.16.26 (2026-09-13); 1.20.1 Forge, 1.15.21 (2026-09-07) | Active | M https://modrinth.com/mod/ad-astra ; G https://github.com/terrarium-earth/Ad-Astra ; license https://raw.githubusercontent.com/terrarium-earth/Ad-Astra/1.19/LICENSE |
| Ad Astra forks | none found | UNVERIFIED: web search budget exhausted. Modrinth search for ad astra surfaced addons only (Giselle Addon MIT 1.21.1; Planets+ MIT 1.20.1; Per Spatium 1.21.1), not forks. | UNVERIFIED (no fork located) | n/a | n/a | https://modrinth.com/mod/ad-astra-giselle-addon |
| Advanced Rocketry 3 (advanced-rocketry-3) | MarvinEckhardt | Rockets, space stations, warp travel (Modrinth description) | LicenseRef-All-Rights-Reserved (Modrinth); repo has no SPDX license (GitHub "Other") | NeoForge 1.21 and 1.21.1, 3.0.beta (2026-08-19) | Beta, active | M https://modrinth.com/mod/advanced-rocketry-3 ; G https://github.com/dercodeKoenig/marvins-minecraft-mods |
| Advanced Rocketry - Reworked (advanced-rocketry-reworked) | MarvinEckhardt | Legacy 1.12.2 rework | LicenseRef-All-Rights-Reserved | 1.12.2 Forge, 2025-08-20 | Legacy | M https://modrinth.com/mod/advanced-rocketry-reworked |
| Create: Northstar - Redux (northstar-redux) | Redstonneur1256 (owner); Astronauts-of-Create org | Space exploration add-on for Create | MIT (Modrinth; LICENSE at repo 1.20.1 branch dev/LICENSE.md) | NeoForge 1.21.1, 0.6.6+1.21.1 beta (2026-09-23); Forge 1.20.1, 0.6.6+1.20.1 beta | Beta, active | M https://modrinth.com/mod/northstar-redux ; G https://github.com/Astronauts-of-Create/Northstar-Redux |
| Create: Northstar (create-northstar) | lightning2401 | Original Create space add-on | MIT | 1.20.1 Forge/Fabric, 2024-08-09 | Stale | M https://modrinth.com/mod/create-northstar |
| Create: Northstar-Aeronautics Compatibility (create-northstar-aeronautics-compatibility) | Vladi-YT-Dev | Northstar + Aeronautics compatibility | MIT | NeoForge 1.21.1, 3.0.0 (2026-07-15) | Active | M https://modrinth.com/mod/create-northstar-aeronautics-compatibility |
| Stellaris (stellaris) | St0x0ef (owner); SanderA, TathanDev, Okamiz, Ilyuxadwa | "Discover the next gen space mod!"; rockets built from components, planets and moons (search snippet) | CONFLICT: Modrinth CC-BY-NC-SA-4.0; repo LICENSE (branch 26.1-stable) reads MIT, Copyright 2026 st0x0ef. Check before reuse. | 26.1.2 NeoForge and Fabric, 2.0.7 (2026-10-09). Only space mod in this table on a 26.x version. | Active (repo pushed 2026-10-11) | M https://modrinth.com/mod/stellaris ; G https://github.com/st0x0ef/stellaris-2.0 ; LICENSE https://raw.githubusercontent.com/st0x0ef/stellaris-2.0/26.1-stable/LICENSE |
| Cosmic Horizons (cosmic-horizons-cosmos) | GHO__ST | "A Space Mod Never seen before" (Modrinth); alpha | LicenseRef-All-Rights-Reserved | 1.20.1 Forge, 0.0.7.3 alpha (2025-01-05) | Stale; no source repo listed | M https://modrinth.com/mod/cosmic-horizons-cosmos ; wiki https://cosmic-mod.github.io/ (Modrinth link) |
| Beyond Earth (beyond-earth) | MrScautHD (owner); St0x0ef (team member) | "Fly to Other Planets and Beyond!" | LicenseRef-Beyond-Earth-License (text on pastebin.com; not opened) | 1.20.1 Forge, 7.0-PRERELEASE (2024-04-14) | Stale | M https://modrinth.com/mod/beyond-earth |
| Create Cosmonautics (create-cosmonatics) | CosmonauticsTeam | "Fulfill your childhood dream by exploring space!" (Modrinth). Search snippet: engines, orbital mechanics for celestial bodies, space dimension, Moon (UNVERIFIED detail). | GPL-3.0-or-later | 1.21.1 NeoForge, 26.08.307 (2026-08-09) | Active (version string is the Modrinth value) | M https://modrinth.com/mod/create-cosmonatics ; G https://github.com/CosmonauticsTeam/Create-Cosmonautics |
| Creating Space (creating-space) | RealAntEngineer | Rockets built from contraptions that reach other planets | MIT | 1.21.1 NeoForge, 1.7.18 (2026-05-03); 1.20.1 Forge, 1.7.13 (2026-03-31) | Active | M https://modrinth.com/mod/creating-space ; G https://github.com/RealAntEngineer/creating_space |
| Starflight Innovation (starflight-innovation) | nanorover59 | "Another space exploration mod with a novel approach. It's kind of rocket science..." Kerbal-inspired (search snippet, UNVERIFIED on repo) | MIT | Fabric 1.21 and 1.21.1, 1.3.1 (2024-12-04) | Stale | M https://modrinth.com/mod/starflight-innovation ; G https://github.com/nanorover59/Starflight-Innovation |
| Spaceward Expansion (spaceward-expansion) | syconn | Moon and planned rockets (per host page; UNVERIFIED) | LicenseRef-All-Rights-Reserved | Fabric and NeoForge, 2024-12-19 | Stale | M https://modrinth.com/mod/spaceward-expansion |
| Aero Science Program (aero-science-program) | Seraphaestus | Sable and Create add-on: research experiments inspired by Kerbal Space Program | MIT | NeoForge 1.21.1, 1.21.1-1.0 (2026-07-30) | Active | M https://modrinth.com/mod/aero-science-program ; G https://github.com/Seraphaestus/AeroScienceProgram |
| CASE: Create Aeronautics - Space Exploration (create-aeronautics-space-exploration) | BriderMC | Modrinth type "modpack" (not a mod) | MIT | NeoForge 1.21.1, 0.1 (2026-09-10) | Active | M https://modrinth.com/modpack/create-aeronautics-space-exploration ; G https://github.com/BriderMC/CASE |
| Distant Horizons (distanthorizons) | jeseibel (owner); TomTheFurry, Ran, coolGi, cola98765 | Level-of-detail far-terrain rendering (distant chunks) | LGPL-3.0-only | 26.3 Fabric and NeoForge, 3.3.4-26.3 (2026-09-29) | Active; 36.8M downloads | M https://modrinth.com/mod/distanthorizons ; source https://gitlab.com/distant-horizons-team/distant-horizons |
| Cubic Chunks (cubicchunks) | niko.sk2x (owner); OpenCubicChunks org | Unlimited height and depth by loading vertical chunk sections | MIT | 1.12.2 Forge only, 1.12.2-0.0.1301.0 (2026-10-01) | Active for 1.12.2 only | M https://modrinth.com/mod/cubicchunks ; G https://github.com/OpenCubicChunks/CubicChunks (default MC_1.12, pushed 2025-11-11) |
| CubicChunks3 (successor; OpenCubicChunks/CubicChunks3) | OpenCubicChunks | Rewrite targeting NeoForge and MC 1.21.6 | MIT (GitHub SPDX) | No playable release. README: "Not yet usable or functional, don't try." | Unfinished (pushed 2025-12-14) | G https://github.com/OpenCubicChunks/CubicChunks3 |
| CubicChunks2 | OpenCubicChunks | Rewrite for post-1.12.2 | UNVERIFIED (not opened) | Archived by owner 2024-10-04 (search snippet; UNVERIFIED) | Archived; superseded by CC3 | https://github.com/OpenCubicChunks/CubicChunks2 (search snippet) |
| Immersive Portals alternative: Immersive Portals (Neo)Forge | Nick1st (owner), qouteall | Portals on NeoForge | Apache-2.0 | 1.21.1 NeoForge, 6.0.7 (2025-06-18) | Stale (last 2025-06) | M https://modrinth.com/mod/immersive-portals-neoforge ; G https://github.com/iPortalTeam/ImmersivePortalsModForNeo |

### 2b. Physics backend comparison (facts only)

| Backend | Used by | Minecraft versions (newest) | Engine and implementation (source) | License | Status |
|---|---|---|---|---|---|
| Sable | Create Aeronautics, Create Propulsion, about 30 Sable addons (Section 1) | 1.21.1 only; 2.0.6 NeoForge and Fabric (2026-10-03) | Rapier physics (Rust) via native build (README) | PolyForm Shield 1.0.0 | Active; intrusive mixins; 1042 open issues on GitHub |
| Valkyrien Skies 2 | Eureka, Clockwork, Trackwork, VS Tournament, Voxel Space Program, Carry On compat | 1.20.1 official (2.4.11, 2026-04-10); unofficial port 26.2 (2.5.0-26.2, 2026-08-24) | Jolt added as a selectable engine in 2.4.10: "Jolt physics now available as an engine option" (Modrinth changelog, 2026-02-02). Source contains a KrunchSupport.kt object with isKrunchSupported = false; what "Krunch" is remains UNVERIFIED. | LGPL-3.0-only | Official active; unofficial 26.2 port by LumixTeam |
| Sable-Fly (GitHub) | none known | Claims newer-MC Sable port; repo empty at fetch | n/a | No license detectable | Not usable as evidence |

Source for VS2 changelog: https://api.modrinth.com/v2/project/valkyrien-skies/version (changelog field of 1.20.1-forge-2.4.10). Source for VS2 source tree: https://api.github.com/repos/ValkyrienSkies/Valkyrien-Skies-2/git/trees/1.20.1/main?recursive=1.

---

## 3. Round planets, spherical worlds and wrap-around

No mod was found that gives true spherical planets with gravity toward the core, tiny planets, or curvature gameplay on a current Minecraft version. Searches for "round planet", "spherical world", "tiny planet", "curved world", "world wrap" and "planet datapack" returned only the items below.

| Project (slug) | Author | Mechanism (how it works) | License | Latest MC + loader (date) | Status | Sources |
|---|---|---|---|---|---|---|
| TFC Eratosthenes (tfc-eratosthenes) | trygve55 | Addon to TerraFirmaCraft: worldgen shaped to the Craster Parabolic map projection; crossing the 180 degree meridian wraps; boats sail around the world; prevents volcano hotspots outside the "world". Projection-based, not a 3D sphere. | GPL-3.0-or-later (Modrinth); repo SPDX NOASSERTION (UNVERIFIED at file level) | 1.20.1 Forge, 3.0.0 beta (2026-08-14); loaders forge and neoforge | Active (pushed 2026-08-14) | M https://modrinth.com/mod/tfc-eratosthenes ; G https://github.com/trygve55/tfc_eratosthenes ; README https://github.com/trygve55/tfc_eratosthenes |
| Circumnavigate (circumnavigate) | FamroFexl | "Finite, Tiled, Seamless World Wrapping": seamless wrap at world edges | AGPL-3.0-only | Fabric 1.21.1, 0.7 (2026-05-21) | Active | M https://modrinth.com/mod/circumnavigate ; G https://github.com/FamroFexl/Circumnavigate |
| Wrap Around World (wrap-around-world) | germlin | Datapack: seamlessly wraps players to the opposite side of a configurable boundary | MIT | Datapack, 1.21 to 1.21.11, 1.0 (2026-06-23) | Active | M https://modrinth.com/mod/wrap-around-world |
| Looping World (looping-world) | neuromuser | Turns the world border into wrap-around teleportation instead of a wall | MIT | Fabric 1.19.2 only, 1.0.1+1.19.2 (2026-01-24) | Stale; not on 1.21 | M https://modrinth.com/mod/looping-world ; G https://github.com/neuromuser/looping-world |
| Round World (round-world) | (Modrinth author not fetched) | Iris shader: slight visual curve and darkening; visual only | MIT | Iris, 1.1 (2025-01-24); MC 1.21 to 1.21.3 listed | Stale | M https://modrinth.com/shader/round-world |
| World Curvature Shaders (Thundking) | Thundking | Visual horizon curve shader; visual only (web listings only; not on Modrinth under this slug) | UNVERIFIED | Listed builds include 1.21.x (search snippet only) | UNVERIFIED | https://www.9minecraft.net/world-curvature-shaders/ (search snippet) |
| GalactiCircle (galacticircle) | mMONTAGEe | Resource pack: changes square Galacticraft planets to circular textures | Restricted: "All-Rights-Reserved, redistribution outside modpacks is forbidden unless permission granted" | 1.7.10 and 1.12.2, 3.0 (2024-06-26) | Stale | M https://modrinth.com/resourcepack/galacticircle ; source https://codeberg.org/mMONTAGEe_Workshop/GalactiCircle |
| Astrocraft Round Planets (astrocraft-round-planets) | chotikoff | Resource pack for the Astrocraft Fabric mod; mechanism UNVERIFIED | LicenseRef-All-Rights-Reserved | Astrocraft pack, 1.20.1 to 1.21.4 (2025-03-26) | Stale | M https://modrinth.com/resourcepack/astrocraft-round-planets |

Not matches (excluded): Reachplus (spherical player reach, not planets; https://modrinth.com/mod/reachplus). Blocky Planet is a Unity demo, not a Minecraft mod (search snippet: https://bowerbyte.com/posts/blocky-planet/).

---

## 4. Orbits, delta-v and Space-Engineers-style mods

No existing mod on a current version combines KSP-style orbital mechanics, delta-v planning and Space-Engineers-style survival ship building. The partial matches below.

### 4a. KSP-style orbits and delta-v

| Project (slug) | Author | What it does | License | Latest MC + loader (date) | Status | Sources |
|---|---|---|---|---|---|---|
| Voxel Space Program | NythicalNorm | VS2 add-on: thrust-to-weight, delta-V and staging for rockets; time warp; solar-system map; maneuver nodes; "at least a dozen planetary bodies"; planned multiplayer with global time-warp voting | Apache-2.0 (sidebar) | Forge 1.20.1 (README); no release data (UNVERIFIED) | Beginning stages (README) | G https://github.com/NythicalNorm/VoxelSpaceProgram |
| Starflight Innovation (starflight-innovation) | nanorover59 | Rocket-science space mod; propellant feed, thrust vs efficiency, delta-v planning (CurseForge write-up via search, UNVERIFIED on repo) | MIT | Fabric 1.21 and 1.21.1, 1.3.1 (2024-12-04) | Stale | M https://modrinth.com/mod/starflight-innovation ; G https://github.com/nanorover59/Starflight-Innovation |
| Create Cosmonautics (create-cosmonatics) | CosmonauticsTeam | Space exploration with orbital mechanics for celestial bodies and a space dimension (search snippet; delta-v not confirmed) | GPL-3.0-or-later | NeoForge 1.21.1, 26.08.307 (2026-08-09) | Active | M https://modrinth.com/mod/create-cosmonatics ; G https://github.com/CosmonauticsTeam/Create-Cosmonautics |
| Creating Space (creating-space) | RealAntEngineer | Rockets from contraptions to other planets; no orbit or delta-v confirmed | MIT | NeoForge 1.21.1, 1.7.18 (2026-05-03) | Active | M https://modrinth.com/mod/creating-space ; G https://github.com/RealAntEngineer/creating_space |
| Aero Science Program | Seraphaestus | Sable and Create research experiments inspired by KSP (not orbits) | MIT | NeoForge 1.21.1, 1.21.1-1.0 (2026-07-30) | Active | M https://modrinth.com/mod/aero-science-program |
| Caelum | Nuparu00 | Keplerian orbital mechanics of the Sun, Earth, Moon and planets, for the sky display only; no gameplay | MIT | NeoForge 1.21.1, 3.0.0.6 (2026-09-22) | Active | M https://modrinth.com/mod/caelum ; G https://github.com/Nuparu00/Caelum |
| Astrophysics: Addon for Astrocraft | PixelFlux | Simulation addon for Astrocraft: general relativity, orbital mechanics, atmospheric physics (sky/simulation) | MIT | Fabric and Quilt 1.21.4, 1.0 (2026-09-18) | Active; not gameplay-oriented per description | M https://modrinth.com/mod/astrophysics |
| Astrocraft | (not fetched) | Realistic night sky: real stars, planets, moons, eclipses (sky only) | MIT | Fabric and Quilt 1.20.1 to 1.20.3, 1.5.1 (2025-05-10) | Stale | M https://modrinth.com/mod/astrocraft |

Excluded name collisions: Orbit (https://modrinth.com/mod/orbit-key, a keybind macro wheel), Orbital+ (https://modrinth.com/mod/orbital+; orbital strike cannons, 1.21.11 Fabric), Orbital Railgun, Orbital Strike Cannon.

### 4b. Space-Engineers-style survival ship building (physics, blocks, survival)

| Project | What it does | License | Latest MC + loader | Status | Sources |
|---|---|---|---|---|---|
| Create: Aeronautics (with Sable) | Physics-based vehicles built from blocks; Sable provides physics; no survival items in Sable itself | Code MIT, assets ARR | 1.21.1 NeoForge, 1.3.2 (2026-08-29) | Active | Section 2a |
| Create: Lift n' Load | Aeronautics/Simulated add-on inspired by Scrap Mechanic (a building-and-physics game) | Code MIT, art ARR | 1.21.1 NeoForge, 1.0c beta (2026-06-24) | Beta | https://modrinth.com/mod/create-lift-n-load |
| Eureka! Ships (with VS2) | Ships from ordinary blocks; Ship Helm and assembly; simplified stabilizing physics | Apache-2.0 | 1.20.1 Forge and Fabric, 1.6.3 (2026-04-24) | Stale-ish | Section 2a |
| Survival ship schematics on CreateMod.com | Schematics for Create: Aeronautics survival ships (e.g. 534-block "Easy Survival Ship") | not applicable | n/a | n/a | https://createmod.com/schematics/easy-survival-ship (search snippet) |

Notes: No Minecraft mod found that reproduces Space Engineers' grid-based survival construction with thrusters, power, life support and planets together. Modrinth search for "space engineers ship" returned 0 results. The SE-style material on the web refers to the original game.

---

## 5. Name check

Checked 2026-10-11. Modrinth search API (exact query, 5 results): 0 hits for every name. CurseForge direct search returned HTTP 403 (not checked). Web search: see column.

| Name | Modrinth (API search) | CurseForge | Web search (WebSearch) | Verdict |
|---|---|---|---|---|
| Apoapsis | 0 hits | UNVERIFIED (direct search blocked) | No match. Closest unrelated: Apotheosis (Forge/NeoForge mod by Shadows_of_Fire, https://www.curseforge.com/minecraft/mc-mods/apotheosis); "Apsis SMP" server modpack (https://www.curseforge.com/minecraft/modpacks/apsis-smp-modpack) | No exact Modrinth match; no exact web match; CurseForge UNVERIFIED |
| Starwright | 0 hits | UNVERIFIED | Combined search (OR query) found nothing. Separate query not run (budget exhausted). | No Modrinth match; other sources UNVERIFIED |
| Periapsis | 0 hits | EXACT NAME TAKEN as a modpack: "Periapsis", Forge space-exploration modpack about 80 mods, Season 2 v1.2 (https://www.curseforge.com/minecraft/modpacks/periapsis; https://www.modpackindex.com/modpack/97225/periapsis; https://moddex.gg/modpack/periapsis) | Same | Exact name exists on CurseForge as a modpack, not a mod. Collision risk for naming. |
| Voidwright | 0 hits | UNVERIFIED | No exact match. Close: Voidweaver (Fabric/Quilt, dimension creation, https://modrinth.com/mod/voidweaver); Void Vanguards (sci-fi, 1.21.1, https://www.mado-hosting.com/minecraft-mods/void-vanguards); Voidscape (https://moddex.gg/mod/voidscape) | No exact match on Modrinth or in web search; CurseForge UNVERIFIED |
| Escape Velocity | 0 hits | UNVERIFIED | Combined search found nothing. Separate query not run (budget exhausted). | No Modrinth match; other sources UNVERIFIED |

Other "Sable" and orbit-related titles seen in searches (not names on the list): Sable Beyond, Sable: Physics Compat, Sable Pathfinder, Sable Photomancy (all addons of Sable; see Section 1).

---

## Items still UNVERIFIED (summary)

- Krunch (VS2): identity and role beyond the KrunchSupport.kt flag.
- Voxel Space Program: release data, last push date, LICENSE file (sidebar only).
- Sable-Fly: content (repo empty at fetch). Port status of Sable to 26.x: none found.
- Galacticraft 5: latest supported Minecraft version (no release).
- Archimedes' Ships: license, source, and whether it has any current build.
- Ad Astra: purpose detail from the wiki; forks; license of the current branch.
- CurseForge: all facts beyond search snippets; name check on CurseForge for Apoapsis, Starwright, Voidwright and Escape Velocity.
- Stellar Engineering (listed in web results): not confirmed on Modrinth.
- Space Exploration (classic standalone mod): not found on Modrinth; only modpacks.
- Create Cosmonautics: delta-v and orbit features (search snippet only).
- Trackwork Refabricated and Stellaris license conflicts (Modrinth vs repo).
- TFC Eratosthenes: repo LICENSE file (GitHub SPDX NOASSERTION).
