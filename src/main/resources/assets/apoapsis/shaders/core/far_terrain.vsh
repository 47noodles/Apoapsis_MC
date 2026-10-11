#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// Far terrain. Positions are stored relative to a tile origin; ModelOffset moves the origin to the camera, so
// Position + ModelOffset is the camera-relative position in float precision. The distance from the camera is then
// remapped along the same ray (the picture does not change, only the depth): the whole mesh lands between the farthest
// loaded voxel's distance and the far plane, so real chunks always win the depth test and terrain far beyond the far
// plane still draws. TextureMat carries the parameters:
//   [0] = (curvature 1/2R or 0, near depth distance, far depth distance, scale length)
//   [1] = (fog share 0..1, unused)
//   [2] = (tile origin on the sphere, 1 when lit by the sun, else 0)
//   [3] = (sun direction xyz, ambient light)
// ColorModulator.a is the fade, and on the surface side ColorModulator.rgb the level's light on a block top.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float sphericalDistance;

void main() {
    vec4 depthParams = TextureMat[0];
    vec4 sphere = TextureMat[2];
    vec4 sun = TextureMat[3];

    vec3 rel = Position + ModelOffset;
    vec3 rgb = Color.rgb;
    if (sphere.w > 0.5) {
        // Orbit side: the sphere's own normal at the vertex against the sun, with a soft terminator, as the planet cap.
        vec3 n = normalize(sphere.xyz + Position);
        float diffuse = clamp((dot(n, sun.xyz) + 0.15) / 1.15, 0.0, 1.0);
        rgb *= sun.w + (1.0 - sun.w) * diffuse;
    } else {
        // Surface side: the ground falls away with the planet's curvature, d^2 / 2R.
        rel.y -= depthParams.x * (rel.x * rel.x + rel.z * rel.z);
        // The level's light on a block top under open sky (day, night or Pale's fixed light), set by the renderer.
        rgb *= ColorModulator.rgb;
    }

    float d = max(length(rel), 0.001);
    float placed = depthParams.y + (depthParams.z - depthParams.y) * (1.0 - exp(-d / depthParams.w));
    gl_Position = ProjMat * ModelViewMat * vec4(rel * (placed / d), 1.0);

    vertexColor = vec4(rgb, Color.a);
    sphericalDistance = d;
}
