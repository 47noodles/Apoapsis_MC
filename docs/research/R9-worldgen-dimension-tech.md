# R9: Worldgen, dimensions, physics attributes and planet tech (Apoapsis research)

Research node R9. Facts only, for the Opus designer. Date of research: 2026-10-11.

## 0. Baseline and how to read this file

**Target version.** Latest Java Edition release is **26.3 "Wilderness Bound"**. The wiki lists data pack format 121.0, resource pack format 97.1 and data version 5023 for it [V: https://minecraft.wiki/w/Java_Edition_26.3]. Release date 2026-09-15 comes from a third-party aggregator [A: https://minespecs.com/minecraft-versions/]. The 2026 drops were 26.1 (Tiny Takeover, Mar 24), 26.1.1, 26.1.2, 26.2 (Chaos Cubed, Jun 16) and 26.3 [A: https://minecraft.wiki/w/Java_Edition_26.1 and aggregator pages]. The last 1.x release was 1.21.11 (Dec 2025) [A]. The 26.x line needs Java 25 [A, per two aggregator sources; not opened]. **26.4 is in snapshots only** (26.4-snapshot-3 dated 2026-10-06 per aggregator [A]). Several facts below come from 26.4 snapshots and may change before release. Target 26.3 and track 26.4.

**Legend.**
- [V] verified from a page I fetched (URL given).
- [A] from a search snippet or third-party aggregator, not opened, or opened but only partly read.
- [U] UNVERIFIED. Inference or a gap. Check before relying on it.

**Limits of this run.** The web search budget for the turn ran out partway through, so some items are [A] or [U] rather than [V]. No Mojang primary release notes were fetched. No binaries or decompiled sources were downloaded (boundary rule). Items that need decompiled code (for example, which entity classes read the gravity attribute) are flagged for local checking.

**Primary sources used.**
- minecraft.wiki: Attribute, Dimension type, Environment attribute, Timeline, World clock, Density function, Noise router, Noise settings, World generation, Biome definition (Java), Gravity (the page returned the Entity motion table), Java Edition 26.3, Dimension definition (fetched at https://minecraft.wiki/w/Custom_dimension, which redirects to it).
- NeoForge: primers https://docs.neoforged.net/primer/docs/1.21.11/, https://docs.neoforged.net/primer/docs/26.1/, https://docs.neoforged.net/primer/docs/26.2/, https://docs.neoforged.net/primer/docs/26.3/. Fluids https://docs.neoforged.net/docs/fluids/. Registries https://docs.neoforged.net/docs/concepts/registries. Resources https://docs.neoforged.net/docs/resources/. Issue https://github.com/neoforged/NeoForge/issues/3542.

---

## 1. Data-driven worldgen (latest)

### 1.1 What changed recently (2025 to 2026)

- **1.21.11 (25w42a):** dimension types gained an `attributes` field. Several boolean dimension fields moved into environment attributes (section 2.2) [V: https://minecraft.wiki/w/Dimension_type].
- **26.1 (snapshot 3 onward):** world clocks and timeline `clock` and `time_markers` were added [V: https://minecraft.wiki/w/World_clock, https://minecraft.wiki/w/Timeline].
- **26.2 (snapshot 1 to 5):** `DensityFunction` API churn. `DensityFunction#mapAll` became a default method and `mapChildren` was added. `weirdScaledSampler` was removed and replaced by `NoiseRouterData$QuantizedSpaghettiRarity#wrapRarity*d`. `DensityFunctions#intervalSelect` was added. `NoiseBasedChunkGenerator#buildSurface` and `SurfaceSystem#buildSurface` take a `Set<Holder<Biome>>` instead of a `Registry<Biome>` [V: https://docs.neoforged.net/primer/docs/26.2/ (offset 100000 segment); https://minecraft.wiki/w/Density_function].
- **26.3 (snapshots 1 to 10), density functions:**
  - snapshot 4 added `ceil`, `div`, `floor`, `negate`, `lerp`, `round`, `sub`, `truncate`. `invert` was renamed `reciprocal`.
  - snapshot 6 added `distance_to_point`, `log`, `pow`, `sign`, `slice`, `sqrt`. `end_islands` was renamed `end_outer_islands`, and `y_clamped_gradient` was replaced by `gradient`.
  - snapshot 7 switched density evaluation from double to single-precision float.
  - snapshot 10 added `ore_vein`, renamed `cache_once` to `cache`, and removed `cache_2d`, `cache_all_in_cell`, `flat_cache` and `shifted_noise` [V: https://minecraft.wiki/w/Density_function].
- **26.3 noise settings:** `surface_rule` renamed `material_rule` (snapshot 1). `spawn_target` accepts any density function (snapshot 2). `size_horizontal` and `size_vertical` moved into the `interpolated` density function, `ore_veins_enabled` was removed (its job moved to `ore_vein`), and `debug_functions` was added (snapshot 10) [V: https://minecraft.wiki/w/Noise_settings].
- **26.3 snapshot 10:** the noise router field `preliminary_surface` (1.21.9) was renamed `chunk_surface_level`. The 1.21.9 (25w31a) change replaced `initial_density_without_jaggedness` with the preliminary surface value [V: https://minecraft.wiki/w/Noise_router].
- **26.4 snapshot 1:** `default_block` removed from noise settings. The block is always air and other blocks must come from `material_rule` [V: https://minecraft.wiki/w/Noise_settings, snapshot only].

### 1.2 Generation pipeline

Chunk statuses run in order: `empty`, `structure_starts`, `structure_references`, `biomes`, `terrain`, `features`, `initialize_light`, `light`, `spawn`, `full`. Distant chunks can be frozen as proto-chunks at an early status and resume when a player approaches. Only `full` makes a level chunk [V: https://minecraft.wiki/w/World_generation].

### 1.3 Noise settings (datapack: `data/<ns>/worldgen/noise_settings/`)

Top-level fields [V: https://minecraft.wiki/w/Noise_settings]:
- `sea_level`: terrain only. Mob spawning uses a fixed 63.
- `disable_mob_generation`, `aquifers_enabled`, `legacy_random_source` (booleans).
- `default_fluid` (water, lava or air in vanilla presets).
- `default_block` (removed in 26.4 snapshot 1, see above).
- `noise`: `min_y`, `height` (plus `size_horizontal` and `size_vertical` before 26.3 snapshot 10).
- `noise_router`: map of density functions (section 1.4).
- `spawn_target`: required list, may be empty. Climate points that guide where players spawn near origin.
- `material_rule` (formerly `surface_rule`).
- `debug_functions` (26.3 snapshot 10).

### 1.4 Noise router

A noise router is a set of density functions that drive terrain shape, biome choice, aquifers and ore veins [V: https://minecraft.wiki/w/Noise_router]:
- `final_density`: solidity. Positive means default block, otherwise air or default fluid. Constant 0 gives a void dimension.
- `barrier`, `fluid_level_floodedness`, `fluid_level_spread`, `lava` (aquifers. `lava` above 0.3 selects lava over water).
- `vein_toggle`, `vein_ridged`, `vein_gap` (ore veins, in the Overworld. Copper and iron ranges as documented on the page).
- `temperature`, `vegetation`, `continents`, `erosion`, `depth`, `ridges`. Biome selection only, except `erosion` and `depth`, which also feed aquifers.
- `chunk_surface_level` (26.3 snapshot 10 name).

### 1.5 Density functions

- Format: a plain number (shorthand for `constant`) or an object with `type` [V: https://minecraft.wiki/w/Density_function].
- Current types in the wiki: `cache`, `interpolated`, `noise`, `old_blended_noise`, `shift`, `shift_a`, `shift_b`, `abs`, `add`, `ceil`, `clamp`, `constant`, `cube`, `div`, `floor`, `log`, `mul`, `min`, `max`, `negate`, `pow`, `reciprocal`, `round`, `sub`, `sign`, `sqrt`, `square`, `truncate`, `gradient`, `lerp`, `squeeze`, `spline`, `half_negative`, `interval_select`, `quarter_negative`, `range_choice`, `beardifier`, `blend_alpha`, `blend_density`, `blend_offset`, `distance_to_point`, `end_outer_islands`, `find_top_surface`, `ore_vein`, `slice`.
- The first density-function types arrived in 1.18.2 (the wiki history starts there). `spline` was added in 1.18.2-pre2. `terrain_shaper_spline` and `slide` were removed in 22w11a and 22w12a [V].
- The wiki says density functions are **Java Edition only** [V]. It does not say whether a new density function *type* can be registered without code [U]. Expect that new types need Java registration (inference).

### 1.6 Biome source and biome definitions

- **Overworld multi-noise:** biomes are chosen from six parameters: temperature, humidity, continentalness, erosion, weirdness and depth. The first five depend only on horizontal position. If a point falls outside all intervals, the closest interval is used [V: https://minecraft.wiki/w/World_generation].
- **Nether:** temperature and humidity, with an offset that is always 0. Biomes are single points and the closest point wins [V].
- **End:** erosion only. Chunks within 1024 blocks of origin are the central island [V].
- **Datapack format for `multi_noise` climate parameters, and for `checkerboard`, `fixed` and `the_end` sources:** not verified [U]. The wiki pages I opened did not document them.
- **Biome definition fields** [V: https://minecraft.wiki/w/Biome_definition_(Java_Edition)]:
  - `has_precipitation` (renamed from `precipitation` in 23w03a).
  - `temperature`, `temperature_modifier` (`none` or `frozen`), `downfall`.
  - `effects`: `water_color` (required), `foliage_color`, `dry_foliage_color` (25w08a), `grass_color`, `grass_color_modifier` (`none`, `dark_forest`, `swamp`). Fog, sky, particle, sound and music fields were removed from `effects` in 25w42a.
  - `attributes`: environment attributes (25w42a, section 2.3).
  - `carvers` (required, tag or list), `features` (usually 11 steps), `player_spawn_friendly`.
  - `spawners`, `spawn_costs`, `creature_spawn_probability` were removed in 26.3 snapshot 4 and moved to environment attributes [V].

### 1.7 Surface rules, carvers, features, structures

- **Surface (material) rules:** a rule pass after base terrain replaces blocks by biome, depth, noise threshold and Y. Documented in tables on the World generation page [V].
- **Carvers:** caves carve Y -56 to 180, with higher probability from -56 to 47. Nether caves run Y 0 to 126. Canyons start between Y 10 and 72 [V].
- **Structures:** structure sets control placement with spacing, separation and frequency. A structure needs a matching biome. `structure_starts` picks sites, `structure_references` tells neighbouring chunks, and pieces are placed in the `features` step. Strongholds use a fixed ring algorithm [V].
- **Jigsaw, template pools, processor lists, structure JSON:** the page links to datapack customisation pages for these, which implies datapack-driven [V-partial].
- **Ore veins:** blocks are hardcoded but sizes can be changed with data [V]. Overworld veins span about Y -60 to 51 [V]. `ore_vein` density function from 26.3 snapshot 10 replaces `ore_veins_enabled` [V].
- **Configured and placed features:** not documented in the fetched text [U].

### 1.8 World height limits

- Dimension `min_y`: -2032 to 2031, multiple of 16 [V: https://minecraft.wiki/w/Dimension_type].
- Dimension `height`: 16 to **4064**, multiple of 16. `min_y + height - 1` must not exceed 2031 [V, same page]. So the maximum total height is **4064**, confirmed.
- Noise settings `noise.min_y` and `height` follow the same bounds. `min_y + height` must be at most 2032 [V: https://minecraft.wiki/w/Noise_settings].
- Vanilla defaults: Overworld `min_y` -64, `height` 384, `logical_height` 384. Nether and End `min_y` 0, `height` 256 (Nether `logical_height` 128, End 256) [V].

### 1.9 Datapack or code: summary

| Element | Datapack JSON? | Needs Java? | Evidence |
|---|---|---|---|
| `dimension`, `dimension_type` | Yes | No | [V] Dimension type page |
| `noise_settings`, `noise_router` and existing density function types | Yes | No | [V] Noise settings, Density function pages |
| New density function types | No (expected) | Yes | [U] |
| Biome definitions | Yes | No | [V] Biome definition page |
| Multi-noise biome source JSON details | Yes (expected) | No | [U] format not verified |
| Surface (`material_rule`), carvers, features, structures, template pools | Yes | No | [V-partial] World generation page |
| Custom `ChunkGenerator`, custom `BiomeSource` type, custom feature or structure type | No | Yes | [A] old Forge forum thread, https://forums.minecraftforge.net/topic/119160-custom-dimenson-with-custom-chunk-generator/ . Check against 26.x |
| Java API churn in worldgen classes | n/a | n/a | [V] 26.2 primer (`DensityFunction`, `SurfaceRules`, `NoiseBasedChunkGenerator`) |
| Datapack registries loaded at world load (not game start) | n/a | n/a | [V] https://docs.neoforged.net/docs/concepts/registries |

---

## 2. Dimension types, effects, skies, fog, time

### 2.1 Current `dimension_type` fields (26.3 wiki; source https://minecraft.wiki/w/Dimension_type)

| Field | Type | Default / range | Notes | Added or changed |
|---|---|---|---|---|
| `ambient_light` | float | Overworld 0.0, Nether 0.1, End 0.25 | 0 follows light level, 1 means none | original |
| `attributes` | compound | map of environment attributes | sets env attributes for the dimension | 25w42a (1.21.11) |
| `cardinal_light` | string | `default` or `nether` | cardinal light direction | 25w45a (replaced `effects`) |
| `coordinate_scale` | double | 1.0 (Nether 8.0); 0.00001 to 30000000.0 | multiplier when leaving the dimension | 1.16.2-pre2 (replaced `shrunk`) |
| `default_clock` | string | clock ID | default clock for `/time` and markers | 26.1 snapshot 3 |
| `has_ceiling` | bool | Nether true | logical bedrock ceiling; **disables weather**, changes respawn and spawn positions | original |
| `has_ender_dragon_fight` | bool | End true | enables dragon fight | 26.1 snapshot 6 |
| `has_fixed_time` | bool | `false` | fixed time | 25w45a (replaced `fixed_time`) |
| `has_skylight` | bool | Overworld and End true, Nether false | **if false, weather is disabled** | original |
| `height` | int | Overworld 384, Nether and End 256 | 16 to 4064, multiple of 16 | 20w49a |
| `infiniburn` | tag, ID or list | `#infiniburn_overworld` etc. | fire-forever blocks | 22w06a; 26.2 snapshot 1 accepts IDs and lists |
| `logical_height` | int | Overworld 384, Nether 128, End 256 | max height for chorus fruit and Nether portals | original |
| `min_y` | int | Overworld -64, Nether and End 0 | -2032 to 2031 | 20w49a |
| `monster_spawn_block_light_limit` | int | Overworld 0, Nether 15, End 0 | 0 to 15 | 1.19-pre1 |
| `monster_spawn_light_level` | int or int provider | Overworld 0 to 7, Nether 7, End 15 | per-attempt threshold | 1.19-pre1 |
| `skybox` | string | `overworld` (default), `none`, `end` | sky type to draw | 25w45a (replaced `effects`) |
| `timelines` | string, tag or list | timeline ID(s) | active timelines | 25w45a |

Source: https://minecraft.wiki/w/Dimension_type [V].

### 2.2 Removed or migrated fields

- `effects` removed in 25w45a, replaced by `skybox` and `cardinal_light` [V].
- `ultrawarm` migrated in 25w42a to `minecraft:gameplay/water_evaporates`, `minecraft:gameplay/fast_lava` and `minecraft:visual/default_dripstone_particle` [V].
- `bed_works` to `minecraft:gameplay/bed_rule`. `respawn_anchor_works` to `minecraft:gameplay/respawn_anchor_works`. `piglin_safe` to `minecraft:gameplay/piglins_zombify`. `has_raids` to `minecraft:gameplay/can_start_raid` (all 25w42a) [V].
- `natural` split: Piglin spawning to `minecraft:gameplay/nether_portal_spawns_piglin` (25w42a); the rest to `minecraft:gameplay/eyeblossom_open` and `minecraft:gameplay/creaking_active` (25w45a) [V].
- `cloud_height` (added 25w15a, 1.21.6) migrated to `minecraft:visual/cloud_height` (25w42a) [V].
- `fixed_time` replaced by `has_fixed_time` (25w45a) [V].
- The NeoForge 1.21.11 primer also lists `natural` and `effectsLocation` as removed from `DimensionType`, with `skybox` and `cardinalLightType` as replacements [V: https://docs.neoforged.net/primer/docs/1.21.11/ (offset 100000 segment)].

### 2.3 The `effects` / `DimensionSpecialEffects` mechanism is gone

- **Removed.** The NeoForge 1.21.11 primer says the "`DimensionSpecialEffects` class is removed, replaced entirely by `EnvironmentAttribute`s". It also says `BuiltinDimensionTypes#*_EFFECTS` and `ClientLevel` `effects` were removed [V: https://docs.neoforged.net/primer/docs/1.21.11/].
- Older guides for `RegisterDimensionSpecialEffectsEvent` and `DimensionSpecialEffects#renderSky` (for example https://moddedmc.wiki/en/project/stellarview/latest/docs/modding/custom_dimension_sky) describe pre-1.21.11 code. **Treat them as obsolete.** Whether the event still exists in 26.x: [U] (inference from the removal).
- **Replacement: environment attributes.** Sources, lowest to highest priority: dimension type, biome definition, timelines. Weather is not data-driven [V: https://minecraft.wiki/w/Environment_attribute].
- **Attribute IDs** [V, same page]:
  - Sky and sun: `visual/sky_color`, `visual/sun_angle`, `visual/moon_angle`, `visual/moon_phase`, `visual/star_angle`, `visual/star_brightness`, `visual/sunrise_sunset_color`, `visual/sky_light_color`, `visual/sky_light_factor`.
  - Fog and clouds: `visual/fog_color`, `visual/fog_start_distance`, `visual/fog_end_distance`, `visual/water_fog_color`, `visual/water_fog_start_distance`, `visual/water_fog_end_distance`, `visual/sky_fog_end_distance`, `visual/cloud_height`, `visual/cloud_color`, `visual/cloud_fog_end_distance`. 26.4 snapshot 2 adds `visual/has_sky_occluder`.
  - Lighting: `visual/ambient_light_color`, `visual/block_light_tint`, `visual/night_vision_color`. Particles: `visual/default_dripstone_particle`, `visual/ambient_particles`.
  - Audio: `audio/ambient_sounds`, `audio/background_music`, `audio/music_volume`, `audio/firefly_bush_sounds`.
  - Gameplay: `gameplay/bed_rule` (compound with `can_sleep`, `can_set_spawn`, `explodes` renamed `destroy_on_use` in 26.3 snapshot 3, and `error_message`), `gameplay/sky_light_level`, `gameplay/fast_lava`, `gameplay/monsters_burn`, `gameplay/can_start_raid`, `gameplay/respawn_anchor_works`, `gameplay/water_evaporates`, and more.
- **Custom environment attributes.** The wiki says datapacks cannot define new attributes [V, but the wiki history is flagged as incomplete]. The NeoForge 1.21.11 primer has a "Custom Environment Attributes" section and describes code registration through `EnvironmentAttribute#builder`, registry `BuiltInRegistries#ENVIRONMENT_ATTRIBUTE`, and `BuiltInRegistries#ATTRIBUTE_TYPE` with `AttributeType` and `LerpFunction` [V: NeoForge 1.21.11 primer, offset 0 segment]. **Conflict to resolve:** the wiki and the primer disagree on datapack-defined attributes. Treat new attributes as code-registered until checked (UNVERIFIED for datapack).
- **Fabric hook.** `DimensionEvents.MODIFY_ATTRIBUTES` in `fabric-dimensions-v1` gives the dimension and an `EnvironmentAttributeMap.Builder` after datapacks load [V: https://github.com/neoforged/NeoForge/issues/3542, reporting the Fabric API].
- **NeoForge gap.** NeoForge has no dimension-level equivalent of `BiomeModifier` for environment attributes. Issue #3542 was opened by Fuzss on 2026-09-26 and was still open with no maintainer reply when I read it. Current workarounds are overwriting the `dimension_type` JSON or mixins [V: https://github.com/neoforged/NeoForge/issues/3542].
- **Biome visuals** moved too: `Biome#getFogColor` became `EnvironmentAttributes#FOG_COLOR` in 1.21.11, and `ClientLevel#getCloudColor` became `CLOUD_COLOR` [V: 1.21.11 primer].

### 2.4 Custom skies: planets, rings, multiple suns

- The dimension `skybox` field accepts only `none`, `overworld` or `end` [V]. A custom sky needs code. No data-only route to extra suns, moons or planets was found.
- Vanilla attributes expose a single sun angle, a single moon angle and a moon phase. Star angle and brightness are also single values [V: environment attribute list]. Multiple bodies therefore need custom rendering.
- Render code moved. In the 1.21.11 primer, `SkyRenderer#renderSkyDisc` takes one ARGB int, `renderSunMoonAndStars` takes two extra floats for moon and star rotation and a `MoonPhase`, and `SkyRenderer` takes `TextureManager` and `AtlasManager` [V: 1.21.11 primer]. In 26.2 `renderSkyDisc` was renamed `render`, `SkyRenderer` takes a `RenderTarget`, and `CloudRenderer#render` became `prepare`/`renderOit` [V: 26.2 primer].
- Hook for custom sky geometry: an older NeoForge javadoc lists `RenderLevelStageEvent.Stage.AFTER_SKY` (1.21.0) [A: https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/neoforged/neoforge/client/event/RenderLevelStageEvent.Stage.html]. **Stage names in 26.x: UNVERIFIED.**
- Fog and cloud distance per dimension: set via `visual/fog_*`, `visual/water_fog_*`, `visual/cloud_height`, `visual/cloud_color` (section 2.3). These are data-driven, so a planet can set fog and cloud look by data alone. Sky body shape cannot.

### 2.5 Day length and time

- **World clocks:** `data/<ns>/world_clock/<id>.json`, an object with no fields. Registry key `Registries#WORLD_CLOCK`. Defaults: `minecraft:overworld` and `minecraft:the_end` (`the_end` added in snapshot 4). Each dimension gets its own clock. A clock stops when the `advance_time` game rule is false [V: https://minecraft.wiki/w/World_clock; NeoForge 26.1 primer].
- **Timelines** (`data/<ns>/timeline/`): `period_ticks` (repeat length; omitted means no repeat), `clock` (one clock ID), `tracks` (env attribute keyframes with `ease`, `modifier`, `keyframes` of `ticks` and `value`), `time_markers` (named ticks, `show_in_commands`). Timelines added in 25w45a. `clock` and `time_markers` in 26.1 snapshot 3 [V: https://minecraft.wiki/w/Timeline].
- **Time markers** built in: `wake_up_from_sleep`, `roll_village_siege` [V: World clock page].
- **Dimension fields:** `default_clock` (26.1 snapshot 3), `timelines` (25w45a), `has_fixed_time` (25w45a).
- **Per-dimension day length:** the wiki does not state that a dimension can run a different day length from the Overworld. In principle a separate clock plus a timeline with its own `period_ticks` should do it, but **UNVERIFIED**.
- **Java API:** `Level#getDayTime` was removed in 26.1 (use `getOverworldClockTime` or `getDefaultClockTime`). `ServerLevel#setDayTime` and `getDayCount` were removed too. `Timelines#DAY` was renamed `OVERWORLD_DAY` [V: NeoForge 26.1 primer].

---

## 3. Entity physics attributes

### 3.1 Table (source https://minecraft.wiki/w/Attribute, [V] unless noted)

| Attribute ID | Default | Range | Added | Effect |
|---|---|---|---|---|
| `minecraft:gravity` | 0.08 | -1 to 1 | 1.20.5 (24w06a) as `generic.gravity`; `minecraft:` prefix since 1.21.2 (24w33a) | gravity in blocks per tick squared |
| `minecraft:safe_fall_distance` | 3 | -1024 to 1024 | 1.20.5 (24w06a) | fall distance before fall damage accumulates |
| `minecraft:fall_damage_multiplier` | 1 | 0 to 100 | 1.20.5 (24w06a) | fall damage multiplier |
| `minecraft:jump_strength` | 0.42 | 0 to 32 | 1.20.5 (24w06a); renamed from `horse.jump_strength` | initial upward velocity (blocks/tick) |
| `minecraft:step_height` | 0.6 | 0 to 10 | 1.20.5 (23w51a) | max step-up without jumping |
| `minecraft:scale` | 1 | 0.0625 to 16 | 1.20.5 (23w51a) | size and hitbox multiplier |
| `minecraft:movement_efficiency` | 0 | 0 to 1 | 1.21 (24w18a) | reduces slowdown on soul sand-like terrain |
| `minecraft:water_movement_efficiency` | 0 | 0 to 1 | 1.21 (24w18a) | speed and drag in water |
| `minecraft:oxygen_bonus` | 0 | 0 to 1024 | 1.21 (24w18a) | underwater air loss chance becomes 1/(value+1) |
| `minecraft:air_drag_modifier` | 1 | 0 to 2048 | 26.2 snapshot 1 | air drag on the entity |
| `minecraft:friction_modifier` | 1 | 0 to 2048 | 26.2 snapshot 1 | friction |
| `minecraft:bounciness` | 0 | 0 to 1 | 26.2 snapshot 1 | bounce on landing |
| `minecraft:knockback_resistance` | 0 | -2 to 1 | range changed 26.2 snapshot 1 | knockback (not explosions) |
| `minecraft:name_tag_distance` | 64 | 0 to 512 | renamed from `nameplate_distance` in 26.2 snapshot 5 | name visibility distance |
| `minecraft:block_interaction_range` | 4.5 | 0 to 64 | 1.20.5 (23w51a) | players only |

The full 26.3 attribute table also includes `air_drag_modifier`, `below_name_distance`, `burning_time`, `camera_distance`, `entity_interaction_range`, `follow_range`, `luck`, `max_health` and others. Source: https://minecraft.wiki/w/Attribute [V].

The attribute page history uses "1.20.5, 24w06a" for `generic.gravity`. The attribute table shows default 0.08 and range -1 to 1 [V].

### 3.2 Gravity: scope and values

- **Attribute:** `minecraft:gravity`, default 0.08, range -1 to 1, in blocks per tick squared [V: https://minecraft.wiki/w/Attribute].
- **Fixed per-entity gravity values** (wiki Entity motion table, read from https://minecraft.wiki/w/Gravity). Values are m/tick^2 with negative pulling down [V]:
  - Players and living entities: -0.08. With Slow Falling: -0.01.
  - Items, falling blocks, TNT, minecarts, boats: -0.04.
  - Experience orbs, thrown eggs, snowballs, ender pearls, fishing bobbers, ice balls: -0.03.
  - Thrown splash potions: -0.05. Arrows and tridents: -0.05. Thrown bottles o' enchanting: -0.07. Llama spit: -0.06.
  - Fireballs, dangerous wither skulls, wind charges: +0.10 (accelerated by damage, not gravity). These are not gravity-driven.
- **Does the attribute affect items, projectiles and falling blocks?** The attribute is documented for living entities. Items and projectiles keep their own fixed defaults above. **UNVERIFIED** whether any non-living entity reads the attribute. The 26.2 primer adds `Entity#getEffectiveGravity`, `Entity#getAirDrag` and `Entity#getEntityBounciness`, which suggests a per-entity effective value that is not just the attribute [V: 26.2 primer, offset 100000 segment]. **Check locally in the 26.3 client or server source before design relies on it.** Do not assume that items, arrows or falling blocks follow the attribute.
- **Edge behaviour:** a Mojang bug report (MC-275195) says gravity of zero or below lets players float, and restoring the default 0.08 kicks players on a dedicated server. The report's version list only runs to 25w06a and it is marked unresolved [A: https://bugs-legacy.mojang.com/browse/MC-275195 (search snippet, not opened)]. Re-test on 26.3.
- **Setting it:** the `/attribute` command accepts `minecraft:gravity` in the bug report above [A]. Attribute modifiers can also be attached by data components or code (exact data route: [U]).
- **Mod precedents:** Gravity Control (CurseForge), Gravity Changer Unofficial Port (Modrinth) and gravity-api-fork (Modrinth) were found in search results [A]. Their technique (attribute vs custom physics) is not verified here. Agent research on showcase mods does not cover these.

### 3.3 New physics attributes in 26.2 (NeoForge primer)

- `air_drag_modifier`, `bounciness`, `friction_modifier`, `below_name_distance`, `name_tag_distance` added. `knockback_resistance` range changed to -2 to 1 [V: 26.2 primer; wiki Attribute table].
- `Entity#getEffectiveGravity`, `Entity#getAirDrag`, `Entity#getEntityBounciness` added [V: 26.2 primer].
- `LivingEntity#travelInFluid` changed from private to protected and `LivingEntity#isInShallowFluid` was added [V: 26.2 primer].

### 3.4 Attributes for a planet design

- Low and high gravity, safe falls, jump height, step height, water and movement efficiency, and oxygen air bonus are all data-readable attributes on living entities [V: attribute table].
- Items and projectiles may not follow gravity attributes (see 3.2 [U]).
- The attribute values are base values that can be modified. The exact modifier syntax for datapacks is not verified here [U].

---

## 4. Runtime dimensions: can they be added or removed without a restart?

### 4.1 Verdict

- **Vanilla and official NeoForge or Fabric API:** **NO** as far as I can find. I found no official creation API for NeoForge or Fabric 26.x [U, negative search only]. NeoForge datapack registries "load data from datapack JSONs at world load, instead of loading them when the game starts" [V: https://docs.neoforged.net/docs/concepts/registries]. The NeoForge resources page says NeoForge applies all mod data packs when a **new world** is created [V: https://docs.neoforged.net/docs/resources/].
- **Datapack-defined dimensions:** changes to a dimension file or dimension type are not picked up until the server or world restarts. `/reload` does not add new dimensions [A: bug reports MC-245063 and MC-264273 seen in search snippets, not opened; a fan datapack page advises re-entering the world].
- **Third-party libraries:** **YES.** Infiniverse, DynamicDimensions and DimLib create `ServerLevel` objects while the server runs. Persistence across restarts is the mod's responsibility in each case (see 4.2). Caveats below.

### 4.2 Libraries

| Library | Loaders | Latest seen (date) | License | Runtime API | Persistence | Caveats |
|---|---|---|---|---|---|---|
| **Infiniverse** (Commoble) | NeoForge builds (repo describes Forge) | **infiniverse-26.1.0.1.jar**, Minecraft 26.1.2+2, NeoForge, uploaded 2026-06-19. Earlier 26.1.0.0 on 2026-03-30 [V: https://www.curseforge.com/minecraft/mc-mods/infiniverse/files] | MIT [V: https://github.com/Commoble/infiniverse] | `net.commoble.infiniverse.api.InfiniverseAPI.get()`. Fires `UnregisterDimensionEvent` on the Forge bus, not cancellable [V: GitHub README] | Not documented [V] | Meant for dimensions created from user input. Static dimensions should use JSON. Clients with the mod see new dimensions in command suggestions. Modrinth page is stale (1.20.x) [V]. |
| **DynamicDimensions** (TeamGalacticraft) | Fabric and NeoForge (common module) | Branch `minecraft/1.21`. 26.x not mentioned [V: https://github.com/TeamGalacticraft/DynamicDimensions] | MIT [V] | `DynamicDimensionRegistry.from(server)` then `createDynamicDimension`, `loadDynamicDimension` (ID, `ChunkGenerator`, `DimensionType`). Remove with `unloadDynamicDimension` (saves) or `deleteDynamicDimension` (deletes files). Optional player callback [V] | Your job: "you will need to track this yourself" [V] | `createDynamicDimension` **deletes existing data for that ID**. ID and `DimensionType` must not already be in use. About a one-tick delay. Deleted files are not recoverable [V] |
| **DimLib** (iPortalTeam) | Fabric | Branch `1.21`. No release shown [V: https://github.com/iPortalTeam/DimLib] | Apache-2.0 [V] | `DimensionAPI` (javadoc). Commands: `/dims add_dimension <id> <preset>` (built-in preset `void`, others via API), `clone_dimension` (copies dimension type and generator, not blocks), `remove_dimension` (keeps save), `view_dim_config` [V] | Not stated [V] | Works while server runs or initialises. Server needs it. Client optional [V] |
| **Immersive Portals** (qouteall / iPortal) | Fabric (6.0.6, 1.21.1, Nov 2024) and NeoForge (6.0.7, 1.21.1, Jun 2025) [A: CurseForge listings] | 26.x: UNVERIFIED | Apache-2.0 [V: https://github.com/iPortalTeam/ImmersivePortalsModForNeo] | README lists "Dynamically add dimensions". Dimension API is marked experimental on the wiki [A] and moved to DimLib from 5.0.0 [A] | Not stated | README and About text disagree on the loader. Verify before using |
| **Multiverse** (senseiwells) | Fabric | Not dated [A] | UNVERIFIED | Commands to create custom dimensions at runtime [A: https://github.com/senseiwells/Multiverse] | UNVERIFIED | Reference for command flow only |
| **Custom Worlds** | Fabric and NeoForge | 1.0.1 for 1.21.1, 2026-05-28 [A: aggregator] | UNVERIFIED | Creates NORMAL, FLAT, NETHER or END worlds at runtime | UNVERIFIED | Vanilla clients can teleport with no mod [A] |
| **Multiworld** | Fabric (NeoForge claimed) | 1.14 for 26.1 to 26.1.2 (Fabric) [A: aggregator]. NeoForge build seen is for 1.21.5 | UNVERIFIED | `/mw create` with ID and dimension type, optional generator and seed [A] | UNVERIFIED | Verify NeoForge support on the official page |

Source for runtime-API facts in the table: the GitHub or project page listed in column 1, each [V] where marked.

### 4.3 Fabric and NeoForge specifics

- Fabric has no official runtime creation API that I found. The Fabric wiki dimension tutorial covers 1.15 and earlier and says 1.16+ dimensions come from datapacks [A: https://fabricmc.net/wiki/tutorial:dimensions].
- Fabric exposes `DimensionEvents.MODIFY_ATTRIBUTES` after datapacks load (section 2.3). It modifies existing dimensions, it does not create them [V via issue #3542].
- NeoForge has no equivalent of `MODIFY_ATTRIBUTES` for dimensions [V].
- Dimension types for planets: `DynamicDimensionRegistry` and DimLib's `clone` both need an existing `DimensionType`. A planet type that is not in the datapack would need a code path to register a new type. **UNVERIFIED** whether runtime dimension type creation works. Design assumption: ship dimension types as datapack entries.

### 4.4 Design-relevant facts from section 4

- Datapack dimensions for planets are fixed at world load. They can be added to an existing world only with a restart of the server or world. Shipping planets as datapack entries is the most reliable route [V registries doc; A bug reports].
- Runtime libraries can create or remove dimensions without restart, but a registered dimension must be re-created on boot by the mod's own save data (DynamicDimensions documents this; Infiniverse persistence is [U]).
- Planets created at runtime need a chunk generator instance. Planning on a shared `ChunkGenerator` implementation is therefore needed [V].

---

## 5. Showcase mods: technique, version, loader, license

Source note: versions, loaders, dates and licences come from Modrinth API and CurseForge pages. GitHub API returned 403, so source was read from HTML directory pages and raw files. Directory listings were sometimes truncated, so an item missing from a listing is not proof it does not exist. Licence texts were read through a summariser, not verbatim. Search budget ran out during this item.

### 5a. Versions, loaders, licences

| Mod | Latest 26.x build (date) | Loaders | Licence | Repo or source |
|---|---|---|---|---|
| Terralith (Stardust Labs; "Yung" credit not found) | 2.6.5+26.3 (2026-09-27) | Datapack, NeoForge, Fabric | Custom Stardust Labs licence, not OSI. Personal and server use allowed. Modified copies and forks not redistributable. AI training prohibited. Modpacks with modified datapacks need permission | https://github.com/Stardust-Labs-MC/Terralith (only branch `1.20`; 26.x source not public) |
| Tectonic (Apollounknowndev) | 3.0.31 for 26.3 (2026-09-27); datapack 3.0.29 for 26.3 | NeoForge, Fabric (no Forge 26.x) | MIT per Modrinth [U: no LICENSE file in repo root] | https://github.com/Apollounknowndev/tectonic (branch `rewrite-squared`) |
| Lithostitched (Apollounknowndev) | 2.0.5 for 26.3 (2026-10-08) | Fabric, NeoForge | MIT per Modrinth [U: no LICENSE file on branch] | https://github.com/Apollounknowndev/lithostitched (branch `cloche`) |
| William Wythers' Overhauled Overworld (WWOO, slug `wwoo`) | 3.0.1 for 26.3 (NeoForge and Fabric/Quilt, 2026-09-16) | NeoForge, Fabric, Quilt; datapack variant | CC-BY-NC-ND-4.0 per Modrinth API (datapack variant ARR, snippet) | No public source. https://modrinth.com/mod/wwoo |
| Biomes O' Plenty (Glitchfiend) | 26.3.0.0.13 (2026-10-02) | Forge, NeoForge, Fabric | All Rights Reserved (LICENSE on branch 26.3) | https://github.com/Glitchfiend/BiomesOPlenty (branch `26.3`) |
| Oh The Biomes We've Gone (Potion Studios) | No 26.x. 4.4.2 for 1.21.11 (2026-10-10); 2.6.3 for 1.21.1 (2026-10-10) | NeoForge, Forge, Fabric/Quilt | All Rights Reserved (Copyright 2023-2025 Potion Studios) | https://github.com/Potion-Studios/Oh-The-Biomes-Weve-Gone (branch `1.21.1`) |
| Alex's Caves (AlexModGuy) | 2.0.2 (2024-10-26), game version 1.20.1 only | Forge, NeoForge | LGPL-3.0-only per Modrinth; repo has no LICENSE. Conflict [U] | https://github.com/AlexModGuy/AlexsCaves (branch `main`, gradle targets 1.20.1) |
| Ad Astra (terrarium-earth) | 1.16.26 for 1.21.1 (2026-09-13). No 26.x; Forge last at 1.20.1 | Fabric, NeoForge | Terrarium Licence v1: code MIT, assets and documentation All Rights Reserved (per LICENSE summary) | https://github.com/terrarium-earth/Ad-Astra (branch `1.21.1`); https://modrinth.com/mod/ad-astra |
| Galacticraft (Team Galacticraft), Galacticraft 5 rewrite | No official release. `main` is 5.0.0-prealpha on 1.21.1 (Fabric loader 0.16.14) | Fabric (team FAQ: Fabric alpha first, Forge later, no NeoForge plan stated) | MIT, "Copyright (c) 2019-2026 Team Galacticraft" | https://github.com/TeamGalacticraft/Galacticraft (`main`, last commit 2026-04-16, snippet). Legacy 1.12.2: https://modrinth.com/mod/galacticraft-legacy (2023-12-09) |
| The Aether (The-Aether-Team) | 1.5.x for 1.21.1 (Modrinth updated 2025-10-03). No 26.x. Aether II is a separate project with 26.1.2 support | NeoForge, Fabric | LGPL-3.0 (LICENSE.txt read). LICENSE-ASSETS.txt not read | https://github.com/The-Aether-Team/The-Aether (branch `1.21.1-develop`); https://modrinth.com/mod/aether |
| Twilight Forest (Team Twilight) | NeoForge 1.21.1 build 4.8.3345 (2026-03-22). Fabric 1.21.1 build 4.8.754 (2026-10-07). No 26.x build visible [U: list truncated]. Repo branch `latest` targets 26.1.2 in gradle | NeoForge, Fabric | CurseForge "Custom License". Repo LICENSE is LGPL-2.1 or later. Sounds and structures All Rights Reserved per README. CurseForge only (Modrinth slugs 404) | https://github.com/TeamTwilight/twilightforest (branch `latest`); https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest |
| Deeper and Darker (KyaniteMods) | NeoForge 1.3.4 (2024-11-16), Fabric 1.3.3 (2024-12-20), both snippet. No 26.x. Project updated 2026-07-11 | Fabric, NeoForge (no Forge 1.21 per snippet) | AGPL-3.0-only (Modrinth API; GitHub sidebar) | https://github.com/KyaniteMods/DeeperAndDarker (checked branch `fabric-1.20.x-2.0.0s`, which is 1.20.1; 1.21 source not located) |

### 5b. How each mod generates worlds

- **Terralith:** pure datapack. The `1.20` branch has `biome`, `configured_carver`, `configured_feature`, `placed_feature`, `density_function/overworld`, `noise`, `processor_list`, `structure`, `structure_set` and `template_pool` JSON, with no Java seen. The mod requires Lithostitched. 26.x content is [U] because no 26.x source is public.
- **Tectonic:** mixed. Java in `src/common/main/java/dev/worldgen/tectonic` (`Tectonic.java`, `TectonicTags`, config, command, client, mixin, `lithostitched/`, `worldgen/placementmodifier/`). It depends on Lithostitched. Which density or noise pieces are JSON versus Java is [U].
- **Lithostitched:** a Java library (`dev.worldgen.lithostitched`) that registers codec types used from JSON. These cover features, modifiers, placement modifiers and conditions, processors, state providers, structures, surface rules, pool elements, block-entity modifiers and block predicates, plus noise wiring (`NoiseWiringHelper.java`). Modrinth describes a Fast Noise density function type, biome injectors, structure guarantees and worldgen modifiers. Authoring is data-driven on top of this codec layer, so new worldgen types can be added without a custom `ChunkGenerator`.
- **WWOO:** overhauls every vanilla biome, adds sub-biomes, uses only vanilla blocks and ships a datapack variant. There is no public source, so whether its mechanism is datapack or code is [U].
- **Biomes O' Plenty:** pure Java with no worldgen JSON (`data/biomesoplenty` holds only advancement, variant, loot_table and tags). Biomes are Java builders (`BOPOverworldBiomes`, `BOPNetherBiomes`, `BOPEndBiomes` and builder classes). Placement goes through TerraBlender: `BOPOverworldRegionPrimary` extends the TerraBlender `Region`, and its `addBiomes()` emits `(Climate.ParameterPoint, ResourceKey<Biome>)` pairs. Surface rules are Java in `BOPMaterialRuleData.java`. No custom `ChunkGenerator` or `BiomeSource` seen. The exact registration call is [U].
- **Oh The Biomes We've Gone:** Java. Packages `net/potionstudios/biomeswevegone/world/level/{block, levelgen, saveddata/maps}`, plus `BiomesWeveGone.java` and `PlatformHandler.java`. Data has `repurposed_structures` processor_list and template_pool, `rs_pool_additions` and `structure`, but no `worldgen/biome` folder (404). Biome definitions are therefore in Java levelgen or an unlisted path [U]. The 1.21.11 branch was not read.
- **Alex's Caves:** hybrid. JSON in `data/alexscaves/worldgen` (biome, configured_feature, placed_feature, structure, structure_set) and `data/alexscaves/structures`. Java in `server/level/{biome, carver, feature, map, storage, structure, surface}`. No `ChunkGenerator` or `BiomeSource` package seen. Targets 1.20.1 only.
- **Ad Astra:** data-driven planets. `data/ad_astra/dimension/{moon, mars, venus, mercury, glacio}.json`. `moon.json` sets type `ad_astra:moon`, generator `minecraft:noise` with settings `ad_astra:moon`, and `biome_source` `minecraft:fixed` on `ad_astra:lunar_wastelands`. Terrain uses vanilla noise settings and density functions in `data/ad_astra/worldgen`. Planet metadata is in `data/ad_astra/planets/*.json`, parsed by `Planet.CODEC` in `common/planets/AdAstraData.java`. Java covers custom biome, carver, feature, processor and structure classes, but no `ChunkGenerator` package. Only `moon.json` was read.
- **Galacticraft:** data-driven moon. `data/galacticraft/dimension/moon.json` uses `minecraft:noise` with settings `galacticraft:moon` and `biome_source` `minecraft:multi_noise`. Noise and density functions are in `data/galacticraft/worldgen`. Java registers dimensions (`GCDimensions`, `GCDimensionTypes`, `GCLevelStems`). `PlanetChunkGenerator` extends `ChunkGenerator` but is a stub: `fillFromNoise` returns the chunk unchanged and most overrides are empty. Whether it is referenced anywhere is [U]. The celestial-body API is in `dev.galacticraft.api.universe.celestialbody` (`CelestialBody` record, `CelestialBodyType`). The registration call is [U].
- **The Aether:** Java features, structures, tree placers and processors in `com/aetherteam/aether/world/{configuration, feature, foliageplacer, placementmodifier, processor, structure, structurepiece, treedecorator, treegrower, trunkplacer}`. No `ChunkGenerator` or `BiomeSource` in `world/`. Dimension and worldgen JSON are datagen output (`src/generated/resources/data/aether/`), contents not read. Whether the Aether dimension uses vanilla noise settings is [U]. Sky and fog rendering not checked.
- **Twilight Forest:** heavy Java. `TFBiomeProvider` extends `BiomeSource`, with a codec built on `RegistryFileCodec` over `BIOME_TERRAIN_DATA` and `BiomeDensitySource.CODEC`. `components/chunkgenerators` holds custom density functions (`AbsoluteDifferenceFunction`, `BoxDensityFunction`, `FocusedDensityFunction`, `HollowHillFunction`, `SqrtDensityFunction`, `TanhHillFunction`), `TerrainDensityRouter`, `NoiseDensityRouter`, `CustomTerrainBeardifier` and `TerrainColumn`. Other components include chunkblanketing, feature, layer, placements, processors, speleothem, structures, `TFCavesCarver` and `BiomeColorAlgorithms`. The `ChunkGenerator` class and dimension registration are [U] (no `TFChunkGenerator` in the listing). `worldgen` and `dimension` JSON paths returned 404.
- **Deeper and Darker:** structures, tags, loot tables and recipes in `data`, no worldgen folder (404). Java in `com/kyanite/deeperdarker` (not read). Structures are placed through tags. No custom `ChunkGenerator` or `BiomeSource` seen on the checked 1.20.1 branch, so the 1.21 source is not located.

### 5c. Planet APIs (what other mods can call)

- **Ad Astra:** read-only `PlanetApi` (`earth.terrarium.adastra.api.planets.PlanetApi`), loaded via `ApiHelper.load(PlanetApi.class)`. Methods include `getPlanet(Level | ResourceKey<Level>)`, `isPlanet`, `isSpace`, `isExtraterrestrial` and `getSolarPower`. The `Planet` record's CODEC keys are `dimension`, `oxygen`, `temperature`, `gravity`, `solar_power`, `solar_system`, `orbit`, `tier` and `additional_launch_dimensions`. Planets load from the `planets` datapack folder through `AdAstraData`. `AdAstraData` has a static `setPlanets(...)` that the API does not expose; its callers were not checked.
- **Galacticraft:** `CelestialBody` record (type, config) with name, gravity, position, display, galaxy and parent. `CelestialBodyType<C>` is an abstract class with a codec field and no static register method. Registries `AddonRegistries.CELESTIAL_BODY` and `BuiltInAddonRegistries.CELESTIAL_BODY_TYPE` are referenced; their packages were not opened.
- **Neither** mod ships an active custom chunk generator for planets. Both define planets as dimension JSON on the vanilla noise generator, and Galacticraft also registers dimensions in Java. Neither exposes a public Java call for adding a planet, except Galacticraft's typed registry classes (call [U]).

### 5d. Pattern across the set

- Planet mods (Ad Astra, Galacticraft) use dimension JSON on vanilla `minecraft:noise`, with planet metadata in datapack JSON.
- Biome-richness datapack mods (Terralith, WWOO) are datapack-driven. WWOO is [U] because there is no public source.
- Tectonic and Lithostitched sit on Lithostitched's codec layer. Lithostitched is the closest published precedent for adding new worldgen types without a `ChunkGenerator`.
- Biomes O' Plenty and Oh The Biomes We've Gone use Java biome builders on TerraBlender, with datapack for structures and processors.
- Twilight Forest is the heavy case: a custom `BiomeSource` and custom density functions in Java.

## 6. Fluids, weather and atmosphere

Source note: the 6 and 7 research was done with WebFetch summaries of GitHub source listings and pages, not raw source. Exact signatures should be spot-checked before code relies on them. The search budget ran out during this item, so several facts rest on a single page fetch or on a search snippet ([A]).

### 6a. Custom fluid APIs (26.3 line)

**Fabric** [V: fabric-api branch 26.3 listing]
- Module `fabric-rendering-fluids-v1`. The registry is `FluidRenderingRegistry`, not `FluidRenderHandlerRegistry` as older guides say. Static methods include `get(Fluid)`, `getOverride(Fluid)`, `register(Fluid, FluidModel.Unbaked, FluidRenderHandler)`, `register(still, flow, FluidModel.Unbaked, FluidRenderHandler)`, `setBlockTransparency(Block, boolean)`, `isBlockTransparent(Block)`. Source: https://github.com/FabricMC/fabric-api/blob/26.3/fabric-rendering-fluids-v1/src/client/java/net/fabricmc/fabric/api/client/render/fluid/v1/FluidRenderingRegistry.java
- `FluidRenderHandler` is an interface with one default method, `renderFluid(FluidRenderer, BlockPos, BlockAndTintGetter, FluidRenderer.Output, BlockState, FluidState)`. No sprite or colour getter was seen in the listing.
- Fabric binds fluid rendering to `FluidModel.Unbaked`, the same type NeoForge uses. [U] whether `SimpleFluidRenderHandler` still exists, and the package of `FluidModel`.
- Fluid storage: `fabric-transfer-api-v1`, fluid package (`FluidStorage`, `FluidStorageUtil`, `FluidVariant`, `FluidVariantAttributes`, `FluidConstants`, `CauldronFluidContent`). Source: https://github.com/FabricMC/fabric-api/tree/26.3/fabric-transfer-api-v1/src/main/java/net/fabricmc/fabric/api/transfer/v1/fluid
- Fabric API version number for 26.x: [U].
- Older fluid tutorial (https://wiki.fabricmc.net/tutorial:fluids, snippet only) uses `SimpleFluidRenderHandler` and `BlockRenderLayerMap` for translucency. Current status [U].

**NeoForge** [V: NeoForge branch 26.3.x listing]
- `RegisterFluidModelsEvent` is a client event on the mod bus, fired during model loading on a worker thread. It has `register(FluidModel.Unbaked, Fluid still, Fluid flowing)` and Supplier variants. Duplicate registration throws `IllegalStateException`. Source: https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/RegisterFluidModelsEvent.java
- Tint: `FluidTintSource` and `FluidTintSources`, plus `CustomFluidRenderer`, in `src/client/java/net/neoforged/neoforge/client/fluid/`. Source: https://github.com/neoforged/NeoForge/tree/26.3.x/src/client/java/net/neoforged/neoforge/client/fluid
- `IClientFluidTypeExtensions` is present on 26.3.x, in `client/extensions/common/` next to `IClientBlockExtensions` and `RegisterClientExtensionsEvent`. Method details [U]. This resolves an earlier open question: the type exists, but its method set is unchecked. Source: https://github.com/neoforged/NeoForge/tree/26.3.x/src/client/java/net/neoforged/neoforge/client/extensions/common
- `FluidModel` class location: [U].
- Search snippets only [A]: a DimStorage NPE on NeoForge 26.1.2.17 involving `FluidTintSource.colorAsStack` and `FluidModel.fluidTintSource()`; NeoForge issue #3058 on translucent container models in 26.1.1 (https://github.com/neoforged/NeoForge/issues/3058).

Both loaders now key fluid appearance on `FluidModel.Unbaked`.

### 6b. Weather

- Vanilla weather is **not data-driven**. The highest-priority source in the environment-attribute order is "Weather (not data-driven)" [V: https://minecraft.wiki/w/Environment_attribute]. Attributes that weather also affects: `visual/fog_color`, `visual/water_fog_color`, `visual/sky_color`, `visual/fog_end_distance`. No precipitation-type attribute exists.
- **NeoForge custom weather renderer** (client rendering only): interface `CustomWeatherEffectRenderer` with default methods `prepare(LevelRenderState, WeatherRenderState, Vec3)`, `renderSnowAndRain(...)`, `renderSnowAndRainOit(...)`, `tickRain(ClientLevel, long, Camera)`. Registered with `RegisterCustomEnvironmentEffectRendererEvent.registerWeatherEffectRenderer(Identifier, CustomWeatherEffectRenderer)` and used through `NeoForgeEnvironmentAttributes#CUSTOM_WEATHER_EFFECTS`. It draws weather. It does **not** change weather state. Sources: https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/CustomWeatherEffectRenderer.java and https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/RegisterCustomEnvironmentEffectRendererEvent.java

Precedent mods:

| Mod | Loaders | MC versions | License | Technique |
|---|---|---|---|---|
| Improved Weather (MC Silver), https://modrinth.com/mod/improved-weather | Data Pack, Fabric, Forge, NeoForge, Quilt | 26.3 down to 1.19.4 | CC-BY-4.0 | Sandstorm, sand tornado, mist, flood, fire and lightning in deserts, fireflies. Control via `/function .menu:mcs_weather`. Mechanism [U] |
| BetterWeather (bios79), https://modrinth.com/mod/betterweatherbios79 | Forge | 1.20.1 to 1.20.6 | ARR | Client dust-cloud sandstorm visuals. Daytime eclipse spawns suggest server state [U] |
| Acid Rain (Axionize), https://github.com/Axionize/acid-rain-mod | Fabric | 1.21.4 | ARR | Server `END_SERVER_TICK` damages players in rain or in water (interval 10 ticks, damage 1.0). Does not change weather |
| Acid Rain datapack (Charken Inc.), https://modrinth.com/mod/acid_rain and https://github.com/Charken-Inc/Datapacks | Bukkit, Data Pack, Fabric, Folia, Forge, NeoForge, Paper, Purpur, Quilt, Spigot | 26.2, 26.1.x, 1.21.2 to 1.21.11 | MIT | Damages players out in rain with no block above |

- Seen only, not checked: Toxicity (https://modrinth.com/mod/toxicity-mod), Twisted Tweaks (https://modrinth.com/mod/twisted-tweaks).
- Sandstorm (https://modrinth.com/mod/sandstorm) is a server-side particle engine, not a weather mod.
- Corosus Weather2 URLs returned 404. Not verified.
- **Summary:** none of the checked mods change server weather state. They add visuals or damage on top of vanilla rain.

### 6c. Breathable atmosphere and sealed rooms

**Ad Astra** [V: 1.21.1 branch only; no 26.x port seen]. Licence: Terrarium licence per Modrinth [A]. Loaders: Fabric, Forge, NeoForge. Repo: https://github.com/terrarium-earth/Ad-Astra
- `hasOxygen(Level, BlockPos)` (`OxygenApiImpl`) reads `PlanetHandler` on the server. With no entry, the default is the dimension's `Planet.oxygen`. Source: https://github.com/terrarium-earth/Ad-Astra/blob/1.21.1/src/main/java/earth/terrarium/adastra/common/systems/OxygenApiImpl.java
- `PlanetHandler`: a `HashMap<BlockPos, PlanetData>` of per-position overrides, saved via `SaveHandler` under `adastra_planet_data`. No tick or block-update listener in that file. Source: https://github.com/terrarium-earth/Ad-Astra/blob/1.21.1/src/main/java/earth/terrarium/adastra/common/handlers/PlanetHandler.java
- Sealing is in `OxygenDistributorBlockEntity` (and `OxygenLoaderBlockEntity`). `serverTick` runs every tick. `tickOxygen` runs when `time % MachineConfig.distributionRefreshRate == 0` (default 100 ticks, 5 s).
- It calls `FloodFill3D.run(level, pos.relative(offset), limit, TEST_FULL_SEAL, true)` with `limit = maxDistributionBlocks` (default **6000**).
- `FloodFill3D` (common/utils/floodfill): FIFO BFS over six face neighbours. The start block is not tested. Each neighbour passes if air or in `PASSES_FLOOD_FILL`, is blocked if in `BLOCKS_FLOOD_FILL`, is a boundary if it is a full collision block, and otherwise uses side-solid, face-sturdy and perpendicular-air checks. A boundary block that cannot be passed is added to the result but not expanded. Source: https://github.com/terrarium-earth/Ad-Astra/blob/1.21.1/src/main/java/earth/terrarium/adastra/common/utils/floodfill/FloodFill3D.java
- **Limit behaviour:** it stops at the cap with no truncation flag, so it can overshoot slightly, and a caller cannot tell a complete seal from a truncated one. Profiler label `adastra-floodfill`. No threading seen.
- Results: `lastDistributedBlocks` set. Positions are written with `OxygenApi.setOxygen(level, positions, true)` and `TemperatureApi.setTemperature`. On shutdown (60 ticks) or removal, `removeOxygen` runs. No block-change invalidation seen. The agent reports that `saveAdditional` does not write the set while `loadAdditional` reads `LastDistributedBlocks`. This looks inconsistent and should be checked.

**Galacticraft** [V: `main` branch, which is the Galacticraft 5 rewrite targeting 1.21.1 per gradle (showcase research). Sealer behaviour for 26.x is [U]. MIT per repo page]. https://github.com/TeamGalacticraft/Galacticraft
- `OxygenSealerBlockEntity` is per sealer. It needs energy and oxygen, and air above it (otherwise BLOCKED). Result is `isSealed`, statuses SEALED or AREA_TOO_LARGE. `SEAL_CHECK_TIME = 20` is only a display countdown. Source: https://github.com/TeamGalacticraft/Galacticraft/blob/main/src/main/java/dev/galacticraft/mod/content/block/entity/machine/OxygenSealerBlockEntity.java
- `SealerManager` (per level): `tick()` calls `updateSealedBlocks()` when `game time % 20 == 0`, over all sealers. Each sealer starts at the block above (offset 0,1,0) and runs a deque BFS. Barrier if in tag `SEALABLE`, or a full collision block not in `UNSEALABLE`. Other positions join `blocksToSeal` and enqueue their six neighbours. No explicit air or face-shape check. Diagonals not visited. A TODO notes that non-full blocks are handled poorly. Source: https://github.com/TeamGalacticraft/Galacticraft/blob/main/src/main/java/dev/galacticraft/mod/machine/SealerManager.java
- **Limit:** `MAX_SEALER_VOLUME = 1024` per sealer. The seal fails if `blocksToSeal.size() > sealers.size() * 1024`, so overlapping sealers in one space raise the cap. No tier dependence in this file.
- Results: `setSealed(boolean)` per sealer and `level.setBreathable(pos, bool)` per block. The cache is the sealers map plus the `sealedBlocks` set. Full recompute on each pass. No block-update or chunk-event invalidation.
- Legacy figures (800 to 1000 block capacity, 5 s vs 1 s recheck) come from forum or search snippets only [A/U].

**Comparison.** Both use a six-neighbour BFS with a hard count cap and a periodic full recompute, with no block-update invalidation.
- Ad Astra: per distributor, cap 6000, every 100 ticks, results in a per-BlockPos override map.
- Galacticraft: all sealers globally, every 20 ticks, cap 1024 per sealer, results as per-block breathable state.

## 7. Performance

Source note: as section 6. Several items rest on snippets or on a single page fetch.

### 7a. Optimisation mods

**C2ME-fabric** [V: https://github.com/RelativityMC/C2ME-fabric, branch `dev/26.3.0`; https://modrinth.com/mod/c2me-fabric]
- Fabric only. No NeoForge build listed. Licence MIT per Modrinth (licence file not read).
- Modrinth support: 26.4-snapshot-3, 26.3, 26.2, 26.1.x, 1.21.x, 1.20.x and older. The README says only the latest release and latest snapshot are fully supported.
- Latest build [U]: CurseForge snippet 0.4.2-alpha.0.102 (26.3 devbuild). A mirror lists 0.4.2-alpha.0.88 dated 2026-09-24. Modrinth benchmark file `c2me-fabric-mc26.2-0.4.2-alpha.0.41.jar`.
- Parallelises chunk generation, chunk I/O and loading across cores. Modules include `c2me-opts-worldgen-vanilla`, `c2me-opts-worldgen-general`, `c2me-opts-worldgen-biome-cache`, `c2me-opts-chunkio`, `c2me-threading-lighting`, `c2me-opts-dfc`, `c2me-opts-accel-opencl` and `c2me-notickvd`.
- README: vanilla-compatible datapacks are fully supported. Mod-based custom world generators "usually run well" but may conflict. Density-function support is implied only by the module name `c2me-opts-dfc` [U].
- Recommends Lithium and Starlight. Notes vanilla worldgen is non-deterministic.

**Noisium** [V: https://github.com/Steveplays28/noisium; https://modrinth.com/mod/noisium]
- Licence LGPL-3.0-only (Modrinth). Loaders Fabric, Quilt, Forge, NeoForge. Server-side only.
- Modrinth support: 1.21 to 1.21.6 and 1.20.x. No 26.x listed. Latest version [U].
- Changes: `NoiseChunkGenerator#populateNoise` writes block states directly to palette storage. Also optimises biome population, block-state sampling and chunk unlocking (1.21+). Claims 1:1 vanilla parity.
- README speed claim: "a few seconds to a few dozen seconds faster depending on the amount of chunks generated". Spark profiles linked but not read.
- Forks (snippet only [A/U]): NoisiumForked (server-focused, Fabric and NeoForge), Noisiumed (Steveplays28 fork), an unofficial Forge port.

**Gap:** Noisium has no verified 26.x support. On Fabric, C2ME is the only 26.x option found. No NeoForge optimisation mod was found in this run.

### 7b. Chunk generation cost

- No credible source ranks vanilla worldgen cost across noise router density, surface, features and lighting. The minecraft.wiki Chunk page does not cover it. That noise router density dominates is [U].
- Noisium names `populateNoise` block placement, biome population and blockstate sampling as its optimised areas, unranked. C2ME module names show which areas are parallelised, not cost shares.

### 7c. How many dimensions can stay loaded

- minecraft.wiki Chunk [V via page summary: https://minecraft.wiki/w/Chunk]:
  - Chunks start loading when they receive a ticket.
  - Load level 31 and below gets full processing. Level 32 is block ticking only, with no natural spawning.
  - Each dimension has an idle timeout. After 300 idle ticks it stops certain processing, including entity and block-entity ticking.
  - From 1.21.9, a dimension stays active while it has player activity, forced chunks, active portals or ender pearls.
  - Unloaded chunks do not process. Java chunk ticks apply within 128 blocks of a player. Entity-ticking square side is `l = min(2s+1, 63)`, where `s` is simulation distance.
- Whether every `ServerLevel` object is ticked, and what idle dimensions still run, is [U]. The wiki describes idle processing only.
- NeoForge `ForcedChunkManager.hasForcedChunks(ServerLevel)` decides whether a level keeps ticking with no players. Source: https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/common/world/chunk/ForcedChunkManager.java
- No numeric cap on loaded dimensions was found [U].

### 7d. Chunk tickets and forced chunks

**NeoForge 26.3.x** (package `net.neoforged.neoforge.common.world.chunk`) [V: source listing]
- `TicketController` record `(Identifier id, @Nullable LoadingValidationCallback callback)`. `forceChunk` overloads take a BlockPos owner, an Entity owner or a UUID owner. Passing `add=false` unforces. No `releaseTicket`. Source: https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/common/world/chunk/TicketController.java
- `RegisterTicketControllersEvent` (mod bus): `register(TicketController)`. A duplicate ID throws `IllegalArgumentException`. Tickets of unregistered controllers are discarded at world load, and `forceChunk` throws for them.
- `ForcedChunkManager`: `hasForcedChunks(ServerLevel)`. Save field `neo_ticket_data`.
- Whether `TicketController` succeeds `ForgeChunkManager` is [U]. The name suggests it.

**Fabric 26.3** [V: source listing]
- `ServerChunkEvents` (fabric-lifecycle-events-v1) is notification-only: `CHUNK_LOAD`, `CHUNK_UNLOAD`, `FULL_CHUNK_STATUS_CHANGE`. `CHUNK_GENERATE` is deprecated in favour of `CHUNK_LOAD`. Source: https://github.com/FabricMC/fabric-api/blob/26.3/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/api/event/lifecycle/v1/ServerChunkEvents.java
- No Fabric forced-chunk API was found. Forcing chunks on Fabric would go through vanilla `ServerChunkCache` ticket APIs [U, not fetched].

### 7e. Chunk loading for moving ships and contraptions

- **Valkyrien Skies 2, official** [V: https://github.com/ValkyrienSkies/Valkyrien-Skies-2, LGPL-3.0]. Branches shown: `1.20.1/main` (default) and fix or playtest branches. No 26.x or 1.21 branch. Loaders Forge and Fabric. Version conflict: README says `2.5.0-beta.3`, while issue #1768 cites `v2.4.10` for 1.20.1 (snippet). The ship chunk-loading code was **not located** [U]. Physics core location [U].
- **Unofficial VS port** [V: https://modrinth.com/project/GE25t0ct; https://www.curseforge.com/minecraft/mc-mods/vs-unofficial-port]. Loaders Fabric and NeoForge. MC 26.2, 1.21.11 and 1.21.1 on Modrinth. Licence LGPL-3.0-only. "View source" points to the official repo. Author not named. CurseForge builds: NeoForge `valkyrienskies-26-2.jar` and Fabric `ValkyrienSkies-Fabric-MC1.21.11-v3.1.1.jar`, both dated 2026-08-24. No 26.1 or 26.3 support listed.
- **Create** [V partial: https://github.com/Creators-of-Create/Create, default branch `mc1.21.1/dev`]. No 26.x branch visible. Licence [U]. `AbstractContraptionEntity` has no chunk-ticket code; its only chunk reference is a comment about chunk-grouped passenger saving. `ContraptionHandler.tick(Level)` runs per level, moves contraptions and collides entities, with no chunk loading. Absence across the whole repo is [U] (two files checked).
- **Gap:** no verified ship chunk-loading mechanism. A moving ship probably needs its own ticket strategy. On NeoForge, `TicketController` is the candidate [U].

---

## Sources (fetched or seen)

- https://minecraft.wiki/w/Java_Edition_26.3 [V]
- https://minecraft.wiki/w/Attribute [V]
- https://minecraft.wiki/w/Dimension_type [V]
- https://minecraft.wiki/w/Custom_dimension (Dimension definition article) [V]
- https://minecraft.wiki/w/Environment_attribute [V]
- https://minecraft.wiki/w/Timeline [V]
- https://minecraft.wiki/w/World_clock [V]
- https://minecraft.wiki/w/Density_function [V]
- https://minecraft.wiki/w/Noise_router [V]
- https://minecraft.wiki/w/Noise_settings [V]
- https://minecraft.wiki/w/World_generation [V]
- https://minecraft.wiki/w/Biome_definition_(Java_Edition) [V]
- https://minecraft.wiki/w/Gravity (returned the Entity motion table) [V]
- https://docs.neoforged.net/primer/docs/1.21.11/ [V, partial]
- https://docs.neoforged.net/primer/docs/26.1/ [V, partial]
- https://docs.neoforged.net/primer/docs/26.2/ [V, partial]
- https://docs.neoforged.net/primer/docs/26.3/ [V, partial]
- https://docs.neoforged.net/docs/fluids/ [V]
- https://docs.neoforged.net/docs/concepts/registries [V]
- https://docs.neoforged.net/docs/resources/ [V]
- https://github.com/neoforged/NeoForge/issues/3542 [V]
- https://github.com/Commoble/infiniverse [V]
- https://www.curseforge.com/minecraft/mc-mods/infiniverse/files [V]
- https://github.com/TeamGalacticraft/DynamicDimensions [V]
- https://github.com/iPortalTeam/DimLib [V]
- https://github.com/iPortalTeam/ImmersivePortalsModForNeo [V]
- https://github.com/senseiwells/Multiverse [A, search only]
- https://minespecs.com/minecraft-versions/ [A]
- https://bugs-legacy.mojang.com/browse/MC-275195 [A]
- https://forums.minecraftforge.net/topic/119160-custom-dimenson-with-custom-chunk-generator/ [A]
- https://moddedmc.wiki/en/project/stellarview/latest/docs/modding/custom_dimension_sky (pre-1.21.11, obsolete) [A]
- https://fabricmc.net/wiki/tutorial:dimensions (1.15 era) [A]
