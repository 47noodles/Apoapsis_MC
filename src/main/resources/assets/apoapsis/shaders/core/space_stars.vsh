#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// Space stars: one quad per star on the unit sphere, sized in pixels (so a star is crisp at any resolution and does not
// change size with the field of view). Per vertex: Position = the star's inertial unit vector, UV = the corner (-1..1),
// Color = star colour, alpha = sqrt(flux / 2.1) where flux 1 is a magnitude 1 star.
// ModelViewMat takes the unit vector to the sky sphere in view space; ModelOffset is the sun's inertial direction;
// ColorModulator = (visibility, sun disc radius, unused, unused).

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec3 starColor;
layout(location = 1) out vec2 ampHalo;
layout(location = 2) out vec2 offset;

void main() {
    float flux = 2.1 * Color.a * Color.a;
    float visibility = ColorModulator.x;

    // Near the sun the stars dim, by the same glow the dome draws, plus a wider gentle falloff.
    float theta = acos(clamp(dot(Position, ModelOffset), -1.0, 1.0));
    float x = theta / ColorModulator.y;
    float glow = 1.2 / (1.0 + 0.25 * x * x);
    float dim = 1.0 / (1.0 + 25.0 * glow);
    dim *= 1.0 - 0.5 * (1.0 - smoothstep(0.1, 0.9, theta));

    float fade = dim * visibility * visibility;
    float amp = min(flux * 1.35, 1.0) * fade;
    // Faint stars are sharp, subpixel points; brighter ones grow, and the brightest get a soft halo.
    float sigma = 0.62 + 0.5 * clamp(log2(1.0 + flux), 0.0, 1.6);
    float halo = clamp((flux - 0.9) * 0.12, 0.0, 0.25) * fade;
    float quadRadius = sigma * (halo > 0.001 ? 9.0 : 3.2);

    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clip.xy += UV0 * quadRadius * 2.0 / ScreenSize * clip.w;
    gl_Position = clip;

    starColor = Color.rgb;
    ampHalo = vec2(amp, halo);
    offset = UV0 * (quadRadius / sigma);
}
