#version 300 es

layout(location = 0) in vec4 vPosition;

out vec4 position;
uniform mat4 uMVPMatrix;

void main() {
  gl_Position = uMVPMatrix * vPosition;
  position = gl_Position;
}