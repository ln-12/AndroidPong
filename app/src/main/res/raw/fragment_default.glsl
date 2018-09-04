#version 300 es

precision mediump float;

uniform vec4 vColor;
uniform bool invertColor;

out vec4 fragmentColor;

void main() {
    if(invertColor){
        fragmentColor = vec4(1.0 - vColor.r, 1.0 - vColor.g, 1.0 - vColor.b, vColor.a);
    }else{
        fragmentColor = vColor;
    }
}