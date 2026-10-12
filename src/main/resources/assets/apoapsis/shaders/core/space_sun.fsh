#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

// The sun: an antialiased disc over a warm Lorentzian glow (1 / angle^2 out to 28 degrees), and a faint broad veil.
// Looking toward the sun brightens the glow and veil. ModelViewMat[0] = (sun direction in view space, disc radius in
// radians), ModelViewMat[1] = (sun strength, glare strength, unused, unused), ColorModulator.x = visibility.

layout(location = 0) in vec3 ray;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 sun = ModelViewMat[0].xyz;
    float radius = ModelViewMat[0].w;
    vec3 dir = normalize(ray);
    float theta = atan(length(cross(dir, sun)), dot(dir, sun));
    if (theta > 0.5) {
        discard;
    }
    float x = theta / radius;
    float glow = 1.2 / (1.0 + 0.25 * x * x) * (1.0 - smoothstep(0.2, 0.5, theta));
    float veil = (1.0 - smoothstep(0.0, 0.5, theta));
    veil *= veil;

    float facing = max(-sun.z, 0.0);
    float facing2 = facing * facing;
    float closeness = 1.0 + 0.8 * facing2 * facing2;

    float px = max(fwidth(theta), 1.0e-5);
    float disc = 1.0 - smoothstep(radius - px, radius + px, theta);
    vec3 color = ModelViewMat[1].x * (disc * vec3(1.0, 0.97, 0.90) + 0.55 * closeness * glow * vec3(1.0, 0.86, 0.66))
            + ModelViewMat[1].y * closeness * veil * vec3(1.0, 0.82, 0.62);
    color *= ColorModulator.x;
    float noise = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    color += (noise - 0.5) / 255.0 * smoothstep(0.0, 0.004, max(color.r, max(color.g, color.b)));
    fragColor = vec4(max(color, 0.0), 1.0);
}
