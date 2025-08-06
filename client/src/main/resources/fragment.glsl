#version 450 core

in vec2 fragTexturePos;
out vec4 finalColor;
uniform sampler2D textureSampler;

void main() {
    finalColor =  texture(textureSampler, fragTexturePos);
}
