#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

// Additive effect particles (engine plume, re-entry glow): the texture times the vertex colour, no fog and no alpha
// cut-off, so the soft edges of the sprites fade out smoothly. The blend is set by the pipeline: the output has alpha 1, so it adds the colour as it is.

uniform sampler2D Sampler0;

layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 0.002) {
        discard;
    }
    // Brightness is the sprite's shape times the tint times the particle's alpha, added to the picture.
    fragColor = vec4(texture(Sampler0, texCoord0).rgb * vertexColor.rgb * vertexColor.a * ColorModulator.rgb, 1.0);
}
