# 03 — Technical architecture

> Design by Opus. Research inputs: R4 (physics mod landscape), R5 (26.x platform), R6 (Sable/VS2 code audit), R7 (sphere techniques), R9 (worldgen/dimensions), R3 (orbits, multiplayer time). Facts marked ✔ were re-verified by the orchestrator against a primary source on 2026-10-11.

## 3.0 Platform decisions

| Decision | Choice | Why |
|---|---|---|
| Minecraft version | **Java 26.3** (released 2026-09-15 ✔), moving to each new drop at stage boundaries only | It's the latest release. 26.1 onward ships **unobfuscated** (Mojang names, no intermediary or Yarn), which makes each later port mostly API churn rather than remapping (R5 §2) |
| Java | 25 (required by 26.3) | Required |
| Loader | **NeoForge first** (26.3 builds exist, still `-beta`). Loader-specific code sits in a thin per-loader layer so Fabric can follow | Mekanism (the main kept dependency) is NeoForge-first. A multiloader layout (Stonecutter or MultiLoader-Template) keeps the door open |
| Build | ModDevGradle, Gradle 9.x | Current NeoForge toolchain |
| Version policy | "Latest drop at each stage boundary". Budget about 1–2 weeks per drop hop, and keep every mixin behind a `compat/mc<version>` facade | Mojang now ships about 4 drops a year (26.1 Mar, 26.2 Jun, 26.3 Sep, 26.4 Q4). Chasing every one mid-feature would stall everything else |

### Module layout (Gradle subprojects)

```
apoapsis/
├─ orbit/           pure Java orbital mechanics, no Minecraft dependency, unit-tested
├─ physics/         APC: sub-level grid engine + Jolt backend (jolt-jni, MIT ✔)
├─ api/             the stable public API for addons
├─ planets/         cube-sphere topology, ghost-halo seams, worldgen density functions, LOD imprints,
│                   orbital LOD sphere, atmosphere scattering, skies, the Distant Horizons generator
├─ grids/           Space-Engineers-style functional blocks and grid systems (power, conveyor, thrust, gyro, …)
├─ kinetics/        Apoapsis Kinetics machines and rotational power
├─ flight/          cockpit, navball, map view, maneuver nodes, autopilots
├─ flightcomputer/  the sandboxed Lua Flight Computer
├─ comms/           owned comms networks, links and radio routing
├─ ages/            staged progression (stage store, item/recipe/ore/dimension gates, quest spine)
├─ ui/              the UI kit and themed widgets
├─ client/          grid rendering, plumes, HUD, post-processing
└─ compat/          optional: create, cc-tweaked, jei, distant-horizons, sodium-iris (Mekanism is a core dependency, used directly)
```

Outside the Gradle build sit `pack/` (the "Apoapsis Ages" modpack definition: recipes, quests, configs), `art/`, `server/`, `tools/` and `docs/` (§3.0.1).

`orbit` has no Minecraft imports, so it can be tested headless and reused by a web planner or Discord bot.

### 3.0.1 From repo to downloads

A Minecraft mod is a **`.jar`**: compiled classes, assets, data and a `neoforge.mods.toml` describing it and its dependencies. The repo is one Gradle multi-project (above), and it produces these artifacts:

| Artifact | Who uses it | What's in it |
|---|---|---|
| **`apoapsis-<ver>+<mc>.jar`** | Every player (via the modpack) | All core modules, nested with NeoForge **jar-in-jar**, plus embedded libraries (Jolt and jolt-jni natives, the Cobalt Lua VM). One file, so nothing can mismatch |
| **`apoapsis-physics`** (standalone library mod) | Other modders | APC on its own: an open-source physics engine for moving block structures that anyone can build a ship mod on |
| **`apoapsis-kinetics`** (standalone mod) | Players and packs without the space content | The Kinetics machines and power on their own |
| **`apoapsis-planets`** (standalone library mod) | Other space and worldgen mods | Cube-sphere topology and ghost halos, the planet generator toolkit, the orbital LOD sphere, atmosphere scattering, rings and skies, and the Distant Horizons LOD generator. Register a planet with data plus a generator and get it rendered from orbit |
| **`apoapsis-ages`** (standalone mod) | Modpack makers | The staged-progression engine (item, recipe, ore, dimension and mob gates, quest book), an open replacement for the old Game Stages family |
| **`apoapsis-comms`**, **`apoapsis-flightcomputer`**, **`apoapsis-ui`** (standalone libraries) | Other mods | Owned comms networks and radio routing; the sandboxed Lua computer; the UI kit and themed widgets |
| **`apoapsis-api`** and **`apoapsis-orbit`** (Maven libraries) | Addon authors, tool builders (web planner, Discord bot) | The stable public API (`16` §16.8), and the pure-Java orbit library |
| **`Apoapsis Ages.mrpack`** (modpack) | Players | A *list*, not a bundle: the Apoapsis jar plus exact versions of Mekanism, JEI, Lithium, Distant Horizons, Continuity (our NeoForge port, unless we write our own reader), Plasmo Voice, Open Parties and Claims and Controlify, plus Sodium and Iris as optional extras. The launcher downloads each from its official page (`09` §9.5). Maintained in the repo's `pack/` folder with packwiz (MIT ✔) |
| **Server pack and Docker image** | Server owners | The same list for servers, with profile configs (`15` §15.1, `16` §16.9) |

**Rules:**
- **Library first, Apoapsis feature second.** Every major system is a module with its own public API package, published as a standalone mod jar and a Maven artifact. The main `apoapsis.jar` nests *the same artifacts* through jar-in-jar, so the standalone and bundled versions can never drift. Someone who wants only our planet rendering, physics or Ages gating declares a dependency; nobody has to extract code (although MIT allows it, with the notice kept).
- **Dependencies only point downward**, from gameplay modules to libraries, never the reverse (table below). Libraries never pull in grids, flight or Mekanism, so a mod that wants only our planet rendering gets `planets` and `orbit` and nothing else.
- **Versions:** each module has its own semantic version, so dependents can pin compatible ranges (`16` §16.8). Module versions are separate from the game's `MAJOR.MINOR.PATCH+MC` release number (`docs/releases/README.md`), and each game release lists the module versions it bundles.
- **Other mods are never compiled into our jar.** They keep their own jars, licences and update cycles. The one planned exception is Infiniverse (MIT), vendored because a Modrinth pack can't fetch it (`09`). Only libraries we're allowed to embed (MIT, BSD, Apache) go into jar-in-jar, with their notices (`09` §9.8).
- **The repo layout** is one monorepo: the Gradle modules in the tree above, plus `pack/` (modpack definition), `art/` (sources: `.bbmodel`, `.svg`, texture scripts), `server/` (Docker), `tools/` (calculator, balance sheets, art pipeline) and `docs/`.
- **Developers** clone the repo and run `gradlew runClient`. **Players** never touch GitHub; they install from Modrinth or CurseForge.

**Module dependencies (inside Apoapsis):**

| Module | Depends on |
|---|---|
| `orbit` | Nothing (pure Java) |
| `physics`, `planets`, `comms` | `orbit` |
| `ui`, `ages`, `kinetics`, `flightcomputer` | Nothing inside Apoapsis. Their grid and space integrations live in the gameplay modules |
| `api` (gameplay extension points, interfaces only) | `orbit` |
| `grids` | `physics`, `kinetics`, `comms`, `flightcomputer`, `ui`, `api`, plus Mekanism |
| `flight` | `grids`, `planets`, `orbit`, `ui` |
| `client`, `compat` | The modules they render or bridge |
| `apoapsis` (the main jar) | Everything above |

---

## 3.1 The physics backend decision (the Sable question)

| Option | Code | Licence | Latest MC | Verdict |
|---|---|---|---|---|
| **Sable** (ryanhcode) | 58.6k Java + 5.9k Rust (Rapier via JNI), 396 mixin files ✔ | **PolyForm Shield 1.0.0** ✔: source-available, non-compete clause | 1.21.1 only ✔ | Best tech and powers Create Aeronautics, but its non-compete covers "any product that competes with the software" or with products the licensor provides using it. An SE-style ship mod could plausibly compete with Create Aeronautics. **Do not depend on or fork it without a written exception from ryanhcode** |
| **Valkyrien Skies 2** (official) | 29.8k Java + 15k Kotlin, 417 mixin files ✔ | Mod is LGPL-3.0 ✔. Physics core `vs-core`/Krunch comes as **external maven artifacts** that aren't in the repo ✔ (source/licence unverified) | 1.20.1 release ✔ (a 1.21.1 branch exists, 430 mixin files ✔) | You can fork the mod but not its physics core. Orbital features need solver-level changes |
| **VS2 unofficial port** (LumixTeam) | VS2 fork | LGPL-3.0 ✔ | **26.2** ✔ (plus 1.21.11, 1.21.1) | Shows a 400-mixin physics mod *can* be brought to 26.x by a small team. Useful as a **prototype host** |
| **APC: own engine on Jolt** | New | MIT (charter, `09`). Jolt is MIT, jolt-jni is MIT ✔ | Whatever we target | **Chosen.** It owns every mixin, puts orbital frames inside the solver loop, and has no licence or upstream wait |

**Decision.** Build **APC (Apoapsis Physics Core)**: a sub-level engine in the same architectural family as Sable and VS2, written fresh against 26.3 with Jolt as the solver behind a `PhysicsBackend` interface. Rapier itself is Apache-2.0, so a Rapier backend stays possible later without touching Sable's PolyForm code. Stage 0 prototypes gameplay on the **VS2 unofficial 26.2 port** behind a thin `GridHost` interface. That way thrusters, gyros and the orbit loop can be playtested while APC is built, and the swap happens during Stage 1 (`12-roadmap.md`).

**Open-source charter (`09`).** Even a private licence exception wouldn't make Sable open source, so Sable is out unless its author relicenses it under an OSI licence. APC is the path either way, and APC itself is MIT.

### 3.1.1 APC design

- **Grid = sub-level.** Each grid owns sparse 16³ chunk sections in a grid-local container rather than a far-off "shipyard" region of the world. That avoids eating world coordinates and keeps grids portable between dimensions. Every block keeps its real `BlockState` and `BlockEntity`, so **any vanilla, Mekanism or Apoapsis Kinetics machine works on a ship** (and Create's too, when its optional compat is installed): a Mekanism Electrolytic Separator on a tanker, or a Kinetics crusher line on a mining barge.
- **Collision.** Greedy-meshed box sets per section feed a Jolt `MutableCompoundShape` with incremental rebuild on block change. Grid-vs-terrain collision samples world `VoxelShape`s near the grid's AABB into a cached static compound.
- **Mass.** A data map `apoapsis:block_mass` (kg per block) with tag-based defaults (stone 2,600, iron 7,800, wool 100, …) gives centre of mass and inertia tensor, both updated incrementally.
- **Entities on grids.** Entity movement and collision are transformed into grid-local space (R6: Sable has 47 mixin files and VS2 has 32 in this area, the biggest risk area). APC routes these through one `EntityGridMotion` facade, so a version hop only touches one place.
- **Joints.** Rotor/hinge map to `HingeConstraint` + motor, piston to `SliderConstraint`, merge block to merging grids, connector to a fixed constraint that also links inventories, landing gear to a fixed constraint to world or grid, and wheels to Jolt's `WheeledVehicleController`.
- **Split and merge.** Incremental union-find over block adjacency. Destroying the last link splits a grid in two, SE-style.
- **Buoyancy.** Per-section submerged volume times fluid density (water, `liquid_methane`, gas-giant layer density).
- **Damage.** Impulse at each contact becomes integrity damage on the blocks involved (SE's integrity model, `05` §5.3). There's no mesh deformation.
- **Scale.** Each grid has a scale factor. **Large grids** use 1 m blocks. **Micro grids** use 0.25 m blocks for drones, fighters and greebles. Players can't walk inside micro grids.
- **Threading.** Fixed 60 Hz physics step on a Jolt job system, synced to the 20 Hz server tick. Clients interpolate 20 Hz snapshots; the pilot gets client-side prediction.
- **Precision.** Grid-local float, bubble-local double, universe double (§3.3). Use jolt-jni's double-precision build if it ships one (a `snapshot-dp.bin` artefact suggests so; **verify**).

---

## 3.2 Planet topology: cube-sphere with ghost-halo seams

### 3.2.1 Options considered (R7)

| Technique | Precedent | Verdict |
|---|---|---|
| Curvature shader only | World Curvature shaderpacks | Visual only. Used *on top of* the chosen approach |
| Torus or cylinder wrap | **Toroidal World** (26.3 ✔ via R7) | Cheap and proven on 26.x, but it isn't a sphere: no real poles or latitude. **Fallback tier.** |
| Separate planet bodies in a space dimension | Genesis (VS addon), Ad Astra | Fine for orbit, but surfaces still need a topology |
| Per-face cube-sphere | Zoxel devlog, SE2's cube-mapped heightmaps | **Chosen**, with a seam technique built for Minecraft |
| Geodesic or Goldberg grid | Climate models | Hex cells don't map to Minecraft's cubic block grid |

### 3.2.2 The Apoapsis cube-sphere

- **One dimension per planet.** It holds **six face regions**, each N×N blocks with N = πR/2 rounded to whole chunks. On Verdance (R = 64 km) that's about 100,500 blocks per face, with faces laid out a million blocks apart in a 3×2 grid. Even a real-Earth face (10 M blocks) fits inside the ±30 M world border (R7 §4).
- **Each face is flattened.** Inside a face, "up" is the planet's radial direction and gravity is plain vanilla *down*. **Vanilla movement, mobs, redstone, Kinetics and Mekanism all work unmodified on every face**, which is the main compatibility win.
- **Mapping.** Block `(face, u, v, y)` maps to a sphere direction through the **tangent-adjusted (equal-angle) cubed-sphere** mapping, chosen for lower area distortion (R7 §2; exact distortion ratios unverified). Worldgen samples 3D noise at `direction × (R + y)`, so terrain is continuous across face edges by construction. The custom bit is a density-function wrapper; the rest is vanilla `minecraft:noise` (R9 §1, §5).
- **Height.** Dimension `min_y -2032`, height `4064` (the 26.3 maximum, R9 §1). Surfaces sit around y 0–600, Thalassa's abyss goes down to -1,900, and the **transition altitude** is about 1,800 m (§3.3.2).

### 3.2.3 Ghost-halo seams (the Minecraft-specific trick)

Rendering the neighbour face through portal-style transforms would mean fighting the 26.x renderer, Sodium and Iris. Apoapsis copies the neighbour instead:

1. Every face gets a **halo margin** of H chunks (default 8) past each edge.
2. Halo chunks are **mirrors** of the neighbouring face's border chunks, rotated by a multiple of 90° about Y. Across a cube edge both faces' "up" is radial, so the transform is just a 2D rotation and translation. Gravity never changes at a seam.
3. The server keeps mirrors in sync. A block change on either side is written to the true chunk and re-mirrored. Block entities in a halo are inert proxies that render the source's state, so no machine ticks twice.
4. An entity that crosses the true edge by one block is **transferred** to the neighbour face, with position, velocity and yaw rotated. The surroundings are identical, so nothing visibly changes.
5. The client just renders ordinary chunks. **No custom renderer, and full Sodium, Iris and mob-AI compatibility.**

**Cost.** Halo chunks only exist where players are, so the cost scales with players near seams, not with planet size.

### 3.2.4 Corners: Vertex Spires

Three faces meet at each of the cube's 8 corners, so walking around one turns you 270° instead of 360°. Halos can't be consistent there. Worldgen forces a **Vertex Spire** within about H×16 blocks of every corner: a colossal pillar, caldera or ice-mountain whose footprint covers the singularity. The awkward spot becomes a landmark visible from orbit, and lore makes the spires "Precursor anchors" with rare ores and strange compass behaviour (`07` §7.2). Grids that fly near a corner use the face their centre of mass is over.

### 3.2.5 Making it feel round

- A **curvature shader** applies a vertex offset `Δy = −d²/2R` in the chunk pipeline, via a Sodium/Iris compat hook. On Verdance the horizon drops 125 m at 4 km, which reads clearly with Distant Horizons LODs and from altitude.
- **True latitude and longitude** drive climate, auroras, sun angle and local solar time (§3.5).
- The **Planet Imprint Service** (KSP's PQS idea) keeps a quadtree of low-res height and albedo tiles per face. They're generated deterministically from the worldgen noise and baked with "imprints" of player-modified chunks (bases glow on the night side). It feeds the orbital LOD sphere, far terrain past render distance (through our own Distant Horizons LOD generator, `17` §17.5), and the landing-site map.

**Tier A, used as the Stage 2 stepping stone** (`12`). If seams slip, a world-creation option generates planets as **cylinder worlds** (east-west wrap, latitude along Z, impassable polar storm walls) using the same planet JSON. R7 shows Toroidal World already does wrap-around on 26.x.

---

## 3.3 Dimensions and frames

| Frame | Units and precision | Lives in |
|---|---|---|
| **Universe** | metres, double, star-centred inertial | `orbit` library |
| **Body** | SOI-parent-centred, inertial and rotating variants | `orbit` |
| **Bubble** | local floating origin, double → block coordinates | `apoapsis:void` dimension |
| **Surface** | block coordinates on a face | per-planet dimension |
| **Grid** | grid-local block coordinates, float physics | APC sub-level |

### 3.3.1 The void dimension and bubbles

Everything above the transition altitude lives in **`apoapsis:void`**, an empty dimension. Objects within 2.5 km of each other share a **bubble**: a local frame anchored to a cell of a 200,000-block lattice (about 89,000 cells inside the world border). Bubbles merge at 2.5 km and split at 5 km (hysteresis). When the bubble's contents drift more than 2 km from the anchor, the whole bubble is **re-based** by an integer chunk offset (KSP's floating origin, SE's cluster model, R7 §4). Bubble position in the universe is stored as a double, so a ship 30,000 km from the star is still exact to the millimetre locally.

### 3.3.2 Surface ↔ orbit transition

When a grid climbs past the **transition altitude** (about 1,800 m, set per planet) it moves from the planet dimension to the void with its passengers. The client is pre-warmed with the destination bubble so there is **no loading screen**. Below the grid, the real chunks cross-fade into the Imprint LOD sphere, and because both come from the same generator the seam is invisible. Descent is the reverse: the LOD resolves into chunks loaded ahead of the grid by chunk tickets along its velocity.

The surface frame uses uniform "down" gravity scaled by `g(h) = g₀(R/(R+h))²`, so the magnitude matches the void frame's radial inverse-square at the hand-off.

**Speed limit in the surface frame.** Default 150 m/s horizontal, set per server, so chunk generation keeps ahead. In practice that means climb first, then accelerate horizontally above the transition, which is how rockets fly anyway.

---

## 3.4 Orbital engine (`orbit` module)

- **Model:** patched conics, exactly as KSP does it (R3 §1). Each body has a designer-set **gameplay SOI**. Stock KSP uses the Laplace radius `a(m/M)^0.4` (R3 §5). Apoapsis's compact values are mostly larger than Laplace, and some exceed the Hill radius. That's harmless for patched conics; the n-body research mode uses physical values instead.
- **Propagation:** universal-variable Kepler propagation (elliptic, parabolic and hyperbolic in one solver) with a Newton–Raphson solve on Kepler's equation.
- **Events:** SOI exit and entry, periapsis and apoapsis, atmosphere interface, terrain impact, and closest approach to a target, found by root-finding on patched segments. They feed alerts, autopilot and the cryo-link pings (§3.5).
- **Two states per grid:**
  - **On-rails:** Keplerian elements plus epoch, costing almost nothing. Thousands of satellites and debris pieces are free.
  - **Active:** full APC rigid-body simulation inside a bubble, gravity from the SOI body as a point mass, plus atmosphere.
  - **Active → rails** when there is no thrust, no contact, no atmosphere and no player within physics range (2.5 km), or the pilot requests "coast".
  - **Rails → active** when a player arrives, an atmosphere or impact event is reached, or the flight computer starts a scheduled burn.
- **Background burns.** An autopilot burn with no player nearby uses a coarse RK4 integrator on the grid's summed thrust vector and mass flow, without block physics. You can launch a cargo transfer from home and it flies itself.
- **Atmosphere:** `p(h) = p₀·e^(−h/H)` with a hard top, plus temperature by altitude. **Drag** is the exposed face area × Cd per face (our model, so streamlined hulls matter; From the Depths also recalculates drag as blocks are lost, R8). **Heating** goes as ρv³ on leading faces (`06` §6.4).
- **Gravity presets:** `compact` and `classic` use a point mass. `real` adds J2 oblateness. An optional **n-body research mode** (Principia-style) is singleplayer only.

---

## 3.5 Time model (the multiplayer problem)

KSP multiplayer precedent (R3 §7): Luna Multiplayer gives each warping player a **subspace** (an offset from server time). Ships in stable orbit get extrapolated, suborbital ones freeze, and control and update rights follow proximity locks. Precedent trackers list consensus warp, subspaces and an operator-set rate as the three options, and none of them is free.

Apoapsis combines all three, layered:

1. **Universal Time (UT).** Server-authoritative double seconds that advance one-for-one with ticks. If the server lags, UT lags with it, so orbits and the world never disagree.
2. **Real time by default.** The Compact system is tuned so the first trips (orbit, Pale) take minutes (`07` §7.1).
3. **Cryo-link** (the SE medical-room idea, generalised). A crew member in a **Cryo Pod** on a coasting ship can *link* to any **Clone Bay** they own and keep playing at base. Their body stays aboard. They get pinged on SOI entry, node time, proximity alerts and arrival, and one click relinks them. The ship's **Flight Computer** flies the nodes. Long transfers cost players no real waiting. The same Link system also lets you take over an android **proxy** anywhere the comms network reaches (`14` §14.1).
4. **Consensus warp** (like sleeping, extended). `/warp request until <event>|<rate>` passes when every online player is *warp-safe*: cryo-linked, asleep, opted-in AFK at base, or on a grid on rails. The rate is capped by the most constrained object (anything under thrust or in atmosphere caps it at 1×). Warp advances UT and on-rails bodies. Chunk ticking is **not** multiplied; crops and furnaces get the same capped catch-up as sleeping.
5. **Singleplayer:** free KSP-style warp from 1× to 100,000× on rails, physics warp up to 4×, and KSP's gates: no rails warp under thrust, in atmosphere or near the surface (R3 §1).
6. **Subspace mode (optional, big servers):** LMP-style per-player offsets. A player in the future can't touch a surface until server UT catches up. It ships off by default, with the trade-offs documented.
7. **Late game:** jump drives and gates (`05` §5.4.8, `06` §6.8) remove the long coasts.

**Local time on a round world.** Planet rotation comes from UT, and local solar angle comes from longitude on the cube-sphere. Mob spawning, crop growth, beds and sky rendering use **local** solar time (one mixin facade over the dimension clock). On 26.x the per-dimension clocks and timelines may be able to carry the planet's reference time (R9 §2; per-dimension day length is unverified, and the fallback is Apoapsis's own clock). Walk west into the night on Verdance, and on Cinder the terminator never moves.

---

## 3.6 Rendering on 26.x

The 26.x renderer is hostile to old mods (R5 top 5): code `RenderPipeline` since 1.21.5, extract-then-render since 26.1, a Vulkan backend since 26.2 (and "Prefer Vulkan" is the default in 26.4 snapshots), plus SDL and a renderer package split in 26.3. **Rule: no raw OpenGL anywhere.** Every custom draw goes through Blaze3D `GpuDevice` and `RenderPipeline`, so Vulkan and OpenGL both work.

| Feature | Approach |
|---|---|
| Grid sections | A `GridSectionRenderer` hooked into the level extraction pass draws each sub-level's section meshes with a model matrix. A Sodium/Iris compat module covers the replaced chunk pipeline |
| Planet sky bodies (moons, gas giant, other planets, rings) | Java sky-pass hook. `DimensionSpecialEffects` was removed in 1.21.11 (R9 §2), so environment attributes handle fog, sky colour and sun angle, and Apoapsis draws the bodies |
| Orbital planet sphere | Imprint quadtree mesh with cube-map albedo and height |
| Atmosphere | Precomputed Bruneton-style scattering lookup textures per planet, built once at load. No verified Java port exists (R7 §5), so write our own; it's well-documented maths |
| Thruster plumes, reentry plasma | Mesh plus particles, with intensity scaled by ambient pressure (plumes balloon in vacuum) |
| HUD, navball, map view | 26.1 GUI extraction API |
| LCD and text panels | Per-block render-to-texture pass, limited per frame |

---

## 3.7 Server performance budget (target)

| Item | Budget on a 4-core server at 20 TPS |
|---|---|
| Players | 10 |
| Active grids (full physics) | 40, average 1,500 blocks each |
| On-rails objects | 2,000+ (analytic, near zero cost) |
| Physics | ≤ 6 ms of each 50 ms tick (Jolt runs on its own threads) |
| Chunk generation | Vanilla noise pipeline plus a density wrapper. C2ME works on Fabric 26.x (R9 §7). A NeoForge port (`c2me-neoforge`, MIT ✔) has 26.3 alpha builds ✔, worth testing once stable |
| Atmosphere sealing | Event-driven flood-fill per grid or room, invalidated on block change, capped at 8,192 cells per room. That's an improvement on Ad Astra and Galacticraft, which re-scan periodically with no invalidation (R9 §6) |
| Grid chunk tickets | NeoForge `TicketController` (R9 §7), along the velocity vector. No existing mod is verified doing this for moving ships, so treat it as a design assumption to prove in Stage 1 |

---

## 3.8 Persistence

| Data | Store |
|---|---|
| Grid block data | `world/apoapsis/grids/<uuid>/` region-style section files (26.x codecs) |
| Orbital state, bubbles, UT | `SavedDataStorage` (26.1+ level-wide data, R5 §4) |
| Planet imprints | `world/apoapsis/imprints/<planet>/<face>/<lod>/` (regenerable cache) |
| Stage and age progress | Player and team attachments (`04`) |

---

## 3.9 Data-driven content (examples)

```jsonc
// data/apoapsis/planet/verdance.json
{
  "radius_m": 64000, "surface_gravity_g": 1.0, "rotation_period_s": 1200, "axial_tilt_deg": 15,
  "soi_m": 900000, "parent": "apoapsis:meridian",
  "orbit": { "a_m": 2.0e7, "e": 0.01, "i_deg": 0.0, "lan_deg": 0, "argp_deg": 0, "mean_anomaly_epoch_deg": 0 },
  "atmosphere": { "surface_pressure_atm": 1.0, "scale_height_m": 2400, "top_m": 12000, "breathable": true,
                  "composition": {"apoapsis:nitrogen": 0.78, "mekanism:oxygen": 0.21} },
  "topology": { "kind": "cube_sphere", "halo_chunks": 8, "transition_altitude_m": 1800 },
  "worldgen": { "noise_settings": "apoapsis:verdance", "biome_source": "apoapsis:latitude_multi_noise",
                "climate": { "equator_temp": 0.9, "pole_temp": -0.8, "lapse_per_100m": 0.02 } },
  "render": { "albedo_lut": "apoapsis:textures/planet/verdance_lut.png", "scattering": {"rayleigh": [5.8e-6, 13.5e-6, 33.1e-6]} }
}
```

```jsonc
// data/apoapsis/engine/hydrolox_bell_large.json
{ "fuel": {"mekanism:hydrogen": 2.0, "mekanism:oxygen": 1.0}, "mass_flow_kg_s": 18,
  "isp_vac_s": 440, "isp_asl_s": 360, "throttle_min": 0.4, "gimbal_deg": 4, "heat_kw": 900 }
```
