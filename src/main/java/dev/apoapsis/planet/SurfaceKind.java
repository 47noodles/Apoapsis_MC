package dev.apoapsis.planet;

/**
 * What a body's ground column is made of, as far as its flat colour goes: the coarse class of the block on top, as the
 * far terrain and the planet maps draw it. {@link SurfaceColumns#kind} gives it next to the colour, so the accuracy
 * GameTests can compare the class the far terrain picks with the block the world generator really put there.
 */
public enum SurfaceKind {
    /** Grass, dirt, podzol, mud and the like: the green and brown ground. */
    GRASS,
    /** Bare rock: stone, deepslate, tuff, calcite, andesite. */
    STONE,
    /** Sand and sandstone. */
    SAND,
    /** Red sand, red sandstone and terracotta: the badlands. */
    RED_ROCK,
    /** Gravel. */
    GRAVEL,
    /** Snow and ice, on land or over water. */
    SNOW,
    /** Water over the ground, drawn by depth. */
    WATER,
    /** Pale's dark basalt. */
    BASALT,
    /** Pale's light regolith. */
    REGOLITH
}
