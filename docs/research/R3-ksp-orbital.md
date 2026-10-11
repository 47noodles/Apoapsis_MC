# R3: KSP1 Orbital Mechanics, Rocketry, Game Systems, and Shared-Time Multiplayer Precedents

Research node R3 for Apoapsis (Minecraft Java mod: Space Engineers building/physics, SevTech-style staged progression, KSP-style multi-planet orbits, multiplayer). Facts only. The Opus designer builds the design from this file.
Research date: 2026-10-11.

---

## Preface: method, tags, and limits

Tags used on every non-obvious fact:

- [F] Page fetched and read this session (WebFetch). The URL is given next to the tag.
- [S] Seen only in WebSearch result text (secondhand summary). Treat as provisional. Most KSP-wiki facts are [S].
- [D] Derived by calculation this session from [F] or [S] inputs. The method is stated where it matters.
- UNVERIFIED: no source in this session confirmed it. Listed so the designer does not treat it as fact.

Source-access limits:

- wiki.kerbalspaceprogram.com returned an Anubis bot-protection block ("Access Denied") to WebFetch on the Time_warp, Kerbol_System, and SOI pages. All KSP-wiki facts below are therefore [S]. The KSP API pages returned 404, the forums returned 403, and the fandom mirror returned 402.
- The Braeunig orbital mechanics site failed with a TLS certificate mismatch, so the standard state-vector and anomaly conversions are UNVERIFIED.
- The WebSearch budget for this turn ran out on the final search attempt. Remaining gaps are marked UNVERIFIED rather than searched further.
- The Kerbol body data comes from a third-party Haskell library (KSP-0.1 on Hackage), not from a KSP game file. Its values were cross-checked against [S] wiki numbers and against internal consistency (section 5).

Premise corrections found during research (details in sections 7 and 8):

1. KSP2: no source found says colonies or interstellar travel were delivered. The Wikipedia article lists them as planned. Last update June 2024; developer Intercept Games closed in 2024.
2. Kitten Space Agency (KSA): multiplayer is "expected," not described as shipped. Its time-warp page is fully sourced (section 7.4).
3. Juno: New Origins (SimpleRockets 2): no multiplayer or shared time found.

---

## 1. KSP1 simulation model

### 1.1 Patched conics and sphere of influence (SOI)

- Patched conics: a vessel is on a two-body Kepler orbit about one dominant body at a time. Trajectories are "patched" when the vessel crosses an SOI boundary. [F] "In the patched conic approximation, the SOI is the boundary where the trajectory switches which mass field it is influenced by." https://en.wikipedia.org/wiki/Sphere_of_influence_(astrodynamics)
- SOI radius: r_SOI = a (m/M)^(2/5), where a is the semi-major axis of the smaller body's orbit about the larger body, m is the smaller body's mass, and M is the larger body's mass. [F] same URL. The article also gives an angle-dependent form and a direction-averaged value of about 0.9431 a (m/M)^(2/5). [F] same URL.
- Consistency check [D]: using the Haskell KSP-0.1 body data (masses and centre-based semi-major axes, section 5), this formula reproduces every stock SOI value in that source to 0.000%. Examples: Kerbin 84,159,286 m; Mun 2,429,559.1 m; Eeloo 1.1908294e8 m; Jool 2.4559852e9 m. This means the stock SOI radii are exactly the 2/5-power formula applied to the stock masses and orbits. Source: https://hackage-origin.haskell.org/package/KSP-0.1/docs/src/System-KSP-Universe.html (third-party data; [F] for the values).
- Kerbin's SOI quoted by the KSP wiki: about 84,159 km [S] https://wiki.kerbalspaceprogram.com/wiki/Kerbin
- The SOI switch is a hard boundary in the patched-conic model. A search summary of SOI sources describes it as a bookkeeping device rather than a physical edge [S] https://en.wikipedia.org/wiki/Sphere_of_influence_(astrodynamics) (the fetched Wikipedia text describes the SOI as an oblate, direction-dependent region [F]). KSP's exact transition test is UNVERIFIED (see section 9.13).

### 1.2 Two-body propagation and Kepler's equation

- Kepler's equation: M = E - e sin E. Mean anomaly M = n(t - t0). [F] https://en.wikipedia.org/wiki/Kepler%27s_equation
- No closed form exists for E. The standard solution is Newton iteration: E_{n+1} = E_n - (E_n - e sin E_n - M) / (1 - e cos E_n). Start at E0 = M for most ellipses, E0 = pi for e > 0.8. Use bisection when e is near 1, where the derivative approaches zero. [F] same URL.
- How KSP's own solver is implemented: UNVERIFIED (no KSP source or developer statement retrieved). Assume only that KSP propagates Kepler orbits analytically for on-rails bodies and vessels.

### 1.3 On-rails versus physics

- On-rails: a vessel's position is computed from its orbit rather than integrated, so it costs no physics simulation. Physics warp keeps integrating the ship. [S] KSP wiki Physics_warp summary: https://wiki.kerbalspaceprogram.com/wiki/Physics_warp
- High-speed (rails) warp "stops all physical calculations except for gravity and collision." [S] same URL and https://wiki.kerbalspaceprogram.com/wiki/Time_Warp
- The forum explanation: warp "skips ahead rather than simulating," so trajectories through air can produce odd results. [S] https://forum.kerbalspaceprogram.com/topic/107368-is-there-a-reason-for-the-time-warp-limits-at-different-altitudes

### 1.4 Time warp levels and limits

Levels (KSP 1):

- Rails warp multipliers: 1x (default), 5x, 10x, 50x, 100x, 1,000x, 10,000x, 100,000x. [S] KSP wiki summary (https://wiki.kerbalspaceprogram.com/wiki/Time_Warp; search summary in session)
- Physics warp: 1x, 2x, 3x, 4x. Keys: Mod + comma or period. [S] https://wiki.kerbalspaceprogram.com/wiki/Physics_warp

Rules:

- Physics warp is the only warp in atmosphere and is capped at 4x. [S] Physics_warp summary.
- Below about 70 km of atmosphere, full ship physics still runs up to 4x. [S] forum summary: https://forum.kerbalspaceprogram.com/topic/107368-is-there-a-reason-for-the-time-warp-limits-at-different-altitudes
- Rails warp is blocked while the vessel is under non-orbital acceleration: in an atmosphere (pressure > 0), on a surface, or under thrust. [S] Time_Warp summary; forum statement "you can't on-rails warp when pressure > 0" in the same forum thread.
- Near bodies, rails warp is auto-throttled. The game "drops to 50x before a sphere-of-influence change." [S] forum summary, same thread.
- Rails warp can "time warp THROUGH the center of a planet" for a fast trajectory (a forum report, not a sanctioned behavior). [S]
- Per-body altitude thresholds for each warp level exist ("every celestial body has its own reference frame which defines the altitude at which the various speeds become available"). [S] Time_Warp summary. The exact per-body numbers were NOT retrieved. UNVERIFIED.
- Rails rate cap is computed by MaxRailsRateMode with values VesselAltitude (per frame, current and next) or PeAltitude (orbital periapsis). [S] search summary of the KSP API TimeWarp page. The API page itself returned 404 on fetch. UNVERIFIED for exact behavior.
- Rails warp can freeze parts. Bent joints stay frozen during warp and can snap back on exit. [S] Physics_warp summary.
- Unofficial mods show the stock limits are a choice: Time Control (per-planet altitude limits, custom warp rates; notes that KSP caps warp at 1,000,000,000x) [S] https://forum.kerbalspaceprogram.com/topic/143763-112-time-control-211 ; WarpUnlocker removes altitude requirements for non-physical warp (not in atmosphere or when throttled) [S] https://forum.kerbalspaceprogram.com/topic/58862-102-warpunlocker-v1-faster-time-warp-no-altitude-restrictions

### 1.5 Floating origin and Krakensbane

- Krakensbane is KSP's workaround for single-precision loss at large coordinates. It periodically repositions all objects so the active vessel stays near the world origin. [S] https://wiki.kerbalspaceprogram.com/wiki/API:Krakensbane and https://forum.kerbalspaceprogram.com/topic/207101-question-on-krakensbane
- It engages above about 750 m/s, per a forum observation. The wiki's MaxV field defaults to 750, but its author was unsure it is constant. [S] same sources. UNVERIFIED as a hard rule.
- Side effects: positions cannot be compared between two moments because the world moved; the Sun appears to move; code that ignored the shift caused bugs. [S] same sources.
- Historical note: before v0.17 a "Deep Space Kraken" applied phantom forces near the Sun; Krakensbane reduced but did not remove them. [S] https://forum.kerbalspaceprogram.com/topic/141709-origins-of-the-kraken
- Double precision in orbits versus single-precision physics: UNVERIFIED as a documented split. Only the single-precision motivation for Krakensbane was found.

### 1.6 What stock KSP does not model

- Stock KSP is a single-SOI patched-conic model. The Principia page contrasts Principia with "your vessels are influenced by all the celestials" instead of "a single sphere of influence." [F] https://github.com/mockingbirdnest/Principia
- Lagrange points, n-body effects, body oblateness (J2), and tidal effects: no stock-KSP source was retrieved that states these are absent. They are consistent with the patched-conic design. UNVERIFIED as explicit statements.

### 1.7 Principia (N-body mod)

- Adds N-body and extended-body gravitation. Designed to pair with RealSolarSystem. [F] https://github.com/mockingbirdnest/Principia
- Builds listed for KSP 1.8.1, 1.9.1, 1.10.1, 1.11.0 to 1.11.2, and 1.12.2 to 1.12.5. Released "on every new moon." [F] same URL
- Integrator: the README does not name one. The wiki FAQ is empty in the fetched view (https://github.com/mockingbirdnest/Principia/wiki/FAQ). Whether it is symplectic or Runge-Kutta-Nystrom is UNVERIFIED.
- Performance cost: UNVERIFIED. No source gave numbers.
- Flight planning: Principia ships its own planner. A forum user says MechJeb's planner uses instantaneous burns, like stock nodes, and may miss Principia's encounters. [S] https://forum.kerbalspaceprogram.com/topic/185116-does-principia-work-with-mechjeb

---

## 2. Rocketry

### 2.1 Tsiolkovsky rocket equation

- delta-v = v_e ln(m0 / mf) = Isp g0 ln(m0 / mf). m0 is wet mass including propellant, mf is dry mass. [F] https://en.wikipedia.org/wiki/Tsiolkovsky_rocket_equation
- g0 = 9.80665 m/s^2. [F] same URL
- The equation applies to each stage separately; each stage's m0 is the mass after the previous stage is dropped. [F] same URL

### 2.2 Specific impulse and sea-level versus vacuum

- Isp = F / (mdot g0) = v_e / g0, in seconds. [F] https://en.wikipedia.org/wiki/Specific_impulse
- Nozzles work better at lower ambient pressure. Engines are described by a sea-level Isp and a vacuum Isp, and the vacuum value is higher. [F] same URL
- How KSP interpolates thrust and Isp between sea-level and vacuum values as a function of ambient pressure: UNVERIFIED (not retrieved). Assume it is pressure-based from the engine's atmosphere curves; confirm in the stock part configs.
- Thrust-to-weight ratio: TWR = thrust / (mass * local g). Definition only. UNVERIFIED source.

### 2.3 Engine table (KSP 1 stock; [S] unless noted)

Columns: thrust sea-level / vacuum (kN); Isp sea-level / vacuum (s); mass (t) where available.

| Engine | Thrust ASL / vac (kN) | Isp ASL / vac (s) | Mass (t) | Source |
|---|---|---|---|---|
| LV-909 Terrier | 14.78 / 60 | 85 / 345 | 0.50 | [S] https://wiki.kerbalspaceprogram.com/wiki/Parts/Propulsion |
| RE-L10 Poodle | 64.29 / 250 | 90 / 350 | 1.75 | [S] same |
| RE-I5 Skipper | 568.75 / 650 | 280 / 320 | 3.00 | [S] same |
| RE-M3 Mainsail | 1,379.03 / 1,500 | 285 / 310 | 6.00 | [S] same |
| LV-1 Ant | ~0.51 / 2.0 | 80 / 315 | not retrieved | [S] wiki Propulsion table (via search) |
| 48-7S Spark | 16.56 / 20 | 265 / 320 | 0.13 | [S] same |
| LV-N Nerv (nuclear) | 13.88 / 60 | 185 / 800 | not retrieved | [S] https://wiki.kerbalspaceprogram.com/wiki/Nerv |
| KR-2L+ Rhino | ~1,206 / 2,000 | 205 / 340 | 9 | [S] wiki Propulsion (via search) |
| LV-T30 Reliant | 205.16 / 240 | 265 / 310 | not retrieved | [S] https://wiki.kerbalspaceprogram.com/wiki/LV-T30 |
| Mk-55 Thud (radial) | 108.20 / 120 | 275 / 305 | not retrieved | [S] wiki engine table (via search) |
| IX-6315 Dawn (electric) | 0.048 / 2.0 | 100 / 4,200 | not retrieved | [S] wiki Propulsion (via search) |

Notes:

- Reliant patch history: thrust raised from 215 kN to 240 kN, vacuum Isp from 300 s to 310 s. [S] https://wiki.kerbalspaceprogram.com/wiki/LV-T30
- Swivel (LV-T45) and Cheetah: not retrieved. UNVERIFIED.
- The wiki pages do not state which game version the figures describe. Forum posts give older values. Treat the table as approximate.
- KSP 2 values differ from KSP 1 (e.g., Ant 80 / 330 s; Thud 126.2 / 140 kN). [S] KSP 2 wiki summary.

### 2.4 Resource types and ISRU

- Stock resource list (liquid fuel, oxidiser, monopropellant, xenon, solid fuel, ore, electric charge): UNVERIFIED. Not retrieved this session.
- ISRU: Engineers greatly boost the production rate of Resource Harvesters and Converters. [S] https://wiki.kerbalspaceprogram.com/wiki/Astronaut_Complex (engineer capability table summary)
- Mining ore and hauling it home is described by the wiki as a hard, costly investment whose infrastructure can cost more than the ore is worth. [S] https://wiki.kerbalspaceprogram.com:443/wiki/Career

### 2.5 Staging and crossfeed

- Not researched. UNVERIFIED.

---

## 3. Atmosphere and reentry

### 3.1 Pressure and density versus altitude (Kerbin)

- Sea-level pressure 101.325 kPa (1 atm); atmosphere top 70 km (advertised). [S] https://wiki.kerbalspaceprogram.com/wiki/Kerbin and [S] Kerbin infobox summary.
- Reference density 1.225 kg/m^3. The atmCurve uses normalized density where 1.0 corresponds to 1.225 kg/m^3. [S] https://forum.kerbalspaceprogram.com/topic/105453-great-new-physics-thread/page/2
- Another forum post derives about 1.2231 kg/m^3 from 101,325 Pa and 293.15 K. [S] Two values, one from the normalized curve and one computed; both are approximately 1.22 kg/m^3.
- Kerbin's atmospherePressureCurve starts with key "0 101.325 0 -0.01501631" (altitude, pressure, two tangents). [S] forum post in search summary. The full curve is in the Kopernicus kittopia-dumps GitHub repository, not fetched. UNVERIFIED for the full curve.
- Model basis (1.0+): the pressure curve is based on the U.S. Standard Atmosphere, with the developers converting geopotential to geometric height and then multiplying heights by 0.8 to lower the top to about 70 km. [S] https://forum.kerbalspaceprogram.com/topic/200953-kerbin-scale-height (one poster's reading; not confirmed by developers).
- Pre-1.0: a single exponential with a 5.6 km scale height; the KSP wiki drag article states about 5600 m. [S] search summary of https://wiki.kerbalspaceprogram.com/wiki/Drag (not fetched). The forums report that 5600 m is only a rough approximation of the 1.0 curve. [S]
- Isothermal reference: H = RT / (Mg); the Wikipedia article gives about 8.4 km for Earth's troposphere. [F] https://en.wikipedia.org/wiki/Barometric_formula
- [D] Isothermal scale height from Kerbin sea-level values: p0 / (rho0 * g) = 101,325 / (1.225 * 9.80665) = 8,435 m. This is a sanity number only. The stock curve is not isothermal.
- Gas constant: a forum post says KSP uses one gas constant (R = 213.75 J/kg-K) for pressure and scale-height work and a different value (287 J/kg-K) for density, drag, and speed of sound. [S] Poster's claim; UNVERIFIED.
- kOS exposes sea-level pressure in atm and an ALTITUDEPRESSURE(altitude) function returning atm; it returns zero above the maximum atmosphere altitude. [F] https://ksp-kos.readthedocs.io/en/latest/structures/celestial_bodies/atmosphere.html

### 3.2 Drag and lift (1.0+)

- As of 1.0, every part produces some lift as well as drag, but wings and control surfaces have a much higher lift-to-drag ratio. [S] https://wiki.kerbalspaceprogram.com/wiki/Lift
- Lift increases with airspeed and decreases with altitude, all else equal. [S] same URL
- Drag is precomputed per part from its shape and applied according to its orientation to the airflow (the community term is "drag cube"). Exposed faces count; faces hidden behind same-size neighbours do not. [S] forum and wiki 1.0 page summary: https://wiki.kerbalspaceprogram.com/wiki/1.0
- Occlusion (for drag and heating) depends on the direction of motion, and the game models it for most parts. [S] forum summary.
- Wings are configured with ModuleLiftingSurface (fields including deflectionLiftCoeff and drag at max/min angle of attack). [S] forum posts. How lift combines with the drag cube is unclear. UNVERIFIED.
- Lift-induced drag: the 1.0 page says it is "properly simulated," but a forum poster doubts the game computes aspect ratio or efficiency. Conflicting. [S]
- Terminal velocity formula: V_t = sqrt(2 m g / (rho A C_d)). Air density increases by about 1% for every 80 m of altitude lost. [F] https://en.wikipedia.org/wiki/Terminal_velocity

### 3.3 Reentry heating

These are player and forum observations, not developer documentation. [S] unless noted.

- Two kinds of heat: surface (skin) heating, which consumes ablator, and core (internal) heating, which conducts into neighbouring parts. [S] forum thread https://forum.kerbalspaceprogram.com/topic/120735-on-ablator-behaviour-during-reentry
- Ablation begins near 1,500 K, and the shield tolerates about 3,300 K. These are one commenter's numbers. [S] Treat as anecdotal.
- Excess heat beyond what the ablator can shed heats the part itself. [S] same thread family
- In the thermal debug view, radiation dominates how a hot shield cools, not ablation. One commenter says ablation removes little heat from the skin. [S]
- A shallow reentry produces lower temperatures for longer (large total heat load); a steep one gives brief, very high temperatures. [S] forum reasoning
- Parts that stick out beyond the shield diameter take shock heating directly. [S]
- KSP does not model overheating very well; the Deadly Reentry mod exists to make heating harsher. [S] https://forum.kerbalspaceprogram.com/topic/50296-112-deadly-reentry-v790-the-barbie-edition-aug-5th-2021/page/206
- Exact stock formulas and constants: UNVERIFIED.

### 3.4 Parachutes (stock part values, [S])

| Part | Semi-deploy min pressure | Semi-deploy drag | Full-deploy drag | Full-deploy altitude (AGL) |
|---|---|---|---|---|
| Mk16 (parachuteMk1.cfg) | 0.04 atm | 1 | 500 | 1,000 m |
| Mk12-R (drogue) | 0.02 atm | 2 | 100 | 2,500 m |
| Mk25 (drogue) | 0.02 atm | 4 | 170 | 2,500 m |
| Mk2-R (radial) | 0.04 atm (wiki: ~17,000 m on Kerbin) | not retrieved | not retrieved | not retrieved |

Sources: https://wiki.kerbalspaceprogram.com/wiki/Parachute ; https://wiki.kerbalspaceprogram.com/wiki/Mk16_Parachute ; https://wiki.kerbalspaceprogram.com/wiki/Mk12-R_Radial-Mount_Drogue_Chute ; https://wiki.kerbalspaceprogram.com/wiki/Drogue (Mk25).

- [D] Check on the Mk2-R altitude claim: with an isothermal scale height of 8.4 km, 0.04 atm occurs at h = H ln(25), about 27 km. The wiki's 17 km figure implies a different curve. The discrepancy is consistent with the non-isothermal stock curve. UNVERIFIED.
- Deployment logic: armed chutes wait for pressure above the minimum to semi-deploy, then fully deploy below the set altitude. [S] Parachute wiki summary.
- Released too fast, a chute is destroyed when the air becomes dense enough to partly open it. [S] same
- Anecdotal: raising min pressure to 0.5 atm gives more survival margin for early-armed chutes; a poster saw it open near 5 km. [S] forum.

---

## 4. Navigation and control

### 4.1 Maneuver nodes and orbital elements

- KSP 1 maneuver node in map view: six handles (prograde, retrograde, normal, anti-normal, radial, anti-radial) set direction and size. Right-click toggles a compact mode with delete, advance, and regress icons. [S] https://wiki.kerbalspaceprogram.com/wiki/Map_View and https://forum.kerbalspaceprogram.com/topic/161228-how-to-use-maneuver-nodes
- Common practice: place a node at apoapsis and burn prograde until periapsis reaches the target. [S] same
- KSP 2 nodes sit on the trajectory, show only in map view, and cannot be created while paused. [S] same
- Orbital elements (general definitions) [F] https://en.wikipedia.org/wiki/Orbital_elements:
  - Eccentricity: shape; below 1 ellipse, 1 parabola, above 1 hyperbola.
  - Semi-major axis: half of apoapsis plus periapsis distances; negative for hyperbolas.
  - Inclination: tilt of the orbital plane, measured at the ascending node.
  - Longitude of the ascending node (LAN, Omega): angle from the reference direction to the ascending node.
  - Argument of periapsis (omega): angle from the ascending node to periapsis; undefined for circular orbits.
  - Epoch, mean anomaly at epoch, time of periapsis passage, true anomaly at epoch, mean motion n, and standard gravitational parameter mu = GM.
  - Period P = 2 pi / n; n = sqrt(mu / |a|^3) in radians (formula in article).
- Which of these KSP's UI displays: the wiki summary covers apoapsis and periapsis only. The other elements in the KSP UI are UNVERIFIED.
- Map view: shows predicted paths of vessels and bodies, altitude/speed, times and altitudes of Ap and Pe, other vessels, and SOI encounters. Most ship commands are disabled there except some (for example, cutting throttle). [S] https://wiki.kerbalspaceprogram.com/wiki/Map_View
- Career: blue orbit lines need Tracking Station and Mission Control upgraded (level 2). [S] same

### 4.2 Transfers, gravity assists, and aerobraking

- Hohmann transfer [F] https://en.wikipedia.org/wiki/Hohmann_transfer_orbit:
  - First burn: delta-v1 = sqrt(mu/r1) (sqrt(2 r2 / (r1 + r2)) - 1)
  - Second burn: delta-v2 = sqrt(mu/r2) (1 - sqrt(2 r1 / (r1 + r2)))
  - Transfer time: t_H = pi sqrt((r1 + r2)^3 / (8 mu))
  - Phase angle at departure: alpha = pi (1 - (1 / (2 sqrt 2)) sqrt((r1/r2 + 1)^3))
  - Hohmann often uses the least impulse but takes longer; some bi-elliptic transfers need less total delta-v for large radius ratios.
  - Maximum delta-v between two circular orbits occurs when the outer radius is about 15.58 times the inner radius (positive root of x^3 - 15x^2 - 9x - 1 = 0). [F] same article
- Porkchop plot [F] https://en.wikipedia.org/wiki/Porkchop_plot : contours of constant characteristic energy C3 over departure date (horizontal) and arrival date (vertical); red lines mark equal time of flight. Engineers pick launch windows from it. Lambert's problem (first solved by Lambert in 1761) is the underlying solver. For Voyager, about 10,000 trajectories were plotted and about 100 selected.
- Gravity assist [F] https://en.wikipedia.org/wiki/Gravity_assist : in the planet's frame, the speed magnitude is unchanged; the heliocentric speed changes. Cassini's typical Titan encounter changed velocity by 0.75 km/s across 127 encounters (used mostly for inclination). The rocket burn near periapsis gains the most energy (Oberth effect).
- Aerobraking [F] https://en.wikipedia.org/wiki/Aerobraking : a small burn sets an elongated orbit; drag at periapsis over many passes circularises it. Final orbit may take over six months and hundreds of passes at Mars. Aerocapture uses one deep pass instead. Hiten (1991) reduced velocity by 1.712 m/s and apogee by 8,665 km in its first aerobraking.
- Rendezvous and docking (KSP 1): target lock and maneuver lock keep ports aligned; navball target mode shows relative velocity; cancel relative velocity before closest approach; turn off target SAS shortly before contact (magnets fight SAS). [S] https://forum.kerbalspaceprogram.com/topic/66479-docking-from-navball-to-first-station-and-beyond
- RCS: balance thrusters around centre of mass; RCS placement changes with moved parts; turn RCS off when unused. [S] same

### 4.3 SAS modes

- SAS holds prograde, retrograde, normal, anti-normal, radial, maneuver (node), and target directions. [S] forum walkthrough; full mode list in KSP 1 UI UNVERIFIED.

---

## 5. Kerbol system body table

Sources and method:

- Radius, mass, and SOI: [F] https://hackage-origin.haskell.org/package/KSP-0.1/docs/src/System-KSP-Universe.html (third-party KSP library). Radius in m, mass in kg, SOI in m. The library's orbit fields are altitudes above the parent surface; centre-based semi-major axis = altitude + parent radius. The derived semi-major axes reproduce the [S] wiki values for Kerbin, Dres, and Eeloo.
- GM = G x mass, with G = 6.67384e-11 (the constant that reproduces stock Kerbin GM = 3.5316e12 from the mass). [D]
- Surface gravity = GM / R^2. [D] Kerbin gives 9.810 m/s^2 (matches stock 9.8).
- Period = 2 pi sqrt(a^3 / GM_parent). [D] Kerbin days are 6 h = 21,600 s (consistent with the wiki's "426 d"). [S]
- Atmosphere tops are [S] from the KSP wiki summaries unless marked.
- Six moons (Gilly, Ike, Vall, Tylo, Bop, Pol) were not extracted from the source. UNVERIFIED for this table.

| Body | Parent | Radius (km) | Mass (kg) | GM (m^3/s^2) | Surface g (m/s^2) | SOI radius (km) | Atmosphere top (km) | Semi-major axis (km, centre-based) | Orbital period |
|---|---|---|---|---|---|---|---|---|---|
| Kerbol | (none) | 261,600 | 1.7565670e28 | 1.17230e18 | 17.13 | infinite | 600 [S] | (none) | (none) |
| Moho | Kerbol | 250 | 2.5263617e21 | 1.6861e11 | 2.698 | 9,646.663 | none [S] | 5,263,138.304 | 2,215,781 s (615.49 h; 102.58 Kerbin days) [D]; wiki 615.49 h [S] |
| Eve | Kerbol | 700 | 1.2244127e23 | 8.1715e12 | 16.677 | 85,109.365 | 90 [S] | 9,832,684.544 | 5,658,063 s (1,571.68 h; 261.95 Kerbin days) [D]; wiki 1,571.7 h [S] |
| Kerbin | Kerbol | 600 | 5.2915793e22 | 3.5315e12 | 9.810 | 84,159.286 | 70 [S] | 13,599,840.256 | 9,203,655 s (426.10 days) [D]; wiki 9,203,545 s, 426 d [S] |
| Duna | Kerbol | 320 | 4.5154812e21 | 3.0136e11 | 2.943 | 47,921.949 | 50 [S] | 20,726,155.264 | 17,315,608 s (801.65 days) [D]; Time page 4,809.80 h [S] |
| Dres | Kerbol | 138 | 3.2191322e20 | 2.1484e10 | 1.128 | 32,832.840 | none [S] | 40,839,348.203 | 47,893,637 s (2,217.3 days) [D]; wiki 47,893,063 s [S] |
| Jool | Kerbol | 6,000 | 4.2332635e24 | 2.8252e14 | 7.848 | 2,455,985.2 | 200 [S] | 68,773,560.320 | 104,662,686 s (4,845.5 days) [D] |
| Eeloo | Kerbol | 210 | 1.1149358e21 | 7.4409e10 | 1.687 | 119,082.94 | none [S] | 90,118,820.000 | 156,993,929 s (7,268.2 days) [D] |
| Mun | Kerbin | 200 | 9.7600236e20 | 6.5137e10 | 1.628 | 2,429.559 | none (UNVERIFIED) | 12,000 | 138,986 s (6.43 days) [D] |
| Minmus | Kerbin | 60 | 2.6457897e19 | 1.7658e9 | 0.490 | 2,247.428 | none (UNVERIFIED) | 47,000 | 1,077,323 s (49.88 days) [D] |
| Laythe | Jool | 500 | 2.9397663e22 | 1.9620e12 | 7.848 | 3,723.646 | UNVERIFIED (one informal forum: about half Kerbin's) | 27,184 | 52,982 s (2.45 days) [D] |

Notes:

- The Kerbin SOI in this table matches [S] wiki value 84,159 km.
- Eeloo's apoapsis 113,549,713.2 km and periapsis 66,687,926.8 km (centre-based) give a = 90,118,820 km, matching the [S] wiki value.
- Dres's apoapsis and periapsis (46,761,053.522 km and 34,917,642.884 km, centre-based) average to 40,839,348.203 km, matching [S] wiki.
- Eve's and Moho's apoapsis and periapsis from the source also average to the stated semi-major axes; those averages were used.
- Surface gravities match [S] wiki values where quoted (Kerbol about 17.1 m/s^2; Eeloo 1.69 m/s^2).
- Haskell library body table for Kerbol is radius 261.6e6 m and mass 1.7565670e28 kg. [F] Wiki quotes Kerbol radius 261,600 km. [S]
- Real Solar System (RSS) scale factor vs stock: UNVERIFIED. Forum evidence (not a config value): a quarter-size RSS is 2.5x stock, implying full RSS is about 10x (poster inference); an alternative RSS config scales Kerbin to a 1:6.4 ratio of Earth. [S] https://forum.kerbalspaceprogram.com/topic/67937-realkerbin-a-real-solar-system-fork/page/2 and https://spacedock.info/mod/1355/Quarter%20Sized%20Real%20Solar%20System%20(1/4%20RSS,%202.5x%20Stock)
- [D] Consistent scaling rule (forum statement, checked by arithmetic): with distances x Z, masses x Z^2 keep surface gravity constant, and times and velocities scale by sqrt(Z). Arithmetic check: GM ~ Z^2, R ~ Z, so g = GM / R^2 is unchanged; T ~ sqrt(a^3 / GM) ~ sqrt(Z). [S] for the rule; [D] for the check.

---

## 6. Game systems

### 6.1 CommNet

- CommNet is a difficulty option that caps antenna reach for control and science transmission. It debuted in the KSP 1.2 pre-release. [S] https://wiki.kerbalspaceprogram.com/wiki/Commnet
- Unpiloted probes need a control link: a Probe Control Point on another vessel or the Deep Space Network (DSN). Links can hop through relays. [S] same
- Max link distance: a forum post gives sqrt(P1 x P2), where P is antenna power. Two identical antennas reach their own power rating (for example, two RA-15s reach about 15 Gm). [S] https://forum.kerbalspaceprogram.com/topic/171167-commnet-range
- DSN effective ratings: 2 G, 50 G, or 250 G, depending on Tracking Station level. [S] same
- Signal strength falls with distance from 100% at zero. Wiki example: Level 3 DSN to a probe near Jool at 73.4 Gm gives 71%. [S] The formula (3 - 2x) x^2 with x = 1 - d/max is a reconstruction from that example; UNVERIFIED.
- Relayed path strength = product of the hop strengths. Each hop must itself be in range. [S] same wiki
- Forum: the game favours a direct stronger link to KSC over a relay. [S]

### 6.2 Science

- Science points unlock tech-tree parts and are earned by running experiments in situations and biomes. Data earns nothing until it returns, by radio or by recovering the vessel or Kerbal. [S] https://wiki.kerbalspaceprogram.com/wiki/Science_points
- Biomes: a separate map per body. The Launch Pad counts as a biome. The Kerbal Space Center area alone is made up of 33 biomes. The wiki says only Kerbin, Mun, and Minmus are fully detailed; that line looks dated. [S] same
- Situations: six, including landed, splashed, low/high flying in atmosphere, and space situations. [S] same
- Transmission: surface samples transmit at 25% efficiency (base 30, max 40). Crew reports transmit at full value. Re-sending an experiment can gain more total value. [S] same; forum statement on re-sending. UNVERIFIED for the forum claim.
- Storage: one copy of each experiment per situation per part. [S] same
- Lab: data is processed, then researched into points, then transmitted; needs a scientist. [S] forum; may be an older mechanic.

### 6.3 Career mode

- Three resources: funds, science, reputation. [S] https://wiki.kerbalspaceprogram.com:443/wiki/Career
- Contracts pay funds, reputation, and science. Prestige tiers are Trivial, Significant, and Exceptional. Failed contracts include deadline overrun or critical-objective failure. Overlapping contracts stack on one launch. [S] same
- Administration Facility: business strategies shift funds, science, or reputation income. [S] same
- Funds appeared with version 0.24; the earlier science-only career became Science mode. [S] same
- Tech tree: only the initial node is open at the start; parts are researched at the R&D facility. [S] same
- Astronaut Complex caps hires at 5 (basic), 12 (level 2), then unlimited. Hire cost formula 150n^2 + 12350n with n counted from 5. [S] https://wiki.kerbalspaceprogram.com/wiki/Astronaut_Complex
- Rescued Kerbals join even above the cap. [S] same

### 6.4 Kerbal traits, experience, and EVA

- Specialisations: Pilot, Engineer, Scientist. Levels 0 to 5, with total XP thresholds of 2, 8, 16, 32, and 64 for levels 1 to 5. [S] https://wiki.kerbalspaceprogram.com/wiki/Skill and https://wiki.kerbalspaceprogram.com/wiki/XP
- XP comes from achievements (orbiting, flag planting, escaping a body's gravity), each once per Kerbal per body. [S] same
- Experience system is on by default in Career and off in Sandbox and Science. [S] same
- Since 1.11, all engineers can use EVA construction (place stored parts or move attached parts while on EVA). [S] same
- Courage and Stupidity meters and the "badass" flag are cosmetic. Experience and level affect gameplay. [S] Astronaut Complex and Skill summaries
- Personal parachute unlocked by level (pilots at level 0; others at level 1). [S] same

### 6.5 Breaking Ground (surface features and robotics)

- Second expansion; arrived May 30, 2019 with version 1.7.1; requires the base game. [S] https://wiki.kerbalspaceprogram.com/wiki/Breaking_Ground
- Surface features (meteors, craters, rock outcroppings, cryovolcanoes) can be scanned with a robotic arm on a rover. [S] same
- Deployable science: weather station, seismometer, RTG, ion detector. Each instrument needs time on site. The active seismometer needs deliberate impacts. [S] same
- Robotics: hinges, pistons, rotors, and rotational servos in several sizes, with a robotics controller. [S] same
- Steam player summary (aggregator, June 2026): mixed reception; robotics described as unstable. [S] https://vaporlens.app/app/982970/kerbal_space_program_breaking_ground_expansion

### 6.6 Notable mods (1-3 lines each, [S] unless noted)

- kOS: kerboscript-based scripting for flight control. [S] https://ksp-kos.readthedocs.io/en/latest/
- MechJeb: autopilot, maneuver planner, and data display; advanced automation unlocked with R&D science. [S] https://forum.kerbalspaceprogram.com/topic/113542-mechjeb-and-atomic-engines
- Kerbal Inventory System (KIS): container storage with volume and mass limits; stock Breaking Ground inventory is separate. [S] https://forum.kerbalspaceprogram.com/topic/101928-112-kerbal-inventory-system-kis-1212/page/3
- Kerbal Attachment System (KAS): not retrieved. UNVERIFIED.
- Kerbal Konstructs: extends KSC with buildings, roads, and fuel tanks; required by Galileo's Planet Pack. [S] https://github.com/hohmannson/Galileos-Planet-Pack
- Realism Overhaul (RO): broad realism overhaul. [S] wiki list
- RP-1: paired with RO for realistic career/parts. Description not retrieved. UNVERIFIED.
- Scatterer: lighting and atmosphere effects (eclipses, god rays). [S] community list
- EVE (Environmental Visual Enhancements): clouds, city lights, auroras; Galileo's Planet Pack needs its plugin. [S] same
- Parallax: not retrieved. UNVERIFIED.
- Principia (N-body), Kopernicus (body config system): see sections 1.7 and 5. [F] https://github.com/Kopernicus/Kopernicus/wiki/Properties (Kopernicus config fields: radius, mass, gravParameter, sphereOfInfluence; "gravParameter = G*mass ... the parameter that is used in the actual simulation.")

---

## 7. Multiplayer time: precedents and mechanics

### 7.1 Luna Multiplayer (LMP): per-player subspaces (best-documented)

Source: LMP wiki pages read as raw markdown: https://raw.githubusercontent.com/wiki/LunaMultiplayer/LunaMultiplayer/Timewarp.md (human URL: https://github.com/LunaMultiplayer/LunaMultiplayer/wiki/Timewarp). All items below are [F].

- Client time is synced to an NTP server time through a TimeSyncerSystem.
- When a player warps, the player "create[s their] own subspace." The client's time becomes server time plus an offset tied to the subspace ID.
- The client sends its new time; the server stores the difference as a dictionary entry {subspace ID, player time - server time}, then sends that subspace data back to the client.
- Example from the page: a player who connects at 06:30:00 and warps at 2x for 5 s ends at 06:30:10; the server records subspace 1 with a 10 s offset.
- Other players' vessels do not move with a warped player's clock. For stable orbits and escape trajectories, a warped player sees the other vessel extrapolated to their time. For suborbital or atmospheric flight, extrapolation is not possible, so the vessel stays at its last position.
- The page does not say whether a player must "catch up" before interacting. The forum (search summary) says catching up is done with a button and is essentially automatic warp. [S] https://forum.kerbalspaceprogram.com/topic/159396-possible-solution-to-the-multiplayer-issue

Lock system (LMP) [F] https://raw.githubusercontent.com/wiki/LunaMultiplayer/LunaMultiplayer/Lock-system.md (human URL: https://github.com/LunaMultiplayer/LunaMultiplayer/wiki/Lock-system):

- Lock types: Update (holder sends vessel position and definition), Unloaded Update (holder sends position), Control (vessels a player controls), Spectator, and Asteroid.
- Update lock: acquired when a player is within the load distance of an uncontrolled loaded vessel, "20km or less by default." Leaving the area unloads the vessel and releases the lock.
- Unloaded Update lock: can be taken at any time if no update lock exists and no other player holds it. Holding an update lock grants it automatically.
- Control lock: owning it while controlling a vessel also grants update and unloaded-update locks.
- Exclusivity is implied (one holder), not stated explicitly. Timeouts and conflict resolution are not described.

LMP Home page: LMP was rebuilt from DMP with a different architecture but keeps the subspace idea. [F] https://github.com/LunaMultiplayer/LunaMultiplayer/wiki/Home (summary via fetch). The Interpolation and Parts-synchronisation wiki pages failed to load and are not summarised here.

### 7.2 DarkMultiPlayer (DMP)

- README: "It supports subspace style (and master controlled warp) warping and career mode, with an easy to edit server database." [F] https://github.com/godarklight/DarkMultiPlayer
- The DMP wiki home page returned only a welcome heading. Subspace sync rules, vessel ownership, and limits are UNVERIFIED beyond the README sentence.

### 7.3 afrigon/xsa issue #184: time warp in multiplayer (three options named)

Source: https://github.com/afrigon/xsa/issues/184 [F]. The game is not named on the issue page. The issue concerns vessel physics and cites KSP multiplayer mods.

- Context: #183 forbids warp to keep one shared timeline; #184 asks how to allow rate changes without breaking that. Status: open, under M1 Multiplayer milestone; no maintainer decision recorded (the page shows a milestone addition dated Oct 10, 2026).
- Option 1, consensus warp: the rate changes only when every player agrees. It is "limited by the most restrictive situation among all players' vessels."
- Option 2, subspaces: each player warps independently and syncs to a later timeline to interact. The issue says KSP multiplayer mods use this approach.
- Option 3, operator-controlled rate: an operator sets the rate and everyone follows it. Tied to permissions (#167).
- Open questions the issue requires every approach to answer: what happens to a vessel under thrust or in atmosphere when another player warps; how events from different times are ordered; how saves record more than one timeline.

### 7.4 Kitten Space Agency (KSA): time warp (fully sourced)

Source: https://kittenspaceagency.wiki.gg/wiki/Time_warp [F] (fan wiki; not official documentation).

- Warp range: 0.1x ("slow motion") to 3,153,600,000x ("100 years per second"). Other speeds via the simspeed command. Changed with comma and period keys.
- Thrust gate: vehicles, engines, and RCS can be fully manually controlled up to 30x. Above 30x, engines cannot be started. Engines already firing at 30x or below keep firing and can only be throttled down. RCS is not manually controllable above 30x but works through autopilot.
- Proximity to bodies does not limit warp.
- Vehicle rotation is preserved during speed changes.
- Multiplayer behaviour of warp: not described on the page.

KSA multiplayer status: Wikipedia says multiplayer functionality is "expected." [F] https://en.wikipedia.org/wiki/Kitten_Space_Agency

### 7.5 Juno: New Origins (formerly SimpleRockets 2)

- No official multiplayer or shared time warp was found. [S] https://steamcommunity.com/app/870200/discussions/0/1696048426817031344/ ; feedback board requests for multiplayer: https://www.simplerockets.com/Feedback/View/xnEIYj/Multiplayer
- A developer reply in the feedback thread appears to decline the request, but the excerpt is cut off. Unconfirmed.
- Community view: one player doing precise docking would be ruined by a partner at 500x warp. Options proposed by players: disable warp, require time sync, or force one warp rate. [S] same
- The App Store listing says orbits are "realistically simulated" and support time warp. [S] https://apps.apple.com/us/app/juno-new-origins/id6475594930
- SimplePlanes developers (2015 dev note, mirrored on Reddit) shelved multiplayer after a month and said it was not designed with multiplayer in mind. [S] https://redlib.ssps.io/r/SimplePlanes/comments/301g68/dev_notes_march_3_2015/cpqebz2/?context=3

### 7.6 Junkyard Space Agency (JSA): persistent offline world

- Co-op on public or self-hosted servers, or local. [F] https://www.gamingonlinux.com/2025/05/junkyard-space-agency-is-a-scrappier-multiplayer-version-of-kerbal-space-program/
- "Players come and go, but the world lives on and keeps evolving." Ships and crews keep flying their trajectories; bases operate autonomously until supplies run out. [F] same
- The article does not say whether this continues when a server is fully shut down, or how off-screen time is computed. UNVERIFIED. The article says nothing about shared or per-player time warp. [F]
- Release: GamingOnLinux (May 2025) says "sometime in 2026." A later Steam listing (Malay-language) lists 2027. Conflicting. UNVERIFIED current date.
- Developer's stated plan (search summary): codebase to be open-sourced under a permissive licence at first public launch. [S] https://nulltermio.itch.io/junkyard-space-agency

### 7.7 Minecraft-relevant precedents (vanilla protocol and Paper API)

- Paper API, Player methods [F] https://jd.papermc.io/paper/org/bukkit/entity/Player.html (paper-api 26.3.build.157-beta):
  - getPlayerTime(): the player's current timestamp.
  - getPlayerTimeOffset(): time offset "relative to server time," or the fixed time if the player's time is absolute.
  - resetPlayerTime(): restores "the normal condition where the player's time is synchronized with the server time."
  - isPlayerTimeRelative(): true if the player's time follows server time; otherwise it is absolute and does not change unless setPlayerTime() changes it.
  - The setPlayerTime(long, boolean) text was not visible in the fetch. Whether the change is client-only is UNVERIFIED from the page itself; the wording implies per-player.
- Time packet: the Minestom javadoc models Update Time as world age and time of day (64-bit each); a newer version adds a tickDayTime flag. [S] https://javadoc.minestom.net/net/minestom/server/network/packet/server/play/TimeUpdatePacket.html
- Older protocol note: 24,000 ticks per day (sunrise 0, noon 6,000, sunset 12,000, midnight 18,000); negative time-of-day freezes the sun. [S] search summary from protocol notes and Paper patch comments; wiki.vg not retrieved. UNVERIFIED for exact field layout.
- Per-player time implementation in a Spigot patch: the server sends each player a packet built from world age, that player's time, and the doDaylightCycle flag, every 20 ticks; the packet is shared unless a player has per-player time. [S] https://ayakael.net/mirrors/papermc/commit/70e091b7d4ffdf79304d64d396733a926306193a and related commits (search summary; not fetched).
- Paper comment: world age is not changed by server commands and must not be negative. [S] same mirror.
- FoundryVTT GameTime (tabletop tool): clients stay in step with the server using a simplified Cristian's algorithm. Generic precedent, not a game. [S] https://foundryvtt.com/api/classes/client.GameTime.html

### 7.8 KSP stock (single-player)

- Stock KSP has no shared-time multiplayer. Krakensbane means world coordinates cannot be compared across moments (section 1.5). [S]

---

## 8. Successors and 2025-2026 KSP-like games

### 8.1 Kerbal Space Program 2

- Developers: Intercept Games and Squad; Star Theory Games initially. Publisher: Private Division. Windows only. [F] https://en.wikipedia.org/wiki/Kerbal_Space_Program_2
- Early access: February 24, 2023. [F] same
- "For Science!" update (December 2023) added Exploration Mode, replacing Science mode. [F] same
- Planned for later or final release: alternative propulsion, habitation modules, orbital and planetary colonies, multiplayer, and interstellar travel. [F] same
- Delivered features: no source found shows colonies, interstellar travel, or multiplayer were delivered. The Wikipedia article states the page "does not state whether any promised features were delivered or cancelled, aside from the Exploration Mode update." The task premise that colonies and interstellar were delivered is NOT supported by the sources found.
- Status: Intercept Games closed in 2024; former developers said layoffs were on June 28, 2024; Take-Two sold Private Division to an unspecified investor in November 2024; last update June 2024; development status unknown. [F] same
- Conflicting signal: an aggregator page shows "Last update 16 May 2026," and a Steam post titled "Some Improvements on the Way" lists v0.2.2.0 with no year. UNVERIFIED. Treat 2026 status as unknown. [S] https://changelog.gg/games/kerbal-space-program-2-954850/updates and https://steamcommunity.com/app/954850/announcements/

### 8.2 Kitten Space Agency (KSA)

- Developer: RocketWerkz (founded by Dean Hall in Dunedin, 2014). Built on RocketWerkz's proprietary "BRUTAL framework." No engine is named. [F] https://en.wikipedia.org/wiki/Kitten_Space_Agency ; [S] RocketWerkz background
- Public pre-alpha: 14 November 2025. [F] same
- Status labels conflict: Wikipedia says "in early access"; the fan wiki says pre-alpha; Hall's Wikipedia entry says "TBA Early Access." Treat as pre-alpha with no confirmed early-access date. [F]/[S] conflict
- Funding: optional supporter contributions; over $150,000 by March 2026. No sales. [F] same
- Simulation approach: pre-alpha demo (version 2025.8.17.2088) described as having "foundational orbital mechanics" and "the full scale solar system." n-body and patched conic details not found. [F] https://kittenspaceagency.wiki.gg/wiki/Version_history
- Latest version listed by fan wiki: 2026.10.7.5541 (page last edited July 6, 2026, so the list may be stale). [F] same
- 2026 devlogs (TikTok listing and teaser, [S]): March 2026 update covered physics, docking, and planetary rings; June 2026 covered the vehicle editor, volumetric trails, and Linux support.
- Multiplayer: "expected" per Wikipedia; not described in detail. [F]
- Time warp: section 7.4. [F]

### 8.3 Other KSP-like games (search results only)

- Junkyard Space Agency: see section 7.6. Not released as of the sources; release date conflicting. [F]/[S]
- Juno: New Origins (formerly SimpleRockets 2): early access on Steam in 2018; no multiplayer found. [S]
- Flight of Nova (2022): Newtonian and real orbital dynamics. [S]
- Mars First Logistics: physics puzzler with vehicle building; compared to KSP. [S] https://en.wikipedia.org/wiki/Mars_First_Logistics
- Spaceflight Simulator: early access (February 2022 listing); orbital features not described. [S]
- No confirmed 2026 orbital-mechanics game with multiplayer was found. Solar Expanse (April 2026 listing) is a management game. [S]

---

## 9. Math appendix

Each entry: formula, one-line explanation, source tag and link.

9.1 Gravitational parameter: mu = G M. Standard gravitational parameter; often used instead of mass. [F] https://en.wikipedia.org/wiki/Orbital_elements. Mass = mu / G [F] same.

9.2 Vis-viva: v^2 = mu (2/r - 1/a). Relative speed v at distance r on an orbit with semi-major axis a. [F] https://en.wikipedia.org/wiki/Vis-viva_equation (formula with r = distance between centres of mass). Circular case: a = r gives v^2 = mu/r [D from 9.2].

9.3 Specific orbital energy: epsilon = v^2/2 - mu/r = -mu/(2a) (elliptic). Constant along the orbit. [F] same URL

9.4 Orbital period: T = 2 pi / n = 2 pi sqrt(a^3 / mu), with n = sqrt(mu / |a|^3) in rad/s. Kepler's third law. [F] https://en.wikipedia.org/wiki/Orbital_elements (mean motion); T formula [D] from n.

9.5 Mean anomaly: M = n (t - t0). Linear in time. [F] https://en.wikipedia.org/wiki/Kepler%27s_equation

9.6 Kepler's equation: M = E - e sin E. Relates mean and eccentric anomaly. [F] same URL

9.7 Newton iteration for E: E_{n+1} = E_n - (E_n - e sin E_n - M) / (1 - e cos E_n). Start E0 = M (e below 0.8), E0 = pi (e above 0.8). Use bisection near e = 1. [F] same URL

9.8 Eccentric to true anomaly and radius: tan(nu / 2) = sqrt((1 + e)/(1 - e)) tan(E / 2); r = a (1 - e cos E). UNVERIFIED: standard relations, not fetched this session (Braeunig failed; Wikipedia True anomaly not fetched). Check against a textbook before use.

9.9 Semi-latus rectum and orbit equation: p = a (1 - e^2); r = p / (1 + e cos nu). [F] for the hyperbolic-article form ell = a(1 - e^2) and r = ell / (1 + e cos theta): https://en.wikipedia.org/wiki/Hyperbolic_trajectory

9.10 Hyperbolic orbit: a < 0; v_inf = sqrt(-mu / a); epsilon = v_inf^2 / 2; e = sqrt(1 + b^2 / a^2) > 1; turn angle delta = 2 arccos(-1/e) - pi. [F] https://en.wikipedia.org/wiki/Hyperbolic_trajectory. Asymptote half-angle theta_inf = arccos(-1/e) [F] same.

9.11 State vectors to elements: angular momentum h = r x v [F] https://en.wikipedia.org/wiki/Orbital_state_vectors. Eccentricity vector, node vector, inclination, LAN, argument of periapsis, true anomaly, and semi-major axis formulas: UNVERIFIED (standard, not fetched; Curtis, "Orbital Mechanics for Engineering Students," is the cited reference on the Wikipedia page). Standard forms, to be checked: e_vec = ((v^2 - mu/r) r - (r.v) v) / mu; i = arccos(h_z / h); a = 1 / (2/r - v^2/mu); ascending-node vector n = k x h.

9.12 Elements to state vectors: UNVERIFIED (standard perifocal-frame method; not fetched).

9.13 SOI transition detection: UNVERIFIED for KSP. The Wikipedia page defines the boundary as "where the trajectory switches which mass field it is influenced by" and gives r_SOI. A common implementation tests, each step, whether the vessel is inside a child body's SOI sphere (|r - r_body| < r_SOI) and re-bases to that body's frame. KSP's exact test and hysteresis are UNVERIFIED.

9.14 SOI radius: r_SOI = a (m/M)^(2/5). [F] https://en.wikipedia.org/wiki/Sphere_of_influence_(astrodynamics); verified against stock values (section 1.1) [D].

9.15 Tsiolkovsky: delta-v = v_e ln(m0/mf) = Isp g0 ln(m0/mf), g0 = 9.80665 m/s^2. [F] https://en.wikipedia.org/wiki/Tsiolkovsky_rocket_equation

9.16 Specific impulse: Isp = F / (mdot g0) = v_e / g0. [F] https://en.wikipedia.org/wiki/Specific_impulse

9.17 Hohmann: see section 4.2. [F] https://en.wikipedia.org/wiki/Hohmann_transfer_orbit

9.18 Terminal velocity: V_t = sqrt(2 m g / (rho A C_d)). [F] https://en.wikipedia.org/wiki/Terminal_velocity

9.19 Barometric (isothermal): p = p0 exp(-h / H), H = R T / (M g). [F] https://en.wikipedia.org/wiki/Barometric_formula. Layered troposphere form [F] same.

9.20 Surface gravity: g = mu / R^2 [D]. Kerbin check: 3.5315e12 / (6e5)^2 = 9.810 m/s^2.

9.21 Gravity assist: speed relative to the planet is conserved; the heliocentric speed changes. [F] https://en.wikipedia.org/wiki/Gravity_assist

9.22 Porkchop: contours of constant C3 across departure and arrival dates; Lambert's problem solves each cell. [F] https://en.wikipedia.org/wiki/Porkchop_plot

---

## Appendix (not a numbered section): gaps and UNVERIFIED list

- KSP wiki direct fetch blocked (Anubis). All wiki facts are [S].
- Per-body warp altitude thresholds: UNVERIFIED.
- Rails rate-cap mode (MaxRailsRateMode) exact behaviour: [S] only.
- KSP's Kepler solver implementation: UNVERIFIED.
- Double-precision split between orbits and physics: UNVERIFIED.
- Principia integrator and performance cost: UNVERIFIED.
- Engine pressure interpolation (ASL to vacuum): UNVERIFIED.
- Resource type list, staging, crossfeed: UNVERIFIED.
- Full pressure curve and drag/lift constants: UNVERIFIED (Kopernicus kittopia-dumps Kerbin.cfg not fetched).
- Swivel, Cheetah, Thud (KSP 1 numbers from one wiki summary only), Dawn (partial), Nerv (partial): partial or UNVERIFIED.
- Real Solar System scale factor: UNVERIFIED.
- Six moons (Gilly, Ike, Vall, Tylo, Bop, Pol) in the Kerbol table: UNVERIFIED.
- Atmosphere heights for Mun, Minmus, Laythe: UNVERIFIED.
- Orbital elements shown in KSP UI (beyond Ap/Pe): UNVERIFIED.
- State-vector and eccentric-to-true anomaly formulas: UNVERIFIED (standard forms, not fetched).
- DMP subspace rules and ownership: UNVERIFIED beyond README.
- LMP interpolation and parts-synchronisation pages: not loaded.
- Juno: New Origins and SimpleRockets 2 multiplayer: none found; not ruled out.
- KSA release status, funding model, and simulation details beyond the fan-wiki pre-alpha note: UNVERIFIED.
- JSA offline-world mechanics and release date: conflicting; UNVERIFIED.
- Minecraft: setPlayerTime wording and wiki.vg packet layout: not confirmed (Paper javadoc partial; wiki.vg not fetched).
- Consensus-warp and subspace trade-offs in xsa #184: options named, no resolution.
