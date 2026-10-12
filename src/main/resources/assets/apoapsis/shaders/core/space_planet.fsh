#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

// A planet cap: the surface map, lit as the far terrain is lit (so the terrain and the cap agree across their seam),
// and on a body with air the unlit side is a deep blue with a trace of the surface, not the map darkened to mud.

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

const float AMBIENT = 0.10;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    vec3 albedo = texture(Sampler0, texCoord0).rgb;
    float l = vertexColor.r;
    float day = clamp((l - AMBIENT) / (1.0 - AMBIENT), 0.0, 1.0);
    vec3 lit = albedo * l;
    float luma = dot(albedo, vec3(0.30, 0.59, 0.11));
    vec3 night = vec3(0.012, 0.026, 0.066) * (0.6 + 0.8 * luma) + albedo * 0.025;
    float n = (1.0 - smoothstep(0.0, 0.30, day)) * vertexColor.g;
    vec3 color = mix(lit, night, n);
    fragColor = vec4(color, vertexColor.a) * ColorModulator;
}
