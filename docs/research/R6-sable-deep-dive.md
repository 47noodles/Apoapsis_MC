# R6: Sable and Valkyrien Skies 2, code-level porting measurement

Measured 2026-10-11 for Apoapsis (physics engine for moving block structures, latest Minecraft Java). Facts and measurements only. No effort estimates.

## 0. Scope, method, limits

Clones (shallow, `git clone --depth 1`; never built, run, or unpacked). Location: a temporary local scratch directory (the clones were deleted after measurement, and no code was copied).

| Clone | Branch / commit | Path under src\ |
|---|---|---|
| Sable | main, d501fb3103c3247e20cb2151a99182ad5dd61adb (2026-10-05) | sable\repo |
| VS2 (default) | 1.20.1/main, a79995cdf4fa863c034feb176000a3fca023ab37 (2026-07-22) | vs2\repo |
| VS2 (newest VS2 branch) | 1.21.1/main, f39132148e717d325933b4ce6e9e9fb13d929390 (2026-06-02) | vs2-1.21.1\repo |

Method notes:
- Windows MAX_PATH: the Sable checkout failed on long neoforge paths (`block_breaking_behaviour`). It was fixed with `git -c core.longpaths=true checkout` for that command only; no global git config was changed. `git grep` on the worktree still reports "Filename too long" for some files, so every count was made from the git object store (`git ls-files -s` plus `git cat-file --batch`).
- Mixin extraction: a Python script (run from stdin with `python -I`, nothing written to disk) reads each tracked `.java` blob (and `.kt` for VS2), matches every `@Mixin(...)` annotation including multi-line ones, resolves class literals through explicit imports, and records `targets = "..."` strings. Single-line cross-check: `git grep --cached -h -E '@Mixin\('`, sorted and counted (Appendix B). File totals agree with `git grep -l '@Mixin'` except one Sable file, `common/src/main/java/dev/ryanhcode/sable/mixin/AbstractSableMixinPlugin.java`, which names the annotation without using it (grep count 397, annotation count 396).
- Licences: licence texts are not reproduced. Only titles and short paraphrases are given.
- Web sources (WebSearch and WebFetch) are summaries. Claims resting only on a summary are marked (secondhand). Items not found are UNVERIFIED.
- Raw outputs are saved next to each clone: `sable\targets.txt`, `sable\grep-mixin-lines.txt`, `sable\ls-remote-heads.txt`, `sable\ls-remote-tags.txt`, `vs2\targets.txt`, `vs2\grep-mixin-lines.txt`, `vs2\ls-remote-heads.txt`, `vs2\ls-remote-tags.txt`, `vs2-1.21.1\targets.txt`.

## 1. Sable: identity and source availability

**Identity.** Sable is a library mod that adds "sub-levels": moving, rotating block assemblies with their own physics (repo README.md describes it as an intrusive library mod for sub-levels). Author RyanHCode (`gradle.properties`: `mod_author=RyanHCode`; credits list: Ocelot, Eriksonn, Cyvack, Bee, Kyan, Cake, Rhyguy1). The README links the Create Aeronautics Discord (`discord.gg/createaeronautics`). The Create: Simulated / Aeronautics suite relationship comes only from WebSearch summaries of mod listings (secondhand); the repo itself does not state it. Sable depends on Create (`create_version=6.0.10-280` in `gradle.properties`).

**Source availability: PUBLIC.** No closed-source branch of the terms applies.
- GitHub: https://github.com/ryanhcode/sable. API (2026-10-11): not a fork, not archived, default branch `main`, language Java, topics minecraft and minecraft-mod, created 2026-04-15T07:55:09Z, pushed 2026-10-05T22:28:51Z, stars 494, forks 225, open issues 1042 (GitHub's count includes PRs). The repo creation date is later than the Modrinth publication date (2025-07-23), so the repo was probably moved or recreated. UNVERIFIED (the clone is shallow).
- Fork, not origin: https://github.com/cxykevin/sable (fork=true, 0 stars, created 2026-05-01).
- Modrinth: https://modrinth.com/mod/sable (project id T9PomCSv). `source_url` = the GitHub repo. `license` = `LicenseRef-PolyForm-Shield-License-1.0.0`, linked to LICENSE.md. `game_versions` = `["1.21.1"]`. `loaders` = `["fabric","neoforge"]`. Client and server both required. 28 versions. Newest 2.0.6+mc1.21.1, published 2026-10-03, on both loaders.
- GitHub releases: `mc1.21.1-2.0.6-neoforge` and `mc1.21.1-2.0.6-fabric`, both 2026-10-03 (latest). `mc1.21.1-2.0.5-*`, 2026-08-18.
- Tags: 36, all `mc1.21.1-*` (1.0.3 through 2.0.6, both loaders). Branches: 6 heads, none for another Minecraft version. Full lists in Appendix A.

**Licence.**
- `LICENSE.md` (repo root, 5,835 bytes). Title: "PolyForm Shield License 1.0.0". README.md line 52 says all content is licensed under PolyForm Shield 1.0.0 by RyanHCode unless otherwise stated.
- GitHub API reports the licence as NOASSERTION (not auto-detected).
- Clauses that matter for a mod like Apoapsis (paraphrase, not quoted): any purpose is permitted except providing a product that competes with the software or with a product the licensor or an affiliate provides using it (LICENSE.md, purpose section, about line 33). A later clause stops a licensee who keeps using an older version from competing once the licensor brings out a competing product (about line 41).
- PolyForm Shield is source-available rather than OSI-approved open source (general knowledge, not checked against the OSI list in this session).
- The bundled native Rapier fork carries its own licence file, `sable_rapier/src/main/resources/natives/sable_rapier/LICENSE-RAPIER`, titled "Apache License, Version 2.0, January 2004". The natives README says the modified Rapier is at https://github.com/ryanhcode/rapier.

**VS2 identity and licence (comparison).**
- Repo https://github.com/ValkyrienSkies/Valkyrien-Skies-2, description "Valkyrien Skies 2". Default branch `1.20.1/main`. Not archived. GitHub licence LGPL-3.0. Created 2021-01-22, pushed 2026-10-10T17:14:30Z, stars 405, forks 209, open issues 330.
- `LICENSE` (repo root, 7,817 bytes). Title: "GNU LESSER GENERAL PUBLIC LICENSE Version 3, 29 June 2007". Modrinth (`valkyrien-skies`, id V5ujR2yw): licence id `LGPL-3.0-only`, `source_url` = this repo.
- Older repo `ValkyrienSkies/Valkyrien-Skies` (VS1 era; its GitHub description says it is a custom multi-threaded physics engine for moving structures) is archived, Apache-2.0, last pushed 2023-01-18. Not the target. Search results often conflate the two.

## 2. Measurements

### 2.1 Side-by-side

| Metric | Sable (main) | VS2 (1.20.1/main, default) | VS2 (1.21.1/main) |
|---|---|---|---|
| Minecraft version | 1.21.1 (`gradle.properties` `minecraft_version`, `minecraft_version_range=[1.21.1]`) | 1.20.1 (`gradle.properties`) | 1.21.1 (`gradle.properties`) |
| Loaders | NeoForge 21.1.228 and Fabric loader 0.16.9 (`gradle.properties`) | `enabled_platforms=quilt,fabric,forge`; Forge 1.20.1-47.4.2; Fabric loader 0.17.2 | `enabled_platforms=fabric,neoforge` |
| Mappings | common: NeoForm 1.21.1-20240808.144430 + Parchment 2024.11.10 (`common/build.gradle`); fabric: `officialMojangMappings()` + Parchment (`fabric/build.gradle` line 26-28) | `loom.officialMojangMappings()` (`build.gradle` line 180) + ParchmentMC repo (line 96-97) | not checked |
| Build tooling | `fabric-loom-remap` 1.16-SNAPSHOT (`build.gradle` line 3) | Architectury plugin 3.4.146; `dev.architectury.loom` 1.3.355; Kotlin 2.0.0 (`build.gradle` lines 9-17) | not checked |
| Java | `java_version=21`; `compatibilityLevel` JAVA_21 (`common/src/main/resources/sable.mixins.json`) | Java 17 (README.md, IDE setup section) | not checked |
| Tracked files | 948 | 876 | 890 |
| Java | 783 files, 58,572 lines | 464 files, 29,786 lines | 479 files, 29,071 lines |
| Kotlin | 0 | 173 files, 15,152 lines | 175 files, 14,991 lines |
| Rust | 19 files, 5,854 lines (`sable_rapier/src/main/rust`) | 0 | 0 |
| C / C++ / C# | 0 files (see 2.8 for the declared bepu submodule) | 0 | not measured |
| GLSL / vert / frag | 7 / 3 / 1 files; 612 / 199 / 31 lines | 0 | not measured |
| Bundled native binaries | 1 archive: `sable_rapier/src/main/resources/natives/sable_rapier/sable_rapier_binaries.zip.l4z`, 9,072,872 bytes | none tracked (dll, so, dylib, exe, jar absent; only gradle-wrapper.jar) | none tracked |
| Mixin files (`@Mixin(` present) | 396 (common 228, neoforge 158, fabric 10) | 417 (common 348, forge 42, fabric 27) | 430 |
| Mixin registrations (mixins json) | common `sable.mixins.json`: 143 server + 85 client (fabric and neoforge json not counted) | common 241 + 104 client; fabric 12 + 11; forge 26 + 11 | not measured |
| Access wideners / transformers | `sable.accesswidener` 123 lines (header `accessWidener v2 named`); `META-INF/accesstransformer.cfg` 71 lines | `valkyrienskies-common.accesswidener` 45 lines; no AT/CFG tracked | 37 lines |
| Public API package | `dev.ryanhcode.sable.api` (62 Java files under `api/`; largest subpackages physics 30, sublevel 10, block 10) | `org.valkyrienskies.mod.api` (11 Kotlin files) + `api_impl` (2) | not measured |
| Physics engine | Rapier (Rust, ryanhcode fork), JNI via the Rust `jni` 0.21.1 crate | Krunch (`vs-core`, external Maven artifact); backends `KRUNCH_CLASSIC`, `KRUNCH_JOLT`, `KRUNCH_PHYSX` | same family; `krunch_version=1.0.0+f0bdc63445` |
| Contributors (API, default branch) | 32 | 60 | not measured |
| Commits on default branch since 2026-09-11 (API, first page) | 5 | 0 | not applicable |
| Open PRs (API, first page) | 41 | 32 | not measured |
| Latest release | mc1.21.1-2.0.6 (2026-10-03) | release/2.4.11 (2026-04-10) | none |

### 2.2 Lines of code (object store, all tracked files)

Sable main (948 tracked files): java 783 files / 58,572 lines; rust 19 / 5,854; glsl 7 / 612; vert 3 / 199; frag 1 / 31; json 53 / 1,453; gradle 9 / 985; toml 6 / 147; yml 7 / 213; md 10 / 499; nbt 7 / 725; hexpat 2 / 227.

Sable by module (java / other): common 44,383 java (plus 61 glsl); fabric 855; neoforge 11,220 java, 551 glsl, 199 vert, 31 frag; sable_rapier 2,114 java, 5,854 rs.

VS2 1.20.1/main (876 tracked files): java 464 / 29,786; kotlin 173 / 15,152; json 158 / 9,230; gradle 6 / 1,035; toml 1 / 45. By module: common java 25,593 + kt 13,948; forge java 2,555 + kt 616; fabric java 1,638 + kt 588.

VS2 1.21.1/main (890 tracked files): java 479 / 29,071; kotlin 175 / 14,991; json 9,023 lines; gradle 1,103 lines; toml 51 lines.

### 2.3 Module layout

- Sable (`settings.gradle`): `common`, `fabric`, `neoforge`, `sable_rapier`. Also `buildSrc`, `wiki` (4 markdown files), `.github`, `.jenkins`. Tracked files per module: common 599, neoforge 230, sable_rapier 47, fabric 39.
- VS2 1.20.1 (`settings.gradle`): `common`, `fabric`, `forge`. There is no neoforge module on 1.20.1. A `vs-core` included build is added only if `./vs-core` exists, and it does not exist in this clone. Tracked files: common 735, forge 69, fabric 42.
- VS2 1.21.1: `enabled_platforms=fabric,neoforge`.

### 2.4 Mixins: counts and annotation usage

Annotation counts (occurrences in mixin files):

| Annotation | Sable (396 files) | VS2 1.20.1 (417 files) | VS2 1.21.1 (430 files) |
|---|---|---|---|
| @Inject | 246 | 293 | 314 |
| @Redirect | 168 | 148 | 159 |
| @Overwrite | 33 | 15 | 12 |
| @Shadow | 363 | 369 | 359 |
| @Accessor | 29 | 95 | 99 |
| @Invoker | 10 | 22 | 22 |
| @ModifyArg / @ModifyArgs | 9 | 21 | 24 |
| @ModifyVariable | 11 | 26 | 25 |
| @ModifyConstant | 3 | 4 | 2 |
| @ModifyReturnValue | 17 | 8 | 10 |
| @ModifyExpressionValue | 2 | 12 | 10 |
| @WrapOperation | 100 | 228 | 194 |
| @WrapMethod | 16 | 29 | 26 |
| @WrapWithCondition | 1 | 1 | 1 |
| @Unique | 189 | 223 | 206 |

Mixin registrations: Sable common json lists 143 server and 85 client mixins. VS2 1.20.1 lists 241 common + 104 client, 12 + 11 fabric, 26 + 11 forge.

### 2.5 Vanilla mixin targets, grouped by subsystem

Grouping rules (heuristic, by package and class name; a class can count in more than one file, so file counts overlap across groups):
- networking: `net.minecraft.network.*`, `net.minecraft.server.network.*`, `ClientPacketListener`, `MultiPlayerGameMode`, `ServerEntity`, `ChunkMap.TrackedEntity`.
- lighting: `net.minecraft.world.level.lighting.*`, and `DataLayer`, `LevelLightEngine`, `ThreadedLevelLightEngine`, `LightLayer`, `LightChunk`, any `*LightEngine`.
- rendering: `net.minecraft.client.renderer.*`, `client.model.*`, `client.particle.*`, `client.resources.model.*`, `client.Camera`, `com.mojang.blaze3d.*`.
- worldgen: `world.level.levelgen.*`, `data.worldgen.*`, `world.level.biome.*`, `ChunkGenerator`, `NoiseBasedChunkGenerator`, `WorldGenRegion`, `StructureManager`, `StructureStart`.
- collision / entity movement: `world.phys.*`, `world.level.CollisionGetter`, `world.level.BlockCollisions`, `world.entity.vehicle.*`, `Entity`, `LivingEntity`, `Mob`, `PathfinderMob`, `Player`, `LocalPlayer`, `ServerPlayer`, `RemotePlayer`.
- chunk / world storage: `world.level.chunk.*`, `server.level.*` (other than the movement classes), `world.level.storage.*`, `world.level.entity.*`, `world.level.dimension.*`, `Level`, `LevelReader`, `LevelAccessor`, `ChunkPos`, `BlockGetter`, `WorldGenLevel`, `world.level.border.*`, `client.multiplayer.ClientLevel`, `client.multiplayer.ClientChunkCache`.
- other: block/pathing (`world.level.block.*`, `material.*`, `pathfinder.*`, `redstone.*`, `gameevent.*`); item/inventory (`world.item.*`, `world.inventory.*`); entity non-movement (other `world.entity.*`); client non-render (other `client.*`); core/misc (everything else in vanilla).
- non-vanilla: any class outside `net.minecraft.*` and `com.mojang.*` (section 2.6).

Subsystem summary (mixin files touching the group | unique vanilla targets):

| Subsystem | Sable files | Sable targets | VS2 1.20.1 files | VS2 1.20.1 targets |
|---|---|---|---|---|
| rendering | 56 | 28 | 32 | 17 |
| chunk / world storage | 35 | 12 | 39 | 18 |
| collision / entity movement | 47 | 13 | 32 | 9 |
| networking | 17 | 12 | 9 | 6 |
| lighting | 0 | 0 | 6 | 5 |
| worldgen | 2 | 2 | 4 | 4 |
| other: block / pathing | 22 | 21 | 22 | 21 |
| other: item / inventory | 3 | 3 | 5 | 4 |
| other: entity (non-movement) | 16 | 16 | 27 | 26 |
| other: client (non-render) | 11 | 9 | 10 | 9 |
| other: core / misc | 17 | 15 | 12 | 11 |
| non-vanilla (third-party) | 171 | 162 | 219 | 213 |

Totals: Sable 396 mixin files, 293 unique target types, 131 vanilla. VS2 1.20.1: 417 files, 343 unique, 130 vanilla. VS2 1.21.1: 430 files, 350 unique, 131 vanilla (diff in 2.5.4).

Full lists (generated from the parse; these are the vanilla classes mixins target, with the number of mixin files per class):

2.5.1 Sable (`sable\targets.txt`):

```
MIXIN FILES: 396

UNIQUE TARGET TYPES (incl. non-vanilla): 293
VANILLA UNIQUE TARGETS: 131

== SUBSYSTEM SUMMARY (mixin FILES touching the subsystem | unique targets)
rendering                    files=  56 targets=  28
chunk/world-storage          files=  35 targets=  12
collision/entity-movement    files=  47 targets=  13
networking                   files=  17 targets=  12
lighting                     files=   0 targets=   0
worldgen                     files=   2 targets=   2
other:block/pathing          files=  22 targets=  21
other:item/inventory         files=   3 targets=   3
other:entity(non-movement)   files=  16 targets=  16
other:client(non-render)     files=  11 targets=   9
other:core/misc              files=  17 targets=  15
non-vanilla (3rd-party)      files= 171 targets= 162

== VANILLA TARGETS BY SUBSYSTEM (FQN  [mixin files])
--- rendering
  com.mojang.blaze3d.audio.Channel  [1]
  com.mojang.blaze3d.shaders.Program  [1]
  net.minecraft.client.Camera  [6]
  net.minecraft.client.CameraType  [1]
  net.minecraft.client.particle.BlockMarker  [1]
  net.minecraft.client.particle.FlameParticle  [1]
  net.minecraft.client.particle.Particle  [1]
  net.minecraft.client.particle.ParticleEngine  [1]
  net.minecraft.client.particle.SuspendedParticle  [1]
  net.minecraft.client.particle.TerrainParticle  [1]
  net.minecraft.client.renderer.FogRenderer  [1]
  net.minecraft.client.renderer.GameRenderer  [5]
  net.minecraft.client.renderer.LevelRenderer  [14]
  net.minecraft.client.renderer.ViewArea  [1]
  net.minecraft.client.renderer.block.ModelBlockRenderer  [1]
  net.minecraft.client.renderer.block.ModelBlockRenderer.AmbientOcclusionFace  [1]
  net.minecraft.client.renderer.block.ModelBlockRenderer.Cache  [1]
  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher  [1]
  net.minecraft.client.renderer.blockentity.ConduitRenderer  [1]
  net.minecraft.client.renderer.chunk.RenderChunkRegion  [1]
  net.minecraft.client.renderer.chunk.SectionCompiler  [2]
  net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection  [2]
  net.minecraft.client.renderer.debug.ChunkBorderRenderer  [1]
  net.minecraft.client.renderer.debug.DebugRenderer  [1]
  net.minecraft.client.renderer.entity.EntityRenderDispatcher  [3]
  net.minecraft.client.renderer.entity.EntityRenderer  [3]
  net.minecraft.client.renderer.entity.LivingEntityRenderer  [1]
  net.minecraft.client.renderer.item.CompassItemPropertyFunction  [1]
--- chunk/world-storage
  net.minecraft.client.multiplayer.ClientChunkCache  [1]
  net.minecraft.client.multiplayer.ClientChunkCache.Storage  [1]
  net.minecraft.client.multiplayer.ClientLevel  [6]
  net.minecraft.server.level.ChunkMap  [3]
  net.minecraft.server.level.ServerChunkCache  [1]
  net.minecraft.server.level.ServerLevel  [11]
  net.minecraft.world.level.BlockGetter  [1]
  net.minecraft.world.level.Level  [4]
  net.minecraft.world.level.border.WorldBorder  [1]
  net.minecraft.world.level.chunk.LevelChunk  [4]
  net.minecraft.world.level.chunk.LevelChunkSection  [1]
  net.minecraft.world.level.entity.PersistentEntitySectionManager  [1]
--- collision/entity-movement
  net.minecraft.client.player.LocalPlayer  [4]
  net.minecraft.client.player.RemotePlayer  [1]
  net.minecraft.server.level.ServerPlayer  [7]
  net.minecraft.world.entity.Entity  [15]
  net.minecraft.world.entity.LivingEntity  [8]
  net.minecraft.world.entity.Mob  [1]
  net.minecraft.world.entity.player.Player  [5]
  net.minecraft.world.entity.vehicle.AbstractMinecart  [1]
  net.minecraft.world.phys.HitResult  [1]
  net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape  [1]
  net.minecraft.world.phys.shapes.CollisionContext  [1]
  net.minecraft.world.phys.shapes.DiscreteVoxelShape  [1]
  net.minecraft.world.phys.shapes.VoxelShape  [1]
--- networking
  net.minecraft.client.multiplayer.ClientPacketListener  [4]
  net.minecraft.client.multiplayer.MultiPlayerGameMode  [1]
  net.minecraft.network.Connection  [1]
  net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket  [1]
  net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos  [1]
  net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot  [1]
  net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket  [1]
  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket  [1]
  net.minecraft.server.level.ChunkMap.TrackedEntity  [1]
  net.minecraft.server.level.ServerEntity  [2]
  net.minecraft.server.network.ServerConnectionListener  [1]
  net.minecraft.server.network.ServerGamePacketListenerImpl  [2]
--- lighting
--- worldgen
  net.minecraft.world.level.biome.Biome  [1]
  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate  [1]
--- other:block/pathing
  net.minecraft.world.level.block.BeehiveBlock  [1]
  net.minecraft.world.level.block.BellBlock  [1]
  net.minecraft.world.level.block.Block  [2]
  net.minecraft.world.level.block.NetherPortalBlock  [1]
  net.minecraft.world.level.block.TntBlock  [1]
  net.minecraft.world.level.block.VineBlock  [1]
  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity  [1]
  net.minecraft.world.level.block.entity.ConduitBlockEntity  [1]
  net.minecraft.world.level.block.entity.EnchantingTableBlockEntity  [1]
  net.minecraft.world.level.block.entity.SignBlockEntity  [1]
  net.minecraft.world.level.block.state.BlockState  [1]
  net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry  [1]
  net.minecraft.world.level.gameevent.GameEventDispatcher  [1]
  net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener  [1]
  net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Ticker  [1]
  net.minecraft.world.level.material.FlowingFluid  [1]
  net.minecraft.world.level.material.WaterFluid  [1]
  net.minecraft.world.level.pathfinder.FlyNodeEvaluator  [1]
  net.minecraft.world.level.pathfinder.Path  [1]
  net.minecraft.world.level.pathfinder.PathfindingContext  [1]
  net.minecraft.world.level.pathfinder.WalkNodeEvaluator  [1]
--- other:item/inventory
  net.minecraft.world.item.Item  [1]
  net.minecraft.world.item.context.BlockPlaceContext  [1]
  net.minecraft.world.item.context.UseOnContext  [1]
--- other:entity(non-movement)
  net.minecraft.world.entity.EntityType  [1]
  net.minecraft.world.entity.Leashable  [1]
  net.minecraft.world.entity.TamableAnimal  [1]
  net.minecraft.world.entity.ai.goal.EatBlockGoal  [1]
  net.minecraft.world.entity.ai.goal.FleeSunGoal  [1]
  net.minecraft.world.entity.ai.navigation.GroundPathNavigation  [2]
  net.minecraft.world.entity.ai.navigation.PathNavigation  [1]
  net.minecraft.world.entity.ai.util.RandomPos  [1]
  net.minecraft.world.entity.animal.Parrot  [1]
  net.minecraft.world.entity.item.FallingBlockEntity  [1]
  net.minecraft.world.entity.item.ItemEntity  [1]
  net.minecraft.world.entity.item.PrimedTnt  [1]
  net.minecraft.world.entity.projectile.AbstractArrow  [1]
  net.minecraft.world.entity.projectile.Projectile  [1]
  net.minecraft.world.entity.projectile.ProjectileUtil  [1]
  net.minecraft.world.entity.projectile.ThrownTrident  [1]
--- other:client(non-render)
  net.minecraft.client.Minecraft  [3]
  net.minecraft.client.MouseHandler  [1]
  net.minecraft.client.gui.Gui  [1]
  net.minecraft.client.gui.components.DebugScreenOverlay  [1]
  net.minecraft.client.gui.screens.options.OptionsScreen  [1]
  net.minecraft.client.multiplayer.ClientSuggestionProvider  [1]
  net.minecraft.client.resources.sounds.AbstractSoundInstance  [1]
  net.minecraft.client.server.IntegratedServer  [1]
  net.minecraft.client.sounds.SoundEngine  [1]
--- other:core/misc
  net.minecraft.commands.synchronization.ArgumentTypeInfos  [1]
  net.minecraft.core.dispenser.ProjectileDispenseBehavior  [1]
  net.minecraft.gametest.framework.GameTestInfo  [1]
  net.minecraft.gametest.framework.StructureUtils  [1]
  net.minecraft.gametest.framework.TestCommand  [1]
  net.minecraft.server.MinecraftServer  [1]
  net.minecraft.server.commands.ExecuteCommand  [1]
  net.minecraft.server.commands.data.DataCommands  [1]
  net.minecraft.server.players.PlayerList  [3]
  net.minecraft.world.damagesource.CombatTracker  [1]
  net.minecraft.world.level.BlockAndTintGetter  [1]
  net.minecraft.world.level.ClipContext  [1]
  net.minecraft.world.level.EntityGetter  [1]
  net.minecraft.world.level.Explosion  [1]
  net.minecraft.world.ticks.LevelChunkTicks  [1]

== NON-VANILLA TARGET PACKAGES (3-segment key)
--- UNRESOLVED: ServerChunkCache[1], TrackGraphVisualizer[1]
--- com.github.exopandora: Perspective[1], EntityHelper[1], ObjectPicker[1], ShoulderSurfingCamera[1], ShoulderSurfingImpl[1]
--- com.simibubi.create: AllBlockEntityTypes[1], AbstractContraptionEntity[1], Contraption[1], ContraptionCollider[1], ContraptionHandlerClient[1], ContraptionControlsRenderer[1], HarvesterBlock[1], HarvesterBlockEntity[1], HarvesterMovementBehaviour[2], HarvesterRenderer[1], SeatBlock[1], SailBlock[1], StickerBlock[1], StickerBlockEntity[1], ElevatorControlsHandler[1], SuperGlueEntity[1], SuperGlueRemovalPacket[1], SuperGlueSelectionHandler[1], ContraptionVisual[1], AbstractBellBlock[2], BlueprintEntity[1], EntityHats[1], SymmetryHandler[1], ToolboxHandler[1], ToolboxHandlerClient[1], ZapperItem[1], OpenEndedPipe[1], PipeConnection[1], HosePulleyBlockEntity[1], HosePulleyFluidHandler[1], BoilerData[1], BlockBreakingKineticBlockEntity[1], BlockBreakingMovementBehaviour[1], BeltBlock[2], BeltBlockEntity[1], BeltRenderer[2], BeltMovementHandler[1], ChainConveyorBlock[1], ChainConveyorBlockEntity[2], ChainConveyorInteractionHandler[1], ChainConveyorRenderer[1], ChainConveyorRidingHandler[1], ChainConveyorShape[1], ChainPackageInteractionHandler[1], ChainPackageInteractionPacket[1], CrushingWheelBlock[1], CrushingWheelControllerBlockEntity[1], DeployerBlockEntity[1], DrillBlock[1], AirCurrent[1], AirFlowParticle[1], EncasedFanBlockEntity[1], NozzleBlockEntity[4], FanProcessingType[1], FlywheelBlockEntity[1], VerticalGearboxItem[1], ArmBlockEntity[1], ArmInteractionPointHandler[1], MillstoneBlock[1], SawBlock[2], SawBlockEntity[1], SawMovementBehaviour[1], TurntableBlock[1], ChuteBlockEntity[1], DepotRenderer[1], EjectorBlock[1], EjectorBlockEntity[1], FactoryPanelConnectionHandler[1], FunnelBlock[1], PackagePortPlacementPacket[1], ChainConveyorFrogportTarget[1], PackagePortTargetSelectionHandler[1], FrogportBlock[1], FrogportBlockEntity[1], FrogportRenderer[1], FrogportVisual[1], StockTickerInteractionHandler[1], BasinBlock[1], BasinBlockEntity[1], BasinOperatingBlockEntity[1], BlazeBurnerBlockEntity[1], RedstoneContactBlock[1], ClickToLinkBlockItem[1], DisplayLinkBlock[1], DisplayLinkBlockEntity[1], LinkRenderer[1], RedstoneLinkNetworkHandler[1], LecternControllerBlock[1], LecternControllerBlockEntity[1], SchematicExport[1], SchematicPrinter[1], SchematicAndQuillHandler[1], SchematicHandler[1], SchematicRenderer[1], SchematicTransformation[1], DeployTool[1], SchematicToolBase[1], SchematicPlacePacket[1], CarriageContraptionVisual[1], CurvedTrackDestroyPacket[1], TrackBlock[1], TrackBlockItem[1], TrackBlockOutline[1], TrackPlacement[1], TrackTargetingBlockItem[1], TrackTargetingClient[1], BigOutlines[1], BlockEntityBehaviour[1], ValueBox[1], FilteringRenderer[1], CapManipulationBehaviourBase[1], SafeBlockEntityRenderer[1], SmartBlockEntityRenderer[1], Matrix3d[1], AirParticle[1], RaycastHelper[1]
--- com.spydnel.backpacks: BackpackPickupEvents[1]
--- dan200.computercraft.shared: WirelessNetwork[1]
--- dev.engine_room.flywheel: EngineImpl[1], LightStorage[1], EmbeddedEnvironment[1], EnvironmentStorage[1], MatrixBuffer[1], ShaderSources[1], VisualManagerImpl[1], VisualizationEventHandler[1], RenderDispatcherImpl[1], BlockEntityStorage[1], AbstractBlockEntityVisual[1]
--- dev.protomanly.pmweather: PMWeather[1], AnemometerBlock[1], AnemometerBlockEntity[1], RadarRenderer[1]
--- dev.ryanhcode.sable: MovingSoundInstanceDelegate[2]
--- gg.moonflower.etched: StopListeningSound[1]
--- io.github.mortuusars: CameraPoses[1], CameraStandEntity[1]
--- net.caffeinemc.mods: SodiumWorldRenderer[1]
--- net.createmod.catnip: TransparentGhostBlockRenderer[1], SchematicLevel[1], AABBOutline[1], BlockClusterOutline[1], ChasingAABBOutline[2], LineOutline[1], Outline[1], PlacementClient[1]
--- net.fabricmc.fabric: ClientPlayNetworkAddon[1]
--- net.irisshaders.iris: ExtendedShader[1]
--- net.mehvahdjukaar.moonlight: LOD[1]
--- net.mehvahdjukaar.vista: ViewFinderController[1], Block[1]
--- snownee.jade.addon: CreatePlugin[1]
--- snownee.jade.impl: BlockAccessorImpl[1]
--- snownee.jade.overlay: RayTracing[1]
--- toni.sodiumextras.EmbyTools: EmbyTools[2]

== UNRESOLVED (simple name with no import)
ServerChunkCache ['common/src/main/java/dev/ryanhcode/sable/mixin/plot/ServerChunkCacheMixin.java']
TrackGraphVisualizer ['neoforge/src/main/java/dev/ryanhcode/sable/neoforge/mixin/compatibility/create/tracks/TrackGraphVisualizerMixin.java']
```

2.5.2 VS2 1.20.1/main (`vs2\targets.txt`):

```
MIXIN FILES: 417

UNIQUE TARGET TYPES (incl. non-vanilla): 343
VANILLA UNIQUE TARGETS: 130

== SUBSYSTEM SUMMARY (mixin FILES touching the subsystem | unique targets)
rendering                    files=  32 targets=  17
chunk/world-storage          files=  39 targets=  18
collision/entity-movement    files=  32 targets=   9
networking                   files=   9 targets=   6
lighting                     files=   6 targets=   5
worldgen                     files=   4 targets=   4
other:block/pathing          files=  22 targets=  21
other:item/inventory         files=   5 targets=   4
other:entity(non-movement)   files=  27 targets=  26
other:client(non-render)     files=  10 targets=   9
other:core/misc              files=  12 targets=  11
non-vanilla (3rd-party)      files= 219 targets= 213

== VANILLA TARGETS BY SUBSYSTEM (FQN  [mixin files])
--- rendering
  com.mojang.blaze3d.audio.Channel  [1]
  com.mojang.blaze3d.audio.Listener  [1]
  com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator  [1]
  net.minecraft.client.Camera  [2]
  net.minecraft.client.particle.Particle  [1]
  net.minecraft.client.renderer.GameRenderer  [1]
  net.minecraft.client.renderer.LevelRenderer  [9]
  net.minecraft.client.renderer.LevelRenderer.RenderChunkInfo  [2]
  net.minecraft.client.renderer.ViewArea  [3]
  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher  [1]
  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk  [2]
  net.minecraft.client.renderer.culling.Frustum  [1]
  net.minecraft.client.renderer.debug.DebugRenderer  [2]
  net.minecraft.client.renderer.entity.EntityRenderDispatcher  [1]
  net.minecraft.client.renderer.entity.EntityRenderer  [2]
  net.minecraft.client.renderer.entity.LivingEntityRenderer  [1]
  net.minecraft.client.renderer.entity.MobRenderer  [1]
--- chunk/world-storage
  net.minecraft.client.multiplayer.ClientChunkCache  [2]
  net.minecraft.client.multiplayer.ClientChunkCache.Storage  [2]
  net.minecraft.client.multiplayer.ClientLevel  [4]
  net.minecraft.server.level.ChunkHolder  [2]
  net.minecraft.server.level.ChunkMap  [6]
  net.minecraft.server.level.DistanceManager  [1]
  net.minecraft.server.level.ServerChunkCache  [2]
  net.minecraft.server.level.ServerLevel  [3]
  net.minecraft.server.level.ServerLevel.EntityCallbacks  [1]
  net.minecraft.server.level.ServerPlayerGameMode  [1]
  net.minecraft.world.level.Level  [4]
  net.minecraft.world.level.border.WorldBorder  [1]
  net.minecraft.world.level.chunk.ChunkStatus  [1]
  net.minecraft.world.level.chunk.LevelChunk  [4]
  net.minecraft.world.level.entity.EntitySection  [1]
  net.minecraft.world.level.entity.EntitySectionStorage  [1]
  net.minecraft.world.level.entity.PersistentEntitySectionManager  [3]
  net.minecraft.world.level.entity.TransientEntitySectionManager  [1]
--- collision/entity-movement
  net.minecraft.client.player.LocalPlayer  [2]
  net.minecraft.server.level.ServerPlayer  [4]
  net.minecraft.world.entity.Entity  [10]
  net.minecraft.world.entity.LivingEntity  [4]
  net.minecraft.world.entity.Mob  [1]
  net.minecraft.world.entity.PathfinderMob  [1]
  net.minecraft.world.entity.player.Player  [7]
  net.minecraft.world.entity.vehicle.ContainerEntity  [2]
  net.minecraft.world.level.BlockCollisions  [1]
--- networking
  net.minecraft.client.multiplayer.ClientPacketListener  [2]
  net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket  [1]
  net.minecraft.network.protocol.game.DebugPackets  [1]
  net.minecraft.server.level.ChunkMap.TrackedEntity  [1]
  net.minecraft.server.level.ServerEntity  [1]
  net.minecraft.server.network.ServerGamePacketListenerImpl  [3]
--- lighting
  net.minecraft.server.level.ThreadedLevelLightEngine  [1]
  net.minecraft.world.level.lighting.LayerLightSectionStorage  [2]
  net.minecraft.world.level.lighting.LevelLightEngine  [1]
  net.minecraft.world.level.lighting.LightEngine  [1]
  net.minecraft.world.level.lighting.SkyLightSectionStorage  [1]
--- worldgen
  net.minecraft.world.level.biome.Biome  [1]
  net.minecraft.world.level.chunk.ChunkGenerator  [1]
  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate  [1]
  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette  [1]
--- other:block/pathing
  net.minecraft.world.level.block.CarvedPumpkinBlock  [1]
  net.minecraft.world.level.block.FireBlock  [2]
  net.minecraft.world.level.block.IceBlock  [1]
  net.minecraft.world.level.block.SlabBlock  [1]
  net.minecraft.world.level.block.SnowLayerBlock  [1]
  net.minecraft.world.level.block.StairBlock  [1]
  net.minecraft.world.level.block.VineBlock  [1]
  net.minecraft.world.level.block.entity.BlockEntity  [1]
  net.minecraft.world.level.block.entity.ConduitBlockEntity  [1]
  net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity  [1]
  net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry  [1]
  net.minecraft.world.level.gameevent.GameEventDispatcher  [1]
  net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener  [2]
  net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Ticker  [1]
  net.minecraft.world.level.material.FlowingFluid  [1]
  net.minecraft.world.level.material.LavaFluid  [1]
  net.minecraft.world.level.material.WaterFluid  [1]
  net.minecraft.world.level.pathfinder.Path  [1]
  net.minecraft.world.level.pathfinder.PathFinder  [1]
  net.minecraft.world.level.pathfinder.SwimNodeEvaluator  [1]
  net.minecraft.world.level.pathfinder.WalkNodeEvaluator  [1]
--- other:item/inventory
  net.minecraft.world.inventory.AbstractContainerMenu  [1]
  net.minecraft.world.inventory.EnchantmentMenu  [1]
  net.minecraft.world.item.BlockItem  [2]
  net.minecraft.world.item.context.BlockPlaceContext  [1]
--- other:entity(non-movement)
  net.minecraft.world.entity.LightningBolt  [1]
  net.minecraft.world.entity.ai.behavior.AcquirePoi.JitteredLinearRetry  [1]
  net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite  [1]
  net.minecraft.world.entity.ai.behavior.GoToClosestVillage  [1]
  net.minecraft.world.entity.ai.behavior.MoveToTargetSink  [1]
  net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget  [1]
  net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi  [1]
  net.minecraft.world.entity.ai.behavior.WorkAtPoi  [1]
  net.minecraft.world.entity.ai.control.MoveControl  [1]
  net.minecraft.world.entity.ai.goal.MoveToBlockGoal  [1]
  net.minecraft.world.entity.ai.navigation.PathNavigation  [1]
  net.minecraft.world.entity.ai.util.AirAndWaterRandomPos  [1]
  net.minecraft.world.entity.ai.util.DefaultRandomPos  [1]
  net.minecraft.world.entity.ai.util.GoalUtils  [1]
  net.minecraft.world.entity.ai.util.LandRandomPos  [1]
  net.minecraft.world.entity.ai.village.VillageSiege  [1]
  net.minecraft.world.entity.ai.village.poi.PoiManager  [1]
  net.minecraft.world.entity.animal.Bee  [1]
  net.minecraft.world.entity.animal.Bee.BeeEnterHiveGoal  [1]
  net.minecraft.world.entity.animal.Bee.BeeGrowCropGoal  [1]
  net.minecraft.world.entity.animal.Bee.BeeLocateHiveGoal  [1]
  net.minecraft.world.entity.animal.Bee.BeePollinateGoal  [1]
  net.minecraft.world.entity.projectile.Projectile  [2]
  net.minecraft.world.entity.projectile.ProjectileUtil  [1]
  net.minecraft.world.entity.raid.Raid  [1]
  net.minecraft.world.entity.raid.Raids  [1]
--- other:client(non-render)
  net.minecraft.client.KeyMapping  [2]
  net.minecraft.client.Minecraft  [1]
  net.minecraft.client.gui.Gui  [1]
  net.minecraft.client.gui.components.DebugScreenOverlay  [1]
  net.minecraft.client.gui.screens.ConnectScreen  [1]
  net.minecraft.client.multiplayer.ClientSuggestionProvider  [1]
  net.minecraft.client.resources.sounds.AbstractTickableSoundInstance  [1]
  net.minecraft.client.resources.sounds.SimpleSoundInstance  [1]
  net.minecraft.client.sounds.SoundEngine  [1]
--- other:core/misc
  net.minecraft.commands.Commands  [1]
  net.minecraft.commands.arguments.selector.EntitySelectorParser  [1]
  net.minecraft.commands.synchronization.ArgumentTypeInfos  [1]
  net.minecraft.realms.RealmsConnect  [1]
  net.minecraft.resources.ResourceKey  [1]
  net.minecraft.server.MinecraftServer  [2]
  net.minecraft.server.players.PlayerList  [1]
  net.minecraft.world.level.ClipContext  [1]
  net.minecraft.world.level.Explosion  [1]
  net.minecraft.world.level.NaturalSpawner  [1]
  net.minecraft.world.level.PathNavigationRegion  [1]

== NON-VANILLA TARGET PACKAGES (3-segment key)
--- UNRESOLVED: UNRESOLVED:LevelRenderer[1], UNRESOLVED:ViewArea[1], LesserSentinelRenderer[1], PedestalBlockEntity[1], ContraptionRenderInfo[1]
--- at.petrak.hexcasting: HexAdditionalRenderers[1], OpEntityRaycast[1], OpTeleport[1], Spell[1]
--- blusunrize.immersiveengineering.common: BlockEntityInventory[1]
--- cofh.core.block: TileCoFH[1]
--- com.github.legoatoom: ChainKnotEntityRenderer[1], ChainCollisionEntity[1], Chainable[1]
--- com.jamieswhiteshirt.reachentityattributes: ReachEntityAttributes[1]
--- com.lilypuree.connectiblechains: ChainLink[1], ChainKnotEntityRenderer[1], ChainCollisionEntity[1], ChainKnotEntity[1]
--- com.lumintorious.tfcambiental: EnvironmentalTemperatureProvider[1]
--- com.simibubi.create: AbstractContraptionEntity[2], Contraption[1], ContraptionCollider[1], ContraptionHandlerClient[2], ControlledContraptionEntity[3], ContraptionControlsRenderer[1], SeatBlock[1], SeatEntity[1], ControlsInputPacket[1], MechanicalBearingBlockEntity[1], PlacementHelper[1], StickerBlock[1], ElevatorControlsHandler[1], SuperGlueRemovalPacket[1], SuperGlueSelectionHandler[1], SuperGlueSelectionHandler[2], ContraptionEntityRenderer[1], ContraptionMatrices[1], ContraptionVisual[1], GirderPlacementHelper[1], ClipboardScreen[1], SymmetryHandler[1], SymmetryMirror[1], ToolboxHandler[1], ToolboxHandlerClient[1], ItemDrainBlock[1], HosePulleyBlockEntity[1], BlockBreakingKineticBlockEntity[2], BlockBreakingMovementBehaviour[1], BeltMovementHandler[1], ChainConveyorBlockEntity[3], ChainConveyorConnectionHandler[1], ChainConveyorInteractionHandler[1], ChainConveyorRenderer[1], ChainConveyorRidingHandler[1], ChainConveyorVisual[1], ChainPackageInteractionHandler[1], CrushingWheelBlock[1], CrushingWheelControllerBlockEntity[1], DeployerBlockEntity[1], DeployerHandler[1], AirCurrent[1], AirFlowParticle[1], EncasedFanBlockEntity[1], NozzleBlockEntity[1], MillstoneBlock[1], SawBlockEntity[1], SawMovementBehaviour[1], DiagonalCogHelper[1], IntegratedLargeCogHelper[1], IntegratedSmallCogHelper[1], LargeCogHelper[1], SmallCogHelper[1], TurntableBlock[1], TurntableHandler[1], PackageEntity[1], PackageItem[1], AbstractChuteBlock[1], ChuteBlockEntity[1], EjectorBlock[1], EjectorBlockEntity[1], EntityLauncher[1], SharedDepotBlockMethods[1], FactoryPanelConnectionHandler[1], FunnelBlock[1], PackagePortPlacementPacket[1], PackagePortTargetSelectionHandler[1], FrogportBlockEntity[1], FrogportRenderer[1], FrogportVisual[1], StockKeeperCategoryMenu[1], BasinBlock[1], BlazeBurnerBlockEntity[1], RedstoneContactBlock[1], LinkBehaviour[1], LinkRenderer[1], RedstoneLinkNetworkHandler[1], LaunchedItem[1], SchematicannonBlockEntity[1], SchematicannonRenderer[1], SchematicTransformation[1], DeployTool[1], SchematicToolBase[1], PlacementHelper[1], CarriageContraptionEntity[1], CarriageSounds[1], TrainRelocationPacket[1], TrainRelocator[1], TrackNodeLocation[1], CurvedTrackDestroyPacket[1], ITrackBlock[1], TrackBlockItem[1], TrackBlockOutline[1], TrackPlacement[1], TrackTargetingClient[1], TrackVisual[1], BezierTrackVisual[1], ValueBox[1], EdgeInteractionHandler[1], EdgeInteractionRenderer[1], FilteringBehaviour[1], FilteringRenderer[1], SidedFilteringBehaviour[1], ScrollValueBehaviour[1], SafeBlockEntityRenderer[1], SmartBlockEntityRenderer[1], Matrix3d[1], BlockEntityConfigurationPacket[1], PoleHelper[1], BlockEntityRenderHelper[3], SoundScapes[1]
--- com.sonicether.soundphysics: SoundPhysics[1]
--- dan200.computercraft.client: CustomLecternRenderer[1], SpeakerSound[2]
--- dan200.computercraft.shared: WirelessModemPeripheral[2], WirelessNetwork[1], SpeakerPosition[1], TurtleBrain[1], TurtleMoveCommand[1]
--- de.bluecolored.bluemap: BmMap[1], HiresModelManager[1], TileModel[1], MCAWorld[1]
--- dev.engine_room.flywheel: VisualizerRegistry[1], AbstractBlockEntityVisual[1], EntityVisibilityTester[1]
--- dev.ftb.mods: ClaimedChunkManagerImpl[1]
--- dev.imabad.theatrical: BaseLightBlockEntity[1], 1[1], MovingLightRenderer[1], 1[1], LightManager[1]
--- dev.su5ed.mffs: ClientPacketHandler[1], FrequencyGrid[1]
--- fuzs.echochest.world: EchoChestBlockEntity[1], EchoChestListener[1]
--- io.github.fabricators_of_create: CullingBlockEntityIterator[1]
--- li.cil.oc2: ProjectorDepthRenderer[1], ComputerRenderer[1], MonitorRenderer[1]
--- li.cil.tis3d: RenderContextImpl[1], CasingBlockEntityRenderer[1], InfraredPacketEntity[1]
--- me.desht.modularrouters: RouterMenu[1]
--- me.duquee.createutilities: VoidLinkBehaviour[1]
--- me.jellysquid.mods: SodiumOptionsGUI[1], SodiumWorldRenderer[3], RenderSection[1], RenderSectionManager[2], ChunkTracker[1]
--- mekanism.client.render: RenderResizableCuboid[1], Builder[1]
--- mekanism.common.lib: RadiationManager[1]
--- mekanism.common.tile: TileEntityMultiblock[1]
--- net.beholderface.ephemera: OpNetworkTeleport[1]
--- net.createmod.catnip: GhostBlockParams[1], DefaultGhostBlockRenderer[1], TransparentGhostBlockRenderer[1], PlacementSimulationServerLevel[1], AABBOutline[1], BlockClusterOutline[1], Outline[1], OutlineParams[1], PlacementClient[1]
--- net.mcreator.createstuffadditions: GrapplinWhiskChainsLineProcedure[1], GrapplinWhiskItemInHandTickProcedure[1]
--- net.mehvahdjukaar.ml_classes: IOneUserInteractable[1], LOD[1]
--- net.mehvahdjukaar.moonlight: FakeServerLevel[1]
--- net.mehvahdjukaar.vista: ViewFinderController[1], LevelRendererCameraState[1], VistaLevelRenderer[1], Block[1]
--- net.minecraftforge.common: IForgePlayer[3]
--- org.cyclops.integrateddynamics: VoxelShapeComponents[1]
--- org.joml.Matrix4f: Matrix4f[1]
--- qouteall.imm_ptl.core: NewChunkTrackingGraph[1], MyBuiltChunkStorage[1], VisibleSectionDiscovery[1]
--- ram.talia.hexal: ServerLinkableHolder[1], RenderHelperKt[1], OpLink[1], OpLinkOthers[1]
--- rbasamoyai.createbigcannons.cannon_control: MountedAutocannonContraption[1], MountedBigCannonContraption[1], PitchOrientedContraptionEntity[1], FixedCannonMountScrollValueBehaviour[1]
--- rbasamoyai.createbigcannons.munitions: AbstractCannonProjectile[1]
--- rbasamoyai.createbigcannons.network: ServerboundSetFixedCannonMountValuePacket[1]
--- rbasamoyai.createbigcannons.utils: CBCUtils[1]
--- rbasamoyai.ritchiesprojectilelib.RitchiesProjectileLib: RitchiesProjectileLib[1]
--- traben.entity_model_features.models: EMFAnimationEntityContext[2]
--- twilightforest.world.components: ChunkGeneratorTwilight[1]

== UNRESOLVED (simple name with no import)
LevelRenderer ['common/src/main/java/org/valkyrienskies/mod/mixin/mod_compat/optifine/MixinLevelRendererOptifine.java']
ViewArea ['common/src/main/java/org/valkyrienskies/mod/mixin/mod_compat/optifine/MixinViewAreaOptifine.java']
LesserSentinelRenderer ['fabric/src/main/java/org/valkyrienskies/mod/fabric/mixin/compat/hexcasting/hexical/MixinLesserSentinelRenderer.java']
PedestalBlockEntity ['fabric/src/main/java/org/valkyrienskies/mod/fabric/mixin/compat/hexcasting/hexical/MixinPedestalBlockEntity.java']
ContraptionRenderInfo ['forge/src/main/java/org/valkyrienskies/mod/forge/mixin/compat/create/client/MixinContraptionRenderInfo.java']
```

2.5.3 Parse notes: Sable `ServerChunkCache` (file `common/src/main/java/dev/ryanhcode/sable/mixin/plot/ServerChunkCacheMixin.java`) and `TrackGraphVisualizer` (file `neoforge/src/main/java/dev/ryanhcode/sable/neoforge/mixin/compatibility/create/tracks/TrackGraphVisualizerMixin.java`) have no explicit import and were not resolved. `ServerChunkCache` is already counted as `net.minecraft.server.level.ServerChunkCache`. `TrackGraphVisualizer` is a Create class; its full name is UNVERIFIED. In VS2, `LevelRenderer` and `ViewArea` were resolved through a single vanilla wildcard import in one file; a Kotlin `1` token from `dev.imabad.theatrical` is a parser artifact.

2.5.4 VS2 1.21.1/main vs 1.20.1/main (vanilla target sets):
- Only in 1.21.1: `net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection`, `net.minecraft.network.Connection`, `net.minecraft.server.level.GenerationChunkHolder`, `net.minecraft.world.level.BlockGetter`, `net.minecraft.world.level.chunk.status.ChunkStatusTasks`, `net.minecraft.world.level.levelgen.FlatLevelSource`, `net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator`.
- Only in 1.20.1: `net.minecraft.client.renderer.entity.LivingEntityRenderer`, `net.minecraft.client.renderer.entity.MobRenderer`, `net.minecraft.server.level.ServerPlayerGameMode`, `net.minecraft.world.entity.LightningBolt`, `net.minecraft.world.level.block.CarvedPumpkinBlock`, `net.minecraft.world.level.chunk.ChunkStatus`.
- Full 1.21.1 vanilla list: Appendix C.

### 2.6 Third-party (non-vanilla) mixin targets

- Sable: 162 unique targets in 171 files. Top packages: `com.simibubi.create` 116, `dev.engine_room.flywheel` 11, `net.createmod.catnip` 8, `com.github.exopandora` 5, `dev.protomanly.pmweather` 4, `net.mehvahdjukaar.vista` 2, `io.github.mortuusars` 2, plus single targets in Jade, Iris, Sodium, Moonlight, Etched, CC: Tweaked, Backpacks.
- VS2 1.20.1: 213 unique targets in 219 files. Top packages: `com.simibubi.create` 111, `net.createmod.catnip` 9, `me.jellysquid.mods` (Sodium) 5, `dev.imabad.theatrical` 5 (includes the parser artifact), `dan200.computercraft.shared` 5, plus about 40 further mod packages (Hexcasting, Mekanism, Immersive Engineering, Bluemap, Create Big Cannons, Connectible Chains, others).
- Full list of non-vanilla targets: `sable\targets.txt` and `vs2\targets.txt`.

### 2.7 Access wideners and transformers

- Sable: `common/src/main/resources/sable.accesswidener` (123 lines, header `accessWidener v2 named`) and `common/src/main/resources/META-INF/accesstransformer.cfg` (71 lines). The AW makes vanilla classes and fields accessible (e.g. `ClientChunkCache$Storage`, `ChunkHolder` futures). The AT lists public access for vanilla members (e.g. `ModelBlockRenderer$Cache`, `ChunkHolder` tick futures).
- VS2 1.20.1: `common/src/main/resources/valkyrienskies-common.accesswidener` (45 lines). No AT or CFG file is tracked. 1.21.1: 37 lines.

### 2.8 Native code and physics backends

Sable (physics = Rapier, via a native library):
- Rust crate `sable_rapier` (`sable_rapier/src/main/rust/rapier/src/*.rs`, 19 `.rs` files, 5,854 lines). Depends on `rapier3d` from `https://github.com/ryanhcode/rapier`, pinned to git rev 38e92f117590862481a53df6fc69a5d893e29186 (`sable_rapier/src/main/rust/rapier/Cargo.toml`). Features: `simd-nightly`, `parallel`. JNI through the `jni` 0.21.1 crate (`sable_rapier/src/main/rust/Cargo.toml`). Crate type `cdylib` (plus `lib`). Toolchain `nightly-2026-01-29` (`rust-toolchain.toml`).
- Java bridge: `sable_rapier/src/main/java/dev/ryanhcode/sable/physics/impl/rapier/` (14 files, 2,114 lines). `Rapier3D.java` reads `/natives/sable_rapier/sable_rapier_binaries.zip.l4z`, unwraps an LZ4 frame and a zip, finds the entry for the current platform (`sable_rapier_x86_64_windows.dll`, `..._macos.dylib` or `..._linux.so`, or the aarch64 variant), writes it into a `.sable/natives` folder under the game directory, and calls `System.load`. Code lines around 38-110.
- Bundled binary: 9,072,872 bytes. Its contents were NOT inspected or unpacked. Which platforms are included is UNVERIFIED.
- Pluggable pipeline: the Sable API is pipeline-agnostic (`common/src/main/java/dev/ryanhcode/sable/api/physics/PhysicsPipeline.java`), and Rapier registers through a `PhysicsPipelineProvider` service file (`sable_rapier/src/main/resources/META-INF/services/dev.ryanhcode.sable.api.physics.PhysicsPipelineProvider`).
- Declared but not present: `.gitmodules` declares `common/src/main/csharp/LibSableBepu/bepuphysics2` (https://github.com/bepu/bepuphysics2.git), and `.gitignore` references `common/src/main/csharp/*/libsable_bepu`. No C# files are tracked at HEAD. Whether a Bepu backend exists or existed is UNVERIFIED.

VS2 (physics = Krunch, from an external artifact):
- No native code is tracked in this repo: no `.rs`, `.c`, `.cpp`, `.h`, and no `.dll`, `.so` or `.dylib` files.
- Physics dependency: `org.valkyrienskies.core:{api,impl,internal,util}` at `vs_core_version` (`common/build.gradle` lines 17-24). Also `krunch_version=1.0.0+6eb0d35353` and `krunch_api_version=1.0.0+e0c015f294` (`gradle.properties`, 1.20.1). On 1.21.1: `vs_core_version=1.1.0+9ca3f9139c`, `krunch_version=1.0.0+f0bdc63445`.
- Backend selection: `common/src/main/kotlin/org/valkyrienskies/mod/common/command/commands/BackendCommand.kt` assigns `ConfigPhysicsBackendType.KRUNCH_CLASSIC`, `KRUNCH_JOLT` and `KRUNCH_PHYSX` (lines 27-69). The enum is imported from `org.valkyrienskies.core.impl.api_impl.config` (line 11), so it lives in vs-core, not in this repo.
- vs-core is not in this repo. `.gitmodules` is 0 bytes. The CI workflow requests submodules and names a vs-core submodule (`.github/workflows/build.yml` line 18), so the workflow looks stale. `build.gradle` includes `vs-core` only when the folder exists.

### 2.9 Public API

- Sable: `common/src/main/java/dev/ryanhcode/sable/api/` (physics, sublevel, block, event, math packages). 62 Java files.
- VS2: `common/src/main/kotlin/org/valkyrienskies/mod/api/` (`VsApi.kt`, `ValkyrienSkies.kt`, `events/*` (Pre/PostRenderShipEvent, RegisterBlockStateEvent), `config/`, `datagen/`, `EntityPhysicsListener.kt`, `BlockEntityPhysicsListener.kt`, `SeatedControllingPlayer.kt`). `api_impl/` has 2 files. All Kotlin.

### 2.10 Contributors and activity (GitHub API, 2026-10-11)

| | Sable | VS2 |
|---|---|---|
| Contributors (default branch, per_page=100) | 32 (ryanhcode 121 commits; Ocelot5836 89; IThundxr 17; github-actions[bot] 14) | 60 (Rubydesic 467; ThePlasticPotato 409; StewStrong 171; ewoudje 136; zaafonin 101) |
| Commits on default branch since 2026-09-11 | 5 | 0 |
| Last commit on default branch (clone HEAD) | 2026-10-05, "Sub-level entity collision changes" | 2026-07-22, "Add dev wiki to README (#1930)" |
| Last commit on 1.21.1/main | not applicable | 2026-06-02, merge of PR #1851 |
| Open PRs (first page) | 41 | 32 |
| Releases | 5 listed; latest mc1.21.1-2.0.6 | latest `release/2.4.11` (2026-04-10); Modrinth 1.20.1-forge/fabric-2.4.11 |

The VS2 Modrinth project lists game versions 1.12.2, 1.16.5, 1.18.2, 1.19.2 and 1.20.1 (113 versions). No 1.21.x release is listed.

## 3. VS2 physics backend (step 3)

- Engine name: Krunch. It is the physics layer in VS2's config and build (`krunch_version`, `KRUNCH_*` backend enum). Its implementation is in vs-core, which is consumed as `org.valkyrienskies.core` artifacts from the VS Maven repository (`settings.gradle` declares `maven.valkyrienskies.org`).
- Selectable backends (from the enum names in BackendCommand.kt): `KRUNCH_CLASSIC`, `KRUNCH_JOLT`, `KRUNCH_PHYSX`. The Jolt option is also described in a CurseForge changelog for VS2 2.4.10 on Forge 1.20.1 (secondhand). Jolt is described as MIT-licensed (secondhand; UNVERIFIED against the Jolt repo). PhysX is NVIDIA's engine (general knowledge; no PhysX code in this repo).
- Open source status of Krunch: UNVERIFIED. The ValkyrienSkies GitHub org's 25 public repositories (API listing) include no repo named vs-core or Krunch. WebSearch returned no primary Krunch documentation.
- Separate repo or closed binary: from this repo's point of view it is an external dependency resolved from Maven. Whether the source is in a separate private or unpublished repo, or only as binaries, is UNVERIFIED.
- Sable's physics engine, by comparison: Rapier, open source (Apache-2.0 licence file for the fork), in-repo Rust source, shipped as a prebuilt native binary.

## 4. Port analysis (step 4)

### 4.1 Latest Minecraft Java version

- Latest: Java Edition 26.3 "Wilderness Bound". The Minecraft Wiki page https://minecraft.wiki/w/Java_Edition_26.3 gives the release date as September 15, 2026, describes it as released rather than pre-release, and lists the minimum Java version as Java SE 25 [S7]. Secondhand sites report the same date. The official minecraft.net launch article was not fetched; UNVERIFIED on that point.
- Before it: 26.2 "Chaos Cubed", released 2026-06-16, with an experimental Vulkan rendering backend [S9, secondhand]. 26.1 "Tiny Takeover", released 2026-03-24 (WebSearch summary, secondhand). The 26.1 notes say it is the first unobfuscated Minecraft release [S6].
- Numbering: the 1.x line ended at 1.21.11 in December 2025 (secondhand, WebSearch summary).

### 4.2 Subsystem table

Base versions: Sable is on 1.21.1. VS2's default branch is on 1.20.1; its 1.21.1 branch is also measured. Evidence below covers 1.21.1 to 26.3. Changes from 1.20.1 to 1.21.1 were not read in this node, so VS2 1.20.1 is only partly covered.

Source keys:
- [S1] NeoForge primer for 1.21.2: https://docs.neoforged.net/primer/docs/1.21.2 (read in two WebFetch windows; the last ~15k of 215k characters were not read).
- [S2] NeoForge primer for 1.21.5: https://docs.neoforged.net/primer/docs/1.21.5 (first ~100k of ~207k characters read).
- [S3] NeoForge 21.6 release notes: https://neoforged.net/news/21.6release/
- [S4] Fabric docs, rendering basic concepts (1.21.11): https://docs.fabricmc.net/1.21.11/develop/rendering/basic-concepts
- [S5] Fabric 1.21.5 modder changes: https://fabricmc.net/2025/03/24/1215.html
- [S6] Fabric 26.1 modder changes: https://fabricmc.net/2026/03/14/261.html
- [S7] Minecraft Wiki, Java Edition 26.3: https://minecraft.wiki/w/Java_Edition_26.3
- [S8] Fabric porting, mappings: https://docs.fabricmc.net/develop/porting/mappings (search summary, secondhand)
- [S9] 26.2 coverage (secondhand): https://hosthavoc.com/blog/minecraft-26-2-chaos-cubed-update, https://games.gg/news/minecraft-26-2-snapshot-1-chaos/. Sources disagree on whether Vulkan is the default. Treat as secondhand.

| Subsystem | Sable: mixin files / vanilla targets | VS2 1.20.1: mixin files / vanilla targets | Changed in later Minecraft? | Evidence |
|---|---|---|---|---|
| Rendering: LevelRenderer, GameRenderer, Camera, ViewArea, SectionCompiler, SectionRenderDispatcher.RenderSection, ChunkRenderDispatcher.RenderChunk, EntityRenderDispatcher, EntityRenderer, ParticleEngine, Particle, DebugRenderer, BlockEntityRenderDispatcher, FogRenderer, ModelBlockRenderer | 56 / 28 | 32 / 17 | YES (large). The render pipeline was reworked from 1.21.5: shader JSON files were replaced by in-code RenderPipeline, RenderType.create takes a RenderPipeline, and GpuDevice / CommandEncoder / RenderPass replaced direct GL-style use. GUI was heavily reworked in 1.21.6. Rendering now splits into an extraction phase and a draw phase (1.21.11). From 26.1, terrain uses ChunkSectionLayer in place of RenderType/RenderLayer. Raw OpenGL code must move to the Blaze3D API. 26.2 adds a graphics backend abstraction (secondhand). Method-level changes in LevelRenderer, GameRenderer and SectionCompiler: UNVERIFIED. | [S2] [S3] [S4] [S6] [S9] |
| Chunk and world storage: ChunkMap, ServerChunkCache, ServerLevel, ClientLevel, Level, LevelChunk, LevelChunkSection, DistanceManager, ChunkHolder, WorldBorder, PersistentEntitySectionManager, ChunkStatus | 35 / 12 | 39 / 18 | YES, in part. 1.21.2: ChunkTaskPriorityQueueSorter renamed ChunkTaskDispatcher; ChunkHolder#hasChangesToBroadcast and ServerChunkCache#onChunkReadyToSend added; DistanceManager#getTickingChunks and getSpawnCandidateChunks added. 1.21.5: ticket system reworked (TicketStorage, TicketType, Ticket) and SavedData/SavedDataType reworked. 26.1: "major changes to world storage", no details given [S6]. ChunkMap, LevelChunk and ServerLevel changes beyond these: UNVERIFIED. | [S1] [S2] [S6] |
| Collision and entity movement: Entity, LivingEntity, Player, ServerPlayer, LocalPlayer, RemotePlayer, Mob, PathfinderMob, AbstractMinecart, vehicle.*, phys.*, VoxelShape, CollisionContext, BlockCollisions, CollisionGetter | 47 / 13 | 32 / 9 | YES for the API surface, UNVERIFIED for the movement core. 1.21.2: Entity#setOnGroundWithMovement gained a boolean; teleportTo gained a boolean; checkInsideBlocks became recordMovementThroughBlocks (takes movement vectors); isInvulnerableTo moved from Entity to LivingEntity; BlockCollisions gained a CollisionContext constructor; CollisionContext.of(Entity, boolean) added; VoxelShape#move(Vec3) added. Entity.move and the collision routines themselves: UNVERIFIED. | [S1] |
| Networking: ClientboundTeleportEntityPacket, ClientboundMoveEntityPacket (Pos, PosRot), ClientboundRotateHeadPacket, ClientboundBlockUpdatePacket, ServerboundMovePlayerPacket, ServerGamePacketListenerImpl, ClientPacketListener, MultiPlayerGameMode, Connection, ServerConnectionListener, ServerEntity, ChunkMap.TrackedEntity | 17 / 12 | 9 / 6 | YES for several packets Sable touches (1.21.2): ClientboundTeleportEntityPacket is now a record taking its parameters directly. ClientboundMoveEntityPacket#getyRot/getxRot return float degrees. ClientboundRotateHeadPacket#getYHeadRot returns float. ClientboundGameProfilePacket renamed ClientboundLoginFinishedPacket. ServerboundPlayerInputPacket and ClientboundPlayerPositionPacket became records. 1.21.5 added game-test packets. ServerboundMovePlayerPacket, ServerGamePacketListenerImpl and the custom-payload path: UNVERIFIED. | [S1] [S2] |
| Lighting: ThreadedLevelLightEngine, LevelLightEngine, LightEngine, LayerLightSectionStorage, SkyLightSectionStorage, DataLayer, LightTexture | 0 / 0 | 6 / 5 | YES for VS2's targets (1.21.2): ThreadedLevelLightEngine now takes a ConsecutiveExecutor and a ChunkTaskDispatcher; LayerLightSectionStorage#lightOnInColumn added; ChunkHolder#sectionLightChanged returns boolean; LightTexture gained getBrightness and lightCoordsWithEmission; light emission added to BakedQuad, BlockElement and FaceBakery#bakeQuad. | [S1] |
| Worldgen: ChunkGenerator, NoiseBasedChunkGenerator, FlatLevelSource, Biome, BiomeGenerationSettings, StructureTemplate (and Palette), ChunkStatusTasks, GenerationChunkHolder | 2 / 2 | 4 / 4 | YES (1.21.2 and 1.21.5). GenerationStep$Carving removed. ChunkGenerator#applyCarvers no longer takes the step; carvers are supplied as a HolderSet. BiomeGenerationSettings carver methods changed. CarvingMaskPlacement removed. StructureTemplate gained getJigsaws and getJointType. The weighted-list rework touches ChunkGenerator#getMobsAt (1.21.5). The Biome class by itself: UNVERIFIED. | [S1] [S2] |
| Other, block and pathing: Block, BlockState, block entity types, FlowingFluid, WaterFluid, LavaFluid, PathFinder, WalkNodeEvaluator, SwimNodeEvaluator, FlyNodeEvaluator, GameEventDispatcher, VibrationSystem, Fire, Vine, Stair, Slab | 22 / 21 | 22 / 21 | PARTIAL. 1.21.5: AbstractBlock#onStateReplaced changed; block entities should use BlockEntity#onBlockReplaced. Pathfinding and game-event internals: UNVERIFIED. | [S5] |
| Other, items and inventory: Item, BlockItem, BlockPlaceContext, UseOnContext, AbstractContainerMenu, EnchantmentMenu | 3 / 3 | 5 / 4 | YES (26.1). ItemStack cannot be created until a world loads; ItemStackTemplate replaces it in many methods. | [S6] |
| Other, entities (non-movement): EntityType, AI goals and navigation, projectiles, ProjectileUtil, villagers and POI, bees, raids, LightningBolt, Leashable, FallingBlockEntity, ItemEntity, PrimedTnt | 16 / 16 | 27 / 26 | PARTIAL. 1.21.2: Entity#hurt deprecated for hurtServer and hurtClient; LivingEntity#canContinueToGlide became canGlide. 1.21.5: equipment, variants and data components reworked. 1.21.5: DataPool replaced by Pool. 26.1: villager trades moved to data (data-driven). Individual AI and projectile classes: UNVERIFIED. | [S1] [S2] [S5] [S6] |
| Other, client (non-render): Minecraft, Gui, KeyMapping, SoundEngine, DebugScreenOverlay, MouseHandler, ConnectScreen, ClientSuggestionProvider, AbstractSoundInstance | 11 / 9 | 10 / 9 | PARTIAL. HudRenderCallback deprecated in 1.21.5 (replacement HudLayerRegistrationCallback) and removed in 26.1 (replacement HudElementRegistry); these are Fabric API changes, not vanilla. Gui and GuiGraphics reworked (1.21.6) and render through state extraction (1.21.11). Minecraft, SoundEngine, KeyMapping: UNVERIFIED. | [S3] [S4] [S5] [S6] |
| Other, core / commands / server / GameTest: MinecraftServer, PlayerList, GameTestInfo, StructureUtils, TestCommand, Commands, ExecuteCommand, DataCommands, Explosion, ClipContext, CombatTracker | 17 / 15 | 12 / 11 | PARTIAL. Vanilla GameTest was refactored (from 25w03a) [S5]. Sable targets GameTestInfo, StructureUtils and TestCommand, so these are affected. Other classes: UNVERIFIED. | [S5] |

Cross-cutting (not subsystem-specific):
- Obfuscation and mappings: 26.1 is the first unobfuscated release. Fabric no longer officially supports Yarn from 26.1 and moves to Mojang names. Loom 1.15 uses the `net.fabricmc.fabric-loom` plugin id, `implementation`, `compileOnly` and `jar` in place of the `modImplementation` and `remapJar` setup [S6, S8]. Both repos already use Mojang names (Sable: NeoForm and Parchment plus official Mojang on Fabric; VS2: `officialMojangMappings()`), so the change is in the build, not in the target names. Mods built for 1.21.11 or earlier need recompiling for 26.1 [S6].
- Java: 26.1 and later require Java 25 [S6, S7]. Sable declares `java_version=21`. VS2 is on Java 17 (README).
- Raw GL and shader footprint: Sable has 24 Java files that import lwjgl, GlStateManager or RenderSystem (38 matching lines), 15 files that use ShaderInstance, setShader or RenderPipeline, 23 files that mention RenderType or RenderLayer, and 612 + 199 + 31 lines of GLSL, vert and frag shader source (common and neoforge modules, Flywheel shaders among them). VS2 has 6 such files (12 lines), 3 shader-related files, 8 RenderType/RenderLayer files, and no shader source. 26.1 notes say raw OpenGL code must move to the Blaze3D API [S6]. 26.2 adds a backend abstraction with Vulkan (secondhand) [S9].
- Fragility indicators (counts, not estimates): @Overwrite: Sable 33, VS2 15 (1.20.1) and 12 (1.21.1). @Redirect: Sable 168, VS2 148 and 159.
- Third-party targets: Create is the largest third-party target in both repos (Sable 116 unique targets, VS2 111). Sable pins Create 6.0.10-280 and Flywheel 1.0.6 for 1.21.1 (`gradle.properties`). VS2 pins Create-Fabric 6.0.7.0 for 1.20.1. The availability of Create and Flywheel for 26.3 is UNVERIFIED.

### 4.3 Gaps (UNVERIFIED, not read in this node)

- Entity.move and the collision routines themselves; ServerboundMovePlayerPacket; ServerGamePacketListenerImpl and ClientPacketListener in full.
- ChunkMap, LevelChunk, ServerLevel and ClientLevel method-level changes; the content of the 26.1 "world storage" change.
- The Biome class, StructureTemplate, LightTexture and lighting engine internals beyond the listed signatures.
- LevelRenderer, GameRenderer and SectionCompiler method-level changes after 1.21.1.
- The 26.2 Vulkan backend and sign/hanging-sign block-model change: secondhand only [S9].
- Changes between 1.20.1 and 1.21.1 (VS2 default branch base): not read.
- Krunch source availability and licence; whether Jolt and PhysX backends in VS2 are production-ready.

## Appendix A. Remote branches and tags (`git ls-remote`)

Sable heads (`sable\ls-remote-heads.txt`):

```
c5d4c7fa7245380a9c233d9035df423aa842d060	refs/heads/feature/arm-windows-natives
cc0fe20b3c565abf6db3cf716658515a018d5589	refs/heads/feature/oritech-compat
44c8875c8c2eed68509f9757ac967d0f0264a960	refs/heads/fix/starlight
d5afc059d61d7711973c85444864da7e483735a0	refs/heads/fix/udp
d501fb3103c3247e20cb2151a99182ad5dd61adb	refs/heads/main
ed62934d4eadea023802c1e6931f5f947b4504b8	refs/heads/proto/narrow-phase-changes
```

Sable tags (`sable\ls-remote-tags.txt`):

```
283c0f61f734e1752eafaeba3f8dcc0ecc9d7f25	refs/tags/mc1.21.1-1.0.3-fabric
283c0f61f734e1752eafaeba3f8dcc0ecc9d7f25	refs/tags/mc1.21.1-1.0.3-neoforge
697394921d82ab3104b3b00700b46b9070e15f14	refs/tags/mc1.21.1-1.0.4-fabric
697394921d82ab3104b3b00700b46b9070e15f14	refs/tags/mc1.21.1-1.0.4-neoforge
1040b2fc82c4cf71708954b56041cf4a1affc3f0	refs/tags/mc1.21.1-1.0.5-fabric
1040b2fc82c4cf71708954b56041cf4a1affc3f0	refs/tags/mc1.21.1-1.0.5-neoforge
988a28822cd60a3784f01b0b5f44baa4f2c1e1e1	refs/tags/mc1.21.1-1.0.6-fabric
988a28822cd60a3784f01b0b5f44baa4f2c1e1e1	refs/tags/mc1.21.1-1.0.6-neoforge
165cf388f775d40c414cf1284c45f6a00a80f74f	refs/tags/mc1.21.1-1.1.0-fabric
165cf388f775d40c414cf1284c45f6a00a80f74f	refs/tags/mc1.21.1-1.1.0-neoforge
2facf7b8688b4639d4f99466b7df2844107ba8af	refs/tags/mc1.21.1-1.1.1-fabric
2facf7b8688b4639d4f99466b7df2844107ba8af	refs/tags/mc1.21.1-1.1.1-neoforge
2c852dcf036c639592a1c119bc686ffd65292184	refs/tags/mc1.21.1-1.1.2-fabric
2c852dcf036c639592a1c119bc686ffd65292184	refs/tags/mc1.21.1-1.1.2-neoforge
78b41b6d4c621dddb2fb40000220951b703e8651	refs/tags/mc1.21.1-1.1.3-fabric
78b41b6d4c621dddb2fb40000220951b703e8651	refs/tags/mc1.21.1-1.1.3-neoforge
c91300301c84fc690e2ab3f00e3eb0fc3959ab5a	refs/tags/mc1.21.1-1.2.0-fabric
c91300301c84fc690e2ab3f00e3eb0fc3959ab5a	refs/tags/mc1.21.1-1.2.0-neoforge
0ca421bdb68231be88423d94525da4bc18de56b8	refs/tags/mc1.21.1-1.2.1-fabric
0ca421bdb68231be88423d94525da4bc18de56b8	refs/tags/mc1.21.1-1.2.1-neoforge
f68bc49a9b715d4e41dbd60118f59b5f1df9cb49	refs/tags/mc1.21.1-1.2.2-fabric
f68bc49a9b715d4e41dbd60118f59b5f1df9cb49	refs/tags/mc1.21.1-1.2.2-neoforge
b7226222caf4eace63a708bdcd73ef36c971137d	refs/tags/mc1.21.1-2.0.0-fabric
b7226222caf4eace63a708bdcd73ef36c971137d	refs/tags/mc1.21.1-2.0.0-neoforge
162fdc940a7ead9ea5f1c6a6282c7495a428a2cc	refs/tags/mc1.21.1-2.0.1-fabric
162fdc940a7ead9ea5f1c6a6282c7495a428a2cc	refs/tags/mc1.21.1-2.0.1-neoforge
8a83709a263e27d6b9a27d7f42ae39b2aebbf5d0	refs/tags/mc1.21.1-2.0.2-fabric
8a83709a263e27d6b9a27d7f42ae39b2aebbf5d0	refs/tags/mc1.21.1-2.0.2-neoforge
17d29f5b99d50e3cc13b536b23f9181ade99f14f	refs/tags/mc1.21.1-2.0.3-fabric
17d29f5b99d50e3cc13b536b23f9181ade99f14f	refs/tags/mc1.21.1-2.0.3-neoforge
56dfd2218e970d1cf58f3bdbdbc8cf50aa0aa2bb	refs/tags/mc1.21.1-2.0.4-fabric
56dfd2218e970d1cf58f3bdbdbc8cf50aa0aa2bb	refs/tags/mc1.21.1-2.0.4-neoforge
6966d2928340de7631abcecf8549904b877df0a8	refs/tags/mc1.21.1-2.0.5-fabric
6966d2928340de7631abcecf8549904b877df0a8	refs/tags/mc1.21.1-2.0.5-neoforge
ad8b4d377741fbd923f4d863b2cf9e462daff9a6	refs/tags/mc1.21.1-2.0.6-fabric
ad8b4d377741fbd923f4d863b2cf9e462daff9a6	refs/tags/mc1.21.1-2.0.6-neoforge
```

VS2 heads (`vs2\ls-remote-heads.txt`; default branch 1.20.1/main, 1.21.1/main present):

```
9489095a0a505e7b71a82d50b45467e6cfa58aca	refs/heads/1.16.x/main
1edb9df58c028811d00f884d2ccdc20b62d7a386	refs/heads/1.18.x/Splitting-3
db25f041483319595243225ff9b8781679da0142	refs/heads/1.18.x/api
2a905f13035b3855136470bad725f43fc2ec7e52	refs/heads/1.18.x/assembly-replace
18b11ea3a1494d9d70ab8440bc76c88edd5642eb	refs/heads/1.18.x/copycat-quickfix
8ae4739d6437e388af7df5118bf9bb4728375087	refs/heads/1.18.x/entity-dragging-redux
83dfca269d0a89919730b574a21529a68bf4d92c	refs/heads/1.18.x/feature/fast-physics
531a63f21fb4b32f9d6062e051eae16b567e969c	refs/heads/1.18.x/feature/no_arch
fb45c09f4821880d7614b66123445304360e46f6	refs/heads/1.18.x/fix/entity-dragging
7209806adfa3963bc29ff717bae4745c48dd82ba	refs/heads/1.18.x/main
cf1ab0c6f21d8d6e145a08cc86c10dd2bf4058b7	refs/heads/1.18.x/unloaded_chunk_flight
e0fc0531ac8374d39ee973ebaef23097a0cc3f21	refs/heads/1.18.x/weight-reduction
5032dbf00d74c536c38d4276057c173389e37ce4	refs/heads/1.19.2/main
8a91cb85d2bd35550f3b48e99ee9eb0e7fec374c	refs/heads/1.20.1/2.3
1f4ba9fa42f7f1d970e8f60ae6a97af4495ea512	refs/heads/1.20.1/2.4
e0bf97639a22b19a0b83291d6a3bfc37e6dd1f2a	refs/heads/1.20.1/aerodynamics-fixes
acb035c4ef6d8476bd2c4100dfe89d0d20eeb0cc	refs/heads/1.20.1/api
5c940aabc4b184faf53f64ad6c0bb9f13eebe345	refs/heads/1.20.1/better_deps
e6153ce10301910e53f5137ef8fc7de9dfb2220c	refs/heads/1.20.1/better_drills
8d130aaff0da410852cc7603b0342972ad77b78a	refs/heads/1.20.1/blockentity_dynlight
6f3811b31155575a0ff33f160802317e3cc62e8d	refs/heads/1.20.1/bluemap_fix
447e05c59e402a649cc8c9f8d6f26f0f491cbba6	refs/heads/1.20.1/bote
eaaed675f5ab3283d443566a890d9d489d94b28b	refs/heads/1.20.1/cbc-chunkloading
e4570b088a38e451776d18a501d6c09710e34501	refs/heads/1.20.1/cleaned_todos
f3ec1d1cfe5f84ab4dc7b67f585d0c12e2960f35	refs/heads/1.20.1/cleanup/gradle
c17efb57d2cd25b112417f1b7012175cabbdd72f	refs/heads/1.20.1/command_cleanup_part_31415
48d9d1f8636070ced3f865766e6d3d024b578c2c	refs/heads/1.20.1/contact_crash_fix
17d6d2639c5711674dac4d81853626a99e8fbddb	refs/heads/1.20.1/copycat_weight_loss
783887696b4b9f5695e285926699959c3a5b51b7	refs/heads/1.20.1/dry_command
0b1244d7ece8d80695feaf21131f945a20730b4e	refs/heads/1.20.1/explosion_changes
6abfa9bc9ad794c258d822aa913011ac1d758880	refs/heads/1.20.1/fix/dragging-safe-cast
e6043d1f921e9e8e176b936914cbf5e055326ed2	refs/heads/1.20.1/fix/immportals
58ed88ed26e7d0e80e4dd70750d919528fa08e55	refs/heads/1.20.1/fix_cannon_recoil
f53a2bec67591bfde3bc67ce7f4cb62a9a7600b3	refs/heads/1.20.1/fixed_ci
d92d2eeb36dff7e6c649254964837bd2972809ea	refs/heads/1.20.1/hypertube
7db125f8b696cc8087791a6752b06a9ce2133169	refs/heads/1.20.1/jenga_command
780bc04dc16e258c34a25841d096c9ae051717be	refs/heads/1.20.1/lightning_fixes
85299758aaafc2e94b4bd133955d2247287b75a4	refs/heads/1.20.1/liquid_overlap
a79995cdf4fa863c034feb176000a3fca023ab37	refs/heads/1.20.1/main
434697ff259aa855a478ceb3c034f908fad1d29c	refs/heads/1.20.1/main+new-assembly
a5669c1b3294ccf7cd10fe9197f3a2cdc4288fa8	refs/heads/1.20.1/main-apr-27-fix-carved-pumpkin-crash
8aa92a2d64fbe788b2f61148fe7ff0261d0ddf2e	refs/heads/1.20.1/main-dec-24-connectible-chains-duplicate
a53d71ae7dcdc9603602e13c00872ce04535595b	refs/heads/1.20.1/main-fix-hexical-version
7e2b7dd6a8dd18fb77502d204548892680ac6562	refs/heads/1.20.1/main-jan-10-echo-chest-remap-fix
b979ddc8ac6b9cef3804ee43cc68eedc78078f47	refs/heads/1.20.1/main-jan-12-deeper-worldgen-disabling
27d67790dfeab11914789b12280f34ae3303d86f	refs/heads/1.20.1/main-jan-13-air-pocket-breathing
c4d2a16c0dabf5d3ead66e2d0aa8c35c2538e435	refs/heads/1.20.1/main-jan-14-hypertube-compat
a108d05945cc22e311a67ab758f12d29f943ddc2	refs/heads/1.20.1/main-jan-15-fix-forge-air-pocket-breathing
c4ee51cebbe4442e17368ef5cdf280311682eabe	refs/heads/1.20.1/main-jan-15-more-connchains
3081e6563fd9e7dc2e49f3af1d459dea96a40079	refs/heads/1.20.1/main-jan-16-fake-serverlevel-fix
c00d0fe2017a38f508ef5097f8ecdc011b9d81fb	refs/heads/1.20.1/main-jan-17-fix-fluidslop-new-forge
23a53644f924ef1cc460edf8129177bcca7420f9	refs/heads/1.20.1/main-jan-18-ship-core-shader
8ce534c9f178351b8d3f2b4ccaa8b1a10b59a998	refs/heads/1.20.1/main-jan-23-random-small-fixes
35ad079b44b660961c75320692cd1e3835d4f879	refs/heads/1.20.1/merge-pockets-from-main
554fd5e6afc71c7945e0dba041c3f1f1beb7e8d4	refs/heads/1.20.1/mobs-on-ships
40e8b351d2f5ec555547286f27188e93a0488cf2	refs/heads/1.20.1/more_theatrical
cf92ba88851cd933c9cad26cbde80d0a6a2be7f1	refs/heads/1.20.1/new_airpockets
8f417d15d2525ef0157360c237a27dab97931c53	refs/heads/1.20.1/new_airpockets-main-merge
7ad734d4ba0aa6104eae0543bb324d9ef7122691	refs/heads/1.20.1/new_airpockets_preoptimization
b601dc402b1a331e679e0034d1e5435d6a3beea9	refs/heads/1.20.1/official_command_feedback
3a8a3a4e6f266f397e519b6f97d1de8a325f949d	refs/heads/1.20.1/pistons-push-ships
520c389b999f80ce846b18c3496ca2fb23bc4bf3	refs/heads/1.20.1/playtest
e97cb8a086130499c71ba43fbf49be021075b640	refs/heads/1.20.1/ready_for_release
e74947fde9e2ecf5c43702496d61c86efaba901a	refs/heads/1.20.1/real_camera
143b18e4df46696b039bbb5fe4c69e8a65f8c4f2	refs/heads/1.20.1/room-service
170a74a6831d41611c67688c32315726bc3b0937	refs/heads/1.20.1/splitting-n-sodium
aad9a6000c46a8a78b147e3a25d8225811c0e715	refs/heads/1.20.1/spotlight_compat
2bf3cdbc5a9ac901b954d6b3c9b1f79c46dd9f17	refs/heads/1.20.1/terrafirmacraft
8a6d77217b7cf97688e916c8562d2dbaeb9a0613	refs/heads/1.20.1/tweaked_lava
cca0d60b3c3d7122bba30f69c68c663a4aeb661b	refs/heads/1.20.1/unvibe_pr
eac930dfbd30ae14d8c0cda0d92049312468280c	refs/heads/1.20.1/vdex_rewrite
aa6d7146436ea1210fa7feccb6157ce962da8a4a	refs/heads/1.20.1/very_many_players
83a474aa861e6a02909e6b7804cb61f4ae340605	refs/heads/1.20.1/wip_command_rewrite
cd368412c7b6e9186db65f7edcd0a15fd96b9f30	refs/heads/1.20.1/wip_debug_vsbody_render
ca5e2204060116db985902e4530329f3627a111b	refs/heads/1.20.1/world_border
c8f691d3e7f54f5534a3fc8fc61334144e5dff1a	refs/heads/1.20/main
f39132148e717d325933b4ce6e9e9fb13d929390	refs/heads/1.21.1/main
150695369c41bb63e19780f29fa94c9449052396	refs/heads/ThePlasticPotato-patch-1
302e6d45973d77ef3de3ac083269519e49b68412	refs/heads/blockninja124-patch-1
b17f9094e5d52e4304fe45999191f2aaa8294681	refs/heads/blockninja124-patch-2
20746f8ca01538927a0a0e271510d3fb51b2d159	refs/heads/claude/forge-gametest-setup-KY6t0
928b5dd9937a68ea6cf8dcf4d00df00e2d7bcf67	refs/heads/feature/commands
976ebf265858e5c938f2227bc305fea1630d38a1	refs/heads/feature/dynamic-lights
830a4166bbc0648ca7462ebed7ffe3322fb4cefd	refs/heads/fix/batched-renderer
dde940b93f9b7e633b1731fb7260677f9c9394a8	refs/heads/fix/boundingbox-ship-sneaking
4f32eb152729b1e1714f95445e11139f550798c6	refs/heads/fix/compound-colliders
263d01081958031d2ab78008d4c22aef76e3717b	refs/heads/revert-1617-1.20.1/main-jan-18-ship-core-shader
d4d5f76cc1f96ae03290ff9798d4f97fe74c82a3	refs/heads/revert-1712-fix/bed-launch-velocity-accumulation
f1ee7fcfea4b71b4c06a4bed205ea5e90c55f696	refs/heads/revert-1739-1.20.1/fix-desync-and-lag
39d993c9d7907bfde5011f1243e463685b3e4bde	refs/heads/revert-1740-1.20.1/fix-desync-and-lag
```

VS2 tags (`vs2\ls-remote-tags.txt`):

```
9a09dd2609d482948f261a02c8103c1ba44ba5c1	refs/tags/2.4.11
c55ce60b4732e6b7843fd34fb6210da9caf7495f	refs/tags/playtest/1.20.1/2.5.0-A
10b9328ee224471d7255415073a01c824b00380c	refs/tags/playtest/1.20.1/2.5.0-B
110955a451041c21f7e1f18b36b77b0d057900d3	refs/tags/playtest/1.20.1/2.5.0-C
7274bb709f97c3fc6f8c02e3fa41ff1d623af536	refs/tags/playtest/1.20.1/2.5.0-D
d0f0265a28b7ada8eb99596c8ea240ea42a1989b	refs/tags/playtest/1.20.1/2.5.0-E
945664b0752154c6c00859510adac4d28288298d	refs/tags/playtest/1.20.1/2.5.0-F
aa94ba5cefd0b7fa3e171ad9389657a347e7cbad	refs/tags/playtest/1.20.1/2.5.0-G
c0407a57938ba8400813bed9663c9fcb9b66a5a8	refs/tags/playtest/1.20.1/2.5.0-H
589ddd5e86060df05e53d76f116455dc0bbaee30	refs/tags/playtest/1.20.1/2.5.0-I
b5e08bf1bac192dcb248b415d44e264875f5ed2c	refs/tags/playtest/1.20.1/2.5.0-J
0593592bad35d9483885398e06893f80465152b3	refs/tags/playtest/1.20.1/2.5.0-K
912293f4c81af6347e423796699f68c6c13d7c74	refs/tags/playtest/1.20.1/2.5.0-L
523f251d3c9486cde7f22e72cd2146bb8638993b	refs/tags/playtest/1.20.1/2.5.0-M
dcebfa596d48c169bfcaba9b1be3924873501d98	refs/tags/playtest/1.20.1/2.5.0-O
c28ae8228e3df86d08f6fecbe8cb745f6055451b	refs/tags/playtest/1.20.1/2.5.0-P
3830d429e2f96539e9c9606dfc203153810e8bef	refs/tags/playtest/1.20.1/2.5.0-Q
0ce9c9195f97ef99e2eefa77e221e6e5a1c486ce	refs/tags/playtest/1.20.1/2.5.0-R
a436e86a70221aed85a1dc064bd24f1b952b3ca4	refs/tags/playtest/1.20.1/2.5.0-S
547841e9eeb2bd068f3dfcfc76a6342dd1a4ab92	refs/tags/playtest/1.20.1/2.5.0-T
2b5aa789c14876b9ab9d8f13cda601c7d2e35793	refs/tags/playtest/1.20.1/2.5.0-U
23331bbb71da0e20fd91d7664e855bbf6e01cdf3	refs/tags/playtest/1.20.1/2.5.0-V
cff177d7e393285e8a79a42fa81a316bf570835a	refs/tags/playtest/1.20.1/2.5.0-W
5e35c548e90320769d51d93f4ce9e209baffbbaf	refs/tags/playtest/1.20.1/2.5.0-X
f5ffb78ab4b459874d23eadc9f35745000f00ea3	refs/tags/playtest/1.20.1/2.5.0-Y
9c794ab049ca79cff00da394ee5196cdbe71122b	refs/tags/playtest/1.20.1/2.5.0-Z
c805b0b2abbed9e952692dca47ebb79904dfe802	refs/tags/playtest/1.20.1/2.5.0-ZA
bb0a2f1544000274a115589a9556fa19869de11b	refs/tags/playtest/1.20.1/2.5.0-ZB
3099c3bc4ad6427a58f04d3a29fb153f9256e07b	refs/tags/playtest/1.20.1/2.5.0-ZC
eeaa2beb3f341012755dc17f46924f7172081cfe	refs/tags/playtest/1.20.1/2.5.0-ZD
9ad766735608b65349bc1acf5f61c9101a52ece7	refs/tags/playtest/1.20.1/2.5.0-ZE
6a304a3a3f8897c841eba5f6e2a82aca8c46d6f2	refs/tags/playtest/1.20.1/2.5.0-ZF
01026b9161b626dc4e4aa1e15ad1fe06a9c54956	refs/tags/playtest/1.20.1/2.5.0-ZG
273f3197d2e3b0d17748bd0ba4cee111aea9f9cb	refs/tags/playtest/1.20.1/2.5.0-ZH
d5099126cf2a4c7b27b8352d25870dfc72c54dc5	refs/tags/playtest/1.20.1/2.5.0-ZH^{}
2f5100ce109e1769e29cf8adedc5321c9d766c36	refs/tags/playtest/1.20.1/2.5.0-ZI
b2e5cecb3d86d33a0721e043db23d3f5f75e86b2	refs/tags/playtest/1.20.1/2.5.0-ZI^{}
6f864361cf0bbc7f80b43174536465dd68132768	refs/tags/playtest/1.20.1/2.5.0-ZJ
7ab76b96eeb68ebfd2e753a218fa2c04eaf5961c	refs/tags/playtest/1.20.1/2.5.0-ZJ^{}
08d6d035f73f91bb95f64315e924ac99399c6214	refs/tags/playtest/1.20.1/2.5.0-ZK
0478ba77ae926688832fb6dee1b3b17c0f705644	refs/tags/playtest/1.20.1/2.5.0-ZK^{}
ed66a29ee3aefaaa9864abbfe6d0ffe78e61f543	refs/tags/playtest/1.20.1/2.5.0-ZL
520c389b999f80ce846b18c3496ca2fb23bc4bf3	refs/tags/playtest/1.20.1/2.5.0-ZL^{}
8ba6c896168ec889b3640b58656f1971fc155cc6	refs/tags/playtest/2.1.0-beta.10
26f4c5e6ad649eb40fdb9155bd340fcddf6e5fc4	refs/tags/playtest/2.1.0-beta.10.2
d84531555f6d27171b763ffb6d6c9575defe3b36	refs/tags/playtest/2.1.0-beta.10.3
a247d65b36a725cc066626828aff25bb1e53055e	refs/tags/playtest/2.1.0-beta.11.1
4fd72831a8daba44937607efc42af5b56c5e8574	refs/tags/playtest/2.1.0-beta1-1
47a3f548dafaf29bae2d415e58ffcb27cb628f45	refs/tags/playtest/2.1.0-beta1-2
044074064f293967b0c9184a667e821e68070aaf	refs/tags/playtest/2.1.0-beta1-3
a8289501a960a0d48791410c7ca3546cbf1b985b	refs/tags/playtest/2.1.0-beta1-4
4762875a6dc02e549062202c73ad666614b87219	refs/tags/playtest/2.1.0-beta1-5
e8532d673d1d3935d3db36ff209e5a9640aea3f9	refs/tags/playtest/2.1.0-beta1-6
52e8a9bc30405bd9d7ce78b78bd605004d9f81d8	refs/tags/playtest/2.1.0-beta1-7
4ed0dd48650ad89b1e2eba15a048fc652b0d279b	refs/tags/playtest/2.1.0-beta8-1
4f530412d2836f46151946a7e47644e07f9c13f5	refs/tags/playtest/2.1.0-beta8-2
bf383c7c247a667d1a5685ddb0996e77ae985902	refs/tags/playtest/2.1.0-beta8-3
b7ee622e99fdae0f85b8da88034d0989c02f892e	refs/tags/playtest/2.2.0-beta.1.1
d4afaa29d143b4cffa2c0c4a3fd507184a86550d	refs/tags/release/1.21.1/2.3.0-beta.7
2437fe9f61cf8fa97caaa092bb8e5e59f9d8cff2	refs/tags/release/1.21.1/2.3.0-beta.8
0dd02e1f03d7f098f36a5afa7de3ce9ab7f82c1d	refs/tags/release/1.3.0-beta.2
c847f121b5c778508d3ddd1873b350f5af965c61	refs/tags/release/2.0-alpha1
5ed35549d83b9509974bf6532999dc478bcde3a7	refs/tags/release/2.0.0-alpha2
8ccd2e9c4dbaaadc08fab14257d72467b53c98dd	refs/tags/release/2.0.0-alpha3
d55ae6c1b58b2f5ef5b30148837f79318563c323	refs/tags/release/2.0.0-alpha4
e85d5875fb33920147b697c71feb2066516b1928	refs/tags/release/2.0.0-alpha5
6626c148b0c7870547e363ef6967f6a5be2d9e6a	refs/tags/release/2.0.0-alpha6
d84531555f6d27171b763ffb6d6c9575defe3b36	refs/tags/release/2.1.0-beta.10
a247d65b36a725cc066626828aff25bb1e53055e	refs/tags/release/2.1.0-beta.11
93fd59898f94b124ee777a33818b72c9b5a002e8	refs/tags/release/2.1.0-beta.12
272da42a67415e4165241a858bb87c5890f40f9d	refs/tags/release/2.1.0-beta.13
acba7d4175d48c1fd66dd6cd74c7c1d3d5abb1d1	refs/tags/release/2.1.0-beta.14
0c28d02ed5ef2c4d601230d08104ae18158b7277	refs/tags/release/2.1.0-beta1
1bd6ff7b479e7a612e57eb53e1885909d238edc5	refs/tags/release/2.1.0-beta2
d1a5efe27977dd5edeb94c6a98438fccf3db4678	refs/tags/release/2.1.0-beta3
0503c414b4e7fa28f915c0d4c8347cb1b0964d94	refs/tags/release/2.1.0-beta4
fee7891696382b8408d290035f47fef790142b8a	refs/tags/release/2.1.0-beta5
8f895664f059ad559f8cc40d91192751d5050f3c	refs/tags/release/2.1.0-beta6
b08c9e729ca7e800ba70a17934cdeb0cc628c923	refs/tags/release/2.1.0-beta7
bf383c7c247a667d1a5685ddb0996e77ae985902	refs/tags/release/2.1.0-beta8
ce70563af65d40184d8452e556c7359f5c472a56	refs/tags/release/2.1.0-beta9
6a526a935e590d437f0e75d078d00e3bc0bcdb15	refs/tags/release/2.1.1-beta.1
328c4f4927456fbb04556f956a24acce19bed053	refs/tags/release/2.1.1-beta.1^{}
e977b9e079a030c4046e7d8d957485449713aa8c	refs/tags/release/2.1.1-beta.2
bfbdffec8fd91fd77cf7cd8d100c9d8fbaad07c2	refs/tags/release/2.1.1-beta.2^{}
3396e16659e98ad723a883ff91daa5704c194d08	refs/tags/release/2.1.1-beta.3
ee9a5f30fe068ce87eadffbd640448d4a3abcece	refs/tags/release/2.1.1-beta.4
72fac3232e20f1179b53d586f7825ab77f233ff4	refs/tags/release/2.1.1-beta.5
cb72d0f763fe0877176f58c901931079384b6f52	refs/tags/release/2.1.2-beta.1
a52f38bd68363eb4a938f5f991f92fb170abefca	refs/tags/release/2.1.3-beta.1
fb44adc8e8cf364daea125a385e3810b44efc32b	refs/tags/release/2.2.0-beta.1
af75aac04897e73b72332603ece303288a302f4c	refs/tags/release/2.2.0-beta.2
c4d068cf4b0c01c57dc0d826f33a35449b164917	refs/tags/release/2.2.0-beta.3
476bc3d1fc142666ee04fdfbf2c68dcfbb94d351	refs/tags/release/2.3.0-beta.1
a661b6337094333e9a4f5fbd2df31c612a61936b	refs/tags/release/2.3.0-beta.10
aada7d064063616562b2d85e32aff6ae5313e43a	refs/tags/release/2.3.0-beta.11
3de82829e0d88fce4b325cfed40a4b6e1f968112	refs/tags/release/2.3.0-beta.12
0dd02e1f03d7f098f36a5afa7de3ce9ab7f82c1d	refs/tags/release/2.3.0-beta.2
9480a7cf6b966b6bd1b6d8abdc130ec732fb19d0	refs/tags/release/2.3.0-beta.3
75d0ef133a2b80edae646ce2bea99ad3aa2ebd08	refs/tags/release/2.3.0-beta.4
62ada2d669566ad28507a8b22a374dfa75f926bb	refs/tags/release/2.3.0-beta.5
b81c6bb376d6642f4312a25a6263183cd54a105d	refs/tags/release/2.3.0-beta.6
55bbd6a99d7a8fbb4156849ead5038f19f53894c	refs/tags/release/2.3.0-beta.7
7d950afb5e3e2186e28570d05f8e773e5981df5d	refs/tags/release/2.3.0-beta.8
e8e23da30723b7f35854abf1a52a05b05351de5e	refs/tags/release/2.3.0-beta.9
99a5c6e40f793abfd6e64ac28d224afdc59502a2	refs/tags/release/2.4.0
1af59bd5851dd5cb7078e212c1edca6ed3c820ab	refs/tags/release/2.4.1
05298c63ae088fda71c3f72da12a14612381e20a	refs/tags/release/2.4.10
9a09dd2609d482948f261a02c8103c1ba44ba5c1	refs/tags/release/2.4.11
4177223cfa9eb82131a21e7965504ad282ff43c6	refs/tags/release/2.4.2
301df3acfa4b14b13712a7f65958e510df7900bd	refs/tags/release/2.4.3
d4cc6ad3efd30b1f597ba414ac56dcd672b6593c	refs/tags/release/2.4.4
76311e1a4d466c7deadc22cc410c1fa29b45ce02	refs/tags/release/2.4.5
fb5dc8021465ef6604b000d499af5725900172ec	refs/tags/release/2.4.6
fabaa0c7372b086b16be9688ece8680a10adb12d	refs/tags/release/2.4.7
41d4bc53325cb647b794db0850ab8b4ad613a718	refs/tags/release/2.4.8
40011b4af20c91cf70b81387814fa868fdae05e3	refs/tags/release/2.4.8.1
321d082d54b4d2598268cf6f429cb9bf853f790f	refs/tags/release/2.4.9
```

## Appendix B. Single-line `@Mixin` grep output (`git grep --cached -h -E '@Mixin\('`, sorted and counted)

Sable (`sable\grep-mixin-lines.txt`; multi-line annotations are not in this list, see 2.5):

```
     13 @Mixin(Entity.class)
     11 @Mixin(ServerLevel.class)
      9 @Mixin(LevelRenderer.class)
      8 @Mixin(LivingEntity.class)
      7 @Mixin(ServerPlayer.class)
      6 @Mixin(ClientLevel.class)
      6 @Mixin(Camera.class)
      5 @Mixin(Player.class)
      5 @Mixin(GameRenderer.class)
      4 @Mixin(NozzleBlockEntity.class)
      4 @Mixin(LocalPlayer.class)
      4 @Mixin(LevelChunk.class)
      4 @Mixin(Level.class)
      4 @Mixin(ClientPacketListener.class)
      3 @Mixin(PlayerList.class)
      3 @Mixin(Minecraft.class)
      3 @Mixin(EntityRenderer.class)
      3 @Mixin(EntityRenderDispatcher.class)
      3 @Mixin(ChunkMap.class)
      2 @Mixin(ServerGamePacketListenerImpl.class)
      2 @Mixin(ServerEntity.class)
      2 @Mixin(ServerChunkCache.class)
      2 @Mixin(SectionRenderDispatcher.RenderSection.class)
      2 @Mixin(SectionCompiler.class)
      2 @Mixin(MovingSoundInstanceDelegate.class)
      2 @Mixin(HarvesterMovementBehaviour.class)
      2 @Mixin(EmbyTools.class)
      2 @Mixin(ChainConveyorBlockEntity.class)
      2 @Mixin(Block.class)
      2 @Mixin(BeltRenderer.class)
      2 @Mixin(AbstractBellBlock.class)
      1 @Mixin({SawBlock.class, DrillBlock.class})
      1 @Mixin({Mob.class, FleeSunGoal.class, GroundPathNavigation.class})
      1 @Mixin({ TrackBlockItem.class, TrackTargetingBlockItem.class })
      1 @Mixin({ BeltBlock.class, MillstoneBlock.class })
      1 @Mixin(value = VisualManagerImpl.class, remap = false)
      1 @Mixin(value = SuperGlueEntity.class, remap = false)
      1 @Mixin(value = SodiumWorldRenderer.class, remap = false)
      1 @Mixin(value = LevelRenderer.class, priority = 400)
      1 @Mixin(value = LevelRenderer.class, priority = 2000) // This makes sure it applies after normal mixins
      1 @Mixin(value = LevelRenderer.class, priority = 2000)
      1 @Mixin(value = LevelRenderer.class, priority = 1002) // Higher priority to go after Flywheel
      1 @Mixin(value = LevelRenderer.class, priority = 1002)
      1 @Mixin(value = Entity.class, priority = 500)
      1 @Mixin(value = Entity.class, priority = 1100)
      1 @Mixin(value = ContraptionControlsRenderer.class, remap = false)
      1 @Mixin(value = ContraptionCollider.class, remap = false)
      1 @Mixin(value = ChasingAABBOutline.class, remap = false)
      1 @Mixin(value = BlockGetter.class, priority = 1100)
      1 @Mixin(value = BlockEntityStorage.class, remap = false)
      1 @Mixin(value = AABBOutline.class, remap = false)
      1 @Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
      1 @Mixin(targets = "net.createmod.catnip.ghostblock.GhostBlockRenderer$TransparentGhostBlockRenderer")
      1 @Mixin(targets = "gg.moonflower.etched.api.sound.StopListeningSound")
      1 @Mixin(targets = "dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl")
      1 @Mixin(ZapperItem.class)
      1 @Mixin(WorldBorder.class)
      1 @Mixin(WirelessNetwork.class)
      1 @Mixin(WaterFluid.class)
      1 @Mixin(WalkNodeEvaluator.class)
      1 @Mixin(VoxelShape.class)
      1 @Mixin(VisualizationEventHandler.class)
      1 @Mixin(VineBlock.class)
      1 @Mixin(ViewFinderController.class)
      1 @Mixin(ViewFinderAccess.Block.class)
      1 @Mixin(ViewArea.class)
      1 @Mixin(VibrationSystem.Ticker.class)
      1 @Mixin(VibrationSystem.Listener.class)
      1 @Mixin(VerticalGearboxItem.class)
      1 @Mixin(ValueBox.class)
      1 @Mixin(UseOnContext.class)
      1 @Mixin(TurntableBlock.class)
      1 @Mixin(TrackTargetingClient.class)
      1 @Mixin(TrackPlacement.class)
      1 @Mixin(TrackGraphVisualizer.class)
      1 @Mixin(TrackBlockOutline.class)
      1 @Mixin(TrackBlock.class)
      1 @Mixin(ToolboxHandlerClient.class)
      1 @Mixin(ToolboxHandler.class)
      1 @Mixin(TntBlock.class)
      1 @Mixin(ThrownTrident.class)
      1 @Mixin(TestCommand.class)
      1 @Mixin(TerrainParticle.class)
      1 @Mixin(TamableAnimal.class)
      1 @Mixin(SymmetryHandler.class)
      1 @Mixin(SuspendedParticle.class)
      1 @Mixin(SuperGlueSelectionHandler.class)
      1 @Mixin(SuperGlueRemovalPacket.class)
      1 @Mixin(StructureUtils.class)
      1 @Mixin(StructureTemplate.class)
      1 @Mixin(StockTickerInteractionHandler.class)
      1 @Mixin(StickerBlockEntity.class)
      1 @Mixin(StickerBlock.class)
      1 @Mixin(SoundEngine.class)
      1 @Mixin(SmartBlockEntityRenderer.class)
      1 @Mixin(SignBlockEntity.class)
      1 @Mixin(ShoulderSurfingImpl.class)
      1 @Mixin(ShoulderSurfingCamera.class)
      1 @Mixin(ShaderSources.class)
      1 @Mixin(ServerboundMovePlayerPacket.class)
      1 @Mixin(ServerConnectionListener.class)
      1 @Mixin(SeatBlock.class)
      1 @Mixin(SchematicTransformation.class)
      1 @Mixin(SchematicToolBase.class)
      1 @Mixin(SchematicRenderer.class)
      1 @Mixin(SchematicPrinter.class)
      1 @Mixin(SchematicPlacePacket.class)
      1 @Mixin(SchematicLevel.class)
      1 @Mixin(SchematicHandler.class)
      1 @Mixin(SchematicExport.class)
      1 @Mixin(SchematicAndQuillHandler.class)
      1 @Mixin(SawMovementBehaviour.class)
      1 @Mixin(SawBlockEntity.class)
      1 @Mixin(SawBlock.class)
      1 @Mixin(SailBlock.class)
      1 @Mixin(SafeBlockEntityRenderer.class)
      1 @Mixin(RenderChunkRegion.class)
      1 @Mixin(RemotePlayer.class)
      1 @Mixin(RedstoneLinkNetworkHandler.class)
      1 @Mixin(RedstoneContactBlock.class)
      1 @Mixin(RaycastHelper.class)
      1 @Mixin(RayTracing.class)
      1 @Mixin(RandomPos.class)
      1 @Mixin(RadarRenderer.class)
      1 @Mixin(ProjectileUtil.class)
      1 @Mixin(ProjectileDispenseBehavior.class)
      1 @Mixin(Projectile.class)
      1 @Mixin(Program.class)
      1 @Mixin(PrimedTnt.class)
      1 @Mixin(PlacementClient.class)
      1 @Mixin(PipeConnection.class)
      1 @Mixin(Perspective.class)
      1 @Mixin(PersistentEntitySectionManager.class)
      1 @Mixin(PathfindingContext.class)
      1 @Mixin(PathNavigation.class)
      1 @Mixin(Path.class)
      1 @Mixin(ParticleEngine.class)
      1 @Mixin(Particle.class)
      1 @Mixin(Parrot.class)
      1 @Mixin(PackagePortTargetSelectionHandler.class)
      1 @Mixin(PackagePortTarget.ChainConveyorFrogportTarget.class)
      1 @Mixin(PackagePortPlacementPacket.class)
      1 @Mixin(PMWeather.class)
      1 @Mixin(Outline.class)
      1 @Mixin(OptionsScreen.class)
      1 @Mixin(OpenEndedPipe.class)
      1 @Mixin(ObjectPicker.class)
      1 @Mixin(NetherPortalBlock.class)
      1 @Mixin(MultiPlayerGameMode.class)
      1 @Mixin(MouseHandler.class)
      1 @Mixin(ModelBlockRenderer.class)
      1 @Mixin(ModelBlockRenderer.Cache.class)
      1 @Mixin(ModelBlockRenderer.AmbientOcclusionFace.class)
      1 @Mixin(MinecraftServer.class)
      1 @Mixin(MatrixBuffer.class)
      1 @Mixin(Matrix3d.class)
      1 @Mixin(LivingEntityRenderer.class)
      1 @Mixin(LinkRenderer.class)
      1 @Mixin(LineOutline.class)
      1 @Mixin(LightStorage.class)
      1 @Mixin(LevelChunkTicks.class)
      1 @Mixin(LevelChunkSection.class)
      1 @Mixin(LecternControllerBlockEntity.class)
      1 @Mixin(LecternControllerBlock.class)
      1 @Mixin(Leashable.class)
      1 @Mixin(LOD.class)
      1 @Mixin(ItemEntity.class)
      1 @Mixin(Item.class)
      1 @Mixin(IntegratedServer.class)
      1 @Mixin(HosePulleyFluidHandler.class)
      1 @Mixin(HosePulleyBlockEntity.class)
      1 @Mixin(HitResult.class)
      1 @Mixin(HarvesterRenderer.class)
      1 @Mixin(HarvesterBlockEntity.class)
      1 @Mixin(HarvesterBlock.class)
      1 @Mixin(Gui.class)
      1 @Mixin(GroundPathNavigation.class)
      1 @Mixin(GameTestInfo.class)
      1 @Mixin(GameEventDispatcher.class)
      1 @Mixin(FunnelBlock.class)
      1 @Mixin(FrogportVisual.class)
      1 @Mixin(FrogportRenderer.class)
      1 @Mixin(FrogportBlockEntity.class)
      1 @Mixin(FrogportBlock.class)
      1 @Mixin(FogRenderer.class)
      1 @Mixin(FlywheelBlockEntity.class)
      1 @Mixin(FlyNodeEvaluator.class)
      1 @Mixin(FlowingFluid.class)
      1 @Mixin(FlameParticle.class)
      1 @Mixin(FilteringRenderer.class)
      1 @Mixin(FanProcessingType.class)
      1 @Mixin(FallingBlockEntity.class)
      1 @Mixin(FactoryPanelConnectionHandler.class)
      1 @Mixin(ExtendedShader.class)
      1 @Mixin(Explosion.class)
      1 @Mixin(ExecuteCommand.class)
      1 @Mixin(EuclideanGameEventListenerRegistry.class)
      1 @Mixin(EnvironmentStorage.class)
      1 @Mixin(EntityType.class)
      1 @Mixin(EntityHelper.class)
      1 @Mixin(EntityHats.class)
      1 @Mixin(EntityGetter.class)
      1 @Mixin(EngineImpl.class)
      1 @Mixin(EnchantingTableBlockEntity.class)
      1 @Mixin(EncasedFanBlockEntity.class)
      1 @Mixin(EmbeddedEnvironment.class)
      1 @Mixin(ElevatorControlsHandler.class)
      1 @Mixin(EjectorBlockEntity.class)
      1 @Mixin(EjectorBlock.class)
      1 @Mixin(EatBlockGoal.class)
      1 @Mixin(DisplayLinkBlockEntity.class)
      1 @Mixin(DisplayLinkBlock.class)
      1 @Mixin(DiscreteVoxelShape.class)
      1 @Mixin(DepotRenderer.class)
      1 @Mixin(DeployerBlockEntity.class)
      1 @Mixin(DeployTool.class)
      1 @Mixin(DebugScreenOverlay.class)
      1 @Mixin(DebugRenderer.class)
      1 @Mixin(DataCommands.class)
      1 @Mixin(CurvedTrackDestroyPacket.class)
      1 @Mixin(CrushingWheelControllerBlockEntity.class)
      1 @Mixin(CrushingWheelBlock.class)
      1 @Mixin(CreatePlugin.class)
      1 @Mixin(ContraptionVisual.class)
      1 @Mixin(ContraptionHandlerClient.class)
      1 @Mixin(Contraption.class)
      1 @Mixin(Connection.class)
      1 @Mixin(ConduitRenderer.class)
      1 @Mixin(ConduitBlockEntity.class)
      1 @Mixin(CompassItemPropertyFunction.class)
      1 @Mixin(CombatTracker.class)
      1 @Mixin(CollisionContext.class)
      1 @Mixin(ClipContext.class)
      1 @Mixin(ClientboundTeleportEntityPacket.class)
      1 @Mixin(ClientboundMoveEntityPacket.PosRot.class)
      1 @Mixin(ClientboundMoveEntityPacket.Pos.class)
      1 @Mixin(ClientboundBlockUpdatePacket.class)
      1 @Mixin(ClientSuggestionProvider.class)
      1 @Mixin(ClientPlayNetworkAddon.class)
      1 @Mixin(ClientChunkCache.class)
      1 @Mixin(ClientChunkCache.Storage.class)
      1 @Mixin(ClickToLinkBlockItem.class)
      1 @Mixin(ChuteBlockEntity.class)
      1 @Mixin(ChunkBorderRenderer.class)
      1 @Mixin(ChasingAABBOutline.class)
      1 @Mixin(Channel.class)
      1 @Mixin(ChainPackageInteractionPacket.class)
      1 @Mixin(ChainPackageInteractionHandler.class)
      1 @Mixin(ChainConveyorShape.class)
      1 @Mixin(ChainConveyorRidingHandler.class)
      1 @Mixin(ChainConveyorRenderer.class)
      1 @Mixin(ChainConveyorInteractionHandler.class)
      1 @Mixin(ChainConveyorBlock.class)
      1 @Mixin(CarriageContraptionVisual.class)
      1 @Mixin(CapManipulationBehaviourBase.class)
      1 @Mixin(CameraType.class)
      1 @Mixin(CameraStandEntity.class)
      1 @Mixin(CameraPoses.class)
      1 @Mixin(BoilerData.class)
      1 @Mixin(BlueprintEntity.class)
      1 @Mixin(BlockState.class)
      1 @Mixin(BlockPlaceContext.class)
      1 @Mixin(BlockMarker.class)
      1 @Mixin(BlockEntityRenderDispatcher.class)
      1 @Mixin(BlockEntityBehaviour.class)
      1 @Mixin(BlockClusterOutline.class)
      1 @Mixin(BlockBreakingMovementBehaviour.class)
      1 @Mixin(BlockBreakingKineticBlockEntity.class)
      1 @Mixin(BlockAndTintGetter.class)
      1 @Mixin(BlockAccessorImpl.class)
      1 @Mixin(BlazeBurnerBlockEntity.class)
      1 @Mixin(BitSetDiscreteVoxelShape.class)
      1 @Mixin(Biome.class)
      1 @Mixin(BigOutlines.class)
      1 @Mixin(BeltMovementHandler.class)
      1 @Mixin(BeltBlockEntity.class)
      1 @Mixin(BeltBlock.class)
      1 @Mixin(BellBlock.class)
      1 @Mixin(BeehiveBlock.class)
      1 @Mixin(BasinOperatingBlockEntity.class)
      1 @Mixin(BasinBlockEntity.class)
      1 @Mixin(BasinBlock.class)
      1 @Mixin(BackpackPickupEvents.class)
      1 @Mixin(ArmInteractionPointHandler.class)
      1 @Mixin(ArmBlockEntity.class)
      1 @Mixin(ArgumentTypeInfos.class)
      1 @Mixin(AnemometerBlockEntity.class)
      1 @Mixin(AnemometerBlock.class)
      1 @Mixin(AllBlockEntityTypes.class)
      1 @Mixin(AirParticle.class)
      1 @Mixin(AirFlowParticle.class)
      1 @Mixin(AirCurrent.class)
      1 @Mixin(AbstractSoundInstance.class)
      1 @Mixin(AbstractMinecart.class)
      1 @Mixin(AbstractFurnaceBlockEntity.class)
      1 @Mixin(AbstractContraptionEntity.class)
      1 @Mixin(AbstractBlockEntityVisual.class)
      1 @Mixin(AbstractArrow.class)
```

VS2 1.20.1 (`vs2\grep-mixin-lines.txt`):

```
     10 @Mixin(Entity.class)
      8 @Mixin(LevelRenderer.class)
      7 @Mixin(Player.class)
      5 @Mixin(ChunkMap.class)
      4 @Mixin(ServerPlayer.class)
      4 @Mixin(LivingEntity.class)
      4 @Mixin(Level.class)
      4 @Mixin(ClientLevel.class)
      3 @Mixin(targets = {
      3 @Mixin(ViewArea.class)
      3 @Mixin(ServerLevel.class)
      3 @Mixin(ServerGamePacketListenerImpl.class)
      3 @Mixin(PersistentEntitySectionManager.class)
      3 @Mixin(Listener.class)
      3 @Mixin(IForgePlayer.class)
      3 @Mixin(ControlledContraptionEntity.class)
      2 @Mixin(value = SodiumWorldRenderer.class, remap = false, priority = 1100)
      2 @Mixin(value = LevelChunk.class, priority = 1500)
      2 @Mixin(value = ChainConveyorBlockEntity.class)
      2 @Mixin(value = BlockEntityRenderHelper.class, remap = false)
      2 @Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler")
      2 @Mixin(ServerChunkCache.class)
      2 @Mixin(Projectile.class)
      2 @Mixin(MinecraftServer.class)
      2 @Mixin(LocalPlayer.class)
      2 @Mixin(LevelRenderer.RenderChunkInfo.class)
      2 @Mixin(LevelChunk.class)
      2 @Mixin(LayerLightSectionStorage.class)
      2 @Mixin(KeyMapping.class)
      2 @Mixin(FireBlock.class)
      2 @Mixin(EntityRenderer.class)
      2 @Mixin(EMFAnimationEntityContext.class)
      2 @Mixin(DebugRenderer.class)
      2 @Mixin(ContraptionHandlerClient.class)
      2 @Mixin(ContainerEntity.class)
      2 @Mixin(ClientPacketListener.class)
      2 @Mixin(ClientChunkCache.class)
      2 @Mixin(ClientChunkCache.Storage.class)
      2 @Mixin(ChunkRenderDispatcher.RenderChunk.class)
      2 @Mixin(ChunkHolder.class)
      2 @Mixin(ChainCollisionEntity.class)
      2 @Mixin(Camera.class)
      2 @Mixin(BlockItem.class)
      2 @Mixin(BlockBreakingKineticBlockEntity.class)
      2 @Mixin(AbstractContraptionEntity.class)
      1 @Mixin(value={SchematicToolBase.class})
      1 @Mixin(value={DeployTool.class})
      1 @Mixin(value = {SchematicTransformation.class}, remap = false)
      1 @Mixin(value = { IceBlock.class, SnowLayerBlock.class })
      1 @Mixin(value = {
      1 @Mixin(value = WirelessNetwork.class, priority = 2000)
      1 @Mixin(value = WirelessModemPeripheral.class, priority = 2000)
      1 @Mixin(value = TurtleMoveCommand.class, priority = 2000)
      1 @Mixin(value = TurtleBrain.class, priority = 2000)
      1 @Mixin(value = StructureTemplate.class)
      1 @Mixin(value = SpeakerSound.class, priority = 2000)
      1 @Mixin(value = SpeakerPosition.class, priority = 2000)
      1 @Mixin(value = SafeBlockEntityRenderer.class, remap = false)
      1 @Mixin(value = RenderSectionManager.class, remap = false)
      1 @Mixin(value = RenderSection.class, remap = false)
      1 @Mixin(value = RenderResizableCuboid.class, remap = false)
      1 @Mixin(value = RenderData.Builder.class, remap = false)
      1 @Mixin(value = LevelRenderer.class, priority = 999)
      1 @Mixin(value = EntityRenderDispatcher.class, priority = 500)
      1 @Mixin(value = ContraptionVisual.class)
      1 @Mixin(value = ChunkMap.class, priority = 1100)
      1 @Mixin(value = ChainLink.class, remap = false)
      1 @Mixin(value = ChainKnotEntityRenderer.class, remap = false)
      1 @Mixin(value = ChainKnotEntity.class, remap = false)
      1 @Mixin(value = ChainConveyorVisual.class, remap = false)
      1 @Mixin(value = ChainConveyorRidingHandler.class)
      1 @Mixin(value = BlockEntity.class, priority = 1200)
      1 @Mixin(value = AirCurrent.class)
      1 @Mixin(targets = {"net.mcreator.createstuffadditions.procedures.GrapplinWhiskItemInHandTickProcedure"})
      1 @Mixin(targets = {"net.mcreator.createstuffadditions.procedures.GrapplinWhiskChainsLineProcedure"})
      1 @Mixin(targets = {"net.createmod.catnip.ghostblock.GhostBlockRenderer$DefaultGhostBlockRenderer",
      1 @Mixin(targets = {"dev.imabad.theatrical.client.blockentities.MovingLightRenderer$1"})
      1 @Mixin(targets = {"dev.imabad.theatrical.client.blockentities.MovingLightRenderer"})
      1 @Mixin(targets = {"dev.imabad.theatrical.client.blockentities.FixtureRenderer$1"})
      1 @Mixin(targets = { "fuzs.echochest.world.level.block.entity.EchoChestBlockEntity" }, remap = false)
      1 @Mixin(targets = "rbasamoyai.createbigcannons.utils.CBCUtils")
      1 @Mixin(targets = "rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile")
      1 @Mixin(targets = "rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity")
      1 @Mixin(targets = "fuzs.echochest.world.level.block.entity.EchoChestListener")
      1 @Mixin(targets = "dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl")
      1 @Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
      1 @Mixin(targets = "com.simibubi.create.content.trains.track.TrackVisual$BezierTrackVisual")
      1 @Mixin(targets = "com.lumintorious.tfcambiental.api.EnvironmentalTemperatureProvider")
      1 @Mixin(targets = "cofh.core.block.entity.TileCoFH", remap = false)
      1 @Mixin(targets = "blusunrize.immersiveengineering.common.gui.BlockEntityInventory")
      1 @Mixin(targets = "at.petrak.hexcasting.common.casting.actions.spells.great.OpTeleport$Spell")
      1 @Mixin(WorldBorder.class)
      1 @Mixin(WorkAtPoi.class)
      1 @Mixin(WirelessModemPeripheral.class)
      1 @Mixin(WaterFluid.class)
      1 @Mixin(WalkNodeEvaluator.class)
      1 @Mixin(VoxelShapeComponents.class)
      1 @Mixin(VoidLinkBehaviour.class)
      1 @Mixin(VisualizerRegistry.class)
      1 @Mixin(VistaLevelRenderer.class)
      1 @Mixin(VisibleSectionDiscovery.class)
      1 @Mixin(VineBlock.class)
      1 @Mixin(VillageSiege.class)
      1 @Mixin(ViewFinderController.class)
      1 @Mixin(ViewFinderAccess.Block.class)
      1 @Mixin(ValueBox.class)
      1 @Mixin(ValidateNearbyPoi.class)
      1 @Mixin(TurntableHandler.class)
      1 @Mixin(TurntableBlock.class)
      1 @Mixin(TransientEntitySectionManager.class)
      1 @Mixin(TrainRelocator.class)
      1 @Mixin(TrainRelocationPacket.class)
      1 @Mixin(TrackVisual.class)
      1 @Mixin(TrackTargetingClient.class)
      1 @Mixin(TrackPlacement.class)
      1 @Mixin(TrackNodeLocation.class)
      1 @Mixin(TrackBlockOutline.class)
      1 @Mixin(TrackBlockItem.class)
      1 @Mixin(ToolboxHandlerClient.class)
      1 @Mixin(ToolboxHandler.class)
      1 @Mixin(TileModel.class)
      1 @Mixin(TileEntityMultiblock.class)
      1 @Mixin(Ticker.class)
      1 @Mixin(ThreadedLevelLightEngine.class)
      1 @Mixin(SymmetryMirror.class)
      1 @Mixin(SymmetryHandler.class)
      1 @Mixin(SwimNodeEvaluator.class)
      1 @Mixin(SuperGlueSelectionHandler.class)
      1 @Mixin(SuperGlueRemovalPacket.class)
      1 @Mixin(StructureTemplate.Palette.class)
      1 @Mixin(StockKeeperCategoryMenu.class)
      1 @Mixin(StickerBlock.class)
      1 @Mixin(StairBlock.class)
      1 @Mixin(SpeakerSound.class)
      1 @Mixin(SoundScapes.class)
      1 @Mixin(SoundEngine.class)
      1 @Mixin(SodiumWorldRenderer.class)
      1 @Mixin(SodiumOptionsGUI.class)
      1 @Mixin(SmartBlockEntityRenderer.class)
      1 @Mixin(SlabBlock.class)
      1 @Mixin(SkyLightSectionStorage.class)
      1 @Mixin(SimpleSoundInstance.class)
      1 @Mixin(SidedFilteringBehaviour.class)
      1 @Mixin(SheetedDecalTextureGenerator.class)
      1 @Mixin(SharedDepotBlockMethods.class)
      1 @Mixin(SetClosestHomeAsWalkTarget.class)
      1 @Mixin(ServerboundSetFixedCannonMountValuePacket.class)
      1 @Mixin(ServerPlayerGameMode.class)
      1 @Mixin(ServerLinkableHolder.class)
      1 @Mixin(ServerLevel.EntityCallbacks.class)
      1 @Mixin(ServerEntity.class)
      1 @Mixin(SeatEntity.class)
      1 @Mixin(SeatBlock.class)
      1 @Mixin(ScrollValueBehaviour.class)
      1 @Mixin(SchematicannonRenderer.class)
      1 @Mixin(SchematicannonBlockEntity.class)
      1 @Mixin(SawMovementBehaviour.class)
      1 @Mixin(SawBlockEntity.class)
      1 @Mixin(RouterMenu.class)
      1 @Mixin(RitchiesProjectileLib.class)
      1 @Mixin(ResourceKey.class)
      1 @Mixin(RenderSectionManager.class)
      1 @Mixin(RenderHelperKt.class)
      1 @Mixin(RenderContextImpl.class)
      1 @Mixin(RedstoneLinkNetworkHandler.class)
      1 @Mixin(RedstoneContactBlock.class)
      1 @Mixin(RealmsConnect.class)
      1 @Mixin(ReachEntityAttributes.class)
      1 @Mixin(Raids.class)
      1 @Mixin(Raid.class)
      1 @Mixin(RadiationManager.class)
      1 @Mixin(ProjectorDepthRenderer.class)
      1 @Mixin(ProjectileUtil.class)
      1 @Mixin(PoiManager.class)
      1 @Mixin(PlayerList.class)
      1 @Mixin(PlacementSimulationServerLevel.class)
      1 @Mixin(PlacementClient.class)
      1 @Mixin(PathfinderMob.class)
      1 @Mixin(PathNavigationRegion.class)
      1 @Mixin(PathNavigation.class)
      1 @Mixin(PathFinder.class)
      1 @Mixin(Path.class)
      1 @Mixin(Particle.class)
      1 @Mixin(PackagePortTargetSelectionHandler.class)
      1 @Mixin(PackagePortPlacementPacket.class)
      1 @Mixin(PackageItem.class)
      1 @Mixin(PackageEntity.class)
      1 @Mixin(Outline.class)
      1 @Mixin(Outline.OutlineParams.class)
      1 @Mixin(OpTeleport.class)
      1 @Mixin(OpNetworkTeleport.class)
      1 @Mixin(OpLinkOthers.class)
      1 @Mixin(OpLink.class)
      1 @Mixin(OpEntityRaycast.class)
      1 @Mixin(NozzleBlockEntity.class)
      1 @Mixin(NewChunkTrackingGraph.class)
      1 @Mixin(NaturalSpawner.class)
      1 @Mixin(MyBuiltChunkStorage.class)
      1 @Mixin(MoveToTargetSink.class)
      1 @Mixin(MoveToBlockGoal.class)
      1 @Mixin(MoveControl.class)
      1 @Mixin(MonitorRenderer.class)
      1 @Mixin(MobRenderer.class)
      1 @Mixin(Mob.class)
      1 @Mixin(Minecraft.class)
      1 @Mixin(MechanicalBearingBlockEntity.class)
      1 @Mixin(Matrix4f.class)
      1 @Mixin(Matrix3d.class)
      1 @Mixin(MCAWorld.class)
      1 @Mixin(LivingEntityRenderer.class)
      1 @Mixin(LinkRenderer.class)
      1 @Mixin(LinkBehaviour.class)
      1 @Mixin(LightningBolt.class)
      1 @Mixin(LightManager.class)
      1 @Mixin(LightEngine.class)
      1 @Mixin(LevelRendererCameraState.class)
      1 @Mixin(LevelLightEngine.class)
      1 @Mixin(LavaFluid.class)
      1 @Mixin(LaunchedItem.class)
      1 @Mixin(LandRandomPos.class)
      1 @Mixin(LOD.class)
      1 @Mixin(JitteredLinearRetry.class)
      1 @Mixin(InfraredPacketEntity.class)
      1 @Mixin(ITrackBlock.class)
      1 @Mixin(IOneUserInteractable.class)
      1 @Mixin(HosePulleyBlockEntity.class)
      1 @Mixin(HiresModelManager.class)
      1 @Mixin(HexAdditionalRenderers.class)
      1 @Mixin(Gui.class)
      1 @Mixin(GoalUtils.class)
      1 @Mixin(GoToClosestVillage.class)
      1 @Mixin(GhostBlockParams.class)
      1 @Mixin(GameRenderer.class)
      1 @Mixin(GameEventDispatcher.class)
      1 @Mixin(FunnelBlock.class)
      1 @Mixin(Frustum.class)
      1 @Mixin(FrogportVisual.class)
      1 @Mixin(FrogportRenderer.class)
      1 @Mixin(FrogportBlockEntity.class)
      1 @Mixin(FrequencyGrid.class)
      1 @Mixin(FlowingFluid.class)
      1 @Mixin(FixedCannonMountScrollValueBehaviour.class)
      1 @Mixin(FilteringRenderer.class)
      1 @Mixin(FilteringBehaviour.class)
      1 @Mixin(FakeServerLevel.class)
      1 @Mixin(FactoryPanelConnectionHandler.class)
      1 @Mixin(Explosion.class)
      1 @Mixin(EuclideanGameEventListenerRegistry.class)
      1 @Mixin(EntityVisibilityTester.class)
      1 @Mixin(EntitySelectorParser.class)
      1 @Mixin(EntitySectionStorage.class)
      1 @Mixin(EntitySection.class)
      1 @Mixin(EntityLauncher.class)
      1 @Mixin(EnchantmentTableBlockEntity.class)
      1 @Mixin(EnchantmentMenu.class)
      1 @Mixin(EncasedFanBlockEntity.class)
      1 @Mixin(ElevatorControlsHandler.class)
      1 @Mixin(EjectorBlockEntity.class)
      1 @Mixin(EjectorBlock.class)
      1 @Mixin(EdgeInteractionRenderer.class)
      1 @Mixin(EdgeInteractionHandler.class)
      1 @Mixin(DistanceManager.class)
      1 @Mixin(DeployerHandler.class)
      1 @Mixin(DeployerBlockEntity.class)
      1 @Mixin(DefaultRandomPos.class)
      1 @Mixin(DebugScreenOverlay.class)
      1 @Mixin(DebugPackets.class)
      1 @Mixin(CustomLecternRenderer.class)
      1 @Mixin(CurvedTrackDestroyPacket.class)
      1 @Mixin(CullingBlockEntityIterator.class)
      1 @Mixin(CrushingWheelControllerBlockEntity.class)
      1 @Mixin(CrushingWheelBlock.class)
      1 @Mixin(ControlsInputPacket.class)
      1 @Mixin(ContraptionMatrices.class)
      1 @Mixin(ContraptionEntityRenderer.class)
      1 @Mixin(ContraptionControlsRenderer.class)
      1 @Mixin(ContraptionCollider.class)
      1 @Mixin(Contraption.class)
      1 @Mixin(ConnectScreen.class)
      1 @Mixin(ConduitBlockEntity.class)
      1 @Mixin(ComputerRenderer.class)
      1 @Mixin(Commands.class)
      1 @Mixin(ClipboardScreen.class)
      1 @Mixin(ClipContext.class)
      1 @Mixin(ClientboundSectionBlocksUpdatePacket.class)
      1 @Mixin(ClientSuggestionProvider.class)
      1 @Mixin(ClientPacketHandler.class)
      1 @Mixin(ChuteBlockEntity.class)
      1 @Mixin(ChunkTracker.class)
      1 @Mixin(ChunkStatus.class)
      1 @Mixin(ChunkMap.TrackedEntity.class)
      1 @Mixin(ChunkGeneratorTwilight.class)
      1 @Mixin(ChunkGenerator.class)
      1 @Mixin(Channel.class)
      1 @Mixin(Chainable.class)
      1 @Mixin(ChainPackageInteractionHandler.class)
      1 @Mixin(ChainKnotEntityRenderer.class)
      1 @Mixin(ChainConveyorRenderer.class)
      1 @Mixin(ChainConveyorInteractionHandler.class)
      1 @Mixin(ChainConveyorConnectionHandler.class)
      1 @Mixin(ChainConveyorBlockEntity.class)
      1 @Mixin(CasingBlockEntityRenderer.class)
      1 @Mixin(CarvedPumpkinBlock.class)
      1 @Mixin(CarriageSounds.class)
      1 @Mixin(CarriageContraptionEntity.class)
      1 @Mixin(BmMap.class)
      1 @Mixin(BlockPlaceContext.class)
      1 @Mixin(BlockEntityRenderHelper.class)
      1 @Mixin(BlockEntityRenderDispatcher.class)
      1 @Mixin(BlockEntityConfigurationPacket.class)
      1 @Mixin(BlockCollisions.class)
      1 @Mixin(BlockClusterOutline.class)
      1 @Mixin(BlockBreakingMovementBehaviour.class)
      1 @Mixin(BlazeBurnerBlockEntity.class)
      1 @Mixin(Biome.class)
      1 @Mixin(BeltMovementHandler.class)
      1 @Mixin(BeeLocateHiveGoal.class)
      1 @Mixin(Bee.class)
      1 @Mixin(Bee.BeePollinateGoal.class)
      1 @Mixin(Bee.BeeGrowCropGoal.class)
      1 @Mixin(Bee.BeeEnterHiveGoal.class)
      1 @Mixin(BaseLightBlockEntity.class)
      1 @Mixin(AssignProfessionFromJobSite.class)
      1 @Mixin(ArgumentTypeInfos.class)
      1 @Mixin(AirFlowParticle.class)
      1 @Mixin(AirAndWaterRandomPos.class)
      1 @Mixin(AbstractTickableSoundInstance.class)
      1 @Mixin(AbstractContainerMenu.class)
      1 @Mixin(AbstractBlockEntityVisual.class)
      1 @Mixin(AABBOutline.class)
      1 //@Mixin(ViewArea.class)
      1 //@Mixin(PedestalBlockEntity.class)
      1 //@Mixin(LevelRenderer.class)
      1 //@Mixin(LesserSentinelRenderer.class)
      1 //@Mixin(ContraptionRenderInfo.class)
```

## Appendix C. VS2 1.21.1/main vanilla mixin targets (`vs2-1.21.1\targets.txt`)

```
MIXIN FILES: 430
UNIQUE TARGET TYPES: 350  VANILLA: 131
   com.mojang.blaze3d.audio.Channel 1
   com.mojang.blaze3d.audio.Listener 1
   com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator 1
   net.minecraft.client.Camera 2
   net.minecraft.client.KeyMapping 2
   net.minecraft.client.Minecraft 1
   net.minecraft.client.gui.Gui 1
   net.minecraft.client.gui.components.DebugScreenOverlay 1
   net.minecraft.client.gui.screens.ConnectScreen 1
   net.minecraft.client.multiplayer.ClientChunkCache 2
   net.minecraft.client.multiplayer.ClientChunkCache.Storage 1
   net.minecraft.client.multiplayer.ClientLevel 4
   net.minecraft.client.multiplayer.ClientPacketListener 2
   net.minecraft.client.multiplayer.ClientSuggestionProvider 1
   net.minecraft.client.particle.Particle 1
   net.minecraft.client.player.LocalPlayer 2
   net.minecraft.client.renderer.GameRenderer 1
   net.minecraft.client.renderer.LevelRenderer 7
   net.minecraft.client.renderer.LevelRenderer.RenderChunkInfo 1
   net.minecraft.client.renderer.ViewArea 3
   net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher 1
   net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk 1
   net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection 1
   net.minecraft.client.renderer.culling.Frustum 1
   net.minecraft.client.renderer.debug.DebugRenderer 2
   net.minecraft.client.renderer.entity.EntityRenderDispatcher 1
   net.minecraft.client.renderer.entity.EntityRenderer 3
   net.minecraft.client.resources.sounds.AbstractTickableSoundInstance 1
   net.minecraft.client.resources.sounds.SimpleSoundInstance 1
   net.minecraft.client.sounds.SoundEngine 1
   net.minecraft.commands.Commands 1
   net.minecraft.commands.arguments.selector.EntitySelectorParser 1
   net.minecraft.commands.synchronization.ArgumentTypeInfos 1
   net.minecraft.network.Connection 1
   net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket 1
   net.minecraft.network.protocol.game.DebugPackets 1
   net.minecraft.realms.RealmsConnect 1
   net.minecraft.resources.ResourceKey 1
   net.minecraft.server.MinecraftServer 2
   net.minecraft.server.level.ChunkHolder 2
   net.minecraft.server.level.ChunkMap 7
   net.minecraft.server.level.ChunkMap.TrackedEntity 1
   net.minecraft.server.level.DistanceManager 1
   net.minecraft.server.level.GenerationChunkHolder 1
   net.minecraft.server.level.ServerChunkCache 2
   net.minecraft.server.level.ServerEntity 1
   net.minecraft.server.level.ServerLevel 3
   net.minecraft.server.level.ServerLevel.EntityCallbacks 1
   net.minecraft.server.level.ServerPlayer 4
   net.minecraft.server.level.ThreadedLevelLightEngine 1
   net.minecraft.server.network.ServerGamePacketListenerImpl 2
   net.minecraft.server.players.PlayerList 1
   net.minecraft.world.entity.Entity 10
   net.minecraft.world.entity.LivingEntity 4
   net.minecraft.world.entity.Mob 1
   net.minecraft.world.entity.PathfinderMob 1
   net.minecraft.world.entity.ai.behavior.AcquirePoi.JitteredLinearRetry 1
   net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite 1
   net.minecraft.world.entity.ai.behavior.GoToClosestVillage 1
   net.minecraft.world.entity.ai.behavior.MoveToTargetSink 1
   net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget 1
   net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi 1
   net.minecraft.world.entity.ai.behavior.WorkAtPoi 1
   net.minecraft.world.entity.ai.control.MoveControl 1
   net.minecraft.world.entity.ai.goal.MoveToBlockGoal 1
   net.minecraft.world.entity.ai.navigation.PathNavigation 1
   net.minecraft.world.entity.ai.util.AirAndWaterRandomPos 1
   net.minecraft.world.entity.ai.util.DefaultRandomPos 1
   net.minecraft.world.entity.ai.util.GoalUtils 1
   net.minecraft.world.entity.ai.util.LandRandomPos 1
   net.minecraft.world.entity.ai.village.VillageSiege 1
   net.minecraft.world.entity.ai.village.poi.PoiManager 1
   net.minecraft.world.entity.animal.Bee 1
   net.minecraft.world.entity.animal.Bee.BeeEnterHiveGoal 1
   net.minecraft.world.entity.animal.Bee.BeeGrowCropGoal 1
   net.minecraft.world.entity.animal.Bee.BeeLocateHiveGoal 1
   net.minecraft.world.entity.animal.Bee.BeePollinateGoal 1
   net.minecraft.world.entity.player.Player 7
   net.minecraft.world.entity.projectile.Projectile 2
   net.minecraft.world.entity.projectile.ProjectileUtil 1
   net.minecraft.world.entity.raid.Raid 1
   net.minecraft.world.entity.raid.Raids 1
   net.minecraft.world.entity.vehicle.ContainerEntity 2
   net.minecraft.world.inventory.AbstractContainerMenu 1
   net.minecraft.world.inventory.EnchantmentMenu 1
   net.minecraft.world.item.BlockItem 2
   net.minecraft.world.item.context.BlockPlaceContext 1
   net.minecraft.world.level.BlockCollisions 1
   net.minecraft.world.level.BlockGetter 1
   net.minecraft.world.level.ClipContext 2
   net.minecraft.world.level.Explosion 1
   net.minecraft.world.level.Level 3
   net.minecraft.world.level.NaturalSpawner 1
   net.minecraft.world.level.PathNavigationRegion 1
   net.minecraft.world.level.biome.Biome 1
   net.minecraft.world.level.block.FireBlock 2
   net.minecraft.world.level.block.IceBlock 1
   net.minecraft.world.level.block.SlabBlock 1
   net.minecraft.world.level.block.SnowLayerBlock 1
   net.minecraft.world.level.block.StairBlock 1
   net.minecraft.world.level.block.VineBlock 1
   net.minecraft.world.level.block.entity.BlockEntity 1
   net.minecraft.world.level.block.entity.ConduitBlockEntity 1
   net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity 1
   net.minecraft.world.level.border.WorldBorder 1
   net.minecraft.world.level.chunk.ChunkGenerator 1
   net.minecraft.world.level.chunk.LevelChunk 4
   net.minecraft.world.level.chunk.status.ChunkStatusTasks 1
   net.minecraft.world.level.entity.EntitySection 1
   net.minecraft.world.level.entity.EntitySectionStorage 1
   net.minecraft.world.level.entity.PersistentEntitySectionManager 3
   net.minecraft.world.level.entity.TransientEntitySectionManager 1
   net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry 1
   net.minecraft.world.level.gameevent.GameEventDispatcher 1
   net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener 2
   net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Ticker 1
   net.minecraft.world.level.levelgen.FlatLevelSource 1
   net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator 1
   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate 1
   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette 1
   net.minecraft.world.level.lighting.LayerLightSectionStorage 2
   net.minecraft.world.level.lighting.LevelLightEngine 1
   net.minecraft.world.level.lighting.LightEngine 1
   net.minecraft.world.level.lighting.SkyLightSectionStorage 1
   net.minecraft.world.level.material.FlowingFluid 1
   net.minecraft.world.level.material.LavaFluid 1
   net.minecraft.world.level.material.WaterFluid 1
   net.minecraft.world.level.pathfinder.Path 1
   net.minecraft.world.level.pathfinder.PathFinder 1
   net.minecraft.world.level.pathfinder.SwimNodeEvaluator 1
   net.minecraft.world.level.pathfinder.WalkNodeEvaluator 1
```
