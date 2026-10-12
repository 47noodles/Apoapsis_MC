#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// The Milky Way band: a strip of the sky sphere along the galactic equator (about 30 degrees either side), so only
// the band's pixels cost anything. Per vertex: Position = galactic unit vector, UV0 = texture coordinates, Color.a =
// fade to zero at the strip's edges. ModelViewMat takes a galactic unit vector to the sky sphere in view space;
// TextureMat[0].xyz is the sun's unit direction in view space; ColorModulator.x = strength * visibility,
// ColorModulator.y = the sun's disc radius. Near the sun the band is washed out, as the sun's glow outshines it.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec2 uv;
layout(location = 1) out float strength;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec3 view = normalize(mat3(ModelViewMat) * Position);
    float theta = acos(clamp(dot(view, TextureMat[0].xyz), -1.0, 1.0));
    float x = theta / ColorModulator.y;
    float glow = 1.2 / (1.0 + 0.25 * x * x) * (1.0 - smoothstep(0.2, 0.5, theta));
    uv = UV0;
    strength = Color.a * ColorModulator.x / (1.0 + 6.0 * glow);
}
