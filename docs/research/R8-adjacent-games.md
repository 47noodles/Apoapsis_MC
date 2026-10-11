# R8: Adjacent-game mechanics catalogue (Project Apoapsis)

Research node R8. Date of research: 2026-10-11. Purpose: raw material for the Opus designer. Facts about how the games work, not design suggestions.

## Method and limits

- Sources: Steam store pages (fetched directly), official patch notes and posts (Keen Software House, Boxelware, Hello Games, Factorio wiki, Satisfactory wiki.gg), and press and review snippets returned by search. Where a page is only known from a search snippet, the URL is the one the search returned.
- Many wikis returned 401, 402 or 403 (Stationeers wiki pages, Outer Wilds fandom, Starfield wiki, NMS wiki, Create wiki). Where content came from search snippets of those sites, the cited URL is the one search returned.
- Several 2026 status claims rest only on hosting-company blogs (supercraft.host, hosthavoc). These are marked UNVERIFIED.
- Steam "% positive" figures are aggregates, not praise or criticism text. They are cited as status and sentiment only.
- WebSearch budget for this run was exhausted before the praise and criticism pass finished. Gaps are marked "GAP" in the relevant sections, not filled.
- One correction to my own working notes: the Steam app 1069190 page returned for Spaceflight Simulator is a different game ("Flight Of Nova"). It is not used here. A search query also named "Stoked Rocket Studios" as developer, which no result supported. The developer named by sources is Stefo (Stefo Mai) Morojna.
- Legend: UNVERIFIED = seen only in a secondary, hosting-blog, or undated source. GAP = searched for but not found in this run.

---

## 1. Stationeers

**One-line:** Co-op space-station survival sim built around per-cell gas simulation, pipe networks, and in-game IC10 assembly chips.

**Release status (2026):** Early Access, released 12 Dec 2017 (Steam). Steam overall English reviews 88% of 5,967 (Very Positive); last 30 days 88% of 59. Source: https://store.steampowered.com/app/544550/Stationeers/
- Feb 2026 hotfix v0.2.6182.26959 fixed an IC10 slot on the Combustion Centrifuge and the logic pin order on the Combustion Deep Miner. Source: https://www.stationeers-wiki.com/V0.2.6182.26959 (search snippet).
- 2026 themed updates "Sanitation" (2 Jul 2026) and "Power Line" (12 Aug 2026, Cable Gun, Electrical Belt, Power Pylons). UNVERIFIED: only from a hosting blog. Source: https://supercraft.host/article/stationeers-roadmap-2026/

**Mechanics:**
1. **Per-cell atmosphere.** The world is split into atmosphere units. Each large-grid cell holds one unit and exchanges gas and heat with neighbours until contents equalise. Gas is tracked in moles. Common gases: Oxygen, Volatiles, Nitrogen, Carbon Dioxide, Pollutant. Water is the only simulated liquid. Source: https://stationeers-wiki.com/Atmosphere/en (search snippet).
2. **Pipe networks as single atmospheres.** Each pipe network runs on one atmosphere. Inserting a pump or valve splits it into two atmospheres that exchange gas only through that device. Source: https://stationeers-wiki.com/Atmosphere/en (search snippet).
3. **Temperature from heat energy per gas amount.** Temperature depends on heat energy relative to gas quantity, so a small volume heats very fast. Pressure is contents adjusted for temperature. Source: https://stationeers-wiki.com/Atmosphere/en (search snippet).
4. **Volume makes emptying slow.** Large low-pressure volumes (station interiors, large tanks, long pipe runs) hold little gas but take a long time to empty. Portable Gas Tank: max 10,135 kPa, 790 L, spawn pressure 8,106 kPa. Source: https://legacy.stationeers-wiki.com/Portable_Tank (search snippet).
5. **Failure states.** Canisters burst from pressure. Fuel autoignites at high pressure or temperature. Items holding gas heat in direct sunlight. Source: https://stationeers-wiki.com/Atmosphere/en (search snippet).
6. **Plant growth gated by atmosphere.** Pressure 25 to 200 kPa (fastest at 50 to 100 kPa). CO2 at least 0.1 mol. Plants die after 5 minutes below 0 C or above 50 C. Water 5 to 60 C. Wrong light builds "Illumination Stress". Plants also need a daily darkness amount. Source: https://stationeers-wiki.com/Farming (search snippet).
7. **IC10 chips use assembly-style code.** Chips run a `loop:` label with `yield` and `j loop`. `alias` names a device port. `define` sets constants. `l` reads one device property into a register. `s` writes to one device. `sb` writes to every device of a type hash. `slt` compares and stores 1 or 0. The page does not state register or line limits. Source: https://xgamingserver.com/blog/stationeers-ic10-solar-tracker-automation/
8. **Broadcast by device-type hash.** A solar tracker reads sensor angles and writes them to all panels of one type hash with `sb`. Panels added later are tracked without rewiring. Source: https://xgamingserver.com/blog/stationeers-ic10-solar-tracker-automation/
9. **Power automation with hysteresis.** A power manager reads battery `Ratio`, compares to 0.25 with `slt`, and writes the generator `On` state with `s`. The page recommends separate on and off thresholds (0.25 on, 0.90 off) and gives no code. Source: https://xgamingserver.com/blog/stationeers-ic10-solar-tracker-automation/

**Praise:** Aggregated player sentiment praises simulation depth (gas and liquid dynamics, engineering) and open-ended problem solving. Developer post calls atmospherics the most defining system. Source: https://vaporlens.app/app/544550/stationeers ; https://devtrackers.gg/stationeers/p/ab534efb-the-airflow-update (search snippets).

**Criticism:** Counter-intuitive controls and hard-to-navigate UI. Thin tutorials and reliance on wiki and video guides. Developer-acknowledged vacuum-pulling bottleneck (post about three years old). DLC pricing concerns. Source: https://vaporlens.app/app/544550/stationeers ; https://devtrackers.gg/stationeers/p/ab534efb-the-airflow-update (search snippets).

---

## 2. Empyrion: Galactic Survival

**One-line:** Eleon Game Studios space survival sandbox with voxel planets, warp travel between worlds, and piece-built ships and bases.

**Release status (2026):** Steam release 5 Aug 2020. Steam store page shows no Early Access label. Steam English reviews 78% of 17,943 (Mostly Positive); last 30 days 62% of 37. Source: https://store.steampowered.com/app/383120/Empyrion__Galactic_Survival/
- v1.13 (Aug 2025) reworked the galaxy and added story missions. Source: https://hosthavoc.com/blog/empyrion-update-v1-13 (hosting blog; secondary).
- v1.19 (4 Aug 2026) and two follow-up point patches: UNVERIFIED, hosting blog only (https://supercraft.host/article/empyrion-roadmap-2026/). The Steam news fetch returned no posts, so no primary confirmation.

**Mechanics:**
1. **Voxel terrain.** Ground can be dug, flattened, or tunnelled. Source: Steam page above.
2. **Per-planet environment.** Each planet has its own gravity, atmosphere, climate, flora, fauna and resources. Source: Steam page.
3. **Warp and flight.** Players warp and fly between planets, moons and star systems. Source: Steam page.
4. **Piece-built ships and stations.** Capital ships, fighters, settlements and stations are built piece by piece, or from Workshop blueprints. Source: Steam page.
5. **Survival loop.** Food, oxygen and ammo matter. Players hunt, forage, farm, craft, and unlock a tech tree. Source: Steam page.
6. **Connected blocks without logistics tubes.** Turrets, storage and constructors connect without tube or wire runs (per reviewer contrast with Space Engineers). Source: https://www.bonusstage.co.uk/archives/102089 (review snippet).
7. **Factions and modes.** PvE areas, PvP, factions and alliances, plus scenarios and mods via the Workshop. Source: Steam page.

**Praise:** Base construction and controllable ships are among the most-cited strengths. Entire solar system is yours to explore, with no sense of being rushed (Alpha 8.0 review). Source: https://www.bonusstage.co.uk/archives/102089 ; https://www.cyberpowerpc.com/blog/empyrion-galactic-survival-alpha-8-0-review-giant-planets-are-in-and-theyre-exciting/

**Criticism:** Mechanics seen as under-developed; one negative review calls it clunky and buggy. Save corruption and physics bugs reported in a third-party summary. Repetition across planets; wish for dungeons or other activities. Several reviewers say it left Early Access too early. Sources: https://vaporlens.app/app/383120/empyrion_galactic_survival ; https://steamcommunity.com/app/383120/discussions/0/595146653381450876 (search snippets; individual opinions).

---

## 3. Avorion

**One-line:** Boxelware space sandbox: block-built ships, a sector-based factory and trade economy, asteroid mining, and co-op or PvP.

**Release status (2026):** Released; patch branch active. Steam overall 89% of 8,009 (Very Positive); last 30 days 81% of 33. Source: https://store.steampowered.com/app/445220/Avorion/
- 2.5.11 live 15 Dec 2025. 2.5.12 beta 12 Feb 2026. 2.5.13 beta 11 Mar 2026, live 16 Mar 2026 (rendering-freeze workaround for AMD on Windows during Rift missions; networking lag fixes). Source: https://www.avorion.net/news/
- 22 Jul 2026: Boxelware announced its second game, Aquapark Tycoon, in Early Access. Post says "There will be more Avorion news soon," no date. Source: https://www.avorion.net/news/

**Mechanics:**
1. **Freely scalable blocks; ships break at hit points.** Ships are built from scalable blocks, and break apart where they are hit. Source: Steam page above.
2. **Crew roles on one ship.** A captain flies. Crew run weapons and hangars. Co-op partners can split steering and firing. Source: Steam page.
3. **Galaxy geometry.** An impassable ring of torn hyperspace blocks the core. Hyperspace rifts appear across the galaxy. Players start at the edge and move inward toward danger and reward. Source: Steam page.
4. **Asteroid mining and mines.** Asteroid fields hold resources. Players can claim gigantic asteroids and found asteroid mines. Source: Steam page.
5. **Founding draws NPC traders and territory.** Founding a mine or factory attracts NPC traders and increases influence (territory size) in that sector. UNVERIFIED for 2.5.x: Steam forum threads, 2018 to 2024. Source: https://steamcommunity.com/app/445220/discussions/0/1700542332330181182 (search snippet).
6. **Factory chains with buy orders.** Each factory makes only the goods its type allows, from inputs supplied by other factories. A factory short on inputs posts buy orders. Forum-sourced, version-dependent. Source: same thread.
7. **Pricing by margin, not price.** A player cannot set a price directly, only a margin against the galactic average. Source: https://steamcommunity.com/app/445220/discussions/0/4417551094437225455 (search snippet; forum).
8. **Cheap clusters.** Clusters of identical factories tend to sell cheaper; a "percentage of cost above average" indicator shows buy-low points. Source: https://steamcommunity.com/app/445220/discussions/0/3110277460816723075 (search snippet; forum).
9. **Off-screen sector model: CONFLICTING.** One forum reply says sectors with your assets stay active. Another says inactive sectors go dormant. A third says the game makes periodic payout calculations rather than full simulation. UNVERIFIED. Source: https://steamcommunity.com/app/445220/discussions/0/2946998508787059127 (search snippet).

**Praise:** Steam aggregate sentiment (see status). Text praise not captured. GAP.

**Criticism:** Forum critique of 2.0: production chains broken, orders need constant re-issuing, micromanagement. One player's opinion. Early station costs quoted at 10 to 50 million credits (2018 figure). Source: https://steamcommunity.com/app/445220/discussions/0/1700542332330181182 (search snippet).

---

## 4. Starbase

**One-line:** Frozenbyte's voxel and vertex space MMO in Early Access, built around bolt-level ship construction, destructible environments, and ship automation chips.

**Release status (2026):** Early Access alpha since 29 Jul 2021 (Steam). Developers' last update was over three years ago per store page. Developers expect more than two years of EA. English reviews 54% of 4,377 (Mixed). Source: https://store.steampowered.com/app/454120/Starbase/
- Store page lists a station-destruction feature for a September "Siege Mechanics" update. Date UNVERIFIED.

**Mechanics:**
1. **Bolt as the smallest building unit.** Ships, stations and devices are built, repaired and modified from single bolts. Source: Steam page.
2. **Hybrid voxel and vertex world.** Environments are fully destructible through voxel and fracture damage. Source: Steam page.
3. **Per-part damage models.** Structural and elemental durability matter. Source: Steam page.
4. **Ship scale.** Small ships can have thousands of parts. Source: Steam page.
5. **Programming chips for automation.** In-game chips automate ship functions. Source: Steam page.
6. **Structural snap-off.** Poorly designed superstructures with strong thrusters can snap ships in half. Source: Steam page.
7. **Bolting and joining rules.** GAP. The store page does not describe joining rules beyond "smallest unit."

**Praise:** GAP for text praise.

**Criticism:** Store page warns the game is a clear alpha with many missing features and bugs. A reseller page reports recent reviews as very negative, snapshot date unknown (weak source). Source: https://store.steampowered.com/app/454120/Starbase/ ; https://www.dekudeals.com/items/starbase (search snippet).

---

## 5. Dyson Sphere Program

**One-line:** Youthcat Studio's 3D factory sim: interplanetary logistics, Dyson Sphere construction, and Dark Fog enemy defence.

**Release status (2026):** Early Access since 20 Jan 2021 (Steam). No 1.0 date found. English reviews 96% of 24,720 (Overwhelmingly Positive). Source: https://store.steampowered.com/app/1366540/Dyson_Sphere_Program/
- 22 Jan 2026 build 0.10.34.28281: Holo Beacon (map marker for factories) and Memo module. Source: https://dsp-wiki.com/Patch_Notes:0.10.34.28281
- 6 Feb 2026 build 0.10.34.28455: Copy Settings Data button; experimental IP display on Holo Beacons and Traffic Monitors. Source: https://www.dsp-wiki.com/Patch_Notes:0.10.34.28455 (search result link)

**Mechanics:**
1. **Dyson Shells on multiple orbits.** Each shell is customisable; nodes connect into patterns. Source: Steam page.
2. **Interplanetary fleets.** Transport ship fleets link planets so factories draw from each world's resources. Source: Steam page.
3. **Dark Fog.** Enemy forces attack factories and the player's mecha. Defended by structures that disperse them. Source: Steam page.
4. **Random star systems.** Neutron stars, white dwarfs, red giants and black holes are present. Source: Steam page.
5. **Snap-to-grid conveyors and blueprints.** Conveyors snap to a grid. Factory designs save and redeploy as blueprints. Source: Steam page.
6. **Mecha fuel limit.** The mecha needs fuel to operate, which reviewer Lane cites as an arbitrary movement limit. Source: https://bit-tech.net/reviews/dyson-sphere-program-review/1/ (review).
7. **Holo Beacon and Traffic Monitor.** Map markers and traffic readouts added in 2026 builds. Source: dsp-wiki links above.

**Praise:** Launch-window reviews call it "impressively complete," compare it to Factorio, and say it goes further on space travel and exploration. Bit-tech floated it as a Game of the Year candidate. Source: https://bit-tech.net/reviews/dyson-sphere-program-review/1/ . Wikipedia summarises early-access reception as positive for gameplay loop and graphics. Source: https://en.wikipedia.org/wiki/Dyson_Sphere_Program

**Criticism:** Onboarding and unclear goals. Late-game repetition and grind. Arbitrary movement and building limits (bit-tech). Sources: bit-tech review above; https://vaporlens.app/app/1366540/dyson_sphere_program (aggregator summary).

---

## 6. Factorio: Space Age

**One-line:** Wube's expansion adding four planets, space platforms, a quality system, and spoilage on top of the base factory game.

**Release status (2026):** Released. Release date not captured in this run (GAP). Reception: Metacritic 90 and Space Age 92 per an aggregator; GameGeeker 9.1; GOG 4.1 of 5; Steam "Very Positive". All from secondary sites. Sources: https://vaporlens.app/app/645390/factorio_space_age ; https://gamegeeker.com/games/factorio/review?country=US

**Mechanics:**
1. **Quality tiers.** Normal 0, Uncommon 1, Rare 2, Epic 3, Legendary 5. Source: https://wiki.factorio.com/Quality
2. **Quality module chance.** A machine's upgrade chance is the sum of its modules' chances. Quality module 1 adds 1% (Normal) to 2.5% (Legendary). Module 3 adds 2.5% to 6.2%. A success moves output up one tier. Further steps succeed at a fixed 10% each. Source: https://wiki.factorio.com/Quality
3. **Exact-quality ingredients.** Recipes have a variant for each ingredient quality. Item ingredients must match the selected quality exactly. Fluids have no quality. Source: https://wiki.factorio.com/Quality
4. **Quality scales machines and equipment.** About +30% per tier, additive, on stats such as health, crafting speed and robot limit. Source: https://wiki.factorio.com/Quality
5. **Recycling returns 25%.** Barrel recipes and recipes that output the item they consume (Kovarex, fish breeding) do not accept quality modules. Source: https://wiki.factorio.com/Quality
6. **Space platforms.** A hub anchors a 10x10 starter foundation. The hub cannot be removed, and its destruction destroys the platform. Chests cannot be placed. Thrusters only on the southern edge. Source: https://wiki.factorio.com/Space_platform
7. **Platform travel by schedule.** Platforms use train-style schedules with wait conditions. If thrusters stop, the platform drifts toward the nearer destination. Source: https://wiki.factorio.com/Space_platform
8. **Asteroid chunks to fuel and ammo.** Collectors gather chunks, crushers make raw materials, and thruster fuel and turret ammo come from them. Large asteroids damage the platform unless turrets break them. Source: https://wiki.factorio.com/Space_platform
9. **Spoilage.** Items spoil in machines, on belts, and while held by inserters. Each item type has its own spoil timer (values not on wiki page). A spoiled product keeps the source Quality. Source: https://wiki.factorio.com/Spoilage
10. **Planet resource loops.** Vulcanus: lava to molten iron and copper. Fulgora: scrap and holmium, with lightning. Gleba: produce spoils before processing. Aquilo: heat pipes keep machines running. Source: https://direct.factorio.com/space-age/content

**Praise:** Quality system and interplanetary logistics praised as major additions; expansion extends the base game's range (Backloggd reviewer, secondary). Source: https://backloggd.com/u/noterday/likes/ (search snippet; reviewer text not captured in full).

**Criticism:** Gleba spoilage widely called frustrating (aggregator summary; forum posts). Interplanetary logistics described as barely functional. Platforms cramped; Vulcanus has little build room. Aquilo heat-pipe requirement called a gimmick. Sources: https://vaporlens.app/app/645390/factorio_space_age ; https://forums.factorio.com/viewtopic.php?p=629095 (search snippet).

---

## 7. Kitten Space Agency (RocketWerkz)

**One-line:** Free pre-alpha KSP-style spaceflight sim from RocketWerkz (led by Dean Hall), with rocket building and solar-system physics.

**Release status (2026):** Pre-alpha. Free download via Ahwoo (ksa.ahwoo.com). Public pre-alpha builds from Nov 2025. July 2026 first-look teaser. Ahwoo contributions over $200,000 (developer claim). Fan wiki FAQ says Ahwoo is the only official download and the game will not appear on Steam. Wikipedia lists it as Early Access (search snippet; conflicts). Sources: https://kittenspaceagency.wiki.gg/wiki/Frequently_Asked_Questions ; https://www.gamingonlinux.com/2025/12/kitten-space-agency-is-the-spiritual-successor-to-kerbal-space-program/ ; https://www.gamedeveloper.com/press-release/rocketwerkz-s-kitten-space-agency-first-game-to-win-the-nzx-most-innovative-award

**Mechanics:**
1. **BRUTAL in-house framework.** Developer says it is built for moddability, performance and long-term scalability (developer claim). Source: https://www.gamingonlinux.com/2025/12/kitten-space-agency-is-the-spiritual-successor-to-kerbal-space-program/
2. **Kepler plus Newtonian physics across a solar system.** Community feature list (UNVERIFIED against build). Source: search snippets from https://piefed.keyboardvagabond.com/post/269426 .
3. **Physics during time warp and across multiple vessels.** No "current vessel" or physics distance, per community feature list (UNVERIFIED). Same source.
4. **Patched-conic orbital work.** Dated forum thread says it was functioning early on (UNVERIFIED for current build). Source: https://forums.coin-return.org/threads/kerbals-kittens-and-more-spaceflight-simulators.1297 (search snippet).
5. **Engineering-focused mission planning and flight.** Developer frames it as learning, testing and exploring aerospace concepts (developer claim). Source: gamingonlinux article above.
6. **Linux via Proton.** A GamingOnLinux author ran the installer with Proton Experimental and it loaded (single test of a development build). Source: gamingonlinux article above.

**Praise:** Credited ex-SpaceX flight software engineer on team (Space.com interview snippet). Free download and optional donation model. Source: https://space.com/entertainment/space-games/kitten-space-agency-is-the-spiritual-successor-to-kerbal-space-program-and-they-have-an-ex-spacex-engineer-on-the-team-interview (search snippet).

**Criticism:** A commenter called it more tech demo than early access; reader reports of antialiasing artifacts (anecdotal). A low-quality pre-alpha guide warns of rough physics, staging and save stability. Source: gamingonlinux comments (anecdotal).

---

## 8. Juno: New Origins

**One-line:** Jundroo's 3D sandbox for building and flying rockets, aircraft and cars with automation scripting and a career mode.

**Release status (2026):** Steam release 26 Jan 2023; Early Access began 8 Nov 2018. English reviews 88% of 2,049 (Very Positive); last 30 days 84% of 25. Latest update not shown on Steam page. Source: https://store.steampowered.com/app/870200/Juno_New_Origins/

**Mechanics:**
1. **30+ parts with resizing.** Tanks, wings and cargo bays resize; engines customisable. Source: Steam page.
2. **Flight model updates with shape.** Drag and lift recalculate as the craft changes. Source: Steam page.
3. **Vizzy visual scripting.** Drag-and-drop automation of vehicles, telemetry logging, custom MFD touch screens. Source: Steam page.
4. **Career and tech tree.** Run an aerospace company. Hand-crafted and procedural contracts pay money. Milestones and landmarks earn tech points that unlock tree branches. Source: Steam page.
5. **Planet Studio.** Planets, stars, moons and systems from templates; advanced mode for terrain noise layers and biomes. Orbits simulated; time warp supported. Source: Steam page.
6. **Shared creations.** Developer says over 100,000 creations can be tested with one click (developer claim). Source: https://space.com/juno-new-origins-game-launch-rockets-build-planets (search snippet).

**Praise:** GAP (text reviews not captured). Steam aggregate only.

**Criticism:** GAP.

---

## 9. Spaceflight Simulator

**One-line:** Mobile-first rocket-building and orbital sim by Stefo Mai Morojna, with selectable scale (1:20, 1:10, 1:1 on PC).

**Release status (2026):** Steam Early Access date conflicts across sources: 25 Jan 2022 (press release) and 18 Feb 2022 (wiki). Steam page not confirmed in this run (GAP). Mobile builds continue: 1.6.00.22 listed 30 Apr 2026 on APKMirror. Sources: https://gamespress.com/Spaceflight-Simulator-Commences-Liftoff-on-Steam-Early-Access-Jan-25 ; https://spaceflight-simulator.fandom.com/wiki/Spaceflight_Simulator ; https://www.apkmirror.com/?p=13605773

**Mechanics:**
1. **Orbital transfers.** Hohmann and bi-elliptic transfers are possible. Source: wiki page above.
2. **Scale by difficulty.** Easy 1:20, Hard 1:10, Realistic 1:1 (PC only). Planets scale with the setting. Source: wiki page above.
3. **Open universe.** No invisible walls. Source: wiki page above.
4. **Parts.** Fuel tanks, payload systems and engines. Developer describes rocket physics as "fully accurate" (developer claim). Source: https://gamespress.com/Spaceflight-Simulator-Commences-Liftoff-on-Steam-Early-Access-Jan-25
5. **Modding on PC only.** Planets and parts. Source: wiki page above.
6. **Solar system DLC.** Jupiter and the Galilean moons added via DLC. Source: wiki page above.

**Praise:** GAP.

**Criticism:** GAP.

---

## 10. No Man's Sky

**One-line:** Hello Games' procedural exploration game with planetary biomes, base building, freighters, Frontiers settlements, and the COSMOS space update.

**Release status (2026):** COSMOS (Update 7.0) launched 9 Sep 2026, free to all existing players across listed platforms. Hello Games announced it the same day. A 7.01 hotfix followed the next day. Sources: https://www.nomanssky.com/2026/09/no-mans-sky-cosmos/ ; https://www.gematsu.com/2026/09/no-mans-sky-cosmos-update-now-available ; https://patched.gg/games/no-mans-sky/a-complete-overhaul-to-space-arrives-in-the-form-of-no-mans-sky-cosmos-70 (search result). Steam English reviews 82% of 186,775; last 30 days 91% of 5,095. Source: https://store.steampowered.com/app/275850/No_Mans_Sky/
- 2026 earlier updates reported (Remnant 6.2 Feb, Xeno Arena Apr, The Swarm 6.4 May). UNVERIFIED: search-snippet claims only.

**Mechanics:**
1. **Procedural planets with saved edits.** A planet is regenerated from a formula; player terrain edits are saved and layered on top. UNVERIFIED: player forum claim. Source: https://steamcommunity.com/app/275850/discussions/0/1753525517908587573 (search snippet).
2. **Base building.** Introduced Update 1 (2016). Near-unlimited building from NEXT (2018). Frontiers (2021) grid menu. Source: https://blog.playstation.com/archive/2016/11/27/no-mans-sky-update-adds-base-building-new-game-modes-and-freighters (2016 freighters and base building); https://www.pcgamer.com/no-mans-sky-frontiers-adds-mos-eisley-type-settlements-and-makes-you-the-mayor (search result).
3. **Freighters and frigate fleets.** Freighters introduced in 2016 update. NEXT allowed command of freighter armadas. Pre-release coverage: a freighter supports up to 50 frigates for missions. Source: https://blog.playstation.com/archive/2016/11/27/no-mans-sky-update-adds-base-building-new-game-modes-and-freighters ; search snippets.
4. **Procedural settlements.** Frontiers settlements are procedurally generated. Player acts as overseer: names the town, commissions buildings and festivals, defends against Sentinels. Settlements in trouble can be rescued. Source: https://www.pcgamer.com/no-mans-sky-frontiers-adds-mos-eisley-type-settlements-and-makes-you-the-mayor (search result).
5. **COSMOS star map on warp.** Warping into a system reveals space points of interest that unlock missions. Source: https://www.nomanssky.com/2026/09/no-mans-sky-cosmos/
6. **Salvage with meltdown risk.** Wrecks are stripped with the physics gravity gun, then chunks thrown into a ship tractor beam. Over-harvesting causes meltdown and explosion. Source: nomanssky.com COSMOS page.
7. **Stations, alliances and free-floating bases.** Players can buy a space station, build its interior and exterior, found alliances from it, and build networked stations. Free-floating modular bases can be built in space. Source: nomanssky.com COSMOS page.
8. **Bio-wrecks and outposts.** Overrun facilities with biological hazards must be cleared before reclaim. Alien traders at outposts sell maps and offer missions. Source: nomanssky.com COSMOS page.

**Praise:** NEXT's building overhaul (build anywhere on a planet, tighter piece focus). Free post-launch content called rare for the industry. Source: https://www.tweaktown.com/news/81429/no-mans-skys-free-updates-continue-putting-aaa-developers-to-shame/index.html (search snippet).

**Criticism:** Frontiers settlement payoff weak: after clearing debt, a player received low-value items and found no trading. Also cited: one settlement per player. Source: Steam discussion threads (search snippets: https://steamcommunity.com/app/275850/discussions/0/3044985412478132592 ; exact thread not pinned). Broader "filler" criticism of content additions. Source: https://www.boringcactus.com/2023/12/27/no-mans-sky-unless.html (search snippet; blog).

**GAP:** Day-to-day fleet management and freighter mechanics (NMS wiki returned 401).

---

## 11. Outer Wilds

**One-line:** Mobius Digital's exploration puzzle game: a 22-minute time loop ends in a solar supernova, and knowledge persists through the ship log.

**Release status (2026):** Released 18 Jun 2020 (Mobius Digital, Annapurna Interactive). Steam English reviews 95% of 53,298 (Overwhelmingly Positive); Metacritic 85. Source: https://store.steampowered.com/app/753640/Outer_Wilds/

**Mechanics:**
1. **22-minute supernova loop.** The sun goes supernova at the 22-minute mark, which resets the loop regardless of what the player is doing. Source: https://www.gamerevolution.com/?p=546073 (search snippet).
2. **Ship log persists knowledge.** Information gathered survives death and reset. Source: search snippets (Wikipedia, Twinfinite).
3. **Ship computer redesign after playtests.** Mobius reports the most significant response to playtest feedback was a redesign of the ship computer. Source: https://www.mobiusdigitalgames.com/news/archives/12-2017 (search snippet).
4. **Timed-window worlds.** The Interloper's ice melts near the sun, opening caves for a limited window. Source: search snippet (Wikipedia and reviews).
5. **Simulated gravity, not scripted tracks.** Every body interacts through simulated gravity. Source: https://wireframe.raspberrypi.com/articles/outer-wilds-review-death-and-translation-in-the-final-frontier (search snippet).
6. **Small bodies with real gravity hazards.** A jetpack can carry you off a small moon; moons tug on the craft and jumps can exploit this. Source: https://news.xbox.com/en-us/2019/05/30/outer-wilds-available-today-xbox-game-pass/ (launch tips, search snippet).
7. **Hourglass Twins.** Ash and Ember share a gravity field and sand; a badly timed jump can bury the player. Source: search snippet (review).
8. **Hidden locations change over time.** Example: an underground city swallowed by sand as the loop runs. Source: Steam page.
9. **Physics checked by a physicist.** Momentum conserved; slingshots possible. Source: https://www.thephysicsmill.com/2024/09/20/an-astrophysicist-attempts-to-measure-the-physics-of-outer-wilds/

**Praise:** Steam 95% sentiment; Game of the Year 2019 from several outlets; BAFTA 2020 Best Game (Steam page).

**Criticism:** Time-dependent puzzles get obtuse late in the game, especially when combined with the log's hint system. Source: https://twinfinite.net/reviews/outer-wilds-review/ (search snippet). Developer rationale for the log and gravity tuning not found (GAP).

---

## 12. Astroneer

**One-line:** System Era's deformable-planet sandbox: terrain tool, tethers for oxygen and power, modular bases, 2 to 4 player co-op.

**Release status (2026):** Released 16 Dec 2016 in Early Access; version 1.0 in 2019; ongoing free updates. Steam English reviews 91% of 54,046 (Very Positive); last 30 days 87% of 408. Source: https://store.steampowered.com/app/361420/ASTRONEER/

**Mechanics:**
1. **Terrain Tool.** Dig, add and flatten terrain; recolour; collect. Modes switch with the controller bumpers. Source: Steam page; https://www.windowscentral.com/how-to-play-astroneer (search result).
2. **Seven celestial bodies** explorable from surface to core. Source: Steam page.
3. **Tethers are craftable items.** Each costs 1 Compound and carries oxygen (and power if a source is attached) to the player. Source: https://astroneer.wiki.gg/wiki/Tether
4. **Tether line colours.** Blue: oxygen. Dashed blue and yellow: oxygen plus power. Black: no connected source. Source: https://twinfinite.net/guides/astroneer-tethers-how-make/ (guide, 8+ years old; UNVERIFIED for current patch).
5. **Tethers power only the player.** Vehicles and bases do not receive tether power; a Small Battery carried over does. Source: https://astroneer.wiki.gg/wiki/Tether
6. **Cable network separate from tethers.** Cables snap into Cable Plugs on platforms and vehicles to distribute power across a base. Source: https://astroneer.wiki.gg/wiki/Tether
7. **Tether Dropper.** Backpack-slot auto-placer that places a new tether each time the player reaches max range. Source: https://astroneer.wiki.gg/wiki/Tether
8. **Relocatable modular base pieces.** Pieces can be moved after building to tune power, logistics and automation. Source: search snippets (Notebookcheck / store copy).
9. **Terrain tool upgrades.** Upgrades are researched and crafted, then plugged in and out of the tool. Source: search snippet (UNVERIFIED on research tree details).

**Praise:** Relaxed rather than punishing; no hostile enemies that wipe progress. Automation more approachable than Satisfactory. Source: https://www.notebookcheck.net/Steam-open-world-survival-game-with-charming-graphics-and-120-000-positive-reviews-drops-to-lowest-ever-price.1107250.0.html (search snippet).

**Criticism:** Light on tutorials at time of writing; control schemes puzzling on Xbox One. Source: https://www.windowscentral.com/how-to-play-astroneer (search snippet; date not shown).

---

## 13. Starfield

**One-line:** Bethesda's space RPG (Creation Engine) with a modular ship builder, planet-specific parts, and outposts for resources and power.

**Release status (2026):** Steam store page is age-gated and was not read. Release date not captured (GAP). Bethesda blog (17 Jul 2026) says more content will launch in 2027. No Aug to Oct 2026 patch found. PS5 port reported for April 2026 (UNVERIFIED). Sources: https://www.gamewatcher.com/Starfield/news (aggregator) ; https://insider-gaming.com/starfield-2026-update-rumoured-to-bring-quicker-space-flights-ps5-switch-2-ports-and-more/ (rumour)

**Mechanics:**
1. **14 ship stats.** Cargo, mass and power among them. Mass is a speed penalty. Source: https://ggrecon.com/guides/starfield-ships-parts-modules-types (guide).
2. **Reactor output caps weapons and engines.** Number of guns and engines is limited by reactor power. Source: same guide.
3. **One-per-ship parts.** Reactors, grav drives and cockpits are single-slot. Source: same guide.
4. **Landing gear scales with mass.** Enough gear to carry the mass; no even count required. Source: same guide.
5. **Landing bay needs a habitat link.** Landing bays must connect to the cockpit through habs, or crew cannot leave the ship. Source: same guide.
6. **Outpost build order.** Beacon first, then power type suited to the planet, then extractors and facilities. Source: https://twinfinite.net/guides/all-outpost-modules-what-they-do-in-starfield/ (guide, search snippet)
7. **Extractors need resource veins.** Extractor types in the menu depend on veins under the beacon. Three tiers (Base, Commercial, Industrial) with different costs and output. Source: https://starfield.wiki.fextralife.com/Outpost+Units (search snippet; some cost tables TBD)
8. **Storage pulls from ship cargo.** Outpost storage can transfer goods from the ship's hold. Source: ggrecon guide.
9. **Outpost Engineering skill** unlocks larger module structures. Source: ggrecon guide.

**Praise:** Ship customisation described as satisfying, and planet-specific parts make redesign tempting. Source: https://steamcommunity.com/app/1716740/discussions/0/4290313152639691965 (search snippet; forum).

**Criticism:** Outposts described as dull, with no custom rooms and no lively residents. Outposts vanish in New Game Plus. Ship building not challenging to some. Source: same Steam thread and search snippets (forums, blog, small site). Launch-era and 2024 sources only.

---

## 14. Stormworks: Build and Rescue, and From the Depths

### 14a. Stormworks: Build and Rescue

**One-line:** Vehicle-building sim with an in-game logic system, buoyancy and hydrodynamics, and survival damage, including helicopters, boats and submersibles.

**Release status (2026):** Released 17 Sep 2020; Steam page shows no Early Access label. English reviews 90% of 41,973 (Very Positive); last 30 days 91% of 428. Source: https://store.steampowered.com/app/573090/Stormworks_Build_and_Rescue/

**Mechanics:**
1. **Lua microcontrollers.** Advanced logic on Survival difficulty. Source: Steam page.
2. **Hydrodynamics and displacement buoyancy.** Wind and ocean conditions affect boats and aircraft. Source: Steam page.
3. **Survival damage model.** Spreadable fire, fall and impact damage, hypothermia, rotor injuries, water pressure, sharks. Source: Steam page.
4. **Block-building editor.** Helicopters, boats and submersibles built from blocks and then programmed. Source: Steam page.
5. **Multi-crew vehicles.** Crew roles on complex missions. Online co-op and online PvP listed. Source: Steam page.
6. **Workshop sharing.** Vehicles and missions. Source: Steam page.

**Praise:** Customisation system deep and rewarding; dynamic storms and winds make the world feel real (Cubed3, 2018 Early Access preview). Source: https://cubed3.com/preview/740/1/stormworks-build-and-rescue-pc.html . Engineering over spectacle; fuel barrels and buoyancy blocks singled out (GameGeeker). Source: https://gamegeeker.com/de/games/stormworks-build-and-rescue/review (search snippet). SteamPulse: 90% positive across 57,957 reviews. Source: https://steampulse.org/game/573090 (search snippet).

**Criticism:** Complex builds get hard to get working; forgetting to wire helicopter steering can be dangerous (Cubed3). Visuals undercut realism; story weak (GameGeeker). Steam comments (2020 and 2022): physics cartoonish; no hard thrust or torque numbers; blocks merged into polygons outside the editor so damage creates holes. Sources: steamcommunity.com/app/573090/discussions/0/2950377478185492979 ; .../1694922526900678267 ; .../3427823723503699684 (search snippets; individual opinions).

### 14b. From the Depths

**One-line:** Voxel block-build game for vehicles, ships and aircraft, with per-block physics damage and AI built from in-game blocks.

**Release status (2026):** Early Access 7 Aug 2014; full release 6 Nov 2020 (Steam). English reviews 90% of 9,776 (Very Positive); last 30 days 86% of 44. Source: https://store.steampowered.com/app/268650/From_the_Depths/
- An undated SteamDB patch note says build 3.2.9 Beta moved to stable and that players must now use Breadboard to control constructs, with AI behaviour and manoeuvre cards removed. This conflicts with the store page, which describes AI cards. UNVERIFIED date. Source: https://steamdb.info/patchnotes/6468387

**Mechanics:**
1. **Over 1,000 components.** Battleships, planes, submarines, spaceships and hot-air balloons; customisable missiles, torpedoes, cannons and engines. Source: Steam page.
2. **Damage recalculates physics.** Destroyed or added blocks change function, physics and control. Drag, inertia tensors, buoyancy and sealed compartments update from the design and damage taken. Source: Steam page.
3. **AI from blocks.** An AI mainframe plus AI cards placed into motherboard blocks can add radar, laser tracking and local weapon control. Source: Steam page.
4. **Dynamic damage simulation.** Shell penetration and explosions, per a 2020-era wiki snapshot. UNVERIFIED for the current build. Source: https://fromthedepths.fandom.com/wiki/From_the_Depths?oldid=7484
5. **Campaign and modes.** Campaign across hundreds of islands against eight factions; Story and Adventure modes; creative mode. Source: Steam page.
6. **Co-op and adversarial multiplayer.** Co-op design, adversarial modes, and co-op through the full campaign. Source: Steam page.

**Praise:** GAP (text reviews not captured).

**Criticism:** GAP (text reviews not captured).

---

## 15. Hardspace: Shipbreaker

**One-line:** Blackbird Interactive salvage sim: grapple and tether ships apart, then sort parts into bins by scrap value.

**Release status (2026):** Left Early Access with version 1.0 on 24 May 2022 (press). Price moved from $24.99 in EA to $34.99 at 1.0 (search snippet). No 2026 update found. Sources: https://gamechronicles.com/hardspace-shipbreaker-exits-early-access-and-releases-on-pc-may-24 ; https://store.steampowered.com/app/1161580/Hardspace_Shipbreaker/

**Mechanics:**
1. **Grapple and tether handling.** Grapple shoves wreck pieces; in zero-g a light nudge moves them. Tethers hold items or chain several for processing. Source: https://www.godisageek.com/reviews/hardspace-shipbreaker-review/ (review).
2. **Three-bin sorting.** Green barge takes valuable components. Blue processor takes hard armour and structural reinforcement. Red furnace takes bulk material. Wrong bin means no credit. Source: godisageek review; https://gamingtrend.com/reviews/hardspace-shipbreaker-review-breaking-things-has-never-been-this-fun/ (review).
3. **Resource pressure.** Thruster fuel, oxygen, and condition of tools, suit and helmet are tracked. Source: godisageek review.
4. **Line-cut risk.** Cutting coolant lines causes trouble; cutting fuel lines is far worse. Source: godisageek review.
5. **Death costs a fee.** Employer restores clone for a fee. Source: godisageek review.
6. **Scoring by scrap value.** Five salvage targets per ship; progress bar by scrap value; progression rewards top out near 90%. Source: gamingtrend review.
7. **Two currencies.** Milestones earn MP for rank and Lynx Tokens for upgrades. Source: gamingtrend review (search snippet).

**Praise:** Destruction physics called good by reviewers. Source: godisageek review; https://www.playstationcountry.com/hardspace-shipbreaker-ps5-review/ (search snippet).

**Criticism:** Upgrades feel dull; only capability increases, grappler range most noticeable. Source: https://primagames.com/featured/hardspace-shipbreaker-review (search snippet). Early Access review: few ship types in variants; unclear debt-goal expansion. Source: https://www.goonhammer.com/hardspace-shipbreaker-early-access-review (search snippet). PS5 review: controls a handful at first until tethers arrive.

---

## 16. Satisfactory

**One-line:** Coffee Stain's first-person factory builder: belts, trains, power, and a Space Elevator whose five project phases gate tiers.

**Release status (2026):** 1.0 released 10 Sep 2024. English reviews 97% of 138,077 (Overwhelmingly Positive); Metacritic 91. Source: https://store.steampowered.com/app/526870/Satisfactory/
- OpenCritic top-critic average 90; all listed critics recommend. Source: https://opencritic.com/game/17273/-/reviews (search snippet).
- Patch naming conflicts across sources (1.2 described two ways). UNVERIFIED. Source: https://supercraft.host/article/satisfactory-roadmap-2026/ (hosting blog).
- "Core Values" paid expansion announced late Sep 2026 (underground biomes, planet MASSAGE-2(A-B)b). Single news source, no date or price. UNVERIFIED. Source: https://www.playnews.gg/en/dlcs/satisfactory-core-values-expansion-underground-biomes-new-story-buildings-no-date-yet

**Mechanics:**
1. **Space Elevator phases gate tiers.** Phase 1 (50 Smart Plating) unlocks Tiers 3 and 4. Phase 2 (1,000 Smart Plating, 1,000 Versatile Framework, 100 Automated Wiring) unlocks 5 and 6. Phase 3 unlocks 7 and 8. Phase 4 unlocks Tier 9. Phase 5 starts the ending. Source: https://satisfactory.wiki.gg/wiki/Space_Elevator
2. **Final gate is a sequence and a lever.** After Phase 5, a specific sequence of tasks on the elevator display, then a final lever launches Project Assembly. Source: https://satisfactory.wiki.gg/wiki/Space_Elevator
3. **Scalable delivery costs.** At new-game setup, phase costs scale from 0.25x to 100x and cannot be changed later. Source: https://satisfactory.wiki.gg/wiki/Space_Elevator
4. **Elevator unlock.** Unlocked by HUB Upgrade 6. Source: https://satisfactory.wiki.gg/wiki/Space_Elevator
5. **Transport options.** Belts, factory carts, hypertubes, jump pads; liquids via pipes; trucks and trains. Source: https://store.steampowered.com/app/526870/Satisfactory/
6. **Co-op with internet requirement.** Online and cross-platform co-op listed. Source: Steam page.

**Praise:** Freedom to design large distribution networks singled out. Source: critic review snippet on https://opencritic.com/game/17273/-/reviews ; https://www.godisageek.com/reviews/satisfactory-review/ (tutorials kept in 1.0; search snippet).

**Criticism:** Crashes, memory leaks, synchronisation problems, save corruption (aggregator summary). Large factories need strong hardware. Steam discussion argues 1.0 was declared too early. Sources: https://vaporlens.app/app/526870/satisfactory (aggregator); https://steamcommunity.com/app/526870/discussions/0/591765076067783655 (search snippet).

---

## 17. GregTech: New Horizons (modpack)

**One-line:** A Minecraft 1.7.10 questing modpack that progresses through GregTech's technology "ages" and places other mods' content along that ladder, with Galacticraft and Galaxy-Space for off-world progression.

**Release status (2026):** Actively updated; the README lists Galacticraft 3.4.34-GTNH, Galaxy-Space-GTNH 1.1.143-GTNH, amunra 0.8.14. Current 2026 pack version not verified (GAP). Source: https://github.com/GTNewHorizons/GT-New-Horizons-Modpack

**Mechanics:**
1. **Ages as progression.** Play starts in a stone age, moves to steam machines, then electricity. GregTech tiers are "basically ages of technology." Source: GTNH README.
2. **Other mods placed on the age ladder.** Content from other mods is slotted at matching points in the progression. Source: GTNH README.
3. **Off-world gating.** Later stages require visiting other planets and dimensions and defeating bosses. Source: GTNH README.
4. **Galacticraft becomes required once materials are off-world.** Source: GTNH README.
5. **Local veins vs off-world materials.** Base grows from local ore veins; advanced materials require off-world trips. Source: GTNH README and search snippets.
6. **Questing.** Described as a big progressive questing pack. Source: GTNH README.
7. **Not GTNH:** an Extra Planets tier table (Mercury T4 to Eris T10) was found. It is for the standalone mod on 1.12.2 and 1.11.2 and is NOT GTNH data. UNVERIFIED for GTNH. Do not use for pack design. Source: https://www.9minecraft.net/extra-planets-mod/wiki/ (search snippet).

**Praise:** Automation depth and earned progression between ages; tech tree with custom recipes; quest book praised (Moddex reviews, about 15 total, self-selected). Source: https://moddex.gg/modpack/gt-new-horizons/reviews/612 ; .../1960 ; search snippets.

**Criticism:** Slow early game; stability issues (version-dependent). Steep learning curve; "hardest pack" label depends on experience. Sources: Moddex review snippets; guide articles (low weight).

---

## 18. Create (base mod)

**One-line:** Kinetic-power Minecraft mod in which rotational power flows through shafts and cogwheels, with stress budgets, contraptions and trains.

**Release status (2026):** Create 6 on Minecraft 1.21.1 NeoForge is the target of the Stressbound add-on. Base mod's current release and 2026 status not confirmed (GAP). Source: https://www.curseforge.com/minecraft/mc-mods/create-stressbound

**Mechanics:**
1. **Capacity versus impact.** Generators have stress capacity. Consumers have stress impact. The network runs only when combined capacity is at least combined impact. Source: https://git.grimmauld.de/mirrors/Create/wiki/Stress-Units%2C-Capacity-and-Impact (Create wiki mirror; page last edited 2021).
2. **Overstress halts the whole network.** When demand exceeds supply, all components stop until impact is reduced or capacity added. Source: same page.
3. **RPM scales stress.** Each component's value is multiplied by its current RPM. Some generators produce more stress at higher RPM. Source: same page.
4. **Shafts and cogwheels are neutral.** They neither consume nor generate stress, but connected components share rotation, so two producers on one shaft can cause overstress. Source: same page.
5. **Contraptions.** Moving assemblies; example: drills that carve tunnels while crawling forward. UNVERIFIED (third-party guide snippet). Source: https://www.mc-mod.net/?p=514266 (search snippet).
6. **Trains.** Base mod includes tracks, stations and schedules. UNVERIFIED (third-party guide snippet). Same search.
7. **Stressbound add-on (not base Create).** Stress Transmitter and Stress Receiver linked by a Kinetic Binder replace long shaft runs. Receivers have SU budgets to prevent free stress duplication. Contraptions and trains get runtime anchor support. Source: https://www.modpackindex.com/mod/105842/create-stressbound (listing).

**Praise:** GAP.

**Criticism:** GAP.

---

## 19. Notable 2025 to 2026 space-building games

### 19a. SpaceCraft (Shiro Games)
**One-line:** Space logistics and factory sim: modular ships, automated bases, drone and cargo-ship delivery lines, and a player-run economy.

**Status:** Early Access launched 11 Jun 2026 (moved from 20 May). Developer expects at least six months of EA. English reviews 64% of 1,306 (Mixed). Sources: https://store.steampowered.com/app/3276050/SpaceCraft/ ; https://insider-gaming.com/spacecraft-early-access-release-date-revealed/

**Mechanics:**
1. **Modular ships with per-part effects.** Each component affects performance. Designs sell as blueprints. Source: Steam page.
2. **Automated resource hubs.** Outposts grow into housing, production, energy and logistics. Source: Steam page.
3. **Drone and cargo-ship delivery lines.** Automate mining, extraction and transport between planets. Source: Steam page.
4. **Player-driven market.** Marketplace transactions shift prices across the economy. Source: Steam page.
5. **Corporations.** Groups assign roles, divide tasks, and run from small crews to interplanetary enterprises. Source: Steam page.

**Praise / criticism:** GAP (text reviews not captured).

### 19b. StarRupture (Creepy Jar)
**One-line:** First-person open-world base builder: machines extract and process resources while the base is defended against alien hordes.

**Status:** Early Access since 6 Jan 2026; developer plans to leave EA in 2027 and reserves the right to raise price. English reviews 81% of 6,986 (Very Positive); last 30 days 68% of 179 (Mixed). Source: https://store.steampowered.com/app/1631270/StarRupture/

**Mechanics:**
1. **Machines mine, harvest, create and assemble.** Source: Steam page.
2. **Horde defence.** Alien monsters attack the base. Source: Steam page.
3. **Survival temperatures.** Extreme temperatures. Source: Steam page.
4. **Co-op up to 4 players.** Source: Steam page.
5. **Search, mine and process in an open world.** First-person open-world play; players search, mine and process resources to build an industrial system that expands the base. Source: https://store.steampowered.com/app/1631270/StarRupture/

**Praise / criticism:** GAP.

### 19c. Space Engineers 2 (Keen Software House)
**One-line:** Alpha successor to Space Engineers with ship and station building, planetary stations, automation tools, and a dated patch cycle.

**Status:** Early Access alpha (Steam listing, Keen patch notes). Latest named update "Smoother, Stable & Faster" with dated hotfixes on 20, 27 and 28 Aug 2026; a third-party blog dates the main post 17 Aug 2026. Sources: https://support.keenswh.com/spaceengineers2/pc/announcement/space-engineers-2-alpha-smoother-stable-faster ; https://store.steampowered.com/app/1133870/Space_Engineers_2/

**Mechanics:**
1. **Save system.** Up to 5 autosaves and 5 manual saves per world; autosaves stored separately; autosave on cockpit entry. Source: Keen patch page.
2. **Thruster override detection** and speed limiter (double-tap Shift disables; hold Shift exceeds). Source: Keen patch page.
3. **Automation tools.** Timer, Sensor, Event Controller. Source: https://blog.marekrosa.org/2026/03/space-engineers-2-vs2-2/ (repost; VS2.3 Keen notes not fetched) - UNVERIFIED primary.
4. **Planetary stations** at Verdure, Kemik, Palatine Orbit, Caligo Orbit. UNVERIFIED primary (same repost).
5. **Warheads and mechanical blocks** (wheels, hinges). UNVERIFIED primary (same repost).
6. **Wind physics, O2H2 tanks, functional reactors.** UNVERIFIED primary (same repost).
7. **Roadmap statements.** Water arrives in VS3 with new planet Byblos; NPCs and 4-player co-op in VS4; full multiplayer in VS5. UNVERIFIED (repost of VS2.2 announcement).

**Praise:** Game8 praises new grid system where blocks fit together and per-block paint. Source: https://game8.co/articles/reviews/space-engineers-2-review-early-access (earlier build).

**Criticism:** Steam review says engineering has little risk or reward, backpack crafting replaces smelting. Source: https://steamcommunity.com/app/1133870/reviews/?browsefilter=toprated (search snippet; date conflicts).

### 19d. Space Haven (not verified)
UNVERIFIED: a search snippet says Space Haven left Early Access in 2026. Not confirmed; not catalogued. Source: https://en.wikipedia.org/wiki/Space_Haven

---

## Cross-cutting table

| Mechanic category | Games that do it best | One-line description |
|---|---|---|
| Logistics | Satisfactory; DSP; SpaceCraft | Satisfactory: belts, trains, hypertubes; DSP: interplanetary transport fleets; SpaceCraft: drone and cargo delivery lines. |
| Atmosphere / life support | Stationeers; Astroneer | Stationeers: per-cell gas simulation with pipe networks split by pumps and valves; Astroneer: tethers carry oxygen to the player. |
| Power | Stationeers; Starfield | Stationeers: IC10-driven power automation; Starfield: reactor output caps guns and engines. |
| Automation / logic | Stationeers; Stormworks; Juno | Stationeers: assembly-style IC10 with device-hash broadcast; Stormworks: Lua microcontrollers; Juno: Vizzy visual scripting. |
| Damage | Stormworks; From the Depths; Starbase | Stormworks: fire spread and hypothermia; FTD: block loss recalculates drag and buoyancy; Starbase: voxel and fracture with structural snap-off. |
| Exploration | Outer Wilds; NMS (COSMOS); Empyrion | Outer Wilds: loop-persistent knowledge; NMS: warp star maps and points of interest; Empyrion: per-planet gravity and atmosphere. |
| Economy | Avorion; SpaceCraft | Avorion: factory buy orders and margin-based pricing; SpaceCraft: player market with price shifts. |
| Progression gating | Satisfactory; GTNH; Factorio Space Age | Satisfactory: Space Elevator phases gate tiers; GTNH: GregTech ages gate other mods; Factorio: planet order and platform constraints. |
| Multiplayer | Stormworks; Avorion; Empyrion | Stormworks: multi-crew vehicles; Avorion: split ship roles; Empyrion: factions and alliances. Satisfactory co-op stability is a known weak point. |
| Endgame / megastructures | DSP; Satisfactory; Factorio Space Age | DSP: Dyson shells; Satisfactory: Project Assembly after Phase 5; Factorio: platforms reach the solar-system edge. |

## Coverage gaps (for follow-up)

- Praise and criticism text not captured for: Create (base), From the Depths, Juno, Spaceflight Simulator, Starbase, KSA (weak), SpaceCraft, StarRupture, Starfield 2026, GTNH 2026, NMS fleet mechanics.
- Factorio Space Age release date not captured.
- Spaceflight Simulator Steam page not confirmed.
- Empyrion 2026 patches are UNVERIFIED (hosting blog only).
- Stationeers 2026 updates UNVERIFIED (hosting blog only).
