# 09 — Open-source charter, licensing and dependencies

> Rule set by 47noodles (chat, 11 Oct 2026): *"open source absolutely everything"*, *"full editability, no legal issues"*. Anything that isn't open source gets rebuilt from the ground up as original Apoapsis work. Licences marked ✔ were checked by the orchestrator against Modrinth's API or the project's own LICENSE file on 2026-10-11.
>
> *This is a design decision record, not legal advice. Before the first public release, have someone qualified read the final pack manifest and this charter. It's cheap insurance.*

## 9.1 The charter

1. **Everything the pack bundles is open.** Every line of code, texture, model, sound, quest, worldgen file and config in Apoapsis and the **Apoapsis Ages** modpack uses an OSI-approved code licence or a free-culture asset licence.
2. **Nothing restricted is bundled.** That means no All-Rights-Reserved, no Non-Commercial (NC), no No-Derivatives (ND), no source-available (PolyForm, BUSL), and no bespoke "custom mod licence" in the pack.
3. **Optional extras are labelled.** A short list of source-available extras (e.g. Sodium) can sit in the pack as **optional entries that the launcher downloads from their official Modrinth page**. Apoapsis never redistributes or modifies them. They can be on by default, the pack must work fully without them, and they're clearly marked as not open source.
4. **Rebuild only what is both core and blocked.** Apoapsis writes its own version, with original code, art, text and names, only when a dependency is **(a) core to Apoapsis gameplay and (b) under a licence that stops us forking, porting or reshipping it**. That covers Sable (rebuilt as APC), Create's assets (rebuilt as Kinetics) and FTB Quests (rebuilt as `ages`). Things that aren't core and don't need changing, like Sodium, stay as optional extras rather than being rebuilt.
5. **Everyone can edit everything.** The source, art sources (`.bbmodel`, `.aseprite`, `.blend`), quest data and worldgen are in the public repo, and anyone can fork the whole pack.

## 9.2 Licences Apoapsis uses

| Content | Licence | Why |
|---|---|---|
| Code (all modules) | **MIT** | Maximum freedom, which is what 47noodles asked for. *Alternative:* LGPL-3.0 for the physics engine (`physics/`) only, which would force people who improve the engine to share their changes. MIT is the default unless 47noodles prefers that. |
| Art, models, sounds, music | **CC BY 4.0** | Commercial use and remixing allowed, credit required. It's the asset-world equivalent of MIT. Avoid NC: it blocks servers that take donations and isn't "open" in the OSI sense |
| Worldgen JSON, recipes, quests, configs | **MIT** (along with the code) | Pack makers can lift any of it |
| Contributions | **DCO sign-off** (`Signed-off-by:`), inbound = outbound, no CLA | Lightweight, standard for open projects |
| Repo hygiene | **REUSE-compliant**: SPDX headers in code, `REUSE.toml` for docs and data, `reuse lint` in CI | Licence status is machine-checkable |

## 9.3 Clean-room rules ("different enough to be our own IP")

**What copyright does and doesn't cover.** Game *mechanics*, rules, ideas and maths can't be owned: Kepler's laws, the rocket equation, rotational power networks, ore multiplication, jump drives, staged progression. **Code, art, models, sounds and written text are protected**, and so are recognisable expressive designs. **Names and logos** can be trademarks: Space Engineers, Kerbal, SevTech, Create, Prototech, Kerbin.

Rules for every contributor:

1. **Never copy** code, assets or text from a non-open project. That includes Sable, Create's assets, Space Engineers and KSP game files, SevTech's quest text or configs, and FTB Quests data.
2. **Build from the spec, not from their source.** These design docs plus public gameplay knowledge (videos, wikis describing *behaviour*) are the spec.
3. **Separate readers from writers.** Anyone who has studied a non-open codebase in depth doesn't write the matching Apoapsis module from memory. (This design's research only *measured* Sable: file and mixin counts, plus the vanilla class names it targets, which are facts, not expression. The clone was deleted afterwards.)
4. **Original names everywhere.** Use "Precursor", not "Prototech", and "Meridian/Verdance", not "Kerbol/Kerbin". Generic engineering words are fine: gyroscope, refinery, merge block, jump drive, connector, landing gear.
5. **Original art from day one.** Placeholders are flat colours or our own sketches, never someone else's textures "for now". Follow the style guide in `13-art-direction.md`.
6. **Look different, not just code different.** Courts have found clones infringing when they copied a game's distinctive *look* even with new code (*Tetris Holding v. Xio*, US 2012). Reusing API declarations in a new implementation was held to be fair use (*Oracle v. Google*, US Supreme Court 2021), and a program's functionality isn't protected (*SAS Institute v. World Programming*, CJEU 2012). Apoapsis has its own art style, models, UI and names.
7. **No decompiling closed mods.** Studying behaviour (playing, watching, reading wikis) is fine. Decompiling ARR or closed jars is where the legal risk is.
8. **"Inspired by" is fine; borrowed branding isn't.** Descriptions can say "inspired by Space Engineers, Kerbal Space Program and SevTech". Mod names, logos and art must not use their names or marks.
9. **Patent glance before 1.0.** A few game mechanics are patented (e.g. WB Games' Nemesis System; the patents Nintendo sued Pocketpair over in 2024). The risk is low for a free mod, but run a quick search on anything that looks distinctive.
10. **Licence gate in CI.** The pack build fails if any bundled jar's licence isn't on the allow-list (MIT, Apache-2.0, BSD-2/3, LGPL-2.1/3.0, GPL-3.0, MPL-2.0, Unlicense, CC0, CC BY, CC BY-SA). The licence field comes from Modrinth's API with a manual override file.

## 9.4 Dependency matrix

Decisions: **Keep** (open, use as is) · **Fork-port** (open; we carry it to our MC version if upstream hasn't) · **Rebuild** (not open; we write our own) · **Extra** (optional, user-installed, not bundled) · **Drop** (not needed).

| Need | Candidate | Licence | On 26.3? | Decision |
|---|---|---|---|---|
| Moving grids, physics | Sable | PolyForm Shield 1.0.0 ✔ (non-compete) | No, 1.21.1 ✔ | **Rebuild → APC** (`03` §3.1) |
| | Valkyrien Skies 2 | Mod LGPL-3.0 ✔. Core `vs-core`/Krunch artifacts not in repo, licence unverified | Unofficial port 26.2 ✔ | **Prototype host only, never shipped** |
| | Jolt Physics + jolt-jni | MIT ✔ | n/a (native library) | **Keep** (bundled natives) |
| | Rapier | Apache-2.0 | n/a | Keep as a possible second backend |
| Kinetic machines (the early-game Create role) | Create | Code MIT ✔, **assets All Rights Reserved** ✔ | No, 1.21.1 ✔ | **Rebuild → Apoapsis Kinetics** (original machines, art and names, built grid-aware from day one; `08` §8.2). Optional **Create compat** module unifies `c:` tags when Create is installed |
| Animated in-game tutorials | Ponder | MIT ✔ | No, 1.21.1 ✔ | **Fork-port** (or write our own "Holo-Manual") |
| Industry, chemicals, reactors | Mekanism (+ Generators, Tools, Additions) | MIT ✔ (R10 found no separate asset licence stated) | Released on 1.21.1 ✔; upstream's default branch is `26.3` (10.8.0 alpha ✔) | **Keep, and fork-port if needed.** Stage 1 needs it. If upstream's 26.3 alpha isn't usable when Stage 1 starts, maintain a public port branch (budgeted in `02` §2.3) and send PRs upstream |
| Staged progression | Game Stages family | LGPL-2.1 ✔ | No, 1.20.3 ✔ | **Rebuild →** `ages` module (stages, gates, quest book) |
| | AStages, FTB Quests, Together Forever | ARR (R2) | — | Drop |
| | Triumph | 1.12.2 only | — | Drop (concept rebuilt) |
| Pack scripting | KubeJS | LGPL-3.0 ✔ | 26.1.2 ✔ | **Keep** (optional tool for pack makers) |
| In-game programmable computers | CC:Tweaked | Code MPL-2.0 ✔ (OSI). Legacy ComputerCraft textures, models and help files CCPL ✔ (not OSI). Modrinth's licence field still says CCPL | 26.3 | **Our own design →** Flight Computer with sandboxed Lua and an API built around grids (`05` §5.7). A design choice, not a licence block: the code is open, but the CCPL assets keep it out of the all-open core pack. Optional CC:Tweaked peripheral compat |
| Recipe viewer | JEI | MIT ✔ | **26.3** ✔ | **Keep** |
| | EMI | MIT ✔ | 1.21.1 | Optional |
| Block-info HUD | Jade | CC BY-NC-SA 4.0 ✔ (NC) | 26.3 | **Drop.** Built-in Apoapsis inspector HUD |
| Server performance | Lithium | LGPL-3.0 ✔ | 26.3 ✔ | **Keep** |
| | ModernFix | LGPL-3.0 ✔ | 26.1.2 ✔ | Keep (when ported) |
| | C2ME | MIT ✔ | Fabric 26.x ✔; NeoForge port `c2me-neoforge` has 26.3 alpha builds ✔ | Candidate: test the NeoForge port for chunk-generation speed once it leaves alpha |
| Client rendering performance | Sodium | **PolyForm Shield 1.0.0** ✔ (current `dev` LICENSE.md, read directly. Older Sodium was LGPL-3.0, and Embeddium continues from that last LGPL line) | 26.3 ✔ | **Extra, on by default**: an optional pack entry downloaded from Modrinth, never forked or reshipped. Rebuilding a renderer optimiser adds no gameplay. Apoapsis's grid, sky and curvature rendering ships a Sodium compat path and works with or without it |
| Shaders | Iris | LGPL-3.0 ✔ (requires Sodium) | 26.3 ✔ | **Extra** (alongside Sodium) |
| | Embeddium | LGPL-3.0 ✔ | Stops at 1.21.4 ✔ | Watch. If an OSI renderer-optimiser revives on 26.x, it can join the core pack |
| Far-terrain LOD | Distant Horizons | LGPL-3.0 ✔ | 26.3 ✔ | **Keep** (bundled, optional dependency). Apoapsis registers its own LOD generator that computes far terrain straight from planet noise (`17` §17.5) |
| | Distant Horizons SeedGen | All Rights Reserved ✔ | 26.3 | Not used. It only proves DH accepts seed-based generator plug-ins. Ours is written from scratch |
| | Voxy | All Rights Reserved ✔, Fabric only | 26.2 | Not supported |
| Connected textures | Continuity | LGPL-3.0 ✔ | 26.3 Fabric-only ✔ (last NeoForge build is for 1.21.1) | **Fork-port to NeoForge** (it stays LGPL), or write our own reader for the same connected-texture formats, so hull plating and glass read as smooth panels (`13` §13.5) |
| | Fusion | All Rights Reserved ✔ | 26.3 | Not used |
| Dynamic lights | LambDynamicLights | Custom "Lambda" licence ✔ (not OSI) | 26.3 | Not used. **Our own dynamic lights** (`13` §13.5) |
| | RyoamicLights, Sodium Dynamic Lights | MIT ✔ | 1.21.1 / 1.21.5 | Port candidates (MIT) if ours lags |
| Custom menus | FancyMenu | Custom licence ✔ (not OSI) | 26.3 | Not used. Our own front end (`13` §13.3) |
| Voice chat | Plasmo Voice | LGPL-3.0 ✔ | 26.3 ✔ | **Keep** (bundled). The Apoapsis radio add-on routes voice over comms networks (`15` §15.6) |
| | Simple Voice Chat | All Rights Reserved ✔ | 26.x | Optional compat module only, never bundled |
| Land claims and parties | Open Parties and Claims | LGPL-3.0 ✔ | 26.3 ✔ | **Keep** (bundled), integrated with teams (`15` §15.3) |
| Controllers | Controlify | LGPL-3.0-or-later ✔ | 26.3 ✔ (NeoForge) | **Keep** (bundled), with Apoapsis flight bindings (`16` §16.5) |
| Replays and cinematics | ReplayMod (GPL-3.0 ✔, 26.2), Flashback (All Rights Reserved ✔) | Mixed | — | ReplayMod optional; Flashback not used. Our own grid flight recorder later |
| Runtime dimensions | Infiniverse | MIT ✔ | 26.1.2 on CurseForge only (R9); Modrinth lists only 1.20.x | **Vendor or port it ourselves** (MIT), since a Modrinth `.mrpack` can't fetch it |
| Wrap-world fallback | Toroidal World | MIT ✔ | 26.3 ✔ | Reference or optional dependency for planet tier A |
| Render helpers | Veil (LGPL-3.0 ✔), Flywheel (MIT ✔) | Open | 1.21.1 and older | Not needed. Use Blaze3D directly |
| Worldgen showcase | Terralith (no-modify licence), Biomes O' Plenty (ARR), WWOO (CC BY-NC-ND) | Not open | — | **Drop.** Our own worldgen. Tectonic and Lithostitched (MIT, R9) are fine to learn from or depend on |
| Space content | Ad Astra (Terrarium licence, ARR assets), Advanced Rocketry 3 (ARR), Galacticraft 5 (MIT, pre-alpha) | Mixed | — | **Drop.** Original content. Galacticraft's MIT code may be read for oxygen-sealing ideas |
| SevTech staples (Tinkers', IE, Betweenlands, Astral…) | Various, many unverified (R2) | — | Mostly no | **Drop.** Ages 0–3 content is original (`04`) |

## 9.5 The open pack: what actually ships

**Apoapsis Ages (core; every bundled file is open):** Apoapsis (all modules) · Mekanism + Generators + Tools (+ Additions) · JEI · Lithium · Distant Horizons · Continuity (our NeoForge port, unless we write our own reader) · Plasmo Voice · Open Parties and Claims · Controlify · ModernFix (when ported) · Infiniverse (vendored) · *(pack-maker tool)* KubeJS.

**Optional extras (on by default, toggleable):** Sodium (source-available, PolyForm Shield) and Iris (LGPL-3.0, which needs Sodium) for performance and shaders. The `.mrpack` lists them as optional files fetched from Modrinth, so the pack repo contains no copy of them. Players who want a 100% OSI install untick them.

## 9.6 What "rebuild" costs

| Rebuild | Size | Notes |
|---|---|---|
| APC physics engine | **Very large** | The single biggest work item, needed regardless of licence because of orbital requirements |
| Apoapsis Kinetics (shafts, gears, belt lines, water wheel, windmill, steam engine, millstone, sheet press, alloy mixer, jaw crusher, draft kiln, tool actuator, actuator arm, chutes) | Large | Built grid-aware from day one, which is an advantage over bolting Create onto ships |
| `ages` (stage store, item/recipe/ore/dimension gates, quest book, team sync) | Medium | |
| Flight Computer (Lua VM sandbox, block API, LCD drawing) | Medium | Our own design rather than a licence-forced rebuild (§9.4 matrix) |
| Holo-Manual (Ponder-style animated guides) | Small–medium | Or fork-port Ponder (MIT) |
| Inspector HUD | Small | |
| Original art | **Large** | Roughly 700–1,000 textures, 150–250 models, plus sounds. Recruit community artists early; CC BY 4.0 credits them in-game |

## 9.7 Carrying open dependencies forward ourselves

Because everything in the core pack is open, Apoapsis never has to wait on anyone:
- **Mekanism.** MIT, so a public `apoapsis/mekanism-26.x` fork is allowed. It gets rebased onto upstream when they port and retired as soon as upstream catches up.
- **Ponder.** MIT, the same arrangement.
- JEI, Lithium and Distant Horizons already have 26.3 builds. ModernFix, Infiniverse and KubeJS are on 26.1.2. Infiniverse is MIT, so we can port it if needed; the other two are optional.
- Several 26.3 builds are still pre-releases (checked 2026-10-11 ✔): Plasmo Voice 2.2.0-beta.1, Open Parties and Claims 0.32.8 (beta), JEI (beta) and Sodium (alpha). The pack pins tested versions and moves to stable ones as they land.

## 9.8 Licence audit of the whole plan (can it really be MIT + CC BY?)

> Audit by Opus, 2026-10-11. Library licences were read from each project's own LICENSE file, or from Modrinth's licence field where noted (✔).

### Verdict

**Yes. Everything Apoapsis *authors* can be MIT (code) and CC BY 4.0 (art, docs, text)**, provided the conditions below hold. Two clarifications:
1. **The installed modpack is "100% open source", not "100% MIT".** Some open dependencies are **LGPL** (NeoForge itself, Lithium, ModernFix, Distant Horizons, KubeJS, Iris, Plasmo Voice, Open Parties and Claims, Controlify, Continuity). LGPL is open source, but we can't relabel other people's LGPL mods as MIT, and we don't need to. If an **all-MIT core** matters, the LGPL mods move to optional extras. The core then becomes Apoapsis + Mekanism + JEI + Infiniverse (all MIT), running on NeoForge (LGPL-2.1, unavoidable as the loader).
2. **Minecraft itself is proprietary.** That's normal for every mod and fine: we never ship Minecraft code or assets, and players bring their own copy.

### Component by component

| Part of the plan | Licence | Compatible with MIT/CC BY Apoapsis? | Condition |
|---|---|---|---|
| Our code (all modules) | MIT | ✔ | Must contain **no Minecraft code** (see the risks below) |
| NeoForge (loader we build on) | LGPL-2.1 ✔ | ✔ Using its API doesn't affect our licence | — |
| Mixin, MixinExtras | MIT ✔ | ✔ | — |
| Jolt Physics, jolt-jni | MIT ✔ | ✔ | Keep their notices in `THIRD_PARTY_NOTICES` |
| Rapier (possible second backend) | Apache-2.0 ✔ | ✔ | Keep its NOTICE file |
| Cobalt Lua VM (Flight Computer) | MIT ✔ (derived from LuaJ, MIT ✔), plus a BSD-3 `LICENSE.doubles` for number formatting | ✔ | Keep both notices |
| Bruneton atmospheric scattering (if ported) | BSD-3-Clause ✔ | ✔ | Ported files keep the BSD copyright line and licence |
| Mekanism (+ our port fork) | MIT ✔ (no separate asset licence, R10) | ✔ | — |
| JEI | MIT ✔ | ✔ | — |
| Ponder (if fork-ported) | MIT ✔ | ✔ | Check no separate asset terms before forking |
| Infiniverse, Toroidal World, Tectonic, Lithostitched | MIT ✔ (Modrinth licence field. Tectonic and Lithostitched have no LICENSE file in their repos, so confirm with the authors before vendoring) | ✔ | — |
| KubeJS, Distant Horizons, Lithium, ModernFix, Iris, Plasmo Voice, Open Parties and Claims, Controlify, Continuity | LGPL-3.0 ✔ | ✔ as separate mods. Our compat code can stay MIT | Don't copy their source into ours |
| Sodium (optional extra) | PolyForm Shield ✔ | ✔ only because we **don't ship it**. Our compat code is ours | Never vendor its classes; the pack only references its Modrinth download |
| CC:Tweaked (optional compat) | Code MPL-2.0 ✔; legacy assets CCPL ✔ (not OSI) | ✔ MPL-2.0 is file-level copyleft, so our peripheral code stays MIT as long as we don't modify their files | Optional extra, not core. Add a licence-gate override, because Modrinth's licence field says CCPL |
| Our docs and research notes | CC BY 4.0 | ✔ | Short quotes inside the research notes stay their owners' (noted in that folder) |
| Pack manifest (`.mrpack`), configs, quests, worldgen JSON | MIT | ✔ | References third-party downloads; contains no copies |

### The real risks (and the rules that cover them)

| Risk | Why it breaks MIT/CC BY | Rule |
|---|---|---|
| **Pasting Minecraft code** into mixins (e.g. copying a vanilla method into an `@Overwrite`, or "copy, then modify" from the now-unobfuscated 26.x sources) | Mojang's code is proprietary. Any copied part can't be MIT. **Highest risk area: APC's entity-movement layer**, which re-implements how entities move | Inject and wrap, don't copy. Re-implement behaviour from scratch. Code review checks for vanilla-shaped blocks |
| **Editing vanilla textures** (recolouring iron into "steel", tweaking a vanilla sprite) | Derivatives of Mojang art can't be licensed CC BY | Every texture drawn from scratch. Matching the vanilla *style* is fine; deriving from its *pixels* isn't |
| **Copying vanilla shaders** for the curvature or sky passes | Mojang's GLSL is proprietary | Write our own shaders |
| **Sounds and music from mixed-licence libraries** (e.g. Freesound has CC BY-NC and Sampling+) | NC and Sampling+ aren't compatible | Only original, CC0 or CC BY sounds. Record each source in REUSE metadata |
| **Fonts** | Most free fonts are OFL-1.1, not CC BY | OFL is fine to bundle; annotate it as OFL |
| **Assets from other games or mods** (SE, KSP, Create, Ad Astra…) | Not ours to license | Banned (§9.3) |
| **AI-generated content** | In some jurisdictions, including the US, purely AI-generated material may not be copyrightable. That doesn't stop us publishing it openly, but it makes "licensing" those parts moot. AI art also raises training-data concerns for some contributors | Code: AI-assisted code is reviewed and edited by humans and licensed MIT as normal. Art: AI-created art is welcome (47noodles' call; pipeline in `16` §16.2). Every AI-made asset is labelled in its REUSE metadata and reviewed by a human art lead for style |
| **Other games' names** | Trademarks, not copyright | Descriptive use only ("inspired by…"), with an all-games disclaimer (§9.3 rule 8) |

### Notices we ship

- `LICENSE` (MIT) and `LICENSES/` (MIT, CC-BY-4.0, plus Apache-2.0, BSD-3-Clause and OFL-1.1 as they come in)
- `REUSE.toml` and per-file SPDX headers, with `reuse lint` in CI
- `THIRD_PARTY_NOTICES.md` listing bundled libraries (Jolt, jolt-jni, Cobalt, the vendored Infiniverse, any ported BSD or Apache code) with their copyright lines
