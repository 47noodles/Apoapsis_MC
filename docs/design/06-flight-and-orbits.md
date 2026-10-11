# 06 — Flight, orbits and navigation (the KSP layer)

> Design by Opus. Research inputs: R3 (KSP model, rocketry, aero, navigation, CommNet, science, multiplayer time), R1 (SE thrusters, dampeners, jump drive, antennas). The engine and orbit internals are in `03` §3.4–3.5.

## 6.1 Two control schemes, one ship

Space Engineers and KSP fly differently. Apoapsis supports both on the same ship, selectable per cockpit:

| Scheme | Feel | Inputs | Best for |
|---|---|---|---|
| **Engineer** (SE) | Six-axis thrusters, **inertial dampeners** hold you still | WASD/Space/C translate, Q/E roll, mouse pitch/yaw, Z dampeners | Building, mining, docking, atmospheric VTOL, rovers |
| **Rocket** (KSP) | Main engines on a **throttle**, attitude from gyros and RCS, **SAS** holds heading | Shift/Ctrl throttle, WASD pitch/yaw, Q/E roll, IJKL/HN RCS translate, T SAS, Space stage | Launches, orbital burns, re-entry |

Dampeners also gain KSP-flavoured modes: **Off**, **Hold velocity** (SE classic), **Hold relative to target**, and **Hold orbit** (this one cancels drift but never fights gravity, so the ship stays on its conic).

**SAS modes** unlock through flight-computer modules (§6.9): Stability → Prograde/Retrograde → Normal/Anti-normal and Radial in/out → Target/Anti-target → Maneuver.

## 6.2 Engines and propulsion

All engines are data-driven (`data/apoapsis/engine/*.json`, `03` §3.9). Propellants are **Mekanism chemicals** carried through the grid's conveyor network (`05` §5.2).

| Family | Blocks (sizes) | Propellant | Isp ASL / vac (s) | Character | Age |
|---|---|---|---|---|---|
| **Propeller** | Prop hub plus blades (1×, 3×, 5×) | Rotational power from Kinetics, or an electric motor | Air-breathing | Cheap, slow, great on Murk | 3 |
| **Atmospheric thruster** (SE) | Small, large | Electricity only | Atmosphere only; thrust ∝ air density | Hover craft, SE-style VTOL | 3 |
| **Turbojet** | Intake + engine | Ethylene or hydrogen, plus intake air (O₂ worlds only) | ~3,000 equivalent (air-breathing) | Fast aircraft, spool-up time | 3–4 |
| **Solid booster** | Segment stack + nozzle | Propellant grain (sulphur + carbon + oxidiser salts, cast in a Kinetics press) | 170 / 200 | Can't throttle or shut down. The first rockets | 4 |
| **Hydrolox** | Bells: "Spark" (small, 1×1×2), "Torrent" (medium, 2×2×3), "Colossus" (large, 3×3×4); look from CallMeCarlos's concept (`13` §13.8) | H₂ + O₂ 2:1, from the Electrolytic Separator | 360 / 440 | The workhorse. Needs cryo tanks | 4 |
| **Methalox** | Medium, large, vacuum-optimised | CH₄ + O₂, via Ochre ISRU (Sabatier) or Murk seas | 320 / 365 | Dense fuel, refuel anywhere with CO₂ or methane | 5 |
| **Ion drive** (SE) | Small, large | Electricity + a trickle of hydrogen | 0 / 4,200 | Vacuum only, tiny thrust, huge Δv | 5 |
| **Nuclear thermal** | One size | Hydrogen heated by Mekanism fissile fuel | 300 / 850 | Radiation; the shielding layout matters | 5 |
| **Fusion torch** | Large | Deuterium + tritium or ³He (Mekanism fusion chain) | — / 12,000 | Late-game interplanetary hauler | 6 |
| **Cold-gas RCS** | Single nozzle, quad block | Nitrogen or hydrogen | 60 / 75 | Attitude and docking | 4 |

**Thrust and Isp vs pressure** (our model: linear interpolation by pressure; R3 §2 couldn't verify KSP's exact curve): `Isp(p) = Isp_vac − (Isp_vac − Isp_asl)·min(p/p₀, 1)`, at constant mass flow, so `thrust = ṁ·Isp(p)·g₀`. A vacuum bell is weak at sea level and great in space, so engine choice per stage matters.

**Engine properties:** gimbal range, minimum throttle, spool time (jets), optional limited ignitions (hardcore toggle), heat output (`06` §6.4), and plume size that grows as pressure falls.

## 6.3 Tanks, staging and Δv

- **Tanks** are grid blocks that hold Mekanism chemicals through the standard capability. **Cryo tanks** hold H₂ and CH₄ and slowly boil off unless powered or shaded (hardcore toggle). Tank mass counts its contents.
- **Feed** runs through the grid conveyor network (`05` §5.2). **Fuel valves** split a grid into feed zones, and crossfeed is a valve setting.
- **Staging.** A **Separator** (decoupler) or **Merge Block** splits a grid into stages. The **Stage Controller** block lists stages, and the cockpit shows KSP's stage stack: Space fires the next stage, and an **SRB-first** template is provided.
- **Δv readout** per stage: vacuum, sea level and current pressure, using each stage's real post-separation mass, plus **TWR against the current body**. This is the single most important number in the HUD.
- **Δv map item:** a craftable atlas showing the system's Δv subway map, generated from the live `system` JSON so it stays correct on rescaled servers.

## 6.4 Aerodynamics and re-entry

- **Drag** is computed per exposed block face against the local airflow. **Cd per shape**: full block 1.0, slope 0.4, nose cone 0.15, and vanilla stairs and slabs count as partial. That makes streamlining a real building skill (our model; From the Depths also recalculates drag as blocks are lost, R8).
- **Lift** comes from **wing blocks** (`L = ½ρv²·A·C_L(α)`) and **control surfaces** (elevator, aileron, rudder, flap), all on a stall curve.
- **Heating**: leading faces take a heat flux of roughly ρv³. Every block material has a heat capacity and a maximum temperature (from tags). **Heat-shield blocks** burn ablator: a carbon composite in Age 4, and longer-lasting HDPE-laminated tiles in Age 5. **Radiators** reject heat in vacuum.
- **Parachutes** (SE's parachute hatch plus KSP's deploy logic): a drogue and a main, each with semi-deploy and full-deploy triggers set by pressure or altitude, plus failure past a speed limit.
- **Aerobraking and aerocapture** are viable at Verdance, Thalassa, Ochre (only just) and Tempest. Aerocapturing at Tempest saves about 1.4 km/s (`07` §7.1).

## 6.5 Navigation instruments

| Instrument | Source game | What it does |
|---|---|---|
| **Navball** | KSP | Attitude ball with prograde, retrograde, normal, radial, target and maneuver markers, in the cockpit HUD |
| **Flight HUD** | SE | Speed, altitude (sea and radar), vertical speed, power, fuel, O₂/H₂, dampener state |
| **Map view** | KSP | 3D system map with patched-conic previews, SOI transitions, closest-approach markers, AN/DN, Ap/Pe, period, inclination |
| **Galaxy map** | NMS, Elite | Meridian among its neighbouring stars. After the finale it shows The Reach's systems, gate links and other players' networks (`14` §14.4) |
| **Maneuver nodes** | KSP | Drag handles for prograde, normal and radial Δv, with burn-time estimate and "burn in T−" countdown |
| **Transfer planner** | KSP mods | A porkchop plot from a Lambert solver in the `orbit` library. It lists the next windows within N hours with departure and capture Δv |
| **Landing guidance** | MechJeb-style | Predicted impact point including drag, and a suicide-burn timer |
| **Target mode** | KSP + SE | Relative velocity and distance, a target-relative navball, and docking alignment |
| **Docking camera** | SE + KSP | Connector-mounted camera with alignment reticle. Connectors magnetically pull within 1 m |

## 6.6 Comms and remote control

KSP's CommNet merged with SE's antennas (R3 §6, R1):

- **Remote Control** blocks (probe cores) need a **signal path** to a crewed cockpit or a **Ground Station** to take live commands.
- **Radio antennas** come in tiered ranges and are blocked by planets (Pale's far side is a radio shadow). **Relays** chain the signal.
- **Laser antennas** (SE) give very long range but need line of sight and pointing.
- **Signal delay** is an optional realism toggle, off by default.
- With no signal, a drone keeps running its loaded Flight Computer program. It just can't take live input, so autonomy becomes a design goal.
- **One network carries everything:** drone control, proxy links, radio voice and text, and science uplinks. Each network has an owner and its own access rules, closed by default (`15` §15.6).

## 6.7 Science and survey

KSP's science system, merged with SE's ore detector:

- **Experiments** are grid blocks: thermometer, barometer, magnetometer, seismometer, regolith sampler, atmospheric spectrometer, gravimeter, biology kit (Glacis life), radiation counter (Mekanism radiation) and ore surveyor.
- **Data value** is set by **Body × Situation × Biome**, KSP's matrix. Situations are landed, splashed, flying low, flying high, space low and space high. Value falls each time you repeat a combination.
- **Transmit** for partial value through the comms network, or **recover** the physical data cartridge for full value.
- **Survey Points** are spent at the **Research Bench** to unlock nodes *within* the current Age (`04` §4.2). Ages gate eras; science fills them in.
- **Orbital scanners** (SCANsat-like) paint each cube face's biome, altitude and ore-concentration maps into the Imprint map. That's how you find the Ochre perchlorate flats before you ever land.

## 6.8 Jump drives and gates (late game)

- **Jump Drive** (SE, Age 6). It charges from power (a Mekanism Induction Matrix makes a good capacitor bank) and **jumps a grid to a target orbit**. It can't jump within a gravity well deeper than 0.05 g or inside an atmosphere. Range scales with capacitor ÷ grid mass. Interplanetary jumps also burn **antimatter** (Mekanism SPS) and **cometary exotics** (`07` §7.9). Arrival error grows with distance and shrinks with upgrades. Jumps cancel any time-warp debt, which makes them the late-game answer to long coasts (`03` §3.5).
- **Jump Gates** are found at Hollow, then built from Precursor Cores. A gate pair gives instant transit for any grid that fits the ring, which is endgame logistics and the hook for a second star system.
- **Balance rule:** jump drives arrive in Age 6, so Hohmann transfers, windows and aerocapture stay the core loop for Ages 4–5.

## 6.9 Autopilot modules (MechJeb-like)

**Program cartridges** are inserted into a Flight Computer (`05` §5.7). Each is unlocked with Survey Points:

| Module | Does |
|---|---|
| Ascent Guidance | Gravity turn to a target apoapsis, auto-staging |
| Circularise | At apoapsis or periapsis |
| Node Executor | Executes the next maneuver node |
| Transfer Planner | Builds Hohmann or Lambert nodes to a target |
| Rendezvous | Closest approach, then match velocity |
| Docking Pilot | Connector-to-connector approach |
| Landing Pilot | Targeted landing with suicide burn and drag compensation |
| Station-Keeping | Holds an orbit or relative position (for background burns and stations) |

Players can always write their own in Lua (`05` §5.7). The built-ins teach how.

---

## 6.10 Feature checklist: flight and orbits

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
- [ ] Engine families from §6.2, data-driven
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
- [ ] Autopilot cartridges (§6.9)

**Late game**
- [ ] Jump Drive with gravity-well, atmosphere and mass rules plus antimatter and exotics cost
- [ ] Jump Gates (Hollow, then player-built)
