# R7: Spherical and round planets for Apoapsis (techniques and precedents)

Date: 2026-10-11. Research node R7. Facts, precedents and write-ups only. The design choice belongs to the Opus designer.

## Legend and method

Every non-obvious fact carries one of these tags and a URL.

- **[V]** Verified: the page was fetched with WebFetch in this session and the fact was checked against the fetched text.
- **[S]** Search-excerpt only: seen in a search result snippet, or the fetch failed or returned a stub. Treat as unconfirmed.
- **[UNV]** Unverifiable with the tools used. Do not rely on it.
- **[INF]** My own inference or arithmetic, labelled as such.

Fetch failures worth knowing: the KSP forum and KSP official dev-insight pages (403/404), the Reddit original (blocked), the Mojira MC-7363 page (stub), the MoDDB Imagine Nations page (403), the gamedev.net geodesic blog (403), the arXiv PDF for Andrews and Jablonowski (binary, numbers not readable), the PC Gamer Outer Wilds article (headline only), and the mathproofs cube-to-sphere page (title only).

Corrections found while checking sources:
- Pertam atmosphere: one search excerpt said breathable air near 4.7 km. The fetched wiki page says "Atmospheric height (sea level): 1.6 km" [V]. Trust the fetch.
- Distant Horizons: the fetched GitLab page describes the project at Distant-Horizons-Team/distant-horizons, not the jeseibel path in one search excerpt [V].
- The claim that Minecraft's world border hard-codes 29,999,999 is not in the minecraft.wiki page [V, negative]. It appears only in a fandom excerpt [S].
- Transvoxel's "shipped in Space Engineers and Astroneer" claim comes from a blog [S]. The transvoxel.org page names no games, only the C4 Engine [V].

---

## 1. Voxel and round-planet representations in other games

### 1.0 Quick comparison

| Game | Planet surface representation | Gravity model | Atmosphere / space transition | Confidence |
|---|---|---|---|---|
| Space Engineers (SE1) | Heightmap (one height per column) on a planet-sized voxel body with a fixed diameter [V] | Radial field with a flat zone near the surface, then falloff with altitude [S] | Pertam: gravity well top 18.51 km, atmosphere 1.6 km [V] | High for heightmap and clusters; low for gravity curve |
| Space Engineers 2 (VRAGE3) | Heightmap projected as a cube onto the planet, plus a detail heightmap, plus volumetric overhangs and caves [V] | Not found | Not found | Medium |
| Astroneer | Deformable voxel terrain; layered noise for generation [S] | Not found | Not found | Low |
| Empyrion | Deformable voxel terrain; older "heightmap planets"; 2016 plan for true spheres, not confirmed shipped [S] | Per-planet gravity [S] | Flight from space without loading screens except at poles [S] | Low |
| No Man's Sky | Voxel-based generation, then polygonization, texturing, population (GDC listing) [S]; "stored as cubes, rendered as projected spheres" is secondhand [S] | Not found | Not found | Low |
| Seed of Andromeda | One cube with a grid per face, normalized to a sphere (developer comment, 2015) [S] | Not found | Not found | Low |
| Planet Explorers | OpenCL voxel terrain; story map 17 by 8 km, adventure map 40 by 40 km [S] | No spherical evidence found [UNV] | Not found | Low |
| Starbase | Hybrid voxel and vertex physics, fully destructible [S] | No devlog found [UNV] | Not found | Low |
| Outer Wilds | Small spherical bodies, one sphere of influence each [S] | Sun inverse-square; planets linear falloff; alignment fields [S] | Not applicable | Medium |
| KSP 1 and KSP 2 | PQS (procedural quad sphere) per cube face with quadtree LOD [S] | Radial | Scatterer atmosphere [V for README] | Medium |
| Kitten Space Agency | Spherical billboards for distant planets, grid-snapped reference vertex [S, secondhand] | Not found | Not found | Low |
| Imagine Nations | Cubic planets with faces segmented from each other; gravity admitted to be imperfect [S] | Spherical gravity on cube faces, acknowledged as a compromise [S] | Not found | Low |
| Zoxel (indie voxel engine) | Six PlanetChunks, each with its own heightmaps, built from a cube sized to the radius, then made spherical [V] | Gravity flips rotation when a character crosses an edge; per-object "gravity quadrant" byte [V] | Not found | High (single devlog) |

### 1.1 Space Engineers (SE1)

- **Terrain representation.** The SE2 devlog says SE1 terrain was mostly limited to pyramid-shaped hills and simple rock because the surface was a heightmap with one value per voxel [V: https://2.spaceengineersgame.com/mareks-dev-diary-august-21-2025/].
- **Planet creation.** The community wiki planet guide describes a planet mod as needing a heightmap, a biome map and an occlusion map, with 2048x2048 textures and 1 pixel = 20 m [S: https://spaceengineers.wiki.gg/wiki/How_to_create_your_own_planet]. Six biome textures assign properties to surface areas [S, same page].
- **Precision and clusters.** The game uses 64-bit doubles, Havok uses 32-bit floats, and the world is split into independent clusters of 50 to 100 km (minimum 20 km). A clustering algorithm keeps dynamic objects at least 2 km from a cluster border [V: https://spaceengineers.wiki.gg/wiki/Planetary_System].
- **Planet size, gravity and atmosphere (Pertam).** Diameter 60 km, surface gravity 1.2 g, "Max Gravity well altitude: 18.51 km", "Atmospheric height (sea level): 1.6 km". The page does not say whether heights are measured from the surface or the centre [V: https://spaceengineers.wiki.gg/wiki/Pertam].
- **Titan (search excerpt).** Gravity reach about 3 km, sea-level atmosphere about 0.6 km, surface gravity 0.25 g, diameter 19 km [S: https://spaceengineers.wiki.gg/wiki/Titan]. Planets range from 19 to 120 km in diameter, and each has its own gravity field [S: https://spaceengineers.wiki.gg/wiki/Planets].
- **Gravity falloff.** The wiki's Gravity page describes a flat zone that includes the surface, a decline with distance above it, and a separate decline toward the core. It gives no formula [S: https://spaceengineers.wiki.gg/wiki/Gravity]. A Keen forum post claims a seventh-power falloff, but the poster's own measurements disagreed [UNV: https://support.keenswh.com/spaceengineers/pc/comments/40272/vote/toggle].
- **Moons.** Moons have weaker fields, 0.25 g [V: Planetary_System page above].
- **Leaving a planet.** Vanilla SE1 needs hydrogen engines, or a hybrid craft with atmospheric engines for liftoff and ion engines above the atmosphere [S: https://spaceengineers.wiki.gg/wiki/Planetary_Entry/Exit, search excerpt].
- **Orbits.** A developer post says orbits are not a feature because planets do not move. A ship must be held at a fixed distance to stay in place [S: Steam discussions, search excerpt].
- **Rendering and LOD.** Transvoxel is said to ship in SE [UNV: https://app.cinevva.com/blog/2026-02-25-open-world-browser-part-09-transvoxel-first-cut]. No primary source found.

### 1.2 Space Engineers 2 (VRAGE3 engine, VS2 milestone)

- **Surface.** The surface is still a heightmap, but it is a cube projected onto the planet, with one height per location. The base heightmap samples 1 pixel per 8 voxels (voxels are 1 m). A detail heightmap raises this to 1 pixel per voxel [V: https://2.spaceengineersgame.com/mareks-dev-diary-august-21-2025/].
- **Beyond 2.5D.** Small pre-built overhang and boulder pieces are placed procedurally. Caves are cut by subtracting material. The devlog says the surface is "no longer merely 2.5D" [V, same].
- **Material transitions.** In SE1 blends stayed within one voxel triangle. In SE2 they can span several voxels, with a height-aware transition mask [V, same].
- **Sphere or cube.** The devlog never says whether planets are true spheres [V, negative, same].
- **Content.** The Almagest system with planets Verdure and Kemik (landable) [S: https://ingamenews.com/2025/11/space-engineers-2-massive-star-system.html; https://2.spaceengineersgame.com/mareks-dev-diary-november-13-2025/ (search listing)].
- **Gravity, atmosphere, precision, transitions.** No official figures found [UNV]. An SE2 generators PDF gives a power-law formula for artificial generators, not natural planets [S: https://2.spaceengineersgame.com/wp-content/uploads/2025/07/SE2_Generators.pdf].

### 1.3 Astroneer

- Built on Unreal Engine 4. Engineer Wolpert (named only by surname in the spotlight excerpt) describes rewriting procedural generation as layered noise that generates terrain and places objects on the fly [S: https://www.unrealengine.com/en-US/spotlights/how-system-era-softworks-leveraged-ue4-to-create-astroneer-s-wonderful-universe]. The excerpt was cut off.
- Seven worlds: planets Sylva, Calidor, Vesania, Glacio, Atrox, and moons Desolo and Novus [S: https://astroneer.wiki.gg/wiki/Astroneer].
- Planet geometry (sphere, meshing method, marching cubes) not verified [UNV]. An academic "planetary marching cubes" method exists [S: https://research.polyu.edu.hk/en/publications/planetary-marching-cubes-a-marching-cubes-algorithm-for-spherical/]. Its use in Astroneer is not shown.

### 1.4 Empyrion: Galactic Survival

- Each planet has voxel terrain that players can deform. Some tools are needed to mine the ore required for crafting [S: https://empyriongame.com/press-kit, attribution not confirmed].
- Early-access player report: flight was possible almost anywhere without loading screens except the poles. A 2016 developer plan proposed replacing a "tricked tube" map with true spherical maps. Dated, and not confirmed shipped [S: https://steamcommunity.com/app/383120/discussions/0/405691147593200663].
- Alpha 8 scaled planets up to 64x, from 32 km^2 to 2048 km^2, and listed procedural terrain generation. Older "heightmap planets" were re-decorated after a system overhaul [S: https://www.moddb.com/news/road-to-alpha-8-development-update].

### 1.5 Starbase

- Official site describes a fully destructible world with hybrid voxel and vertex physics [S: https://www.starbasegame.com/].
- No Starbase devlog on spherical voxel planets or gravity was found [UNV]. Wccftech describes a custom engine and a 2019-era roadmap [S: https://wccftech.com/starbase-hugely-ambitious-vertex-voxel-space-sandbox-mmo/amp/].

### 1.6 No Man's Sky

- GDC 2017 talk "Continuous World Generation in No Man's Sky" (Innes McKendrick). The listing describes a pipeline from voxel-based generation through polygonization and texturing to population and simulation [S: https://gdcvault.com/play/1024265/Continuous-World-Generation-in-No]. The transcript was not reachable.
- A 2015 Reddit thread says a Hello Games speaker at nucl.ai described planets stored as cubes and rendered as projected spheres. The thread names a different speaker, so the link to the GDC talk is unconfirmed [S: https://redlib.hbubli.cc/r/VoxelGameDev/comments/3ese9u/spherical_world_generation].
- Sean Murray's separate GDC 2017 talk "Building Worlds Using Math(s)" [S: https://gdcvault.com/play/1024514/Building-Worlds-Using].

### 1.7 Seed of Andromeda

- A developer flair comment on the same 2015 Reddit thread: "We use a giant cube where each plane is a separate grid, and then use rendering tricks (normalization) to turn it into a sphere" [S: https://redlib.hbubli.cc/r/VoxelGameDev/comments/3ese9u/spherical_world_generation]. Secondhand. The current state of this system is unknown [UNV].

### 1.8 Planet Explorers (Pathea)

- Voxel terrain computed with OpenCL, reshapable and placeable anywhere. Story map 17 by 8 km. Adventure mode generates a 40 by 40 km map from a seed [S: https://store.steampowered.com/app/237870/Planet_Explorers]. No sphere or wrap evidence found [UNV].

### 1.9 Outer Wilds (small planets, gravity design)

- Mobius's GDC 2020 talk "The 4D Level Design of Outer Wilds" with Alex Beachum [S: https://gdconf.com/news/see-4d-level-design-outer-wilds-deconstructed-gdc-2020].
- Per a PC Gamer excerpt (full article not retrieved): the inverse-square law applies only to the sun. Applying it to planets made escape velocity too easy, so planet gravity falls off linearly. Each body has a sphere of influence, and planets do not pull on each other. Alignment fields orient the player toward masses [S: https://pcgamer.com/how-to-build-a-black-hole, headline fetch only].

### 1.10 Kerbal Space Program (PQS) and KSP2

- The KSP2 dev insight says KSP2 uses "a very similar PQS (procedural quad sphere system) that was used in KSP1". Mesh generation runs in compute shaders and is drawn procedurally on the GPU. Vertex positions are computed relative to the floating origin, not the planet centre, to keep values small [S: https://www.kerbalspaceprogram.com:443/news/dev-diaries-developer-insights-12-planet-tech; page fetch returned 404, forum copy 403].
- Community forums describe PQS as "Procedural Quadtree Sphere" and describe PQS mods such as VertexPlanet and VertexSimplexHeightAbsolute [S: https://forum.kerbalspaceprogram.com/topic/184506-at-what-altitudes-does-pqs-terrain-surface-stop-rendering].
- Krakensbane: a community explanation says the origin resets to the player past a distance threshold, and velocity is absorbed past a speed threshold [S: https://forum.kerbalspaceprogram.com/topic/91108-how-does-ksp-work]. No official doc found [UNV].
- KSP1 seams between quads were reported as a precision issue in absolute quad positions [S: https://bugs.kerbalspaceprogram.com/issues/13213].

### 1.11 Kitten Space Agency

- Secondhand (Reddit mirror, Nov 2025): planets drawn with "spherical billboarding" from pre-generated sphere libraries with varying subdivisions; reference vertex snapped to a grid to avoid shimmer; terrain features such as erosion computed on the fly [S: https://r.datuan.dev/r/kittenspaceagency]. The original devlog was not found.

### 1.12 Imagine Nations

- Designers plan cubic planets where each face is segmented from the others. They say simulated gravity falls short of a true cube, where gravity stays spherical [S: https://www.moddb.com/games/imagine-nations/news/the-science-of-imagine-nations-planets; page fetch 403].

### 1.13 Zoxel (indie voxel engine, closest match to per-face block worlds)

- Each planet is six PlanetChunks, each with its own heightmap and other maps, combined into the final terrain. The planet is first generated as a cube sized to its radius, then made spherical. Making it spherical required rework of physics and the character controller, which took close to a month [V: https://deus0.itch.io/zoxel/devlog/305343/planets-portals].
- Gravity: when a character goes over an edge, gravity flips its rotation. Each character or physics object stores a "gravity quadrant" byte [V, same].
- Known issue: raising the heightmap increases fall distance; a gravity field is planned [V, same].
- Trees now generate across chunks because Y chunks are handled [V, same].

### 1.14 Other voxel-sphere projects (for reference, low confidence)

- Blocky Planet (Unity tech demo): miniature planets of 20+ block types; mesh gaps between vertical shells [S: https://bowerbyte.itch.io/blocky-planet].
- Cuberact Godot "Procedural Planet - Chunked LOD" (MIT): quadtree chunked LOD, cube-to-sphere, origin shifting, shader-noise terrain, no seams or gravity noted [S: https://www.godotengine.org/asset-library/asset/edit/21636].
- planet-builder-web (BabylonJS): spherical voxel planet, marching-cubes-like meshing [S: search listing].
- PaperPrototype VoxelSystem (Unity): SmallPlanet sample with gravity script; README warns descriptions may be outdated [S: https://github.com/PaperPrototype/VoxelSystem].

### 1.15 Transition and LOD technique used by voxel planets

- Transvoxel (Eric Lengyel): stitches neighbouring marching-cubes meshes at different resolutions using transition cells. Invented 2009; patent-free; implemented in the C4 Engine; transition cells need only local voxel data to rebuild after edits [V: https://transvoxel.org/]. The page lists no games.
- Skirts (extra faces on LOD boundaries) are a simpler option used by one developer [S: r/VoxelGameDev thread via search, https://redlib.hbubli.cc/r/VoxelGameDev/comments/1ae7mfg/seams_octree].

---

## 2. Geometry techniques

### 2.1 Mappings from cube to sphere

- **Normalized cube (radial projection).** Normalize each point of a cube face to the unit sphere. Face centres are enlarged much more than edges. This is the baseline distortion [V: https://redblobgames.com/x/1938-square-tiling-of-sphere].
- **Equal-angle / tangent adjustment.** Pre-warp each cube coordinate so grid lines are equal in angle, using the forward transform pi/4 * atan(w) before normalization. The author flags the inverse tan(w' * pi) as suspicious and says it does not match the forward transform. The author concludes "We can't have it all": area, aspect and angle distortion can only be moved between types or around the cube [V: same page].
- **Spherified cube.** A Unity forum thread gives the per-axis form x' = x * sqrt(1 - y^2/2 - z^2/2 + y^2 z^2/3), with the other axes cyclic [S: https://discussions.unity.com/t/problems-at-mapping-cube-to-a-sphere/66729]. Check: at (1,1,1) each component is sqrt(1/3) = 1/sqrt(3), so the corner lands on the unit sphere [INF]. The mathproofs cube-to-sphere page was fetched but returned only its title, so the formula and author attribution there are unconfirmed [V, negative: http://mathproofs.blogspot.com/2005/07/mapping-cube-to-sphere.html]. The attribution to Everitt is [UNV].
- **Gnomonic family (equidistant, equiangular, equi-edge).** NCEP's GRIB2 documentation uses a parameter B: B=0 gives Sadourny's equidistant grid (1972), B=1 gives the equiangular grid, and B=1/2 gives uniform spacing along each cube edge [S: https://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc/grib2_temp3-60.shtml].
- **Distortion numbers (from a search summary of Andrews and Jablonowski, arXiv:2505.05624).** Equiangular cell-area max/min ratio approaches sqrt(2) for odd panel sizes, citing Ronchi et al. 1996. Equidistant max/min area ratio is reported as more than three times the equiangular value. Aspect ratio peaks at sqrt(2) at edge midpoints for equidistant and equiangular grids [S: https://arxiv.org/abs/2505.05624]. The full text could not be read via WebFetch, so these figures are [UNV] until someone reads the paper.
- **Ronchi, Iacono and Paolucci (1996), "The cubed sphere".** Six congruent patches from projecting a circumscribed cube. Grid lines are great-circle arcs, so there are no coordinate singularities, and all six share one metric. The six grids are coupled by a composite-mesh finite-difference scheme. Published in J. Comput. Phys. 124, 93-114 [S: https://sostenibilita.enea.it/en/node/5736, abstract and bibliographic record only].
- **Geodesic and icosahedral grids.** A Goldberg polyhedron has exactly 12 pentagonal faces (Euler's formula), with icosahedral symmetry. Vertex count for icosahedral forms is T = m^2 + mn + n^2. A Goldberg polyhedron is the dual of a geodesic polyhedron [V: https://en.wikipedia.org/wiki/Goldberg_polyhedron]. A gamedev blog planned to place voxels on a geodesic sphere, arguing that cube-based spherical voxel worlds need awkward space warping [S: https://gamedev.net/blogs/entry/2265006-planet-generation-plans; fetch 403].
- **Tangent frames.** Any continuous tangent field on a sphere must have singularities (the hairy ball theorem). One developer hit this computing tangents for a spherified-cube mesh [S: https://gamedev.net/forums/topic/717873-make-normals-from-terrain-in-spherical-form]. Per-face tangent frames need a seam rule.
- **Quadtree per face.** Each face is subdivided by a quadtree for LOD. A Patreon post describes six quadtrees with cross-face neighbour logic [S: https://www.patreon.com/posts/25870973, search excerpt only].

### 2.2 Seams and corners for a block grid

- A cube-sphere has six faces that meet along 12 edges and 8 corners. At each corner three faces meet, so cells there have valence three, not four [INF, standard quad-mesh topology]. This is why the cubed-sphere papers emphasise avoiding coordinate singularities: the metric stays smooth, but the topology still has 8 three-valent corners [INF].
- For axis-aligned blocks, a block near an edge must be re-oriented to the neighbouring face's axes. No verified write-up for Minecraft block faces was found [UNV].
- Seams between LOD levels are handled by Transvoxel [V] or by skirts [S].
- A Unity forum question notes that the player's y-axis no longer aligns with a voxel face away from face centres [S: https://discussions.unity.com/t/smooth-voxel-planets/168810].

### 2.3 Gravity and "up" per face

- **Radial gravity.** "Up" is the normalized vector from centre to position. In a block world, that means a per-block or per-chunk up vector, which cannot be smooth across seams (see hairy ball above) [INF].
- **Per-face gravity with edge flip.** Zoxel flips gravity rotation when crossing an edge and stores a per-object face quadrant [V: https://deus0.itch.io/zoxel/devlog/305343/planets-portals].
- **Accepted compromise.** Imagine Nations accepts a gravity model that is not truly spherical on cube faces [S: https://www.moddb.com/games/imagine-nations/news/the-science-of-imagine-nations-planets].
- **Alignment.** Outer Wilds uses alignment fields to orient the player toward a mass [S: https://pcgamer.com/how-to-build-a-black-hole].
- **Per-body gravity on moving ships.** Valkyrien Skies gravity mods exist but are unofficial [S: https://moddex.gg/mod/gravityvs-mr-Hpc0M50G].

### 2.4 Torus and wrap-around worlds as fake spheres

- **Toroidal World** (NeoForge and Fabric; Minecraft 26.3, 26.2, 26.1.2, 1.21.1): toroidal (both horizontal axes loop) and cylinder (one axis loops) shapes, set at world creation, size presets tiny to huge via server level-type. Shape cannot be converted for existing worlds. Pre-1.0. Distant Horizons shows the far side across the seam out to half the world width [V: https://modrinth.com/mod/toroidal-world].
- **Torus versus sphere.** A Minecraft feedback thread notes wrapping in all four directions gives a torus, and a true sphere needs much more design work [S: https://feedback.minecraft.net/hc/en-us/community/posts/360011428891/comments/360001293472].

---

## 3. Minecraft-specific precedents

### 3.1 Curvature and world-bending shaders (visual only)

- **World Curvature Shaders** bends the landscape toward the horizon so the world looks like a small planet, with an inverted fish-eye option. It omits shadows and grass animation. Third-party download site, dated 2023, loader not confirmed [S: https://www.9minecraft.net/?p=469643].
- **Vertigo Shader** (Iris, Sodium, Minecraft 1.19.x to 1.21.x): 10 distortion modes, including "Horizon Fracture" and "Gravity Well" [S: https://www.curseforge.com/minecraft/shaders/vertigo-shader].
- **Cinematic Horizon Shaders** [S: https://www.modpackindex.com/mod/175748/cinematic-horizon-shaders].
- Shaders change perception only, not physics or collision [S, Vertigo listing].

### 3.2 Immersive Portals (portals that change scale and gravity, multi-dimension)

- Archived by its owner and read-only. Apache-2.0 licence. README: "The portal can transform player scale and gravity direction." Supports portal-in-portal rendering and loading several dimensions at once, with a dynamic dimension API [V: https://github.com/iPortalTeam/ImmersivePortalsMod].
- Successors: a community NeoForge 1.21.1 build at UpperMoon0/ImmersivePortals [S: search listing] and the Immersive Portal CE build, not affiliated with the original maintainers [S: https://www.modpackindex.com/mod/175287/immersive-portal-ce].
- Dimension stacking in practice: Void Dimension Stack [S: https://www.modpackindex.com/mod/79118/void-dimension-stack]; Abyssal Ascent climbs through more than twelve stacked dimensions [S: search listing, https://www.boxtoplay.com/en/serveur-minecraft/modpacks/modpack-abyssal-ascent-2563].
- Gravity direction and strength are also set by the Gravity Changer family. Its commands set a base direction and base strength, overridable by effects, anchors and plating [S: https://www.mado-hosting.com/minecraft-mods/gravity-api-fork]. Per-dimension gravity appears only as strength, not direction [S: search listing for Screw Gravity modpack].

### 3.3 Wrap-around worlds and round-world experiments

- **Toroidal World** (see 2.4) [V].
- **Wrap Around World** is a datapack, not a mod. Crossing the boundary triggers relocation to the far side, not seamless terrain. Default border 3500 gives a 7000 by 7000 block play area [S: https://modrinth.com/mod/wrap-around-world].
- **Spherical World** (CurseForge): reaching an edge teleports you to the opposite side. Listing does not show curved terrain [S: https://www.curseforge.com/minecraft/mc-mods/spherical-world].
- **NeoBiosphere** (NeoForge 1.21.1): generates multiple spheres connected by bridges; radius and spacing configurable; warns about overworld noise-setting datapacks [S: https://www.modpackindex.com/mod/92088/neobiosphere].
- **TFC Eratosthenes** (TerraFirmaCraft add-on): world follows a map projection; crossing 180 degrees moves to the other side [S: https://www.curseforge.com/minecraft/mc-mods/tfc-eratosthenes]. Not a true sphere.
- **Robin's Outer Wilds** prototype: overworld is a looping area of 32 by 32 chunks; leaving the planet drops you into zero gravity [S: https://www.modpackindex.com/mod/147715/robins-outer-wilds].
- **Big Globe Mod**: a third-party page describes a spherical world wrap. Low-quality text, no developer or version confirmed [UNV: https://common-lemur.origin.boxcast.com/big-globe-mod].

### 3.4 Valkyrien Skies and space-dimension mods (planets as bodies in space)

- **Genesis** adds a space dimension with customizable orbiting planets and stars, defined by datapacks, with size, gravity, atmosphere, orbit speed, and tidally locked skyboxes. Pehkui and Valkyrien Skies scale the space dimension to 1/16 size for speed. Flying a VS ship to y = 2048 sends ship and pilot to space [S: https://moddex.gg/mod/vs-genesis; https://www.mado-hosting.com/minecraft-mods/vs-genesis].
- **VS Orbit** applies a planet's pull to ships; pre-release version 0.0.5 for 1.20.1; known "gravitational error" [S: https://moddex.gg/mod/vs-orbit].
- **Starlance** links space mods with VS; thrusters, drag block, magnet boots for artificial gravity [S: https://www.curseforge.com/minecraft/mc-mods/starlance].

### 3.5 Distant Horizons (far terrain LOD)

- Adds a level-of-detail system: renders simplified chunks beyond the normal render distance [V: https://gitlab.com/jeseibel/distant-horizons, fetched page describes Distant-Horizons-Team/distant-horizons]. Licence listed as GPLv3 with a LICENSE.LESSER file [V, same].
- Works with Iris only with shaders built for DH support (DH 2.1.0+). Strict version matching [S: https://gitlab.com/jeseibel/distant-horizons/-/issues/606 search listing; compatibility text from search excerpt].
- A flat-world LOD system, not a sphere. Relevant for showing a distant planet surface at low detail.

### 3.6 Cubic Chunks (unlimited height)

- OpenCubicChunks: only ceiling named is the 32-bit integer limit. Loads vertical sections on demand [V: https://github.com/OpenCubicChunks/CubicChunks].
- Forge branch for 1.12.2 and lower; post-1.12.2 work is in a separate CubicChunks2 repo [V, same].
- Warnings: breaks many mods; with JEID it can corrupt a world [S: https://www.curseforge.com/minecraft/mc-mods/opencubicchunks].
- Maintenance status unclear from the README, which points users to Discord [V, same].

### 3.7 Orbit views in Minecraft

- Galacticraft: orbital station with a view of the world below after launch; no curved-planet render described [S: https://pcgamer.com/mod-of-the-week-galacticraft-for-minecraft].
- Ad Astra: five celestial bodies (Moon, Mars, Venus, Mercury, Glacio), each with its own mobs and materials. Ad Astra: Planets+ adds more [S: https://sportskeeda.com/minecraft/best-minecraft-space-mods]. Whether each body is a separate dimension was not checked [UNV].
- Astrocraft: real stars, planets and an orbiting moon in the sky; Astrocraft: Ad Astra Compat puts those skies on Ad Astra planets [S: search listing; URL not captured].
- No Minecraft mod found that shows a curved planet below the player in orbit [UNV].

### 3.8 Minecraft summary

- Verified working precedents: Toroidal World, Immersive Portals (gravity and scale transforms), Cubic Chunks, Distant Horizons.
- Visual-only: curvature shaders.
- Planets as separate bodies: Genesis and Ad Astra (space dimensions and datapacks).
- No verified Minecraft mod puts a cube-sphere or geodesic voxel planet in one world with per-face block orientation.

---

## 4. Precision

### 4.1 Minecraft Java

- World border default: faces at +/-29,999,984 blocks from origin, diameter 59,999,968 [V: https://minecraft.wiki/w/World_border].
- Hard boundary at 30 million blocks on X and Z [V, same].
- Java entity positions use 64-bit doubles [S: https://minecraft.wiki/w/User:Liyz7928/Java_Edition_distance_effects_(Historic), user draft, not authoritative].
- Visible bugs come from float casts. MC-161969 (end-portal particles) is marked fixed in 1.16 pre-release 1 [S: https://bugs.mojang.com/browse/MC-161969]. Reporters say effects are most visible beyond 16,777,216 blocks on both axes [S, same].
- Camera-relative rendering: MC-7363 proposes rounding the camera to a multiple of 16, computing the offset in doubles, then demoting the small difference to float [S: https://bugs-legacy.mojang.com/browse/MC-7363; fetch returned a stub]. Shipping status unconfirmed [UNV].
- Java performs normally at +/-2,000,000,000 in modern versions, per the fandom page [S: https://minecraft.fandom.com/wiki/World_boundary].
- Bedrock uses 32-bit floats for player position. Bedrock thresholds: entity coordinates snap to 0.03125 at 262,144 and 0.0625 at 524,288. At 16,777,216, only even block coordinates render [S: https://bugs.mojang.com/browse/MCPE-101723; https://minecraft.miraheze.org/wiki/Distance_Effects_in_Bedrock_Edition].

### 4.2 Float32 spacing check [INF]

For float32, the spacing (ulp) in [2^k, 2^(k+1)) is 2^(k-23). This reproduces the Bedrock thresholds: at 2^18 (262,144) ulp = 0.03125; at 2^19 (524,288) ulp = 0.0625; at 2^21 (2,097,152) ulp = 0.25; at 2^24 (16,777,216) ulp = 2. Near the 30M border (in [2^24, 2^25)) the ulp is 2, which fits the even border value. So any float-stored coordinate beyond about 16.7M has 2-block steps.

### 4.3 Far Lands

- Far Lands begin at X/Z about +12,550,821 (or -12,550,824), per a Bedrock tracker excerpt [S: https://bugs.mojang.com/browse/MCPE-101723]. Cause given as float rounding in noise coordinates, per Far Lands wiki excerpts from search; those pages were not fetched [S].

### 4.4 Floating origin and cluster approaches

- **Floating origin.** Keep the viewer near zero and shift the world. Originally attributed to Chris Thorne's paper "Using a Floating Origin to Improve Fidelity and Performance of Large, Distributed Virtual Worlds" [S: https://speckle.systems/blog/speckles-take-on-spatial-jitter-2].
- **Cell grids.** big_space (Bevy): entities are positioned relative to a grid cell centre, recomputed on crossing [S: https://docs.rs/big_space/0.2.0].
- **Camera-relative (Minecraft-like).** Rasterizers keep one model-view matrix so float precision sits near the viewer [S: https://pharr.org/matt/blog/2018/03/02/rendering-in-camera-space].
- **Space Engineers.** 64-bit doubles, 32-bit Havok, clusters of 50 to 100 km (min 20 km), with a 2 km border margin [V: https://spaceengineers.wiki.gg/wiki/Planetary_System]. A Real Solar Systems guide reports precision trouble beyond about 50,000 km from origin [S: search excerpt, page not identified].
- **KSP.** KSP2 generates planet vertex positions relative to the floating origin for precision [S: KSP dev insight]. KSP's Krakensbane is a community-described origin reset and velocity absorption [S].

### 4.5 Precision implications for a multi-planet design [INF]

- A float coordinate stored in Minecraft's rendering path has 2-block steps by 2^24. Planet-local coordinates, camera-relative rendering, or separate dimensions avoid this. Planets placed far from origin in one shared coordinate space hit this wall quickly.
- The 30M world border is a hard limit for vanilla dimensions [V].

---

## 5. Orbit-view rendering

### 5.1 Atmospheric scattering

- **Bruneton and Neyret (2008), "Precomputed Atmospheric Scattering".** EGSR 2008; Computer Graphics Forum 27(4), 1079-1086 [S: https://diglib.eg.org/items/0af15b7c-795a-4cca-97d3-4cc9b99126e4]. Renders the sky from any altitude between surface and orbit, with multiple Rayleigh and Mie scattering. Precomputes the light transport so runtime evaluation is constant time [S, same].
- **Official code.** ebruneton/precomputed_atmospheric_scattering on GitHub. BSD licence. Topics include OpenGL, shader, WebGL. Links to the HAL paper record [V: https://github.com/ebruneton/precomputed_atmospheric_scattering]. Project page at ebruneton.github.io [S].
- **Three.js port.** takram three-atmosphere: does not render correctly underground; no volumetric light shafts; effectively limited to Earth's atmosphere [S: https://unpkg.com/@takram/three-atmosphere@0.13.2/README.md].
- **O'Neil (GPU Gems 2, ch. 16).** Sean O'Neil implements the full Nishita et al. 1993 equations in GPU shaders. Ground and space shaders are split by camera position. Optical depth approximated by an exponential (exp(-4h)) and a fitted polynomial "scale function" [V: https://developer.nvidia.com/gpugems/GPUGems2/gpugems2_chapter16.html]. Book code had gaps, and the author's site has gone offline [S: search forum excerpts].
- **Scatterer (KSP mod).** Its README calls it "Atmospheric scattering graphical effects for Kerbal Space Program". GPL-3.0. The README does not mention Bruneton or any specific model [V: https://github.com/LGhassen/Scatterer]. The claim that Scatterer builds on Bruneton is [UNV].
- **Java and LWJGL.** No verified Java or LWJGL Bruneton port found. Candidates:
  - Delve: LWJGL 3, OpenGL 3.3 core, sky pass, PlanetProfile class. Scattering math not confirmed [S: https://github.com/jskrivseth/Delve].
  - jMonkeyEngine forum thread on a planet atmosphere based on O'Neil, LWJGL-based [S: https://hub.jmonkeyengine.org/t/planet-shader-and-atmosphere-scattering/13523].
  - GPUWorks opengl_sky: procedural Mie and Rayleigh sky, C/C++ [S: https://github.com/GPUWorks/opengl_sky].
  - OSDVF RealisticAtmosphere: standalone planetary app with scattering, Visual Studio [S: https://github.com/OSDVF/RealisticAtmosphere].

### 5.2 Terrain LOD for planets from orbit

- **Geometry clipmaps (Losasso and Hoppe, 2004).** Nested regular grids around the viewer, held in GPU vertex buffers and refilled as the camera moves. Tested on a 40 GB US height map compressed about 100x, at 60 fps [S: https://hhoppe.com/proj/geomclipmap/].
- **GPU-based clipmaps.** Asirvatham and Hoppe, GPU Gems 2 ch. 2 [S: https://developer.nvidia.com/gpugems/gpugems2/part-i-geometric-complexity/chapter-2-terrain-rendering-using-gpu-based-geometry].
- **Quadtree cube-sphere LOD.** KSP PQS (see 1.10) and Cuberact Godot (see 1.14) [S].
- **Transvoxel** for crack-free volumetric LOD [V: https://transvoxel.org/].
- **Impostors.** KSA's spherical billboards (secondhand) [S: https://r.datuan.dev/r/kittenspaceagency]. Pixar's "Planet and Clouds" paper is in search results, contents not reviewed [S: https://graphics.pixar.com/library/PlanetAndClouds/paper.pdf].

### 5.3 Clouds and rings

- No source gathered for cloud layers or planetary rings beyond the Pixar paper title [UNV]. Rings in SE2 are mentioned only as an editing feature [S: SE2 devlog search excerpt].

---

## 6. Summary table and open questions

| Technique | Games using it | Pros | Cons | Known Minecraft implementation (yes/no + link) |
|---|---|---|---|---|
| Cube-projected heightmap per planet (2.5D) | SE1 and SE2 surface [V]; Empyrion older heightmap planets [S] | Cheap; LOD-friendly; mature tooling | No overhangs or caves without volumetric additions; SE2 moved past 2.5D [V] | No (none found) |
| Normalized six-face cube, per-face grids | Seed of Andromeda [S]; NMS cube storage claim [S]; Zoxel [V] | Simple; six independent grids | Strong area distortion; seams at face edges | No (Zoxel is not Minecraft) |
| Equal-angle / tangent-adjusted cube | Not confirmed in any game [UNV] | Less area and angle distortion than raw normalize [V, qualitative] | Shader needs inverse warp; trade-offs [V] | No |
| Spherified cube polynomial | Not confirmed [UNV] | Closed form, cheap per vertex [S] | Distortion not quantified in sources | No |
| Ronchi six-patch great-circle cubed sphere | Climate and weather models [S]; games not found | No coordinate singularities; shared metric [S] | Multi-grid coupling; topology still has 8 three-valent corners [INF] | No |
| Geodesic / icosahedral (Goldberg dual) | Gamedev blog plan [S]; no shipped game verified | Near-uniform cells; no cube seams | 12 pentagons [V]; indexing and seams more complex | No |
| Quadtree per-face LOD on cube sphere (PQS) | KSP and KSP2 [S]; Cuberact Godot [S] | Proven LOD; distance-based detail | Heightfield-first; needs floating origin [S] | No |
| Transvoxel LOD seams | C4 Engine [V]; SE and Astroneer claimed [UNV] | Crack-free volumetric LOD; patent-free [V] | Marching cubes only; tables are large [S] | No |
| Torus or cylinder wrap | Not a game technique; Minecraft-first | Seamless terrain across the seam [V] | Not a sphere; fixed at world creation [V] | Yes: Toroidal World, https://modrinth.com/mod/toroidal-world |
| Teleport-at-edge wrap | None | Trivial | Not curved | Yes: Wrap Around World datapack, https://modrinth.com/mod/wrap-around-world [S] |
| Visual curvature shader | None | Cheap; client-only | Visual only; no physics [S] | Yes: World Curvature Shaders, https://www.9minecraft.net/?p=469643 [S]; Vertigo, https://www.curseforge.com/minecraft/shaders/vertigo-shader [S] |
| Planets as dimensions with portals changing gravity and scale | Not applicable | Avoids giant shared coordinates; portals transform scale and gravity [V] | Archived upstream [V]; design of cross-dimension travel | Yes: Immersive Portals, https://github.com/iPortalTeam/ImmersivePortalsMod [V] |
| Space dimension with orbiting planets (datapacks) | Not applicable | Orbits, gravity and atmosphere per body via datapack [S] | Scale hacks (1/16 with Pehkui) [S]; early mods | Yes: Genesis, https://moddex.gg/mod/vs-genesis [S] |
| Planet gravity applied to ships | Not applicable | Ships pulled by planets [S] | Pre-release; gravitational error [S] | Yes (partial): VS Orbit, https://moddex.gg/mod/vs-orbit [S] |
| Floating origin / cluster-relative coordinates | SE clusters [V]; KSP2 [S] | Keeps float precision near the viewer [S] | Rebasing logic; distant positions imprecise [S] | Partial: Minecraft camera-relative rendering [S, MC-7363] |
| Precomputed atmospheric scattering (Bruneton) | Scatterer for KSP [V README; Bruneton link UNV]; takram three-atmosphere [S] | Accurate ground-to-space sky [S] | Precompute step; Earth-only in takram [S] | No verified Java port |
| O'Neil analytic GPU scattering | GPU Gems 2 demo [V] | Single-pass shader; ground and space [V] | Approximations; code gaps [S] | No (Astrocraft is sky only [S]) |
| Impostor / spherical billboards | KSA [S] | Cheap distant planets | Cannot land on them | No |
| Geometry clipmaps | Heightfield terrain [S] | Nested grids; GPU-friendly [S] | Heightfield-only | Partial: Distant Horizons is LOD for a flat world [V], https://gitlab.com/jeseibel/distant-horizons |
| Marching cubes on a sphere (planetary MC) | Academic [S] | Smooth spherical voxel surface [S] | Shipping status unknown | No |

### 6.1 Open questions and unknowns

1. SE1 and SE2 gravity falloff curve, and whether heights are from surface or centre. The Pertam atmosphere figure also conflicts between sources (4.7 km in a search snippet, 1.6 km on the page) [V, discrepancy].
2. Whether Transvoxel ships in SE or Astroneer [UNV].
3. Astroneer planet geometry, Starbase planet model, Planet Explorers sphericity, and whether Seed of Andromeda still uses its cube grids [UNV].
4. The No Man's Sky cube-storage claim. The GDC 2017 McKendrick talk transcript was not reachable [S].
5. Exact area and aspect numbers for equiangular and equidistant grids. The arXiv PDF could not be read as text [UNV]. Someone should extract the text and check Table 1.
6. Everitt's derivation and attribution of the spherified cube. The mathproofs page was unreadable [UNV].
7. Whether MC-7363's camera-relative proposal shipped in current Java Edition. Current renderer code not checked [UNV].
8. A Java or LWJGL Bruneton port. None verified [gap].
9. Cloud layers and planetary rings: no sources gathered [gap].
10. KSP2 official Krakensbane and floating-origin documentation. Official pages were blocked [UNV].
11. Current maintenance status of Cubic Chunks for recent Minecraft versions, and the NeoForge port status for Immersive Portals [UNV].
12. Big Globe Mod: developer, version and source unconfirmed [UNV].
13. Whether any Minecraft mod renders a curved planet visible from orbit [gap].

### 6.2 Sources (verified pages marked V)

- https://2.spaceengineersgame.com/mareks-dev-diary-august-21-2025/ [V]
- https://spaceengineers.wiki.gg/wiki/Planetary_System [V]
- https://spaceengineers.wiki.gg/wiki/Pertam [V]
- https://spaceengineers.wiki.gg/wiki/Titan [S]
- https://spaceengineers.wiki.gg/wiki/Gravity [S]
- https://spaceengineers.wiki.gg/wiki/Planetary_Entry/Exit [S]
- https://deus0.itch.io/zoxel/devlog/305343/planets-portals [V]
- https://redblobgames.com/x/1938-square-tiling-of-sphere [V]
- https://en.wikipedia.org/wiki/Goldberg_polyhedron [V]
- https://transvoxel.org/ [V]
- https://github.com/iPortalTeam/ImmersivePortalsMod [V]
- https://minecraft.wiki/w/World_border [V]
- https://modrinth.com/mod/toroidal-world [V]
- https://github.com/OpenCubicChunks/CubicChunks [V]
- https://github.com/ebruneton/precomputed_atmospheric_scattering [V]
- https://github.com/LGhassen/Scatterer [V]
- https://developer.nvidia.com/gpugems/GPUGems2/gpugems2_chapter16.html [V]
- https://gitlab.com/jeseibel/distant-horizons [V]
- https://arxiv.org/abs/2505.05624 [V, abstract only]
- https://www.unrealengine.com/en-US/spotlights/how-system-era-softworks-leveraged-ue4-to-create-astroneer-s-wonderful-universe [S]
- https://gdcvault.com/play/1024265/Continuous-World-Generation-in-No [S]
- https://redlib.hbubli.cc/r/VoxelGameDev/comments/3ese9u/spherical_world_generation [S]
- https://www.kerbalspaceprogram.com:443/news/dev-diaries-developer-insights-12-planet-tech [S]
- https://pcgamer.com/how-to-build-a-black-hole [S]
- https://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc/grib2_temp3-60.shtml [S]
- https://sostenibilita.enea.it/en/node/5736 [S]
- https://discussions.unity.com/t/problems-at-mapping-cube-to-a-sphere/66729 [S]
- https://diglib.eg.org/items/0af15b7c-795a-4cca-97d3-4cc9b99126e4 [S]
- https://hhoppe.com/proj/geomclipmap/ [S]
- https://bugs.mojang.com/browse/MCPE-101723 [S]
- https://bugs.mojang.com/browse/MC-161969 [S]
- https://speckle.systems/blog/speckles-take-on-spatial-jitter-2 [S]
- https://docs.rs/big_space/0.2.0 [S]
- https://pharr.org/matt/blog/2018/03/02/rendering-in-camera-space [S]
- https://www.modpackindex.com/mod/92088/neobiosphere [S]
- https://www.curseforge.com/minecraft/mc-mods/spherical-world [S]
- https://modrinth.com/mod/wrap-around-world [S]
- https://moddex.gg/mod/vs-genesis [S]
- https://moddex.gg/mod/vs-orbit [S]
- https://www.curseforge.com/minecraft/mc-mods/starlance [S]
- https://www.9minecraft.net/?p=469643 [S]
- https://www.curseforge.com/minecraft/shaders/vertigo-shader [S]
- https://www.mado-hosting.com/minecraft-mods/gravity-api-fork [S]
- https://www.modpackindex.com/mod/147715/robins-outer-wilds [S]
- https://www.curseforge.com/minecraft/mc-mods/tfc-eratosthenes [S]
- https://feedback.minecraft.net/hc/en-us/community/posts/360011428891/comments/360001293472 [S]
- https://unpkg.com/@takram/three-atmosphere@0.13.2/README.md [S]
- https://github.com/jskrivseth/Delve [S]
- https://hub.jmonkeyengine.org/t/planet-shader-and-atmosphere-scattering/13523 [S]
- https://github.com/OSDVF/RealisticAtmosphere [S]
- https://github.com/GPUWorks/opengl_sky [S]
- https://developer.nvidia.com/gpugems/gpugems2/part-i-geometric-complexity/chapter-2-terrain-rendering-using-gpu-based-geometry [S]
- https://gamedev.net/blogs/entry/2265006-planet-generation-plans [S]
- https://research.polyu.edu.hk/en/publications/planetary-marching-cubes-a-marching-cubes-algorithm-for-spherical/ [S]
- https://store.steampowered.com/app/237870/Planet_Explorers [S]
- https://www.starbasegame.com/ [S]
- https://www.moddb.com/news/road-to-alpha-8-development-update [S]
- https://r.datuan.dev/r/kittenspaceagency [S]
- https://www.moddb.com/games/imagine-nations/news/the-science-of-imagine-nations-planets [S]
- https://minecraft.fandom.com/wiki/World_boundary [S]
- https://minecraft.miraheze.org/wiki/Distance_Effects_in_Bedrock_Edition [S]
- https://graphics.pixar.com/library/PlanetAndClouds/paper.pdf [S]
- https://bowerbyte.itch.io/blocky-planet [S]
- https://www.godotengine.org/asset-library/asset/edit/21636 [S]
- https://github.com/PaperPrototype/VoxelSystem [S]
- https://sportskeeda.com/minecraft/best-minecraft-space-mods [S]
- https://pcgamer.com/mod-of-the-week-galacticraft-for-minecraft [S]
- https://www.modpackindex.com/mod/175287/immersive-portal-ce [S]
- https://www.modpackindex.com/mod/79118/void-dimension-stack [S]
- https://www.modpackindex.com/mod/175748/cinematic-horizon-shaders [S]
- https://common-lemur.origin.boxcast.com/big-globe-mod [UNV]
- https://minecraft.wiki/w/User:Liyz7928/Java_Edition_distance_effects_(Historic) [S]
- https://bugs-legacy.mojang.com/browse/MC-7363 [S]
- https://app.cinevva.com/blog/2026-02-25-open-world-browser-part-09-transvoxel-first-cut [UNV for the game claims]
- http://mathproofs.blogspot.com/2005/07/mapping-cube-to-sphere.html [V, title only]
- https://forum.kerbalspaceprogram.com/topic/91108-how-does-ksp-work [S]
- https://forum.kerbalspaceprogram.com/topic/184506-at-what-altitudes-does-pqs-terrain-surface-stop-rendering [S]
- https://bugs.kerbalspaceprogram.com/issues/13213 [S]
- https://gitlab.com/jeseibel/distant-horizons/-/issues/606 [S]
- https://ingamenews.com/2025/11/space-engineers-2-massive-star-system.html [S]
- https://2.spaceengineersgame.com/mareks-dev-diary-november-13-2025/ [S]
- https://2.spaceengineersgame.com/wp-content/uploads/2025/07/SE2_Generators.pdf [S]
- https://astroneer.wiki.gg/wiki/Astroneer [S]
- https://empyriongame.com/press-kit [S]
- https://steamcommunity.com/app/383120/discussions/0/405691147593200663 [S]
- https://gdconf.com/news/see-4d-level-design-outer-wilds-deconstructed-gdc-2020 [S]
- https://gdcvault.com/play/1024514/Building-Worlds-Using [S]
- https://redlib.hbubli.cc/r/VoxelGameDev/comments/1ae7mfg/seams_octree [S]
- https://discussions.unity.com/t/smooth-voxel-planets/168810 [S]
- https://gamedev.net/forums/topic/717873-make-normals-from-terrain-in-spherical-form [S]
- https://www.mado-hosting.com/minecraft-mods/vs-genesis [S]
- https://www.mado-hosting.com/minecraft-mods/gravitychanger [S]
- https://wccftech.com/starbase-hugely-ambitious-vertex-voxel-space-sandbox-mmo/amp/ [S]
- https://www.curseforge.com/minecraft/mc-mods/opencubicchunks [S]
- https://www.curseforge.com/minecraft/mc-mods/opencubicchunks/files/2876951 [S]
- https://spaceengineers.wiki.gg/wiki/How_to_create_your_own_planet [S]
- https://spaceengineers.wiki.gg/wiki/Planets [S]
- https://support.keenswh.com/spaceengineers/pc/comments/40272/vote/toggle [UNV]
- https://minecraft.wiki/w/World_border [V]
