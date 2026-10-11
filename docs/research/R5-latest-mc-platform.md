# R5: Latest Minecraft Java platform, for Apoapsis

Research node R5. Facts only, for the Opus designer's feasibility call. Research date: 2026-10-11.

Conventions:
- [S#] points to the source list in section 9. Every non-obvious fact has one.
- UNVERIFIED means the fact could not be confirmed from a fetched source. Section 8 collects them.
- [secondary] means the fact came from a search snippet or a third-party page that was not opened in full, or it is a claim that was not checked against the primary page.
- Dates are as printed by the source. Modrinth timestamps are UTC.

Method notes:
- Every fetch of a minecraft.net article timed out (about 8 attempts). Mojang primary text is therefore replaced by minecraft.wiki pages, fabricmc.net and docs.neoforged.net pages, and the Modrinth API.
- WebSearch was cut off by a tool quota after 13 successful searches. The Create 26.x, Quilt Loader and Valkyrien Skies 2 checks were attempted through direct source reads instead.
- Modrinth API reads were direct reads of Modrinth's public API (project, version and tag endpoints, plus its search endpoint for counts). They are not web-search results.

---

## 0. Summary

- Latest Java release: **26.3, "Wilderness Bound", released 2026-09-15**. Minimum Java: **Java SE 25**. Data pack format 121.0, resource pack format 97.1. [S1][S2][S11]
- Latest snapshot as of 2026-10-11: **26.4-snapshot-3, released 2026-10-06**. Earlier 26.4 snapshots: 2026-09-22 and 2026-09-29. 26.4 has no release date; the wiki lists it for Q4 2026. [S3][S4][S5][S2]
- Versioning: from 2026 the scheme is year.drop.hotfix (26.x). The last 1.x release was 1.21.11 (2025-12-09). [S2][S43][secondary: minecraft.net numbering article, not fetched]
- Deobfuscation: Mojang announced removal on **2025-10-29** (per minecraft.wiki; see section 2). First unobfuscated build: **26.1 Snapshot 1**. [S8][S9]
- Loader builds for 26.3 as of 2026-10-11: NeoForge **26.3.0.69-beta** (maven latest), Forge **66.0.10** (2026-10-09), Fabric Loader **0.19.5** with Fabric Loom **1.17**. [S26][S27][S11]

---

## 1. Latest version, versioning scheme, release list

### 1.1 Versioning scheme

- The 1.x line ended at 1.21.11 (Mounts of Mayhem, 2025-12-09). Mojang skipped 1.22. [S43][S2][secondary: gameserverkings summary]
- From 2026 the Java version is `26.<drop>[.<hotfix>]`. Initial releases have no hotfix digit. Example: 26.1.1 is the first hotfix on the 26.1 drop. [S43][secondary: hostvalues/gameserverkings summaries]
- Drops in 2026 so far: 26.1 (2026-03-24), 26.2 (2026-06-16), 26.3 (2026-09-15). A 4th drop (26.4) is planned for Q4 2026. [S2][S3]
- Mojang's stated reason is more frequent, smaller drops. [secondary: search summaries]
- NeoForge uses four components from 26.1: the Minecraft version plus the NeoForge build number, for example `26.3.0.69-beta`. [S13]
- Java and Bedrock share the year prefix but count differently (Java 26.1 corresponds to Bedrock 26.10). [secondary: search summary]

### 1.2 Release list, 1.20.1 to 26.3

Dates are the minecraft.wiki release dates. Where the Modrinth tag date differs, it is noted. Modrinth tag dates are the times the version tag was created, so they can differ by a day.

| Release | Date (wiki) | Title | Headline change(s) | Source |
|---|---|---|---|---|
| 1.20.1 | 2023-06-12 | none | Serbian (Latin) language; realms menu and softlock fixes | [S43][S2] |
| 1.20.2 | 2023-09-21 | none | Villager trade rebalance experiment; /random command; function macros | [S43] (Modrinth tag 2023-09-20) |
| 1.20.3 | 2023-12-05 | "Bats and Pots" (retroactive) | Shield improvements; decorated pots store a stack; bat model and animations | [S43] (Modrinth tag 2023-12-04) |
| 1.20.4 | 2023-12-07 | none (hotfix) | Decorated pot fix. Last release that supported Java 17 to 20 and 32-bit OS | [S43] |
| 1.20.5 | 2024-04-23 | "Armored Paws" | Armadillo and wolf armor; item NBT replaced by data components; attributes gravity, step_height, safe_fall_distance added; jump_strength renamed (see section 4) | [S43][S24][S15] |
| 1.20.6 | 2024-04-29 | none (hotfix) | Trader llama inventory data-loss fix | [S43] |
| 1.21 | 2024-06-13 | "Tricky Trials" | Trial chambers, crafter block, mace, copper variants; data-driven enchantments and recipe changes (section 4) | [S43][S16] |
| 1.21.1 | 2024-08-08 | none | Exploit fixes (block entity swap); Tzotzil and Belarusian (Latin) languages | [S43][S2] |
| 1.21.2 | 2024-10-22 | "Bundles of Bravery" | Bundles; banner patterns; experimental winter drop features (pale garden, creaking) | [S43] |
| 1.21.3 | 2024-10-23 | none (hotfix) | Realms with resource packs fix; salmon fix; small salmon becomes medium | [S43] |
| 1.21.4 | 2024-12-03 | "The Garden Awakens" | Pale garden biome, creakings, resin | [S43] |
| 1.21.5 | 2025-03-25 | "Spring to Life" | Cold and warm mob variants; plant expansion; render pipeline rework (section 3) | [S43][S18] |
| 1.21.6 | 2025-06-17 | "Chase the Skies" | Happy ghast; player locator bar; craftable saddles | [S43] |
| 1.21.7 | 2025-06-30 | none | "Lava Chicken" music disc; painting; 16 fixes | [S43] |
| 1.21.8 | 2025-07-17 | none | Intel integrated graphics freeze fix; AMD shader-pack crash fix | [S43] |
| 1.21.9 | 2025-09-30 | "The Copper Age" | Copper tools and armor; copper golems; copper shelves and chests | [S43] |
| 1.21.10 | 2025-10-07 | none | Fixes: cobweb piston clipping, wind charge collisions, chunk loading on teleport | [S43] |
| 1.21.11 | 2025-12-09 | "Mounts of Mayhem" | Nautilus and zombie nautilus mobs; spears; Lunge enchantment; camel husk, parched, zombie horseman | [S43][S2] |
| 26.1 | 2026-03-24 | "Tiny Takeover" | Baby mob textures and models; golden dandelion; craftable name tags. First release with no obfuscated build; Java SE 25 | [S43][S2][S8] |
| 26.1.1 | 2026-04-01 | none (hotfix) | Chat message reporting fix (MC-307140) | [S43][S2] |
| 26.1.2 | 2026-04-09 | none (hotfix) | UI checkbox tweaks; spectator-mode attack exploit fix | [S43][S2] |
| 26.2 | 2026-06-16 | "Chaos Cubed" | Sulfur caves with sulfur cubes; cinnabar and sulfur blocks; experimental Vulkan renderer; friends list | [S43][S7][S2] |
| 26.3 | 2026-09-15 | "Wilderness Bound" | Dappled forest biome (poplar wood, shelf mushrooms); wool and concrete stairs and slabs; straw beds; abandoned camp structure. Also GLFW replaced by SDL; compostable, fuel and brewing fuel moved to item components | [S1][S11][S2][S3] |
| 26.4 snapshot 1 | 2026-09-22 | n/a | Default graphics option now equals "Prefer Vulkan"; no automatic API switch after a startup crash | [S6] |
| 26.4 snapshot 2 | 2026-09-29 | n/a | Render distance fog improvements; new sky occluder; redesigned F3 screen | [secondary: search summary] |
| 26.4 snapshot 3 | 2026-10-06 | n/a | Ice caves, icicles, frostbite mob, Freezing effect; Other Players menu; known Vulkan window-resize crash (MC-312444) | [S5][S2] |

Notes:
- 26.3 has a development date of 2026-06-23 and a release date of 2026-09-15. [S3]
- The 1.21.11 release date matches the start of the obfuscation change (see section 2). [S8][S2]

### 1.3 Other fixed points

- Latest release: 26.3, 2026-09-15. Confirmed by the minecraft.wiki page "Java Edition 26.3" [S1], the Modrinth tag list (`26.3` release, 2026-09-15T11:23:02Z) [S2], and the Fabric 26.3 post dated Sep 15, 2026 [S11].
- Latest snapshot: 26.4-snapshot-3, 2026-10-06T12:54:26Z per Modrinth [S2], confirmed by the minecraft.wiki page [S5]. The wiki "Development versions" page lists the sidebar item as "26.4-snap-3" [S4]. No 26.4-snapshot-4 was found; a Tuesday cadence would put one near 2026-10-13, which is an extrapolation and UNVERIFIED.

---

## 2. Deobfuscation

### 2.1 Timeline

| Date | Event | Source |
|---|---|---|
| 2019-09 | Mojang publishes official obfuscation maps | [secondary: JCG 2026-02-19] |
| 2025-10-29 | Mojang announces Java Edition will ship without obfuscation. Maps no longer published from the first snapshot after Mounts of Mayhem | [S8] (minecraft.wiki "Obfuscation map", History section, which links the minecraft.net article) |
| 2025-10-30 | A secondary report gives this as the announcement date | [secondary] (conflicts with 2025-10-29) |
| 2025-10-31 | Fabric publishes "Removing Obfuscation from Fabric" | [S9] (fetched) |
| 2025-12-16 | 26.1 Snapshot 1: first unobfuscated build, first to require Java 25 | [S8][secondary: JCG] |
| 2026-01 | McDeob 3.4.0 supports unobfuscated builds | [secondary: JCG] |
| 2026-02-03 | Quilt publishes "Bringing Quilt into the non-obfuscated era"; Quilt Mappings discontinued | [S40] (title and summary read; article body not read) |
| 2026-03-14 | Fabric for Minecraft 26.1 post: unobfuscated Loom plugin, Gradle and Java requirements | [S10] (fetched) |
| 2026-03-19 | 26.1 Release Candidate 1 is only an unobfuscated build | [secondary: wiki page title in search] |
| 2026-03-24 | 26.1 "Tiny Takeover" released with no obfuscated companion build. NeoForge 26.1 release post: Mojang parameter names now available | [S43][S13] (fetched) |

Primary confirmation of the announcement date is missing (minecraft.net timeouts). See section 8.

### 2.2 What it means for mappings

- **Obfuscated names** (for example `brc`) are no longer in the 26.1+ jars. The maps were last exported for 1.21.11, from snapshot 19w36a onward. [S8]
- **Intermediary** no longer exists, because the game uses Mojang's names at runtime. [S9]
- **Yarn**: Fabric says it cannot justify maintaining Yarn in its current form. It accepts contributions only for existing game versions. [S9] Fabric's 26.1 post says Fabric no longer officially supports Yarn. [S10]
- **Mojang Mappings (Mojmap)** are the names used in source. [S10] NeoForge's toolchain now uses Mojang names directly, and NeoForm versions are `<mc version>-<neoform build>`. [S13]
- **Parchment**: NeoForge says Mojang's parameter names make Parchment unnecessary. [S13] A secondary source says Parchment is still useful for parameter names and Javadoc. [secondary: JCG] This conflicts and is UNVERIFIED.
- **Quilt Mappings** discontinued (2026-02-03 news). [S40]
- **Gradle and Loom**: the unobfuscated plugin is `net.fabricmc.fabric-loom`. The old `fabric-loom-remap` plugin is replaced. It does not remap Minecraft or mods. `modImplementation` and `modCompileOnly` become `implementation` and `compileOnly`. `remapJar` becomes `jar`. [S10]
- **Fabric API renames**: Fabric API now uses Mojang names where applicable. These renames break old code. Example: `ItemGroupEvents` became `CreativeModeTabEvents`. [S10] Other examples (`getWorld` to `getLevel`, `KeyBindingHelper` to the keymapping package) come from a secondary search summary. [secondary]

### 2.3 Porting effort implications

- Fabric's 26.1 post: "no mods from 1.21.11 or before will work without, at a minimum, recompilation." [S10]
- The remap step is gone, but every name changed for APIs that moved, so a port is a rename pass plus recompile. Loom has automated tooling for code renames. [S9]
- Java 25 is required for the Gradle JVM. IntelliJ IDEA 2025.3 or later is needed for mixins. [S10]
- NeoForge's 2025 retrospective says porting the 26.1 snapshots took "a great deal of effort and manual labour" and bugs were likely introduced. [secondary: search summary of neoforged.net/news/2025-retrospection]
- The NeoForge 26.1 primer covers 1.21.11 to 26.1 changes. It was read in part: Java 25 and deobfuscation, loot unrolling, validation, villager trades, data component initializers, item instances, saved-data split. [S19]

---

## 3. Rendering

### 3.1 Timeline of rendering changes

| Version | Change | Source |
|---|---|---|
| 1.21.2 | GuiGraphics, RenderType and shader rewrites; EntityRenderState; EquipmentLayerRenderer | [S17] |
| 1.21.5 (2025-03-25) | **Render pipeline rework.** RenderPipeline is built in code and replaces JSON shader definitions. All render-library calls go through `GpuDevice` (`RenderSystem.getDevice()`). Command recording uses `CommandEncoder` and `RenderPass`. Buffers and textures are `GpuBuffer` and `GpuTexture`. Blend settings are `BlendFunction` objects, not numeric ids. `RenderType` still exists but takes a pipeline. | [S18] (fetched) |
| 1.21.6 | Custom dialogs take a RenderPipeline and TextureSetup. Uniform handling reworked. `CompiledRenderPipeline#containsUniform` removed. Render pass scissoring works only on OpenGL. | [secondary: NeoForge 1.21.6 primer excerpt, not fetched in full] |
| 26.1 (2026-03-24) | `GuiGraphics` renamed `GuiGraphicsExtractor`; `Screen#render` becomes `Screen#extractRenderState`. This is the start of the extract-then-render pattern. | [S13] |
| 26.2 (2026-06-16) | **Vulkan backend** added beside OpenGL (`com.mojang.blaze3d.vulkan` mirrors `...opengl`). `GpuFormat` replaces `TextureFormat`. `RenderPipeline` takes BindGroupLayouts and up to 8 ColorTargetStates. Depth test uses GREATER_THAN_OR_EQUAL (inverted depth). `MultiBufferSource` removed; submissions go through `SubmitNodeStorage` and `FeatureRenderDispatcher`. `LevelRenderer` work moves to `LevelExtractor`. `ShapeRenderer` becomes `ShapeOutlineFeatureRenderer`. `Font.drawInBatch` removed. | [S20] (read to about half of the page) |
| 26.2 settings | Vulkan is "experimental". Default equals "Prefer OpenGL". OpenGL is the fallback on hardware or drivers without Vulkan support. | [S7] |
| 26.3 (2026-09-15) | **GLFW replaced by SDL**: input codes and `KeyEvent` fields change. Blaze3D classes move to `com.mojang.renderpearl` (api, backend, frontend). `outputColorTextureOverride` and `outputDepthTextureOverride` removed. GLSL: `#moj_import` becomes `#include`; `in` and `out` need explicit `layout(location = N)`; GlslCompiler requires `GL_ARB_separate_shader_objects`. New OIT pipeline support. `ChunkSectionsToRender` becomes an abstract class with `$DrawIndirect` and `$DrawSeparate`. `PoseStack#mulPose` becomes `rotate`. `GpuDevice#precompilePipeline` becomes `compilePipeline`. | [S21] (read to about half of the page), [S11] (SDL and component changes) |
| 26.4 snapshot 1 (2026-09-22) | "Default" behaves the same as "Prefer Vulkan". The game no longer changes the graphics API automatically after a startup crash. | [S6] |
| 26.4 snapshot 3 (2026-10-06) | Known issue: resizing the window with Vulkan can crash and hard-lock (MC-312444, resolution "None"). Vulkan 20 fps framerate cap fixed (MC-307830). New debug option `chunk_section_status`. | [S5] |

### 3.2 Vulkan status

- 2026-02-19: Khronos reports that Java Edition is preparing to move from OpenGL to Vulkan. No schedule is given. [S23]
- The minecraft.wiki Vibrant Visuals page says Java is switching from OpenGL to Vulkan. It describes OpenGL as deprecated. Developers are rebuilding the render code onto a dedicated thread. Vibrant Visuals on Java is "planned" and "has not appeared in any development versions yet." The page is undated. [S22]
- Motivation (secondary): OpenGL has had no updates in about nine years, and macOS OpenGL support is ending. [secondary: search summaries]
- Mojang says it will remove the OpenGL implementation once Vulkan is stable and fast enough. This is from secondary sources only. [secondary] UNVERIFIED primary.
- Fabric 26.1 post: "26.1 is also expected to be the last Minecraft release to solely support OpenGL." Mods that make raw OpenGL calls need migration. [S10]
- Vulkan is the default in 26.4 snapshots (S6). Whether it becomes the default in a release is not known. Mojang's snapshot notes do not give a release date for OpenGL removal.

### 3.3 What breaks for mods that draw custom geometry

Facts from the primers and wikis:
1. **Raw OpenGL calls** do not work on the Vulkan path. [S10]
2. **Shader files**: JSON shader definitions are gone from 1.21.5 onward. Pipelines are code objects. [S18]
3. **Shader source syntax** changes in 26.3 (`#include`, explicit `layout(location)`). A GLSL requirement of `GL_ARB_separate_shader_objects` is also listed. [S21]
4. **Draw path**: `RenderType` still exists, but the NeoForge primer advises against custom uniforms inside a `RenderType`. [S18] Immediate-mode `MultiBufferSource` is removed in 26.2. Submitted geometry goes through `SubmitNodeCollector` and `FeatureRenderDispatcher`. [S20]
5. **Chunk and section rendering**: `LevelRenderer` is split (`LevelExtractor`). `SectionRenderDispatcher` no longer takes `ClientLevel` or `LevelRenderer`. `ChunkSectionsToRender` becomes abstract in 26.3. [S20][S21] Any LOD mod that hooks chunk section rendering or indirect draws is affected.
6. **Render-state extraction**: the game moves to an extract-then-render model, with render state classes such as `CameraRenderState`, `LevelRenderState`, `ChunkLoadingRenderState` and `SectionUpdateRenderState` (26.2). [S20][S13] Mods that read live world or entity state during render code are at risk. Impact on Apoapsis is an inference, not a verified finding.
7. **Sky and fog**: 26.4 snapshot 2 changes render-distance fog and adds a sky occluder. [secondary] Impact on planet-sky mods is UNVERIFIED.
8. **GUI and text**: `Gui` owns the current screen; the HUD moves to `Hud`; `Font.drawInBatch` is removed (26.2). [S20]
9. **Input**: GLFW to SDL (26.3) changes key and mouse constants. [S21][S11]

### 3.4 Sodium, Iris, Distant Horizons on the latest version

Status from the Modrinth API (fetched 2026-10-11) and GitHub release notes, unless noted.

| Mod | Loaders on Modrinth | 26.3 builds | Latest build | Notes |
|---|---|---|---|---|
| Sodium | fabric, neoforge, quilt | 0.9.2 (2026-09-15, Fabric and NeoForge), 0.9.3-alpha.1 (2026-09-20) | 0.9.3-alpha.1 (2026-09-20) | Release notes say "Port to 26.3" for 0.9.2. No note mentions Vulkan or OpenGL. [S35][S37] |
| Iris | fabric, neoforge, quilt | Fabric 1.11.5 and 1.11.6 (2026-09-15); Fabric and NeoForge 1.11.7 (2026-09-30) | 1.11.7+26.3 (2026-09-30) | Sodium 0.9.2 notes (Sep 15) said "Iris is not yet compatible". NeoForge Iris 26.3 landed Sep 30. [S34][S37] Vulkan compatibility UNVERIFIED. The GitHub releases page is stale (latest there is 1.7.3 for MC 1.21, July 2025), so Modrinth is the active channel. [S38] |
| Distant Horizons | fabric, forge, neoforge | 26.3 listed in game versions; project updated 2026-09-29 | UNVERIFIED specific build | Build numbers and Vulkan support not checked. [S30] |
| Lithium | fabric, neoforge, quilt | 26.3 listed; updated 2026-09-28 | UNVERIFIED | [S30] |
| C2ME (Fabric) | fabric | 26.3 and 26.4-snapshot-3 listed; updated 2026-10-07 | UNVERIFIED | [S30] |

---

## 4. API churn, 1.20.1 to 26.3

Each item gives the version it first appeared in. Items marked [secondary] are from docs pages that were only partly read or from search summaries.

### 4.1 Data components (item NBT replaced)

- **1.20.5**: `ItemStack` uses `get`, `set`, `update`, `remove` with a `DataComponentType` key. Arbitrary NBT is stored under `minecraft:custom_data`. Custom components use `DataComponentType$Builder` with `persistent(Codec)` for disk and `networkSynchronized(StreamCodec)` for network. Item defaults use `Item$Properties#component`. Classes live in `net.minecraft.world.item.component`. The hard max stack size rises from 64 to 99. [S15][S24]
- **1.21.2**: `ITEM_NAME` and `ITEM_MODEL` set via `Item$Properties#overrideDescription` and `#overrideModel`. `CONSUMABLE` replaces `FOOD`. `EQUIPPABLE` replaces the old equip checks. Armor attributes move to `ArmorMaterial` components. `Item$Properties#setId` required. [S17]
- **1.21.5**: `DataComponentGetter` introduced. `TooltipDisplay` replaces hide flags. `WEAPON`, `TOOL`, `ARMOR` and `BLOCKS_ATTACKS` components replace `SwordItem`, `DiggerItem` and `ArmorItem` behavior. `Entity` implements `DataComponentGetter`. `Saddleable` removed. [S18]
- **26.1**: `DataComponentInitializers` set components on holders at datapack reload. `ItemStackTemplate` and `ItemInstance` are new. `Item$Properties#delayedComponent` lazily sets datapack-dependent components. `DataComponents.DYE` replaces `DyeItem`'s color. [S19] (partial read)
- **26.3**: compostables, fuel and brewing fuel move to item components. Brewing recipes become data-driven. [S11]

### 4.2 Codecs and registries

- **1.20.5**: `MapCodec` used for dispatch. `optionalFieldOf` now strict by default. Loot tables become reloadable dynamic registries (replacing `LootDataManager`). [S15]
- **1.21**: `Enchantment` becomes a datapack registry (`Holder<Enchantment>`). Enchantment effects are `EnchantmentEffectComponents` in JSON, with `ConditionalEffect` wrappers. Recipe and related types take `RecipeInput`. The `recipes` data folder is renamed `recipe`. [S16]
- **1.21.2**: Recipes become a datapack registry. `RecipeDisplay`, `SlotDisplay` and `PlacementInfo` are added. `RecipeProvider` no longer extends `DataProvider`. Recipe book categories are registry objects. [S17]
- **26.1**: loot type registries hold `MapCodec`s. `IntProvider` and `FloatProvider` become interfaces. `RecipeSerializer` becomes a record holding a `MapCodec` and `StreamCodec`. `Registries.TRADE_SET` and `VILLAGER_TRADE` are new datapack registries. [S19] (partial read)
- **26.2**: `EntityPredicate` restructured around `EntitySubPredicate`s. `ChatFormatting` codecs replaced by `TextColor#CODEC`. [S20] (partial read)
- **26.3**: `LootTable`, `LootItemCondition`, `LootItemFunction`, `SlotSource`, `Advancement` and `Recipe` become reloadable datapack registries. `ContextAwarePredicate` removed. [S21] Fabric adds: block codecs and the `block_type` registry removed; dynamic registries now include recipes and advancements; number providers split into float and integer; surface rules renamed to material rules; configured features restructured. [S11]

### 4.3 Networking (CustomPacketPayload and StreamCodec)

- **1.20.5**: `StreamCodec` replaces hand-written `FriendlyByteBuf` reads and writes. `StreamCodec.composite` takes up to 6 fields. Packets are registered through `ProtocolInfoBuilder#addPacket`. [S15] The claim that `RegistryFriendlyByteBuf` works only in play packets is [secondary: docs.neoforged.net payload guide search summary, not fetched].
- **NeoForge payloads (1.20.5 and later)**: a payload has a `CustomPacketPayload.Type` (a wrapper around an identifier) and a `StreamCodec`. Both are registered in `RegisterPayloadHandlersEvent`. [secondary: docs.neoforged.net networking/payload pages, search summary, not fetched in full]
- **26.3**: `ClientboundAddTransientBlockPacket` added. `SignText` gains a `STREAM_CODEC`. [S21]

### 4.4 Data attachments

- **1.20.5 and 1.20.6 (NeoForge)**: attachments work on block entities, chunks and entities. Item-stack attachments were dropped because components replaced them. Level-level data should use `SavedData`. [secondary: docs search summary. The release note text is said to mention levels, which conflicts.]
- Attachment syncing to clients uses a `STREAM_CODEC` in the current docs. [secondary]
- **26.1 (saved data split)**: `SavedDataType` identifies saved data by an identifier that can include subdirectories. `DimensionDataStorage` renamed `SavedDataStorage`. `MinecraftServer` gains its own storage for global data. Custom boss events, per-dimension ender dragon fight, game rules, wandering trader spawning, weather and world generation settings move to saved data. `ServerLevelData` gains clock states. `LevelData#getDayTime` removed. [S19] (partial read). This matters for Apoapsis global state.

### 4.5 Data-driven enchantments

- **1.20.5**: `Enchantment` gets an `EnchantmentDefinition`. `EnchantmentCategory` replaced by `minecraft:enchantable/*` tags. [S15]
- **1.21**: enchantments are datapack JSON with `supported_items`, `primary_items`, `min_cost`/`max_cost`, `slots`, `exclusive_set` and `effects`. Application goes through `EnchantmentHelper` `runIterationOn*` calls. [S16]

### 4.6 Entities and attributes

Attribute values and IDs:

| Attribute | Added / changed | Default (current wiki) | Source |
|---|---|---|---|
| `gravity` (wiki Java 1.20.5 page: `generic.gravity`) | Added in 1.20.5 (2024-04-23) | 0.08 blocks per tick squared | [S24][S25] |
| `step_height` (`generic.step_height` in 1.20.5) | Added in 1.20.5 | 0.6 | [S24][S25] |
| `safe_fall_distance` (`generic.safe_fall_distance`) | Added in 1.20.5 | 3 | [S24][S25] |
| `jump_strength` (was `horse.jump_strength`) | Renamed to `generic.jump_strength` in 1.20.5; applies to all entities per the 1.20.5 page | 0.42 | [S24][S25] |

- The current wiki lists IDs without the `generic.` prefix. The version where the prefix was dropped is UNVERIFIED.
- A secondary NeoForge page says jump strength affects only horses. The 1.20.5 wiki says it applies to all entities. This conflict is UNVERIFIED and should be checked in code.
- **1.20.5 code changes**: `AttributeModifier` is a record. Operations renamed `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`. `Entity#getGravity` and `applyGravity` exist. `LivingEntity#getJumpPower` reads from the attribute. [S15]
- **1.21**: `AttributeModifier` is identified by `ResourceLocation`, not a UUID string. `AttributeInstance#getModifiers` returns a map keyed by `ResourceLocation`. `addOrReplacePermanentModifier` added. `Leashable` interface added. `LivingEntity#getExperienceReward` becomes `getBaseExperienceReward`. [S16]
- **1.21.2**: `LivingEntity#canContinueToGlide` becomes `canGlide`. `EntityRenderer#affectedByCulling` replaces `Entity#noCulling`. [S17]
- **26.1-26.2**: `EntityRenderer#extractNameplates` becomes `extractNameTags`. `SulfurCubeRenderer` and `SulfurCubeRenderState` added. Entity renderer state classes continue the extraction pattern. [S20]

### 4.7 Chunks, lighting and worldgen

- **1.21**: chunk generation reorganized into `ChunkGenerationTask`, `GeneratingChunkMap` and `GenerationChunkHolder`, with steps in `ChunkPyramid`. `ChunkGenerator#createBiomes` and `fillFromNoise` no longer take an `Executor`. `LiquidSettings` added. `Entity#changeDimension(DimensionTransition)` replaces the `PortalInfo` flow. `ChunkAccess#getStatus` becomes `getPersistedStatus`. [S16]
- **26.1**: `ChunkPos` construction changes. `ChunkPos.containing(BlockPos)` replaces `new ChunkPos(BlockPos)`. `asLong` becomes `pack`. `new ChunkPos(long)` becomes `ChunkPos.unpack`. [S13]
- **26.2**: `LevelRenderer` split with much of its work moving to `LevelExtractor`. `SectionRenderDispatcher` changes constructor. `ViewArea` takes min and max Y bounds. `LightCoordsUtil#getLightCoords` replaces `LevelRenderer#getLightCoords`. [S20]
- **26.3**: configured features restructured; several placement modifiers renamed; `ChunkSectionsToRender` abstract. [S11][S21]

### 4.8 Java and Gradle requirements by release

- 1.20.4 is "the last release supporting Java 17 to 20" per the wiki. [S43]
- 1.20.5 requires Java 21. [S15]
- 1.21.x requires Java 21. This is inferred from the 1.20.5 primer and not separately verified for every 1.21.x release. UNVERIFIED per release.
- 26.1 and later require Java SE 25. [S1][S43][S10][S13]

---

## 5. Loaders and ecosystem

### 5.1 Loader status on the latest version (2026-10-11)

| Loader | Status for 26.3 | Evidence | Notes |
|---|---|---|---|
| NeoForge | Latest 26.3.0.69-beta. Every 26.3 build in the maven list has the -beta suffix. | [S26] | Builds 26.1.2.115 and 26.1.2.116, and 26.2.0.89 and 26.2.0.90, have no -beta suffix. Whether these are stable releases is UNVERIFIED (check neoforged.net). NeoForge 1.21.1 still receives builds (21.1.258, 21.1.259). [S26] |
| Forge (MinecraftForge) | 26.3 build 66.0.10 dated 2026-10-09. Builds from 66.0.0 (2026-09-19) onward. | [S27] | The page does not label a recommended build. 26.2 line is in the 65.x range and 26.1.2 is in the 64.x range [secondary]. Forge 1.21.1 status UNVERIFIED. |
| Fabric Loader | 0.19.5 | [S11] | 0.18.4 was current at 26.1 (2026-03-14). [S10] |
| Fabric Loom | 1.17 recommended for 26.3 (2026-09-15) | [S11] | 1.15 recommended at 26.1. [S10] Maven metadata fetch returned unrelated data. Any Loom newer than 1.17 is UNVERIFIED. |
| Quilt Loader | UNVERIFIED for 26.x. Quilt-tagged Modrinth builds exist for 26.x in Sodium, Iris, Lithium and Architectury. | [S40][S30] | Quilt's English site redirect did not show a supported-version list. Quilt Mappings discontinued (2026-02-03). |
| Sinytra Connector | Targets 26.1.2 build 3.0.0-beta.6+26.1.2 on NeoForge 26.1.2.77. Compatible game versions listed: 26.1, 26.1.1, 26.1.2. | [S39][S30] | Test results: 354 compatible, 46 incompatible, 0 with errors, 400 total. Modrinth project updated 2026-08-15. No 26.2 or 26.3 Connector build found (UNVERIFIED). |
| Architectury API | Fabric, Forge, NeoForge, Quilt. 26.1 to 26.3 listed. | [S30] | Updated 2026-10-09. |
| MultiLoader-Template (jaredlll08) | Branch 26.3. NeoForge and Fabric subprojects. Java 25 required. | [S41] | README names no Loom, ModDevGradle or NeoGradle version (UNVERIFIED). |
| Stonecutter | Multi-version Gradle plugin. | [S42] | Project moved to Codeberg; the GitHub repo is a mirror. Version UNVERIFIED. |

### 5.2 Big tech mods on the latest version (Modrinth API, 2026-10-11)

| Mod (Modrinth slug) | Loaders | Latest MC listed | 26.x listed | Last Modrinth update | Source |
|---|---|---|---|---|---|
| Create (create) | forge, neoforge | 1.21.1 (6.0.10, 2026-04-21) | No | 2026-04-21 | [S31] |
| Mekanism (mekanism) | forge, neoforge | 1.21.1 (10.7.19.85, 2026-04-10) | No | 2026-04-10 | [S32] |
| Applied Energistics 2 (ae2) | fabric, forge, neoforge | 26.3 (26.3.1-alpha NeoForge, 2026-10-03) | Yes: 26.1.2, 26.2, 26.3. No plain "26.1" tag. | 2026-10-03 | [S33] |
| Immersive Engineering (immersiveengineering) | forge, neoforge | 1.21.1 | No | 2025-07-05 | [Modrinth API, project endpoint] |
| Tinkers' Construct (tinkers-construct) | forge, neoforge | 1.20.1 in listing | No | 2026-09-22 | [Modrinth API, project endpoint]. Whether a 1.21.1 build exists under another project is UNVERIFIED. |
| Valkyrien Skies 2 (valkyrien-skies) | fabric, forge | 1.20.1 (1.20.1-forge-2.4.11, 2026-04-10) | No. No NeoForge build on this slug. | 2026-04-10 | [S36] |
| Thermal Expansion (thermal-expansion) | forge | 1.20.1 | No | 2024-05-26 | [Modrinth API, project endpoint]. Whether a NeoForge 1.21.1 port exists elsewhere is UNVERIFIED. |
| Sodium | fabric, neoforge, quilt | 26.3 | Yes | 2026-09-20 | [S35] |
| Iris | fabric, neoforge, quilt | 26.3 | Yes | 2026-09-30 | [S34] |
| Lithium | fabric, neoforge, quilt | 26.3 | Yes | 2026-09-28 | [S30] |
| Distant Horizons (distanthorizons) | fabric, forge, neoforge | 26.3 | Yes | 2026-09-29 | [S30] |
| Architectury API | fabric, forge, neoforge, quilt | 26.3 | Yes | 2026-10-09 | [S30] |
| Fabric API | fabric | 26.3 (26.4-snapshot-3 also listed) | Yes | 2026-10-06 | [S30] |
| C2ME (c2me-fabric) | fabric | 26.4-snapshot-3 | Yes | 2026-10-07 | [S30] |
| Sinytra Connector (connector) | forge, neoforge | 26.1.2 | 26.1.2 only | 2026-08-15 | [S30][S39] |

Points for the designer:
- Valkyrien Skies 2 is the closest existing analogue to moving ships. On Modrinth it has Fabric and Forge builds for 1.20.1 only. No 1.21.1 or 26.x build is listed.
- None of Create, Mekanism, Immersive Engineering or Valkyrien Skies 2 has a 26.x build on Modrinth as of 2026-10-11.

---

## 6. Java and tooling

| Item | Latest value | Source |
|---|---|---|
| Java for 26.1 to 26.3 | Java SE 25 (Gradle JVM and runtime) | [S1][S2][S10][S13][S43] |
| Java for 1.20.5 to 1.21.11 | Java 21 (from the 1.20.5 primer). Per-release check UNVERIFIED. | [S15] |
| Gradle, 26.1 MDG minimum | Gradle 9.1.0 or newer | [S13] |
| Gradle, Fabric recommendation | 9.4.0 (2026-03-14); 9.6.0 at 26.3 (2026-09-15) | [S10][S11] |
| ModDevGradle (`net.neoforged.moddev`) | 2.0.148, created 2026-09-29 | [S28] |
| ModDevGradle at 26.1 post | 2.0.141 (2026-03-24) | [S13] |
| NeoGradle (`net.neoforged.gradle`) | 7.1.21 at 26.1 post. Latest version UNVERIFIED; the plugin page fetch failed. | [S13] |
| Fabric Loom (`net.fabricmc.fabric-loom`) | 1.17 at 26.3; 1.15 at 26.1. Newer UNVERIFIED. | [S10][S11] |
| Fabric Loader | 0.19.5 | [S11] |
| IDE for mixins | IntelliJ IDEA 2025.3 or later | [S10] |
| NeoForge maven latest | 26.3.0.69-beta (also 21.1.259 for 1.21.1) | [S26] |
| Forge latest | 66.0.10 for 26.3 | [S27] |

---

## 7. Update cadence and ecosystem share

### 7.1 Share of mod projects on each version (Modrinth search count, 2026-10-11)

Counts are Modrinth search hits for `project_type:mod` with `versions:<X>`. They count projects, not downloads. They are not popularity-weighted. Shares are of all mod projects (77,057).

| Minecraft version | Mod projects on Modrinth | Share of all mod projects |
|---|---|---|
| 1.20.1 | 34,670 | 45.0% |
| 1.21.1 | 31,999 | 41.5% |
| 1.21.11 | 19,547 | 25.4% |
| 26.1 | 11,630 | 15.1% |
| 26.2 | 13,540 | 17.6% |
| 26.3 | 7,474 | 9.7% |
| All mod projects | 77,057 | 100% |

Sources: [S29]. Note that a project can match several versions, so the rows do not sum to the total.

### 7.2 Port and release gaps

| Event | Date(s) | Gap | Source |
|---|---|---|---|
| 1.20.1 released | 2023-06-12 | n/a | [S43] |
| 1.21.1 released | 2024-08-08 | About 14 months after 1.20.1 | [S43] |
| Create first 1.21.1 build on Modrinth (6.0.0) | 2025-03-01 | About 6.7 months after 1.21.1. Create 1.20.1 6.0.0 was published the same day. | [S31] |
| Mekanism earliest 1.21.1 build in fetched list | 2024-08-31 (list truncated) | About 3 weeks after 1.21.1 | [S32] |
| AE2 earliest 1.21.1 build in fetched list | 19.2.6-beta, 2025-03-19 (list truncated) | UNVERIFIED first build | [S33] |
| 26.1 released | 2026-03-24 | n/a | [S43] |
| Sodium 26.1.2 build (0.9.1) | 2026-07-08 (earliest visible; list truncated) | About 3.5 months after 26.1 (first 26.1 build may be earlier) | [S35] |
| AE2 first visible 26.x build | 26.1.6-alpha (NeoForge), 2026-05-05 (list truncated) | About 6 weeks after 26.1 | [S33] |
| Connector build for 26.1.2 | 3.0.0-beta.6, 2026-08-15 | About 4.7 months after 26.1 | [S39][S30] |
| Sodium 26.3 port | 0.9.2, 2026-09-15 | Same day as 26.3 release | [S35][S37] |
| Forge 26.3 first build | 66.0.0, 2026-09-19 | 4 days after 26.3 | [S27] |
| Iris NeoForge 26.3 build | 1.11.7+26.3, 2026-09-30 | 15 days after 26.3 | [S34] |
| Create, Mekanism, Immersive Engineering, Valkyrien Skies 2 on 26.x | None on Modrinth at 2026-10-11 | 26.1 is 201 days old; 26.3 is 26 days old | [S31][S32][Modrinth API] |

### 7.3 Why many packs stayed on 1.20.1

Facts from the data:
- 1.20.1 still has the most mod projects (34,670). [S29]
- Valkyrien Skies 2 and Thermal Expansion have no 1.21.1 build under the slugs checked. Tinkers' Construct lists 1.20.1 as its last version under its slug. [S36][Modrinth API]
- Create's 1.20.1 line got 6.0.x builds on the same day as its 1.21.1 line (2025-03-01), and was still updated in 2025-11 (1.20.1-6.0.8). [S31]

Reasons (secondary, UNVERIFIED):
- Search summaries say Forge and NeoForge are not interchangeable from 1.20.2 onward. NeoForge reportedly recommends Forge for 1.20.1. [secondary: hosting-company summaries]
- The 1.20.2 to 1.20.5 changes (data components, StreamCodec networking) are said to make ports expensive. [secondary, and consistent with section 4]

---

## 8. UNVERIFIED register and gaps

1. **Mojang primary sources not read.** Every minecraft.net fetch timed out: the deobfuscation article, the version numbering article, the 26.1 Snapshot 1 article, the 26.4 Snapshot 1 article, the Vibrant Visuals articles, and the 26.3 article. Dates for these come from minecraft.wiki and the Modrinth API.
2. **Deobfuscation announcement date**: minecraft.wiki says 2025-10-29. A secondary report says 2025-10-30. Mojang's own article was not read.
3. **Vulkan timing**: the claim that Mojang will remove OpenGL once Vulkan is stable is secondary only.
4. **Vibrant Visuals on Java**: wiki says "planned", undated. No feature list was verified.
5. **1.21.6 rendering details**: from an excerpt of a NeoForge primer, not a full read.
6. **26.3 NeoForge primer**: read to about half the page. Attachments, attributes, worldgen and the Java/Gradle minimums in the second half were not read.
7. **26.1 and 26.2 primers**: read in part. Several later sections were truncated.
8. **Attribute prefix**: the version in which `generic.` was dropped from attribute IDs is UNVERIFIED.
9. **jump_strength scope**: the 1.20.5 wiki says it applies to all entities; a secondary NeoForge page says horses only. Conflict unresolved.
10. **Parchment**: NeoForge says it is unnecessary; a secondary source says it is still useful for Javadoc. Conflict unresolved.
11. **NeoForge stability**: 26.1.2.115/116 and 26.2.0.89/90 appear without the -beta suffix in the maven list. Whether they are stable releases is UNVERIFIED. A secondary search summary said 26.1 builds were still betas.
12. **Forge 26.1.2 (64.x) and 26.2 (65.x)** build ranges are from a secondary search summary.
13. **Forge 1.21.1 status**: UNVERIFIED.
14. **Quilt Loader status for 26.x**: UNVERIFIED. Only the Quilt news title was read.
15. **Create, Mekanism, Immersive Engineering, Valkyrien Skies 2, Thermal, Tinkers' Construct on other sites** (CurseForge, GitHub): not checked. Modrinth is the only source for their version lists.
16. **Create's Fabric port for 26.x**: UNVERIFIED.
17. **Connector beyond 26.1.2**: no 26.2 or 26.3 build found. UNVERIFIED.
18. **Iris and Distant Horizons under Vulkan**: not verified.
19. **Fabric Loom newer than 1.17**: maven metadata fetch returned unrelated data. UNVERIFIED.
20. **NeoGradle latest version**: UNVERIFIED (7.1.21 is from the March 2026 NeoForge post).
21. **Stonecutter and MultiLoader-Template versions**: not verified.
22. **26.4 snapshot 4**: not found. UNVERIFIED that none exists.
23. **Java 21 for each 1.21.x release**: inferred, not checked per release. Java 17 for 1.20.1: inferred from the wiki note on 1.20.4, not checked directly.
24. **Modrinth release dates vs wiki**: they differ by one day for 1.20.2 and 1.20.3. The wiki dates are used.
25. **Modrinth list truncation**: several version lists were read only partly (Mekanism, AE2, Sodium, Create). The "earliest" dates are the earliest visible, not necessarily the first ever.
26. **WebSearch**: quota reached after 13 successful searches. Not searched: Create 26.x news, Quilt Loader news, Valkyrien Skies 2 NeoForge plans, Mojang's OpenGL removal statement.

---

## 9. Sources

Access date for all: 2026-10-11.

- [S1] https://minecraft.wiki/w/Java_Edition_26.3 (release 2026-09-15; Java SE 25; data pack 121.0; resource pack 97.1; headline changes)
- [S2] https://api.modrinth.com/v2/tag/game_version (release tag dates for 1.20.1 to 26.3; 26.4 snapshot dates)
- [S3] https://minecraft.wiki/w/Java_Edition_version_history (26.x development and release dates; 26.4 "Upcoming Q4 2026")
- [S4] https://minecraft.wiki/w/Development_versions (sidebar: latest snapshot "26.4-snap-3")
- [S5] https://minecraft.wiki/w/Java_Edition_26.4_Snapshot_3 (2026-10-06; Vulkan known issues; debug option; ice caves)
- [S6] https://minecraft.wiki/w/Java_Edition_26.4_Snapshot_1 (2026-09-22; Default equals Prefer Vulkan)
- [S7] https://minecraft.wiki/w/Java_Edition_26.2 (2026-06-16; Vulkan experimental; Default equals Prefer OpenGL; Java SE 25)
- [S8] https://minecraft.wiki/w/Obfuscation_map (announcement 2025-10-29; 26.1 Snapshot 1 first without maps)
- [S9] https://fabricmc.net/2025/10/31/obfuscation.html (fetched; dated Oct 31, 2025)
- [S10] https://fabricmc.net/2026/03/14/261.html (fetched; Fabric for Minecraft 26.1; dated Mar 14, 2026)
- [S11] https://fabricmc.net/2026/09/15/263.html (fetched; Fabric for Minecraft 26.3; dated Sep 15, 2026)
- [S12] https://fabricmc.net/blog/ (index of Fabric posts 2018 to 2026)
- [S13] https://neoforged.net/news/26.1release/ (fetched; NeoForge 26.1 post, dated Mar 24, 2026)
- [S14] https://docs.neoforged.net/primer/docs/ (index of primers 1.14 to 26.3)
- [S15] https://docs.neoforged.net/primer/docs/1.20.5/ (fetched; 1.20.4 to 1.20.5 primer; the page content is titled this way)
- [S16] https://docs.neoforged.net/primer/docs/1.21/ (fetched; 1.20.6 to 1.21 primer)
- [S17] https://docs.neoforged.net/primer/docs/1.21.2/ (fetched; 1.21.1 to 1.21.2/3 primer, partial)
- [S18] https://docs.neoforged.net/primer/docs/1.21.5/ (fetched; 1.21.4 to 1.21.5 primer)
- [S19] https://docs.neoforged.net/primer/docs/26.1/ (fetched; 1.21.11 to 26.1 primer, partial)
- [S20] https://docs.neoforged.net/primer/docs/26.2/ (fetched; 26.1.x to 26.2 primer, partial)
- [S21] https://docs.neoforged.net/primer/docs/26.3/ (fetched; 26.2 to 26.3 primer, partial)
- [S22] https://minecraft.wiki/w/Vibrant_Visuals (Java section; undated)
- [S23] https://www.khronos.org/news/permalink/minecraft-switches-to-vulkan-for-virbrant-visuals (dated Feb 19, 2026)
- [S24] https://minecraft.wiki/w/Java_Edition_1.20.5 (release 2024-04-23; attributes; data components)
- [S25] https://minecraft.wiki/w/Attribute (current attribute IDs and defaults)
- [S26] https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml (NeoForge versions; latest 26.3.0.69-beta)
- [S27] https://files.minecraftforge.net/ (Forge 26.3 build 66.0.10, 2026-10-09)
- [S28] https://plugins.gradle.org/plugin/net.neoforged.moddev (ModDevGradle 2.0.148, created 2026-09-29)
- [S29] https://api.modrinth.com/v2/search?facets=[["project_type:mod"],["versions:X"]]&limit=0 (counts for 1.20.1, 1.21.1, 1.21.11, 26.1, 26.2, 26.3, and all mods)
- [S30] https://api.modrinth.com/v2/project/{slug} (sodium, iris, lithium, connector, distanthorizons, architectury-api, fabric-api, c2me-fabric, ae2 and others)
- [S31] https://api.modrinth.com/v2/project/create/version (Create build dates)
- [S32] https://api.modrinth.com/v2/project/mekanism/version (Mekanism build dates)
- [S33] https://api.modrinth.com/v2/project/ae2/version (AE2 build dates)
- [S34] https://api.modrinth.com/v2/project/iris/version (Iris build dates)
- [S35] https://api.modrinth.com/v2/project/sodium/version (Sodium build dates)
- [S36] https://api.modrinth.com/v2/project/valkyrien-skies/version (Valkyrien Skies 2 build dates)
- [S37] https://github.com/CaffeineMC/sodium/releases (Sodium 0.9.2 "Port to 26.3"; "Iris is not yet compatible")
- [S38] https://github.com/IrisShaders/Iris/releases (stale; latest listed 1.7.3, July 2025)
- [S39] https://connector.sinytra.org/compatibility (Connector 3.0.0-beta.6+26.1.2; 354 of 400 compatible)
- [S40] https://quiltmc.org/en/ (news title "Bringing Quilt into the non-obfuscated era", dated Feb 3, 2026)
- [S41] https://github.com/jaredlll08/MultiLoader-Template (branch 26.3; Java 25)
- [S42] https://github.com/kikugie/stonecutter (moved to Codeberg)
- [S43] minecraft.wiki release pages: https://minecraft.wiki/w/Java_Edition_1.20.1 , 1.20.2, 1.20.3, 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 (each at /w/Java_Edition_<version>). Dates and headline changes read from each page.
- Modrinth project and version endpoints not numbered above: create, mekanism, immersiveengineering, tinkers-construct, thermal-expansion (project endpoints), distanthorizons, architectury-api, fabric-api, c2me-fabric, all at https://api.modrinth.com/v2/project/<slug>.
