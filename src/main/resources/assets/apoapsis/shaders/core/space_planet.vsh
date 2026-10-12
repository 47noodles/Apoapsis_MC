#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// A planet cap seen from space (see PlanetCap): the sky-sphere quads of a textured body. Color.r = the day-side
// lighting (ambient + (1 - ambient) * diffuse, as the far terrain uses), Color.g = how blue the night side is (an
// atmosphere scatters a little light onto it; an airless body has none), Color.a = the fade-in.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
