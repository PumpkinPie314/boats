#version 450 core

in vec3 fragColor;
in vec2 fragTexturePos;
out vec4 finalColor;
uniform sampler2D textureSampler;

void main() {
    finalColor =  texture(textureSampler, fragTexturePos) * vec4(fragColor, 1.0);
}
