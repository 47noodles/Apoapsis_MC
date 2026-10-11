# 11 — Ideas to expand Apoapsis

> Original ideas by Opus, some sparked by mechanics catalogued in R8 (Stationeers, Factorio Space Age, Outer Wilds, Satisfactory, Hardspace: Shipbreaker, Avorion, Stormworks, DSP, No Man's Sky). Parked here so they don't creep into the MVP. Each is tagged with a rough size: **S** (weeks), **M** (1–3 months), **L** (3+ months).

## 11.1 Orbital mechanics, deeper

| Idea | Size | What it adds |
|---|---|---|
| **Lagrange stations** | M | In the `real` preset, an approximate restricted three-body solver gives L1–L5 points for Verdance–Pale and Tempest–moons. Parking a station at L4 means it stays put relative to both bodies, and halo orbits become a late skill |
| **Kessler debris** | M | Destroyed grids leave debris on rails. Debris that hits debris makes more. Servers get a debris density per orbital shell, and **cleanup tugs** and **debris contracts** become a profession |
| **Mass driver at Pale** | M | An electromagnetic launch rail flings cargo pods into transfer orbits for free. A **catcher station** has to match velocity, so it's a logistics puzzle with real maths |
| **Skyhooks and orbital rings** | L | Rotating tethers that grab suborbital craft and throw them to orbit. The orbital ring is a server-wide megaproject (`04` §4.3) |
| **Gravity-assist league** | S | Achievements and leaderboards for slingshots, Oberth-effect burns and the cheapest Δv to each body |
| **Interplanetary autopilot routes** | M | Factorio Space Age-style schedules: cargo grids fly a computed transfer between two stations on their own, every window, and players manage the fleet |

## 11.2 Living worlds

| Idea | Size | What it adds |
|---|---|---|
| **Terraforming Ochre** | L | A server-wide project: greenhouse factories and comet impacts raise Ochre's pressure and temperature in stages. Biome palettes, ice caps and weather shift with a "terraform index" in the planet JSON, until rain falls on Ochre for the first time |
| **Space weather** | M | Meridian's flares send radiation storms across the system. **Forecast satellites** give warning time, and unshielded crews must shelter. Auroras flare on Verdance and Tempest |
| **Real tides** | M | Extend the Tidewild Flats tide simulation to more coastlines, with tidal power generators |
| **Eclipses and conjunctions as events** | S | Server announcements; some Precursor puzzles only open during an eclipse (Outer Wilds-style timing) |
| **Seasons everywhere** | S | Axial tilt per planet. Ochre's polar CO₂ caps grow and retreat |
| **Glacis xenobiology** | M | Tame, breed and study alien fauna. Bio-engineered algae strains for better O₂ farms |

## 11.3 Ships and engineering

| Idea | Size | What it adds |
|---|---|---|
| **Atmospherics 2.0** | L | Stationeers-style per-room gas mixtures (O₂, N₂, CO₂, toxins), temperature and pressure. Fires in O₂-rich rooms, explosive decompression, scrubbers. It replaces the simple sealed/unsealed model |
| **Wear and maintenance** | M | Components degrade with use and heat. Inspection drones and spare parts make long missions plan for repairs |
| **Multi-crew stations** *(promoted to core: `15` §15.5)* | M | Separate engineer (power, heat), navigator (map and nodes) and gunner consoles. Mission Control at a Ground Station can plan nodes for the crew in flight |
| **Salvage profession** | M | Hardspace-style derelicts with cut-lines (reactor, fuel and coolant lines that punish bad cuts) and sorting bins for scrap value |
| **Standard docking ports** | S | Community-standard port sizes and an adapter set, so blueprints from different players dock together |
| **Ship classes and registries** | S | Registered ship names, hull classes and a server registry, with transponders showing names on HUDs |
| **Zero-g mobs** | M | Mob AI that drifts, grabs handholds and pushes off walls in zero-g, for boarding fights and derelict creatures |
| **Thermal signature and stealth** | M | For PvP servers: heat-based detection, radiator management, cold running |

## 11.4 Progression and play modes

| Idea | Size | What it adds |
|---|---|---|
| **Space Agency career mode** | M | KSP career: funds from contracts, reputation, an agency per team, mission budgets, failure costs |
| **Colonists** | L | Thaw the *Arkwright*'s frozen colonists to staff stations. NPC roles (miner, engineer, botanist) and colony needs, the colonies KSP2 planned |
| **Hardcore crew** | S | Permadeath crews and limited engine ignitions, for KSP RP-1 fans |
| **Challenge scenarios** | S | Scripted starts: "Stranded on Murk", "Rescue at Glacis", "Ochre in 3 windows" |
| **Procedural star systems** | L | Each server seed generates a different system under the same design rules (`07` §7.0), with constraints for playable Δv and window cadence |
| **In-game system editor** | M | A creative tool to design planets (radius, g, air, biomes, orbit) and export them as datapacks. The data-driven planet JSON makes this mostly UI work |

## 11.5 Beyond Meridian

| Idea | Size | What it adds |
|---|---|---|
| **Second star system via the Jump Gate** | L | **Twinlight**, a binary star with circumbinary planets. It's the one place n-body physics is on by default: unstable orbits, chaotic transfers, and a ring of Precursor gates |
| **A neutron-star or black-hole showcase** | M | A gravitational-lensing shader, extreme tidal forces that tear apart unbraced grids, and relativistic visuals for the ultimate flex |
| **Precursor knowledge temples** | M | Outer Wilds-style puzzles that teach orbital mechanics. A door opens only to a craft in a specific resonant orbit, for example |
| **Dyson swarm** | L | DSP-style endgame. Launch solar collectors around Meridian for system-wide beamed power. It's a visible, server-shared monument |

## 11.6 Community and tooling

| Idea | Size | What it adds |
|---|---|---|
| **Blueprint hub** | M | A Git-backed, open gallery of `.apbp` ships with validation (complexity budget, Age requirements) and one-click import |
| **Web mission planner** | S | The `orbit` library compiled for the browser (TeaVM or a small TS port). Transfer windows, Δv maps and porkchop plots from any server's `system` JSON |
| **Discord bot** | S | Posts upcoming transfer windows, eclipses and comet seasons from the server's UT |
| **Sound in vacuum** | S | Outside sounds muffle to silence in vacuum, and you hear only through your hull and suit, the way SE does. Cheap, very immersive |
| **Photo mode and telescope** | S | Telescopes discover the Wanderer and new asteroids (a discovery-credit mechanic), and photo mode shows off planet rendering |
| **Multi-server universe** | L | For very large public servers: shard planets across backend servers behind a proxy, handing ships off at the transition altitude (`15` §15.9) |
| **Accessibility** | S | Autopilot assists, a colour-blind HUD palette, difficulty presets (Δv multiplier, forgiving re-entry), and full keybinding |
