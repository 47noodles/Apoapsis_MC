# 14 — The best parts of other games

> Design by Opus, prompted by 47noodles ("getting the best parts of multiple games"; the android-link idea is his). Facts about other games come from R8 and R3. Every borrowed idea is re-expressed with original names and mechanics (clean-room rules, `09` §9.3). Each idea is tagged **Core** (planned for 1.0) or **Post-1.0**.

## 14.0 The design principle that ties it together

> **Minds travel at light speed. Matter travels by rocket.**

No Man's Sky and Elite make the galaxy convenient with teleporters and magic drives. KSP makes it real with Δv and coast times. Apoapsis gets both. Your *mind* can jump anywhere the comms network reaches, through android bodies and clones. Every *kilogram* of cargo still has to fly a real orbit. Convenience for people, honest physics for logistics.

---

## 14.1 Proxies: android bodies you link into *(Core, Age 4+)*

*47noodles' idea, built on KSP's CommNet (R3 §6) and SE's medical rooms (R1).*

You build **Proxies**, android bodies, and **link** your mind into them over the radio network. While linked, you *are* the proxy: you walk, mine, build, fight and fly as it, and other players see it as a player with your name in proxy colours (`◇47noodles`). Your real body sleeps in a **Link Pod** back home.

### Bodies, unified

Cryo-link (`03` §3.5) becomes one case of a single system, the **Link system**:

| Body | Made at | Notes |
|---|---|---|
| **Original** | You | Your organic body. Rests in a Link Pod or Cryo Pod while you're linked elsewhere |
| **Clone** | Clone Bay | Organic, used for respawn and as the cryo-link target |
| **Proxy** | Proxy Fabricator | Android, needs a comms link, comes in several frames, can be loaned to friends |

### Link rules (CommNet-style)

- **Signal path:** a link needs a radio path from the Link Pod's grid (its antennas) to the proxy's built-in antenna, through **relays**, **laser antennas** and **Ground Stations** (`06` §6.6). Planets block line of sight, so Pale's far side needs relays.
- **Link quality:**
  - *Strong:* full control.
  - *Weak:* input lag, HUD static, slower tool speed.
  - *Lost:* you snap back to your real body, and the proxy falls back to its **standing order**: hold position, return to its dock, or run a Flight Computer program (`05` §5.7).
- **Light-speed delay:** optional realism toggle. In the compact system the delay is under half a second, so it's off by default. In the `real` preset Mars-like delays become minutes, and proxies turn into program-driven rovers, like real ones.
- **Proxy destroyed:** you snap back with **link shock** (a short slowness and nausea debuff). The proxy's own inventory drops where it fell, and your real inventory stays safe at home.
- **Real body attacked:** the link breaks and you wake up. A Link Pod in an unsafe place is a risk you choose.

### Frames

| Frame | Good at | Limits | Age |
|---|---|---|---|
| **Standard** | General work, tools, building | No hazard rating | 4 |
| **Lifter** | Heavy cargo, faster grinding and welding | Slow, no jetpack | 5 |
| **Scout** | Speed, jump-jets, long sensor range | Small inventory, fragile | 5 |
| **Diver** | Thalassa and Glacis pressure, swimming | Clumsy on land | 5 |
| **Furnace** | Cinder dayside, Ember heat and radiation | Needs coolant top-ups | 5 |
| **Precursor shell** | Salvaged from Hollow; links at any range with no lag | One per player | 6 |

Frames take **modules** (battery, armour, sensor, tool mount, tank) and run on batteries that recharge at a **Proxy Dock**.

### Why it matters

1. **Multiplayer presence without warp.** A proxy waiting at your Ochre outpost lets you work there *now*, while the next cargo ship is still coasting. This sits alongside cryo-link and consensus warp (`03` §3.5).
2. **Drop-in co-op.** Loan a proxy to a friend (link permissions per player or faction). You place a **guest Link Pod** at your base (or the server runs a public one at spawn), and a new player can link through it to join you on Pale on their first evening. While in a loaned proxy they act with *your* Age and equipment, and nothing they learn or carry transfers back, so their own progression is untouched. It's a deliberate, contained exception to Age gating, and servers can switch it off.
3. **Hazard work.** Send a Furnace frame onto Cinder's dayside or a Diver into Thalassa's abyss instead of risking your body.
4. **Infrastructure has a purpose.** Relay constellations and Ground Stations stop being science trinkets. They're what lets you be somewhere.

### Balance

- Proxies have to be **physically delivered**. You ship a Proxy Fabricator (or the proxy itself) on a real rocket first. Matter still travels by rocket.
- **Coverage takes work.** Interplanetary links need relays in place along the way.
- Proxies can't use Clone Bays, can't cryo-link, and can't link into other proxies (no chaining).
- **Server option:** limit proxies per player.

### How it works (technical)

**Body swap.**
- When you link, your `ServerPlayer` moves to the proxy's position and takes on the proxy's inventory, attributes and frame.
- Your organic body stays behind as an inert **Vacant Body** entity in the Link Pod, holding your real inventory.
- Unlinking swaps everything back.
- Because you're still a real `ServerPlayer`, **every vanilla and modded interaction just works**: mining, machines, Mekanism, Kinetics.

**Cross-dimension links** are a dimension change, with the same client pre-warm as orbital transitions (`03` §3.3.2), hidden behind a static-and-reconnect "link" effect.

**State.** An idle proxy persists as an entity with its own saved inventory and state. Link permissions and frames live in player attachments.

---

## 14.2 Hub stations *(Core, Age 4+)*, inspired by No Man's Sky

NMS puts a station in every system, and its COSMOS update (Sept 2026) lets players buy, build and network stations and found alliances from them (R8 §10).

- **NPC Hubs** orbit Verdance (Age 4), Ochre and Tempest (Age 5–6). They're real dockable grids with hangars you fly into (seamless, because they're just grids).
- **What's inside:**
  - Free Trader market (`05` §5.9) and a mission board, including orbital delivery contracts.
  - A **ship broker** that buys and sells blueprints and ships. Sold ships are real grids, delivered to a pad.
  - Refuel and repair, and a **Proxy Dock** bank. NPC hubs keep a stock of proxies that were delivered there, so renting one lets you visit any hub your network reaches without breaking the delivery rule.
- **Player Hubs.** Place a **Hub Core** on your own station to make it public: it shows on everyone's map, runs player shops through trade terminals, and can be the seat of an **alliance** (a cross-faction group with shared docking rights and hub-network fees).
- **Social hub.** On servers, the Verdance hub is the natural meeting place, with a notice board, a blueprint kiosk (`11` blueprint hub) and photo spots.

## 14.3 Capital ships and fleet expeditions *(Post-1.0)*, inspired by NMS freighters and frigates

- **Capital ships** are just very large grids. The extra is a **Fleet Command** block that runs **expeditions**: send crewed frigates (proxy-crewed or colonist-crewed) on abstracted multi-hour missions such as survey, mining, salvage or trade.
- Outcomes depend on ship stats and risk: resources, discoveries, damage and logs.
- It's an idle loop that's great for multiplayer: queue expeditions before logging off.
- Expeditions travel **on rails along real transfer windows** (the `orbit` library computes them), so they respect the physics.

## 14.4 Interstellar: The Reach *(Post-1.0)*, inspired by NMS and Elite

After the finale, the **Jump Gate** at Hollow opens **The Reach**, a galaxy map of **procedurally generated star systems** seeded per server.
- Each system is built by the same design rules as Meridian (`07` §7.0): one signature mechanic per world, playable Δv, sensible window cadence.
- Planets draw from a growing library of **biome templates**.
- Every system has a hub station and faction flavour, and arriving reveals points of interest, NMS-style (R8 §10 mechanic 5).
- **Getting there:**
  - **Gate network.** Players extend it by building gate pairs (the first gate in a new system has to be carried there).
  - **Hyperdrive.** A late ship module burning antimatter and cometary exotics. Jumps are instant, hidden behind a hyperspace sequence.
- **Inside a system it's still KSP.** Hyperdrives drop you at the system's edge, and orbital mechanics take over.
- **Technical:** each visited planet is a runtime dimension (`09`; R9 §4 says runtime dimensions are possible only through libraries, so we'd rely on Infiniverse or our own). Unvisited systems exist only as metadata.

## 14.5 Discovery Registry *(Core)*, inspired by NMS's naming and Subnautica's scanning

- **Scan** flora, fauna, minerals, biomes and (in The Reach) whole planets. The first scan pays **Survey Points** (`06` §6.7) and gives **naming rights**. Your name and chosen name appear to everyone who scans it later.
- **Fragment scanning** (Subnautica-style): scanning *Arkwright* wreck fragments and Precursor debris teaches blueprints, alongside the salvage loot (`04` §4.1). Exploring becomes how you learn.

## 14.6 More of the best bits

| Idea | From | Apoapsis version | Tag |
|---|---|---|---|
| **Derelict boarding** | NMS derelict freighters, salvage meltdown risk (R8) | Procedurally assembled derelict grids in the Shoals and The Reach. Board them in zero-g with mag-boots and 6DOF (`05` §5.5.1), fight Remnant drones, salvage cores. Over-salvaging a live reactor risks a meltdown | Core (Age 5) |
| **Settlements** | NMS Frontiers settlements (R8 §10) | The *colonists* idea (`11`): thaw *Arkwright* colonists to found a settlement you oversee. Its jobs feed your industry. Avoid NMS's weak-payoff criticism by making settlements produce real goods | Post-1.0 |
| **Base-to-base travel** | NMS teleporters | Done the Apoapsis way: link into a proxy at the other base. No teleporting matter | Core (via §14.1) |
| **Damage-control crew loop** | Barotrauma, FTL | Hull breaches, fires, floods (Thalassa subs) and reactor scrams need someone to act. Pairs with multi-crew consoles (`15` §15.5) | Post-1.0 |
| **Torch transfers** | Elite's fast travel, done with real physics | Late game, the Fusion Torch (`06` §6.2) flies constant-thrust "brachistochrone" transfers that cut coasts sharply: Verdance to Tempest takes roughly 3 hours at 0.2 g of constant thrust instead of a 7.9-hour Hohmann coast. That's NMS-like speed within the system, honestly earned | Core (Age 6) |
| **Ship log of knowledge** | Outer Wilds | The Custodian Log records clues and connections, not just quests. Some Precursor puzzles open only when you know something | Core |
| **Habitat depth progression** | Subnautica | Thalassa and Glacis gate deeper zones by hull rating, with seabases built from grids | Core (Age 5–6) |
| **Weather and hazard forecasting** | NMS storms, plus our space weather | Storm warnings on planets, flare forecasts in space (`11` space weather) | Post-1.0 |

## 14.7 Feature checklist: best of other games

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
