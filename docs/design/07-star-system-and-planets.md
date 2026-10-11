# 07 — The Meridian System: planets, moons and biomes

> Design by Opus. Numbers come from `tools/calc/system.py` (Compact preset). Every value is data-driven (`data/apoapsis/planet/*.json`), so servers can rescale. Research inputs: R3 (KSP bodies, SOI), R7 (sphere techniques), R9 (worldgen tech).

## 7.0 Design rules for every world

1. **Each world has one signature mechanic** that changes how you build. You can't fly the same ship everywhere, the way Factorio Space Age's planets each change the factory (R8 §6).
2. **Each world supplies one link in the materials chain.** Materials stay on Mekanism plus Apoapsis Kinetics (Create-style zinc and brass, rebuilt as original open-source work: `08-materials-and-industry.md`, `09`). A planet doesn't add a new ore set. It changes the *mix and abundance* of existing ores and adds at most one or two **chemicals** (Mekanism chemical system) that only exist there.
3. **Real latitude.** Planets are cube-spheres (see `03-architecture.md` §3.2), so every column knows its true latitude and longitude. Climate, day and night, and the sun angle are computed from position, not faked.
4. **Readable from orbit.** Every biome has a distinct albedo and colour so it reads on the orbital LOD sphere. You choose landing sites from orbit, the way you do in KSP.
5. **Gravity, air and temperature use vanilla attributes where possible.** Player gravity comes from the `minecraft:gravity` attribute (vanilla 0.08 blocks/tick², which feels like about 32 m/s² with drag), scaled by each world's g ratio so players keep the vanilla feel. Fall damage uses `safe_fall_distance` and `fall_damage_multiplier`, and jumping uses `jump_strength`. Physics grids use real m/s².
6. **At least 50 biomes on every major world**, each world with its own generator, colours and life (`17`). The tables below list each world's *signature* biomes; the full sets are built with the biome composer (`17` §17.2).

## 7.1 System overview (Compact preset)

Star **Meridian**: a compact orange dwarf, GM = 1.52 × 10¹⁵ m³/s². It is rendered at a visual distance and size chosen independently of the simulation.

| Body | Type | R (km) | Surface g | Atmosphere | Day | Year / orbit | LEO period | Ascent Δv to LEO |
|---|---|---|---|---|---|---|---|---|
| Cinder | Tidally locked inner world | 38 | 0.55 | Trace sodium exosphere | = year (1.6 h) | 1.6 h | 10 min | ~0.8 km/s |
| **Verdance** | Homeworld | 64 | 1.00 | 1.0 atm N₂/O₂, top 12 km | 20 min (vanilla day) | 4.0 h | 11.6 min | ~1.4–1.8 km/s |
| ↳ Pale | Moon of Verdance | 20 | 0.16 | None | 1.6 h (locked to Verdance) | 1.6 h | 14.6 min | ~0.2 km/s |
| Ochre | Cold desert world | 48 | 0.42 | 0.08 atm CO₂, top 7 km | 24 min | 7.3 h | 14.5 min | ~0.7–0.85 km/s |
| ↳ Rook, Pawn | Captured asteroid moons | 4 / 2.5 | 0.012 / 0.008 | None | Tumbling | 1 h / 2 h | — | ~15–30 m/s |
| Thalassa | Ocean world | 72 | 1.20 | 2.4 atm N₂/O₂, top 18 km | 16 min | 12.2 h | 12.3 min | ~1.6–2.1 km/s |
| The Shoals | Asteroid belt | — | µg | None | — | ~16–24 h | — | — |
| Tempest | Gas giant (no surface) | 420 (cloud tops) | 2.10 | H₂/He, ~140 km to the deep | 36 min | 32 h | 24 min | ~2.9+ km/s (scoop-only, never land) |
| ↳ Ember | Volcanic moon | 22 | 0.18 | Trace SO₂ | locked, 1.7 h | 1.7 h | 14 min | ~0.2 km/s |
| ↳ Glacis | Ice-shell ocean moon | 26 | 0.13 | None | locked, 3.4 h | 3.4 h | 18 min | ~0.2 km/s |
| ↳ Murk | Hazy hydrocarbon moon | 32 | 0.14 | 1.5 atm N₂/CH₄, top 9 km | locked, 6.3 h | 6.3 h | 25 min | ~0.25 km/s |
| Hollow | Precursor shell world | 30 | 0.30 (outside) / 0.3 g spin gravity inside | None | 40 min (shell); the inner drum spins every ~10 min | 2.6 d | 12 min | ~0.3 km/s |
| Wanderer | Periodic comet | 3 | µg | Coma near perihelion | Tumbling | ~2 d, e ≈ 0.8 | — | — |

**Transfers (Hohmann, from LEO to LEO):**

| Route | Coast time | Departure Δv | Capture Δv | Window every |
|---|---|---|---|---|
| Verdance → Pale | 23 min | 190 m/s | ~100 m/s | Every ~13 min (phasing) |
| Verdance → Cinder | 1.4 h | 990 m/s | 1,280 m/s | 2.8 h |
| Verdance → Ochre | 2.8 h | 595 m/s | 540 m/s | 8.8 h |
| Verdance → Thalassa | 3.9 h | 1,040 m/s | 840 m/s | 6.0 h |
| Verdance → Tempest | 7.9 h | 1,810 m/s | 1,380 m/s (aerocapture: ~0) | 4.6 h |
| Tempest → Hollow | 23 h | 1,065 m/s | 290 m/s | 2.7 d |

**What these numbers mean for play.** Ascent from Verdance needs about 1.6 km/s, a modest two-stage chemical rocket. The moon is a real-time trip (23 min), so the first space milestone works with no time warp at all. Inner planets take 1.4–4 h of coasting, and the outer system takes 8–24 h. That is why `03-architecture.md` §3.5 sets the time model: real time by default, crew cryo-link for long coasts, server-consensus warp, and jump drives late in the game. Transfer windows recur every few hours, so the porkchop plot matters without making anyone wait days.

**Scale presets.** `compact` is the default above. `classic` uses ×10 distances and ×6.7 radii (a 430 km homeworld) for a KSP-like 30 min LEO, and needs warp. `real` uses true Solar System scale (hardcore, experimental). An Earth-radius cube face is 10.0 M blocks across, inside the ±30 M world border, but anything drawn in absolute float coordinates jitters past about 16.7 M blocks (R7 §4). So `real` needs camera-relative rendering throughout. One orbital engine runs all three. Only `data/apoapsis/system/*.json` changes.

**Sky set pieces** (angular sizes computed from the numbers above):
- From Verdance, **Pale spans 8.9°**, about 17 times the size of Earth's Moon. It goes through visible phases, eclipses the sun, and raises big tides on the Tidewild Flats.
- From Pale, **Verdance spans about 24°** and never moves (tidal lock). From the far side it is never visible, so the far side needs relay satellites to talk home.
- From Tempest's moons, Tempest fills a large part of the sky. Its rings are rendered edge-on from the equatorial moons.
- On Cinder the star hangs fixed: noon forever on the dayside, night forever on the nightside, and permanent dusk along the terminator.

---

## 7.2 Verdance — the homeworld

**Role.** Ages 0–4 (see `04-progression-ages.md`). Familiar terrain made round: it uses the **vanilla overworld biome registry and noise router**, so vanilla and datapack biome mods (Terralith, Tectonic and the like) keep working. The climate *parameters* are re-driven by true latitude and altitude.

**Climate model.** Vanilla's multi-noise `temperature` parameter becomes `latitudeBand(lat) + altitudeLapse(y) + noise`, and `humidity` becomes `oceanDistanceField + noise`. Poles freeze, the equator holds jungles and deserts, and the mid-latitudes hold forests and plains. A 15° axial tilt gives one season per hour of real time over the 4 h year: the snow line creeps toward the equator in winter. Seasons can be switched off per server.

**Signature mechanic: the crash site.** The colony seed-ship *Arkwright* broke up on entry. Its wreck fields are hand-placed structure chains scattered across all six faces, and each one holds sealed salvage that opens one Age at a time (the SevTech-style story spine, `04` §4.1).

**Apoapsis biomes added to Verdance** (alongside vanilla's):

| Biome | Where | Terrain | Life | Resources | Hazards and notes |
|---|---|---|---|---|---|
| **Crash Scar** | One per face (6 total); spawn is in one | Glassed impact craters, furrows of fused soil, hull plates half-buried | Pioneer grasses, burnt stumps | *Arkwright* salvage (locked by Age), scrap steel, fluorite crystals exposed by the impact | Residual radiation near reactor fragments (Mekanism radiation) |
| **Skyreach Highlands** | Mid-latitudes | Plateaus and mesas rising 900–1,400 m, cliffs, wind-carved arches | Wind-bent pines, cliff-nesting gliders | Zinc and copper on exposed faces | Strong winds affect light aircraft; the natural test range for atmospheric thrusters |
| **Tidewild Flats** | Coasts facing Pale's tidal bulge | Kilometres of mudflat and tidal channels | Shellbeds, wading birds, giant crabs | Clay, salt (Mekanism brine), kelp | **Tides:** a water-level band rises and falls twice per 25-minute lunar day (about every 12.7 min), with spring and neap tides over Pale's 2.6 h phase cycle. Moving-water blocks are confined to this biome for performance |
| **Glasswood** | Temperate | Forest of trees with translucent, crystalline leaves | Glass moths, luminous fungi | Fluorite clusters on roots, silica sand | Leaves shatter into shards (light damage) when exploded or struck by lightning |
| **Basalt Rift** | Along a tectonic seam across two faces | Rift valley 300–600 m deep, columnar basalt, steaming vents | Thermophile mats | Geothermal vents (Kinetics steam boilers can be heated directly), lead, tin | Sulphur fumes in pockets (suffocation without a mask) |
| **Meteor Steppe** | Dry continental interiors | Grassland pocked with ancient craters | Grazers, burrowers | **Uranium-rich impact cores** (the early uranium source), iron | Irradiated crater floors |
| **Aurora Taiga** | Above 60° latitude | Snow taiga under a permanent auroral sky at night | Elk, snow foxes | Osmium in permafrost seams | Night temperatures need warm gear in Age 0–1 |
| **Deep Kelp Shelf** | Continental shelves | Kelp forests 40–80 m tall | Seals, shoals | Prismarine-like "shelf glass", sponges | Currents push swimmers and small boats |
| **Luminous Hollows** (cave) | Under the temperate band | Huge caverns of glowing crystal | Cave fauna | Fluorite, redstone, deep lead | Darkness outside the glow |
| **Vertex Spire** | Exactly at each of the 8 cube corners | A colossal natural pillar or caldera that hides the topological corner (see `03` §3.2.4) | Unique flora that bends toward the spire | Concentrated rare ores, Precursor beacons | Compass spin and gravity flicker (cosmetic). Landmarks visible from orbit |

**Ore distribution (Verdance).** Full vanilla set; Mekanism osmium, tin, lead, uranium (rare outside Meteor Steppe) and fluorite; zinc (Kinetics). Every Age 0–4 material is available here. Nothing on Verdance gates space travel except effort.

---

## 7.3 Pale — the moon

**Role.** The first space destination (Age 4). Signature mechanic: **vacuum and regolith.** Nothing burns, nothing breathes, and fine dust abrades.

| Biome | Terrain | Resources | Hazards and notes |
|---|---|---|---|
| **Mare Basalt** | Dark, smooth lava plains | Iron-rich basalt (crushes to iron and osmium in the Kinetics crusher) | The best landing zones: flat, low albedo |
| **Highland Regolith** | Bright, heavily cratered uplands | Anorthosite (aluminium-flavoured decor stone) | Rough terrain tips landers that have no landing-gear spread |
| **Ray Fields** | Bright ejecta streaks from the young crater *Lumen* | **Helium-3** in sifted regolith (`apoapsis:helium_3`, a Mekanism chemical) | Glass beads coat the surface (abrasion) |
| **Cold Traps** | Permanently shadowed polar craters | **Water ice**, so the Electrolytic Separator makes H₂/O₂: the first off-world fuel depot | -230 °C. Unheated machines freeze |
| **Peaks of Eternal Light** | Polar crater rims | Near-constant solar | Prime real estate for multiplayer, so expect claims fights |
| **Lava Tubes** (cave) | Collapsed and intact tubes 30–80 m wide | Shelter from radiation and meteors | Natural pressurisable bases: seal the ends and add air |

**Mechanics:** no aerobraking; regolith dust (machines working outdoors lose efficiency until you clean their filters, which an automated dust-skirt upgrade fixes); micrometeorite showers (SE-style meteor events); far-side radio shadow (needs a comms relay to fly drones from home, KSP CommNet-style).

---

## 7.4 Cinder — the furnace world

**Role.** Age 5, the energy world. Signature mechanic: **heat budget.** Every grid on Cinder tracks its thermal load. On the dayside, radiators, coolant loops (Mekanism sodium for hot reactor loops, ammonia for radiator loops, `08` §8.5) and shaded hulls decide whether a base survives.

| Biome | Terrain | Resources | Hazards |
|---|---|---|---|
| **Dayside Crucible** | Plains of glass and slag around **molten lead seas** | Lead (pumpable molten), tin, gold. Solar intensity ×3.3 of Verdance, and the dayside never sets (about ×6.6 energy per day) | 450 °C ambient. Unshielded blocks above a heat rating slowly soften and lose integrity |
| **Terminator Belt** | A ring of permanent dusk circling the planet | Everything in moderation | The only zone where a basic suit survives indefinitely, so every base starts here |
| **Nightside Glacier** | Frozen volatile plains | Water ice, frozen sulphur | -170 °C |
| **Ironglass Badlands** | Fused-glass canyons | Quartz, iron, uranium. A sealed **Precursor vault** holds one Precursor Fragment | Glass shards puncture tyres (wheel damage) |
| **Fumarole Fields** | Vent-pocked crust | Sulphur, fluorite | Gas vents erupt on a timer |

**Planet-exclusive:** none. Cinder is the **abundance world** for uranium, lead and solar flux, which makes it the natural home for Mekanism fission and the solar-pumped industry that feeds the antimatter chain.

---

## 7.5 Ochre — the cold desert

**Role.** Age 5, the first true planet. Signature mechanics: **dust storms and ISRU.** You can only get home on fuel made from Ochre's own air.

| Biome | Terrain | Resources | Hazards and notes |
|---|---|---|---|
| **Rust Dunes** | Iron-oxide dune seas | Iron (oxide sand smelts directly) | Dunes migrate slowly (falling-sand creep during storms) |
| **Canyon Labyrinth** | Canyon system up to 800 m deep | Layered strata exposing tin, lead and zinc bands | Rockfalls; a world-class flying course |
| **Tharsis Flank** | The shield volcano **Mons Ascendant**, rising about 1,600 m to the interface altitude | Basalt, sulphur | The summit sits at the edge of space, so it is the cheapest launch site in the system |
| **Polar Layered Deposits** | CO₂ and water ice stacks | Water ice, dry ice (CO₂) | Seasonal CO₂ sublimation geysers |
| **Chaos Terrain** | Jumbled collapsed blocks | Subsurface ice pockets | Unstable ground caves in under heavy grids |
| **Perchlorate Flats** | Salt pans | Perchlorates, which give **chlorine** and oxygen for the Mekanism HCl and sulphuric-acid chain | Toxic dust (needs a filter) |
| **Precursor Ruins** | Structure biome | Precursor fragments, the start of the late-game story | Dormant sentinels |

**Planet chemical:** `apoapsis:carbon_dioxide`, pulled by an **Atmospheric Intake**. A **Sabatier Reactor** (CO₂ + H₂ → CH₄ + H₂O) makes `apoapsis:methane` for **methalox engines**, the mid-game workhorse. Moons **Rook** and **Pawn** are rubble piles with almost no gravity, the first SE-style asteroid mining you meet (magnetic boots, tether lines).

**Dust storms.** Regional storms hit every few in-game days, and a global one comes roughly once per Ochre year. They cut solar output, hide terrain, and sandblast unsealed machines.

---

## 7.6 Thalassa — the ocean world

**Role.** Age 5–6, the fusion-fuel world. Signature mechanics: **buoyancy, pressure, storms.** About 95% of the surface is ocean, with sea level at y=0 and the abyss reaching y≈-1,900 (this needs the extended dimension height, R9 §1).

| Biome | Terrain | Life | Resources | Hazards |
|---|---|---|---|---|
| **Atoll Archipelago** | The only land: coral rings and volcanic islets | Seabirds, crabs | Coral limestone, sand | Rising tides flood low builds |
| **Kelp Spires** | Kelp towers up to 200 m | Grazers, eels | Bio-mass (Mekanism bio fuel) | Entanglement slows small subs |
| **Storm Belt** | A band of permanent cyclone | — | Wind power ×4 | Lightning strikes (SE lightning damage), 40 m/s gusts load structures |
| **Twilight Zone** | 200–1,000 m depth, bioluminescent | Lanternfish, siphonophores | — | Pressure starts to matter: hull rating tier 2 |
| **Abyssal Vent Fields** | Hydrothermal chimneys at the floor | Tube worms, the **Leviathan** | **Heavy water** (deuterium via the Electrolytic Separator), **lithium brine** (Thermal Evaporation, then tritium) | Crush depth: hull rating tier 3+. Superheated plumes |
| **Drowned Plateaus** | Sunken highlands with Precursor ruins | — | Precursor caches | Guardian constructs |
| **Polar Ice Shelves** | Floating ice | Ice seals | Ice | Calving icebergs |

**Mechanics:** grid buoyancy from displaced volume (the physics backend computes the submerged hull volume per tick, `03` §3.1.1); hull pressure ratings; 2.4 atm breathable air that causes *pressure fatigue* without a mixed-gas rebreather (a gentle debuff, not death); Thalassa's 1.2 g plus thick air makes ascent from here the most expensive in the system.

---

## 7.7 Tempest — the gas giant and its moons

**Role.** Age 6. Tempest has no surface. Its "biomes" are **altitude layers** in a dimension with no ground, where the build volume is a column of atmosphere.

| Layer | Pressure | Feature | Resources | Hazards |
|---|---|---|---|---|
| **Upper Haze** | <0.1 atm | Ring shadow bands, aurora | Hydrogen (scoop at orbital speed) | Radiation belts above (Mekanism radiation) |
| **Ammonia Decks** | 0.5–1 atm | White cloud plains that look solid | Ammonia | Ammonia ice storms |
| **Water Decks** | 2–5 atm | Towering storm cells, floating **sky-kelp rafts** and **balloon fauna** | Water, bio-mass, **Helium-3 at its richest** | Lightning, downdrafts |
| **The Deep** | >10 atm, rising | Darkness, heat | — | Crush. Grids sink below the void-floor kill layer if buoyancy fails |

You build **aerostats**: grids with buoyancy cells filled with hot hydrogen, which float in the Water Decks as permanent skimming stations. The **Halo Rings** are a field of small ice and rock sub-grids in the equatorial plane (water ice and silicate rubble), mined SE-asteroid style.

**Moons:**
- **Ember** (Io-like). Signature: **sulphur and radiation.** Biomes: **Sulphur Plains** (yellow, red and black allotropes), **Lava Lakes**, **Plume Fields** (eruptions loft debris to orbit), **Radiant Scarps**. It supplies sulphur in industrial quantity for the Mekanism sulphuric-acid and 4×/5× ore-processing chain. Lead-lined hulls are mandatory.
- **Glacis** (Europa-like). Signature: **drilling through the ice into an alien ocean.** Biomes: **Lineae Plains**, **Chaos Melt**, **Cryovolcano Fields** (geysers vent to space; ride one up), **Subsurface Ocean** (under a 300–400 block ice shell, home to the system's native biosphere). It supplies xenobiology samples, the late-game science currency, and huge amounts of water.
- **Murk** (Titan-like). Signature: **thick air with low gravity.** At 0.14 g and 1.5 atm, almost anything flies. Gliders work for players, and propellers beat thrusters. Biomes: **Methane Seas** (a new fluid, `apoapsis:liquid_methane`, that you sail and pump), **Tholin Dunes** (organic sand that processes to **ethylene**, feeding the Mekanism HDPE chain in bulk), **Cryovolcanic Domes**, **Haze Highlands**, plus methane rain. It supplies fuel and plastics.

---

## 7.8 Hollow — the Precursor shell (endgame)

**Role.** Age 6 finale. From outside it is a dark, icy dwarf world. **The Breach**, a chasm at one pole, leads through 2 km of crust into a **hollow spin-habitat**: inside the shell, a Precursor **drum** about 28 km in radius spins on its own every ~10 minutes. That gives its inner surface **0.3 g of spin gravity pointing outward**, while the shell itself only turns every 40 minutes. It is the only place where "down" is away from the centre.

| Zone | Description |
|---|---|
| **Outer Crust** | Ice and carbon plains, Precursor survey markers |
| **The Breach** | A descending shaft with gravity shifting from inward to outward. It is the navigation puzzle that showcases the per-grid gravity-field model |
| **Arboretum Drum** | Overgrown gardens on the inside of the spinning drum, lit by a central "lamp" fusion core |
| **Machine Strata** | Precursor factories (Precursor machinery, salvage only) |
| **Core Lattice** | The zero-g central lattice around the **Heart**, the final objective. It powers the **Jump Gate**, the hook into a second star system (expansion) |

---

## 7.9 The Shoals and the Wanderer

**The Shoals.** A procedurally generated asteroid belt. Asteroids are **free-floating static sub-grids** (SE-style voxel rocks) of four classes:

| Class | Composition (Mekanism / Kinetics / vanilla) | Notes |
|---|---|---|
| C-type | Water ice, carbon (coal), bio-organics | Fuel depots |
| S-type | Iron, tin, zinc, copper, silicate | Bulk construction |
| M-type | Osmium, iron, gold, lead | High value, very dense (mass matters for tugging) |
| U-type (rare) | Uranium, fluorite | Radiation |

Derelicts (pirate bases, *Arkwright* escape pods, Precursor buoys) spawn in the belt as encounters (SE Global Encounters analogue).

**The Wanderer.** A periodic comet with e ≈ 0.8 and a period of about 2 real days. Its perihelion (about 21,000 km from Meridian) sits just outside Verdance's orbit, so it sweeps close past the homeworld every two days. Near perihelion it grows a coma and tail (a render effect plus a particle hazard) and vents geysers. Intercepting it is a KSP-style challenge (high relative velocity). Its core yields **cometary exotics**, a rare input for the jump-drive capacitor. A server-wide "comet season" event is announced in chat.

---

## 7.10 Native life (summary; full species design in `17` §17.4)

| World | Fauna (passive / neutral / hostile) |
|---|---|
| Verdance | Vanilla mobs plus gliders, giant tide crabs, glass moths. *Arkwright* security drones (hostile, salvageable) |
| Pale | No organic life. Precursor **regolith mites** (machine "life" that grazes on metal), plus rogue drones and Salvager pirates |
| Cinder | Terminator-belt extremophiles: crystal grazers, heat-vent tube colonies, glass-shell crawlers |
| Ochre | Brine-ice cave dwellers, microbial mats in chaos-terrain ice, dust-skimming spore drifters, plus dormant Precursor sentinels at the ruins |
| Thalassa | Lanternfish, kelp eels, ice seals, **the Leviathan** (boss, which can crush small subs) |
| Glacis ocean | Vent grazers, glass jellies, ice-borers (they bore into hulls) |
| Tempest | Balloon jellies (lift-gas source when farmed), sky-kelp rafts, storm rays |
| Murk | Cryo-life in the methane seas: slow azotosome swimmers, tholin-crusted shore crawlers |
| Ember | Sulphur chemotrophs at the vents, plume-riding spore clouds |
| Hollow | Precursor constructs and the overgrown arboretum ecology |
