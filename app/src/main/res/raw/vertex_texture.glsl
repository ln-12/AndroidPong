#version 300 es

layout(location = 0) in vec4 vPosition;
layout(location = 1) in vec2 vTexCoords;

uniform mat4 uMVPMatrix;
out vec2 fTexCoords;

void main() {
  gl_Position = uMVPMatrix * vPosition;
  fTexCoords = vTexCoords;
}