#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) in vec2 uv;
layout(location = 1) in float strength;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 color = textureLod(Sampler0, uv, 0.0).rgb * strength;
    if (max(color.r, max(color.g, color.b)) < 0.0012) {
        discard; // below one level of 8-bit output: no blend, no write
    }
    // A little noise where there is light, so the dark gradient does not band in 8 bits.
    float noise = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    color += (noise - 0.5) / 255.0 * smoothstep(0.0, 0.004, max(color.r, max(color.g, color.b)));
    fragColor = vec4(max(color, 0.0), 1.0);
}
