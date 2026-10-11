# R1: Space Engineers research inventory (SE1 through 1.210, plus Space Engineers 2 status)

Research node R1 for Apoapsis. Facts only, no design recommendations. Research date: 2026-10-11.

## 0. Method, conventions, and source quality

- **Conventions.** S = small grid, L = large grid. Mass, hit points (HP), PCU, power and build time come from the wiki.gg infobox unless noted. "UNVERIFIED" means not confirmed by a second source or a primary source in this pass. "CONFLICT" means two sources disagree; both are given.
- **Primary and secondary sources.** The official wiki (spaceengineers.wiki.gg) is community-maintained and version-stamped; some pages are stale (the thruster comparison is v1.203 data). Keen's own pages (spaceengineersgame.com, Steam news feeds) are used for dates and versions. Review aggregators, hosting-company knowledge bases and news sites are flagged as third-party.
- **Fetch caveat.** Figures were extracted from wiki pages by a summariser. Where a page's text and infobox disagree, both are reported. None of the numbers were checked against game files (the wiki's own Block Definition references were not opened).
- **Coverage gaps.** The per-turn web search budget was exhausted before the Alien planet page could be found (its /wiki/Alien and /wiki/Alien_Planet URLs returned 404). The limit was not worked around. Several block pages (decoy, heat vent, button panels, LCDs, most cargo containers, wheels and suspension, hinge, spherical gravity generator, magnetic plate, Prototech blocks, cryo chamber, and many component recipes) were not fetched or did not yield numbers. These are listed in section 5.

---

## 1. BLOCKS

### 1.0 Index and availability

- The block category ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)) lists 553 block pages in three read pages. Its subcategories are Decorative, Functional, LCD and Structural. The full name list is in Appendix A.
- L-only blocks, as stated on the wiki: Wind Turbine ("works only on large grid stations", [Wind Turbine](https://spaceengineers.wiki.gg/wiki/Wind_Turbine)); Medical Room ([Medical Room](https://spaceengineers.wiki.gg/wiki/Medical_Room)); Safe Zone ([Safe Zone](https://spaceengineers.wiki.gg/wiki/Safe_Zone)); Store, Contracts Block, ATM ([Store](https://spaceengineers.wiki.gg/wiki/Store), [Contracts Block](https://spaceengineers.wiki.gg/wiki/Contracts_Block), [ATM](https://spaceengineers.wiki.gg/wiki/ATM)); Basic Refinery ("can only be built on large grids", [Basic Refinery](https://spaceengineers.wiki.gg/wiki/Basic_Refinery)); planar Gravity Generator ([Gravity Generator](https://spaceengineers.wiki.gg/wiki/Gravity_Generator)); Artillery Turret ([Artillery Turret](https://spaceengineers.wiki.gg/wiki/Artillery_Turret)); Jump Drive stats given for L only ([Jump Drive](https://spaceengineers.wiki.gg/wiki/Jump_Drive)).
- Size CONFLICTS within wiki pages (text vs infobox), noted where they arise: Welder, Grinder, Basic Refinery, Rocket Turret, Oxygen Farm, O2/H2 Generator, Cockpit (mass order), Antenna (L).

### 1.1 Armor, windows, doors, hatches

- **Armor.** Light: Light Armor Block, Light Armor Corner, Half Light Armor Block, Light Armor Slope, Light Armor Panel. Heavy: Heavy Armor Block, Heavy Armor Corner, Half Heavy Armor Block, Heavy Armor Slope, Heavy Armor Panel. Other: Rounded Edge Blocks, Rounded Edge A/B/C, Rounded Corner/Slope families ([Armor](https://spaceengineers.wiki.gg/wiki/Armor) disambiguation page; [Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)).
- **Armor HP (search excerpt of the Light Armor Block page, not fetched directly).** S light armor block 20 kg, 100 HP; L 500 kg, 2,500 HP ([Light Armor Block](https://spaceengineers.wiki.gg/wiki/Light_Armor_Block), via search summary, UNVERIFIED).
- **Deformation.** "Damaged armor blocks deform and allow damage to neighbouring blocks" ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- **Windows.** Many variants in the category: Window 1x1 Face/Flat/Side/Slope (and Inv.), 1x2 variants, 2x3 Flat, 3x3 Flat, Half Window family, Bay Window family, Barred Window, Round Window, Diagonal Window, Vertical Window, Viewport, Bridge Window family ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)). Window stats and per-variant behaviour were not fetched.
- **Doors and gates.** Door, Sliding Door, Sliding Hatch Door, Half Sliding Hatch Door, Sci-Fi Sliding Door, Gate, Small Gate Tall, Small Gate Wide, Narrow Door, Narrow Door Half, Offset Door, Angled Door A/B, Blast Doors, Blast Door Corner, Blast Door Corner Inverted, Blast Door Edge, Airtight Hangar Door, Warfare Hangar Door ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)).
- **Airtightness of doors.** Closed sliding doors seal and bring their own airtight side walls. Other doors do not; open doors leak. Offset doors are "somewhat buggy". The blast door block is airtight, but its corners and edges leak. The Airtight Hangar Door is non-airtight as a 1x1x1 block; closed, it extends into an airtight 1x3x1 block ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- **Parachute Hatch.** See 1.14.
- **Heat Vent, Exhaust Cap, Exhaust Pipe, Willis Ducts.** Names only ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)); descriptions not fetched.

### 1.2 Conveyors, ports and logistics

- **Blocks.** Conveyor Tube (and Reinforced), Curved Conveyor Tube, Conveyor Tube T Junction, Conveyor Junction, Conveyor Sorter, Conveyor Frame, Conveyor Converter, Conveyor Cap. Small-grid variants: Small Conveyor, Small Conveyor Tube, Small Curved Conveyor Tube, Small Conveyor Tube T Junction, Small Conveyor Sorter, Small Reinforced Conveyor Tube (and T/Curved variants). Large-grid Reinforced Conveyor Tube family ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system); [Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)).
- **Ports.** Ports connect by touching or through tubes. Small and large ports cannot connect directly. Inventory ports are a subset of conveyor ports, and engineers reach inventory only through inventory ports. Small ports reject large items (most components, rockets, O2/H2 bottles). Dual-size blocks that convert between sizes: Conveyor Junction, Conveyor Converter, Medium Cargo Container, O2/H2 Generator, Oxygen Tank, Fighter Cockpit, Cockpit ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- **Gases.** Gases flow through ports and tubes of any size. Gas bottles require large conveyors. Tanks pull gas only when Stockpile is enabled ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- **Connector, Ejector, Collector.** Connectors can link conveyor systems of two grids. Ejectors pull from conveyored inventories into space. Collectors push floating items into their grid's inventories. Collector and Flat Collector names exist ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system); [Connector](https://spaceengineers.wiki.gg/wiki/Connector)).
- **Cargo.** Cargo Crate, Cargo Terminal (and Half), Small/Medium/Large/Modular Cargo Container, Freight 1/2/3, Storage Bin (Pallet, Stack), Storage Shelf 1/2/3, Barrel(s), Stacked Barrels, Armory, Lockers, Server Rack, Weapon Rack. Capacities for most were not fetched ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)).

### 1.3 Production: refining, assembling, survival, gas and farming

- **Refinery** (names: Refinery, Basic Refinery, Prototech Refinery). Base speed factor 1.3; runs at max speed with 560 kW; "Advanced refineries refine all ores into ingots" including platinum, uranium, silver, gold. Up to 4 upgrade modules, each needing two matching attachment points (else halved): Speed Module (more speed and power), Yield Module ("up to 200% ingots", no extra power), Power Efficiency Module ([Refinery](https://spaceengineers.wiki.gg/wiki/Refinery)). Mass/HP not fetched.
- **Basic Refinery** (L only). Speed factor 0.65 ("half the speed of the Refinery"). Accepts iron, nickel, cobalt, silicon, magnesium ore, stone and scrap. Mass 2,802 kg, HP 13,010, PCU 75. Power not stated. Infobox size 1x2x1 vs text 1x1x2 (CONFLICT) ([Basic Refinery](https://spaceengineers.wiki.gg/wiki/Basic_Refinery)).
- **Assembler** (names: Assembler, Industrial Assembler (a reskin, same function, different size), Prototech Assembler, Basic Assembler). Max draw 560 kW; runs slower with less power. Two 4,000 L inventories under Realistic settings (input takes material only, output takes components only). Mass 4,092 kg, HP 17,710, PCU 40, build time 80 s, size 1x1x2 per page. Speed and Power Efficiency modules as for the refinery. One queue at a time ([Assembler](https://spaceengineers.wiki.gg/wiki/Assembler)).
- **Basic Assembler** (infobox "Fits large grid", i.e. L only). Idle 1.00 kW, max 280 kW. Mass 2,208 kg, HP 9,700, PCU 40, build 40 s. Can make magazines (S-10, S-20A, MR-20, MR-50A) and Gatling Ammo Boxes. Cannot make Explosives, Thruster Components, Superconductors, Reactor Components, Gravity Components, mid/endgame ammo, Proficient/Elite tools, or Elite pistols/rifles/rocket launchers. Speed relative to the Assembler not stated ([Basic Assembler](https://spaceengineers.wiki.gg/wiki/Basic_Assembler)).
- **Survival Kit** (S 4x3x3 or L 1x1x1; Survival Kit Type II exists). S: 691 kg, HP 1,040, PCU 140, build 20 s; L: 1,171 kg, HP 3,440, PCU 140, build 30 s. Power 15 kW idle, 200 kW max. "A limited all-in-one factory, health station, and respawn point." Can build Computer, Construction Comp, Display, Girder, Interior Plate, Motor, Solar Cell, Steel Plate, the Grinder/Hand Drill/Welder tools, a Flare Gun, an S-10 pistol and magazine, and a Meal Pack from algae. Cannot build mid-game components ([Survival Kit](https://spaceengineers.wiki.gg/wiki/Survival_Kit)). The page also gives stone yields per 100 kg (see 2.11, CONFLICT).
- **O2/H2 Generator** (names: O2 H2 Generator, Lab O2 H2 Generator, Prototech O2 H2 Generator). Ice in; oxygen and hydrogen out. Each gas is produced only if something consumes it. Wiki table (per second, per the page): S 50 L O2 and 100 L H2 from 5 kg ice; L 250 L O2 and 500 L H2 from 25 kg ice. Active power 100 kW (S) / 500 kW (L), idle 1 kW. S 3x3x2, 298.6 kg, HP 1,203, PCU 50, 14 s; L 1x2x1 (infobox) vs 1x1x2 (text), 2,587 kg, HP 12,435, PCU 50, 22 s ([O2 H2 Generator](https://spaceengineers.wiki.gg/wiki/O2_H2_Generator)). The Prototech O2/H2 Generator (L) refuels "up to 10x faster" with less power ([Prosperity update](https://www.spaceengineersgame.com/prosperity/)).
- **Oxygen Farm.** "One Oxygen Farm produces, in perfect conditions, 0.03 liters per second." Power input 20 kW. Works only in sunlight. Mass 3,004 kg, HP 11,970, PCU 25. Size conflict: infobox 3x1x1 vs text 1x1x3 ([Oxygen Farm](https://spaceengineers.wiki.gg/wiki/Oxygen_Farm)).
- **Farming and food.** Algae Farm, Farm Plot, Irrigation System, Planters, Food Processor, Oven, Microwave, Fridge, Kitchen, Modular Kitchen, Dispenser, Vending Machine. Names only (not fetched). Algae feeds the Survival Kit's Meal Pack ([Survival Kit](https://spaceengineers.wiki.gg/wiki/Survival_Kit)).
- **Refill Station.** Name only. A search excerpt says it restores O2/H2 (UNVERIFIED).

### 1.4 Power

- **Large Reactor** (S and L). L 300 MW (3x3x3 = 7.5 m per side); S 14.75 MW (1.5 m per side). Fuel: uranium ingots, 1 MWh per kg. At full power 1 kg lasts 12 s (L) or 244 s (S). HP 147,075 (L) / 8,845 (S). Mass 73,795 kg (L) / 3,901 kg (S). PCU 25. Damage causes radiation within about 60 m ([Large Reactor](https://spaceengineers.wiki.gg/wiki/Large_Reactor)).
- **Small Reactor.** Separate block. Large Reactor is about 27 times the volume of Small Reactor; per volume, L gives about 26% less output than S on large grids, and about 9% more on small grids ([Large Reactor](https://spaceengineers.wiki.gg/wiki/Large_Reactor)).
- **Prototech Fusion Reactor.** Name only (listed on the Power page).
- **Battery** (names: Battery, Small Battery, Battery Bank, Battery Bank Offset, Warfare Battery, Prototech Battery). S: 4 MW max in and out, 1 MWh stored, 1040.4 kg, size 3x2x3. L: 12 MW in and out, 3 MWh stored, 3845 kg, size 1x1x1. The Warfare Battery needs the Warfare 2 Pack DLC. Modes: Automatic (default), Recharge (draws from other sources, including other batteries; thrusters get no power from a battery in this mode), Discharge ([Battery](https://spaceengineers.wiki.gg/wiki/Battery)).
- **Solar Panel** (Colorable variants also exist). S 0.04 MW, L 0.16 MW. Best at 90 degrees to the sun. Nothing is produced during the night half of the day cycle. Shadows and glass obstruct. Weather can dim it ([Solar Panel](https://spaceengineers.wiki.gg/wiki/Solar_Panel)).
- **Wind Turbine** (L only; Twin-Blade Wind Turbine also exists). Max about 0.4 MW at average weather. Output depends on clearance (height above ground and obstructions), atmospheric density and weather: ground placement 150-200 kW, pillar placement 200-400 kW. Zero output in space, on mobile grids, and on airless moons ([Wind Turbine](https://spaceengineers.wiki.gg/wiki/Wind_Turbine)).
- **Hydrogen Engine.** L: 5 MW max, 500 L/s fuel, 3,253.8 kg, HP 13,654, size 1x1x2. S: 0.5 MW, 50 L/s, 1,005.2 kg, HP 4,141, size 3x2x2. PCU 25. Works on any craft or base, even underground, and is independent of sunlight and wind. Vacuum behaviour not stated ([Hydrogen Engine](https://spaceengineers.wiki.gg/wiki/Hydrogen_Engine)).
- **Power network rules.** See 2.1.

### 1.5 Thrusters

| Type (block) | Grid / size | Thrust | Power | Mass | HP | Notes |
|---|---|---|---|---|---|---|
| Atmospheric Thruster (3x1x1) | S | 96 kN | 0.6 MW | 9.7 Mg (table) | not given | Atmospheric only in practice |
| Atmospheric Thruster (3x1x1) | L | 648 kN | 2.4 MW | 66 Mg (table); CONFLICT: a search excerpt quotes a 4,000 kg infobox | not given | |
| Large Atmo (5x3x3) | S | 576 kN | 2.4 MW | 58.7 Mg | not given | |
| Large Atmo (5x3x3) | L | 6,480 kN | 16.8 MW | 661 Mg | not given | |
| Flat Atmo (1x1x1) | S / L | 32 kN / 200 kN | 0.2 MW / 0.8 MW | 3.2 / 20 Mg | not given | |
| Large Flat Atmo (2x3x3) | S / L | 230 kN / 2,600 kN | 1.0 MW / 6.7 MW | 23 / 265 Mg | not given | |
| Hydrogen Thruster | S / L | 98,400 N / 1,080,000 N | 0 (needs a powered tank or generator) | 334 kg / 1,420 kg | 1,390 / 5,980 | Works equally in atmosphere and vacuum |
| Ion Thruster | S / L | 14,400 N / 345,600 N | 0.2 MW / 3.36 MW | 121 kg / 4,380 kg | 350 / 7,180 | Fuel-free, power only; best in vacuum |
| Prototech Thruster | not fetched | not fetched | not fetched | | | Described as an end-game thruster "with similar functionality to Ion Thrusters" |

Sources: [Atmospheric Thrusters](https://spaceengineers.wiki.gg/wiki/Atmospheric_Thrusters) (table and footnotes; Earthlike values), [Hydrogen Thruster](https://spaceengineers.wiki.gg/wiki/Hydrogen_Thruster), [Ion Thruster](https://spaceengineers.wiki.gg/wiki/Ion_Thruster), [Thruster](https://spaceengineers.wiki.gg/wiki/Thruster) (type descriptions).

- Other thruster names in the category: Large Atmospheric Thruster, Large Flat Atmospheric Thruster (and D Shape variants), Sci-Fi Atmospheric Thruster, Sci-Fi Large Atmospheric Thruster (function not verified), Hydrogen Thrusters, Large Hydrogen Thruster, Large Ion Thruster, Ion Thrusters ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)).
- Thruster effectiveness vs atmosphere is covered in 2.5.

### 1.6 Gyroscope, gravity generators, artificial mass, space ball, jump drive

- **Gyroscope** (S and L; Prototech Gyroscope exists). S: 1x1x1, 613.6 kg, HP 2,793, power -0.002 MW (text says 0.6 W), force 448,000 N, PCU 50, 14 s. L: 1x1x1, 12,817 kg, HP 63,105, power -0.01 MW (text says about 30 W), force 3.36E+07 N, PCU 50, 40 s. Torque in N·m not stated. Effectiveness is "inversely proportional to the vessel's mass". Rotation is about the centre of mass. Power figures CONFLICT between infobox and text ([Gyroscope](https://spaceengineers.wiki.gg/wiki/Gyroscope)).
- **Gravity Generator, planar** (L only). Cuboid area of effect: each side 1 to 150 m, measured from the collision box edges. Intensity 0 to 1 G, and can be set to -1 G (pushes upward). Stacked fields can exceed 1.0 G. Acceleration is uniform inside the area, with no falloff by distance or by side of the generator. Power -0.56713 MW. Mass 8,532 kg, HP 20,320, PCU 185. Affects players with jetpacks off, floating items, and grids with Artificial Mass blocks. Planetary gravity weakens it, so it is used mostly in space. On planets, usable strength is capped at 50% of the artificial value ([Gravity](https://spaceengineers.wiki.gg/wiki/Gravity); [Gravity Generator](https://spaceengineers.wiki.gg/wiki/Gravity_Generator)).
- **Spherical Gravity Generator.** Pull centred on the block, flips at a focal point. Radius, strength, power, mass and interaction numbers not retrieved ([Gravity Generator](https://spaceengineers.wiki.gg/wiki/Gravity_Generator)).
- **Artificial Mass** (S and L). "When powered, it creates 50,000Kg of artificial mass in a single block." S 906.4 kg mass per block, L 9,544 kg. Power S -0.025 MW, L -0.6 MW (600 kW needed to function). Gravity generators are "very selective" and by default do not affect ships. Effect strength scales with the number of AM blocks; more AM blocks add mass, power and thrust requirements ([Artificial Mass](https://spaceengineers.wiki.gg/wiki/Artificial_Mass)). The 50,000 kg figure is not reconciled with the infobox masses on the page.
- **Space Ball** (S and L). Spherical. Mass S 2,281.4 kg, L 7,144 kg; HP 7,807 / 24,920; PCU 25; no power. Has mass and is pulled by gravity. Bouncy, and "generally do not destroy blocks". A fast enough impact can kill players. Virtual mass is configurable. Optional broadcast within 50 m. Pushed with mechanical blocks or gravity generators ([Space Ball](https://spaceengineers.wiki.gg/wiki/Space_Ball)). The Space Ball page does not describe any dampening function; the name is not evidence of one.
- **Jump Drive** (large-grid stats). 3x3x2, 35,980 kg, HP 30,080, power -32 MW, PCU 100, build 120 s. Range 2,000 km per drive (multiples stack). Full range only at total mass under 1,250,000 kg; above that the range falls proportionally (example: 1,562,500 kg gives 80%). Storage 3 MWh, charge up to 32 MW, 80% charge efficiency (3.75 MWh input for full), charge time about 7 min. Jump has a 10 s countdown. Must be at 100% charge. Blocked by a planet or asteroid in line of sight. Jumps into or out of natural gravity wells are not allowed. Targets under 5 km are refused. Only a pilot can trigger a jump (not timers or button panels). Attached grids (connectors, magnetic landing gear, mechanical blocks) and seated players come along ([Jump Drive](https://spaceengineers.wiki.gg/wiki/Jump_Drive)).

### 1.7 Connection, docking and mechanical blocks

- **Merge Block.** S 1x3x3, 152.2 kg, HP 621, PCU 125, 10 s. L 1x1x1, 558.4 kg, HP 2,092, PCU 125, 20 s. Small Merge Block (1x1x1) added in 1.199. Both grids must be the same size. Full-face contact gives a permanent bond; slope or edge contact gives a temporary one. Needs control of at least one grid, and ownership or faction permission on both. Captured NPC grids cannot merge. Rotor- or piston-joined sub-grids do not merge ([Merge Block](https://spaceengineers.wiki.gg/wiki/Merge_Block)).
- **Connector.** S 3x3x2, 685.2 kg, HP 2,796, PCU 125, 20 s, inventory 1,152 L. L 1x1x1, 3,564 kg, HP 16,720, PCU 125, 30 s, inventory 8,000 L (Realistic). Magnetic lock, lock status shown by rings. Connected grids share power and inventory. Auto-unlock timer. Trading Mode blocks inventory, power and gas transfer for strangers. Power requirement CONFLICTS within the page: "no longer require any power" vs "both sides are powered" ([Connector](https://spaceengineers.wiki.gg/wiki/Connector)).
- **Landing Gear.** S 1x2x3, 104 kg, HP 390, PCU 35, 12 s. L 1x2x3, 3,304 kg, HP 15,840, PCU 35, 30 s. Magnetic, needs no power to lock. Switching off does not release it. Autolock option. Ring colours: white (no surface), blue (autolock armed), yellow (surface in range), green (secured), red (off). No break-force figure given ([Landing Gear](https://spaceengineers.wiki.gg/wiki/Landing_Gear)).
- **Magnetic Plate** (and Large Magnetic Plate). Compact variant of landing gear; magnetises through thin blocks. No stats fetched.
- **Rotor and Advanced Rotor.** Rotor carries power and terminal access into the sub-grid. Advanced Rotor also connects conveyor systems. Rotor: S 1x2x1, 604.6 kg, HP 2,618, PCU 100, 10 s, power -0.2 kW; L 1x2x1, 1,006 kg, HP 4,105, PCU 100, 20 s, power -2 kW. Velocity in RPM (no maximum given). Default torque zero. Safety detach 10 cm to 5 m (default) ([Rotor](https://spaceengineers.wiki.gg/wiki/Rotor)).
- **Hinge** (Hinge, Hinge 3x3, Hinge Part, Hinge Part 3x3). Controls are "practically the same" as rotors. Stats not fetched.
- **Piston** (and Square Piston, Top Piston Part). S 1x3x1, 304.2 kg, HP 1,291, PCU 100, power -0.2 kW. L 1x3x1, 1,731 kg, HP 6,905, PCU 100, power -2 kW. Extension S 0 to 2 m (total 1 to 3 m); L 0 to 10 m (total 5 to 15 m). Max speed 5 m/s in 0.5 m/s steps. Braking torque cannot be zero, so pistons cannot move passively ([Piston](https://spaceengineers.wiki.gg/wiki/Piston)).
- **Wheels and suspension.** Wheel 1x1, 2x2, 3x3, 5x5; Wheel Suspension 1x1 to 5x5; Short Wheel Suspension 1x1 to 5x5; Offroad Wheels. Suspension speed slider is governed by the session setting AdjustableMaxVehicleSpeed (default true, see 2.22). Stats not fetched.

### 1.8 Tools: drill, welder, grinder, projector

- **Drill** (ship drill; Prototech Drill exists). S 3x3x6, 1,004.2 kg, HP 4,381, PCU 190, build 20 s, inventory 3,375 L. L 1x1x3, 6,741 kg, HP 32,125, PCU 190, build 30 s, inventory 23,437.5 L. Uses 2 kW while running. Mines ore; the inventory accepts only ore. Terrain-clearing mode deletes ores along with stone. A dig makes a hole about three small blocks across and one or two deep ([Drill](https://spaceengineers.wiki.gg/wiki/Drill)). Mining rate not stated.
- **Welder Block.** Range 3 small blocks (1.5 m) on S, 4 small blocks (2.0 m) on L and stations. Spherical range. 2 kW whether working or idle. S 3x3x6 (infobox) and 448.4 kg, HP 1,882; L 1x1x2 (infobox), 713.4 kg, HP 3,042. PCU 150. Build time 18 s (S) / 24 s (L). Inventory accepts components only (3,375 L S, 15,625 L L). A welded block belongs to the welder's owner; Help Others keeps the other player's ownership. Active welder tip is lethal to players in range ([Welder (Block)](https://spaceengineers.wiki.gg/wiki/Welder_(Block))). Size CONFLICT between infobox and text.
- **Grinder Block.** Range 1.5 m (S) / 2.0 m (L). Grinding speed "the same as a handheld grinder" (rate not given). 2 kW. S 488.4 kg, HP 1,932; L 761.4 kg, HP 3,122. PCU 100. Does not affect voxels or ores, nor free-floating items. Will not grind its own grid, but sub-grids on pistons or rotors can be hit. Lethal to players in Survival. Refund and damaged-block rules not fetched ([Grinder (Block)](https://spaceengineers.wiki.gg/wiki/Grinder_(Block))).
- **Projector.** Shows a non-solid, full-size hologram of the blueprint. Projected blocks are unbuilt, so they must be welded in Survival. S 130.4 kg, HP 422; L 526.4 kg, HP 2,382; PCU 50. Each projected block costs 1 PCU. Projector and blueprint must match in grid size. Only supported blocks can be welded. Sub-grids are not projected. Keep Projection, Show Only Buildable, and Mark Missing/Unfinished (150 m) options. Decorative projections are "a bit performance heavy" ([Projector](https://spaceengineers.wiki.gg/wiki/Projector)). Projection distance and maximum blueprint size not stated.
- **Blueprints.** Saved grid templates (blocks, names, colours, control panel setup, PB scripts, toolbars, block groups). Stored locally under the user's SpaceEngineers Blueprints folder. Publishable to Steam Workshop or mod.io. Creative paste is free; Survival requires a projector and components and welding. Multi-grids can be pasted in Creative but only one (sub)grid can be projected in Survival ([Blueprint](https://spaceengineers.wiki.gg/wiki/Blueprint)).

### 1.9 Cockpits, control, medical and life support

- **Cockpit** (and Industrial, Fighter, Flush, Open Sloped, Buggy, Cab, Rover, Saddle, Saddle Compact, Sloped variants). S 3x3x3 (797 kg, HP 3,180); L 1x1x1 (1,768 kg, HP 7,380). PCU 150. Build 30 s (S) / 40 s (L). The page says the S version is heavier than the L version in its construction text, which contradicts the infobox (CONFLICT). Multiple cockpits require one "Main Cockpit" to steer and to start Jump Drives. A seated player recharges suit power; a closed cockpit in an oxygen-fed, airtight room recharges suit oxygen ([Cockpit](https://spaceengineers.wiki.gg/wiki/Cockpit)).
- **Remote Control.** Flies drones or shuttles from range. Manual control needs a powered antenna owned by the controlling player on both sides, an RC block on the controlled grid, and at least a front camera. Autopilot needs no antenna. S 1x1x1, 38.2 kg, HP 101, PCU 25, power 10 kW, 14 s. L 137 kg, HP 505, 30 s. Drones remove the player-health risk from cockpit destruction; there is no third-person view ([Remote Control](https://spaceengineers.wiki.gg/wiki/Remote_Control)).
- **Control Seat, Flight Seat, Helm, Bridge Helm, Control Station, Suspended Control Seat/Station, Passenger Seat, Passenger Bench.** Names only; functions not fetched.
- **Medical Room** (L only). 2x1x2, 4,257 kg, HP 9,510, PCU 30, build 50 s. Heals "five times faster" than a Survival Kit. Recharges suit power and health; if conveyored to gas tanks, also refills suit O2/H2 and bottles. Respawn point for owner and shared faction members who have used it. Requires power ([Medical Room](https://spaceengineers.wiki.gg/wiki/Medical_Room)).
- **Cryo Chamber.** Name in the category; the Medical Room page does not describe it. A search excerpt says it restores O2/H2 but not health (UNVERIFIED).

### 1.10 Communication and sensing

- **Antenna** (S and L; Compact Antenna, Parabolic Antenna, Antenna Dish also listed). S 1x1x2, 72.2 kg, HP 236, PCU 100, 16 s, power -0.2 MW. L 1x6x2 3,401.6 kg, HP 12,808, PCU 100, 40 s, power -0.2 MW. Range up to 50 km (0 to 50,000 m slider). Power is range in metres divided by 250, so max is 200 kW. Lightning is attracted within 10 m (L) or 2 m (S) when powered ([Antenna](https://spaceengineers.wiki.gg/wiki/Antenna)).
- **Laser Antenna** (S and L). Paired point-to-point link, unlimited range at power cost, invisible to players (no broadcast). Needs line of sight: planets, asteroids, ships or players block it. Power: at or under 200 km, watts equal metres to the partner; over 200 km, 100,000 + distance² / 400,000 watts (2.6 MW at 1,000 km; 250.1 MW at 10,000 km). S 3x3x3, 666 kg, HP 1,925, PCU 100. L 1x2x1, 3,584 kg, HP 8,050, PCU 100 ([Laser Antenna](https://spaceengineers.wiki.gg/wiki/Laser_Antenna)). Size CONFLICT for L (text says 1x1x2).
- **Beacon** (S and L). Visible on other players' HUDs within a radius of 0 to 200,000 m (log slider). Can be attached to moving grids. S 1x1x2, 84.2 kg, HP 306, PCU 50. L 1x2x1, 2,662 kg, HP 10,710, PCU 50. Power about 1 kW per 5 km of radius (20 km default = 2 kW) ([Beacon](https://spaceengineers.wiki.gg/wiki/Beacon)).
- **Ore Detector** (S and L). Shows up to five ore locations on the HUD. Range 50 m (S) and 150 m (L). Detectors do not stack range. Antenna broadcast extends visibility across antenna range. S 1x1x2, 105.2 kg, HP 405, PCU 40, 14 s; L 1x1x2, 1,545 kg, HP 6,505, PCU 40, 30 s. Power not stated ([Ore Detector](https://spaceengineers.wiki.gg/wiki/Ore_Detector)).
- **Sensor** (S and L). Detects players (on foot or jetpack; seated players are not detected), grids and blocks (filtered by owner/friendly/neutral/enemy), floating objects, sub-grids and asteroids. Range up to 50 m in each of six directions, minimum 0.1 m. Two action slots (enter, leave), up to 9 toolbar actions. HP 515, PCU 25, 0 kg mass ([Sensor](https://spaceengineers.wiki.gg/wiki/Sensor)).
- **Camera.** 1x1x1, 0 kg, HP 203, power -0.00003 MW, PCU 25, build 6 s. Used to view remote locations, to examine planets and asteroids for ores, and to analyse passing grids. Required for Remote Control driving ([Camera](https://spaceengineers.wiki.gg/wiki/Camera)). View distance and FOV not given.
- **Broadcast Controller.** 1x1x1. S 46.4 kg, HP 142; L 344 kg, HP 1,155; PCU 25; power -0.0001 MW; build 6 s (S) / 16 s (L). Sends chat messages (up to eight prerecorded). Recipients: owner, faction, or everyone. Without an antenna on the same grid, only players seated in cockpits on that grid see the message ([Broadcast Controller](https://spaceengineers.wiki.gg/wiki/Broadcast_Controller)).

### 1.11 Weapons

- **Gatling Turret** (S and L; Gatling Gun and Gatling Turret Type II DLC variant also listed). S 5x5x5, 692 kg, HP 2,810. L 3x3x3, 1,428 kg, HP 6,070. PCU 225 both. Power 2 kW. Range 600 m (S) and 800 m (L); projectiles vanish between 640-800 m. Fire rate 600 RPM (the wiki notes Weapons.sbc says 700). Magazine 140 rounds, Gatling Ammo Box. Damage 90 to blocks, 33 to characters, 120 to a head. Traverse 110 deg/s (S), 82.51 deg/s (L). Feed port on the base; small grids have four side ports. Targets: characters, grids, stations, meteors, rockets, each Enemy or Neutral ([Gatling Turret](https://spaceengineers.wiki.gg/wiki/Gatling_Turret)).
- **Rocket Turret** (wiki name "Missile Turret" redirects here; Rocket Turret Type II DLC variant listed). Rockets only. S 5x5x5, 894 kg, HP 3,300, PCU 100. L 3x3x3, 1,826 kg, HP 6,960, PCU 275. Range 600 m / 800 m. 90 RPM; S fires 2 then 6 s reload, L fires 6 then 4 s reload. Power 2 kW ([Rocket Turret](https://spaceengineers.wiki.gg/wiki/Missile_Turret)).
- **Artillery Turret** (L only). 3x3x3, 14,224 kg, HP 62,120, PCU 275, build 100 s. Detection up to 800 m; shells fly up to 2,000 m. Shell: 17,000 penetration damage; requires uranium. 600 L inventory holds six shells. Two shots then 12 s reload (about 10 RPM overall). Traverse 11 deg/s, elevation -15 to 60 deg. Recoil 150 kN per shot. Shells are missiles, so turrets with Target Rockets can shoot them down. Ricochet chance 85% at 0-10 degrees, falling to 25% at 40 degrees, none past 40 ([Artillery Turret](https://spaceengineers.wiki.gg/wiki/Artillery_Turret)).
- **Interior Turret.** 1x1x1, 311 kg, HP 1,190, PCU 125. Range 600 m (detection); projectiles vanish between 480 and 600 m. Fires at hostiles by default (neutrals optional). Ammo MR-50A or 5.56x45 NATO magazines, hand-loaded (no conveyor port), up to 768 MR-50A or 1,919 legacy magazines in 384 L. Power 2 kW. Introduction update not stated ([Interior Turret](https://spaceengineers.wiki.gg/wiki/Interior_Turret)).
- **Custom Turret Controller.** S 1x1x2, 250 kg, HP 871, PCU 100, 10 s. L 1x1x1, 948 kg, HP 3,490, PCU 100, 24 s. Drives custom turrets built as a weapon or tool on a hinge and rotor. Azimuth and elevation multipliers default 9.0, angle deviation 5.0. Auto aim, target subsystems (weapons, propulsion, power), solar tracking. Custom turrets are bulkier and slower to build or repair (projectors cannot project rotor/hinge sub-grids). The page does not describe interaction with vanilla turret blocks ([Custom Turret Controller](https://spaceengineers.wiki.gg/wiki/Custom_Turret_Controller)).
- **Assault Cannon Turret, Autocannon Turret, Railgun, Rocket Launcher, Reloadable Rocket Launcher, Block Weapons, Firework Launcher.** Names only ([Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks)); not fetched.
- **Warhead** (S and L). 1x1x1. S 106.2 kg, HP 486, PCU 50, 10 s; L 700.4 kg, HP 2,932, PCU 100, 30 s. Small blast disintegrates small blocks in a 20-block (5 m) diameter; large blast disintegrates blocks of any size in a 12-block (30 m) diameter. Triggers once armed: collision with blocks or voxels, thruster plume, weapon fire, or chain reaction. Manual detonation can be immediate or on a countdown. Physical player contact does not trigger it. An armed warhead explodes from a single hit; an unarmed one is destroyed like a normal block. With Progression on, a refinery, assembler and reactor must be built before the warhead unlocks ([Warhead](https://spaceengineers.wiki.gg/wiki/Warhead)). Damage numbers and arming delay not given.
- **Decoy.** Name only. Lightning prefers decoy blocks within 50 m ([Weather](https://spaceengineers.wiki.gg/wiki/Weather)).

### 1.12 Automation and logic

- **Programmable Block** (category entry; no Prototech variant listed). C# scripts, runs on PC and dedicated servers only, not consoles. Script can read inventories, damage and positions that timers cannot. Runs on a loop or on an event (toolbar, button, sensor, timer, autopilot waypoint). One block per script. Scripts do not see added, removed or renamed blocks until Recompile. Full API facts in 2.21 ([Programmable Block](https://spaceengineers.wiki.gg/wiki/Programmable_Block)).
- **Timer Block.** 1x1x1. S 30.2 kg, HP 121, PCU 25, build 4 s. L 259 kg, HP 995, PCU 25, build 10 s. Delay 1 s to 1 h (H:M:S). Timers can start or stop each other, including themselves (loops). Up to 81 actions across nine toolbars. Cannot steer, use handheld tools, aim handheld weapons or place blocks. Power 0.1 W when on. Timers are "not accurate" with delays ([Timer Block](https://spaceengineers.wiki.gg/wiki/Timer_Block)).
- **Event Controller** (added with Automatons, Update 1.202; AND-gate behaviour changed before 1.203). 1x1x1. S 38.4 kg, HP 127, PCU 10, build 5 s, 500 W. L 304 kg, HP 1,080, PCU 10, build 12 s. Watches one of 22 events: altitude, angle changed, block added/removed, block integrity %, block on/off, cargo filled %, cockpit occupied, connector connected, connector ready to lock, distance to locked target, door opened, gas tank filled %, grid speed changed, landing gear locked, magnetic lock ready, merge block merged, natural gravity changed, piston position %, power output %, rotor/hinge attached/detached, stored power %, thrust %. Slot 1 fires on true, slot 2 on false ([Event Controller](https://spaceengineers.wiki.gg/wiki/Event_Controller)).
- **Button Panel family** (Button Panel, Button Panel Pedestal, Vertical Button Panel, Sci-Fi Four-Button Panel, Sci-Fi One-Button Terminal, Bridge Button Panel, Inset Button Panel); **Control Panel** and Control Panel Pedestal. Names only; not fetched.
- **LCD and text panels.** LCD Panel, Wide LCD Panel, Corner LCD, Curved LCD Panel, Sloped LCD Panel, Transparent LCD, Billboard LCD (and Round), Holo LCD, Sci-Fi LCD Panel 3x3 / 5x3 / 5x5, Console Module LCD, Text Panel. Names only.
- **Action Relay, Emotion Controller, Sound Block, Warning Signs, Symbols, Media Player, Jukebox.** Names only.
- **AI blocks (Automatons, Update 1.202, April 2023).** AI Flight (Move) steers and needs waypoints from other AI blocks. AI Basic (Task) is an extended Remote Control: follow player, travel home, autopilot. AI Recorder (Task) records and replays waypoints and hotbar actions; it does not keep beacon references when copied. AI Defensive (Combat) handles retreat and fallback. AI Offensive (Combat) handles targeting and attack patterns. Each is 1x1x1 in S and L. One of each type can be active per grid. Drone needs thrust in all six directions. Combat blocks activate when an enemy is within 2,500 m; Task blocks when none is. Source: Keen announcement excerpt via search ([Update 1.202](https://www.spaceengineersgame.com/update-1-202-automatons/); [AI Flight](https://spaceengineers.wiki.gg/wiki/AI_Flight), not fetched). Facts from the search summary are UNVERIFIED on the wiki pages.

### 1.13 Economy and world-service blocks

- **Safe Zone** (L only). Infobox 2x2x3, 25,945 kg, HP 92,925, PCU 50, build 40 s. Projects a protective field: 2-minute startup. Spherical (diameter) or cubical (H x W x L). Range minimum 10 m; maximum 1,000 m per the main page, but the construction text says 10 to 500 m (CONFLICT). Power draw 5 MW to 128 MW by size. Player-owned zones need one Zone Chip per real-life hour; NPC and admin zones don't. Filter by players, factions or grids, whitelist or blacklist. Permissions are all off by default (damage, shooting, drilling, welding, grinding, building, projections, voxel hands, lock, station conversion). Disabling grinding and welding also blocks hacking ([Safe Zone](https://spaceengineers.wiki.gg/wiki/Safe_Zone)). Safe Zone Type II exists.
- **Store** (L only). 1x1x1, 938 kg, HP 3,870, PCU 10, power -0.002 MW, build 25 s. Storage 1,000 L, conveyor-connectable. Buys and sells components, materials, tools, data pads and pre-built grids (NPCs sell grids; players buy them). Currency is space credits. Owner sets offers and orders; listing and transaction fees apply on larger amounts. "Anyone can use" and "Searchable" options. No automatic restock described ([Store](https://spaceengineers.wiki.gg/wiki/Store)).
- **Contracts Block** (L only; contracts from NPC Trading Outposts or players). 1x1x1, 938 kg, HP 3,870, PCU 10, power -0.002 MW, build 25 s. Reward is faction reputation and space credits. Abandoning a contract costs reputation and may forfeit collateral. Amounts not given. Needs a connected cargo container for goods ([Contracts Block](https://spaceengineers.wiki.gg/wiki/Contracts_Block)).
- **ATM** (L and stations only). 1x1x1, 642 kg, HP 2,710, PCU 10, power -0.002 MW, build 15 s. Personal space-credit deposit and withdrawal. Building it requires the Economy Deluxe Pack DLC. Use of other players' or NPC ATMs does not ([ATM](https://spaceengineers.wiki.gg/wiki/ATM)).
- **Services Terminal, Trading Outpost stations.** Services Terminal offers repairs, salvage and storage. Trading Stations spawn when Economy is on; goods depend on the owning faction type ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).

### 1.14 Storage, gas, venting, descent

- **Hydrogen Tank** (and Small Hydrogen Tank, Bulky Hydrogen Tank (Prosperity Pack cosmetic), Lab Hydrogen Tank variants). S 5x5x5, 1,580.8 kg, HP 6,254, PCU 25, build 30 s. L 3x3x3, 8,161.6 kg, HP 34,908, PCU 25, build 50 s. Capacity table CONFLICT/ambiguous: small tank reads as 500,000 L (S) and 15,000,000 L (L) on one reading, or 15,000 L and 1,000,000 L on the other; UNVERIFIED. Must be powered and conveyored. Stockpile ON: tank fills from generators or other tanks, and hydrogen thrusters and Medical Rooms cannot draw from it. Stockpile OFF: thrusters and Medical Rooms draw from it ([Hydrogen Tank](https://spaceengineers.wiki.gg/wiki/Hydrogen_Tank)).
- **Oxygen Tank** (and Small Oxygen Tank, Lab Small Oxygen Tank). Capacity not fetched. Connects to any-size conveyor ports (dual-size block per Conveyor system).
- **Air Vent** (and Air Vent Fan, Air Vent Full). 1x1x1 (both grid sizes). S 189 kg, HP 685, PCU 10. L 1,001 kg, HP 4,005, PCU 10. Power required. Pressurises a room from an oxygen source, or depressurises into tanks. Must be conveyored to Oxygen Tanks, Oxygen Farms or an O2/H2 Generator. Flow rate not stated ([Air Vent](https://spaceengineers.wiki.gg/wiki/Air_Vent)).
- **Parachute Hatch** (S and L). 1x1x1. S 84.2 kg, HP 316, PCU 50, build 10 s. L 472.4 kg, HP 1,847, PCU 50, build 22 s. Each hatch slows about 10,000 kg of grid mass (S) or 100,000 kg (L). Consumes canvas: 1 piece (S), 5 pieces (L) per deployment. Auto-deploys on planets with atmosphere and gravity; manual opening needs dense atmosphere and natural gravity. Less effective in thin atmosphere. Recommended landing speed under 8 m/s for small ships ([Parachute Hatch](https://spaceengineers.wiki.gg/wiki/Parachute_Hatch)).

### 1.15 Blocks and mechanics added in 2023 to 2026 (by update)

Dates from [SE1 roadmap](https://www.spaceengineersgame.com/roadmap/) and Keen announcements (via search summary where noted). Version numbers 1.206 to 1.208 come from the search summary of Keen's announcements list, not a fetched page.

- **1.202 Automatons (April 2023).** AI Flight, AI Basic, AI Recorder, AI Defensive, AI Offensive; Event Controller (see 1.12).
- **1.203 10th Anniversary (October 2023).** Event Controller AND-gate fix (wiki). Other block changes not retrieved.
- **1.204 Signal (May 2024), 1.205 Contact (October 2024).** Block changes not retrieved. An unofficial page claims broadcast and relay blocks (UNVERIFIED).
- **1.206 Fieldwork (April 2025), 1.207 Apex Survival (September 2025), 1.208 Core Systems (November 2025).** 1.207: suit O2 reservoir halved in exchange for Survival Buffs ([Player](https://spaceengineers.wiki.gg/wiki/Player)); meteor impacts no longer leave craters or ores ([Meteor](https://spaceengineers.wiki.gg/wiki/Meteor)). Block lists for 1.206 and 1.208 not retrieved.
- **1.209 Economy 2 (listed for May 2026 on the roadmap).** Store and redeploy whole grids; repair ships at trade stations without components; bounties and scrap selling for credits; NPCs inside trade stations ([SE1 roadmap](https://www.spaceengineersgame.com/roadmap/)). Live status not stated on the roadmap page.
- **1.210 Prosperity (27 July 2026, free content plus paid Prosperity Pack).** Free: Prototech O2/H2 Generator (L); Flat Collector (S); Structural Platform Conveyor (L); Short Stairs (L); Passage 2 Frame (L); Triangular Grated Catwalks (Decorative Pack 2); Ping Grid button; Story Datapads on Economy Stations; Factorum Global Encounter ([Prosperity](https://www.spaceengineersgame.com/prosperity/)). Paid Prosperity Pack ("$4.99 USD"): over 50 cosmetic blocks including sloped cockpits, bulky hydrogen tanks, stairs, battery banks, railings, server racks, cable runs and lights; Keen says they "don't affect gameplay balance".
- **1.210.014 hotfix (14 August 2026).** Reverted Wheel Propulsion Override change; fixed Flat Collector side conveyors; stations with sub-grids offset after reload ([SE1 Steam news](https://store.steampowered.com/feeds/news/app/244850/?cc=US&l=english&count=25)).
- **Warfare 3 (planned November/December 2026, Keen roadmap).** Guided missiles, heavy torpedoes, countermeasure flares, EMP weapons, jump detection ([SE1 roadmap](https://www.spaceengineersgame.com/roadmap/)). Steam reveal series in September and October 2026: "Artillery Turret, Jump Seat", "Jump Detection, Fighter Cockpit", "Automation Events, Warhead & Intake Blocks", "EMP Weaponry" ([SE1 Steam news](https://store.steampowered.com/feeds/news/app/244850/?cc=US&l=english&count=25)). Block-level detail not fetched.
- **Economy release (1.192.0).** Economy, space credits, trading stations, contracts. Not 2023 to 2026, but the base for 1.209 ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).

---

## 2. SYSTEMS

### 2.1 Power network rules

- Touching blocks conduct power; no cables are needed ([Power](https://spaceengineers.wiki.gg/wiki/Power)).
- Survival suits and all grids with functional blocks need power. In Creative, producers have infinite charge but limited throughput ([Power](https://spaceengineers.wiki.gg/wiki/Power)).
- HUD shows usage as a percentage. Below 99% is adequate. At 100% the bar turns red and the grid lacks power for the requested level ([Power](https://spaceengineers.wiki.gg/wiki/Power)).
- The page describes no priority or blackout sequence (UNVERIFIED that none exists).
- Unpowered grids can be removed by Trash Removal on multiplayer servers; the page advises setting batteries to Auto and adding renewable generation before logging off ([Power](https://spaceengineers.wiki.gg/wiki/Power)).
- Producers named: Large Reactor, Wind Turbine, Solar Panel, Hydrogen Engine, Prototech Fusion Reactor. Battery is the only storage named on the page ([Power](https://spaceengineers.wiki.gg/wiki/Power)). Output figures are in section 1.4.
- Battery modes and charge behaviour: section 1.4.

### 2.2 Conveyor network rules

- Transfer is "instantaneous" once the grid is powered and ports are connected. No throughput or speed numbers are given on the page ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Tubes show red (unpowered), yellow (unconnected at one or both ends), green (connected and powered) ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Progression: when enabled, a cargo container must be built to unlock conveyors ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Inventories on a grid are accessible from any inventory screen. Two grids joined by Connectors share a conveyor system that "respects ownership" ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Ejectors take only from inventories flagged as supplying requests; assembler materials and reactor fuel stay. Collectors take all floating items unless a Sorter and Ejector filter them ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Large tubes are 3x3 S-blocks wide; they struggle through 1- and 2-block gaps, and Conveyor Frames help ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).
- Page revision: oldid 39972, last edited 6 June 2026 ([Conveyor system](https://spaceengineers.wiki.gg/wiki/Conveyor_system)).

### 2.3 Ownership, factions, sharing

- Ownership applies to blocks with computers. Grid owner is whoever holds the majority of blocks (check the grid info screen). A block's owner is set when its computer components finish welding ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- Blocks owned by nobody are open to everyone ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- Sharing modes: No Share, Share with all, Share with Faction. Only the owner can change ownership or sharing settings ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- Transfer to "Nobody" is restricted to server admins ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- Hacking: taking ownership from another player or NPC faction without consent. Without hacking, AI turrets attack captured grids ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- NPC-owned blocks deny non-allies until hacked ([Ownership](https://spaceengineers.wiki.gg/wiki/Ownership)).
- Faction rules beyond sharing are not defined on the Ownership page. Who can reprogram or remove a shared block is not stated (UNVERIFIED).
- Faction limit: MaxFactionsCount default 0 (no limit) ([Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).
- Wolves and sabiroids belong to the NPC faction SPID, and players cannot lose reputation with it ([Wolf](https://spaceengineers.wiki.gg/wiki/Wolf); [Saberoid](https://spaceengineers.wiki.gg/wiki/Saberoid)).

### 2.4 Grid physics: mass, centre of mass, speed, dampeners

- **Centre of mass.** Gyroscopes rotate the grid about its centre of mass. Heavy gyros shift the centre of mass ([Gyroscope](https://spaceengineers.wiki.gg/wiki/Gyroscope)).
- **Inertial dampeners.** Simulate braking friction. When there is no input in a direction, opposite thrusters fire to cancel velocity. Z toggles them; Ctrl+Z toggles relative dampers. Full function needs at least one thruster facing each of the six directions. Gyroscopes "cannot alter a spacecraft's speed" ([Inertial Dampeners](https://spaceengineers.wiki.gg/wiki/Inertial_Dampeners)).
- **Sub-grid instability.** The wiki says ships with rotor, hinge or piston sub-grids can sink in gravity even with dampeners on, because "the game engine has issues calculating Inertial Dampeners accurately" ([Inertial Dampeners](https://spaceengineers.wiki.gg/wiki/Inertial_Dampeners)).
- **Max speed.** Official default not found. The session settings page has no ship speed setting; only AdjustableMaxVehicleSpeed (default true), which controls the speed-limit slider on suspension blocks ([Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).
  - Community and Keen-forum statements put the cap near 100 m/s: a forum-quoted Keen QA line that the game "is tuned to work with speeds up to 100 m/s", and a developer's claim that block physics do not calculate above 100 m/s. These are second-hand (search summary of Steam discussions), so the 100 m/s default is UNVERIFIED. A 2019 Steam discussion asks about 110 m/s (UNVERIFIED) ([Keen forum topic](https://support.keenswh.com/spaceengineers/pc/topic/24907-potential-increases-to-top-speed-limits-to-them) is a community thread with no staff reply).
  - Player speed is reported as 10 m/s above the top grid speed (UNVERIFIED, a Steam comment).
  - Mods such as Relative Top Speed raise the cap (reported); community config values of 500 m/s exist (UNVERIFIED, mod-specific).
- **Speed override path.** The officially documented setting is the suspension speed slider only (above). Changing top speed in vanilla is not documented on the wiki. Mods are the reported route (UNVERIFIED).
- **Mass and thrust.** Thrust-to-weight is the design lever; no official formula found. The Atmospheric Thruster page suggests about one large atmospheric thruster per 180,000 kg of lift as a player estimate (UNVERIFIED, forum).

### 2.5 Thruster effectiveness versus atmospheric density

- **No numeric efficiency curve was found on the official wiki.** Thruster mechanics: "Ion Thrusters and Atmospheric thrusters Force output changes with altitude." Also: "do not expect to get maximum force out of them in an atmosphere above 3,000 meters." Thrusters "help fight against planetary gravity" ([Thruster mechanics](https://spaceengineers.wiki.gg/wiki/Thruster_mechanics)).
- **Legacy per-planet metrics (v1.203, 2023, marked possibly outdated).** Effectiveness on Earthlike at surface level ([Thruster Comparison](https://spaceengineers.wiki.gg/wiki/Thruster_Comparison)):

| Thruster | Grid | Effectiveness (Earthlike surface) | Max lift (kg) |
|---|---|---|---|
| Atmospheric Thruster | S / L | 100% | 9,796 / 66,122 |
| Flat Atmospheric Thruster | S / L | 100% | 3,265 / 20,408 |
| Large Atmospheric Thruster | S / L | 100% | 58,776 / 661,224 |
| Large Flat Atmospheric Thruster | S / L | 100% | 23,469 / 265,306 |
| Hydrogen Thruster | S / L | 100% | 10,041 / 110,204 |
| Large Hydrogen Thruster | S / L | 100% | 48,980 / 734,694 |
| Ion Thruster | S / L | 20% | 294 / 7,053 |
| Large Ion Thruster | S / L | 20% | 3,527 / 88,163 |

- The same page has per-planet data only for Earthlike ("More data at Thruster Comparison" is circular). No per-planet thrust figures for Mars, Alien, Titan, Pertam, Triton or the Moon were found (UNVERIFIED for those bodies).
- **Community estimate (conflicts with the legacy table).** A Steam forum post puts a small atmospheric thruster at about 90% (about 88 kN) at sea level, falling to zero by about 10 km (UNVERIFIED, forum, not fetched).
- **Role by type.** Atmospheric thrusters are "the most efficient source of thrust in an atmosphere, better than hydrogen thrusters", use power only, and "slowly lose all thrust" in transition to space ([Atmospheric Thrusters](https://spaceengineers.wiki.gg/wiki/Atmospheric_Thrusters)). Hydrogen thrusters are "equally strong in vacuum and atmospheres" but need voluminous tanks ([Thruster](https://spaceengineers.wiki.gg/wiki/Thruster)). Ion thrusters are best in vacuum, with no fuel, power only ([Ion Thruster](https://spaceengineers.wiki.gg/wiki/Ion_Thruster)).
- A search excerpt of the Atmospheric Thrusters page says usable planets depend on atmospheric thickness, not oxygen (UNVERIFIED wording).
- **Ion thruster in atmosphere.** Thruster mechanics also says ion force changes with altitude, but no numbers are given. Legacy metric: 20% on Earthlike surface.

### 2.6 Natural gravity on planets: formula and falloff

**Parameters** (PlanetGenerator definition, [PlanetGenerator Definition](https://spaceengineers.wiki.gg/wiki/Modding/Reference/SBC/PlanetGenerator_Definition)):

- SurfaceGravity default 1 (page: "gravity acceleration in m/s²" across the terrain band; see unit conflict below).
- GravityFalloffPower default 7 (saved as GravityFalloff in the world file).
- LowestTerrain = PlanetRadius + HillParams.Min × PlanetRadius; HighestTerrain = PlanetRadius + HillParams.Max × PlanetRadius. Wiki example: radius 100 km, Min -0.01, Max 0.12 gives 99 km and 112 km.
- Gravity between LowestTerrain and HighestTerrain is always SurfaceGravity.

**Formulas (verbatim from the wiki):**

- Outside the terrain band: G = (DistanceToCenter / HighestTerrain) ^ (0 - GravityFalloffPower); Acceleration = SurfaceGravity × G.
- Underground (below LowestTerrain): G = DistanceToCenter / LowestTerrain, with G floored at 0.01; Acceleration = SurfaceGravity × G.
- Maximum reach: MaxGravityReach = HighestTerrain × ((SurfaceGravity / 0.05) ^ (1 / GravityFalloffPower)). The 0.05 is a fixed constant. Reading the formula as "the distance where pull falls to 0.05 (in surface-gravity units)" is R1's own interpretation, not stated on the page.

Source: [PlanetGenerator Definition](https://spaceengineers.wiki.gg/wiki/Modding/Reference/SBC/PlanetGenerator_Definition). Caveat from that page: SurfaceGravity and GravityFalloffPower are saved into the world file, so editing the definition does not change planets already spawned; respawn is needed.

**Worked numbers (R1 arithmetic from the formula with P = 7 and Sg = 1, not game-verified):**

| d / HighestTerrain | Gravity (× SurfaceGravity) |
|---|---|
| 1.00 | 1.000 |
| 1.25 | 0.210 |
| 1.50 | 0.0585 |
| 2.00 | 0.0078 |
| 3.00 | 0.00046 |

- Reach with the wiki's example (HighestTerrain 112 km, Sg = 1, P = 7): 112 × 20^(1/7) = about 171.8 km from centre, about 60 km above the highest terrain.
- The page's own example and any values above assume the Sg units. Units CONFLICT: the page says m/s², but planet presets are given in multiples of g (Earthlike "1.0 g", [EarthLike Planet](https://spaceengineers.wiki.gg/wiki/Earth_Planet)). If Sg is in g, the 0.05 cut-off is 0.05 g. UNVERIFIED which unit the game uses.
- The community formula g = b × (MaxR / r)^7 (Keen support forum) matches P = 7 but its freefall test was about four times higher than predicted (UNVERIFIED; [Keen forum](https://support.keenswh.com/spaceengineers/pc/comments/40272/vote/toggle)).
- The Gravity page says gravity "stays constant in a wide area that includes the surface", falls toward zero at a distance, and "gradually decreases towards zero g the closer you dig to the core" ([Gravity](https://spaceengineers.wiki.gg/wiki/Gravity)). Asteroids exert no perceivable natural gravity ([Gravity](https://spaceengineers.wiki.gg/wiki/Gravity)).

### 2.7 Artificial gravity and how it combines with natural gravity

- Planar generator: uniform acceleration inside its box, no distance falloff, direction set by the setting ([Gravity](https://spaceengineers.wiki.gg/wiki/Gravity)).
- Spherical generator: centred on the block; flips at a focal point ([Gravity Generator](https://spaceengineers.wiki.gg/wiki/Gravity_Generator)).
- On planets, usable artificial strength is capped at 50% of the artificial value ([Gravity](https://spaceengineers.wiki.gg/wiki/Gravity)). Planetary gravity weakens the artificial field, so generators are mostly used in space ([Gravity Generator](https://spaceengineers.wiki.gg/wiki/Gravity_Generator)).
- Artificial Mass blocks make a grid respond to gravity fields; effect scales with AM block count ([Artificial Mass](https://spaceengineers.wiki.gg/wiki/Artificial_Mass)).
- How natural and artificial gravity sum (vector sum, any cap) beyond the 50% statement is not documented here (UNVERIFIED).

### 2.8 Damage, deformation and block integrity

- Each block has hit points equal to the sum of its components' health; more material means more HP ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Functional line: blocks above it work; below it they stop providing their function. Damaged blocks can be repaired with a welder ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- At zero HP a block breaks beyond repair, its components become Scrap Metal, and neighbours start to come loose ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Damage sources: collisions (ships, rovers, voxels, characters), explosions (shielded by neighbours with enough HP), weapons (direct, penetration, explosive, ricochet), voxel damage (drills, meteors, explosions, collisions) ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Bonus damage: "a block built to 80% will take +20% more damage from all sources." The bonus does not grow as damage accumulates ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Penetration example: a 6,000 penetration projectile destroys two 2,500 HP armor blocks and deals 1,000 to a third ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- HP examples: Construction Comp 30 HP (8 kg); Steel Plate 100 HP (20 kg); Prototech Frame 1,000 HP (100 kg) ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Deformation: "Damaged armor blocks deform and allow damage to neighbouring blocks" ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)). A 2018 developer post (dated, not rechecked) says armor has a hidden deformation HP pool and there is no damage reduction or deflection, only more or fewer HP (developer post URL not retained; search summary; UNVERIFIED for current game).
- Collision-speed thresholds: no official figures found. Anecdotal: 100 m/s impacts into terrain did no damage with a mod (UNVERIFIED, mod-specific).
- Player impacts above about 20 m/s can be fatal ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Radiation damages players once exposure reaches 75% ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).
- Explosive radii: dropped explosives 1 cm per explosive in the stack; rockets 4 m ([Damage Mechanics](https://spaceengineers.wiki.gg/wiki/Damage_Mechanics)).

### 2.9 Build stages, components, welding and grinding

- Components are crafted from refined materials in an Assembler ([Component](https://spaceengineers.wiki.gg/wiki/Component)).
- Build stages (per-stage material costs and percentage) were not retrieved; the Welding page returned 404 (UNVERIFIED).
- Welder range, grinder range and building rules: section 1.8.
- Projector-based building: section 1.8 and 2.9 (the projected blocks are unbuilt until welded; sub-grids are not projected).
- Blueprints: section 1.8.
- Build time per block is in each block's infobox (for example Survival Kit 20 s S, Medical Room 50 s).

### 2.10 Voxel terrain and ore deposits

- Ores are voxel materials. Yield per unit is set by the material's minedoreratio. All deposits of the same ore give the same amount ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).
- Planetary deposits appear in groups of two or three and spread in horizontal layers. Certain ores occur more often at certain depths (depth chart is an image; values not read) ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).
- Co-occurrence: cobalt with iron and nickel; magnesium with nickel and silicon; gold with iron and silver ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).
- Ore spawn is not close to Trading Outposts ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).
- Hand drill: left-click mines stone, ore and ice; right-click digs voxel material and destroys ores it meets ([Hand Drill](https://spaceengineers.wiki.gg/wiki/Hand_Drill)).
- Deposits on planets look like discoloured patches; easiest to see on ice or sand plains ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).
- Ore patch positions per body: section 2.23 for category-level proportions.

### 2.11 Ore types, minedoreratio and refinery yields

**Ore ratios (minedoreratio) from the Ore page:** Iron 5; Ice 5; Nickel 3; Silicon 3; Cobalt 3; Magnesium 3; Silver 1; Gold 1; Platinum 1; Uranium 0.3 ([Ore](https://spaceengineers.wiki.gg/wiki/Ore)).

**Refinery yields (Refinery page, cross-checked with Ore page where marked):**

| Ore | Output | Refinery yield | Basic Refinery yield | Source |
|---|---|---|---|---|
| Iron | Iron Ingot | 70% (text: 1,000 kg gives 700 kg) | 49% (table) | Refinery, Basic Refinery, Ore |
| Silicon | Silicon Wafer | 70% | 49% | Refinery, Ore |
| Nickel | Nickel Ingot | 40% | 28% | Refinery, Ore |
| Cobalt | Cobalt Ingot | 30% | 21% | Refinery, Ore |
| Silver | Silver Ingot | 10% | Not available | Refinery, Ore |
| Gold | Gold Ingot | 1% | Not available | Refinery, Ore |
| Uranium | Uranium Ingot | 1% (text: 1,000 kg gives 10 kg) | Not available | Refinery (text), Ore |
| Platinum | Platinum Ingot | 0.5% | Not available | Refinery, Ore |
| Magnesium | Magnesium Powder | 0.7% | 0.49% | Refinery, Basic Refinery, Ore |
| Scrap Metal | Iron Ingot | 80% | 56% (Ore page) | Refinery, Ore |
| Ice | not refined; O2/H2 Generator | n/a | n/a | Ore; O2 H2 Generator |
| Stone | Gravel, Silicon, Nickel, Iron | CONFLICT (see below) | n/a | Survival Kit; Refinery table (garbled) |

- **Stone CONFLICT.** The Survival Kit page: per 100 kg of stone, 3.0 Iron Ingots, 0.24 Nickel Ingots, 0.4 Silicon Wafers, 1.4 Gravel ([Survival Kit](https://spaceengineers.wiki.gg/wiki/Survival_Kit)). The Refinery table (parsed by summariser) gives Gravel 3%, Silicon 1.4%, Nickel 0.4%, Iron 0.24%. The Ore page gives "1.40% / 0.98% / 1.40%" for stone-derived items. Treat the stone row as UNVERIFIED until checked against the game.
- Refinery speed: 1.3 base (with 560 kW). Basic Refinery 0.65 (section 1.3).
- Refinery's basic yield is 0.7 × Refinery yield in every row above (R1 observation).

### 2.12 Components and recipes

- Recipe amounts on the wiki are written as "kg" of ingot material. The conversion from kg of ingot to item count is not verified in this pass (UNVERIFIED).

| Component | Recipe (wiki) | Craft time | Component mass / volume | Source |
|---|---|---|---|---|
| Steel Plate | Iron Ingot 21 kg | 1 s | 20 kg / 3 L | [Steel Plate](https://spaceengineers.wiki.gg/wiki/Steel_Plate) |
| Interior Plate | Iron Ingot 3 kg | 1 s | not fetched (Survival Kit builds it) | [Interior Plate](https://spaceengineers.wiki.gg/wiki/Interior_Plate) |
| Construction Component | Iron Ingot 8 kg | 1 s | 8 kg / 2 L | [Construction Component](https://spaceengineers.wiki.gg/wiki/Construction_Component) |
| Motor | Iron Ingot 20 kg, Nickel Ingot 5 kg | 3 s | 24 kg / 8 L | [Motor](https://spaceengineers.wiki.gg/wiki/Motor) |
| Computer | Iron Ingot 0.5 kg, Silicon Wafer 0.2 kg | not stated | 0.2 kg / 1 L | [Computer](https://spaceengineers.wiki.gg/wiki/Computer) |

- Survival Kit can build: Computer, Construction Comp, Display, Girder, Interior Plate, Motor, Solar Cell, Steel Plate ([Survival Kit](https://spaceengineers.wiki.gg/wiki/Survival_Kit)).
- The Component table page lists 30 components with ingredient columns (Cobalt, Gold, Iron, Magnesium Powder, Nickel, Platinum, Silicon Wafer, Silver, Gravel, Prototech Scrap). Amounts did not parse reliably. Names: Bulletproof Glass, Computer, Construction Comp, Detector Comp, Display, Explosives, Girder, Gravity Comp, Interior Plate, Large Steel Tube, Medical Comp, Metal Grid, Motor, Power Cell, Radio-comm Comp, Reactor Comp, Small Steel Tube, Solar Cell, Steel Plate, Superconductor, Thruster Comp, Zone Chip, Prototech components ([Component](https://spaceengineers.wiki.gg/wiki/Component)).
- Missile Components and Canvas recipes were not on that page (UNVERIFIED).
- Not fetched or not parsed: Girder, Metal Grid, Bulletproof Glass, Display, Large/Small Steel Tube, Reactor Comp, Thruster Comp, Gravity Comp, Medical Comp, Radio-comm Comp, Detector Comp, Explosives, Superconductor, Power Cell, Solar Cell, Zone Chip, Canvas, Missile Components.
- Basic Assembler cannot make Explosives, Thruster Comp, Superconductors, Reactor Comp, Gravity Comp (section 1.3).

### 2.13 Oxygen, pressurisation and airtightness

- Oxygen supports the engineer's suit and fills airtight rooms. It moves through large and small conveyors ([Oxygen](https://spaceengineers.wiki.gg/wiki/Oxygen)).
- Sources: O2/H2 Generator (ice), Oxygen Farm (0.03 L/s, sunlight only), Trading Outposts sell oxygen ([Oxygen](https://spaceengineers.wiki.gg/wiki/Oxygen); [Oxygen Farm](https://spaceengineers.wiki.gg/wiki/Oxygen_Farm)).
- Airtight room: interior fully enclosed by airtight faces. Planet and asteroid voxels are not airtight. Closed cockpits count as single-block airtight interiors. Blocks still under construction are never airtight ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- Air vent: at least one, powered and conveyored to oxygen; more vents pressurise faster ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- Airtightness is per grid; a sub-grid parked in a pressurised room is not pressurised with it ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- Auto-healing: player health slowly recovers in pressurised rooms if enabled ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- Airlock: two closed sliding doors back to back; room loses "only two blocks' worth of oxygen" ([Airtightness](https://spaceengineers.wiki.gg/wiki/Airtightness)).
- Flow rates, room volume numbers and pressurisation time: not given on the page (UNVERIFIED).
- Hydrogen: only generator name and tank use (not a room gas) ([Oxygen](https://spaceengineers.wiki.gg/wiki/Oxygen)).

### 2.14 Character

- Suit life support covers power and oxygen; suit jetpack uses hydrogen ([search summary citing wiki pages; UNVERIFIED]).
- Suit power at 20% or less is "critical". At 0%, handheld tools and life support stop working ([Player](https://spaceengineers.wiki.gg/wiki/Player)).
- Suit O2 reservoir halved in 1.207 in exchange for Survival Buffs ([Player](https://spaceengineers.wiki.gg/wiki/Player)).
- Hydrogen reservoir: 125 L on the suit and a 400 L Hydrogen Bottle (search summary, UNVERIFIED; not in Player page text).
- Backpack inventory: 400 L under Realistic settings ([Player](https://spaceengineers.wiki.gg/wiki/Player)).
- Jetpack thrust and walking speed: not stated on the page (UNVERIFIED).
- Health reaches 0 means death. Hunger at 0% reduces health over time ([Player](https://spaceengineers.wiki.gg/wiki/Player)).
- Respawn: at the nearest faction-owned Survival Kit or Medical Room (own, if factionless). Depending on the Autorespawn setting, a list of respawn points may appear, including starter Drop Pods. Dead players drop the backpack. Backspace forces a respawn without inventory ([Player](https://spaceengineers.wiki.gg/wiki/Player)).
- Hand tools: Hand Drill tiers (wiki):

| Tier | Speed (infobox) | Speed (usage table) | Reach | Harvest |
|---|---|---|---|---|
| Standard | 1.0 | 1.0 | 1.0 | 1.0 |
| Enhanced | 1.5 | 1.2 | 1.2 | 1.0 |
| Proficient | 2.0 | 1.4 | 1.4 | 1.2 |
| Elite | 5.0 | 1.4 | 1.6 | 1.4 |

(CONFLICT: Elite and Enhanced speed differ between infobox and usage table. Source: [Hand Drill](https://spaceengineers.wiki.gg/wiki/Hand_Drill).) Welder tiers match the drill (search summary, UNVERIFIED).
- Hand tools (welder, grinder) drain suit energy; the grinder drains faster ([search summary, UNVERIFIED]).
- The Ore Detector has a handheld counterpart: the Hand Drill displays ore signals on the HUD ([Ore Detector](https://spaceengineers.wiki.gg/wiki/Ore_Detector)).

### 2.15 Economy, factions and trade

- Economy is a free feature added in Version 1.192.0. Enable it in World Settings to spawn Trading Stations ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).
- Free features: player-to-player trade (multiplayer only), inter-faction reputation, space credits, economy blocks (contracts, trade), Safe Zones, Services Terminals ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).
- Economy tick: default 10 minutes (EconomyTickInSeconds). Each tick recalculates prices, restocks Store blocks, refreshes contracts, pulls reputation toward neutral, and gives NPC factions money ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).
- Item prices use a hidden minimal price from crafting inputs plus assembly and refine time ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).
- Factions and stations are generated once; to customise, disable Enable Economy, Random Encounters, Cargo Ships, Planetary Encounters, and set Global Encounters Cap to 0 ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).
- Economy 2 (1.209) adds grid store and redeploy, ship repair at stations, bounties, scrap selling ([SE1 roadmap](https://www.spaceengineersgame.com/roadmap/)).
- Economy Deluxe Pack DLC required to build an ATM ([ATM](https://spaceengineers.wiki.gg/wiki/ATM)).

### 2.16 NPCs and encounters

- Encounters are enabled in World Settings. They spawn randomly in a radius around active engineers on suitable ground. Hostile or neutral signals (blinking) mark them. Challenges include traps, drones and AI-controlled turrets. Some have loot; some are lore ([Encounters](https://spaceengineers.wiki.gg/wiki/Encounters)).
- Damaging NPC-faction ships or buildings lowers reputation with that faction ([Encounters](https://spaceengineers.wiki.gg/wiki/Encounters)).
- Categories: SPRT Space Pirates; Unknown Signals (Steam/PC only); Global Encounters (7 pages, includes Unidentified Signals); Planetary Encounters (12 pages); Cargo Ships (no details); Random Encounters (31 pages, includes Old Random Encounters); Player-made Encounters ([Encounters](https://spaceengineers.wiki.gg/wiki/Encounters)).
- Factorum Global Encounter (Prototech, strong defences) added in 1.210 ([Prosperity](https://www.spaceengineersgame.com/prosperity/)).
- Spawn frequencies, cargo amounts, and pirate drone behaviour not fetched (UNVERIFIED).
- Automatons encounters: not mentioned on the encounter index page.
- Global encounters cap: default 1 on the Star System template ([Economy](https://spaceengineers.wiki.gg/wiki/Economy)).

### 2.17 Weather

- Setting: WeatherSystem default true ("automatic weather generation on planets") ([Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)). WeatherLightingDamage default false ([Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).
- Types (each light and heavy): Rain, Thunderstorm, Snow, Sandstorm/dust storm, Electrical storm, Fog, Hailstorm, Toxic haze ([Weather](https://spaceengineers.wiki.gg/wiki/Weather)).
- Effects: health damage from hail (small over time), lightning (high explosive damage to a player or a random grid block), temperature and oxygen shifts for suit life support, radiation changes; wind turbine output rises or falls; thrusters unaffected; solar panels, Oxygen Farms and Algae Farms lose or gain efficiency; fog and storms reduce visibility ([Weather](https://spaceengineers.wiki.gg/wiki/Weather)).
- Lightning protection ranges: trees 20 m; decoys 50 m; antennas 10 m (L) and 2 m (S) ([Weather](https://spaceengineers.wiki.gg/wiki/Weather)).
- Weather durations, occurrence chances and lightning damage values: not given (UNVERIFIED).
- Planet specifics: Moon has no atmosphere and no weather. Pertam and Triton have little oxygen in valleys and low plains. Alien has caustic rain and toxic fog ([Weather](https://spaceengineers.wiki.gg/wiki/Weather)).

### 2.18 Meteors

- Meteor Activity (session EnvironmentHostility, default NORMAL): SAFE (never), NORMAL (occasional small numbers), CATACLYSM (more often), CATACLYSM_UNREAL (presumably the "Armageddon" option; mapping inferred; continuous) ([Meteor](https://spaceengineers.wiki.gg/wiki/Meteor); [Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).
- Meteors appear in space and on planets. On Earthlike they burn up and do not reach the ground. They spawn near players but are not aimed at players or grids ([Meteor](https://spaceengineers.wiki.gg/wiki/Meteor)).
- Damage to grids and players is rare. Since 1.207, impacts leave no crater, no voxel deformation and no mineable ores; before, impacts damaged grids and craters held ores ([Meteor](https://spaceengineers.wiki.gg/wiki/Meteor)).
- Frequency, size and damage numbers not given (UNVERIFIED).

### 2.19 Wildlife

- Wolves: hostile to players, native to the EarthLike Planet, spawn alone or in packs of 2 or 3 near players, flee below a health threshold or after a pack member dies, lunge at range. Mammal Meat drop. Enabled by default in Star System survival worlds, can be disabled in World Settings ([Wolf](https://spaceengineers.wiki.gg/wiki/Wolf)). Health and damage numbers not stated.
- Sabiroids: car-sized, six-legged, hostile. Native to Alien and Titan; rare on Pertam (brown variant established). Spawn from sand tunnels in groups of two or three; can burrow into bases. Drop Insect Meat. Enabled in Star System survival worlds; can be disabled ([Saberoid](https://spaceengineers.wiki.gg/wiki/Saberoid)). Health and damage numbers not stated.- Spiders: "if Spiders is enabled in world settings, green Sabiroids inhabit Titan" (Titan page). Pertam: brown and black spiders very rare if enabled (Pertam page). See 2.23.
- Planets with no fauna: Moon, Europa, Mars ("No flora or fauna"), Triton ([Mars](https://spaceengineers.wiki.gg/wiki/Mars); [Moon](https://spaceengineers.wiki.gg/wiki/Moon); [Europa](https://spaceengineers.wiki.gg/wiki/Europa); [Triton](https://spaceengineers.wiki.gg/wiki/Triton)).

### 2.20 Respawn, safe zones, PvP

- Respawn: see 2.14. Session EnableAutorespawn default true ("automatic respawn at nearest available respawn point") ([Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).
- Safe Zone PvP protection and upkeep: section 1.13. Safe Zone "can help limit offline raiding damage" around a station ([Safe Zone](https://spaceengineers.wiki.gg/wiki/Safe_Zone)).
- Friendly-fire and PvP settings beyond the safe zone: not fetched (UNVERIFIED).

### 2.21 Programmable block API essentials

- Language: C# scripts, run on PC and dedicated servers only ([Programmable Block](https://spaceengineers.wiki.gg/wiki/Programmable_Block)).
- Runtime budget: 60 ticks per second, 16.66667 ms per tick. "1 ms is very slow" (page). Instruction limit exists as MaxInstructionCount, a safeguard against infinite loops; no numeric value given. Advice: use coroutines or split tasks ([The Runtime](https://spaceengineers.wiki.gg/wiki/Scripting/The_Runtime)).
- Run triggers: by default runs only when told (terminal button or toolbar); UpdateFrequency lets it self-run at intervals. The option values (Update1, Update10, Update100, Once) were not extracted (UNVERIFIED for exact names) ([The Runtime](https://spaceengineers.wiki.gg/wiki/Scripting/The_Runtime)).
- IGC (Inter-Grid Communication) via the `IGC` property of the Program class, type IMyIntergridCommunicationSystem ([IGC](https://spaceengineers.wiki.gg/wiki/Scripting/Antenna_Communication_(IGC))).
  - Broadcast: IGC.SendBroadcastMessage(tag, data, optional TransmissionDistance). Options: AntennaRelay (default), CurrentConstruct, ConnectedConstructs. Receivers must open a channel named by the tag.
  - Unicast: IGC.SendUnicastMessage(targetID, tag, argument). All unicast messages arrive on one channel; check tags yourself.
  - Messages carry tag, data, and source (for replies). Data must be an allowed type; string is easiest; others allowed (full type list on a separate page, not fetched).
  - Receiving: check HasPendingMessage and AcceptMessage(); unaccepted messages are lost when queue drains. "Enable Broadcast" must be on to receive broadcasts.
  - Range: an antenna can receive at a lower transmit range, but its messages will not reach beyond its own transmit range. The grid must be within antenna range.
  - Message size limits, rate limits, and the full allowed-type list were not found (UNVERIFIED).
- API references named by the wiki: Whitelist Overview (the allowed surface), a community-hosted API index at https://malforge.github.io/spaceengineers/pbapi, and a MDK page ([Scripting](https://spaceengineers.wiki.gg/wiki/Scripting)). The wiki does not link Keen's official scripting reference (per the fetch). IMyGridTerminalSystem is a standard name but is not on the pages read.
- Scripts do not detect block additions, removals or renames until you click Recompile ([Programmable Block](https://spaceengineers.wiki.gg/wiki/Programmable_Block)).
- Admins may punish or ban "badly written" performance-heavy scripts ([Programmable Block](https://spaceengineers.wiki.gg/wiki/Programmable_Block)).
- SE2 scripting: see section 3.

### 2.22 Limits (session settings)

Source: [Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings). Defaults apply only where a setting is missing from a world; a template's sandbox_config.sbc gives actual values.

| Setting | Default | Meaning |
|---|---|---|
| BlockLimitsEnabled | NONE | Mode for type and PCU limits (NONE disables them, and MaxGridSize and MaxBlocksPerPlayer) |
| MaxGridSize | 50,000 blocks | 0 = no limit |
| MaxBlocksPerPlayer | 100,000 blocks | 0 = no limit |
| TotalPCU | 600,000 | Meaning depends on BlockLimitsEnabled |
| BlockTypeLimits | per type, e.g. Assembler 24, Refinery 24, ShipWelder 100, ShipGrinder 150 | |
| LimitBlocksBy | BlockPairName | Other option: Tag |
| MaxFactionsCount | 0 | 0 = no limit |
| MaxPlayers | 4 | Dedicated server GUI 2 to 64; player-hosted worlds capped at 8 |
| MaxPlanets | 99 | Planet types, not individual planets |
| EnableSunRotation | true | Day/night cycle on planets |
| SunRotationIntervalMinutes | 120 | Minutes per full sun rotation |
| WeatherSystem | true | Automatic weather |
| WeatherLightingDamage | false | Lightning damage on or off |
| EnvironmentHostility (Meteor Activity) | NORMAL | SAFE, NORMAL, CATACLYSM, CATACLYSM_UNREAL |
| EnableAutorespawn | true | Respawn at nearest point |
| AdjustableMaxVehicleSpeed | true | Speed slider on suspension blocks (no general ship speed setting found) |

- Economy tick: EconomyTickInSeconds default 10 minutes (section 2.15).
- No gravity-strength setting on this page; gravity comes from planet generator definitions (section 2.6).

### 2.23 Planets and moons

**Star System (default scenario).** Eight bodies (planets and moons). Bodies do not spin and do not move on orbits ([Planets](https://spaceengineers.wiki.gg/wiki/Planets)). The system centre is GPS 0:0:0. Distances from the origin: EarthLike 0.86 m (page value), Moon 178.26 km, Mars 1,934.09 km, Europa 1,858.15 km, Triton 2,478.13 km, Pertam 4,040.87 km, Alien 5,734.07 km, Titan 5,800.92 km ([Planets](https://spaceengineers.wiki.gg/wiki/Planets)). Planet-moon distances: EarthLike-Moon 178.26 km; Mars-Europa 162.85 km; Alien-Titan 149.38 km ([Planets](https://spaceengineers.wiki.gg/wiki/Planets)).

| Body | Type / parent | Diameter | Surface gravity | Atmosphere (sea level) | Breathable / O2 | Ores (share of surface, order) | Weather and hazards | Fauna |
|---|---|---|---|---|---|---|---|---|
| EarthLike | Planet; moon: small grey Moon | 120 km | 1.0 g | 1 atm; height 14.4 km; thick, heavily clouded | Breathable; high oxygen | About 3.5%; silicon, magnesium, iron, cobalt, nickel, gold, silver; ice in lakes and underground | Fog, rain, thunderstorms in moderate zones; snowstorms and fog at poles; sandstorms in deserts | Wolves ([EarthLike](https://spaceengineers.wiki.gg/wiki/Earth_Planet)) |
| Moon | Moon of EarthLike | 19 km | 0.25 g | None; no clouds | No atmosphere; carry oxygen | About 1.4%; nickel, iron, silicon, ice, magnesium, platinum, gold, cobalt, silver | Very high radiation; freezing; steep craters and hills | None ([Moon](https://spaceengineers.wiki.gg/wiki/Moon)) |
| Mars | Planet; moon: Europa | 120 km | 0.9 g | 1 atm (page); height 14.4 km; "very thin, no oxygen" | Not breathable (no oxygen) | 4.1%; ice, silicon, magnesium, iron, nickel, gold, silver, cobalt | Storms, dust storms; fog and snowstorms at poles; always freezing; deep trenches; max gravity altitude 39,311 m; parachutes do not help | None; solar better than wind ([Mars](https://spaceengineers.wiki.gg/wiki/Mars)) |
| Europa | Moon of Mars | 19 km | 0.25 g | 1 atm (page); height 1.15 km; oxygen "None" | Not breathable | Entire surface is ice; 1.4% mineral deposits | Very high radiation; heavy fog or snowfall; freezing; rovers struggle | None ([Europa](https://spaceengineers.wiki.gg/wiki/Europa)) |
| Alien | Planet; moon: Titan | 120 km (Planets page) | 1.1 g (Planets page) | Caustic rain and toxic fog (Weather page) | Not fetched | Not fetched | Caustic rain; toxic fog | Sabiroids native (Saberoid page). Alien page 404 (UNVERIFIED for atmosphere, ores, gravity source) |
| Titan | Moon of Alien | 19 km | 0.25 g; gravity to 3 km altitude | 1 atm (page); height 0.6 km; no oxygen | Helmet needed; air "thick enough" for wind turbines; air vents do not make oxygen from the atmosphere | Nickel, iron, silicon, ice (lakes and underground), magnesium, platinum, gold, cobalt, silver; about 1.4% | Freezing; dust and sandstorms; thin cloud; rare fog | Green Sabiroids if Spiders enabled ([Titan](https://spaceengineers.wiki.gg/wiki/Titan)) |
| Pertam | Planet; no moon | 60 km | 1.2 g (heaviest in the list) | 1 atm (page); height 1.6 km | Breathable with medium oxygen | 0.3%; iron, nickel, silicon, cobalt, magnesium, gold, silver; ice underground only | Electric lightning storms; sandstorms with extreme heat; thin air at altitude; dust storms common | No fauna; if Spiders enabled, brown and black spiders very rare ([Pertam](https://spaceengineers.wiki.gg/wiki/Pertam)) |
| Triton | Planet; no moon | 80 km | 1.0 g | 1 atm (page); height 3.9 km | Breathable with clouds; oxygen in valleys | About 3.5%; silicon, iron, nickel, magnesium, gold, cobalt, silver; ice in lakes | Freezing; snowstorms; fog | None ([Triton](https://spaceengineers.wiki.gg/wiki/Triton)) |

- The Planets page gives gravity in g only, and no atmosphere values. Surface gravity in m/s² is not on any body page; 1.0 g is "about 9.8 m/s²" per the EarthLike page.
- Atmosphere density is given only as "1 atm" at sea level, with heights. No pressure or density curve for altitude was found. Thruster and wind data for the bodies are in 2.5 and 1.4.
- Per-body ore lists are category-level on the Planets page: Planets (Ice, Cobalt, Gold, Silver, Magnesium, Nickel, Silicon, Iron, Stone; no Platinum or Uranium); Moons (Ice, Platinum, Cobalt, Gold, Silver, Magnesium, Nickel, Silicon, Iron, Stone; no Uranium); Asteroids (not on that page) ([Planets](https://spaceengineers.wiki.gg/wiki/Planets)). Per-body entries above (from body pages) are more specific but conflict with the category-level list on Uranium/Platinum for some planets. UNVERIFIED for uranium deposits.
- Day/night: sun blocks half of each cycle; default cycle 120 minutes ([Solar Panel](https://spaceengineers.wiki.gg/wiki/Solar_Panel); [Session settings](https://spaceengineers.wiki.gg/wiki/Modding/Reference/Saves/SessionSettings)).

---

## 3. SPACE ENGINEERS 2

### 3.1 Release status (as of 2026-10-11)

- Early Access launch: 27 January 2025 ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- Planned Early Access length: about three years, to the end of 2027 ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/); [3DNews](https://3dnews.ru/1115711/space-engineers-2-otpravitsya-pokoryat-kosmos-rannego-dostupa-uzhe-sovsem-skoro-podrobnosti-i-predzakaz-v-rossiyskom-steam)). No full-release date on Steam.
- Steam price: A$44.95 for the base game; developers plan to raise the price as content ships ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)). A search summary gave a launch price of $29.99 (UNVERIFIED, not checked against Steam).
- Current content build named on Steam: "Vertical Slice 2.3, Drive, Automate & Detonate" ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)). Public patch numbering in Steam news is 2.4.0.x: hotfix 2.4.0.93 (27 August 2026) fixed crashes on some AMD RX 9000 cards, a frame-rate drop from solar panels, and a Gravity Generator that dropped to 10% power after a strength change; hotfix 2.4.0.95 (28 August 2026) adds detection for unstable AMD GPU drivers ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- "Smoother, Stable & Faster" update on 17 August 2026 focused on performance, saves and stability (third-party news via search summary, UNVERIFIED on Steam).
- Next content: "Space Engineers 2: Hostile Encounters Launching October 19!" (Steam news dated 8 October 2026). Combat NPCs (Renegades and heavier Boatmen) arrive early, separated from co-op. Almagest System expansion: improved Locations and Contracts, overhauled ores, new Verdure flora, updated lighting, combat effects. New blocks: Cables, Pipes, Armor Slabs, Interior Turrets, decorative props. Livestream 5 PM UTC ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- Four-player player-hosted co-op: "in development for early 2027" (Steam news, 8 October 2026) ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).

### 3.2 Published roadmap (Steam store page, with community-wiki dates)

- VS2: planets and survival foundations (delivered per store page) ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- VS2.2: "Mechanical Blocks and Weapons" per the community wiki, dated 23 March 2026 (UNVERIFIED) ([SE2 wiki](https://spaceengineers2.wiki.gg/wiki/Space_Engineers_2_Wiki); search summary).
- VS2.3 ("Drive, Automate & Detonate", the current build named on Steam): the Steam description (via search summary) lists planetary stations, Endless Encounters, reworked ore distribution, wheels and hinges, destructive warheads, timer/sensor/event controller, and wind physics ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)). The SE2 wiki lists VS2.3 as both delivered and in development. The two sources name the milestones differently; treat the mapping as UNVERIFIED.
- VS3: volumetric water and the Byblos water planet; in development; no date ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- VS4: NPCs and co-operative survival for four players; the roadmap "currently targets" VS4 ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)). Note: the 8 October 2026 Steam news moves combat NPCs ahead of co-op.
- VS5: full multiplayer and long-form campaign; no date ([SE2 wiki](https://spaceengineers2.wiki.gg/wiki/Space_Engineers_2_Wiki)).

### 3.3 Grid size and building

- Unified grid: "The grid has 25cm (~10 inches) granularity." Block sizes start at 25 cm and go through 0.5 m and 2.5 m, with no upper bound. Larger and smaller blocks can share one grid ([SE2 wiki: Grid](https://spaceengineers2.wiki.gg/wiki/Grid)).
- In SE1 the small-grid block is 0.5 m and the large-grid block is 2.5 m (from the Large Reactor page: 3x3x3 = 1.5 m S, 7.5 m L) ([Large Reactor](https://spaceengineers.wiki.gg/wiki/Large_Reactor)).
- SE2 blocks named in Steam news: Cables, Pipes, Armor Slabs, Interior Turrets, wheels and hinges (VS2.2/2.3 per the Steam description), and decorative props. No SE2 block catalogue was fetched (UNVERIFIED).
- Physics, mass, speed, PCU and block limits in SE2: not found on the wiki grid page (UNVERIFIED). The search summary about a 300 m/s top speed in space is from a player suggestion, not a Keen statement ([Keen support forum](https://support.keenswh.com/spaceengineers2/pc/topic/52863/vote/add/1), UNVERIFIED).
- Wind physics and combat effects are described for VS 2.3 in a Steam description summary (UNVERIFIED on the store page itself).

### 3.4 Planets, survival and NPCs

- Playable planets now: Verdure, Kemik, moons, asteroid rings, dense clusters. Byblos arrives with VS3 ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- Survival mechanics (power, oxygen, hydrogen, health) are in the alpha alongside Creative. Long-term survival progression is still planned ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- Renegades (hostile NPCs and space pirates) were introduced on 27 August 2026 with an early combat test; Boatmen are a heavier enemy that leads Renegade encounters (24 September 2026 dev diary) ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- Thrusters and jetpacks in SE2 differ from SE1 (search summary: SE2 thrusters consume electric power; backpack jetpack uses power first and hydrogen second; a booster reaches 300 m/s and uses hydrogen). UNVERIFIED (third-party or wiki summary).

### 3.5 What is new or changed vs SE1 (confirmed items)

- Unified 25 cm grid with mixed block sizes on one grid ([SE2 wiki: Grid](https://spaceengineers2.wiki.gg/wiki/Grid)).
- Planets: Almagest System with Verdure and Kemik; Byblos water planet in VS3 ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- NPC combat via Renegades and Boatmen; NPC pathing and cover (dev diaries) ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- Mods through the VRAGE3 Mod HUB and Mod Editor plus Steam Workshop ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)).
- Multiplayer: single-player available now; co-op and full multiplayer are future VS content ([Steam store page](https://store.steampowered.com/app/1133870/Space_Engineers_2/)). Dedicated servers: not announced with a date; third-party news says "planned" (UNVERIFIED).
- Scripting: the SE2 wiki has an "Ingame Scripting" section with no content retrieved (UNVERIFIED for the API).
- Gravity Generator strength is adjustable; hotfix 2.4.0.93 fixed maximum power dropping to 10% after a strength change ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- Stations: planetary stations and Endless Encounters, per the Steam description summary for the current 2.3 build (UNVERIFIED on the store page itself).

### 3.6 SE2 vs SE1 numbers not confirmed

- Grid sizes (block 25 cm, 0.5 m, 2.5 m) confirmed; small-grid and large-grid equivalents: not stated.
- Max speed, PCU, block limits, thruster efficiency, gravity equation: not found (UNVERIFIED).

---

## 4. PLAYER PRAISE AND CRITICISM

Sources are mostly secondhand. Dates are shown where known.

### 4.1 What players praise (SE1)

- **Building and physics.** Feedback summaries highlight the range of buildable things (stations, spacecraft, cars, planes) and physics-driven construction ([Vaporlens SE1](https://vaporlens.app/app/244850/space_engineers); third-party).
- **Modding.** A Steam forum poster rated modding 10/10, building 8/10, survival 6/10 (search summary; Steam thread URL not retained, UNVERIFIED). Mod support is a major draw ([Vaporlens SE1](https://vaporlens.app/app/244850/space_engineers)).
- **Value.** Community reviews describe the base game as worth about $20, especially on sale (search summary, UNVERIFIED).
- **Technology.** Shamus Young says the game "is not terrible" and its underlying technology is impressive ([Shamus Young](https://www.shamusyoung.com/twentysidedtale/?p=40142)).
- **Reception.** GameGeeker reports about 89% positive across more than 100,000 reviews; five million copies sold (third-party; not verified) ([GameGeeker](https://gamegeeker.com/games/space-engineers-244850/review?market=US)).

### 4.2 What players criticise (SE1)

- **Performance.** An early-access user review on GiantBomb says 720p at low settings reached about 20 fps on a GTX 660 Ti, blamed in part on real-time voxel deformation, and called online netcode "janky". Dated (early access) ([GiantBomb user review](https://www.giantbomb.com/space-engineers/3030-44124/user-reviews/2200-28278/)).
- **Clang.** Players describe "clang" as sudden, chaotic failures where everything breaks and explodes unpredictably; it can break the engine and is avoidable unless deliberately triggered (Steam forum threads via search summary; definition from commenters, UNVERIFIED as a term).
- **Survival loop.** Shamus Young says survival scenarios start you with a finished base, ships and gear, leaving nothing to build from scratch; he could not tell what the mode was for ([Shamus Young](https://www.shamusyoung.com/twentysidedtale/?p=40142)). Date not checked; early-access era per search summary.
- **Learning curve and UI.** Vaporlens complaints: steep learning curve, weak tutorial and UI, game-breaking bugs, heavy DLC reliance. Steam Deck score 55, "Tinkering Required" (performance, controller support, Linux support) ([Vaporlens SE1](https://vaporlens.app/app/244850/space_engineers)).
- **Speed cap.** A Keen forum thread asks for a higher top speed (for example 200 m/s) with penalties like atmospheric drag and debris; the author says the limit was set almost a decade ago and that a cap is still needed for defences. No Keen staff reply on the page ([Keen forum](https://support.keenswh.com/spaceengineers/pc/topic/24907-potential-increases-to-top-speed-limits-to-them)).
- **Materials.** Counterpoint from a forum commenter: rich ore on each planet and moon supplies most needs, so materials are not tedious (UNVERIFIED, forum).

### 4.3 Space Engineers 2 (early access, 2026)

- Vaporlens lists severe performance issues, game-breaking bugs and crashes for SE2 (third-party review summary, [Vaporlens SE2](https://vaporlens.app/app/1133870/space_engineers_2/stats/details)).
- Keen's own hotfix notes: crashes on AMD Radeon RX 9000 with driver 26.8.1; frame-rate drop from solar panels; texture streaming and low-memory device failures; undo/redo placement; save deletion. The 2.4.0.95 hotfix detects unstable AMD drivers and advises rolling back ([SE2 Steam news](https://store.steampowered.com/feeds/news/app/1133870/?cc=US&l=english&count=25)).
- Keen's SE2 wiki and forum include a proposal for a structural-integrity system (proposal, not implemented) ([SE2 Keen forum](https://support.keenswh.com/spaceengineers2/pc/comments/115665/vote/toggle), via search).

---

## 5. Gaps and UNVERIFIED register

Ordered by importance for a Minecraft adaptation.

1. **Thruster efficiency curve vs density.** No numeric curve on the official wiki. Only legacy Earthlike surface values (v1.203): atmospheric and hydrogen 100%, ion 20%. Forum estimate of about 90% at sea level conflicts. Per-planet thrust data beyond Earthlike not found.
2. **Natural gravity unit.** SurfaceGravity described in m/s² but planets given in g. Formula and exponent 7 are from the wiki; unit UNVERIFIED.
3. **Max ship speed (100 m/s).** Not found in session settings. Community and forum claims only.
4. **Alien planet.** Gravity 1.1 g and 120 km diameter from the Planets page only. Atmosphere, ores and hazards: page not reached (404 at two URLs; search budget exhausted and not worked around).
5. **Atmosphere densities.** Only "1 atm" and heights are published. No altitude curve.
6. **Stone yields.** CONFLICT between Survival Kit text and Refinery table parse (2.11).
7. **Component recipes.** Fetched for Steel Plate, Construction Comp, Motor, Computer, Interior Plate. Others not parsed. Ingot-to-kg conversion not verified.
8. **Block stats not fetched.** Decoy, Heat Vent, Button Panels, LCD panels, Cargo containers (capacities), Oxygen Tank capacity, Refill Station, Cryo Chamber (only a search excerpt), Magnetic Plate, Hinge, Wheels and Suspension, Spherical Gravity Generator, Prototech blocks (except O2/H2 generator at L from the Prosperity page), Railgun, Rocket Launcher, Assault Cannon, Autocannon, Warfare Hangar Door, Gatling Gun (handheld), Survival Kit Type II.
9. **AI blocks (Automatons).** Facts from Keen's announcement excerpt via search; wiki pages not fetched.
10. **Programmable block.** UpdateFrequency option names, IGC message size limits and allowed types not extracted.
11. **Block grid availability for several blocks** (Interior Turret, Hinge, Magnetic Plate, Gatling Gun) not stated on fetched pages.
12. **Build stages.** Welding page returned 404; per-stage material costs not retrieved.
13. **Hydrogen tank capacity.** Table ambiguous (two readings).
14. **Hand tool tier speeds.** Conflict between infobox and usage table (2.14). Welder tiers from search summary only.
15. **Suit capacities.** Suit hydrogen 125 L and bottle 400 L from search summary only; oxygen capacity not given.
16. **SE2 facts from third-party sources.** price (A$44.95 on Steam vs $29.99 in a search summary), dedicated server timing, the content of the "Smoother, Stable & Faster" update, and the thruster and jetpack power rules.
17. **Update history 1.204 to 1.208 block contents.** Not retrieved; only names and dates (1.204 May 2024; 1.205 Oct 2024; 1.206 Apr 2025; 1.207 Sep 2025; 1.208 Nov 2025).
18. **Warfare 3 block list.** Titles from Steam news only; release Nov/Dec 2026 is a target.
19. **Basic Assembler speed vs Assembler.** Not stated. Basic Assembler power is half of the Assembler's max (280 kW vs 560 kW), which is a reading, not a stated speed.
20. **Power priority and blackout.** The Power page describes no priority system (UNVERIFIED that none exists).

---

## Appendix A. Block names

The full list of block names (553 pages at research time) is on the Space Engineers wiki: [Category:Blocks](https://spaceengineers.wiki.gg/wiki/Category:Blocks). It's linked rather than copied here.
