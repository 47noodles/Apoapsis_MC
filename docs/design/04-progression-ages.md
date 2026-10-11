# 04 — Progression: the Ages of Apoapsis (the SevTech layer)

> Design by Opus. Research inputs: R2 (SevTech Ages structure, Game Stages family, reception), R10 (Mekanism and Create progression chains), R8 (Satisfactory space-elevator phases, GTNH gating, Factorio planet order). The gating engine is Apoapsis's own open `ages` module (`09`), because the modern options are ARR (AStages, FTB Quests), stuck on old versions (the Game Stages family), or a scripting layer rather than a full gating system (KubeJS stages) (R2 §5).

## 4.0 What we keep from SevTech, and what we fix

| SevTech trait (R2 §6, from a small sample of public reviews) | Apoapsis |
|---|---|
| A tactile, slow-burn early game, **praised** | Kept: Age 0 is short, hands-on and full of discovery |
| Betweenlands and dimension grinds, **criticised** as tedious | No mandatory grind dimension. New *places* are always new planets with new mechanics |
| A final age "not worth playing" | Inverted: the **richest content is last** (gas giant, three moons, Hollow, jump tech) |
| Hidden ores (easy to miss; our judgement) | Locked ores look like the host stone but show a faint "unknown mineral" fleck. The Ore Surveyor (from Age 3) names them. Before that, mining one before its Age yields host stone plus a tooltip: *"You don't know how to work this yet. Age 2."* |
| Items hidden completely from JEI (our judgement) | Locked items show in JEI as **silhouettes with "Age N"**, so players can see what's coming |
| Per-player stages by default (R2 §3) | **Team-scoped by default** (friend servers), per-player as an option. Late joiners can be caught up by their team |
| Triumph advancement trees (1.12 only, R2 §3 and §5) | The **Custodian Log** quest book: data-driven JSON, open, story-led |

## 4.1 The story spine: the *Arkwright*

You are a colonist aboard the seed-ship ***Arkwright***, which broke apart entering Verdance's atmosphere. You wake in an escape pod with a cracked datapad. On it is a fragment of the ship's AI, the **Custodian**, which can barely talk.

- The wreckage lies in **Crash Scar** biomes on every cube face (`07` §7.2). Each wreck section is **sealed by technology**. A bulkhead needs steel to cut, a reactor bay needs radiation gear, and the bridge needs a powered data link. **Each Age opens the next tier of salvage**, which returns schematics, story logs and a few rare parts. The SevTech gating has an in-world reason.
- The Custodian is restored step by step. It becomes your quest giver, then your flight-computer tutor, then a voice that knows more about the Precursors than it should.
- **The goal:** rebuild the *Arkwright*'s jump core, find out why the ship was sent to Meridian, and reach the Heart of Hollow, where the Precursors left a door to the next star.

## 4.2 Two layers of progression

1. **Ages** are eras gated by milestones, the SevTech layer. Each Age reveals ores, recipes, machines, dimensions and story.
2. **Survey Points** buy nodes *inside* the current Age at the Research Bench, the KSP science layer (`06` §6.7). Ages decide *what era you're in*. Science decides *which toys you get first*. The Research Bench arrives in Age 3, because Ages 0–2 are short and quest-led.

## 4.3 The Ages

Pacing targets assume one experienced player. A team of 3–4 moves about 2× faster.

### Age 0 — Landfall *(target 1–2 h)*

- **Fantasy:** stranded with nothing.
- **Unlocks:** 2×2 hand crafting, knapped flint, rock tools, plant fibre and cordage, campfire, drying rack, clay pit and **clay kiln**, primitive shelters, the pod's emergency beacon.
- **Ores visible:** stone, clay, flint, surface copper nuggets in riverbeds.
- **Locked:** crafting table (needs a sawn plank bench), metal tools, every machine.
- ***Arkwright* salvage:** pod lockers only (food bars, a cracked datapad, rope).
- **Exit:** fire the clay kiln and cast your first **copper ingot**. The Custodian wakes: *"Metallurgy detected. Resuming colony protocol."*

### Age 1 — Hearth *(target 2–3 h)*

- **Fantasy:** a frontier smithy.
- **Unlocks:** crafting table, **bloomery** (iron blooms), **bronze** (copper + Mekanism tin, crucible-cast in the kiln; the alloy mixer automates it in Age 2), stone and bronze tools, a basic **Kinetics** set (hand crank, water wheel, wooden shafts and cogs, millstone), farming, leather and textiles.
- **Ores revealed:** **tin** (Mekanism), iron, coal.
- ***Arkwright* salvage:** exterior hull fragments, which give scrap metal (smelts to iron) and the first **Colony Charter** log.
- **Exit:** build a **Kinetic Forge Hammer** driven by a water wheel and forge **wrought iron**, then repair the pod beacon's coupling. The Custodian maps two more wreck sites.

### Age 2 — Steam and Brass *(target 4–6 h)*

- **Fantasy:** the Industrial Revolution.
- **Unlocks:** **zinc** and **brass** (copper + zinc, alloyed in a heated mixer), the full **Kinetics** set (belt lines, chutes, sheet press, alloy mixer, jaw crusher, draft kiln, actuator arm, tool actuator, steam boiler and engine on coal or Basalt Rift geothermal), early rail carts, glass and optics.
- **Ores revealed:** **zinc** (Kinetics), **lead** (Mekanism), gold, redstone.
- ***Arkwright* salvage:** the **cargo holds**, which need a steam-powered cutting saw. They give crates of seed stock, a broken drone (study it for Age 3) and the **Engineering Schematics** log.
- **Exit:** a steam-powered production line makes 32 **Gear Trains** and 8 **Thermionic Valves**. Use them to open the sealed **engineering bay** door.

### Age 3 — Current *(target 6–10 h)*

- **Fantasy:** electricity, steel and the first flight.
- **Unlocks:**
  - **Mekanism core:** osmium, the Metallurgic Infuser, **steel**, **infused alloy**, **basic and advanced control circuits**, the Enrichment Chamber (2×), Energized Smelter and Crusher.
  - **Apoapsis grids:** cable-less power, conveyors, refinery, assembler, components (`05` §5.6), static grid bases, the **first rovers** (wheels), then **aircraft** (propellers, atmospheric thrusters, wings, control surfaces, gyroscopes).
  - The **Nether** opens (dimension gate), because Mekanism needs refined glowstone and obsidian.
- **Ores revealed:** **osmium**, **fluorite** (Mekanism), diamond, lapis, quartz.
- ***Arkwright* salvage:** the **engineering bay**: an intact **Survival Pod** block, a blueprint projector schematic, and the **Custodian Core v1** (its voice comes back).
- **Exit (dual):** *First Flight*: reach **1,500 m** in a craft you built and land it intact. *Industrial base*: make 16 **advanced control circuits**. Then the Custodian asks you to *"restore orbital telemetry."*

### Age 4 — Ascent *(target 8–12 h)*

- **Fantasy:** the space race.
- **Unlocks:**
  - **Rocketry:** solid boosters, **hydrolox** engines, cryo tanks, separators, the Stage Controller, heat shields, parachutes, RCS.
  - **The Electrolytic Separator** (H₂ and O₂ from water) and the 3× **Purification Chamber**.
  - **Flight Computer** (Lua), antennas and relays, a **Ground Station**, **map view and maneuver nodes**, the navball's SAS modes, and the science experiments.
  - **Life support:** air vents, airlocks, **Clone Bay**, medbay, the **Ice Processor**.
  - **Also:** **buoyancy cells** (boats, submarines, balloons), the **trade terminal** and **contracts board** (Free Traders orbit Verdance).
  - **Link system:** Link Pod, Proxy Fabricator (Standard frame), Proxy Dock, distress beacon, and the Verdance **hub station** (`14`).
- **Ores revealed:** **uranium** (Meteor Steppe impact cores), plus anything still locked on Verdance.
- **Milestone chain** (KSP-style "firsts", each paying Survey Points): suborbital flight (above 12 km) → **orbit** → relay satellite → **Pale flyby** → **Pale landing** → crewed return.
- ***Arkwright* salvage:** the **bridge**, reached by flying to the Arkwright's **orbiting forward section**, a derelict in high Verdance orbit. That's the first rendezvous mission. It gives the **Navigation Archive**, which reveals the other planets.
- **Exit:** return a **Pale regolith sample** to Verdance *and* rendezvous with the forward section.

### Age 5 — Expanse *(target 15–25 h)*

- **Fantasy:** a solar-system frontier.
- **Unlocks:**
  - **Engines:** methalox, ion and nuclear thermal.
  - **ISRU:** Atmospheric Intake, Sabatier Reactor, Regolith Sifter.
  - **Mekanism:** 4× **Chemical Injection** (HCl) and 5× **Dissolution and Crystallisation** (sulphuric acid) ore processing, reinforced alloy, elite circuits, HDPE, the **fission reactor**, Thermal Evaporation (brine → lithium), radiation gear.
  - **Cryo pods** and **cryo-link**, blast doors, lead-lined plating, autonomy cores, weapons (if enabled), **reaction wheels**, specialised **proxy frames**, the **Hub Core** (player hubs and alliances), **crew consoles** and **Mission Control** (`14`, `15`).
  - **The End** opens (elytra are held back until now so they don't trivialise Age 3 flight).
- **Worlds:** Ochre (plus Rook and Pawn), Cinder, Thalassa, the Shoals.
- **Story:** **Precursor ruins** on Ochre, the Thalassa Drowned Plateaus and Cinder's Ironglass Badlands each hold a **Precursor Fragment**.
- **Exit:** bring all three fragments to a **Decryption Array**. They reveal the Tempest moons' resonance and the coordinates of **Hollow**. The Custodian: *"This system was not chosen. It was summoned."*

### Age 6 — Beyond *(target 20–40 h)*

- **Fantasy:** a mastery-of-physics endgame.
- **Unlocks:**
  - **Fusion:** the Mekanism fusion reactor, the **Fusion Torch** engine, compact fusion cores.
  - **Antimatter:** the SPS, antimatter pellets.
  - **Exotic tech:** **jump drives**, **gravity generators**, mass blocks, atomic alloy, ultimate circuits, aerostat envelopes (large hot-hydrogen lift rigs), Precursor salvage blocks, railguns and artillery.
- **Worlds:** **Tempest** (skimming and aerostats), **Ember**, **Glacis**, **Murk**, the **Wanderer** comet, and **Hollow**.
- **Megaprojects** (optional, server-shared):
  - **Orbital Ring Station** around Verdance.
  - **Skyhook** (a rotating tether) at Pale.
  - **Solar Lens** at Cinder (beamed power).
  - **Jump Gate** pair (from Precursor Cores).
- **Finale:** descend through **the Breach** into Hollow, cross the spin-gravity interior, and reach the **Heart**. Activating it opens the Jump Gate toward a second star, the expansion hook (`11`).

## 4.4 Gate types (the `ages` module)

| Gate | Behaviour when locked | Replaces |
|---|---|---|
| **Item** | Can't use, place or equip. Held items get dropped. Silhouette in JEI with "Age N" | Item Stages |
| **Recipe** | Recipe hidden from crafting and JEI, or shown greyed with its Age | Recipe Stages |
| **Ore** | Disguised as host stone with a faint fleck. Mining yields host stone plus a tooltip. The Ore Surveyor reveals it | Ore Stages |
| **Dimension** | Portals and transit refuse travel (planets are mostly gated by needing a rocket anyway) | Dimension Stages |
| **Machine tier** | Mekanism and Kinetics tiered machines and upgrades are locked by Age | Tinker and Multiblock Stages |
| **Mob** | Some mobs don't spawn before an Age (e.g. Salvager raiders before Age 5) | Mob Stages |
| **Interaction** | Specific block interactions (wreck bulkheads) need an Age plus a tool | — |

- **Stage store:** team-scoped player attachments, plus a `/ages` admin command and a KubeJS binding (KubeJS is LGPL, optional).
- **Quest book (Custodian Log):** JSON quests. Task types are obtain, craft, build-multiblock, reach altitude, orbit, land on a body, science value, visit a biome, rendezvous and survive. Rewards are knowledge (recipes and schematics) and salvage, never loot boxes.
- **Sandbox switch:** server config `ages.mode = story | sandbox`. Sandbox unlocks everything for creative builders.

## 4.5 Feature checklist: progression

- [ ] `ages` stage store (team-scoped default, per-player option, late-joiner catch-up)
- [ ] Item, recipe, ore, dimension, machine-tier, mob and interaction gates
- [ ] JEI silhouettes for locked content; ore flecks plus surveyor reveal
- [ ] Custodian Log quest book (JSON, open), with all seven Ages' quest chains
- [ ] *Arkwright* wreck structures on every face, salvage tiers 0–6, the orbiting forward section
- [ ] Research Bench and Survey Points tech tree within each Age
- [ ] KSP-style milestone "firsts" (suborbital, orbit, flyby, landing, return per body)
- [ ] Megaprojects (orbital ring, skyhook, solar lens, jump gate)
- [ ] Sandbox mode and admin commands
- [ ] Pacing playtests against the targets above
