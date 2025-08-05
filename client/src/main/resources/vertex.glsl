#version 450 core

out gl_PerVertex {
    vec4 gl_Position;
};
in vec3 position;
in vec3 color;
in vec2 texturePos;
out vec3 fragColor;
out vec2 fragTexturePos;
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main() {
    fragColor = color;
    fragTexturePos = texturePos;
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
}