# 05 — Construction, grids and ship systems (the Space Engineers layer)

> Design by Opus. Research inputs: R1 (SE1/SE2 blocks and systems, 553 block names), R10 (Mekanism and Create chains), R8 (Stationeers, FTD and Starbase ideas). The physics implementation is in `03` §3.1. Names are original (charter, `09` §9.3). SE's generic engineering terms (gyroscope, connector, merge block) are kept because they are plain words.

## 5.0 Lessons taken from Space Engineers' reception (R1 §4)

| SE pain point | Apoapsis answer |
|---|---|
| **"Clang"**: joints and sub-grids exploding unpredictably | Jolt constraints with solver substeps, joint break thresholds that *damage* instead of explode, and a sub-grid safety governor (it clamps impulses and freezes runaway joints with a warning) |
| **Survival starts with a finished base** | The Ages spine starts you with nothing (`04`) |
| **Steep learning curve, weak tutorial** | Holo-Manual animated guides (Ponder-style), a staged unlock order, and a first-flight tutorial quest chain |
| **SE's widely reported 100 m/s speed cap** | No global cap in space. The floating-origin bubbles and continuous collision detection handle speed. Servers can set a "Kessler cap" for PvP balance. The *surface frame* keeps a cap for chunk generation (`03` §3.3.2) |
| **Performance with big grids** | A grid complexity budget per player (SE's PCU idea, §5.10), greedy collision meshing, and rails for idle grids |

## 5.1 Grids

- **A grid** is any set of blocks connected face to face that isn't part of the terrain.
  - **Static grids** (stations, bases) are anchored to the terrain or to nothing in space, and never move.
  - **Dynamic grids** (ships, rovers, drones) are simulated by APC.
  - A static grid can be **released** into a dynamic one, and a dynamic grid can be **anchored** with landing gear or a Station Anchor.
- **Grid sizes:** **Large** uses 1 m blocks. **Micro** uses 0.25 m blocks for drones, fighters and fine detail. They join through micro↔large adapter mounts (rotor heads, connectors).
- **Starting a grid in space** (SE behaviour): a block placed with no neighbours in `apoapsis:void` becomes a new static grid.
- **Any block can be built into a grid.** Vanilla, Mekanism, Kinetics and other mods' blocks keep their behaviour. Mass comes from the `apoapsis:block_mass` data map.
- **Splitting:** destroy the last link and the grid becomes two (`03` §3.1.1).
- **Ownership:** grids and blocks are owned. Sharing is *none*, *faction* or *everyone* (SE's ownership model). Grids are taken over by grinding through computer components, which is SE's hacking. A server can disable PvP takeover.

## 5.2 Grid networks: power, conveyance and data

- **Power** (SE rule): every powered block on a grid shares one electrical network with **no cables**. Energy is **Mekanism Joules**, so the whole Mekanism energy ecosystem plugs straight in, and an FE bridge comes for free. The network shows supply and demand, battery charge and a **priority ladder** that sheds load in a set order during brown-outs: life support last, weapons first. SE's wiki describes no priority system; Apoapsis makes priorities explicit.
- **Conveyance** (SE rule, Mekanism-aware): **conveyor blocks** link every inventory on a grid. A single conveyor network carries **items, fluids and Mekanism chemicals**. SE conveyors carry gases too, and here that means hydrogen, oxygen, methane and so on.
  - **Port sizes:** standard ports carry anything, while bulk ports carry high-throughput fluids and chemicals and large crates. Small ports never mate with bulk ports (an SE rule kept for build depth).
  - **Sorters** and **filters** route flow, and **valves** split networks into isolated zones for fuel feed (`06` §6.3).
  - Transfer is fast but **throughput-limited per conveyor tier** (Basic → Advanced → Elite → Ultimate, matching Mekanism's tiers). SE's instant transfer gives no reason to design logistics.
- **Data:** every block with a computer component appears in the **grid terminal**, SE's control panel. There you can name blocks, group them, toggle actions and bind them to toolbar slots in cockpits.

## 5.3 Build stages, integrity and damage

- **Building blocks** (SE style):
  - Apoapsis functional blocks are placed as a **frame**, which costs the first component.
  - **Welding** feeds in the remaining components from your inventory, from connected grid storage, or from a ship-mounted welder.
  - Partly built blocks show a construction model.
  - **Grinding** dismantles a block back into components.
- **Block Kits** (quality of life): an **Assembler** can bundle a block's components into a kit that places fully built. This keeps the build familiar for Minecraft players without losing SE's welding identity.
- **Integrity:** every block has HP set by its material and components. Below the **functional line** (the red mark on the integrity bar) a block stops working. At zero it pops and drops part of its components.
- **Damage sources:** collisions (impulse-based), explosions, weapons, heat (`06` §6.4), pressure (Thalassa's depths and Tempest's deep atmosphere), radiation (slow electronics degradation), lightning and meteors.
- **Visuals:** there's no mesh deformation. Damaged blocks get crack overlays at three levels, plus sparks and smoke while below the functional line.
- **Projectors:** a **Blueprint Projector** shows a hologram of a saved ship. Welders, by hand or in automated arrays, build it piece by piece, SE's classic shipyard loop. Blueprints are saved as an open, documented format (`.apbp`, JSON plus section data) that players can share freely.

## 5.4 Block catalogue

Each Apoapsis block below has a functional equivalent in SE (R1 §1). Names are Apoapsis's own. **Age** is when it unlocks (`04`). L means large grid and M means micro grid.

### 5.4.1 Structure

| Block | Grid | Function | Age |
|---|---|---|---|
| Hull plating (light, heavy), with slopes, corners, half and inverted variants | L, M | Armour. Heavy plating has more HP and mass, and resists radiation | 3 |
| Armoured glass panes and windows | L, M | Transparent and airtight | 3 |
| Blast doors | L | Very high HP | 5 |
| Catwalks, stairs, ladders, railings, frames | L | Decor and access, not airtight | 3 |
| Lead-lined plating | L | Radiation shielding (Mekanism lead), needed at Ember and Tempest | 5 |
| Heat-shield tiles | L, M | Ablative re-entry protection (`06` §6.4) | 4 |

### 5.4.2 Logistics

| Block | Grid | Function | Age |
|---|---|---|---|
| Conveyor (standard, bulk), junction, corner, tube | L, M | The grid network (§5.2) | 3 |
| Sorter, filter, valve | L, M | Routing and isolation | 3 |
| Cargo container (small, medium, large) | L, M | Storage | 3 |
| Connector | L, M | Docking that also links networks and ownership | 4 |
| Collector, ejector | L, M | Pick up floating items, throw items out | 3 |
| Tank (standard, cryo, pressure) | L, M | Mekanism chemicals and fluids (`06` §6.3) | 3–4 |

### 5.4.3 Industry on grids

| Block | Function | Age |
|---|---|---|
| **Refinery** (field, standard) | Ores to ingots or dusts. Built on Mekanism processing, so it upgrades through the 2×/3×/4×/5× tiers (`08` §8.3) | 3 |
| **Assembler** (field, standard) | Makes **components** from ingots, with queues and blueprints (§5.6) | 3 |
| **Survival Pod** | Tiny refinery, assembler, medbay and respawn in one block, for starter ships and lifeboats | 3 |
| **Ice Processor** | Ice to water. It feeds the Mekanism Electrolytic Separator to make H₂ and O₂ (SE's O₂/H₂ generator role) | 4 |
| **Algae Farm** | Slow solar-powered O₂ (SE's oxygen farm) plus bio-mass | 4 |
| **Atmospheric Intake** | Pulls the planet's air as a chemical (CO₂ on Ochre, N₂/CH₄ on Murk, H₂ on Tempest) | 5 |
| **Sabatier Reactor** | CO₂ + H₂ → CH₄ + H₂O | 5 |
| **Regolith Sifter** | Pale regolith to helium-3 | 5 |
| **Research Bench** | Spend Survey Points on in-Age tech nodes (`04` §4.2, `06` §6.7) | 3 |

Any Mekanism or Kinetics machine can also sit on a grid as-is.

### 5.4.4 Power

| Block | Grid | Function | Age |
|---|---|---|---|
| Battery bank (small, large) | L, M | Storage, with auto, recharge and discharge modes | 3 |
| Solar array (panel, flat collector) | L, M | Output scales with star angle and distance (×3.3 intensity on Cinder, and about ×6.6 energy per day because the dayside never sets) | 3 |
| Wind turbine | L | Atmosphere only, scales with air density and wind (Storm Belt ×4) | 3 |
| Combustion generator | L | Burns hydrogen, methane or ethylene (SE's hydrogen engine role) | 4 |
| Mekanism generators (heat, gas-burning, bio, advanced solar, wind) | L | Work on grids as-is | 3–4 |
| **Compact fission reactor** | L | A grid-scale reactor on Mekanism fissile fuel (SE's reactor role). It makes radiation and waste, and coolant failure means a meltdown (Mekanism-style) | 5 |
| **Fusion core** | L | D-T or D-³He, a late-game ship reactor | 6 |
| Mekanism multiblocks (fission, fusion, turbine, induction matrix) | static or L | Can be built on big station grids | 5–6 |

### 5.4.5 Movement and control

| Block | Grid | Function | Age |
|---|---|---|---|
| Thrusters and engines (all families) | L, M | See `06` §6.2 | 3–6 |
| **Gyroscope** | L, M | Torque for attitude control. Override and power settings | 3 |
| Reaction wheels (advanced gyros) | L, M | Stronger torque per mass, saturate over time | 5 |
| Cockpit (open, enclosed, fighter, bridge), control seat, passenger seat | L, M | Piloting. The enclosed cockpit is airtight | 3 |
| Crew consoles (engineer, navigator, gunner), Mission Control console | L | Multi-crew roles (`15` §15.5) | 5 |
| Remote control (probe core) | L, M | Unmanned control (`06` §6.6) | 4 |
| Stage controller, separator (decoupler) | L, M | Staging (`06` §6.3) | 4 |
| Parachute hatch | L, M | `06` §6.4 | 4 |
| Wings, control surfaces, nose cones | L, M | Aero (`06` §6.4) | 3 |
| Landing gear, magnetic plate | L, M | Lock to terrain or grids | 3 |
| Wheels and suspension (1×, 3×, 5×) | L, M | Rovers. Jolt vehicle controller | 3 |
| **Rotor, advanced rotor, hinge, piston** | L, M | Sub-grids through Jolt constraints, with safety governor (§5.0) | 3 |
| **Merge block** | L, M | Joins two grids into one (stage-building, modular ships) | 4 |
| Buoyancy cell | L | Lift gas or ballast for aerostats and submarines | 4 |

### 5.4.6 Tools on grids

| Block | Function | Age |
|---|---|---|
| Ship drill | Mines terrain and asteroids straight into the conveyor network | 3 |
| Ship welder | Welds or repairs blocks in range, including projector builds | 3 |
| Ship grinder | Salvages blocks into components | 3 |
| Blueprint projector | Shipyard holograms (§5.3) | 4 |
| Tractor beam | Pulls small grids and items, used to tug asteroids (Shoals M-types) | 5 |

### 5.4.7 Life support and crew

| Block | Function | Age |
|---|---|---|
| Air vent | Pressurise or depressurise a sealed room from the O₂ network | 4 |
| Airlock door, hangar door | Airtight when closed. Airlock cycles on a timer | 4 |
| Oxygen tank | O₂ storage (a Mekanism chemical) | 4 |
| **Clone Bay** | Respawn and *cryo-link* target (`03` §3.5). It's SE's medical room, extended | 4 |
| **Cryo pod** | Crew stasis for long coasts; the cryo-link source | 5 |
| Medbay | Heals, refills suit O₂/H₂/energy, removes radiation dose | 4 |
| Galley (food processor) | Long-mission food, with SE survival-buffs style bonuses | 4 |
| Thermal regulator | Heats or cools a room. Needed on Cinder, Glacis and Murk | 5 |
| **Link Pod** | Where your body rests while you're linked into a proxy or clone (`14` §14.1) | 4 |
| **Proxy Fabricator** | Builds android proxies: Standard frame in Age 4, specialised frames in Age 5 | 4 |
| **Proxy Dock** | Recharges and stores proxies; a hub bank of them can be rented | 4 |

### 5.4.8 Gravity and exotic tech

| Block | Function | Age |
|---|---|---|
| **Gravity generator** (planar, spherical) | Artificial gravity fields that add to natural gravity as vectors | 6 |
| **Mass block** (artificial mass) | Responds to artificial gravity. **Gravity drives** are allowed as emergent tech, as in SE | 6 |
| Gravity sphere (space ball) | Tunable mass and friction | 6 |
| **Jump drive** | `06` §6.8 | 6 |
| Precursor blocks (salvage only) | Rare, powerful variants such as the Precursor reactor and Precursor thruster. The answer to SE's endgame tier, with original designs | 6 |

### 5.4.9 Comms, sensing and automation

| Block | Function | Age |
|---|---|---|
| Radio antenna (tiers), laser antenna, relay | `06` §6.6 | 4 |
| Ground Station | Fixed control and relay hub for remote craft (`06` §6.6) | 4 |
| Beacon | Long-range marker, also keeps a grid "remembered" for cleanup protection | 3 |
| Ore surveyor | SE's ore detector plus KSP surface scanning (`06` §6.7) | 3 |
| Sensor | Volume detection that triggers actions | 3 |
| Camera | Remote view and raycast range-finding | 4 |
| **Flight Computer** | SE's programmable block, in Lua (§5.7) | 4 |
| Timer block, event controller | Sequencing and conditional triggers | 3 |
| Button panel, toggle switch | Manual triggers | 3 |
| Display panels (LCD, wide, corner, transparent) | Text, sprites and script-drawn graphics | 3 |
| **Autonomy cores** (Flight, Task, Patrol, Defence, Recorder) | SE's AI blocks from its 2023 Automatons update (R1 §1.15): drones that fly, follow, attack, defend and replay recorded paths | 5 |

### 5.4.10 Weapons (servers can disable all of these)

| Block | Function | Age |
|---|---|---|
| Autocannon turret, missile turret, interior turret | Point defence and anti-ship | 5 |
| Artillery turret, railgun | Long-range heavy | 6 |
| Turret controller | Custom turrets from rotors, hinges and guns (SE's custom turret controller) | 5 |
| Warhead, decoy | Demolition and missile bait | 5 |

### 5.4.11 Economy and world services

| Block | Function | Age |
|---|---|---|
| Trade terminal | Buy and sell with NPC factions and players | 4 |
| Contracts board | Haul, mine, escort, salvage and rescue contracts (§5.9) | 4 |
| Safe-zone emitter | Server-configurable protected bubble | 5 |
| Hub Core | Makes a station a public hub and the seat of an alliance (`14` §14.2) | 5 |
| Distress beacon | Posts a rescue contract on the always-open emergency channel to everyone in range (`15` §15.6) | 4 |

## 5.5 Life support and the character

- **The suit** has O₂, H₂ (jetpack fuel), energy and a thermal rating. It refills at a medbay, a Survival Pod or a connected tank. **Mekanism's jetpack runs on hydrogen too**, so it plugs into the same hydrogen economy. The MekaSuit is the late-game alternative.
- **Hand tools** (welder, grinder, drill) come in four tiers that match Mekanism's alloy and circuit tiers: Basic, Infused, Reinforced and Atomic (R10 §1).
- **Rooms and airtightness:**
  - Rooms on grids and in terrain bases are found by an **event-driven flood-fill**, invalidated on block change and capped per room (`03` §3.7).
  - Doors and vents change room state.
  - A breach vents the room with a decompression push on loose entities and items.
- **Temperature and radiation:**
  - Environment temperature comes from the planet model plus local heat sources.
  - Radiation plugs into **Mekanism's radiation system** (dose and hazmat gear; which shielding behaviour its API exposes is still to verify).
- **Gravity:** the player's gravity attribute scales with local g, *plus* artificial gravity fields, as a vector sum.

### 5.5.1 Free flight: full 6DOF in zero-g (SE-style)

Space Engineers' character flight is in scope. Minecraft's camera normally has only yaw and pitch: pitch is clamped at ±90°, and "up" is always world +Y. Apoapsis gives the player a full orientation of their own.

| Mode | When | Feel |
|---|---|---|
| **Grounded** | Natural gravity on planets (any face of the cube-sphere) | Vanilla: upright, yaw and pitch, vanilla movement |
| **Free flight (6DOF)** | Zero-g or microgravity (orbit, deep space, Rook and Pawn), jetpack on | Full rotation. The mouse pitches and yaws around your own view axes with no ±90° clamp, **Q/E roll**, and WASD plus Space/C translate relative to where you're looking. The suit's **inertial dampeners** (toggle) cancel drift, and **relative dampening** matches your velocity to the nearest grid (SE's Ctrl+Z) |
| **Mag-boots** | Standing on any grid surface in zero-g | "Up" becomes the surface normal, so you walk along a station's walls and ceilings at any angle |
| **Grid gravity** | Inside a grid's artificial gravity field | "Up" follows the gravity generator's field and **rotates with the ship**, so you walk on the floor of a ship that's upside down relative to the planet |
| **Atmospheric jetpack** | In gravity with the jetpack on | Upright hover, like SE's planetside jetpack: thrust fights gravity, no free roll |

**Transitions:** entering a gravity field rights you smoothly over about a second (SE does the same), and leaving gravity frees the roll axis again.

**How it works:**
- **Orientation:** a per-player **orientation quaternion** (a synced player attachment) replaces the yaw/pitch pair whenever you're not Grounded. Vanilla yaw and pitch are derived from it, so other mods still see sensible values.
- **Camera roll** comes from a camera-setup hook. *Do a Barrel Roll* (GPL-3.0) already rolls the camera freely on 26.3, which proves the renderer allows it. Ours is written from scratch (clean-room, `09`).
- **Movement input** is transformed into the player's local frame before physics. Suit thrust and dampeners are applied as forces, so the suit behaves like a tiny grid.
- **Collision:** in free flight the hitbox becomes a compact "tucked" box about 0.6 m across, so orientation never affects collision (Minecraft hitboxes are axis-aligned and can't rotate).
- **Per-player gravity direction** (mag-boots, grid gravity) goes through the same `EntityGridMotion` facade as standing on moving grids (`03` §3.1.1). Gravity API (MIT, 1.20.1) already switches a player's gravity between six directions; ours is continuous.
- **Other players** see your model rotated by your quaternion, so expect upside-down engineers in a hangar.
- **Mobs** stay vanilla-upright in 1.0. Zero-g mob behaviour is an expansion idea (`11`).

**Risk:** medium. It touches the camera, input, entity rendering and collision, but it's contained in one module and gets built in Stage 1 alongside entity-on-grid motion.

## 5.6 Components (the assembler economy)

SE builds every block from ~20 intermediate components. Apoapsis keeps that structure and builds each component from **Mekanism and Kinetics materials** (`08`). Every Mekanism item named below is a real item (R10 §1).

| Apoapsis component | SE role it fills | Made from |
|---|---|---|
| Hull plate | Steel plate | **Mekanism steel** ingot, pressed (Kinetics press) |
| Interior panel | Interior plate | Iron or zinc sheet |
| Framework | Construction component | Iron rods and zinc rivets |
| Structural girder | Girder, metal grid | Steel rods, welded |
| Pipe section (small, large) | Small and large steel tube | Steel sheet, rolled |
| Gear train | Part of the motor role | Brass gears (Kinetics brass) |
| Electric motor | Motor | Copper wire coil + steel + **osmium** core |
| **Control circuit** (basic, advanced, elite, ultimate) | Computer | **Mekanism control circuits**, used directly |
| Display panel | Display | Glass + basic control circuit |
| Armoured glass | Bulletproof glass | Glass + steel wire mesh (Age 3). An **HDPE**-laminated upgrade (Mekanism) comes in Age 5 |
| Battery cell | Power cell | Zinc + copper + steel (Age 3). **Lithium** cells (Mekanism brine → lithium) double the capacity in Age 5 |
| Sensor module | Detector | Thermionic valve (Kinetics) + basic circuit |
| Radio module | Radio-communication | Copper + advanced circuit |
| Reactor housing | Reactor component | **Reinforced alloy** + lead plating |
| Thrust chamber | Thruster component | **Infused alloy** + steel + fluorite-glazed liner |
| Superconductor coil | Superconductor | Gold + **atomic alloy**, cryo-annealed |
| Solar cell | Solar cell | Silicon-free: fluorite-doped glass + copper |
| Medical kit | Medical component | Bio-fuel derivatives + steel |
| Explosive charge | Explosives | Sulphur + carbon + oxidiser salts |
| Parachute canvas | Canvas | Wool or HDPE fibre |
| **Jump coil** | Gravity and jump component | Mekanism **teleportation core** + superconductor coil |
| Precursor core | SE's endgame material role | Salvage only (Hollow, ruins) |

## 5.7 Flight Computer (the programmable block)

- **Language:** **Lua** in a sandboxed VM with an instruction budget per tick. It fills CC:Tweaked's role with an API built around grids. CC:Tweaked's code is MPL-2.0, but its legacy assets are CCPL, so it stays an optional extra (`09`), with an optional CC:Tweaked peripheral as compat.
- **API:**
  - `grid.blocks()`, `grid.block("Main Thruster")`, `grid.group("Landing Lights")`, plus per-block actions and properties.
  - `nav.orbit()` returns Ap, Pe, inclination, period and next event. `nav.target()`, `nav.node()`.
  - `ctl.throttle()`, `ctl.attitude(prograde|vector)`, `ctl.stage()`, plus inter-grid messaging (like SE's IGC) over antennas.
- **Updates:** event-driven or periodic, at 1, 10 or 100 ticks (SE's UpdateFrequency equivalent).
- **LCD drawing:** a sprite and text API.
- **Built-in programs:** the autopilot cartridges (`06` §6.9) ship as readable Lua, a learning resource in themselves.

## 5.8 Weather, meteors and events

Lightning strikes tall or metal grids (SE-style damage). Dust storms (Ochre), methane rain (Murk), the Thalassa storm belt and meteor showers (on airless bodies or as a server event) are all configurable. These are server-side weather states rather than visual-only effects, because vanilla weather isn't data-driven (R9 §6).

## 5.9 NPCs, economy and encounters

- **Factions:**
  - **Free Traders** run trade stations in orbit around Verdance, Ochre and Tempest.
  - **Salvagers** are pirates whose drones and raiders hit derelicts and weak convoys.
  - The ***Arkwright* Remnant** is rogue security drones from the seed-ship.
  - **Precursor Sentinels** are dormant guardians that wake near ruins.
- **Credits:** "scrip", the currency for station trade and contracts, server-configurable or off.
- **Contracts:** hauling, mining, escort, salvage, search and rescue (stranded pods), and **orbital delivery** ("put a relay satellite in a 400 km polar orbit", KSP-contract style). Players can post contracts for each other.
- **Encounters** (SE's Global Encounters): derelicts, convoys, distress beacons and Precursor buoys spawn along trade lanes and in the Shoals.

## 5.10 Limits and server protection

- **Grid complexity budget:** each block has a complexity cost, and players or factions get a budget (SE's PCU idea). It protects TPS on public servers.
- **Cleanup policy:** unowned, unpowered, beacon-less debris older than N hours is despawned (SE's trash removal), with a warning.
- **Admin tools:** grid list, teleport, freeze, delete, ownership transfer, and physics stats per grid.

---

## 5.11 Feature checklist: construction and grids

**Grids and physics hooks**
- [ ] Static and dynamic grids, release and anchor, start-in-space grids
- [ ] Large (1 m) and micro (0.25 m) grids, with adapter mounts
- [ ] Any block works on a grid. `block_mass` data map with tag defaults
- [ ] Split on disconnect, merge blocks
- [ ] Ownership and sharing modes, grind-to-take-over, PvP toggle

**Networks**
- [ ] Cable-less grid power in Mekanism Joules, with a priority ladder
- [ ] Unified conveyor network for items, fluids and chemicals, with port sizes, sorters, filters, valves and tiered throughput
- [ ] Grid terminal: naming, groups, toolbar bindings

**Building and damage**
- [ ] Frames, welding, grinding, construction models
- [ ] Block Kits from assemblers
- [ ] Integrity and functional line; collision, explosion, heat, pressure, radiation, lightning and meteor damage
- [ ] Crack overlays and damage effects
- [ ] Blueprint projector and open `.apbp` format

**Blocks** (§5.4)
- [ ] Structure set · [ ] Logistics set · [ ] Industry set · [ ] Power set · [ ] Movement and control set · [ ] Tools set · [ ] Life support set · [ ] Gravity and exotic set · [ ] Comms, sensing and automation set · [ ] Weapons set · [ ] Economy set

**Character and life support**
- [ ] Suit O₂, H₂, energy and thermal; jetpack on hydrogen; four hand-tool tiers
- [ ] 6DOF free flight: orientation quaternion, camera roll, view-relative translation, suit dampeners and relative dampening, tucked zero-g hitbox, auto-righting in gravity
- [ ] Mag-boots and grid-relative gravity (walk on any surface; "up" rotates with the ship)
- [ ] Event-driven airtight rooms, vents, airlocks, decompression
- [ ] Temperature model; Mekanism radiation integration; vector gravity with artificial fields

**Automation and AI**
- [ ] Flight Computer: Lua sandbox, block, nav and control API, IGC-style messaging, LCD drawing
- [ ] Timer and event controller, sensors, buttons, displays
- [ ] Autonomy cores (flight, task, patrol, defence, recorder)

**World**
- [ ] Weather states with gameplay effects; meteors
- [ ] NPC factions, trade terminals, contracts (including orbital delivery), encounters
- [ ] Complexity budget, cleanup policy, admin tools
