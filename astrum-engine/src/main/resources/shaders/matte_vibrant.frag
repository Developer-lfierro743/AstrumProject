#version 450

layout(location = 0) in vec3 fragColor;

layout(location = 0) out vec4 outColor;

void main() {
    // Matte-Vibrant Art Style: High-contrast, vibrant visuals.
    // We can apply slight gamma correction and saturation boost later.
    outColor = vec4(fragColor, 1.0);
}
