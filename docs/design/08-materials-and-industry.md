# 08 — Materials and industry

> 47noodles' rule: materials tie into Mekanism and Create rather than Space Engineers' ores. Then the open-source rule: Create's art is ARR, so its role goes to **Apoapsis Kinetics**, an original module (`09`). Mekanism (MIT; R10 found no separate art licence stated) is used directly. Mekanism facts below come from R10 §1. Original design by Opus.

## 8.1 The material stack

| Layer | Provider | Role |
|---|---|---|
| Vanilla | Minecraft | Iron, copper, gold, coal, redstone, diamond, lapis, quartz, obsidian |
| Mechanical era | **Apoapsis Kinetics** (original) | Zinc, bronze, brass, gear trains, Thermionic Valves, rotational power (Ages 1–2) |
| Industrial era | **Mekanism** (+ Generators, Tools) | Osmium, tin, lead, uranium, fluorite. Steel, alloys, control circuits, chemicals, ore multiplication, reactors (Ages 3–6) |
| Space layer | **Apoapsis** | Components (`05` §5.6), a handful of planet chemicals (§8.5), three rare exotics (§8.6) |

**No new ore set.** Planets change the *abundance* of the ores above (§8.4). New substances are limited to **five chemicals and one fluid**, plus three special items.

## 8.2 Apoapsis Kinetics (original, built for grids)

Rotational power is a generic mechanic, but this version is built to feel distinct from Create and to work on moving ships.

- **Torque *and* speed.** Each network carries torque (N·m) and RPM separately. **Gearboxes trade one for the other.** Each shaft material has a **torque limit**: wood 200, bronze 800, steel 3,000, reinforced 12,000. Overload **shears that shaft** (it pops off with a clunk) instead of halting the whole network. Players learn load paths and redundancy, closer to engineering than Create's global overstress halt.
- **It works on grids.** Kinetic networks run on APC grids, and **reaction torque is real.** A big motor or flywheel on a small ship twists the hull unless it's counter-rotated or anchored. Flywheels store energy and act as reaction wheels, which links straight into `05`'s gyros.
- **Sources:** hand crank, water wheel, windmill sails (output scales with air density, so useless on Pale and superb in Thalassa's storm belt), **steam engine** (coal, or Basalt Rift geothermal), torsion spring (storage), and an **electric motor/dynamo** pair bridging to Mekanism Joules.
- **Machines** (original designs, with plain engineering names such as water wheel, gearbox and flywheel rather than Create's own item names): millstone, **jaw crusher** (ore → crushed ore, a 1.5× bonus that bridges to Mekanism's 2×), **sheet press**, **alloy mixer** (heated: bronze, brass), saw, **forge hammer**, belt line, chute, **actuator arm** (moves items between inventories), **tool actuator** (uses an item in the world, the deployer role), clutch, governor (holds a target RPM), gearbox, flywheel, **draft kiln** (bulk heat processing by blown hot air).
- **Signature intermediates:** **Gear Train** (brass gears), **Thermionic Valve** (glass, copper and a heated filament, the first "electronics"), **Bronze Housing** and **Brass Housing**.
- **Optional Create compat:** if Create is installed, shared `c:` tags unify zinc, brass, sheets and crushed ores (`c:ingots/zinc`, `c:plates/brass`, …). The two systems coexist rather than merge.

## 8.3 Mekanism chains Apoapsis leans on (R10 §1)

| Chain | Recipe (Mekanism, 1.21.1) | Used by Apoapsis for |
|---|---|---|
| **Steel** | Iron + carbon (infuser) → enriched iron, + carbon → steel dust, smelt | Hull plates, girders, pipes, thrust chambers |
| **Infused alloy** | Copper + redstone (infuser) | Thrust chambers, tier-2 tools |
| **Reinforced alloy** | Infused alloy + diamond dust | Reactor housing, tier-3 tools |
| **Atomic alloy** | Reinforced alloy + refined obsidian dust | Superconductors, tier-4 tools |
| **Control circuits** | Basic = osmium + redstone. Advanced = infused + basic. Elite = reinforced + advanced. Ultimate = atomic + elite | The "computer" component at four tiers (`05` §5.6) |
| **Ore processing** | 2× enrichment · 3× purification (O₂) · 4× chemical injection (HCl) · 5× dissolution, washing and crystallisation (sulphuric acid) | The Refinery block upgrades through these tiers. Off-world chemicals feed the high tiers (§8.4) |
| **H₂ / O₂** | Electrolytic Separator: water → hydrogen + oxygen | Hydrolox fuel, breathing air, Mekanism jetpack |
| **Plastics** | Substrate + ethylene + oxygen → HDPE (pressurised reaction chamber) | Armoured glass, ablator, canvas. Murk's tholin dunes supply ethylene in bulk |
| **Lithium** | Brine (thermal evaporation) → lithium | Battery cells; tritium via the solar neutron activator |
| **Polonium → antimatter** | The SPS turns polonium (as a chemical) into antimatter at 2 mB/t from 2,000 mB polonium at 800 MFE/t. The Chemical Crystallizer makes antimatter pellets | Jump drives |
| **Energy** | Joules (1 FE = 2.5 J) | Grid power is Mekanism Joules (`05` §5.2) |

## 8.4 Resource abundance by world

●●● rich · ●● common · ● scarce · — absent. Rows marked † are planet chemicals (§8.5).

| Resource | Verdance | Pale | Cinder | Ochre | Thalassa | Shoals | Tempest | Ember | Glacis | Murk | Hollow |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Iron | ●● | ●● | ●● | ●●● | ● | ●●● (S/M) | — | ● | ● | ● | ● |
| Copper | ●● | ● | ●● | ●● | ● | ●● | — | ●● | — | — | — |
| Zinc | ●● | ● | ● | ●● | ● | ●● (S) | — | ● | — | — | — |
| Tin | ●● | ● | ●●● | ●● | ● | ●● | — | ● | — | — | — |
| Lead | ●● | ● | ●●● (molten seas) | ●● | ● | ●● (M) | — | ●● | — | — | — |
| Osmium | ● | ●●● | ●● | ●● | ● | ●●● (M) | — | ● | — | — | ●● |
| Gold | ● | ● | ●● | ● | ● | ●● (M) | — | ● | — | — | ● |
| Uranium | ● (Meteor Steppe) | — | ●●● | ●● | — | ●● (U) | — | ●● | — | — | — |
| Fluorite | ●● | ● | ●● | ● | ● | ●●● (U) | — | ● | — | — | — |
| Diamond / redstone / lapis | ● | — | ●● | ● | ● | ● | — | ●● | — | — | ●● |
| Coal / carbon | ●● | — | — | — | — | ●● (C) | — | — | — | ●●● (tholins) | — |
| Water / ice | ●●● | ● (cold traps) | ● (nightside) | ●● (poles) | ●●● (heavy water at vents) | ●●● (C) | ●● (water decks) | — | ●●● | ● | ●● |
| Sulphur | ● (Basalt Rift) | — | ●● (fumaroles) | ●● | ● | — | — | ●●● | — | — | — |
| Salt / brine / lithium | ●● (Tidewild) | — | — | ●● (perchlorates) | ●●● (vents) | — | — | — | ●● (ocean salts) | — | — |
| **Helium-3** † | — | ●● (Ray Fields) | — | — | — | — | ●●● (water decks) | — | — | — | — |
| **Carbon dioxide** † | — | — | — | ●●● (atmosphere) | — | — | — | ● | — | — | — |
| **Methane** † | — | — | — | (Sabatier) | — | ● (C) | ● | — | — | ●●● (seas, air) | — |
| **Nitrogen** † | ●● (air separation) | — | — | ● | ●● | — | — | — | — | ●●● | — |
| **Ammonia** † | — | — | — | — | — | — | ●●● (ammonia decks) | — | ● | ● | — |

**Why each trip pays off:** Pale gives cheap osmium and He-3 from low gravity. Cinder gives uranium, lead and solar flux. Ochre gives CO₂, so methane can be made locally (ISRU). Thalassa gives deuterium and lithium for fusion. The Shoals give the best bulk metals. Tempest gives He-3 and H₂ at scale. Ember gives sulphur for the 5× chain. Murk gives plastics and fuel. Glacis gives water and life samples, and Hollow gives Precursor cores.

## 8.5 Planet chemicals (the only new substances)

| Chemical | Form | Source | Main uses |
|---|---|---|---|
| `apoapsis:carbon_dioxide` | gas | Ochre air (Atmospheric Intake), combustion exhaust | Sabatier → methane; algae farms |
| `apoapsis:methane` / `apoapsis:liquid_methane` | gas / fluid | Sabatier reactor, Murk seas and air, C-type asteroids | Methalox engines, combustion generators |
| `apoapsis:helium_3` | gas | Pale regolith sifting, Tempest scooping | D-³He fusion (aneutronic, no radiation, top Isp) |
| `apoapsis:nitrogen` | gas | Air separation (Verdance, Thalassa), Murk air | Cold-gas RCS, pressurisation buffer gas, cryo cooling |
| `apoapsis:ammonia` | gas | Tempest ammonia decks | **Ammonia coolant loops** (Cinder heat rejection), fertiliser |

All of them use Mekanism's chemical capability, so Mekanism tanks, pipes and machines handle them unchanged.

## 8.6 Rare items

| Item | Source | Use |
|---|---|---|
| **Cometary exotics** | The Wanderer comet's core (periodic event) | Jump-drive capacitor input |
| **Precursor fragments and cores** | Ruins (Ochre, Thalassa, Cinder), Hollow | Story decryption; jump gates; Precursor salvage blocks |
| **Xenobiology samples** | Glacis ocean fauna and vents | Late-game Survey Points; bio-tech upgrades (medbay, algae strains) |

## 8.7 Fuel chains

| Fuel | Chain |
|---|---|
| **Hydrolox** | Ice or water → (Ice Processor) → Electrolytic Separator → H₂ + O₂ → cryo tanks |
| **Methalox** | Ochre CO₂ + H₂ → Sabatier → CH₄ (+ water recycled) · *or* pump Murk's liquid methane · O₂ from water or perchlorates |
| **Solid** | Sulphur + carbon + oxidiser salts (perchlorates, cave saltpetre) → cast propellant grain (Kinetics press) |
| **Ion** | Grid power + an H₂ trickle |
| **Nuclear thermal** | Mekanism fissile fuel + H₂ working mass |
| **Fusion** | D-T (Mekanism: deuterium from heavy water, tritium from lithium) · *or* D-³He (Apoapsis option) |
| **Antimatter** | SPS (polonium) → antimatter pellets → jump drive |

## 8.8 Power progression

| Age | Base power | Ship power |
|---|---|---|
| 1–2 | Water wheels, windmills, steam (Kinetics) | — |
| 3 | Dynamos; Mekanism heat, bio, gas-burning and solar generators | Batteries, solar |
| 4 | Electrolysis economy; wind | Batteries, solar, combustion generator |
| 5 | **Fission reactor** and turbine (Mekanism) | Compact fission reactor |
| 6 | **Fusion reactor**; antimatter storage | Fusion core |

## 8.9 Feature checklist: materials

- [ ] Kinetics: torque/RPM model, shaft shear, grid reaction torque, sources and machines from §8.2
- [ ] Kinetics ↔ Mekanism Joules bridge (motor and dynamo)
- [ ] Mekanism dependency (or our 26.x port fork, `09` §9.7). Refinery tiers mapped to 2×–5×
- [ ] Components from `05` §5.6 wired to Mekanism and Kinetics recipes
- [ ] Planet ore-abundance tables (worldgen) per §8.4
- [ ] Five chemicals and one fluid registered through Mekanism's chemical API
- [ ] Atmospheric Intake, Sabatier Reactor, Regolith Sifter, Ice Processor
- [ ] Rare items and their sources
- [ ] Optional Create compat (shared `c:` tags)
