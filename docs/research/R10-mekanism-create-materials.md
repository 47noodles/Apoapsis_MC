# R10 - Mekanism and Create: materials, versions, licences, precedents

Research node R10 for Apoapsis. Date of research: 2026-10-11. Facts only; no design decisions.

Legend
- [S#] = source index at the end of this file. Every non-obvious fact cites one.
- VERIFIED = read from a primary page (repo file, official wiki, store page) in this session.
- PARTIAL = read from a secondary page, a search snippet, or a third-party listing; treat as indicative.
- UNVERIFIED = not confirmed by any source I could reach. Do not build on these without checking.

Access notes
- create.fandom.com returned HTTP 402 for every page; no Fandom Create content is used.
- Several wiki.aidancbrady.com pages returned 404 (Boiler, Infuse_Types, Slurries, Pigments, Fluorite_Ore); Fluorite was read from the "Fluorite" page instead.
- Some GitHub paths returned 404/503/429 (noted where relevant). Mekanism's own tag and generated-data paths were not reachable.

---

## 0. Snapshot (2026-10-11)

- Latest Minecraft Java release: 26.3 "Wilderness Bound", released 2026-09-15 [S98]. 26.2 finished 2026-06-16 per one wiki source [S99].
- Mekanism: latest official build is 10.7.19.85 for Minecraft 1.21.1 NeoForge, uploaded 2026-04-10 and marked "Latest" [S3][S4]. No official 26.x release found [S4]. The GitHub repo page resolved to branch "26.3", whose README is titled "Mekanism for Minecraft 26.3" [S2]; treat that branch as in-development.
- Create: latest official build is 6.0.10 for Minecraft 1.21.1 NeoForge, uploaded 2026-04-21 [S52]. Create wiki: 26.1 status "In Progress"; quote: "We are currently working on a port to 26.1; stay tuned for updates!" [S55]. The wiki also says "Sadly, we cannot give out ETA's for future releases." [S55]

---

## 1. MEKANISM

### 1.1 Modules, versions, licence

| Module | Latest verified build | Minecraft (per listing) | Loader | Licence | Source |
|---|---|---|---|---|---|
| Mekanism (core) | 10.7.19.85, 1.21.1, uploaded 2026-04-10 | 1.21-1.21.1, 1.20.4, 1.20.1, 1.19.x, 1.18.2, 1.16.x, 1.15.x, 1.12.x, 1.11.2, 1.10.2, 1.7.10 (Modrinth) | Forge and NeoForge (Modrinth); 1.21.1 builds are NeoForge | MIT | [S1][S2][S3][S4][S7] |
| Mekanism Generators | Same repo; build number UNVERIFIED | 1.21-1.21.1 and older | Forge and NeoForge | MIT | [S9] |
| Mekanism Additions | Same repo; build number UNVERIFIED | 1.21-1.21.1 and older | Forge and NeoForge | MIT | [S10] |
| Mekanism Tools | Same repo; build number UNVERIFIED | 1.21-1.21.1 and older | Forge and NeoForge | MIT | [S11] |

Module content
- Generators: Heat Generator, Gas-Burning Generator, Bio-Generator, Solar Generator, Advanced Solar Generator, Wind Generator, Fission Reactor, Fusion Reactor, Turbine [S9].
- Additions: "Additions module for Mekanism, contains things that don't quite fit in the other modules." Adds baby mobs, obsidian TNT, glow panels, balloons, plastic block variants, a voice server [S10].
- Tools: "Powerful tools and armor for Mekanism." Armour, shields and tools for Bronze, Lapis Lazuli, Osmium, Refined Glowstone, Refined Obsidian, Steel; includes Paxels [S11].

Licence quotes
- LICENSE file (branch 1.21.x): "MIT License", "Copyright (c) 2017-2025 Aidan C. Brady", followed by the standard MIT grant text [S1].
- README: "Mekanism is licensed under the MIT license." and "You may use it in modpacks, reviews, or any other form as long as you abide by the terms below." [S2]
- Asset licence: none stated separately. README credits artists (CyanideX, Ridanisaurus, Cheapshot, Archadia) with no separate terms [S2]. I found no exclusion of assets from MIT. Treat assets as MIT unless the author says otherwise (exclusion absence UNVERIFIED against every file).

Minecraft version status for Mekanism
- 1.21.1 NeoForge: official line. Recent builds: 10.7.19.85 (2026-04-10), 10.7.18.84 (2026-02-14), 10.7.17.83 (2025-10-21), 10.7.16.82 (2025-09-21), 10.7.15.81 (2025-08-17) [S3][S4].
- 1.20.1 Forge: last official build seen 10.4.16.80 (2025-05-21) [S3][S4].
- 26.x: no official build found. On 2026-07-12 a maintainer (pupnewfster) answered "Yes" to "Is in your plan to update the mod to 26.2?" with no timeline; the issue was closed the same day; comments were locked 2026-10-06 [S8].
- Community, unofficial "Version Locked" ports exist for Minecraft 26.1.2 on NeoForge (fan-made; they share the Mekanism mod ID, so official and port jars must not be installed together) [S102][S103]. One port maintainer says addons need "Mekanism 10.8.0" for 26.1.2; no official 10.8.0 was found, so 10.8.0 is UNVERIFIED [search-only].
- A developer's Iris bug report says the 26.1 branch reworked renderers and changed compat targets for MekaSuit/SPS/Flame pseudo-mixins [S101] (third-party).

### 1.2 Port history, 1.20.1 -> 1.21.1

- First public 1.21 alpha seen: Mekanism-1.21-10.6.5.52.jar, uploaded 2024-07-11, NeoForge, release type Alpha. Changelog warning: "Warning: Mekanism is currently in alpha, and is not recommended for widespread use in modpacks." [S5]
- 10.6.7.54 (1.21, alpha) listed 2024-07-28 and 10.7.0.55 (1.21.1, alpha) listed 2024-08-20 on Modrinth/CurseForge [S108][S109] (PARTIAL: search snippets).
- Mekanism-1.21.1-10.7.9.72.jar uploaded 2025-02-25, still labelled Alpha with the same warning [S6].
- Earliest build seen labelled R (release) on the CurseForge list: 10.7.15.81, 2025-08-17 [S3]. Builds 10.7.10 to 10.7.14 were not checked, so the exact switch date is UNVERIFIED.
- 1.20.1 remained maintained after the 1.21 alpha started (10.4.16.80, 2025-05-21) [S3].
- Elapsed time: about 13 months from first public 1.21 alpha (2024-07-11) to the first release-labelled 1.21.1 build I saw (2025-08-17). When the 1.21 port started is UNVERIFIED.

### 1.3 Ores, raw materials and world generation

Stock world-generation defaults were not verifiable. The table below separates what is documented from one modpack's config (not stock).

| Material | Items named by wiki | Stock world-gen (documented) | Example pack config (lytedev jland pack, NOT stock) [S19] |
|---|---|---|---|
| Copper | Copper Ore, Copper Ingot, Copper Dust [S16] | Y range UNVERIFIED | Not in the pack excerpt |
| Tin | Tin Ore, Tin Ingot, Tin Dust [S15] | Y range UNVERIFIED | small: 14 veins/chunk, size 4, Y -20 to 94; large: 12/chunk, size 9, Y -32 to 72 |
| Osmium | Osmium Ore, Osmium Ingot, Osmium Dust [S13] | "at nearly any height"; generation "similar ... as iron, but at a lower world height"; "between 12-36 for best results"; no retro-generation [S13] | upper: 65/chunk, size 7, anchors ABSOLUTE 72 to BELOW_TOP -24 (i.e. 72 up to top-24); middle: 6/chunk, size 9, Y -32 to 56; small: 8/chunk, size 4, Y bottom+0 to 64 |
| Uranium | Uranium Ore, Raw Uranium, Uranium Ingot, Yellow Cake Uranium [S14] | "It spawns from Y level 69 down, and most commonly around Y level 42." [S14] | small: 4/chunk, size 4, Y bottom+0 to 8; buried: 7/chunk, size 9, anchors ABOVE_BOTTOM -24 to ABOVE_BOTTOM 56 (raw values) |
| Lead | Lead Ore, Lead Ingot, Lead Dust [S17] | Y range UNVERIFIED; "spawning slightly less frequently than iron" [S17] | normal: 8/chunk, size 9, anchors ABOVE_BOTTOM -24 to ABSOLUTE 64 (raw values) |
| Fluorite | Fluorite (raw and dust) [S18] | Y range UNVERIFIED; "Fluorite is a material found in the world." [S18] | normal: 5/chunk, size 5, Y bottom+0 to 23; buried: 3/chunk, size 13, Y bottom+0 to 4 |
| Salt | "Salt Block" listed under Ores [S12] | UNVERIFIED | salt: 2/chunk, radius 2-3, halfHeight 1 |

Other listed raw forms (wiki Ores page): Dusts, Dirty Dusts, Clumps, Shards, Crystals, Slurry [S12].

Notes on the pack config [S19]
- Commit e4442c458e4dbfb070152827a60cb2511a633029. Header: "World generation settings for Mekanism. This config is synced from server to client". No Mekanism version stated.
- Top-level shouldGenerate is false for tin, osmium, uranium and lead, while the sub-veins are true. The file's comments say sub-veins require the parent enabled, so the file is internally inconsistent.
- Anchor types used: ABSOLUTE, ABOVE_BOTTOM, BELOW_TOP. Config setting enableRegeneration = false.
- Stock values: launch Mekanism once with the target version to generate config/Mekanism/world.toml; the source says so only indirectly [S19]. UNVERIFIED for any stock number above.
- Datapacks such as "Distant Ores" remove default generation and replace it with distance-based generation (search snippet) [search].

### 1.4 Ore processing tiers (2x / 3x / 4x / 5x)

Wiki tier numbering is Tier 0 to Tier 4 with yields x1 to x5 [S20]. Guides use the 2x-5x names [S21].

| Tier (yield) | Machines (added at this tier) | Chemicals | Chain (one line) |
|---|---|---|---|
| Tier 0 (x1) | Energized Smelter | none | Raw ore -> ingot [S20] |
| Tier 1 (x2) | Enrichment Chamber | none | Ore -> 2 dust (Enrichment) -> 2 ingots (Energized Smelter) [S20] |
| Tier 2 (x2 raw / x3 silk-touch) | Purification Chamber, Crusher | Oxygen | Ore + O2 -> clumps (Purification) -> dirty dust (Crusher) -> clean dust (Enrichment) -> ingots [S20][S21] |
| Tier 3 (x4) | Chemical Injection Chamber, Chemical Infuser, Thermal Evaporation Plant (plus Tier 2) | Hydrogen Chloride, Oxygen | Silk-touch ore + HCl -> 4 shards; + O2 -> 4 clumps -> dirty -> dust -> 4 ingots [S20] |
| Tier 4 (x5) | Chemical Dissolution Chamber, Chemical Washer, Chemical Crystallizer, Rotary Condensentrator, Chemical Oxidizer (plus earlier) | Sulfuric Acid, Water, Hydrogen Chloride, Oxygen, Sulfur Dioxide, Sulfur Trioxide, Water Vapor | Ore + sulfuric acid -> dirty slurry; + water (Washer) -> clean slurry; (Crystallizer) -> crystals; then the Tier 3 chain [S20] |

Details and caveats
- Tier 4 step counts are not given per step; the x5 yield comes from the comparison table [S20].
- Hydrogen chloride is made in a Chemical Infuser from hydrogen and chlorine; chlorine is said (guide) to come from brine via a Thermal Evaporation Plant [S21]. The Gases page says chlorine comes from the Electrolytic Separator using brine [S22]. These conflict; UNVERIFIED which is the intended route.
- Tier 4 needs sulfur, obtained from gunpowder, gravel or flint in the wiki text [S20].
- Silk-touch variants and "raw" variants both appear in the wiki text; yield figures differ by page.

### 1.5 Chemicals

Four families per the wiki and an unofficial overview: gases, infuse types, pigments, slurries [S23][S110].

Gases (Chemical Tanks page, 24 entries) [S23]: Antimatter, Chlorine, Deuterium, D-T Fuel, Ethylene (spelled "Ehtylene" on page), Fissile Fuel, Gaseous Brine, Hydrofluoric Acid, Hydrogen, Hydrogen Chloride, Liquid Osmium, Lithium, Nutritional Paste, Oxygen, Sodium, Steam, Sulfur Dioxide, Sulfur Trioxide, Sulfuric Acid, Superheated Sodium, Tritium, Uranium Hexafluoride, Uranium Oxide, Water Vapor.

Gases (Gases page, 19 entries, adds production route) [S22]: Hydrogen (Electrolytic Separator, water); Polonium (Solar Neutron Activator, nuclear waste); Fissile Fuel (Isotopic Centrifuge, hydrofluoric acid); Nuclear Waste (Fission Reactor, fissile fuel); Spent Nuclear Waste (PRC, polonium + water + fluorite dust); Oxygen (Electrolytic Separator, water; flint optional on some machines); Chlorine (Electrolytic Separator, brine); Hydrogen Chloride (Chemical Infuser, H2 + Cl2); Sulfur Dioxide (Chemical Oxidizer, sulfur); Sulfur Trioxide (Chemical Infuser, O2 + SO2); Gaseous Brine (Chemical Oxidizer with salt, or Rotary Condensentrator with brine); Water Vapor (Rotary Condensentrator, water); Sulfuric Acid (Chemical Infuser, SO3 + water vapor); Ethylene (PRC, water + hydrogen); Deuterium (Electrolytic Separator, heavy water); Lithium (Rotary Condensentrator, liquid lithium); Tritium (Solar Neutron Activator, lithium); D-T Fuel (Fusion Reactor or Chemical Infuser, deuterium + tritium); Sodium (Electrolytic Separator, brine).

Infuse types (8) [S23]: Biomass, Carbon, Diamond, Funghi, Gold, Redstone, Refined Obsidian, Tin.

Slurries (14: dirty and clean for each) [S23]: Copper, Gold, Iron, Lead, Osmium, Tin, Uranium.

Pigments: no list found. UNVERIFIED. The wiki Chemical Tanks page lists none [S23]; a 1.16-era CraftTweaker page shows a two-pigment mixing recipe [search only]. Not used.

1.21 chemical system: I found no source stating that the four families were merged into one "chemical" type in 1.21. UNVERIFIED either way. The unofficial DeepWiki overview describes them as four custom resource types (gases, infusions, pigments, slurries) [S110].

Production routes for other chemicals (from the same wiki pages)
- Polonium pellet: Pressurized Reaction Chamber (PRC): 1 Fluorite Dust + 1000 mB Water + 1000 mB Polonium -> Polonium Pellet + 1000 mB Spent Nuclear Waste [S43].
- Antimatter pellet: Chemical Crystallizer from 1,000 mB Antimatter [S45].
- Antimatter gas: Supercritical Phase Shifter from Polonium [S32].
- Ethylene for HDPE: PRC from water + hydrogen [S22].

### 1.6 Electrolytic Separator

- Inputs/outputs per wiki [S22][S24]: water -> hydrogen + oxygen; brine -> chlorine (and other gases, unnamed on the page); heavy water -> deuterium (+ oxygen per Gases page). Gases go to internal tanks; a full tank stops the machine; a Pressurized Tube or "dump" removes gas [S24].
- Energy: "requires 400 joules of power per tick, or 160 FE per tick" (introduced in Mekanism 7.1, per the page) [S24].
- Speed: 1 operation per tick base; each speed upgrade doubles speed; maximum 256 operations per tick with 8 speed upgrades; energy upgrades do not change efficiency ("for balancing purposes") [S24].
- mB per operation, mB/t rates, and per-gas tank size: UNVERIFIED. The wiki gives none. A search snippet gives 10,000 mB liquid storage (FTB wiki) [search only].
- Recipe structure (GroovyScript docs): one fluid input and exactly two gas outputs, with a separate energy cost [search: cleanroommc.com]. Use the in-game JEI or the CraftTweaker command /ct mekrecipes separator (Mekanism 9.7.0+, per search snippet) to get exact amounts. Not run.

### 1.7 Energy units

- Wiki: "1 FE = 2.5 J = 0.25 EU"; "0.4 FE = 1 J = 0.1 EU"; "4 FE = 10 J = 1 EU"; "RF is completely equivalent to FE." [S25]
- Prefixes listed in order: f, p, n, µ, m, (none), k, M, G, T, P, E, Z, Y; each step 1000x [S25]. The page gives no kFE/MFE definitions; kFE = 1,000 FE and MFE = 1,000,000 FE is my inference from the prefix rule, not stated.
- Units used by multiblock pages are not consistent: Electrolytic Separator uses J/t and FE/t [S24]; Fusion uses MFE/t [S26]; SPS uses MFE/t only [S32]; Induction Matrix pages refer to MFE/t [S31].

### 1.8 Multiblocks and outputs

| Multiblock | Function and I/O | Size | Outputs and key numbers | Source |
|---|---|---|---|---|
| Fission Reactor | Burns fissile fuel; "does not produce power on its own"; power comes from an Industrial Turbine fed by heated coolant | min outside 3x4x3, max 18x18x18; at least 4 Fission Reactor Ports | Burn rate starts 0.1 mB/t, max 1 mB/t per Fission Fuel Assembly. At 1 mB/t burn, heating is 20,000 mB/t (water) or 200,000 mB/t (sodium). Outputs: heated coolant, Nuclear Waste (convertible to Polonium, Plutonium or Antimatter pellets) | [S27] |
| Industrial Turbine | Steam in (no fresh-water input listed); condensed water exits through turbine vents | min 5x5 base, 5 high; max 17x17 base, 18 high | Energy: "FLOWxROTOR_HEIGHTx50RF/7" (time unit not stated; presumably per tick). Each vent 32,000 mB/t; each pressure disperser 1,280 mB/t per interior block. No fixed max power given | [S28] |
| Fusion Reactor | Fuel: deuterium and tritium supplied separately or as D-T Fuel; consumption per fuel = injection rate / 2 per tick | Size not stated | Highest injection stated 98 mB/t. Separate D/T at 98 mB/t: "around 20 MFE/t". D-T Fuel: "around 200 MFE/t". Water cooling produces steam; ignition threshold 100 MK. D-T tank holds 1000 mB by default. Page warns D-T supply must exceed 1000 mB/t | [S26] |
| Thermal Evaporation Plant | Converts water to Brine, or Brine to Lithium | min 34 evaporation blocks + controller + valve + resistive heater; max 213 blocks + 2 valves + 4 Advanced Solar Generators; height up to 18 | Production = 0.4 x (Height/18) x min(Temp - 300, 3000), Temp in K. Smallest setup: 180 mB/t at max heat, rising to 1080 mB/t at max height. Pipes must be set to Pull with a Configurator | [S29] |
| Thermoelectric Boiler | Makes steam from external heat; best with superheated sodium from a sodium-cooled Fission Reactor (resistive heaters also possible) | Page gives min 3x3x4 in one place and 3x4x3 in another; max 18x18x18 | Water cavity x 16,000 mB; coolant x 256,000 mB; steam x 160,000 mB per steam-cavity volume; boil capacity = superheaters x 320,000 mB. No steam mB/t rate given | [S30] |
| Induction Matrix | Energy storage ("endgame catch-all solution for a battery") | Cell structure up to 16x16x16; casing up to 18x18x18; one Induction Port in, one out | Capacity and transfer rate not stated on the page (UNVERIFIED numerically) | [S31] |
| Supercritical Phase Shifter (SPS) | Polonium gas -> antimatter gas | SPS Casings each cost 50,000 mB processed nuclear waste (4 polonium pellets + 1 plutonium pellet); optimal minimal build: 72 casings (12 for 3 ports), 123 glass, 3 ports, 1 Supercharged Coil | Max conversion: 2000 mB polonium -> 2 mB antimatter per tick; 800 MFE/t maximum energy; each port accepts up to 400 MFE/t; extra energy wasted | [S32] |
| Dynamic Tank | Large single-type fluid/gas storage with Dynamic Valve | min 3x3x3, max 18x18x18; all edges must be Dynamic Tank; valves only on faces | Default per block: 350,000 mB liquid or 16,000,000 mB gas (configurable) | [S33] |

### 1.9 Jetpack, scuba, MekaSuit (space-suit relevance)

- Jetpack (Armored Jetpack variant also exists) [S35]
  - Fuel: hydrogen gas, up to 24,000 mB.
  - Modes via Armor Mode Switch: Regular (space to rise; fall damage), Hover (constant flight; Space up, Shift down; uses hydrogen at a reduced rate), Disabled.
  - Consumption rate and energy use: not stated.
  - Recipe (partial, layout partly UNVERIFIED): Tin Ingot, Steel Ingot, Basic Control Circuit, Basic Chemical Tank.
  - Armored Jetpack: Jetpack + Diamond Dust + Bronze Ingot + Steel Block [S35].
- Scuba Tank [S36]: stores Oxygen only (durability shown 24,000 mB); chest slot; "provides underwater respiration when a Gas Mask is worn" in the helmet slot; cannot be armored. Recipe components: Basic Control Circuit, Infused Alloy x2, Basic Chemical Tank, Steel Ingot x3 (layout UNVERIFIED).
- MekaSuit [S34]: four pieces (helmet, bodyarmor, pants, boots), each holds 6.40 MFE base; damage drains evenly across worn pieces. Blocking costs 100 kJ (40 kFE) per damage point; magic damage 1 kJ (400 FE) per point and only with the Inhalation Purification Unit. Empty piece = netherite-equivalent armour. Modules add function (jetpack, Nutritional Injection Unit, Inhalation Purification Unit). The page does not list a vacuum or space module.
  - Recipe pattern (all four pieces): HDPE Sheet / Ultimate Control Circuit / HDPE Sheet; HDPE Sheet / matching netherite piece / HDPE Sheet; Polonium Pellet / Basic Induction Cell / Polonium Pellet [S34].
- Mekanism x Create: Northstar (ARR, not MIT) [S49]: turns the MekaSuit into a space suit; uses Mekanism oxygen and liquid hydrogen as rocket fuel and breathable air; adds Space Suit Unit, Oxygen Tank Unit, Space Suit Insulation, Heat Resistive module, Oxygen Tank Expansion. Licence on page: "Licensed ARR (All Rights Reserved)". Minecraft 1.21.1, 1.20.1, 1.19.2; Forge and NeoForge; 40.2K downloads [S49].

### 1.10 Progression chain: intermediate items and recipes

Each recipe is one line. Machine and input from the wiki page named; quantities are not on the pages unless stated.

- Steel Ingot: Iron Ingot + 10 carbon (Metallurgic Infuser) -> Enriched Iron; Enriched Iron + 10 carbon (Metallurgic Infuser) -> Steel Dust; Steel Dust smelted in any furnace -> Steel Ingot. Carbon may be Coal, Charcoal, Block of Coal, Charcoal Block, or Enriched Carbon. Page revision is old; version UNVERIFIED [S37].
- Infused Alloy: Copper Ingot + Redstone (or Block of Redstone or Enriched Redstone), Metallurgic Infuser [S39].
- Reinforced Alloy: Infused Alloy + Diamond Dust (or Enriched Diamond), Metallurgic Infuser [S40].
- Atomic Alloy: Reinforced Alloy + Refined Obsidian Dust (or Enriched Obsidian), Metallurgic Infuser [S41].
- Basic Control Circuit: Osmium Ingot + Redstone (or Block of Redstone or Enriched Redstone), Metallurgic Infuser with an energy step [S38].
- Advanced Control Circuit: Infused Alloy + Basic Control Circuit (crafting) [S38].
- Elite Control Circuit: Reinforced Alloy + Advanced Control Circuit (crafting) [S38].
- Ultimate Control Circuit: Atomic Alloy + Elite Control Circuit (crafting) [S38].
- Polonium Pellet: 1 Fluorite Dust + 1000 mB Water + 1000 mB Polonium (gas) in PRC -> Polonium Pellet + 1000 mB Spent Nuclear Waste [S43]. Polonium gas: Solar Neutron Activator from Nuclear Waste [S22].
- Plutonium Pellet: a "product made from Nuclear Waste"; makes as much spent waste as Polonium Pellets; recyclable into Fissile Fuel at 80% efficiency. Machine and recipe: UNVERIFIED [S44].
- Antimatter Pellet: Chemical Crystallizer from 1,000 mB Antimatter [S45]. Used in Elytra Unit and Gravitational Modulating Unit [S45].
- HDPE Pellet: PRC from Substrate + Liquid Ethylene + Oxygen (liquid ethylene = ethylene condensed in Rotary Condensentrator) [S47][S22]. HDPE Sheet and HDPE Rod use HDPE pellets as an input; their own recipes: UNVERIFIED [S47].
- Bio Fuel: plant matter put in a powered Crusher; seeds give 2, pumpkins/melons/potatoes give 4 (melons and pumpkins usable since 9.3.4). Bio Fuel powers the Bio-Generator and is an input to the PRC for Ethylene [S46].
- Teleportation Core (page name "Teleportation_Core"): gold ingot, lapis lazuli, atomic alloy, diamond; crafting shape per wiki summary is lapis/atomic/lapis on outer rows and gold/diamond/gold in the middle row; layout UNVERIFIED. Used in Portable Teleporter, Teleporter, Digital Miner [S42].
- Fluorite use: raw fluorite + sulfuric acid -> hydrofluoric acid; fluorite dust -> Polonium or Plutonium pellet route [S18].

### 1.11 Public API for chemicals and energy

Source tree: src/api/java/mekanism/api on branch 1.21.x [S48].
- Top-level packages: annotations, chemical, codec, container, datagen, datamaps, energy, event, fluid, functions, gear, heat, integration, inventory, lasers, math, providers, radial, radiation, recipes, robit, security, text, tier [S48].
- Top-level files include MekanismAPI.java, MekanismAPITags.java, MekanismIMC.java, MekanismItemAbilities.java, IMekanismAccess.java, IConfigurable.java, Upgrade.java [S48].
- chemical/: Chemical.java, ChemicalBuilder.java, ChemicalStack.java, ChemicalUtils.java, BasicChemicalTank.java, IChemicalHandler.java, IChemicalTank.java, IMekanismChemicalHandler.java, ISidedChemicalHandler.java, attribute/ [S48b].
- energy/: IEnergyContainer.java, IEnergyConversion.java, IEnergyConversionHelper.java, IStrictEnergyHandler.java, IMekanismStrictEnergyHandler.java, ISidedStrictEnergyHandler.java [S48c].
- Recipe JSON syntax is documented in the Mekanism GitHub wiki (Recipe-Type-JSON-Syntax, Ingredient-JSON-Syntax, Stack-JSON-Syntax, JsonThings-Integration) [S50]. Applicability to 1.21.1 UNVERIFIED.
- Custom chemical registration via CraftTweaker/KubeJS is mentioned for v10.1+ in search snippets (mekanism:gas, mekanism:slurry, mekanism:infuse_type, mekanism:pigment) [search only].

### 1.12 Mekanism port status and history

- Newest official Minecraft target: 1.21.1 NeoForge (build 10.7.19.85) [S3][S4].
- Newest Minecraft version: 26.3 [S98]. No official Mekanism build for 26.x found (UNVERIFIED for any date after 2026-10-10).
- 1.20.1 to 1.21.1: about 13 months from first public 1.21 alpha (2024-07-11) to first release-labelled 1.21.1 build seen (2025-08-17) [S3][S5][S6]. Port start: UNVERIFIED.

---

## 2. CREATE (and notable addons)

### 2.1 Versions and licence

- Create core: 6.0.10 for 1.21.1 NeoForge (2026-04-21, release) [S52]. 6.0.9 (2026-01-02); 6.0.8 (2025-11-02); 6.0.7 (2025-10-25); 6.0.6 (2025-06-11); 6.0.5 (2025-06-07) [S52].
- 6.0.0 "Post Production" (changelog index title) [S56]; uploaded 2025-02-28 on CurseForge and 2025-03-01 on Modrinth [S52][S54].
- 1.20.1 Forge: last official 6.0.8 (2025-11-02) [S52]; wiki status "Available (6.0.8)" [S55].
- 1.21.2 to 1.21.11: skipped; 1.20.2 to 1.21.0: skipped [S55].
- 26.1: "In Progress" (wiki) [S55]. No 26.x release found (UNVERIFIED after the wiki snapshot date).
- Fabric port: maintained by a separate team with its own repository [S84][S53].

Licence quotes (LICENSE.md, branch mc1.21.1, folder dev) [S51]
- Heading: "License Overview"
- "## Assets License (All Rights Reserved)" / "All Rights Reserved" / "Copyright (c) The Create Team / The Creators of Create"
- "This license applies to any files under the following directories:" / "./src/main/resources/assets/"
- "## Code License (MIT)"
- Store pages say: "Create Mod License, linked to the project's LICENSE.md on GitHub" [S53].
- Consequence: code is MIT; art, textures, models, sounds under src/main/resources/assets/ are all rights reserved.

Repository: github.com/Creators-of-Create/Create; facts here are read from branch mc1.21.1, folder dev [S51]. The default branch was not confirmed.

### 2.2 Materials (recipe in one line each)

Source for recipes: generated recipe JSON in mc1.21.1/dev/src/generated/resources/data/create/recipe [S58]. Quantities as stated in the file; unstated count = 1.

| Material | Recipe or source (one line) | Source |
|---|---|---|
| Zinc ore | World gen: create:zinc_ore, count 8 per chunk, uniform Y -63 to 70, config_filter. Crushing: zinc ore -> crushed raw zinc (guaranteed), crushed raw zinc (75%), experience nugget (75%), cobblestone (12.5%), processing 250 ticks | [S59][S58 crushing/zinc_ore.json] |
| Raw zinc, raw zinc block | Files exist (raw_zinc.json, raw_zinc_block.json); recipes not read | [S58] |
| Zinc ingot | 9 c:nuggets/zinc in 3x3 shaped -> 1 create:zinc_ingot | [S58 crafting/materials/zinc_ingot_from_compacting.json] |
| Andesite alloy | Mixing (no heat): minecraft:andesite + c:nuggets/iron -> create:andesite_alloy | [S58 mixing/andesite_alloy.json] |
| Andesite alloy (alt) | Mixing (no heat): minecraft:andesite + c:nuggets/zinc -> create:andesite_alloy | [S58 mixing/andesite_alloy_from_zinc.json] |
| Brass ingot | Mixing with heat: c:ingots/copper + c:ingots/zinc -> 2 create:brass_ingot | [S58 mixing/brass_ingot.json] |
| Copper sheet | Pressing: c:ingots/copper -> 1 create:copper_sheet | [S58 pressing/copper_ingot.json] |
| Brass sheet | Pressing: c:ingots/brass -> 1 create:brass_sheet | [S58 pressing/brass_ingot.json] |
| Zinc sheet | No pressing recipe seen in the visible listing (listing truncated); UNVERIFIED | [S58] |
| Iron and gold sheets | pressing/iron_ingot.json and gold_ingot.json exist; recipes not read | [S58] |
| Precision mechanism | Sequenced assembly, input c:plates/gold, 5 loops; 3 deploying steps: + create:cogwheel, + create:large_cogwheel, + c:nuggets/iron. Weighted outputs: precision_mechanism 120, golden_sheet 8, andesite_alloy 8, cogwheel 5, gold nugget 3, shaft 2, crushed raw gold 2; iron ingot and clock present without weight | [S58 sequenced_assembly/precision_mechanism.json] |
| Precision mechanism success rate | About 80% per a schematic page (search snippet, not verified) | [S112] |
| Sturdy sheet | Sequenced assembly, input c:dusts/obsidian: step 1 filling with 500 mB lava; steps 2-3 pressing; output create:sturdy_sheet (no loop count given) | [S58 sequenced_assembly/sturdy_sheet.json] |
| Electron tube | Shaped crafting, 1 wide x 2 tall: polished rose quartz (top) over c:plates/iron -> 1 create:electron_tube. Polished rose quartz recipe UNVERIFIED | [S58 crafting/materials/electron_tube.json] |
| Rose quartz | Shapeless: c:gems/quartz + 8 x c:dusts/redstone -> 1 create:rose_quartz | [S58 crafting/materials/rose_quartz.json] |
| Experience nugget | (a) crushing zinc, tin or osmium ore: 75% chance; (b) shapeless: 1 create:experience_block -> 9 experience nuggets. Experience block recipe UNVERIFIED | [S58 crafting/materials/experience_nugget_from_block.json] |
| Tin ore (crushing, Create) | Conditional on tag c:ores/tin being non-empty: c:ores/tin -> crushed raw tin (100%), crushed raw tin (75%), experience nugget (75%); 400 ticks | [S58 crushing/tin_ore.json] |
| Osmium ore (crushing, Create) | Conditional on tag c:ores/osmium being non-empty: same pattern as tin; 400 ticks | [S58 crushing/osmium_ore.json] |
| Other crushing files | Also present by name: lead_ore, uranium_ore, nickel_ore, platinum_ore, silver_ore, aluminum_ore, quicksilver_ore, veridium, ochrum and others; conditions not read except tin and osmium | [S58] |

Note: Mekanism's ore tag assignments were not verified. Whether Mekanism ores enter c:ores/tin and c:ores/osmium (which would make Create crush them) is UNVERIFIED.

### 2.3 Kinetic stress system

- Model: a rotating network has stress capacity sources and stress-impact consumers. Registry values are base values at 1 RPM. Effective values scale with |speed| [S64][S65].
  - KineticNetwork: capacity = sum(sources) x |generated speed|; stress = sum(members) x |theoretical speed| [S65].
  - Overstress is decided in KineticBlockEntity or elsewhere, not in KineticNetwork (not read; UNVERIFIED how it compares).
- Unit name: "[in Stress Units]" (config comments) [S60][S66].
- Speed cap: maxRotationSpeed = 256 RPM [S60].
- Indicator thresholds (config "stats"): medium speed 30 RPM, fast 100 RPM; medium impact 4 SU, high 8 SU; medium capacity 256 SU, high 1024 SU [S60].
- Stress can be disabled altogether: disableStress = false by default [S60].
- Impact doubling: config comment says "Note that this cost is doubled for every speed increase it receives." [S66]

Sources (base value at 1 RPM; generator RPM given in parentheses) [S63]
- Water wheel: capacity 32 (8 RPM, may not generate less: false).
- Windmill bearing: capacity 512 (16 RPM, may generate less).
- Steam engine: capacity 1024 (64 RPM, may generate less).
- Hand crank: capacity 8 (32 RPM).
- Creative motor: capacity 16384 (256 RPM, may generate less).

Consumers (impact at 1 RPM) [S63]
- Mechanical press 8; crushing wheel 8; mechanical mixer 4; mechanical bearing 4; mechanical saw 4; millstone 4; deployer 4; mechanical drill 4.
- Shaft, cogwheel, large cogwheel, encased chain drive, speed controller: no impact.

Derived examples (my arithmetic from the code's linear multiplier; not checked in game)
- Water wheel at 8 RPM: 32 x 8 = 256 SU.
- Press at 32 RPM: 8 x 32 = 256 SU.
- Steam engine at 64 RPM: 1024 x 64 = 65,536 SU.
- Creative motor at 256 RPM: 16384 x 256 = 4,194,304 SU.

Power sources in Create core: mechanical only (water wheel, windmill, steam engine, hand crank, creative motor). No FE in Create core; FE bridges come from addons (section 2.8).

### 2.4 Contraptions: movement, limits, what it cannot do

Code structure: content/contraptions contains Contraption, ContraptionCollider, AssemblyException, ContraptionHandler, and folders actors, bearing, behaviour, chassis, elevator, gantry, glue, minecart, mounted, piston, pulley, render, sync, wrench [S69].

Config defaults (CKinetics, "contraptions" group) [S60]
- maxBlocksMoved = 2048 ("Maximum amount of blocks in a structure movable by Pistons, Bearings or other means.")
- maxPistonPoles = 64; maxChassisRange = 16; maxRopeLength = 384; maxCartCouplingLength = 32; rollerFillDepth = 12.
- survivalContraptionPickup = true (minecart contraptions can be picked up in survival).
- minecartContraptionInContainers = false; moveItemsToStorage = true; harvesterReplants = true; harvestPartiallyGrown = false.
- Per-block movement setting defaults: obsidian UNMOVABLE; reinforced deepslate UNMOVABLE; spawner NO_PICKUP; budding amethyst NO_PICKUP.
- Other kinetics limits: maxBeltLength 20; maxChainConveyorLength 32; maxChainConveyorConnections 4; maxEjectorDistance 32; minimumWindmillSails 8; windmillSailsPerRPM 8 [S60].

API hooks
- ContraptionMovementSetting enum: MOVABLE ("Block is fully movable with no restrictions."), NO_PICKUP (block can be moved but a minecart contraption containing it cannot be picked up), UNMOVABLE ("Block cannot ever be moved by a contraption."). Registry REGISTRY or MovementSettingProvider on the block class [S67].
- BlockMovementChecks registration: registerMovementNecessaryCheck, registerMovementAllowedCheck, registerBrittleCheck (torches/ladders), registerAttachedCheck, registerNotSupportiveCheck. Checks run in reverse registration order; first non-PASS wins; fallback otherwise [S68].
- "Immobile blocks generally stop a contraption from assembling" [S68].

What it cannot do (from defaults and checks)
- Move UNMOVABLE blocks; stop at a non-supportive block; carry NO_PICKUP blocks in minecart contraptions; exceed 2048 blocks per movement (default).
- Other limits (rope, piston poles, chassis range, cart coupling) per the defaults above.

### 2.5 Trains, schedules, Ponder, 6.0 changes

Trains (config defaults, CTrains) [S61]
- trainTopSpeed 28 blocks/s; turning top speed 14; acceleration 3 blocks/s^2.
- poweredTrainTopSpeed 40 blocks/s; powered turning top speed 20; powered acceleration 3 blocks/s^2.
- maxBogeyCount = 20; maxAssemblyLength = 128 (station assembly track); maxTrackPlacementLength = 32; manualTrainSpeedModifier = 0.75; trainsCauseDamage = true.

Code folders: content/trains contains bogey, display, entity, graph, observer, schedule, signal, station, track [S113].

Ponder: a separate library (ponder-neoforge 1.0.82 in the dependency page), bundled with the main Create jar [S57][S54].

6.0.0 headline changes [S54][S56]
- Version scheme changed to major.minor.patch starting with 6.0.0.
- Ponder split out into its own library (bundled).
- Flywheel 1.0 rendering engine.
- New content: chain conveyor, item hatch, packager and re-packager, cardboard packages and armour, stock links and tickers, redstone requesters, factory gauges.
- Copper component palette and model updates; rope and hose pulley scrolling texture.
- Runtime recipe generation for some recipes, "to improve mod compat".
- Wrench radial menu; filters, clipboards and schedules can be copied at a crafting table.

Developer dependency (NeoForge 1.21.1, 6.0.10) [S57]
- Maven: https://maven.createmod.net (Create, Ponder, Flywheel); https://maven.ithundxr.dev/snapshots (Registrate).
- create_version 6.0.10-280; ponder_version 1.0.82; flywheel_version 1.0.6; registrate_version MC1.21-1.3.0+67.
- Dependency artifact: com.simibubi.create:create-1.21.1:6.0.10-280:slim (transitive false).
- mods.toml range: [6.0.10,6.1.0).

### 2.6 Public API surfaces

- api/ tree: behaviour, boiler, connectivity, contraption (dispenser, storage, train, transformable), data, effect, equipment, event, packager, registrate, registry, schematic, stress [S70].
- api/stress/BlockStressValues.java: public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS and CAPACITIES (base at 1 RPM); SimpleRegistry<Block, GeneratedRpm> RPM (tooltips only); getImpact(Block), getCapacity(Block) (return 0 if none); setGeneratorSpeed(int) and setGeneratorSpeed(int, boolean mayGenerateLess) [S64].
- api/contraption: BlockMovementChecks, ContraptionMovementSetting, ContraptionType, plus sub-packages dispenser, storage, train, transformable [S70][S67][S68].
- Tags for blocks, items and fluids are documented on wiki.createmod.net/developers [S57].

### 2.7 Port status and 1.20.1 -> 1.21.1 timing

- Create 26.1: "In Progress" [S55].
- 1.20.1 to 1.21.1 timing: Create 1.20.1 v0.5.1i (Forge) uploaded 2024-10-09 [S52]; 1.21.1 first official build 6.0.0 uploaded 2025-02-28 (CurseForge) and 2025-03-01 (Modrinth); 1.20.1 6.0.0 uploaded 2025-02-28 on CurseForge [S52][S54]. The 1.21.1 port start date: UNVERIFIED.
- Create's official build line for 1.21.1 therefore starts with 6.0.0 (Feb 2025), about 4.5 months after 1.20.1 v0.5.1i. Interpreting that gap as port duration is PARTIAL.

### 2.8 Electricity bridges (Create to FE)

- Create Crafts & Additions (MIT) [S71]: "Create Crafts & Additions extends Create and acts as a bridge between electricity and kinetic energy". Minecraft 1.21.1, 1.20.1, 1.19.2, 1.18.2; Forge, NeoForge, Fabric, Quilt; Fabric build "currently not actively maintained". Source: github.com/mrh0/createaddition [S71]. Components (guide, PARTIAL): Electric Motor (consumes FE, produces rotation), Alternator (consumes rotation, produces FE), Accumulator (large FE battery buffering both sides), Portable Energy Interface (energy to and from contraptions). Mekanism recipe compatibility covers Rose Quartz Enriching and Metallurgic Infusing [S71b].
- Create: New Age (BSD-3-Clause) [S72]: "Create: New Age is an addon for the Create mod that adds integration with electricity." Features listed: electricity generation via copper coils and magnets; motors that generate rotation from electricity; an Energiser (crafts using rotation and electricity); wires. The page does not mention FE. Minecraft 1.21.1, 1.20.1, 1.19.2; Fabric, Forge, NeoForge. Source: gitlab.com/antarcticgardens/create-new-age [S73].
- Mekanical Create (ModPackIndex, search only): inserts Create machines (Deployer, Mechanical Press) into Mekanism-style factories; not an energy bridge [S86].
- Mekanism side: 1 FE = 2.5 J [S25]. A search forum post says Create by default has no way to generate FE (community opinion) [search].

### 2.9 Create-adjacent addons (latest verified and licence)

| Addon | Latest verified | Minecraft / loader | Licence (quoted) | Notes | Source |
|---|---|---|---|---|---|
| Create Crafts & Additions | Build number not captured | 1.21.1, 1.20.1, 1.19.2, 1.18.2; Forge/NeoForge/Fabric/Quilt | MIT [S71] | Electricity <-> kinetics bridge | [S71] |
| Create: New Age | Build number not captured | 1.21.1, 1.20.1, 1.19.2; Fabric/Forge/NeoForge | BSD-3-Clause [S72] | Electricity integration | [S72][S73] |
| Create Big Cannons | 5.11.7, NeoForge 1.21.1, requires Create 6.0.7+, uploaded 2026-06-22 (release) [S75] | 1.21.1, 1.20.1 (Forge build 5.11.4 on 2026-05-30) [S75] | "Create Big Cannons License" [S74] (text UNVERIFIED) | Source: github.com/Cannoneers-of-Create/CreateBigCannons [S74] | [S74][S75] |
| Create: Enchantment Industry | 2.5.4b, for Create 1.21.1-6.0.10 (NeoForge), uploaded 2026-10-09 (beta) [S77] | 26.1.2, 1.21.11, 1.21.1, 1.20.1, 1.19.2, 1.18.2 (Modrinth) [S76] | LGPL-3.0-or-later [S76] | Source: github.com/DragonsPlusMinecraft/CreateEnchantmentIndustry [S76] | [S76][S77] |
| Steam 'n' Rails (NeoForge port) | 0.3.0 beta listed 2026-07-27 (CurseForge, search snippet) [S79]; Modrinth shows 0.1.0-rc beta from 2026-01-09 [S78] | 1.21.1 NeoForge | LGPL-3.0-or-later [S78] | "unofficial fork" of the Railway upstream (Layers-of-Railways/Railway) [S78]; alpha may have game-breaking bugs | [S78][S79] |
| Create: Aeronautics (Simulated) | 1.3.2, "Latest release", uploaded 2026-08-29 [S81] | 1.21.1 NeoForge [S80] | "Simulated Project License" (Modrinth) [S80]; "Custom License" (CurseForge) [S81] | Requires Sable and Create [S80]; source: github.com/Creators-of-Aeronautics/Simulated-Project [S80] | [S80][S81] |
| Sable (Aeronautics physics library) | Build not captured | 1.21.1 Fabric and NeoForge | "PolyForm Shield License 1.0.0" [S82] | "A library mod for interactive moving block structures, or 'sub-levels'." Rapier physics engine (Rust). Source: github.com/ryanhcode/sable [S82] | [S82] |
| Create Propulsion: Simulated | 1.1.4 (aggregator, search only) [S83] | 1.21.1 NeoForge | MIT (aggregator, UNVERIFIED on the project page) [S83] | Part of the Sable + Aeronautics ecosystem [S83] | [S83] |

---

## 3. PRECEDENTS: Create and Mekanism together (and space travel)

| Pack or mod | Combines | How Mekanism is gated or ordered | What worked (per source) | Criticism (per source) | Source |
|---|---|---|---|---|---|
| Create 2 Mekanism | Create, AE2, Industrial Foregoing, Mekanism, Ad Astra | Quest chain in that order: "Create >> AE2 >> Industrial Foregoing >> Mekanism >> Ad Astra". 14 quest chapters. Minecraft 1.19.2 Forge, latest file 1.5.3 (2024-02-17) | Clear step-by-step quest chain | Older 1.19.2 pack; successor "Sky Edition 2" reported as built on Create 6.0 (search snippet) | [S87] |
| Create: Mekanized | Create + Mekanism tech pack | Not quest-gated order; "over 2,500 lines of KubeJS code" add recipes so both mods work together; 10-chapter quest book. 1.19.2 Forge, v1.6.3 (2025-01-25) | Recipes glued together by KubeJS; described as "A complex, technology-based modpack revolving around Create and Mekanism." | 1.19.2 only in the pack page | [S88] |
| Create: Above and Beyond | Create-based progression | Quests give a linear order; no Mekanism mention found in search | Clear linear chapters | Mekanism not found in its quests (search) | [S93] |
| Create: Astral | Create-centric quests | Fabric 1.18.2 per review site | Described as more forgiving than Above and Beyond | No Mekanism mention found | [S92] |
| Monifactory | GregTech-focused, Minecraft 1.20.1 | Developers FAQ: Create "is not included by default" and is available only through an optional compatibility script | Developers warn added mods may break progression | Third-party listing calls it Create-heavy (conflicts with FAQ; FAQ preferred) | [S91] |
| Mekanism + Ad Astra | Mekanism ores on Ad Astra planets | Mekanism: Ad Astra Ores places Mekanism ores on all five Ad Astra planets; Desh not processed | Adds ores on each planet | Desh processing gap noted by author | [S90] |
| Create + Ad Astra | Create crushing for Ad Astra ores | Create: Ad Astra Compatibility adds crushed Calorite, Ostrum, Desh | Lets Create process Ad Astra ores | Compatibility issue reports (search) | [S105] |
| Giselle Addon (Ad Astra) | Mekanism suit modules for Ad Astra | Adds a Space Breathing Unit; lets Create's backtanks act like Oxygen Can; Create Jetpack also works | Space suit path using existing items | Older issue: oxygen piping from one planet to another did not work (Ad Astra issue #599 request context) | [S104][S89] |

Criticisms collected from sources (not a survey)
- Mekanism learning curve: "The learning curve is steep, but every step of the progression feels earned." Players who rush advanced machines without the right alloys get stuck (search snippet guide) [search].
- Old forum complaint: Brine sat in Mekanism Generators (Large Solars), which one player called illogical; predates current versions [search].
- Progression of Mekanism itself: the wiki implies a linear order, a Fandom page says there is no defined progression; packs like Create 2 Mekanism therefore supply their own order [search].
- Pack-maker frustration over a compatibility patch that changed textures and was pushed without contact [search].
- No dedicated review or survey of the Create + Mekanism + space combination was found; criticism above is scattered forum posts and issue trackers.

Patterns observed
- Quest-gated order (Create 2 Mekanism): explicit sequence.
- Script glue (Create: Mekanized): 2,500+ lines of KubeJS for recipes.
- Tag unification (Almost Unified): see section 4.

---

## 4. OVERLAP MAP: Mekanism, Create, vanilla

| Material | Mekanism | Create | Vanilla | Shared tag or note | Source |
|---|---|---|---|---|---|
| Copper | Copper Ore, Copper Ingot, Copper Dust (smelt or enrich) | Brass input (c:ingots/copper); copper sheet via pressing; copper ingot/nugget recipes | Copper ore, ingot | c:ingots/copper used by both Create recipes [S58] | [S16][S58] |
| Tin | Tin Ore, Tin Ingot, Tin Dust; tin slurry; tin infuse type | No tin item; crushing recipe conditional on c:ores/tin (crushed raw tin, experience nugget) | None | Almost Unified example: tin ingots assigned to Mekanism as priority [S94] | [S15][S23][S58][S94] |
| Zinc | None listed in Mekanism Ores page | Zinc ore, raw zinc, zinc ingot (from 9 nuggets), zinc nugget; zinc in brass and andesite alloy | None | Create-only zinc chain [S58][S59] | [S12][S58] |
| Lead | Lead Ore, Lead Dust; lead slurry | Crushing file lead_ore.json present (condition not read) | None | Create crushing tags unverified for lead [S58] | [S17][S23][S58] |
| Osmium | Osmium Ore, Dust, Ingot; osmium slurry; liquid osmium gas; Basic Control Circuit input | Crushing conditional on c:ores/osmium | None | Create uses c:ores/osmium tag [S58] | [S13][S23][S38][S58] |
| Uranium | Uranium Ore, Ingot, Yellow Cake | Crushing file uranium_ore.json present (condition not read) | None | No Create recipe use confirmed | [S14][S58] |
| Iron | Enriched iron (Steel path) | Iron nuggets (c:nuggets/iron) in andesite alloy and precision mechanism; iron sheet (c:plates/iron) in electron tube; iron ingot pressing | Iron ore, ingot | c:nuggets/iron, c:plates/iron | [S37][S58] |
| Steel | Steel Ingot via Metallurgic Infuser + furnace | No steel item | None | Mekanism-only | [S37] |
| Brass vs Bronze | Bronze: armour and tools (Mekanism Tools); Bronze Ingot in Armored Jetpack recipe | Brass ingot (2 from copper + zinc, heated mixing); brass sheet; brass in Create recipes | None | Bronze tag ownership UNVERIFIED; no shared tag found | [S11][S35][S58] |
| Gold | Gold infuse type; gold slurry | Golden sheet (c:plates/gold) in precision mechanism; raw gold outputs | Gold ore, ingot | c:plates/gold used by Create | [S23][S58] |
| Redstone | Redstone infuse type; redstone in Basic Control Circuit | 8 x c:dusts/redstone in Rose Quartz recipe | Redstone dust | c:dusts/redstone shared by both | [S38][S58] |
| Quartz | None found | Rose quartz from quartz gem (c:gems/quartz) + 8 redstone dust; polished rose quartz -> electron tube | Nether quartz | c:gems/quartz used by Create | [S58] |
| Obsidian | Refined Obsidian infuse type; Refined Obsidian Dust in Atomic Alloy | Sturdy sheet: c:dusts/obsidian + 500 mB lava | Obsidian block | Shared tag candidate c:dusts/obsidian; Mekanism dust tag UNVERIFIED | [S23][S41][S58] |
| Diamond | Diamond infuse type; Diamond Dust in Reinforced Alloy | None found | Diamond | Mekanism only | [S23][S40] |
| Lapis | Lapis in Teleportation Core (wiki) | None | Lapis | Mekanism only | [S42] |
| Experience | None | Experience nugget (crushing 75%; block -> 9 nuggets); experience block | XP orbs | Create-only | [S58] |
| Electron tube | None (Mekanism circuits are separate) | Electron tube: polished rose quartz + iron sheet | None | Create-only | [S58] |
| Steam | Steam gas; Thermoelectric Boiler; Industrial Turbine | Steam engine (base capacity 1024 at 64 RPM) | None | Two steam systems, different units; cross-compat via addons only | [S23][S28][S30][S63] |
| Oxygen and hydrogen | Gases; Electrolytic Separator; Jetpack and suits | No gas item in Create core | None | Create backtank/oxygen links only via addons (search) | [S22][S35][S104] |
| Energy | FE and J (1 FE = 2.5 J), MFE/t in multiblocks | Stress units (SU), kinetic | None | No shared energy unit; bridge only via addons | [S25][S60][S71] |

Unification and tag tools
- Almost Unified: unifies items via tags and a priority list; config config/almostunified/unify.json; keys modPriorities, priorityOverrides (example: tin ingots to Mekanism), customTags, tagOwnerships, tagInheritance, ignoredTags, ignoredItems, ignoredRecipes, hideJeiRei [S94]. Wiki last edited September 2023 and uses "forge:" examples; 1.21.1 uses c: namespace (per the search summary; UNVERIFIED against the wiki) [S94]. NeoForge 1.21.1 builds 1.4.2, 1.4.1, 1.4.0 listed; one hosting listing dates 1.4.2 to 2026-05-22 [S95].
- Create: Addon Compatibility (requires Almost Unified; 1.20.1 and 1.21.1). Default priority list: pneumaticcraft, copycats, create_connected, create_dd, tfmg; base Create absent from it [S96].
- Tag Fixes: adds 46 tags to items, blocks and fluids missing or broken in their mods; requires LMFT; includes Create; base Mekanism coverage not confirmed [S97].
- Tag namespace used by Create 6.0.10 generated recipes: c:ingots/*, c:nuggets/*, c:plates/*, c:dusts/*, c:gems/*, c:ores/tin, c:ores/osmium [S58]. Mekanism's use of the same c: tags: UNVERIFIED (generated-data path not reachable).
- Mekanism API includes MekanismAPITags.java for its own tags [S48].

---

## 5. UNVERIFIED and gaps (do not build on these without checking)

- Mekanism pigments list and pigment recipes.
- Whether Mekanism 1.21.1 merged the four chemical families into one type.
- Electrolytic Separator mB amounts per operation and per-gas tank size.
- Stock world-gen Y ranges and counts for copper, lead, fluorite, salt (and tin/uranium stock values).
- Plutonium Pellet recipe; HDPE Sheet and HDPE Rod recipes; Teleportation Core exact layout; Jetpack and Scuba Tank exact layouts.
- Induction Matrix capacity and transfer rate; Fusion Reactor size; Thermoelectric Boiler minimum size (page contradicts itself).
- Whether Mekanism ore tags include c:ores/tin, c:ores/osmium, c:ores/lead, c:ores/uranium (determines whether Create's crushing reaches Mekanism ores).
- Mekanism 10.8.0 claim for 26.1.2; any official Mekanism 26.x build after 2026-07-12.
- Create zinc sheet recipe; polished rose quartz; experience block; iron and gold sheet recipe bodies.
- Overstress logic location in KineticBlockEntity (not read).
- Create Crafts & Additions, Create: New Age, Create Propulsion: Simulated, Create Enchantment Industry build numbers beyond those listed.
- Create Big Cannons licence text.
- Create wiki materials pages (Fandom blocked, HTTP 402); Create materials cross-checked only against generated JSON.

---

## 6. Source index

| ID | Title / what | URL | Read as |
|---|---|---|---|
| S1 | Mekanism LICENSE (1.21.x) | https://github.com/mekanism/Mekanism/blob/1.21.x/LICENSE | fetched |
| S2 | Mekanism repository and README (view of branch 26.3) | https://github.com/mekanism/Mekanism | fetched |
| S3 | Mekanism CurseForge files (newest first) | https://www.curseforge.com/minecraft/mc-mods/mekanism/files/all | fetched |
| S4 | Mekanism GitHub releases | https://github.com/mekanism/Mekanism/releases | fetched |
| S5 | Mekanism-1.21-10.6.5.52.jar (alpha, 2024-07-11) | https://www.curseforge.com/minecraft/mc-mods/mekanism/files/5523554 | fetched |
| S6 | Mekanism-1.21.1-10.7.9.72.jar (alpha, 2025-02-25) | https://www.curseforge.com/minecraft/mc-mods/mekanism/files/6237192 | fetched |
| S7 | Mekanism on Modrinth | https://modrinth.com/mod/mekanism | fetched |
| S8 | Mekanism issue #8603 (26.2 question) | https://github.com/mekanism/Mekanism/issues/8603 | fetched |
| S9 | Mekanism Generators (Modrinth) | https://modrinth.com/mod/mekanism-generators | fetched |
| S10 | Mekanism Additions (Modrinth) | https://modrinth.com/mod/mekanism-additions | fetched |
| S11 | Mekanism Tools (Modrinth) | https://modrinth.com/mod/mekanism-tools | fetched |
| S12 | Wiki: Ores | https://wiki.aidancbrady.com/wiki/Ores | fetched |
| S13 | Wiki: Osmium Ore | https://wiki.aidancbrady.com/wiki/Osmium_Ore | fetched |
| S14 | Wiki: Uranium Ore | https://wiki.aidancbrady.com/wiki/Uranium_Ore | fetched |
| S15 | Wiki: Tin Ore | https://wiki.aidancbrady.com/wiki/Tin_Ore | fetched |
| S16 | Wiki: Copper Ore | https://wiki.aidancbrady.com/wiki/Copper_Ore | fetched |
| S17 | Wiki: Lead Ore | https://wiki.aidancbrady.com/wiki/Lead_Ore | fetched |
| S18 | Wiki: Fluorite | https://wiki.aidancbrady.com/wiki/Fluorite | fetched |
| S19 | Example pack config world.toml (lytedev jland pack, commit e4442c4; not stock) | https://git.lyte.dev/lytedev/jland-packwiz-pack/raw/commit/e4442c458e4dbfb070152827a60cb2511a633029/config/Mekanism/world.toml | fetched |
| S20 | Wiki: Ore Processing | https://wiki.aidancbrady.com/wiki/Ore_Processing | fetched |
| S21 | Guide: Mastering Mekanism ore processing 2x-5x | https://jangro.com/2024/12/22/mastering-mekanism-ore-processing-from-2x-3x-4x-to-5x | search snippet |
| S22 | Wiki: Gases | https://wiki.aidancbrady.com/wiki/Gases | fetched |
| S23 | Wiki: Chemical Tanks | https://wiki.aidancbrady.com/wiki/Chemical_Tanks | fetched |
| S24 | Wiki: Electrolytic Separator | https://wiki.aidancbrady.com/wiki/Electrolytic_Separator | fetched |
| S25 | Wiki: Energy Unit Conversion | https://wiki.aidancbrady.com/wiki/Energy_Unit_Conversion | fetched |
| S26 | Wiki: Fusion Reactor | https://wiki.aidancbrady.com/wiki/Fusion_Reactor | fetched |
| S27 | Wiki: Fission Reactor | https://wiki.aidancbrady.com/wiki/Fission_Reactor | fetched |
| S28 | Wiki: Industrial Turbine | https://wiki.aidancbrady.com/wiki/Industrial_Turbine | fetched |
| S29 | Wiki: Thermal Evaporation Plant | https://wiki.aidancbrady.com/wiki/Thermal_Evaporation_Plant | fetched |
| S30 | Wiki: Thermoelectric Boiler | https://wiki.aidancbrady.com/wiki/Thermoelectric_Boiler | fetched |
| S31 | Wiki: Induction Matrix | https://wiki.aidancbrady.com/wiki/Induction_Matrix | fetched |
| S32 | Wiki: Supercritical Phase Shifter | https://wiki.aidancbrady.com/wiki/Supercritical_Phase_Shifter | fetched |
| S33 | Wiki: Dynamic Tank | https://wiki.aidancbrady.com/wiki/Dynamic_Tank | fetched |
| S34 | Wiki: MekaSuit | https://wiki.aidancbrady.com/wiki/MekaSuit | fetched |
| S35 | Wiki: Jetpack | https://wiki.aidancbrady.com/wiki/Jetpack | fetched |
| S36 | Wiki: Scuba Tank | https://wiki.aidancbrady.com/wiki/Scuba_Tank | fetched |
| S37 | Wiki: Steel Ingot | https://wiki.aidancbrady.com/wiki/Steel_Ingot | fetched |
| S38 | Wiki: Basic Control Circuit | https://wiki.aidancbrady.com/wiki/Basic_Control_Circuit | fetched |
| S39 | Wiki: Infused Alloy | https://wiki.aidancbrady.com/wiki/Infused_Alloy | fetched |
| S40 | Wiki: Reinforced Alloy | https://wiki.aidancbrady.com/wiki/Reinforced_Alloy | fetched |
| S41 | Wiki: Atomic Alloy | https://wiki.aidancbrady.com/wiki/Atomic_Alloy | fetched |
| S42 | Wiki: Teleportation Core | https://wiki.aidancbrady.com/wiki/Teleportation_Core | fetched |
| S43 | Wiki: Polonium Pellet | https://wiki.aidancbrady.com/wiki/Polonium_Pellet | fetched |
| S44 | Wiki: Plutonium Pellet | https://wiki.aidancbrady.com/wiki/Plutonium_Pellet | fetched |
| S45 | Wiki: Antimatter Pellet | https://wiki.aidancbrady.com/wiki/Antimatter_Pellet | fetched |
| S46 | Wiki: Bio Fuel | https://wiki.aidancbrady.com/wiki/Bio_Fuel | fetched |
| S47 | Wiki: HDPE Pellet | https://wiki.aidancbrady.com/wiki/HDPE_Pellet | fetched |
| S48 | Mekanism API tree (1.21.x) | https://github.com/mekanism/Mekanism/tree/1.21.x/src/api/java/mekanism/api | fetched |
| S48b | Mekanism API chemical package | https://github.com/mekanism/Mekanism/tree/1.21.x/src/api/java/mekanism/api/chemical | fetched |
| S48c | Mekanism API energy package | https://github.com/mekanism/Mekanism/tree/1.21.x/src/api/java/mekanism/api/energy | fetched |
| S49 | Mekanism x Create: Northstar (ARR) | https://modrinth.com/mod/mekanism-x-create-northstar | fetched |
| S50 | Mekanism GitHub wiki index | https://github.com/mekanism/Mekanism/wiki | fetched |
| S51 | Create LICENSE.md (branch mc1.21.1, dev) | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/LICENSE.md | fetched |
| S52 | Create on CurseForge, files list | https://www.curseforge.com/minecraft/mc-mods/create/files/all | fetched |
| S53 | Create on Modrinth (licence and loaders) | https://modrinth.com/mod/create | fetched |
| S54 | Create 1.21.1-6.0.0 on Modrinth | https://modrinth.com/mod/create/version/1.21.1-6.0.0 | search snippet |
| S55 | Create wiki: Development Status | https://wiki.createmod.net/users/development-status | fetched |
| S56 | Create wiki: Changelog index | https://wiki.createmod.net/users/changelogs/ | fetched |
| S57 | Create wiki: Depending on Create (NeoForge 1.21.1) | https://wiki.createmod.net/developers/depend-on-create/neoforge-1.21.1 | fetched |
| S58 | Create generated recipes, mc1.21.1/dev | https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/generated/resources/data/create/recipe (raw JSON under .../raw/...) | fetched |
| S59 | Create worldgen: zinc_ore placed feature | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/generated/resources/data/create/worldgen/placed_feature/zinc_ore.json | fetched |
| S60 | Create config: CKinetics.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/infrastructure/config/CKinetics.java | fetched |
| S61 | Create config: CTrains.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/infrastructure/config/CTrains.java | fetched |
| S62 | Create config: CWorldGen.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/infrastructure/config/CWorldGen.java | fetched |
| S63 | Create AllBlocks.java (stress calls) | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/AllBlocks.java | fetched |
| S64 | Create api/stress/BlockStressValues.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/api/stress/BlockStressValues.java | fetched |
| S65 | Create KineticNetwork.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/KineticNetwork.java | fetched |
| S66 | Create config: CStress.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/infrastructure/config/CStress.java | fetched |
| S67 | Create api/contraption/ContraptionMovementSetting.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/api/contraption/ContraptionMovementSetting.java | fetched |
| S68 | Create api/contraption/BlockMovementChecks.java | https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/src/main/java/com/simibubi/create/api/contraption/BlockMovementChecks.java | fetched |
| S69 | Create content/contraptions tree | https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/contraptions | fetched |
| S70 | Create api tree | https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/api | fetched |
| S71 | Create Crafts & Additions on Modrinth | https://modrinth.com/project/kU1G12Nn | fetched |
| S71b | Create Crafts & Additions guide (Mekanism and FE notes) | https://craftdownunder.co/guides/mods/create-crafts-additions | search snippet |
| S72 | Create: New Age on Modrinth | https://modrinth.com/mod/create-new-age | fetched |
| S73 | Create: New Age source | https://gitlab.com/antarcticgardens/create-new-age | link on S72 |
| S74 | Create Big Cannons on Modrinth | https://modrinth.com/mod/create-big-cannons | fetched |
| S75 | Create Big Cannons, files list | https://www.curseforge.com/minecraft/mc-mods/create-big-cannons/files/all | fetched |
| S76 | Create: Enchantment Industry on Modrinth | https://modrinth.com/mod/create-enchantment-industry | fetched |
| S77 | Create: Enchantment Industry, files list | https://www.curseforge.com/minecraft/mc-mods/create-enchantment-industry/files/all | fetched |
| S78 | Steam 'n' Rails NeoForge on Modrinth | https://modrinth.com/project/L3Jv0QZI | fetched |
| S79 | Steam 'n' Rails NeoForge on CurseForge | https://www.curseforge.com/minecraft/mc-mods/steam-n-rails-neoforge | search snippet |
| S80 | Create: Aeronautics on Modrinth | https://modrinth.com/mod/create-aeronautics | fetched |
| S81 | Create: Aeronautics, files list | https://www.curseforge.com/minecraft/mc-mods/create-aeronautics/files/all | fetched |
| S82 | Sable on Modrinth | https://modrinth.com/mod/sable | fetched |
| S83 | Create Propulsion: Simulated (ModDex aggregator) | https://moddex.gg/mod/create-propulsion-simulated | search snippet |
| S84 | Modded Wiki: Create | https://modded.wiki/w/Create | fetched |
| S85 | Simulated Project source | https://github.com/Creators-of-Aeronautics/Simulated-Project | link on S80 |
| S86 | Mekanical Create (ModPackIndex) | https://www.modpackindex.com/mod/173114/mekanical-create | search snippet |
| S87 | Create 2 Mekanism, CurseForge | https://www.curseforge.com/minecraft/modpacks/create-2-mekanism | fetched |
| S88 | Create: Mekanized, CurseForge | https://www.curseforge.com/minecraft/modpacks/create-mekanized | fetched |
| S89 | Ad Astra issue #599 (Mekanism compat request) | https://github.com/terrarium-earth/Ad-Astra/issues/599 | search snippet |
| S90 | Mekanism: Ad Astra Ores, CurseForge | https://www.curseforge.com/minecraft/mc-mods/mekanism-ad-astra-ores | search snippet |
| S91 | Monifactory FAQ | https://github.com/Omicron-Industries/Monifactory/blob/main/FAQ.md | search snippet |
| S92 | Create: Astral reviews | https://mmcreviews.com/all/modpacks/create-astral/ | search snippet |
| S93 | Create: Above and Beyond wiki, Getting Started | https://create-above-and-beyond.fandom.com/wiki/Getting_Started | search snippet |
| S94 | Almost Unified config wiki | https://github.com/AlmostReliable/almostunified/wiki/Unification-Config | search snippet |
| S95 | Almost Unified hosting listing (version 1.4.2 date) | https://gdlauncher.com/mods/modrinth/almostunified | search snippet |
| S96 | Create: Addon Compatibility, CurseForge | https://www.curseforge.com/minecraft/mc-mods/createaddoncompatibility | search snippet |
| S97 | Tag Fixes on Modrinth | https://modrinth.com/mod/tag-fixes | search snippet |
| S98 | Minecraft 26.3 release (GamingOnLinux) | https://www.gamingonlinux.com/2026/09/minecraft-java-edition-26-3-wilderness-bound-update-released/ | search snippet |
| S99 | Minecraft 26.2 final date (one wiki source) | https://minecraft.fandom.com/wiki/Java_Edition_26.2 | search snippet |
| S100 | NeoForge for Minecraft 26.1 | https://neoforged.net/news/26.1release/ | search snippet |
| S101 | Iris issue #3141 (26.1 compat targets) | https://github.com/IrisShaders/Iris/issues/3141 | search snippet |
| S102 | Mekanism: Version Locked, CurseForge (26.1.2 community port) | https://www.curseforge.com/minecraft/mc-mods/mekanism-version-locked | search snippet |
| S103 | Mekanism Version Locked, GitHub (community port) | https://github.com/victor0hxz/Mekanism-Version-Locked | search snippet |
| S104 | Ad Astra Giselle Addon on Modrinth | https://modrinth.com/mod/ad-astra-giselle-addon | search snippet |
| S105 | Create: Ad Astra Compatibility (ModPackIndex) | https://www.modpackindex.com/mod/58246/create-ad-astra-compatibility | search snippet |
| S106 | Mekanism and Create modpack (Modrinth; not inspected) | https://modrinth.com/modpack/mekanism-create | search listing only |
| S107 | Create wiki source repository (users folder listing) | https://github.com/Creators-of-Create/wiki | fetched (folder listing) |
| S108 | Mekanism 10.7.0.55 on Modrinth (alpha; 2024-08-20) | https://modrinth.com/mod/mekanism/version/10.7.0.55 | search snippet |
| S109 | Mekanism 10.6.7.54 on CurseForge (alpha; 2024-07-28) | https://www.curseforge.com/minecraft/mc-mods/mekanism/files/5574951 | search snippet |
| S110 | DeepWiki Mekanism overview (unofficial, auto-generated) | https://deepwiki.com/mekanism/Mekanism/1-overview | search snippet |
| S111 | Mekanism Electrolytic Separator recipe structure (GroovyScript docs) | https://cleanroommc.com/groovy-script/mods/mekanism/electrolytic_separator | search snippet |
| S112 | Create schematic page: precision mechanism success rate | https://createmod.com/schematics/ultra-compact-precision-mechanism-assembler-mk1 | search snippet |
| S113 | Create content/trains tree | https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/trains | fetched |
