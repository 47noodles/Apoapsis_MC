# Stage 0 prototype: the code contract

> The fixed interfaces of the 0.0.2 prototype, written before the code so the modules could be built in parallel. Each module owns its own package and files. **Public signatures in the stub files are fixed**: change them only through the integration step. Units are SI everywhere (metres, seconds, kilograms, newtons). 1 block = 1 m.

## What the prototype is

A playable vertical slice, Stage 0's "prototype rocket to orbit" check (`../design/02-feasibility.md` §2.4, `../design/12-roadmap.md` Stage 0):

1. **Space Engineers:** build a ship from blocks, right-click the cockpit to board (the blocks become one moving ship), fly it with thrust, gyros and fuel, land, and turn it back into blocks.
2. **KSP:** climb out of Verdance's atmosphere into real orbit, see it on a full-screen map, and burn for Pale. Pale's gravity takes over at its sphere of influence. Land on Pale at 0.16 g.
3. **SevTech:** the ship parts are locked by Age. Milestones raise your Age, and the last prototype milestone is *First Flight*.
4. **Test worlds:** a save that starts you in a ready-built rocket (the *Kestrel*) in low Verdance orbit, with a second one on a launch pad.

Placeholder art and UI. Round planets, real grid physics (Jolt), Mekanism and multiplayer polish come in later stages (`../plan/`).

## Modules and owners

| Package / path | Owner node | Contents |
|---|---|---|
| `orbit/` (Gradle subproject, pure Java, no Minecraft) | ORB | Kepler propagation, elements, bodies, patched-conic predictor, RK4 |
| `dev.apoapsis.grid` | BLK | Blocks, block items, fuel-tank block entity, creative tab, `ShipParts` |
| `dev.apoapsis.ship` | SHIP | `ShipEntity`, `ShipStructure` persistence and codec, assembly, surface physics, pilot input handling |
| `dev.apoapsis.flight` | FLT | Regimes, dimensions, transitions, orbital flight, universal time, time warp, state sync |
| `dev.apoapsis.ages` | AGE | Age store, gates, milestones, `/apoapsis age` |
| `dev.apoapsis.scenario` | SCN | Kestrel blueprint, `/apoapsis scenario`, headless test saves |
| `dev.apoapsis.client.ShipClient` (+ new client files it adds) | SHIPC | Key mappings, input sending, ship renderer |
| `dev.apoapsis.client.MapClient` (+ new client files) | MAP | Full-screen system map, flight HUD |
| `dev.apoapsis.client.SkyClient` (+ new client files) | SKY | Bodies in the sky |
| `dev.apoapsis.test` | GT | GameTests |
| `assets/apoapsis/{textures,models,blockstates,items}` | ART | Placeholder art |
| `Apoapsis`, `ApoapsisClient`, `net/`, `client/ClientState`, `build.gradle` | S0, INT | Entry points, payloads, shared client state |

**Strings:** each module writes its own `src/main/lang/en_us/<module>.json`. The build merges them into `en_us.json` and fails on a duplicate key. **Data** (`src/main/resources/data/apoapsis/...`): each module only writes the folders named in its spec.

## Registry ids (fixed)

| Id | Kind | Owner | Notes |
|---|---|---|---|
| `apoapsis:hull_plate` | block + item | BLK | 400 kg. Age 3 |
| `apoapsis:cockpit` | block + item | BLK | Horizontal facing. 800 kg. Right-click to board. Age 3 |
| `apoapsis:gyroscope` | block + item | BLK | 500 kg, 200 kN·m. Age 3 |
| `apoapsis:fuel_tank` | block + item + block entity | BLK | 150 kg dry, holds 1,200 kg hydrolox. Age 3 |
| `apoapsis:spark_thruster` | block + item | BLK | Facing = exhaust direction. 350 kg. 100 kN vacuum, Isp 440 s vacuum / 360 s sea level. Age 3 |
| `apoapsis:regolith` | block + item | BLK | Pale ground |
| `apoapsis:hydrolox_canister` | item | BLK | Right-click a fuel tank: +300 kg. Age 3 |
| `apoapsis:ship` | entity | SHIP | `ShipEntity` |
| `apoapsis:orbit` | dimension | FLT | Void, starry black sky; ships in orbit live here |
| `apoapsis:pale` | dimension | FLT | Flat regolith over basalt, sea level y = 64, no atmosphere |
| `#apoapsis:ship_blocks` | block tag | BLK | Blocks assembly may include (at most 1,024 per ship): all of the above except regolith, plus glass, iron/copper blocks, lamps |

## Bodies (from `tools/calc/system.py`, g0 = 9.81)

| | Verdance | Pale |
|---|---|---|
| Radius | 64 km | 20 km |
| μ = GM | 9.81 × 64,000² ≈ 4.018 × 10¹⁰ m³/s² | 0.16 × 9.81 × 20,000² ≈ 6.278 × 10⁸ |
| Atmosphere | top 12 km, ρ₀ 1.225 kg/m³, scale height 2 km | none |
| Orbit | — | circular, 320 km, prograde in Verdance's XY plane, phase 0 at UT 0 |
| SOI | — | Laplace, ≈ 60.6 km |
| Surface dimension | `minecraft:overworld`, sea level y = 63 | `apoapsis:pale`, sea level y = 64 |
| Transition altitude | 2,000 m | 1,500 m |

Low Verdance orbit at 15 km is ≈ 713 m/s with an 11.6-minute period.

## Frames

- **Surface frame** (overworld, Pale): Minecraft axes, +x east, +y up, +z south. Altitude h = y − sea level.
- **Body inertial frame** (orbit library): origin at the body centre, X and Y in the equatorial plane, Z north. The prototype ignores planet rotation.
- **Surface ↔ inertial:** the surface dimension is a tangent map around (lat 0, lon 0): lon = x / R, lat = −z / R, r = (R + h)·(cos lat cos lon, cos lat sin lon, sin lat). The local basis is E = (−sin lon, cos lon, 0), N = (−sin lat cos lon, −sin lat sin lon, cos lat), U = r̂. Velocity: v = vx·E − vz·N + vy·U. Orientation: compose the ship's orientation with the matrix whose columns are (E, U, −N), which maps surface axes (x, y, z) into inertial.
- **Orbit-dimension axes ↔ inertial:** local (x, y, z) = (X, Z, −Y). A proper rotation, so the stars, the planet below and thrust all agree.
- **Ship body frame:** block grid axes as assembled. The entity position is the world position of the **centre of mass**. A block at local `p` has its centre at `position + R·(p + 0.5 − com)`.

## The seam: SE layer ↔ KSP layer

`ShipEntity.tick()` (server) does, in order:
1. `Environment env = FlightHooks.environment(this)`.
2. Apply the pilot's input (throttle, rotation commands; SAS damps angular velocity).
3. Rotation: torque from gyros, integrate angular velocity and orientation.
4. If `env.orbital()`: compute the thrust acceleration in dimension axes and call `FlightHooks.orbitalTick(this, thrustAccel, 0.05)`. Translation belongs to FLT.
5. Otherwise surface physics: gravity + thrust + drag (CdA = 0.5 × blocks^(2/3) m², ½ρv²), integrate at 4 substeps, collide with terrain per axis, set `landed`.
6. Burn fuel: ṁ = F / (Isp·g0).
7. `FlightHooks.afterTick(this)`: FLT checks transitions, milestones and sends `OrbitStatePayload` to the pilot every 5 ticks.

## Network (fixed, in `dev.apoapsis.net`)

| Payload | Direction | When | Handler |
|---|---|---|---|
| `ShipInputPayload(shipId, pitch, yaw, roll, throttle, flags)` | client → server | every tick while piloting | `ShipControls.handle` (SHIP) |
| `OrbitStatePayload(shipId, body, regime, ut, warp, r, v)` | server → client | every 5 ticks to the pilot | `ClientState.accept` |
| `AgeSyncPayload(age)` | server → client | login and on change | `ClientState.accept` |

## Controls (keyboard and mouse first; Java Edition)

| Key | Action | Notes |
|---|---|---|
| Right-click cockpit block | Board: assemble and sit | |
| Right-click a ship | Sit in its cockpit | |
| W / S | Pitch: tip the nose forward / back | Vanilla movement keys, read while piloting |
| A / D | Yaw: tip the nose left / right | |
| Q / E | Roll left / right | While piloting, Q's drop and E's inventory are suppressed |
| R / F | Throttle up / down | While piloting, F's swap-hands is suppressed |
| Z / X | Full / cut throttle | |
| G | SAS (hold attitude) on or off | Not T: T is chat |
| B | Disassemble (landed and slow) | |
| M | System map | |
| . / , | Time warp up / down | Orbit only, engines off, singleplayer |
| Shift | Leave seat | Vanilla dismount |

## Ages in the prototype

| Age | Name | Reached by | Unlocks here |
|---|---|---|---|
| 0 | Landfall | start | vanilla |
| 1 | Hearth | holding a copper ingot | — |
| 2 | Steam and Brass | holding an iron ingot | — |
| 3 | Current | holding a piston | hull plate, cockpit, gyroscope, fuel tank, Spark thruster, canister |
| 4 | Ascent | *First Flight*: 1,500 m in a ship | nothing new in Stage 0 (orbital operations begin) |
| 5–6 | Expanse, Beyond | later stages | — |

Gates: the crafting result is withheld, and placing or using the item is refused with a calm action-bar line ("Needs Age 3 · Current"). Tooltips show the Age. `/apoapsis age`, `/apoapsis age get <player>`, `/apoapsis age set <player> <0-6>`, `/apoapsis age sandbox <on|off>` (set and sandbox need permission level 2). Sandbox (everything unlocked) is a per-world flag.

**Custodian guide.** Ten short steps from the Custodian walk the survival loop (parts, build, launch, Age 4, orbit, map and burn, landing on Pale, sample, home, loop closed). Each opens once, on the event it is about (the login start, crafting a cockpit, first boarding, a first, coming back into Verdance's sphere of influence), and is stored per player (`apoapsis:guide`). `/apoapsis guide` shows the current step again; `/apoapsis guide book` gives all steps as a written book. The text is in `lang/en_us/guide.json`. A step shows on a small panel at the top right for 25 seconds (payload `apoapsis:guide_step`, the step's number; the client reads its own lang text), so it never covers the flight instruments at the bottom of the view; a client without the channel gets the header and two lines in chat instead.

**Control frame.** The nose is the thrust direction. Forward is the cockpit's facing. Right = forward × nose. Pitch turns about right (W tips the nose toward forward), yaw about forward (D tips the nose right), and roll about the nose (E turns the view right). Rotation is rate-controlled: gyros drive the angular rate toward the input. With SAS on and no input, the rate is driven to zero.

## Test worlds

- `/apoapsis scenario pad`: builds the *Kestrel* on a pad next to you, fuelled, and raises you to Age 4 quietly (no Custodian announcement, since no flight happened).
- `/apoapsis scenario orbit`: puts the *Kestrel* in a 16 km circular Verdance orbit with you in the cockpit, Pale about 2 minutes from the transfer window (a burn of about 190 m/s prograde). It raises you to Age 4 quietly.
- `/apoapsis scenario lander burn <200-5000>`: the *Kestrel* lander with full tanks, four landing legs and no parachutes, that many metres above sea level over you, with you in the cockpit (for the radar altitude and burn cue).
- `/apoapsis seamless dive <altitude> <speed> <path deg> <plain|chutes>`: a *Kestrel* in the orbit regime above Verdance at that speed, its velocity that many degrees below prograde and its nose retrograde, with you in the cockpit (for re-entry heating; `chutes` is the lander with parachutes and empty tanks).
- `gradlew runTestFlight` starts the game with the system property `apoapsis.testFlight=true`. On the title screen it opens the save **Apoapsis Test Flight**, creating it on first launch (creative, cheats on, fixed seed). On the first join it runs the orbit scenario and builds a second Kestrel on a pad at spawn. No dedicated server and no EULA file are involved.

- `gradlew runTestReturn` flies the whole way home by script in one run (Pale landing, ascent, transfer back, Verdance re-entry with chutes, landing on legs) with the player's own controls, and writes `run/screenshots/m09-telemetry.csv` and `m09-*.png`; `-PreturnStage=ascent|escape|entry` starts a later leg from a labelled stage start (`/apoapsis scenario lander pale [fuel %]`, `/apoapsis seamless orbit <body> ... lander`, `/apoapsis seamless dive ... lander`).

## Cross-module hooks added before dispatch

- `grid.FuelTankBlockEntity`: `getFuelKg()`, `setFuelKg(double)`, `CAPACITY_KG = 1200` (BLK implements; SHIP and SCN call).
- `ShipEntity.setTransferring(boolean)` and `isTransferring()`: while true, the ship lets its pilot dismount and does no physics (FLT sets it during dimension moves). Otherwise the ship refuses dismount in the orbit dimension (no EVA in the prototype).
- `MapClient.openMap()`: SHIPC's M key calls it; MAP implements.
