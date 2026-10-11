# 13 — Visual design and art direction

> Design by Opus. 47noodles (chat, 11 Oct 2026): *"Visual design should be a big part. Making it feel less like minecraft and more like a polished game, including UI, Menu screen, etc."* Art is created mostly through the AI art pipeline with a human art lead (`16` §16.2). Licences marked ✔ were checked on Modrinth on 2026-10-11.

## 13.0 The goal

**It should feel like a polished space game first and Minecraft second**, while blocks stay readable and buildable. Five principles:

1. **One design language** across world, UI, sound and marketing. A player should recognise an Apoapsis screenshot instantly.
2. **Diegetic first.** Information lives in the world where it can: helmet visor HUD, cockpit screens, terminal panels, holographic projectors.
3. **Every action has feedback:** an animation, a sound and a particle. No silent clicks (§13.6).
4. **Clarity over decoration.** Readable at a glance, readable at distance, accessible by default.
5. **Consistent at every scale,** from a 16×16 texture to the planet seen from orbit.

## 13.1 Brand identity

- **Logo:** an original wordmark, APOAPSIS, with an orbit-ellipse mark whose highest point (the apoapsis) is lit.
- **Core palette:** deep-space navy (backgrounds), Meridian amber (the accent, the star's colour), signal teal (anything interactive), plus warning red, success green and neutral greys. Every colour is defined as a theme token used by the UI, the HUD and the website.
- **Typography:** open-licensed (OFL) fonts loaded through Minecraft's TrueType font support. A clean geometric sans for UI, a monospace for numbers and telemetry, and the vanilla font kept for chat and signs as an option. *Pick final fonts and confirm their OFL licences before Stage 1 ships.*
- **Key art and trailer frames** come from the same palette and rendering as the game, so marketing never promises a different look.

## 13.2 The Apoapsis interface (UI kit)

A single component kit, authored as SVG by the AI art pipeline and documented in Penpot (open source):
- **Panels:** dark translucent "glass" with thin teal borders and a subtle depth shadow. Content sits on a 4 px grid.
- **Controls:** buttons, toggles, sliders, tabs, dropdowns, search fields and steppers, each with hover, focus, pressed and disabled states. **Hover and focus states are designed for keyboard and mouse first**, because this is Java Edition. Gamepads are supported through Controlify (`16` §16.5), but mockups and key hints show keyboard and mouse.
- **Data display:** tables, Δv bar charts, gauges, arcs, sparklines and tooltips with stat comparison.
- **Feedback:** toasts, mission tracker, confirmation modals, progress rings.
- **Icons:** an original set of about 300 icons on one grid and stroke weight.
- **Motion:** 150–250 ms ease-out. Panels slide and fade, numbers tick up, and nothing pops in without a transition. A *reduce motion* setting turns all of it off.
- **UI sound:** soft clicks, confirm chirps, error buzz and radio blips, all one family (`16` §16.3).
- **Classic UI toggle:** for compatibility and personal taste, one switch returns vanilla-styled screens.

## 13.3 Front-end screens

| Screen | Design |
|---|---|
| **Title screen** | A live 3D scene: Verdance from orbit with Pale rising, rendered by our planet renderer, with a slow camera drift and the logo. The menu sits on the left: Play, Universes, Blueprints, Codex, Settings, Credits. A **What's new** panel shows the latest patch notes (`12` §12.6), over the title music |
| **Universe browser** | Singleplayer worlds and servers shown as **universe cards**: system preview art, the Age reached, playtime, players online and comms-network status |
| **World-creation wizard** | Step-by-step choice of scale, server profile, story or sandbox, difficulty and seed, with a live preview of the homeworld (`16` §16.6) |
| **Loading and transitions** | Themed art per destination with tips and a real progress bar. Link static, re-entry glow and the jump-drive flash cover dimension changes (`03` §3.3.2) |
| **Pause and settings** | Restyled pause menu. Settings regrouped as Graphics, Audio, Controls, Flight, Interface, Accessibility and Multiplayer, with search and live previews |
| **Death and respawn** | Instead of "You died!", a **revival sequence**: you wake at your bed or Landfall pod as the HUD reboots. From Age 4 the **Clone Bay** version plays: the vat drains, the clone wakes, and the HUD boots up |
| **Credits** | Auto-generated from REUSE metadata, scrolling over orbital footage |

## 13.4 In-game HUD and screens

**Helmet HUD (diegetic):**
- Suit O₂, H₂, energy, temperature and radiation shown as arcs at the visor edge, plus a compass ribbon with waypoints, signals and teammates' markers (from the comms network).
- In jetpack mode, velocity and altitude readouts.
- **Visor overlays:** frost on cold worlds, dust on Ochre, condensation on Thalassa, scratches when hit and cracks at low suit integrity. Breath fogs in the cold.

**Cockpit HUD:**
- The navball, flight data and Δv per stage (`06` §6.5).
- **Projected markers on the world:** prograde, target, landing site, docking alignment, plus a horizon line.
- **Cockpit screens are live displays** inside the model, rendered to texture.

**Full-screen apps:**
- **Grid terminal:** SE's control panel rebuilt. Search, groups, block list, drag-to-toolbar, and live block state.
- **Inventories and machines:** reskinned in the kit while keeping slot mechanics, so other mods still work. Item tooltips show stats, and assembler queues and refinery yields are visible.
- **Maps:**
  - The **planet survey map** (cube faces wrapped onto a globe), the **system map** (orbits, nodes, windows) and the **galaxy map**.
  - One continuous **zoom from planet to system to galaxy**.
- **Codex** (Holo-Manual), **Custodian Log**, and a **Discovery Registry** viewer with rotating 3D previews of species.
- **Radio console:** chat restyled as radio, with channel tabs for team, faction and networks, a signal-strength meter, and static on weak signal (`15` §15.6).
- **Toasts and indicators:** quest tracker, damage-direction indicators, alerts for SOI changes and comms links.

## 13.5 The world: less "Minecraft", still blocks

- **Detailed block models** for every functional block (Blockbench):
  - Bevels, panel seams, greebles and emissive screens.
  - **Animated parts:** turning radar dishes, blinking beacons, engine flywheels and gauge needles that follow the throttle, pumping pistons, spinning turbines, opening hatches, extending landing gear.
  - Damage states.
- **The look tells you the Age.** The Ages 1–4 look is brass, bronze and copper over dark steel, with gauges and exposed piping, as in the thruster concept (§13.8). It stays distinct from Create's well-known brass-and-wood style: no wooden casings, but riveted dark-steel frames, hazard striping and status lamps. Age 5 shifts to steel, composites and teal-lit screens. Age 6 and Precursor tech go sleek and luminous. A player can judge a block's tier at a glance.
- **Connected textures** let hull plating and glass merge into smooth large panels, so big ships read as ships, not grids. They use the standard connected-texture resource formats. **Continuity** (LGPL-3.0 ✔) reads those but is Fabric-only on 26.3 ✔ (its last NeoForge build is for 1.21.1), so we fork-port it to NeoForge, keeping its LGPL licence, or write our own reader for the same formats if the port proves heavy. Decide in Stage 1. Panel lines and **decals** sit on top, plus per-block paint (`16` §16.2).
- **Light and glow:**
  - Emissive textures for screens, engines and running lights.
  - **Dynamic lights** from engines, headlights and suit lamps come from our own implementation. LambDynamicLights uses a custom non-OSI licence ✔. The MIT options (RyoamicLights ✔, Sodium Dynamic Lights ✔) are on older Minecraft versions and could be ported.
- **Post-processing without a shader pack**, through our own passes in the Blaze3D pipeline. Vanilla already runs post-effect chains, for example the creeper view.
  - Effects: per-world colour grading, gentle bloom on emissives, vignette, exposure adaptation (stepping out of a hangar into Cinder's noon), G-force greyout and redout, heat haze behind engines, and a lens flare from Meridian.
  - Every effect has a slider. *Confirm how far 26.3's post-effect pipeline stretches in Stage 3.*
- **Skies:** a starfield with a galactic band and nebulae, the sun's corona, planets with atmospheric scattering, rings and auroras (`03` §3.6), all per world.
- **The character:**
  - Original suit and helmet designs with suit paint.
  - **Animations:** zero-g floating poses, jetpack lean and thrust, magnetic-boot steps, ladder and hatch moves, and first-person tools (welder sparks, grinder glow).
  - Optional visible body in cockpits.
- **Camera feel:**
  - FOV kick on boost, launch and landing shake scaled by thrust, gentle head sway in zero-g, and a lagging chase camera for ships.
  - A cinematic photo mode.
  - Everything respects *reduce motion*.

## 13.6 Feedback ("juice") for key actions

| Action | Visual | Audio | Gamepad rumble (Controlify) |
|---|---|---|---|
| Welding a block | Sparks, glow, build-up shimmer through construction stages | Arc crackle rising in pitch | Light buzz |
| Stage separation | Decoupler flash, debris, plume swap | A deep thunk, then the new engine's roar | Strong jolt |
| Docking | Alignment beams turning green, magnetic snap | A clunk, then a hiss as the seal forms | Click |
| Airlock cycle | Lights sweep red to green, vent mist | Pressure hiss fading to silence in vacuum | — |
| Re-entry | Plasma builds on leading blocks, which glow with heat, and the cockpit shakes | A roar muffled by the hull | Rumble rising with heat |
| Proxy link | Static fades into the proxy's view | A radio sweep tone | Short pulse |
| Discovery scan | Scan lines over the target, then the registry card unfolds | A bright chime | Tap |

## 13.7 Accessibility and options

UI scale, a high-contrast theme, colour-blind palettes for the HUD and maps, *reduce motion*, subtitles with speaker tags, font size, the Classic UI toggle, and remappable everything (`16` §16.5).

## 13.8 Pipeline and quality bar

- **Style guide:** reference boards (original concepts only, `09` §9.3), palette tokens, model and texture rules, an animation guide, and example "good / not yet" pairs.
- **Design before code** (47noodles' rule, `12` §12.3): every screen and block is designed and approved before it's implemented, one stage ahead.
  - The UI kit lives in Penpot (open source).
  - Flows such as the grid terminal, map view, navball and radio console are also built as **clickable HTML prototypes**, so layout and flow can be tried in a browser before any Java.
  - Implementation PRs show screenshots next to the approved mockup.
- **AI art pipeline plus art lead** (`16` §16.2): textures generated by scripts, the UI kit as SVG, models as Blockbench JSON, VFX parameters. Every asset passes a **visual review checklist**: palette, readability at distance, consistency, animation, emissives, accessibility.
- **Golden-image tests in CI** for key UI screens and HUD states, to catch regressions on both OpenGL and Vulkan (`16` §16.7).
- **Performance budgets:** post-processing ≤ 1 ms per frame on mid-range GPUs, plus UI draw-call caps.

### Concept art log

| Date | Subject | Artist | What it sets | Status |
|---|---|---|---|---|
| 2026-10-11 | Thrusters: small (1×1×2) and large (3×3×4) | CallMeCarlos | The first liquid rocket engines: a machine powerhead (flywheel turbopumps, pressure gauges, status screens, coloured fuel and power ports) over a stepped bell wrapped in copper cooling lines. Brass, bronze and copper over dark steel | **Adopted** for the hydrolox "Spark" and "Colossus" (`06` §6.2) and the Age look rule (§13.5). Simplify the small engine's gears and gauges so they read at 16 px per block. Method (hand-drawn or AI-assisted) to be confirmed; AI-assisted art is labelled (`CONTRIBUTING.md` rule 10). The image joins `docs/concept/` once the artist confirms CC BY 4.0 |

## 13.9 Compatibility

- Other mods' screens get our theme on standard vanilla widgets but keep their own layouts.
- JEI overlays are supported.
- Every screen works with keyboard and mouse first, and is also navigable with Tab, the arrow keys or a gamepad.
- The Classic UI toggle is always one click away.

## 13.10 Visual design by stage (`12`)

Each row is **designed and approved one stage earlier** (the design gate, `12` §12.3) and **implemented** in the stage shown. Stage 0 produces designs only.

| Stage | Visual deliverables (implemented) |
|---|---|
| 0 Foundations | Designs only: brand, UI kit v1, HUD, every Stage 1 screen and block model, approved before Stage 1 code starts |
| 1 Engineers | Brand identity, UI kit v1, helmet HUD, grid terminal, functional block models, connected-texture hull panels, welding and thruster feedback |
| 2 Orbits | Navball, map view and system map, cockpit HUD and live screens, re-entry and stage-separation effects |
| 3 Round Worlds | Skies, atmospheric scattering, post-processing and colour grading, planet survey map |
| 4 Landfall | **Title screen**, universe browser, world-creation wizard, Codex and Custodian Log, Clone Bay revival |
| 5 Together | Radio console, proxy-link transitions, hub interiors |
| 6–7 | Per-world palettes, creatures and plants, galaxy map |
| 8 Launch | Final polish pass on every screen, auto-generated credits, trailer, key art, golden-image test coverage |

## 13.11 Feature checklist: visual design

- [ ] Brand identity: logo, palette tokens, OFL fonts, key art
- [ ] UI kit (SVG) with keyboard-and-mouse-first hover and focus states (gamepad via Controlify), motion rules, UI sounds and the Classic UI toggle
- [ ] Title screen with live orbital scene, universe browser, world-creation wizard, themed loading and transitions, restyled settings, revival sequence (Clone Bay from Age 4), auto-credits
- [ ] Helmet HUD with visor overlays; cockpit HUD with projected markers and live screens
- [ ] Grid terminal, reskinned inventories and machines, survey, system and galaxy maps with continuous zoom, Codex, Discovery viewer, radio console
- [ ] Detailed animated block models, connected textures (Continuity port or our own), decals and paint
- [ ] Our own dynamic lights; emissives
- [ ] Post-processing passes (grading, bloom, vignette, exposure, G-force, heat haze, flare) with sliders
- [ ] Per-world skies (starfield, corona, scattering, rings, auroras)
- [ ] Suit designs and animations; camera feel; photo mode
- [ ] Feedback table (§13.6) implemented for every listed action
- [ ] Accessibility options; golden-image CI tests; performance budgets
- [ ] Design gate: approved mockups or prototypes for each stage's screens and blocks before implementation, with side-by-side design QA
