# R2: SevTech: Ages research (Apoapsis)

Date of research: 2026-10-11. Node: R2. Scope: facts only, for the Opus designer. No design recommendations are made here.

## 0. Method, limits, and legend

**Method.** Pack facts come from the CurseForge project, file list, file changelogs and the file 3569915 relations pages (13 pages, 254 entries). Mod licences, source repos and latest Minecraft versions come from the Modrinth v2 API (`https://api.modrinth.com/v2/projects?ids=[...]`) and GitHub repo pages. Mechanics come from Darkhax and BlameJared READMEs on GitHub and docs sites.

**Limits (read these before relying on the file).**
- The SevTech Ages Fandom wiki (`sevtechages.fandom.com`) returned HTTP 402 for every fetch (Mod_List, Age_0, How_to_play_guide). Per-age detail therefore comes from secondary sources (9minecraft wiki, holy.gg guides, search summaries). Per-age mod assignments are mostly UNVERIFIED.
- The pack jar, manifest and `minecraftinstance.json` were not downloaded (prohibited). Exact mod versions for the 3.2.3 file are not confirmed. The "Ver in pack" column uses the most recent version named in a changelog I read (3.1.2-hotfix.1 of Aug 2019, 3.1.7, 3.2.0, 3.2.3). Changelogs for 3.0.x and 3.1.0 to 3.1.6 were not read.
- The WebSearch budget for this turn ran out partway through the run (the tool reported the 200-call per-turn cap was reached, shared across agents). Two checks could not be repeated: a Reddit thread search and a second "SevTech 2" search. The first "SevTech 2" search (extended mode) found nothing.
- Modrinth "not found" means no Modrinth project under the slug I tried. It does not prove the mod is absent from CurseForge.

**Legend.** UNVERIFIED = not checked or not found in any source read. "not read" = mod is in the pack list but I did not read its description; the role is UNVERIFIED. "(knowledge)" = one-line role from general knowledge of the mod, not from a fetched source. ARR = All Rights Reserved (Modrinth `LicenseRef-All-Rights-Reserved`). Licence strings are SPDX ids as Modrinth reports them, unless noted.

---

## Key findings (summary)

1. **Pack status.** SevTech: Ages is a Minecraft 1.12.2 Forge pack by darkosto. Its final file is 3.2.3 (17 Dec 2021). No newer file appears on the CurseForge file list as of 2026-10-11, roughly 4.8 years later. No "SevTech 2" announcement was found.
2. **Gating core is Darkhax's Game Stages family.** Stages are per-player named flags, granted with commands or CraftTweaker scripts. Addons restrict items, recipes, ores, dimensions, mobs, TinkerStages, JEI, Waila and JourneyMap. The 1.12.2 Darkhax addons are LGPL-2.1-only and mostly have no 1.20+ build on Modrinth.
3. **Modern equivalents exist for the stage core.** Game Stages has a 1.20.1 Forge build (CurseForge, Oct 2024). Item Stages (1.20.3) and Recipe Stages (1.20.1, MIT) have modern builds. Dimension Stages stops at 1.19.2. AStages (ARR, 1.20.1 and 1.21.1, Forge and NeoForge) is the active modern stage system, with an FTB Quests bridge. KubeJS (26.1.2, LGPL-3.0-only) has a Game Stages integration.
4. **Age gates are sourced for Ages 0 to 5.** Exit conditions: Age 0, Tinkers' Construct Melter; Age 1, Luminous Crafting Table; Age 2, Twilight Forest and Betweenlands, then a coal engine; Age 3, first plastic; Age 4, rocket to the Moon; Age 5, endgame (no single requirement stated). The Nether opens in Age 3 and the End in Age 4.
5. **Advanced Rocketry is not in the pack.** It is absent from the 254-entry relations list. Space travel is via Galacticraft (4.0.2.280 in the 3.2.0 changelog). Galacticraft Core and Planets are not in the 3.2.3 relations list, so their presence in 3.2.3 is UNVERIFIED. The only modern-named Galacticraft fork found is Galacticraft-Legacy, which is 1.12.2 only (MIT).
6. **Triumph is advancement-tree curation.** It is BnB's 1.12.2 mod for building or trimming advancement trees. Its config removes or keeps vanilla and mod advancements. No modern port was found, so the advancement-driven unlock chain has no direct modern equivalent in the sources read.
7. **Team progression is optional, not default.** Per-player progression is the default. Together Forever (team sync of advancements and GameStages, on by default, configurable) is bundled. Its modern build (ARR) exists on Modrinth at 26.2.
8. **Licensing of the modern stack is mixed.** Darkhax stage mods are LGPL-2.1. KubeJS is LGPL-3.0, CraftTweaker, JEI and Mekanism are MIT. AStages, FTB Quests and Together Forever are ARR. ARR matters if the Apoapsis project reuses these.

---

## 1. Overview

**Name and author.** SevTech: Ages (CurseForge project 268208; slug `sevtech-ages`). Project owner: darkosto. Listed team members: Firstnecron, DarkhaxDev, artdude543, Jaredlll08, vadis365, RobertDropTable. Sources: https://www.curseforge.com/minecraft/modpacks/sevtech-ages (fetched); https://www.modpackindex.com/author/2504/darkosto (search result).

**Built for.** The README says the pack was "Made by Darkosto for Sevadus on Twitch". Sources: https://github.com/DarkPacks/SevTech-Ages (README, fetched).

**Minecraft and loader.** Minecraft 1.12.2 on Forge. The CurseForge listing shows 1.12.2 as the game version. Suggested world type is `quark_realistic` (CurseForge description, fetched).

**Release history (from CurseForge file list, fetched).**
- Earliest visible file: 3.0.0 on 24 Mar 2018 (file 2544542). The list shows 20 files down to 3.0.0.
- 3.1.0 beta: 12 Mar 2019 (file 2686458). Darkosto announced it on X (search result: x.com/darkostotv/status/1105621654162464768).
- 3.1.2 beta: 19 Jul 2019 (file 2744522). 3.1.2-hotfix.1 release: 1 Aug 2019 (file 2752761).
- 3.1.7 release: 1 Sep 2020 (file 3047439). 3.2.0 beta: 30 Oct 2020 (file 3096770). 3.2.1 release: 26 Nov 2020 (file 3123514).
- 3.2.2 release: 11 Dec 2021 (file 3559141). Changelog: Chinese translation update and server security patch.
- **3.2.3 release: 17 Dec 2021 (file 3569915, `SevTech_Ages_3.2.3.zip`). Latest release.** Server files: file 3570046. Changelog (fetched): Simple Tomb 1.0.0 added; Forge 14.23.5.2858 to 14.23.5.2860; RebornCore 3.13.12.447 to 3.19.5; Corail's Tombstone removed.
- No file after 17 Dec 2021 appears on the CurseForge files page (fetched). The project page's "More from darkosto" section lists other projects with 2026 dates, not SevTech updates.

**Downloads and size.** 7,190,205 downloads on the CurseForge project page (fetched). The pack file is about 17.9 MB for 3.1.2 (fetched).

**Stated goals.** The CurseForge page says plans include "a prestige system, more advancements, extra end-game content, and bug fixes" and updates that won't corrupt worlds (fetched). Prestige is in the relations list as Prestige by Jaredlll08 (1.12.2, Modrinth). Whether a prestige system was ever shipped is UNVERIFIED.

**Spin-off.** SevTech: Ages of the Sky, a skyblock variant on 1.12.2 with most of the same mods (CurseForge page, search result: https://www.curseforge.com/minecraft/modpacks/sevtech-ages-of-the-sky).

**Development channel.** Darkosto streams development at darkosto.tv/SevTechLive (CurseForge page and GitHub README).

**Repo.** Public GitHub repo `DarkPacks/SevTech-Ages`. Fetched values: 2,667 commits, 230 stars, 74 forks, 458 open issues, 1 open PR, no releases. Pack lives in `original/`, with `docs/mods/triumph` (Triumph.txt, criteria/triggers, dataObjects, script folders). Source: https://github.com/DarkPacks/SevTech-Ages.

**Mod count.** The 3.2.3 relations list has 254 entries across 13 pages (12 full pages of 20, plus 14). Source: https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3569915/dependencies (fetched pages 1 to 13). The older MYM list (243 names, pre-3.1.2 snapshot) also includes AppleCore and Caliper, which 3.1.2 removed (https://mineyourmind.readthedocs.io/en/latest/Modpacks_Server/1.12.2/sevtech_ages.html).

**Installation facts.** GDLauncher quick-install ID `#sevtech-ages` (search result). CreeperHost lists 3.2.3 and a 4000 MB minimum for servers (search result: https://www.creeperhost.net/modpack/c268208-sevtech-ages).

---

## 2. Ages

### 2.1 Age table

Sources for the table: 9minecraft wiki (fetched: https://wiki.9minecraft.net/sevtech-ages-modpacks/ and https://www.9minecraft.net/sevtech-ages-modpacks/wiki/), holy.gg guides (search results: https://www.holy.gg/de/post/sevtech-ages-server-setup-guide and https://www.holy.gg/post/como-instalar-y-jugar-sevtech-ages-en-un-server-de-minecraft), the SevTech How-to guide snippet (https://sevtechages.gamepedia.com/How_to_play_guide_for_SevTech:_Ages, search result only, the page itself redirects to the 402 Fandom page), and the pack README (https://github.com/DarkPacks/SevTech-Ages).

| Age | Name | Gate to leave (secondary sources) | What becomes active | Signature gating items / features | Notes |
|---|---|---|---|---|---|
| 0 | Stone Age (tutorial) | Complete the advancement path up to the Tinkers' Construct Melter. | Ores are hidden. Normal wood gathering is blocked. Player works with rocks, sticks, plant fiber. Mobs described as weak. | Melter (TiC). Start has almost nothing: no map, no HWYLA (README). | Early game praised in reviews (see section 6). |
| 1 | Bronze Age | Upgrade to a Luminous Crafting Table. | Copper and tin become visible and minable, enabling bronze. Ceramics, early magic, proper crafting table, more farming tools. | Copper, tin, bronze, Luminous Crafting Table. | Ceramics mod is in this age (sources name it in the Age 1 description). |
| 2 | Medieval Age (also called Middle Ages) | Explore the Twilight Forest and the Betweenlands, then build a coal engine. | Iron, gold, lapis, rock crystal ore. More Tinkers' Construct content. Astral Sorcery. Pushes players into TF and Betweenlands. | Coal engine (recipe not confirmed). Smeltery controller needs gems from Betweenlands (review). | Betweenlands and TF exploration is the main pacing complaint in reviews. |
| 3 | Industrial Age | Craft your first piece of plastic. | Redstone and platinum ore. The Nether opens. Electricity. Steel, nether bricks, oil drilling, power, plastic. | Steel, oil, plastic. Pack includes Industrial Foregoing and Just Enough Petroleum (search result). | Plastic is described as an elaborate multi-material step. |
| 4 | Modern Age | Launch a rocket and land on the Moon. | Cobalt and ardite (manyullyn). The End opens. Applied Energistics and Galacticraft become important. Digital storage, advanced chemistry. | Manyullyn, rocket, Moon landing. | Only source for the rocket requirement is 9minecraft and holy.gg (two sources agree). |
| 5 | Futuristic / Creative Age | No single requirement stated. Described as the endgame. | Diamonds, emeralds, uranium in the overworld. Bosses, advanced machines, late-game tools. Interstellar travel and "Creative" tier items (wiki.9minecraft.net). | "Creative" tier items. | One review says this final age is "not worth playing" (Moddex headline). |

**Sub-stages.** None listed. The 9minecraft FAQ says there are six ages.

**Advancements.** The pack uses the vanilla advancement system to steer players, with "hundreds of custom advancements" (README). Players press L to view the next steps (gamepedia snippet and 9minecraft). Exact advancement IDs and their Triumph definitions were not obtained (Fandom 402; DarkPacks `docs/mods/triumph` folder contains config docs only).

**Locked items.** Locked items show as "Unfamiliar Item" and cannot be used or crafted until the age is reached (gamepedia snippet). Ores and items stay hidden until the matching age (CurseForge, BoxToPlay).

**Per-age mod assignments.** Only the mods named above are sourced. The fuller per-age mod list is UNVERIFIED (see section 4, Age column).

### 2.2 Where the dimensions and space travel sit

| Element | Where it sits | Source | Status |
|---|---|---|---|
| The Betweenlands ([ANGRY PIXEL] The Betweenlands, MrCompost) | Age 2 exit requirement (explore, collect gems) | 9minecraft wiki; MMC review | In pack (254-list). 1.12.2 only per sources read. |
| The Twilight Forest (Benimatic) | Age 2 exit requirement | 9minecraft wiki | In pack. Modern builds exist per GitHub README (Maven section 1.20.6+). |
| The Nether | Opens at Age 3 | 9minecraft wiki | Vanilla dimension. Gated by Dimension Stages only if pack scripts do so (UNVERIFIED). |
| The End | Opens at Age 4 | 9minecraft wiki | Vanilla. |
| Advanced Rocketry | Not in the pack | Absent from 254-entry relations list (fetched). Not found in any changelog. | **Not in pack.** |
| Galacticraft (core) and Galacticraft Planets | Age 4 rocket to Moon | 9minecraft; holy.gg; 3.1.2 and 3.2.0 changelogs (Galacticraft 4.0.2.280) | Core and Planets are NOT in the 3.2.3 relations list. Presence in 3.2.3 is UNVERIFIED. Galacticraft Tweaker is in the list. |
| ExtraPlanets (MJRLegends) | Space planets (inferred from name and space progression) | 3.2.0 changelog (0.7.0); Modrinth | In pack; age assignment UNVERIFIED. |
| Age 5 interstellar travel | Endgame | wiki.9minecraft.net | Mechanism UNVERIFIED. |

### 2.3 Transition design (as described by sources)

- Progression is advancement-driven. Ore is hidden until unlocked. Items and recipes change with progress. New mobs appear further along (CurseForge; README).
- Each age adds mods. Players begin with almost nothing (README).
- Age 0 is described as "primitive survival" where players "must gather materials from the environment" (9minecraft wiki).
- Reviewers say gear restrictions by age softened the pacing impact of the dimensions (MMC Reviews, April 2026).
- Per-player advancement is the default. Team syncing is optional (BoxToPlay; Together Forever, section 3.12).
- Developers say they work to keep updates from damaging existing worlds (CurseForge).

---

## 3. Gating mechanics

### 3.1 Game Stages (core, Darkhax)

Source: README of the 1.12.2 branch (https://github.com/Darkhax-Minecraft/Game-Stages/blob/1.12.2/README.md, fetched).

- A stage is a named flag tied to a player. Stages do not need to be registered. Any name works. Names must be lowercase with no spaces (underscores are fine).
- Commands: `/gamestage add <player> <stage>` (notifies), `/gamestage silentadd`, `/gamestage remove`, `/gamestage silentremove`, `/gamestage clear <player>`, `/gamestage check <player> <stage>` (can drive a comparator), `/gamestage info`, `/gamestage reloadfakes`.
- Mod developers should use the `GameStageHelper` class so that hooks and events fire. The README names no method.
- If a player cannot be identified (for example, a creeper destroys a block), the stage check fails.
- Scope: per-player. The README says nothing about teams, parties or shared progress.
- Restrictions are not built in. Game Stages is data only. Gameplay gating comes from addons and CraftTweaker scripts (modern Modrinth and search summaries agree).
- Loot condition: `"required_stage"` with `"stage_name"` gates loot table entries (README).
- Configuration: `config/gameStagesFakePlayerData.json` (fake player stages).
- Example from the README pairs it with Dimension Stages and an advancement that grants `nether_access`.

Modern: Game Stages (LGPL-2.1-only), https://github.com/Darkhax-Minecraft/Game-Stages. Modrinth lists the Forge builds up to 1.20.3 (updated 2024-10-07). CurseForge has `GameStages-Forge-1.20.1-15.0.2.jar` posted Oct 2024, with NeoForge in its loader tags (search result). The NeoForge build is not confirmed.

### 3.2 Item Stages

Source: README (https://github.com/Darkhax-Minecraft/ItemStages/blob/1.12.2/README.md, fetched).

- Restricts items and blocks by ID, or by ore dictionary entry (all items under that entry).
- Also restricts liquids (`stageLiquid`), tooltip lines (`stageTooltip`), JEI recipe categories (`stageRecipeCategory`), enchantments (`stageEnchant` for all levels, `stageEnchantByLevel`), and specific NBT-matched variants.
- Unstaged player holding a restricted item: the item drops immediately. The tooltip is replaced with a restricted message. The item is hidden in JEI if JEI is installed (NBT variants may need a separate JEI entry).
- Other effects are listed as "to come" in the README. Crafting and GUI behaviour are not described.
- Script syntax (CraftTweaker): `mods.ItemStages.addItemStage(String stage, Item/Block/OreDict);`, `setUnfamiliarName(...)`, `stageLiquid`, `stageTooltip`, `stageRecipeCategory`, `stageEnchant`, `stageEnchantByLevel`.
- Dependencies: GameStages API, CraftTweaker (config), JEI (optional).

Modern: Item Stages, LGPL-2.1-only, Forge, latest listed 1.20.3 on Modrinth (https://api.modrinth.com/v2/projects?ids=["item-stages"]).

### 3.3 Recipe Stages

Source: BlameJared docs (https://docs.blamejared.com/1.18/en/mods/RecipeStages/RecipeStages, fetched, page version 1.18 layout).

- Locks crafting recipes behind a stage. Shaped, mirrored shaped and shapeless recipes can be added with a stage. Existing recipes can be assigned a stage by output item, by resource location or by mod ID.
- Machine recipes that do not go through a normal crafting table can be gated by container class (`setContainerStages`) or by package prefix (`setPackageStages`). Examples named: Refined Storage's Crafting Grid, AE2's crafting terminal. A broad prefix can affect unrelated mods.
- `setPrintContainers(true)` logs container and package names to crafttweaker.log when a player tries to craft in a table.
- Methods: `mods.recipestages.Recipes.addShaped(String stage, String recipeName, IItemStack output, IIngredient[][] ingredients, ...)`, `addShapeless(...)`, `setRecipeStage(...)`, `setRecipeStageByMod(String name, String modid)`, `clearRecipeStage(...)`, `showJEILabel(boolean)`.
- Locked-player experience: JEI "Stage:" label shown by default (`showJEILabel`). Whether the recipe is hidden or uncraftable is not stated on the docs page.

Modern: Recipe Stages, MIT, Forge, latest listed 1.20.1 on Modrinth (https://github.com/jaredlll08/RecipeStages).

### 3.4 Ore Stages (disguised ores)

Source: README (https://github.com/Darkhax-Minecraft/Ore-Stages, fetched; default branch master, repo archived read-only 22 Feb 2024).

- A locked player sees the block the ore is disguised as (for example stone). The player cannot right-click it. It drops the items its replacement would drop, and it takes as long to mine as the replacement.
- Defaulting behaviour: when no player is involved, the mod assumes no stage, so the block stays hidden. The `addNonDefaultingReplacement` variants break as if they were never hidden when water, explosions or machines break the block.
- Syntax: `mods.orestages.OreStages.addReplacement(String stage, IIngredient original)` (replaces with vanilla stone), `addReplacement(stage, original, replacement)`, `addReplacementById(stage, original, replacement)` (exact IDs), and matching `addNonDefaultingReplacement*` methods. The first two only work on blocks that have items.
- Dependencies: GameStage API, CraftTweaker.

Modern: Ore Stages Reborn (LGPL-2.1-only, fabric and forge, latest listed 1.20.4, https://github.com/SagaDeoMissTeam/Ore-Stages-Reborn). SDM Ore Stages is described as a rewrite for 1.21 and up with CraftTweaker and KubeJS support, default disguise stone or deep slate, and full BlockState control (search result; Modrinth slug `sdm-ore-stages` not found). Ore Stages: ReStaged is a bug-fix fork (search result).

### 3.5 Dimension Stages

Source: https://github.com/Darkhax-Minecraft/DimensionStages (fetched).

- Dimensions are unstaged by default. A script stages a dimension, and players need one or more of the listed stages to enter.
- `DimensionStages.stageDimension(dimensionId, stages);` and `DimensionStages.stageDimensionWithMessage(dimensionId, message, stages);`.
- The README says a custom denial message can be shown. It does not describe portal or travel handling.
- Depends on Game Stages and CraftTweaker. License LGPL-2.1.

### 3.6 Mob Stages

Source: https://github.com/Darkhax-Minecraft/MobStages (fetched).

- Gates spawning only. A mob cannot spawn if no valid player within range has the stage (default search range 256).
- Replacement mobs can spawn in place of a gated mob. A spawner toggle lets a mob ignore stage rules.
- Syntax: `mods.MobStages.addStage("one", "minecraft:creeper");`, `mods.MobStages.toggleSpawner(...)`, `mods.MobStages.addReplacement(...)`, `addRange`. Dimension-specific variants take a numeric dimension ID. The README names `toggleSpawners` in setup and `toggleSpawner` in the example (inconsistent).
- Modern: none on Modrinth beyond 1.12.2.

### 3.7 TinkerStages

Source: https://github.com/Darkhax-Minecraft/TinkerStages (fetched; archived read-only 6 Feb 2024).

- Gates parts of Tinkers' Construct behind stages: general gates for tool crafting at the tool station, part replacing, part building at the part builder, and applying modifiers. Specific gates for a single tool type, a single material, or a single modifier.
- A player with at least one listed stage can use a general system.
- Methods: `mods.TinkerStages.addGeneralCraftingStage`, `addGeneralPartReplacingStage`, `addGeneralPartBuildingStage`, `addGeneralModifierStage`, `addToolTypeStage(stage, toolId)`, `addMaterialStage(stage, material)`, `addModifierStage(stage, modifier)`.
- The README does not describe melting or casting gates, nor UI feedback.

### 3.8 Waila Stages

Source: https://github.com/Darkhax-Minecraft/WailaStages (fetched).

- Restricts the WAILA/HWYLA HUD behind a stage (`addWailaStage`). Specific tooltip lines can be restricted by prefix (`addRequirement(stage, prefix)`). If several stages are added to the HUD, the player needs at least one.
- No config file options documented.
- Modern: the successor HUD is Jade (Snownee, CC-BY-NC-SA-4.0, latest 26.3 on Modrinth). Whether Jade has a stage hook is UNVERIFIED.

### 3.9 JourneyMapStages

Source: https://github.com/Darkhax-Minecraft/JourneyMapStages (fetched).

- Four gated features: waypoints (cannot create or open manager; existing ones are destroyed immediately), deathpoints (none created until unlocked; existing ones destroyed), minimap (hidden), fullscreen map and options (unavailable).
- Syntax: `mods.jmapstages.JMapStages.setWaypointStage`, `setDeathpointStage`, `setMinimapStage`, `setFullscreenStage`.

### 3.10 Multiblock Stages

Not read. The pack lists MultiBlock Stages (Sky_Som, entry 148). Its behaviour is UNVERIFIED.

### 3.11 What an unstaged player sees (summary)

| Mechanic | Unstaged player sees | Can they interact? | Source |
|---|---|---|---|
| Ore Stages | The disguise block (e.g. stone). Mining time and drops of the disguise. | No right-click. Non-defaulting variants: break as if unhidden when water, explosion or machine breaks it. | Ore Stages README |
| Item Stages | Item drops if held. Tooltip replaced. JEI hides it. | Cannot use or hold (drops on pickup, per README). | Item Stages README |
| Locked items in SevTech | "Unfamiliar Item" name. Cannot be used or crafted until age. | No. | gamepedia snippet |
| Recipe Stages | JEI "Stage:" label. | Recipe gating behaviour not stated on docs page. | BlameJared docs |
| Dimension Stages | Denial message (optional). | Cannot enter. | Dimension Stages README |
| Mob Stages | No spawning of gated mobs within range. | n/a | Mob Stages README |
| TinkerStages | Gated action is prevented. | No. | TinkerStages README |
| Waila Stages | HUD hidden. Restricted tooltip lines hidden. | n/a | Waila Stages README |
| JourneyMapStages | Waypoints, deathpoints, minimap, fullscreen map unavailable. | n/a | JourneyMapStages README |

### 3.12 Multiplayer: per-player vs per-team

- **Default is per-player.** Game Stages stores stages per player (README). The CurseForge page says "Each player advances individually, and team syncing is optional" (BoxToPlay search result). Progression is tracked per player (CurseForge fetched).
- **Team sync: Together Forever** (Buuz135, pack entry 227). Sources: MetaMods page (https://metamods.net/en/mods/together-forever) and CurseForge pack page. Behaviour per search summaries: a team system and API that syncs advancements and GameStages among teammates. Both sync types are enabled by default and can be disabled in config. Teammates offline when a sync fires receive it on joining. Team commands are `/tf` (invite, accept, decline, kick, leave, info, forced sync). Only the team creator can kick. The 1.12.2 build targets Forge 14.23.1.2560 with optional GameStages 1.0.76 (search summary of the 1.12.2 listing). Modern: Together Forever on Modrinth (LicenseRef-All-Rights-Reserved, fabric and forge, latest 26.2; https://api.modrinth.com/v2/projects?ids=["together-forever"]).
- **Teams Stages** (separate addon for Game Stages plus FTB Teams) lets a stage attach to a team (search result: https://www.curseforge.com/minecraft/mc-mods/teams-stages/install). Compatibility with Together Forever is UNVERIFIED.
- **FTB modern path.** FTB Quests (NeoForge 26.1.2.9, uploaded 7 Oct 2026, ARR, team-based questing, 226M downloads) is the modern team-quest system (https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge). FTB Teams: not verified.

### 3.13 Triumph (advancement-tree tool)

- Mod: Triumph (BloodNBonesGaming, pack entry 231). Described as a tool for modpack creators that builds custom advancement trees in place of vanilla's, defined with JSON or script. Requires BnBGamingLib 2.8.1+ and BnBGamingCore 0.3.1+. Last updated 10 Nov 2019 (craftane.onrender.com search result).
- Pack config (DarkPacks `docs/mods/triumph/Triumph.txt`, fetched raw): boolean `removeAdvancements` (list of mod or advancement IDs to remove; keeps all others if set), `removeModAdvancements`, `keepAdvancements` (list to keep; removes everything else if set), `removeVanillaAdvancements`, `printDefaultConfigs`, `printDocumentation`, `forceReloadFunctions` (no effect).
- Pack folder also has `criteria/triggers`, `dataObjects` and `script` folders (names only; contents not read).
- Better Advancements is recommended by Triumph's author for its GUI (search result).
- The pack does not expose its exact advancement IDs in the sources read. The exact Triumph tree for each age is UNVERIFIED.
- Modern: no port found. Better Advancements has a 26.3 build (LicenseRef-Dont-Be-a-Jerk, Modrinth).

### 3.14 Modern KubeJS hooks for stages

- KubeJS built-in Game Stages integration page (https://kubejs.com/wiki/addons/gamestages, fetched, last updated 2 Mar 2026): "Stage Added Event" and "Stage Removed Event" (non-cancellable); `.stages` property on entities with `.add(stage)`, `.remove(stage)`, `.has(stage)`. Scope (per-player vs global) and loader versions not stated.
- KubeJS 1.20.1 report (GitHub issue #821, search result): stage-added event did not fire for stages added by script or FTB Quests reward, only via command. Whether fixed is UNVERIFIED.

### 3.15 Warning from community (combined stages)

Found Enough Items (1.12.2) describes using Game Stages, Item Stages and Recipe Stages together to hide items and recipes, and warns of "enormously high" performance overhead and strain on pack developers (modpackindex search result: https://www.modpackindex.com/mod/63090/found-enough-items).

---

## 4. Full mod list (3.2.3 relations, 254 entries)

**Source of names and authors:** CurseForge file 3569915 relations, pages 1 to 13 (https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3569915/dependencies?page=N, fetched). Order is the relations order. Not every name is a mod in the jar: the list is the set of included relations.

**Column meanings.**
- **Ver in pack:** the most recent version named in a changelog read: 3.1.2h = 3.1.2-hotfix.1 (1 Aug 2019); 3.1.7; 3.2.0; 3.2.3. UNVERIFIED = not named in any changelog read. The exact version inside the 3.2.3 jar is UNVERIFIED for every row.
- **Age:** a sourced age only when cited in section 2. Otherwise UNVERIFIED (or "inferred", explained in the cell).
- **License:** Modrinth licence id (or GitHub, where noted). "ARR" = All Rights Reserved.
- **Latest MC:** highest Minecraft version on Modrinth (Modrinth game_versions list, ascending) or GitHub branch (where noted). Java latest release is 26.3 (section 5).
- **Modern port:** modern equivalent with the Modrinth slug or GitHub repo, if found.

**Not in 3.2.3 relations, but named in earlier changelogs** (status UNVERIFIED for 3.2.3): ArcFurnacePatch 1.0.0.3 (added 3.1.2), JustEnoughIDsIntegration 1.2.1.29 (added 3.1.2), Advanced Mortars 1.6.23, Galacticraft Core 4.0.2.280, Galacticraft Planets 4.0.2.280, MicdoodleCore 4.0.2.280, Nutrition 4.3.0, PlayerBosses 1.0.0, Corail's Tombstone 3.5.3 (removed in 3.2.3), AppleCore, Caliper, Sampler, World Stripper (removed in 3.1.2). Advanced Rocketry is not named anywhere.

| # | Mod (author) | Ver in pack | What it does in pack | Age | License | Source repo | Latest MC | Modern port / equivalent |
|---|---|---|---|---|---|---|---|---|
| 1 | To the Bat Poles! (ByEnderLanky) | UNVERIFIED | not read | UNVERIFIED | CC-BY-ND-4.0 (Modrinth slug match, author UNVERIFIED) | NONE listed | 26.3 (Modrinth) | to-the-bat-poles on Modrinth (author match UNVERIFIED) |
| 2 | A Block of Charcoal (TurkeyDev) | UNVERIFIED | charcoal storage block (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 3 | AbyssalCraft (Shinoow) | 1.9.8 (3.1.2h) | Lovecraftian dimension and mobs (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 4 | AbyssalCraft Integration (Shinoow) | 1.8.1 (3.1.2h) | AbyssalCraft integration (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 5 | Actually Additions (Ellpeck) | r149 (3.1.2h) | automation and utility machines (knowledge) | UNVERIFIED | MIT | https://github.com/Ellpeck/ActuallyAdditions | 1.21.1 | Modrinth actually-additions (1.21.1, Forge/NeoForge) |
| 6 | Actually Baubles (Jacky) | UNVERIFIED | Baubles integration (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none found on Modrinth |
| 7 | Advanced Generators (bdew) | UNVERIFIED | power generators (knowledge) | UNVERIFIED | MIT | https://github.com/bdew-minecraft/generators | 1.20.1 | Modrinth advanced-generators (1.20.1) |
| 8 | Animalium (vadis365) | UNVERIFIED | animal additions (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 9 | Antique Atlas (Hunternif) | 4.5.1 (3.1.2h) | atlas map item (knowledge) | UNVERIFIED | GPL-3.0-only | https://github.com/AntiqueAtlasTeam/AntiqueAtlas | 1.18.2 | Modrinth antique-atlas (1.18.2; not 1.20+) |
| 10 | AppleSkin (squeek502) | UNVERIFIED | food and saturation HUD (knowledge) | UNVERIFIED | Unlicense | https://github.com/squeek502/AppleSkin | 26.3 | AppleSkin 26.x (same GitHub owner) |
| 11 | Applied Energistics 2 (thetechnici4n) | UNVERIFIED | ME storage and autocrafting (knowledge) | Age 4 (cited) | LicenseRef-Multiple | https://github.com/AppliedEnergistics/Applied-Energistics-2 | 26.3 | Modrinth ae2 (26.x) |
| 12 | ArmoreableMobs (Buuz135) | UNVERIFIED | mobs wear armour (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 13 | Aroma1997Core (Aroma1997) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 14 | AromaBackup (Aroma1997) | UNVERIFIED | backup utility (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 15 | AstikorCarts [Horse Carts] (issork) | 0.1.2.7 (3.1.2h) | horse-drawn carts (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 16 | Astral Sorcery (HellFirePvP) | 1.10.19 (3.1.2h) | starlight and constellation magic (knowledge; GitHub 1.21.1 branch "Some magic mod") | Age 2 (cited) | UNVERIFIED (LICENSE file, name not shown) | https://github.com/HellFirePvP/AstralSorcery | 1.21.1 (GitHub branch) | NeoForge 1.21.1 branch in same repo (not on Modrinth) |
| 17 | AutoRegLib (Vazkii) | 1.3-31 (3.1.2h) | registry helper library (knowledge) | UNVERIFIED | LicenseRef-Multiple | https://github.com/VazkiiMods/AutoRegLib | 1.19.2 | none on 1.20+ (Modrinth) |
| 18 | AutoRun (Mysticdrew) | UNVERIFIED | auto-run key (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth autorun is a different author (Emonadeo, CC0-1.0); not a match |
| 19 | B.A.S.E (lanse505) | UNVERIFIED | not read | UNVERIFIED | MIT | https://github.com/The-Acronym-Coders/B.A.S.E | 1.12.2 | none |
| 20 | BNBGamingLib (bloodnbonesgaming) | 2.17.6 (3.1.2h) | shared library for BnB mods | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none found |
| 21 | Baubles (Azanor13) | UNVERIFIED | accessory slots (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 22 | BdLib (bdew) | UNVERIFIED | library (knowledge) | UNVERIFIED | MIT | https://github.com/bdew-minecraft/bdlib | 1.20.1 | Modrinth bdlib (1.20.1) |
| 23 | Bedrock B Gone (Karjah) | UNVERIFIED | not read | UNVERIFIED | ARR | NONE | 1.12.2 | none |
| 24 | Better Advancements (way2muchnoise) | 0.1.0.77 (3.1.2h) | advancements screen (knowledge; Triumph author recommends it) | UNVERIFIED | LicenseRef-Dont-Be-a-Jerk | https://github.com/way2muchnoise/BetterAdvancements | 26.3 | Better Advancements 26.3 (Fabric, Forge, NeoForge) |
| 25 | Better Builder's Wands (Portablejim) | 0.13.2.271 (3.1.2h) | wand-based building (knowledge) | UNVERIFIED | ARR | NONE | 1.12.2 | none |
| 26 | Better Foliage (octarine_noise) | UNVERIFIED | foliage textures (knowledge) | UNVERIFIED | MIT (Modrinth slug, author UNVERIFIED) | NONE | 26.3 | Modrinth better-foliage (author match UNVERIFIED) |
| 27 | Better With Addons (BordListian) | 0.50 (3.1.2h) | addons (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 28 | Better With Lib (BWM - Core) (primetoxinz) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 29 | Better With Mods (BeetoGuy) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 30 | BetterFps (Guichaguri) | UNVERIFIED | rendering performance (knowledge) | UNVERIFIED | MIT (Modrinth "betterfps" is a Fabric and Quilt project) | NONE | 1.21.4 (Modrinth, mismatch likely) | not a verified match |
| 31 | BiblioCraft (Nuchaz) | UNVERIFIED | bookshelf and storage (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 32 | Blood Magic (WayofTime) | 2.4.1-103 (3.1.2h) | blood magic and rituals (knowledge) | UNVERIFIED | CC-BY-4.0 | https://github.com/WayofTime/BloodMagic | 1.20.1 | Modrinth blood-magic (1.20.1, Forge) |
| 33 | BnBGamingCore (bloodnbonesgaming) | 0.11.0 (3.1.2h) | core API for BnB mods (search summary) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none found |
| 34 | Bonsai Trees 4 (davenonymous) | UNVERIFIED | decorative trees (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 35 | Bookshelf (DarkhaxDev) | 2.3.581 (3.1.2h) | Darkhax-era utility library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | Modrinth "bookshelf" is a different project (LOOHP, Paper plugin); not a match |
| 36 | Bring Back Bedrock (JayJay1989BE) | 0.0.4 (3.1.7) | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 37 | BuildCraft\|Builders (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft module (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 38 | BuildCraft\|Core (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft core (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 39 | BuildCraft\|Factory (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft factory (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 40 | BuildCraft\|Robotics (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft robotics (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 41 | BuildCraft\|Silicon (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft silicon (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 42 | BuildCraft\|Transport (Covert_Jaguar) | 7.99.24.1 (3.1.2h) | BuildCraft transport (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 43 | Building Gadgets (Direwolf20) | 2.6.8 (3.1.2h) | building wand (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 44 | CB Multipart (covers1624) | UNVERIFIED | multipart block framework (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/TheCBProject/CBMultipart | 1.21.1 | Modrinth cb-multipart (1.21.1) |
| 45 | CD4017BE Library (Cd4017be) | 6.4.6 (3.1.2h) | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 46 | Carry On (Tschipp) | UNVERIFIED | pick up blocks and entities (knowledge) | UNVERIFIED | LGPL-3.0-only | https://github.com/Tschipp/CarryOn | 26.3 | Carry On 26.3 (Modrinth) |
| 47 | Ceramics (KnightMiner) | UNVERIFIED | ceramic items (knowledge) | Age 1 (cited) | MIT | https://github.com/KnightMiner/Ceramics | 1.20.1 | Modrinth ceramics (1.20.1) |
| 48 | Chameleon (Texelsaur) | UNVERIFIED | not read | UNVERIFIED | MIT (Modrinth "chameleon" is Fabric/Quilt; author UNVERIFIED) | https://github.com/LemonCaramel/Chameleon | 1.21.11 | mismatch likely |
| 49 | Chargers (GoryMoon) | UNVERIFIED | charger blocks (knowledge) | UNVERIFIED | LGPL-3.0-or-later | https://github.com/GoryMoon/Chargers | 1.21.1 | Modrinth chargers (1.21.1) |
| 50 | Chisel (tterrag1098) | UNVERIFIED | decorative block carving (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 51 | Chisels & Bits - For Forge (AlgorithmX2) | 14.33 (3.1.2h) | block carving (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 52 | Client Tweaks (BlayTheNinth) | UNVERIFIED | client-side tweaks (knowledge) | UNVERIFIED | ARR | https://github.com/TwelveIterations/ClientTweaks | 26.3 | Client Tweaks 26.3 (TwelveIterations) |
| 53 | Clumps (Jaredlll08) | UNVERIFIED | XP orb clumping (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 54 | CodeChicken Lib 1.8.+ (covers1624) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 55 | Coloured Tooltips (DarkhaxDev) | UNVERIFIED | tooltip colours (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Coloured-Tooltips | 1.19.2 | Modrinth coloured-tooltips (1.19.2) |
| 56 | Common Capabilities (kroeser) | 2.4.1 (3.1.2h) | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 57 | Compact Machines (davenonymous) | 3.0.18-b278 (3.1.2h) | pocket-dimension machines (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 58 | ConnectedTexturesMod (tterrag1098) | UNVERIFIED | connected textures (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 59 | Construct's Armory (TheIllusiveC4) | 1.2.4 (3.1.2h) | Tinkers' armour (knowledge) | UNVERIFIED | LGPL-3.0-or-later | https://github.com/TheIllusiveC4/ConstructsArmory | 1.16.5 | Modrinth constructs-armory (1.16.5; not 1.20+) |
| 60 | ContentTweaker (Jaredlll08) | UNVERIFIED | content scripting (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 61 | Controlling (Jaredlll08) | UNVERIFIED | key-binding search (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 62 | Cooking for Blockheads (BlayTheNinth) | 6.4.70 (3.1.2h) | cooking (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 63 | CraftTweaker (Jaredlll08) | 4.1.18 (3.1.2h) | ZenScript scripting (knowledge) | infrastructure | MIT | https://github.com/CraftTweaker/CraftTweaker | 1.21.1 | CraftTweaker 1.21.1 (Forge, NeoForge, Fabric) |
| 64 | Cucumber Library (BlakeBr0) | UNVERIFIED | library (knowledge) | UNVERIFIED | MIT | https://github.com/BlakeBr0/Cucumber | 26.1.2 | Cucumber 26.1.2 |
| 65 | Custom Main Menu (Lumien231) | UNVERIFIED | main menu (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 66 | Cyclic (Lothrazar) | 1.19.11 (3.1.2h) | utility blocks and items (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 67 | Cyclops Core (kroeser) | 1.3.0 (3.1.2h) | library (knowledge) | UNVERIFIED | MIT | https://github.com/CyclopsMC/CyclopsCore | 26.3 | Cyclops Core 26.3 |
| 68 | Dark Utilities (DarkhaxDev) | 1.8.226 (3.1.2h, as DarkUtils) | utility blocks (knowledge) | UNVERIFIED | LGPL-2.1-only (Modrinth; source URL cut off in summary) | UNVERIFIED | 26.3 | Dark Utilities 26.3 (Modrinth) |
| 69 | Death Compass (vadis365) | UNVERIFIED | death location compass (knowledge) | UNVERIFIED | ARR | NONE | 1.20.6 | Death Compass 1.20.6 (Modrinth) |
| 70 | Default Options (BlayTheNinth) | UNVERIFIED | default options (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 71 | Despawning Spawners (Buuz135) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 72 | Dimension Stages (DarkhaxDev) | UNVERIFIED | gates dimension access by stage (README, section 3.5) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/DimensionStages | 1.19.2 | none on 1.20+ (Modrinth) |
| 73 | Dimensional Control (bloodnbones) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 74 | Dung Pipe (BordListian) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 75 | EmberRoot Zoo (Lothrazar) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 76 | Enchantment Descriptions (DarkhaxDev) | UNVERIFIED | enchantment tooltips (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 77 | Ender Storage (covers1624) | UNVERIFIED | ender chests (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 78 | Ender Utilities (masady) | UNVERIFIED | utility blocks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 79 | Environmental Tech (ValkyrieofNight) | 2.0.20.1 (3.1.2h) | machines (knowledge) | UNVERIFIED | ARR | NONE | 1.16.5 | Modrinth environmental-tech (1.16.5) |
| 80 | Extended Crafting (BlakeBr0) | 1.5.4 (3.1.2h) | crafting table tiers (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 81 | ExtraPlanets (MJRLegends) | 0.7.0 (3.2.0) | space planets (knowledge) | Age 4-5 (inferred from space progression) | ARR | https://github.com/MJRLegends/ExtraPlanets | 1.12.2 | none found |
| 82 | Fancy Block Particles (MorphoxL) | UNVERIFIED | block particles (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 83 | Farming for Blockheads (BlayTheNinth) | UNVERIFIED | farming (knowledge) | UNVERIFIED | ARR | https://github.com/TwelveIterations/FarmingForBlockheads | 26.3 | Farming for Blockheads 26.3 (TwelveIterations) |
| 84 | Farseek (delvr) | 2.3.2 (3.1.2h) | not read | UNVERIFIED | MIT | https://github.com/delvr/Farseek-Mods | 26.2 | Farseek 26.2 (Modrinth) |
| 85 | FastFurnace (Shadows_of_Fire) | 1.3.1 (3.1.2h) | furnace speed (knowledge) | UNVERIFIED | MIT | https://github.com/Shadows-of-Fire/FastFurnace | 26.1.2 | FastFurnace 26.1.2 |
| 86 | FastWorkbench (Shadows_of_Fire) | 1.7.2 (3.1.2h) | workbench speed (knowledge) | UNVERIFIED | MIT | https://github.com/Shadows-of-Fire/FastWorkbench | 26.1.2 | FastWorkbench 26.1.2 |
| 87 | Fat Cat (vadis365) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 88 | Ferdinand's Flowers (c0_0ry) | UNVERIFIED | flowers (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 89 | FindMe (Buuz135) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 90 | FoamFix (asiekierka) | 0.10.5 (3.1.2h) | performance (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | Modrinth slug not found |
| 91 | Galacticraft Tweaker (MJRLegends) | UNVERIFIED | Galacticraft recipe and item tweaks (search summary) | Age 4 (inferred) | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 92 | Game Stages (DarkhaxDev) | 2.1.115 (3.1.2h, as Gamestages) | per-player stage API (README, section 3.1) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Game-Stages | 1.20.3 (Modrinth Forge); CurseForge 1.20.1 15.0.2 | Game Stages 1.20.1 (CurseForge file 15.0.2) |
| 93 | Geolosys (oitsjustjose) | 2.2.6 (3.1.2h) | geology and ores (knowledge) | UNVERIFIED | GPL-3.0-only | https://git.oitsjustjose.com/me/Geolosys/ | 1.20.1 | Modrinth geolosys (1.20.1) |
| 94 | Giant Player Boss (Lothrazar) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 95 | Gobble Core (TurkeyDev) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 96 | Guide-API (TehNut) | UNVERIFIED | guidebook API (knowledge) | UNVERIFIED | MIT (Modrinth guide-api; author match UNVERIFIED) | https://github.com/maxanier/Guide-API | 26.1.2 | Guide-API 26.1.2 (author match UNVERIFIED) |
| 97 | Horse Power (GoryMoon) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED (Modrinth horse-power is a different author, AGPL-3.0-only) | UNVERIFIED | UNVERIFIED | mismatch likely |
| 98 | Hunting Dimension (DarkhaxDev) | UNVERIFIED | dimension (name; README not read) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Hunting-Dimension | 1.12.2 | none |
| 99 | Hwyla (TehNut) | UNVERIFIED | HUD (knowledge) | UNVERIFIED | CC-BY-NC-SA-4.0 | https://github.com/TehNut/HWYLA | 1.16.5 | Jade (Snownee, CC-BY-NC-SA-4.0, 26.3) as HUD successor |
| 100 | Immersive Craft (McJty) | UNVERIFIED | not read | UNVERIFIED | MIT | https://github.com/McJtyMods/ImmersiveCraft | 1.12.2 | none |
| 101 | Immersive Engineering (BluSunrize) | UNVERIFIED | electrical tech and multiblocks (knowledge; GitHub description) | Age UNVERIFIED (3-4 likely) | UNVERIFIED (LICENSE file, name not shown) | https://github.com/BluSunrize/ImmersiveEngineering | 1.21.1 (GitHub branch) | 1.21.1 branch (GitHub; not on Modrinth) |
| 102 | Immersive Petroleum (Flaxbeard) | UNVERIFIED | oil processing (knowledge) | Age 3 (inferred from oil progression) | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 103 | Immersive Tech (Forge_User_11180423) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 104 | Improved Backpacks (DreenDex) | UNVERIFIED | backpacks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 105 | In Control! (McJty) | 3.9.8 (3.1.2h) | not read | UNVERIFIED | MIT | https://github.com/McJtyMods/InControl | 26.2 | In Control 26.2 (Modrinth) |
| 106 | In-Game Wiki Mod (Forge_User_59359881) | UNVERIFIED | in-game wiki (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 107 | Inductive Logistics (Cd4017be) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 108 | Industrial Foregoing (Buuz135) | 1.12.11-235 (3.1.2h) | machines (knowledge) | Age 3 (inferred) | MIT | https://github.com/Buuz135/Industrial-Foregoing | 1.21.1 | Modrinth industrial-foregoing (1.21.1) |
| 109 | Informational Accessories (DarkhaxDev) | UNVERIFIED | information accessories (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Informational-Accessories | 1.12.2 | none |
| 110 | Integrated Dynamics (kroeser) | 1.0.14 (3.1.2h) | logic and data network (knowledge) | UNVERIFIED | MIT | https://github.com/CyclopsMC/IntegratedDynamics | 26.3 | Integrated Dynamics 26.3 |
| 111 | Inventory Tweaks [1.12 only] (JimeoWan) | UNVERIFIED | inventory UI helper (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | 1.12 only (name) | none |
| 112 | Iron Backpacks (gr8pefish) | UNVERIFIED | backpacks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 113 | Iron Chests (ProgWML6) | 7.0.69.845 (3.1.2h) | chests (knowledge) | UNVERIFIED | GPL-3.0-only | https://github.com/progwml6/ironchest | 1.21.11 | Iron Chests 1.21.11 (Modrinth) |
| 114 | Iron Jetpacks (BlakeBr0) | UNVERIFIED | jetpacks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 115 | Item Stages (DarkhaxDev) | 2.0.49 (3.1.2h) | item and block gating (README, section 3.2) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/ItemStages | 1.20.3 | Item Stages 1.20.3 (Modrinth) |
| 116 | IvToolkit (Ivorius) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 117 | JourneyMap (techbrew) | UNVERIFIED | map (knowledge) | UNVERIFIED | ARR | https://github.com/TeamJM/journeymap | 26.3 | JourneyMap 26.3 (Modrinth) |
| 118 | JourneyMapStages (DarkhaxDev) | 2.0.9 (3.1.2h) | gates waypoints, deathpoints, minimap, fullscreen map (section 3.9) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/JourneyMapStages | 1.12.2 | none |
| 119 | Just A Raft Mod (Mrbysco) | UNVERIFIED | raft (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 120 | Just Enough Items (JEI) (mezz) | 4.15.0.278 (3.1.2h) | recipe viewer; hides staged items (section 3.2) | infrastructure | MIT | https://github.com/mezz/JustEnoughItems | 26.3 | JEI 26.3 (Modrinth) |
| 121 | Just Enough Petroleum (BordListian) | UNVERIFIED | oil (name) | Age 3 (inferred) | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 122 | Just a Few Fish (Tmtravlr) | UNVERIFIED | fish (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 123 | JustEnoughIDs (Runemoro) | UNVERIFIED | ID expansion (knowledge; 3.1.2 added JustEnoughIDsIntegration) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 124 | KleeSlabs (BlayTheNinth) | UNVERIFIED | slabs (knowledge) | UNVERIFIED | ARR | https://github.com/TwelveIterations/KleeSlabs | 26.3 | KleeSlabs 26.3 (Modrinth) |
| 125 | LibraryEx (LogicTechCorp) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 126 | Loading Screens (bloodnbones) | UNVERIFIED | loading screens (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 127 | LootTableTweaker (DarkhaxDev) | UNVERIFIED | loot tables (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 128 | MJRLegends Lib (MJRLegends) | 1.2.0 (3.2.0) | library (knowledge) | UNVERIFIED | ARR | https://github.com/MJRLegends/MJRLegendsLib | 1.12.2 | none |
| 129 | MPUtils (GenDeathrow) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 130 | MPUtils Basic Tools (GenDeathrow) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 131 | MTLib (Jaredlll08) | 3.0.6 (3.1.2h) | library (knowledge) | UNVERIFIED | MIT | NONE | 1.12.2 | none |
| 132 | Magma Monsters (Kashdeya) | UNVERIFIED | mobs (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 133 | Mantle (mDiyo) | 1.3.3.55 (3.1.2h) | library for Tinkers (knowledge) | UNVERIFIED | MIT | https://github.com/SlimeKnights/Mantle | 1.20.1 | Mantle 1.20.1 (Modrinth) |
| 134 | McJtyLib (McJty) | UNVERIFIED | library (knowledge) | UNVERIFIED | MIT | https://github.com/McJty/McJtyLib | 1.21.1 | McJtyLib 1.21.1 (Modrinth) |
| 135 | MekaTweaks (Firstnecron) | UNVERIFIED | Mekanism tweaks (name) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 136 | Mekanism (bradyaidanc) | 9.7.9.380 (3.1.2h) | machines and power (knowledge) | Age 3-4 (inferred) | MIT | https://github.com/mekanism/Mekanism | 1.21.1 | Mekanism 1.21.1 (Modrinth) |
| 137 | Mercurius (TamasHenning) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 138 | Mo' Bends (IVESIRIS) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 139 | Mo' Villages (TheWeatherPony) | UNVERIFIED | villages (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 140 | Mob Grinding Utils (vadis365) | UNVERIFIED | mob grinder (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 141 | Mob Stages (DarkhaxDev) | UNVERIFIED | gates mob spawning (section 3.6) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/MobStages | 1.12.2 | none |
| 142 | ModTweaker (Jaredlll08) | 4.0.17 (3.1.2h) | CraftTweaker predecessor (knowledge) | infrastructure | MIT | https://github.com/jaredlll08/ModTweaker | 1.12.2 | superseded by CraftTweaker 1.21.1 |
| 143 | Modular Machinery (HellFirePvP) | UNVERIFIED | multiblock machines (knowledge) | UNVERIFIED | UNVERIFIED (1.12 original; Modrinth shows only the fork) | https://github.com/alec016/modular-machinery-reborn (fork) | 1.21.1 | Modular Machinery Reborn (LGPL-3.0-only, NeoForge 1.21.1; fork, not the original) |
| 144 | Modular Routers (desht_08) | UNVERIFIED | item routers (knowledge) | UNVERIFIED | MIT | https://github.com/desht/ModularRouters | 26.2 | Modular Routers 26.2 |
| 145 | More Overlays (feldim245) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 146 | Morpheus (Quetzi) | UNVERIFIED | sleep and time skip (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 147 | Mouse Tweaks (YaLTeR) | UNVERIFIED | inventory mouse tweaks (knowledge) | UNVERIFIED | BSD-3-Clause | https://github.com/YaLTeR/MouseTweaks | 26.3 | Mouse Tweaks 26.3 (Modrinth) |
| 148 | MultiBlock Stages (Sky_Som) | UNVERIFIED | multiblock gating (name; not read) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 149 | Mundane Redstone Ore (ShyNieke) | UNVERIFIED | redstone ore (knowledge) | UNVERIFIED | MIT | https://github.com/ShyNieke/MundaneRedstone | 26.1.1 | Mundane Redstone Ore 26.1.1 |
| 150 | MystAgrad Cloche Compat (senilepenguin) | UNVERIFIED | cloche compat (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 151 | Mystical Agradditions (BlakeBr0) | 1.3.2 (3.1.2h, as MystAgrad) | Mystical Agriculture addon (knowledge) | UNVERIFIED | MIT | https://github.com/BlakeBr0/MysticalAgradditions | 26.1.2 | Mystical Agradditions 26.1.2 |
| 152 | Mystical Agriculture (BlakeBr0) | 1.7.5 (3.1.2h) | crop-essence farming (knowledge) | UNVERIFIED | MIT | https://github.com/BlakeBr0/MysticalAgriculture | 26.1.2 | Mystical Agriculture 26.1.2 |
| 153 | Natura (mDiyo) | 4.3.2.69 (3.1.2h) | trees and plants (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 154 | Nature's Compass (Chaosyr) | UNVERIFIED | biome compass (knowledge) | UNVERIFIED | CC-BY-NC-SA-4.0 (Modrinth slug; author UNVERIFIED) | https://github.com/MattCzyr/NaturesCompass | 26.3 | modern Nature's Compass (author match UNVERIFIED) |
| 155 | Neat (Vazkii) | UNVERIFIED | not read | UNVERIFIED | CC-BY-NC-SA-3.0 | https://github.com/VazkiiMods/Neat | 26.2 | Neat 26.2 (Modrinth) |
| 156 | NetherEx (LogicTechCorp) | UNVERIFIED | Nether expansion (knowledge) | UNVERIFIED | MIT for code, some assets ARR (Modrinth LicenseRef) | https://github.com/LogicTechCorp/NetherEx | 26.1.2 | NetherEx 26.1.2 (Fabric, NeoForge) |
| 157 | No Recipe Book (Adorn) | UNVERIFIED | hides recipe book (knowledge) | UNVERIFIED | ARR (Modrinth slug; author UNVERIFIED) | https://github.com/ChongYu668/No-Recipe-Book | 26.3 | author mismatch possible |
| 158 | No Worldgen 5 You (DarkhaxDev) | UNVERIFIED | worldgen removal (name) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/No-Worldgen-5-You | 1.12.2 | none |
| 159 | Ocean Floor - Clay Sand and Dirt (Lothrazar) | UNVERIFIED | ocean floor blocks (knowledge) | UNVERIFIED | MIT | NONE | 26.1.2 | Modrinth ocean-floor-clay-sand-and-dirt (26.1.2) |
| 160 | OpenBlocks Elevator (vsngarcia) | 1.4.2 (3.1.2h, as Elevator Mod) | elevator blocks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 161 | Ore Excavation (Funwayguy) | UNVERIFIED | vein mining (knowledge) | UNVERIFIED | ARR | NONE | 1.21 | Ore Excavation 1.21 (Modrinth; author match UNVERIFIED) |
| 162 | Ore Excavation Integration (BloodWorkXGaming) | UNVERIFIED | integration (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 163 | Ore Prospectors (Lothrazar) | UNVERIFIED | prospectors (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 164 | Ore Stages (DarkhaxDev) | UNVERIFIED | disguises locked ores (README, section 3.4) | infrastructure (ore hiding per sources) | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Ore-Stages (archived 22 Feb 2024) | 1.12.2 | Ore Stages Reborn (LGPL-2.1-only, fabric and forge, 1.20.4); SDM Ore Stages (1.21+, rewrite; not found on Modrinth) |
| 165 | Overloaded (cjm721) | 0.0.57 (3.1.2h) | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | Modrinth "overloaded" is a different project (diamonddevv, CC0-1.0); not a match |
| 166 | Pickle Tweaks (BlakeBr0) | 2.1.3 (3.1.2h) | tweaks (name) | UNVERIFIED | MIT | https://github.com/BlakeBr0/PickleTweaks | 26.1.2 | Pickle Tweaks 26.1.2 |
| 167 | Placebo (Shadows_of_Fire) | 1.6.0 (3.1.2h) | library (knowledge) | UNVERIFIED | MIT | https://github.com/Shadows-of-Fire/Placebo | 26.1.2 | Placebo 26.1.2 |
| 168 | Player API (Forge_User_03353248) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 169 | Player Skin Mob (GenDeathrow) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 170 | PneumaticCraft: Repressurized (desht_08) | 0.11.1-361 (3.1.2h) | pneumatic machines (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/TeamPneumatic/pnc-repressurized | 1.21.1 | PneumaticCraft 1.21.1 (Modrinth) |
| 171 | Power Adapters (DarkhaxDev) | UNVERIFIED | power adapters (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Power-Adaptors | 1.12.2 | none |
| 172 | Prestige (Jaredlll08) | UNVERIFIED | prestige system (name; planned per CurseForge) | UNVERIFIED | LGPL-2.1-only | https://github.com/JarHax/Prestige | 1.12.2 | none |
| 173 | Primal Tech (vadis365) | 0.3.4 (3.1.2h) | primitive tech (knowledge) | UNVERIFIED (Age 0-1 likely) | UNVERIFIED | UNVERIFIED | UNVERIFIED | none found (not on Modrinth) |
| 174 | PrimalChests (Jaredlll08) | UNVERIFIED | chests (knowledge) | UNVERIFIED | LGPL-2.1-only | https://github.com/jaredlll08/PrimalChests | 1.12.2 | none |
| 175 | PrimalCore (An_Sar) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 176 | Progression Tweaks (TurkeyDev) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 177 | Proportional Destruction Particles (Phylogeny) | UNVERIFIED | particles (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 178 | QuantumStorage (Gigabit101) | UNVERIFIED | storage (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 179 | Quark (Vazkii) | r1.5-158 (3.1.2h) | quality-of-life and decor content (knowledge) | UNVERIFIED | CC-BY-NC-SA-3.0 | https://github.com/VazkiiMods/Quark | 1.21.1 | Quark 1.21.1 (Modrinth) |
| 180 | Quick Leaf Decay (Lumien231) | UNVERIFIED | leaf decay (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 181 | RFTools (McJty) | 7.71 (3.1.2h) | machines and dimensions (knowledge) | UNVERIFIED | MIT | https://github.com/McJty/RFTools | 1.12.2 | none on Modrinth beyond 1.12.2 |
| 182 | RFTools Control (McJty) | 2.0.2 (3.1.2h) | control blocks (knowledge) | UNVERIFIED | MIT | https://github.com/McJtyMods/RFToolsControl | 1.21.1 | RFTools Control 1.21.1 |
| 183 | RandomPatches (TheRandomLabs) | UNVERIFIED | patches (knowledge) | UNVERIFIED | MIT | https://github.com/TheRandomLabs/RandomPatches | 1.16.5 | none (Modrinth 1.16.5) |
| 184 | Ranged Pumps (raoulvdberge) | UNVERIFIED | pumps (knowledge) | UNVERIFIED | MIT | https://github.com/refinedmods/rangedpumps | 26.1.2 | Ranged Pumps 26.1.2 (refinedmods) |
| 185 | ReAuth (TechnicianLP) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 186 | Realistic Item Drops (Funwayguy) | UNVERIFIED | drop rules (knowledge) | UNVERIFIED | CC-BY-SA-4.0 (Modrinth slug; author UNVERIFIED) | https://github.com/silabear/realistic-item-drops | 1.21 | mismatch possible |
| 187 | Reborn Core (modmuss50) | 3.19.5 (3.2.3) | library (3.2.3 changelog update) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none found |
| 188 | Reborn Storage (Gigabit101) | 3.3.4.85 (3.1.2h) | storage (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none found |
| 189 | Recipe Stages (Jaredlll08) | UNVERIFIED | gates crafting recipes (section 3.3) | infrastructure | MIT | https://github.com/jaredlll08/RecipeStages | 1.20.1 | Recipe Stages 1.20.1 (Modrinth) |
| 190 | Recurrent Complex (Ivorius) | 1.4.8.2 (3.1.2h) | structure generation (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 191 | Redstone Flux (TeamCoFH) | UNVERIFIED | RF energy API (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 192 | Refined Storage (raoulvdberge) | 1.6.14 (3.1.2h) | storage network (knowledge) | UNVERIFIED | MIT (Modrinth refined-storage, RS2 repo) | https://github.com/refinedmods/refinedstorage2 (RS2; RS1 repo UNVERIFIED) | 26.1.2 | Refined Storage 2 (rewrite; not a direct port of RS1) |
| 193 | Refined Storage Addons (raoulvdberge) | 0.4.4 (3.1.2h) | addons (knowledge) | UNVERIFIED | MIT | https://github.com/refinedmods/refinedstorageaddons | 1.20.4 | RS addons 1.20.4 |
| 194 | Resource Loader (Lumien231) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 195 | Road Runner (Bamboo97) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 196 | Rustic (cadaverous_queen) | UNVERIFIED | cooking and food (knowledge) | UNVERIFIED | MIT | https://github.com/cadaverous-eris/Rustic | 1.12.2 | none |
| 197 | Scannable (Sangar) | UNVERIFIED | scanning (knowledge) | UNVERIFIED | MIT (Modrinth scannable, MightyPirates; author UNVERIFIED) | https://github.com/MightyPirates/Scannable | 1.21.11 | mismatch possible |
| 198 | SevPatches (RobertDropTable) | 1.7-8 (3.2.0) | pack-specific patches (name) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none |
| 199 | SevTweaks (artdude543) | 0.4.0-1 (3.2.0) | pack-specific tweaks (name) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none |
| 200 | Shadowfacts' Forgelin (ShadowfactsDev) | UNVERIFIED | Kotlin library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 201 | Simple Generators (Valoryk) | 2.0.20.2 (3.1.2h) | generators (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 202 | Simple Storage Network (Lothrazar) | 1.7.6 (3.1.2h) | storage network (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 203 | Simple Tomb (Lothrazar) | 1.0.0 (3.2.3, added) | tombstones (3.2.3 changelog; replaces Corail Tombstone) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | none checked |
| 204 | SimpleHarvest (TehNut) | UNVERIFIED | harvesting (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 205 | Simply Arrows (DarkhaxDev) | UNVERIFIED | arrows (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 206 | Smooth Font (bre2el) | 2.0 (3.1.2h) | font (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 207 | Spartan Shields (ObliviousSpartan) | 1.5.2 (3.1.2h) | shields (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 208 | SpatialCompat (MrDj200) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 209 | Steve's Carts Reborn (modmuss50) | 2.4.30.134 (3.1.2h) | carts (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 210 | Storage Drawers (Texelsaur) | UNVERIFIED | storage drawers (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 211 | Streams (delvr) | 0.4.7 (3.1.2h) | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 212 | Stuff A Sock In It (DarkhaxDev) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 213 | Super Sound Muffler: Revived (Firstnecron) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 214 | SwingThroughGrass (exidex) | UNVERIFIED | grass interaction (name) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 215 | TallGates (GoryMoon) | UNVERIFIED | gates (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 216 | Tesla Core Lib (face_of_cat) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 217 | The Beneath (Shinoow) | 1.5.1 (3.1.2h) | dimension (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 218 | The Twilight Forest (Benimatic) | UNVERIFIED | dimension; Age 2 exit requirement (section 2) | Age 2 exit (cited) | Code LICENSE file, assets ARR (GitHub README; not SPDX) | https://github.com/TeamTwilight/twilightforest | 1.20.6+ (GitHub Maven section; Modrinth slug not found) | modern Twilight Forest (GitHub README) |
| 219 | The Weirding Gadget (AtomicBlom) | 2.1.16 (3.1.2h) | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 220 | Thirsty Bottles (DarkhaxDev) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 221 | TinkerStages (DarkhaxDev) | UNVERIFIED | gates Tinkers' tools, parts, modifiers (section 3.7) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/TinkerStages (archived 6 Feb 2024) | 1.12.2 | none |
| 222 | Tinkers Construct (mDiyo) | 2.12.0.135 (3.1.2h) | tool and material system (knowledge); Melter gates Age 0 exit (section 2) | Age 0-2 (cited) | MIT | https://github.com/SlimeKnights/TinkersConstruct | 1.20.1 | Tinkers' Construct 1.20.1 (Modrinth) |
| 223 | Tinkers' Complement (KnightMiner) | 0.4.1 (3.1.2h) | Tinkers' addon (knowledge) | UNVERIFIED | MIT | https://github.com/KnightMiner/TinkersComplement | 1.12.2 | none |
| 224 | Tinkers' Tool Leveling (bonusboni) | UNVERIFIED | tool leveling (name) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Modrinth slug not found |
| 225 | TipTheScales (Jaredlll08) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 226 | Toast Control (Shadows_of_Fire) | 1.8.1 (3.1.2h) | toast notifications (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 227 | Together Forever (Buuz135) | 1.0.12-22 (3.1.2h) | team sync of advancements and GameStages (section 3.12) | infrastructure | ARR | NONE listed (Modrinth) | 26.2 (Modrinth; 1.20.1 per 9minecraft) | Together Forever 26.2 (Modrinth, Fabric and Forge) |
| 228 | Totemic (ljfa2) | 0.11.6 (3.1.2h) | totem tools (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 229 | Translocators 1.8.+ (covers1624) | UNVERIFIED | item transport (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 230 | Traverse Reforged (Prospector) | UNVERIFIED | biomes (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 231 | Triumph (bloodnbones) | 3.17.0 (3.1.2h) | advancement-tree curation (section 3.13) | infrastructure | UNVERIFIED | UNVERIFIED (not on Modrinth) | 1.12.2 (last update 10 Nov 2019) | none found |
| 232 | Trumpet Skeleton (jamieswhiteshirt_) | UNVERIFIED | mob (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 233 | Tumbleweed (bconv) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 234 | Ultimate Car Mod (henkelmax) | UNVERIFIED | vehicles (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 235 | Uppers (vadis365) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 236 | ValkyrieLib (ValkyrieofNight) | 2.0.20.1 (3.1.2h) | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 237 | ViesCraft Machines (Vies) | UNVERIFIED | machines (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 238 | Villager Trade Tables (Forge_User_41096743) | UNVERIFIED | trades (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 239 | Waddles (Girafi) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 240 | Waila Stages (DarkhaxDev) | UNVERIFIED | gates WAILA HUD and tooltip lines (section 3.8) | infrastructure | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/WailaStages | 1.12.2 | none (Jade is the modern HUD) |
| 241 | Wall-Jump! [FORGE] (genandnic) | 1.3.2 (3.2.0) | wall jumping (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 242 | WanionLib (WanionCane) | UNVERIFIED | library (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 243 | Water Control Extreme (Buuz135) | UNVERIFIED | water control (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 244 | Water Strainer (Ruuubi) | UNVERIFIED | strainer (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 245 | Wawla - What Are We Looking At (DarkhaxDev) | 2.5.273 (3.1.2h, as WAWLA) | HUD (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED (pack is 1.12.2) | Jade 26.3 (HUD successor, lineage UNVERIFIED) |
| 246 | Wild Crops (Kormic911) | UNVERIFIED | crops (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 247 | Wither Skeleton Tweaks (Shadows_of_Fire) | UNVERIFIED | tweaks (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 248 | Wopper (Ellpeck) | UNVERIFIED | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 249 | XNet (McJty) | 1.8.1 (3.1.2h) | network (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 250 | YNot (asiekierka) | 0.2.4 (3.1.2h) | not read | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 251 | Yoyos (jozufozu) | 1.3.0.21 (3.1.2h) | yoyo weapons (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 252 | ZenLoot (RobertDropTable) | UNVERIFIED | loot (knowledge) | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 253 | ZenStages (artdude543) | UNVERIFIED | ZenScript stage addon (name) | infrastructure | UNVERIFIED | UNVERIFIED | UNVERIFIED | none checked |
| 254 | [ANGRY PIXEL] The Betweenlands (MrCompost) | UNVERIFIED | dimension; Age 2 exit requirement (section 2) | Age 2 exit (cited) | UNVERIFIED | UNVERIFIED (CurseForge and GitHub slugs 404) | UNVERIFIED (pack is 1.12.2) | none found |

### 4.1 Counts (generated from the table above)

Counted from the table above (awk over the 254 mod rows):

| Measure | Count (of 254) | Notes |
|---|---|---|
| Licence: open source (OSI, or CC-BY, CC-BY-SA, CC0, Unlicense) | 72 | Includes MIT, LGPL, GPL, BSD. Per Modrinth or GitHub metadata. |
| Licence: All Rights Reserved (ARR) | 13 | Per Modrinth. Some ARR rows are flagged "author match UNVERIFIED" (No Recipe Book, Ore Excavation). |
| Licence: CC-BY-NC-SA (source-available, not OSI) | 4 | Hwyla, Nature's Compass, Neat, Quark. Per Modrinth. |
| Licence: CC-BY-ND (not OSI) | 1 | To the Bat Poles! (slug match only). |
| Licence: LicenseRef (non-SPDX) | 3 | AE2, AutoRegLib, Better Advancements. |
| Licence: other or mixed | 1 | The Twilight Forest (code LICENSE file, assets ARR; not SPDX). |
| Licence: UNVERIFIED | 160 | Not on Modrinth or GitHub checked. Immersive Engineering, Astral Sorcery and Triumph have LICENSE files or are unnamed in sources read. |
| Source repo URL present | 85 | Modrinth source_url or GitHub. |
| Latest MC >= 1.20 (Modrinth or GitHub) | 67 | Some are author-mismatch flags (see table). |
| Latest MC UNVERIFIED | 155 | Includes "pack is 1.12.2" rows, which are not verified as latest. |
| Version in pack named in a changelog read | 93 | 3.1.2h, 3.1.7, 3.2.0 or 3.2.3. |
| Version in pack UNVERIFIED | 161 | Not named in any changelog read. The exact 3.2.3 jar version is UNVERIFIED for all rows. |
| Age cited to a source | 6 | Tinkers Construct (0-2), Ceramics (1), Astral Sorcery (2), Betweenlands (2), Twilight Forest (2), Applied Energistics 2 (4). Other ages are UNVERIFIED, inferred or infrastructure. |

Mods named in the 3.1.2 or later changelogs but not in the 254-entry 3.2.3 relations list: ArcFurnacePatch, JustEnoughIDsIntegration, Advanced Mortars, Galacticraft Core, Galacticraft Planets, MicdoodleCore, Nutrition, PlayerBosses, Corail's Tombstone (removed 3.2.3), AppleCore, Caliper, Sampler, World Stripper (removed 3.1.2).

---

## 5. Modern staging tools (1.20.1, 1.21.x, latest)

**Latest Minecraft.** Java Edition latest release is **26.3 "Wilderness Bound"**, released 15 Sep 2026, requiring Java SE 25 (https://minecraft.wiki/w/Java_Edition_26.3, fetched; https://mcreference.com/latest, search result). Version numbering is year-based (26.x), not 1.2x. Snapshots 26.4 are in test (UNVERIFIED beyond the search result). The 1.21.x line is no longer the latest.

| Tool | Role | Loaders | Latest MC (source) | License | Source | Notes |
|---|---|---|---|---|---|---|
| Game Stages (Darkhax) | Per-player stage API | Forge (Modrinth); NeoForge tag on CurseForge | 1.20.3 (Modrinth); 1.20.1 15.0.2 (CurseForge, Oct 2024) | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/Game-Stages | No 26.x build found. |
| Item Stages (Darkhax) | Item and block gating | Forge | 1.20.3 (Modrinth) | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/ItemStages | Modern build exists. |
| Recipe Stages (Jaredlll08) | Recipe gating | Forge | 1.20.1 (Modrinth) | MIT | https://github.com/jaredlll08/RecipeStages | |
| Ore Stages Reborn (SagaDeoMissTeam) | Ore disguise | Fabric, Forge | 1.20.4 (Modrinth) | LGPL-2.1-only | https://github.com/SagaDeoMissTeam/Ore-Stages-Reborn | Port of Ore Stages. |
| SDM Ore Stages | Ore disguise rewrite (1.21+) | UNVERIFIED | 1.21+ (search summary) | UNVERIFIED | https://www.curseforge.com/minecraft/mc-mods/ore-stages-reborn (CurseForge page) | Modrinth slug not found. Supports CraftTweaker and KubeJS (search summary). |
| Dimension Stages (Darkhax) | Dimension access | Forge | 1.19.2 (Modrinth) | LGPL-2.1-only | https://github.com/Darkhax-Minecraft/DimensionStages | No 1.20+ build found. |
| Mob Stages / TinkerStages / Waila Stages / JourneyMapStages (Darkhax) | Mob spawn, TiC, HUD, JourneyMap | Forge | 1.12.2 (Modrinth) | LGPL-2.1-only | respective Darkhax GitHub repos | No modern build found on Modrinth. |
| AStages (Alessandro-Casale) | Stage system with containers, blocks and permissions unlock on stage | Forge, NeoForge | 1.20.1, 1.21.1 (Modrinth); 2.0.0 (1.20.1, Apr 2026; search summary) | ARR (Modrinth) | https://github.com/Alessandro-Casale/AStages; https://www.curseforge.com/minecraft/mc-mods/astages | Modrinth licence is ARR despite public GitHub repo (verify against repo LICENSE, UNVERIFIED). Offline stage handling and presets in 2.0.0 (search summary). |
| AStages FTB Quests bridge | Quest rewards grant or remove stages (AStages Player and AStages Server rewards) | Forge, NeoForge | 1.20.1, 1.21.1 (Modrinth); 2.1.0 (1.20.1, 7 Jul 2026) | ARR | https://github.com/Alessandro-Casale/AStagesAddons; https://github.com/Alessandro-Casale/AStages/wiki/AStages-Addon-for-FTB-Quests | Stage names are case-sensitive. FTB's default Stage reward may write to KubeJS stages when KubeJS is present (search summary; UNVERIFIED). |
| FTB Quests (NeoForge) | Team-based quests | NeoForge | 26.1.2 (CurseForge file 26.1.2.9, uploaded 7 Oct 2026) | All Rights Reserved | https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge | 226,261,285 downloads (CurseForge). Modrinth slug not found. |
| FTB Teams | Team and stage-per-team base | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Not fetched. |
| KubeJS | Scripting; built-in Game Stages hooks (`.stages` add, remove, has) | Fabric, Forge, NeoForge, Quilt | 26.1.2 (Modrinth) | LGPL-3.0-only | https://github.com/KubeJS-Mods/KubeJS; https://kubejs.com/wiki/addons/gamestages | 1.20.4, 1.21, 1.21.1, 26.1.2 on Modrinth. |
| ProbeJS | Type-hint and doc generation for KubeJS addons | Fabric, Forge, NeoForge | 1.21.1 (Modrinth) | LGPL-2.1-only | https://github.com/Prunoideae/ProbeJS | Syntax help for addons (JStages page suggests it; search summary). Does not document AStages specifically (search summary: no source found). |
| Item Obliterator (nvb-uy) | Modpack utility: remove items or recipes, regex and NBT support | Fabric, Forge, NeoForge, Quilt | 1.21.1 (Modrinth) | LicenseRef-AGNYA-License (not an OSI licence; restricts redistribution and monetization) | https://github.com/nvb-uy/item_obliterator | Behaviour details not read. |
| JEI (mezz) | Recipe viewer; hides items for stages | Fabric, Forge, NeoForge | 26.3 (Modrinth) | MIT | https://github.com/mezz/JustEnoughItems | JEI runtime hide API not read (UNVERIFIED). |
| CraftTweaker (CraftTweaker) | ZenScript scripting for stage configs | Fabric, Forge, NeoForge | 1.21.1 (Modrinth; last three versions 1.20.6, 1.21, 1.21.1) | MIT | https://github.com/CraftTweaker/CraftTweaker | Modrinth may lag any 26.x build (UNVERIFIED). |
| Together Forever (Buuz135) | Team sync of advancements and GameStages | Fabric, Forge | 26.2 (Modrinth); 1.20.1 (9minecraft) | ARR | https://api.modrinth.com/v2/projects?ids=["together-forever"] | Modrinth lists no source URL. |
| Better Advancements | Advancement GUI | Fabric, Forge, NeoForge | 26.3 (Modrinth) | LicenseRef-Dont-Be-a-Jerk | https://github.com/way2muchnoise/BetterAdvancements | Triumph's author recommends it (search summary). |
| Jade (Snownee) | HUD successor to HWYLA and WAWLA | Fabric, Forge, NeoForge, Quilt | 26.3 (Modrinth) | CC-BY-NC-SA-4.0 | https://github.com/Snownee/Jade | Stage hook UNVERIFIED. |
| Modular Machinery Reborn | Multiblock machines (fork) | NeoForge | 1.21.1 (Modrinth) | LGPL-3.0-only | https://github.com/alec016/modular-machinery-reborn | Fork of the 1.12 original (not the same author). |
| Ages Reimagined | Pack, NeoForge, "modernized" (search summary) | NeoForge | UNVERIFIED | UNVERIFIED | https://moddex.gg/modpack/ages-reimagined (search result; no reviews) | Not by darkosto per sources read. Relationship to SevTech UNVERIFIED. |
| Found Enough Items (1.12.2) | Hides items and recipes together (search summary) | Forge | 1.12.2 | UNVERIFIED | https://www.modpackindex.com/mod/63090/found-enough-items | Warns of performance cost. |
| Teams Stages | Stages attached to FTB team (search summary) | UNVERIFIED | UNVERIFIED | UNVERIFIED | https://www.curseforge.com/minecraft/mc-mods/teams-stages/install | Requires FTB Teams. |

**Triumph modern status.** Not on Modrinth. No modern port found. Only a 1.12.2 version named (3.17.0 in the pack). Last update 10 Nov 2019 per the search summary.

---

## 6. Design lessons (player and reviewer reports)

**Reviews (sources read).**
- **MMC Reviews** (https://mmcreviews.com/all/modpacks/sevtech-ages/, search summary): average 4.1 from 9 reviews. The April 2026 long review (also on Moddex) says the early game is "very solid", with a unique start (sticks and stones from the ground). It calls Ages 0 and 1 enjoyable and says the pack falls off in the late game. Overall verdict: a fun pack for tech players, with less exploration.
- **Moddex** (https://moddex.gg/modpack/sevtech-ages/reviews/1068): scores gameplay 4.0, aesthetics 2.0, performance 3.0, overall 4.0. Headline: great, but the final age is not worth playing. The reviewer took about 262 hours (about 15 AFK) and says they cheated two singularities to finish. A May 2024 Moddex review (about 100 hours) liked the early-to-mid stages most and found it more tedious toward the end.
- **Betweenlands and Twilight Forest:** the April 2026 review calls the Betweenlands trip a chore: dark, hard to navigate, for a couple of gems for a smeltery controller. It says both dimensions feel unnecessary and disconnected from the pack theme. Sulfur torches in the Betweenlands are the one thing credited (no relighting). Gear restrictions by age softened the pacing impact.
- **Late game:** multiple summaries put the late-game tedium in the endgame, not Age 0.

**Community signals (sources read).**
- No Reddit thread was found. A search for Reddit threads returned none (search budget then ran out; the Reddit check could not be repeated). Reddit sentiment is UNVERIFIED.
- GitHub `DarkPacks/SevTech-Ages`: 458 open issues, 1 open PR (fetched). Issues are for bugs and feature requests only; gameplay questions go to Discord (README).
- Server cost: CreeperHost lists a 4000 MB minimum for servers (search result).

**Maintenance signals (facts).**
- Last SevTech file: 3.2.3, 17 Dec 2021. As of 2026-10-11, no later file on CurseForge (fetched file list).
- The pack's 3.2.x changelogs carry few content changes: 3.2.0 updated ExtraPlanets, Galacticraft, MJRLegendsLib, SevPatches, SevTweaks, Wall-Jump! (no new content); 3.2.1 and 3.2.2 changed bug fixes and translations or security only.
- Prestige system and "extra end-game content" were promised on the CurseForge page. No file with these was found after 2021.

**Successor status.**
- No "SevTech 2" or Darkosto successor announcement was found in the extended search run before the budget ran out. The second check could not be run.
- SevTech: Ages of the Sky (1.12.2 skyblock variant, same mod core per search summary) is the only named sibling pack (https://www.curseforge.com/minecraft/modpacks/sevtech-ages-of-the-sky).
- Darkosto's CurseForge profile shows newer unrelated mods dated 2026 (fetched: "Junk Drawers", "Underground Villages").

---

## Sources

- https://www.curseforge.com/minecraft/modpacks/sevtech-ages (project; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files (file list; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3569915 (3.2.3 changelog; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3569915/dependencies?page=1..13 (relations, 254 entries; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/2752761 (3.1.2-hotfix.1 changelog; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/2744522/dependencies (3.1.2 relations and changelog; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3047439 (3.1.7; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3096770 (3.2.0; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3123514 (3.2.1; fetched)
- https://www.curseforge.com/minecraft/modpacks/sevtech-ages/files/3559141 (3.2.2; fetched)
- https://mineyourmind.readthedocs.io/en/latest/Modpacks_Server/1.12.2/sevtech_ages.html (243-name older list; fetched)
- https://wiki.9minecraft.net/sevtech-ages-modpacks/ (age exits; fetched via redirect of https://www.9minecraft.net/sevtech-ages-modpacks/wiki/)
- https://www.holy.gg/de/post/sevtech-ages-server-setup-guide (search result summary of ages)
- https://www.holy.gg/post/como-instalar-y-jugar-sevtech-ages-en-un-server-de-minecraft (search result summary; Age 2 coal engine)
- https://sevtechages.gamepedia.com/How_to_play_guide_for_SevTech:_Ages (search result snippet; "Unfamiliar Item"; L key)
- https://sevtechages.fandom.com/wiki/Mod_List (HTTP 402 on fetch)
- https://github.com/DarkPacks/SevTech-Ages (repo and README; fetched)
- https://raw.githubusercontent.com/DarkPacks/SevTech-Ages/master/original/docs/mods/triumph/Triumph.txt (fetched)
- https://github.com/Darkhax-Minecraft/Game-Stages/blob/1.12.2/README.md (fetched)
- https://github.com/Darkhax-Minecraft/ItemStages/blob/1.12.2/README.md (fetched)
- https://github.com/Darkhax-Minecraft/Ore-Stages (fetched)
- https://docs.blamejared.com/1.18/en/mods/RecipeStages/RecipeStages (fetched)
- https://github.com/Darkhax-Minecraft/DimensionStages (fetched)
- https://github.com/Darkhax-Minecraft/MobStages (fetched)
- https://github.com/Darkhax-Minecraft/TinkerStages (fetched)
- https://github.com/Darkhax-Minecraft/WailaStages (fetched)
- https://github.com/Darkhax-Minecraft/JourneyMapStages (fetched)
- https://github.com/nvb-uy/item_obliterator (fetched; about text only)
- https://kubejs.com/wiki/addons/gamestages (fetched)
- https://api.modrinth.com/v2/projects?ids=[...] and https://api.modrinth.com/v2/project/{kubejs,game-stages,astages} (fetched, batches listed in section 4 and 5)
- https://github.com/TeamGalacticraft/Galacticraft-Legacy (fetched; MIT, 1.12.2)
- https://github.com/micdoodle8/Galacticraft (fetched; license not shown)
- https://github.com/HellFirePvP/AstralSorcery (fetched; 1.21.1 branch; license not shown)
- https://github.com/BluSunrize/ImmersiveEngineering (fetched; 1.21.1 branch; license not shown)
- https://github.com/TeamTwilight/twilightforest (fetched; README 1.12 and beyond, Maven 1.20.6+)
- https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge (fetched)
- https://minecraft.wiki/w/Java_Edition_26.3 (fetched)
- https://www.9minecraft.net/?p=182778 (Together Forever 26.1.2 and 1.20.1 listing; fetched title only)
- https://mmcreviews.com/all/modpacks/sevtech-ages/ (search summary)
- https://moddex.gg/modpack/sevtech-ages/reviews/1068 (search summary and page text)
- https://www.boxtoplay.com/en/minecraft-hosting/minecraft-server/curseforge-modpacks/modpack-sevtech-ages-28 (search summary)
- https://www.creeperhost.net/modpack/c268208-sevtech-ages (search summary)
- https://craftane-main.onrender.com/mods/Triumph (search summary; Triumph description and 2019 update)
- https://metamods.net/en/mods/together-forever (search summary; team sync)
- https://www.curseforge.com/minecraft/mc-mods/teams-stages/install (search summary)
- https://www.modpackindex.com/mod/63090/found-enough-items (search summary)
- https://github.com/Darkhax-Minecraft/Ore-Stages (fetched)
- https://github.com/SagaDeoMissTeam/Ore-Stages-Reborn (Modrinth metadata; repo not fetched)
- https://github.com/Alessandro-Casale/AStages (Modrinth metadata; repo not fetched)
- https://github.com/Alessandro-Casale/AStagesAddons (Modrinth metadata; repo not fetched)
- https://github.com/Alessandro-Casale/AStages/wiki/AStages-Addon-for-FTB-Quests (search summary)
- https://github.com/KubeJS-Mods/KubeJS/issues/821 (search summary)
- https://www.curseforge.com/minecraft/mc-mods/ore-stages-reborn (search summary; SDM Ore Stages)
- https://moddex.gg/modpack/ages-reimagined (search summary)
