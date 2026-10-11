# 15 — Multiplayer

> Design by Opus. 47noodles: multiplayer "is a big aspect" of Apoapsis. This section gathers the multiplayer design from `03` (time, networking, performance), `05` (ownership, factions, PvP), `06` (comms) and `14` (proxies, hubs) in one place, and adds what was missing. Precedents: Luna Multiplayer's subspaces and vessel locks (R3 §7), SE's ownership and sharing (R1 §2.3). Licences marked ✔ were checked on Modrinth on 2026-10-11.

## 15.0 Multiplayer pillars

1. **Nobody waits.** No player is ever stuck watching a coast timer while friends play. Real time, cryo-link, proxies and consensus warp work together (§15.2).
2. **Presence anywhere the network reaches.** Link into a proxy to work beside a friend on another world tonight (`14` §14.1).
3. **Co-op by default, rivalry by choice.** Friends' servers are cooperative out of the box. Factions, PvP and economy competition are server options.
4. **Shared, persistent universe.** Orbits keep moving while you're offline (as long as the server is running), cargo ships arrive, and the comet comes round on schedule for everyone. If an empty server pauses itself (vanilla's pause-when-empty setting; verify on 26.3), the whole universe pauses with it, because UT follows ticks.
5. **Healthy servers.** Every system has a budget, so 10 friends or 50 strangers keep 20 TPS.

## 15.1 Server profiles (one-click presets)

| Profile | Size | Defaults |
|---|---|---|
| **Friends** | 2–10 | Team progression shared (`04` §4.4). PvP off. Consensus warp, with idle players counted as warp-safe so AFK friends never block it. Unlimited proxies. Generous complexity budget. Offline protection on |
| **Community** | 10–50 | Factions and alliances, Open Parties and Claims land claims, NPC and player hub economy, PvP in zones only, per-player complexity budgets, cleanup policy, subspace warp available |
| **Hardcore** | any | `real` or `classic` scale, light-speed link delay, limited engine ignitions, PvP on, permadeath crews (`11`) |
| **Creative** | any | `ages.mode = sandbox`, free warp, no budgets: a shipyard and showcase server |
| **Singleplayer** | 1 | Free KSP-style warp (`03` §3.5), everything else as Friends |

## 15.2 Time and presence: how "nobody waits" works

| Situation | What happens |
|---|---|
| A friend launches to Pale while you're mining at home | Both happen in real time. Pale is a 23-minute trip (`07` §7.1) |
| You're coasting 3 hours to Ochre | **Cryo-link** to your home Clone Bay and keep playing. You get a ping at the SOI change and node time and on arrival, then relink in one click. The Flight Computer flies the burns (`03` §3.5) |
| A teammate is already at your Ochre outpost | Link into your **proxy** there and help *now*. Your cargo ship can arrive later (`14` §14.1) |
| Everyone wants to skip an 8-hour coast to Tempest (or 23 hours on to Hollow) | **Consensus warp**: when every online player is warp-safe (cryo-linked, asleep, AFK-opted-in or on rails), the orbital clock jumps. It's capped by any grid that's under thrust or in atmosphere |
| A big server with dozens of independent players | Optional **subspace mode** (LMP-style per-player time offsets, R3 §7.1). A player in the future can't touch surfaces until server time catches up. Off by default |
| You log off mid-transfer | Your grid stays **on rails**, scheduled burns run as background burns (`03` §3.4), and you arrive while offline. The optional Discord bot (`11`) pings you |
| A new player joins a server that's in Age 5 | They start Age 0 on Verdance, and their team can catch up their Age (`04` §4.4). They can also join friends on Pale on night one through a **guest Link Pod** and a loaned proxy, acting with the lender's Age while linked (`14` §14.1) |

## 15.3 Teams, factions and alliances

| Layer | What it is | Built on |
|---|---|---|
| **Team (party)** | Shared Age progress, shared Survey Points, shared map markers and orbit display, shared blueprint library | Open Parties and Claims parties (LGPL-3.0 ✔, 26.3 ✔) through an integration, or Apoapsis's own team store if OPAC isn't installed |
| **Faction** | SE-style ownership group: grids and blocks shared with the faction (`05` §5.1), NPC reputation per faction, faction tags on transponders | Apoapsis |
| **Alliance** | Several factions sharing docking rights and a hub network, founded at a player Hub (`14` §14.2) | Apoapsis |

## 15.4 Claims, protection and griefing

- **Land claims:** Open Parties and Claims (LGPL-3.0 ✔, 26.3 ✔) protects terrain on every planet face. Apoapsis adds **orbital claims** for parking volumes around a player station, with no-build and no-dock flags.
- **Ships are protected by ownership.** Non-owners can't open terminals or grind computer components unless PvP rules allow takeover (`05` §5.1).
- **Offline protection** (server option): grids of offline players that are anchored or inside a safe zone take no PvP damage.
- **Safe zones** around NPC hubs and optionally around spawn (`05` §5.4.11).
- **Debris rules:** cleanup of unowned junk (`05` §5.10), and salvage rights on wrecks after a timeout (whoever owned the wreck gets first claim).
- **Anti-grief:** action logs per grid (who welded, ground down, changed ownership), rollback for admins, and limits on tractor-beaming other players' grids.

## 15.5 Playing together

- **Multi-crew ships.** Separate **pilot**, **engineer** (power priorities, heat, damage control), **navigator** (map, maneuver nodes, transfer planner) and **gunner** consoles, so a four-person ship has four jobs. A **Mission Control** console at a Ground Station can plan nodes and push them to a ship in flight.
- **Control locks** (LMP lock system, R3 §7): one player holds a grid's control lock, and others are crew or passengers. The lock passes on request and drops on disconnect. Physics stays server-authoritative, so there's no "who simulates" problem.
- **Shared megaprojects** (`04` §4.3): contributions are tracked per player and per faction on a progress board, and finished projects carry the contributors' names.
- **Player contracts:** haul this, mine that, rescue me (`05` §5.9). A stranded player can fire a **distress beacon**, which posts a rescue contract on the always-open emergency channel (§15.6).
- **Shared science:** team Survey Points, plus Discovery Registry naming credited to the scanner (`14` §14.5).
- **Drop-in co-op via proxy loans** (`14` §14.1).
- **Team blueprints:** a shared library, so the team shipyard can print everyone's designs.

## 15.6 Comms networks, voice and radio

### Networks: owned, independent, closed by default

Every relay, antenna and Ground Station belongs to a **network**. A network is owned by a player, team, faction or alliance, and **sets its own rules**.

| Permission | What it allows |
|---|---|
| **Join** | Use the network's relays for your own links (proxies, drones) |
| **Read** | Hear its radio channels and see what it broadcasts (transponders, shared survey maps, beacons, market prices) |
| **Transmit** | Speak on its channels and broadcast your own data |
| **Relay-through** | Pass traffic across it between other networks (peering) |

- **Closed by default:** a new network allows only its owner's group.
- **Easy to open:** one click picks a preset. *Private* (default), *Friends* (allow list), *Public* (anyone can join and read), *Broadcast* (anyone can read, only the owner transmits, for news, beacons or a station's prices), or *Custom*.
- **Your side:** subscribe to open networks to hear and see them, or **block** any network so its voice and broadcasts never reach you.
- **Peering:** two networks can agree to relay each other's traffic, so alliances share coverage without merging.
- **Across star systems:** in The Reach (`14` §14.4), gate relays carry networks between systems, so a network can span the galaxy if its owner builds or peers for it.
- **Closed traffic stays private.** Without Read permission you can't hear or see a network's traffic, even when its relays are in range.
- **Emergency channel:** one channel ignores network settings. Every receiver in range hears it and nobody can block it. Distress beacons and Mayday calls go out on it, rate-limited per player, with abuse visible to admins.

### Voice and radio

- **Voice:** Plasmo Voice (LGPL-3.0 ✔, 26.3 ✔) ships in the open pack, with an **Apoapsis radio add-on**:
  - **In atmosphere:** normal proximity voice.
  - **In vacuum:** **no sound carries.** You only hear people through suit or ship **radio**, or by touching helmets (contact range).
  - **Across distance:** radio voice rides the same comms network as proxies (`06` §6.6). Talking to someone on Ochre needs relay coverage between you, with static on weak signal and optional light delay. Comms infrastructure becomes social infrastructure.
- **Text:** team, faction, alliance and local channels, plus a **radio channel** that needs the same link.
- Simple Voice Chat is All Rights Reserved ✔, so it's only an optional compat module, never bundled.

## 15.7 PvE and PvP

- **PvE pressure that scales:** Salvager raids and Remnant drone activity scale with a team's wealth, grid count and online player count, so groups face proportionate threats, and there's a quiet mode for builders.
- **PvP modes:** off / zones only / everywhere. Zones are set by planet, orbit band or claims.
- **Rules:** optional no-offline-raids, takeover by grinding only in PvP, and a **bounty board** (contracts on other players, PvP servers only).
- **Fair fights at speed:** server-authoritative hit detection, and weapon range limited by turret tracking rather than client latency.

## 15.8 Networking and sync

| Concern | Approach |
|---|---|
| Authority | **Server-authoritative physics** (APC on the server). Clients send inputs only |
| Grid motion | 20 Hz transform snapshots, delta-compressed, interpolated on clients. The pilot gets **client-side prediction** with server reconciliation |
| Interest management | Players receive full updates for grids in their bubble (`03` §3.3.1), low-rate updates within render distance, and rails elements only (orbit lines on the map) beyond that |
| Docking and close formation | Inside one bubble, grid states are sent **relative to each other**, so docking stays smooth on high-ping connections |
| Block changes on grids | Standard section deltas addressed by grid ID |
| Proxies and dimension hops | Pre-warmed destination state, so links and transitions show a short transition effect, not a loading screen (`03` §3.3.2, `14` §14.1) |
| Bandwidth target | Typical play under ~150 KB/s per player, with large-battle spikes capped by priority (nearest and fastest-changing grids first) |
| Time sync | Server UT broadcast every second, so clients extrapolate rails objects locally |

## 15.9 Server performance and scaling

- **Budgets** (`03` §3.7): 10 players, 40 active grids and 2,000+ rails objects at 20 TPS on 4 cores. Community servers scale with complexity budgets.
- **Planets sleep.** Planet dimensions with no players, proxies or force-loaded machines unload after the idle timeout (R9 §7). Their bases stay simulated only through optional chunk loaders, which count against the complexity budget.
- **Far grids go on rails.** Only grids near players run full physics.
- **Chunk tickets** for moving grids are rate-limited per player.
- **Profiling:** `/apoapsis perf` shows physics ms per grid, ticket counts, halo-sync cost and bandwidth per player.
- **Post-1.0, multi-server universe** (for very large public servers): shard planets across backend servers behind a proxy. Ships hand off at the transition altitude, which already moves them between dimensions, so the hand-off is a natural seam. Very ambitious, parked in `11`.

## 15.10 Persistence, safety and admin

- Offline players' grids keep orbiting while the server runs, burns run in the background, and arrivals are queued for login.
- Scheduled backups. A grid snapshot export and import tool (open `.apbp` format, `05` §5.3).
- Admin tools: grid list, freeze, delete, teleport, ownership transfer, physics stats, warp override, time-of-universe control (`05` §5.10).

## 15.11 Community events

- **Comet season** when the Wanderer is near perihelion, every two days (`07` §7.9). It's a server-wide intercept race.
- **Eclipses and transfer windows** announced in chat (and by the optional Discord bot).
- **Megaproject campaigns** with a server progress board.
- **Leaderboards:** cheapest Δv to each body, first landings, races (`11` §11.1).
- **Post-1.0:** NMS-style seasonal expeditions, with community goals and a fresh-start universe for a limited time.

---

## 15.12 Feature checklist: multiplayer

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
