#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float sphericalDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    // The level's own atmospheric fog by true distance (the render-distance part is left to the voxels), as much of it
    // as TextureMat[1].x says: the climb takes it away as the sky goes black.
    float fogValue = linear_fog_value(sphericalDistance, FogEnvironmentalStart, FogEnvironmentalEnd) * TextureMat[1].x;
    color.rgb = mix(color.rgb, FogColor.rgb, fogValue * FogColor.a);
    color.a *= ColorModulator.a;
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
