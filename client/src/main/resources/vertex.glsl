#version 450 core

in vec3 position;
in vec3 color;
out vec3 fragColor;
uniform bool drawScreenSpace;
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main() {
    fragColor = color;
    if (drawScreenSpace) {
        gl_Position = vec4(position, 1.0);
    } else {
        gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
    }
}