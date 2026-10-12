#version 330
#extension GL_ARB_separate_shader_objects : require

// A star: a Gaussian point (offset is in units of its sigma) with, for the brightest, a soft wide halo.

layout(location = 0) in vec3 starColor;
layout(location = 1) in vec2 ampHalo;
layout(location = 2) in vec2 offset;

layout(location = 0) out vec4 fragColor;

void main() {
    float r2 = dot(offset, offset);
    float v = ampHalo.x * exp(-0.5 * r2) + ampHalo.y * exp(-0.5 * r2 / 9.0);
    fragColor = vec4(starColor * v, 1.0);
}
