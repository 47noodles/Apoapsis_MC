# 10 — Master feature checklist

> Every feature in the design as one list to tick off. The Progression, Construction, Flight and Materials sections are copied from the checklists at the end of files 04, 05, 06, 08, 13, 14, 15, 16 and 17; the rest are written here.

## Architecture and platform (03)
- [ ] Gradle multi-module layout (`orbit`, `physics`, `api`, `planets`, `grids`, `kinetics`, `flight`, `flightcomputer`, `comms`, `ages`, `ui`, `client`, `compat`), with `pack/` outside the build
- [ ] NeoForge 26.3 build. Java 25. Loader-specific code in a thin per-loader layer so a Fabric build can follow
- [ ] Mixins behind per-subsystem facades (`EntityGridMotion`, render, storage, network)
- [ ] APC: sub-level grids, Jolt backend, collision meshing, mass data map, joints, split and merge, buoyancy, impulse damage, micro-grid scale, 60 Hz physics thread, snapshots and prediction
- [ ] `PhysicsBackend` interface (Jolt now, Rapier possible)
- [ ] Void dimension, bubbles (merge 2.5 km, split 5 km), floating-origin re-basing
- [ ] Surface ↔ orbit transition at the transition altitude, no loading screen, LOD cross-fade
- [ ] Surface-frame speed limit and chunk tickets along velocity
- [ ] Rendering only through Blaze3D (no raw GL); works on the OpenGL and Vulkan backends
- [ ] Persistence: grid section files, `SavedDataStorage` orbital state, Imprint cache
- [ ] Performance budget scenario in CI (`03` §3.7)

## Planets and worlds (07)
- [ ] Cube-sphere topology: six faces per planet dimension, equal-angle mapping, 3D-noise terrain continuity
- [ ] Ghost-halo seams: mirrored halo chunks, write-through edits, inert block-entity proxies, entity transfer with rotation
- [ ] Vertex Spire landforms at all eight corners
- [ ] Curvature shader (with Sodium/Iris compat path)
- [ ] Latitude/altitude climate model; axial-tilt seasons (toggle)
- [ ] Local solar time per position (spawning, crops, beds, sky)
- [ ] Planet Imprint Service: quadtree height and albedo tiles plus player imprints; Distant Horizons integration
- [ ] Orbital LOD sphere, atmosphere scattering, rings, sky bodies
- [ ] Planet JSON and system JSON (`compact`, `classic`, `real` presets)
- [ ] Cylinder-world fallback (tier A)
- [ ] **Verdance**: vanilla biome registry with latitude climate plus Crash Scar, Skyreach Highlands, Tidewild Flats (tides), Glasswood, Basalt Rift, Meteor Steppe, Aurora Taiga, Deep Kelp Shelf, Luminous Hollows, Vertex Spire
- [ ] **Pale**: Mare Basalt, Highland Regolith, Ray Fields, Cold Traps, Peaks of Eternal Light, Lava Tubes; dust, micrometeorites, far-side radio shadow
- [ ] **Cinder**: Dayside Crucible, Terminator Belt, Nightside Glacier, Ironglass Badlands, Fumarole Fields; heat-budget mechanic; tidal lock
- [ ] **Ochre**: Rust Dunes, Canyon Labyrinth, Tharsis Flank (Mons Ascendant), Polar Layered Deposits, Chaos Terrain, Perchlorate Flats, Precursor Ruins; dust storms; Rook and Pawn rubble moons
- [ ] **Thalassa**: Atoll Archipelago, Kelp Spires, Storm Belt, Twilight Zone, Abyssal Vent Fields, Drowned Plateaus, Polar Ice Shelves; pressure and buoyancy; Leviathan
- [ ] **Tempest**: altitude layers (Upper Haze, Ammonia Decks, Water Decks, the Deep); aerostats; Halo Rings; radiation belts
- [ ] **Ember**, **Glacis**, **Murk** biomes and signature mechanics (`07` §7.7); `liquid_methane` fluid; methane rain
- [ ] **Hollow**: Outer Crust, the Breach (gravity inversion), Arboretum drum (0.3 g outward spin gravity), Machine Strata, Core Lattice (zero g)
- [ ] **The Shoals**: procedural asteroid sub-grids (C, S, M, U classes), derelicts
- [ ] **The Wanderer**: periodic comet, coma and tail render, comet-season event
- [ ] Native fauna per world (`07` §7.10)

## Progression (04)
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

## Construction and grids (05)

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

**Blocks (`05` §5.4)**
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

## Flight and orbits (06)

**Orbital engine**
- [ ] Patched-conic propagation (elliptic, parabolic, hyperbolic) with universal variables
- [ ] Designer-set gameplay SOIs (`03` §3.4) and SOI transition events
- [ ] On-rails ↔ active switching with the rules in `03` §3.4
- [ ] Background burns for unloaded active grids (RK4 on summed thrust)
- [ ] Scale presets: compact, classic, real
- [ ] Optional J2 (`real` preset) and n-body research mode (singleplayer)

**Time**
- [ ] UT locked to server ticks
- [ ] Local solar time on cube-sphere faces drives spawning, crops, beds and sky
- [ ] Singleplayer rails warp (to 100,000×) and physics warp (4×) with KSP gates
- [ ] Multiplayer consensus warp with warp-safe rules
- [ ] Cryo-link to Clone Bays, with event pings and one-click relink
- [ ] Optional subspace mode (off by default)

**Propulsion and resources**
- [ ] Engine families from `06` §6.2, data-driven
- [ ] Isp and thrust by pressure, gimbal, throttle limits, spool, heat
- [ ] Mekanism chemical tanks, cryo boil-off (toggle), fuel valves and crossfeed
- [ ] Stage Controller, separators, Space-to-stage
- [ ] Δv and TWR per stage (vacuum, sea level, current)

**Aero**
- [ ] Per-face drag with shape Cd
- [ ] Wings, control surfaces, stall curve
- [ ] Re-entry heating, block heat limits, ablative heat shields, radiators
- [ ] Parachutes with deploy logic
- [ ] Aerobraking and aerocapture

**Navigation and UI**
- [ ] Navball, flight HUD, map view, maneuver nodes
- [ ] Transfer planner (Lambert solver, porkchop)
- [ ] Landing guidance, target mode, docking camera
- [ ] Δv atlas item generated from live system data

**Comms, science, autopilot**
- [ ] Antenna tiers, relays, planet occlusion, laser antennas, optional signal delay
- [ ] Ground Station block; Remote Control blocks behave sensibly without signal
- [ ] Experiments × situations × biomes science matrix, transmit vs recover
- [ ] Orbital scanners painting Imprint maps
- [ ] Autopilot cartridges (`06` §6.9)

**Late game**
- [ ] Jump Drive with gravity-well, atmosphere and mass rules plus antimatter and exotics cost
- [ ] Jump Gates (Hollow, then player-built)

## Materials and industry (08)
- [ ] Kinetics: torque/RPM model, shaft shear, grid reaction torque, sources and machines from `08` §8.2
- [ ] Kinetics ↔ Mekanism Joules bridge (motor and dynamo)
- [ ] Mekanism dependency (or our 26.x port fork, `09` §9.7). Refinery tiers mapped to 2×–5×
- [ ] Components from `05` §5.6 wired to Mekanism and Kinetics recipes
- [ ] Planet ore-abundance tables (worldgen) per `08` §8.4
- [ ] Five chemicals and one fluid registered through Mekanism's chemical API
- [ ] Atmospheric Intake, Sabatier Reactor, Regolith Sifter, Ice Processor
- [ ] Rare items and their sources
- [ ] Optional Create compat (shared `c:` tags)

## Visual design (13)
- [ ] Brand identity: logo, palette tokens, OFL fonts, key art
- [ ] UI kit (SVG) with gamepad-first focus states, motion rules, UI sounds and the Classic UI toggle
- [ ] Title screen with live orbital scene, universe browser, world-creation wizard, themed loading and transitions, restyled settings, revival sequence (Clone Bay from Age 4), auto-credits
- [ ] Helmet HUD with visor overlays; cockpit HUD with projected markers and live screens
- [ ] Grid terminal, reskinned inventories and machines, survey, system and galaxy maps with continuous zoom, Codex, Discovery viewer, radio console
- [ ] Detailed animated block models, connected textures (Continuity port or our own), decals and paint
- [ ] Our own dynamic lights; emissives
- [ ] Post-processing passes (grading, bloom, vignette, exposure, G-force, heat haze, flare) with sliders
- [ ] Per-world skies (starfield, corona, scattering, rings, auroras)
- [ ] Suit designs and animations; camera feel; photo mode
- [ ] Feedback table (`13` §13.6) implemented for every listed action
- [ ] Accessibility options; golden-image CI tests; performance budgets
- [ ] Design gate: approved mockups or prototypes for each stage's screens and blocks before implementation, with side-by-side design QA

## Best of other games (14)
- [ ] Link system unifying originals, clones (cryo-link) and proxies
- [ ] Link Pod, Proxy Fabricator, Proxy Dock blocks; Vacant Body entity
- [ ] Link rules: signal path, quality tiers, standing orders on link loss, optional light delay
- [ ] Proxy frames (Standard, Lifter, Scout, Diver, Furnace, Precursor) and modules
- [ ] Body-swap implementation (player, inventory and attribute swap) with cross-dimension pre-warm
- [ ] Proxy loaning and permissions; server proxy limits
- [ ] NPC Hub stations (Verdance, Ochre, Tempest) with market, missions, ship broker, proxy rental
- [ ] Player Hub Core and alliances
- [ ] Discovery Registry: scanning, first-discovery naming, Survey Points
- [ ] Fragment scanning for blueprints
- [ ] Derelict boarding encounters with meltdown risk
- [ ] Torch transfers (constant-thrust planner in the `orbit` library)
- [ ] *(Post-1.0)* Capital ships and fleet expeditions · The Reach (procedural systems, gate network, hyperdrive) · settlements · damage-control loop · hazard forecasting

## Multiplayer (15)
- [ ] Server profiles (Friends, Community, Hardcore, Creative) as one-click config presets
- [ ] Consensus warp with warp-safe rules and a vote UI; optional subspace mode
- [ ] Cryo-link and proxy presence (`14`); offline orbits, background burns and arrival notifications
- [ ] Teams (OPAC integration or own store), factions, alliances
- [ ] Land claims (OPAC), orbital claims, offline protection, safe zones, action logs, rollback
- [ ] Multi-crew consoles (pilot, engineer, navigator, gunner) and Mission Control
- [ ] Control locks per grid (request, transfer, drop on disconnect)
- [ ] Shared megaprojects with contribution tracking; player contracts; distress beacons
- [ ] Owned comms networks: join, read, transmit and relay permissions, presets (closed by default), subscribe and block, peering, inter-system relays
- [ ] Plasmo Voice radio add-on: vacuum silence, radio over the comms network, helmet contact, static and optional delay
- [ ] Text channels: team, faction, alliance, local, radio
- [ ] PvE scaling; PvP modes; no-offline-raid option; bounty board
- [ ] Netcode: server-authoritative physics, snapshots, prediction, interest management, relative docking sync, bandwidth caps
- [ ] Planet sleep, rails for far grids, ticket rate limits, `/apoapsis perf`
- [ ] Backups, grid export/import, admin tools
- [ ] Community events (comet season, windows, eclipses, leaderboards)

## Production and polish (16)
- [ ] Art direction (`13`), per-world palettes and light tints, UI kit, VFX library, ship livery
- [ ] Physics-aware audio (air, hull, vacuum), engine sound layers, planet ambiences, adaptive CC BY soundtrack, Custodian voice lines with subtitles
- [ ] Story bible, Custodian arc, faction voices, short in-engine story moments, endings
- [ ] Flight school, Controlify bindings, joystick/HOTAS layer, accessibility options, localisation pipeline
- [ ] Title screen, world-creation wizard, themed transitions, auto-generated credits
- [ ] Patch notes for every release (template, What's new screen, CHANGELOG, upgrade notes)
- [ ] GameTest suites, soak tests, render smoke tests, save-migration framework, opt-in crash reporter, release channels, balance tooling, Lua sandbox hardening, dupe tests
- [ ] `apoapsis-api` artifact with semver, SDK examples, generated docs site
- [ ] `.mrpack`, CurseForge listing, server pack, Docker image, website, trailer, press kit
- [ ] Governance (maintainers, RFCs, ADRs), code of conduct, contributor guides, transparent optional funding within Mojang's rules

## Worlds, biomes and life (17)
- [ ] Verdance on vanilla overworld generation (cube-sphere, latitude climate)
- [ ] Per-world generator datapacks plus custom density functions (sphere mapping, craters, lava seas, ice shells, abyss)
- [ ] Biome composer (families × climate × elevation × weirdness) with the distinctness rule
- [ ] CI coverage report: ≥ 50 biomes per major world, area shares, distinctness, orbital readability
- [ ] Per-world block palettes, colour maps, fog, sky, light tint, particles, ambient sound
- [ ] Optional Iris shader uniforms per world
- [ ] Creature kit (body plans, parts, behaviours, adaptations) and flora kit (part grammars)
- [ ] Species sets per world to the targets in `17` §17.4, each with drops, uses and a Discovery Registry entry
- [ ] Our own Distant Horizons LOD generator from planet noise, with client-side terrain sub-seeds (seed privacy)
- [ ] Near-chunk → DH LOD → Imprint sphere hand-off from one formula
- [ ] *(Fork note)* NMS-style procedural galaxy, random spawns and per-seed species

## Open source and licensing (09)
- [ ] MIT `LICENSE` for code, CC BY 4.0 for assets, SPDX headers, `reuse lint` in CI
- [ ] DCO sign-off on every contribution
- [ ] CI licence gate on the pack manifest (allow-list, Modrinth licence lookup plus an override file)
- [ ] Clean-room rules in CONTRIBUTING.md (`09` §9.3)
- [ ] Optional extras (Sodium, Iris) as optional `.mrpack` entries, never vendored
- [ ] Mekanism 26.x port fork, only if upstream lags (`09` §9.7)
- [ ] Ponder fork-port or our own Holo-Manual
- [ ] A qualified licence and IP read of the charter and pack manifest before 1.0

## Cross-cutting
- [ ] Holo-Manual animated guides for every major system
- [ ] First-flight tutorial chain
- [ ] Localisation pipeline (en_us first, community translations)
- [ ] Accessibility: colour-blind HUD palette, full keybinding, difficulty presets
- [ ] Server admin docs and recommended configs (small friends server, public server)
- [ ] Art style guide (`13` §13.8) and asset pipeline (Blockbench sources in repo)
- [ ] Automated test worlds: seams, grids, orbits, time warp, sealing
- [ ] Playtest programme per stage (`12`)
