#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// The sun's quad: a square on the tangent plane at the sun's direction, wide enough to hold its glow (28 degrees).
// Position.xy = corner (-1..1). ModelViewMat[0].xyz = the sun's unit direction in view space, ColorModulator.z =
// the distance the quad is placed at (any distance inside the view volume; nothing is depth tested).

layout(location = 0) in vec3 Position;

layout(location = 0) out vec3 ray;

void main() {
    vec3 sun = ModelViewMat[0].xyz;
    vec3 side = normalize(cross(sun, abs(sun.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 up = cross(sun, side);
    // tan(28 degrees) is 0.53; 0.6 holds the circle's edge with a margin.
    vec3 p = sun + (side * Position.x + up * Position.y) * 0.6;
    gl_Position = ProjMat * vec4(p * ColorModulator.z, 1.0);
    ray = p;
}
