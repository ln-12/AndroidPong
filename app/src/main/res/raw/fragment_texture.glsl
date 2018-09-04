#version 300 es

precision mediump float;

uniform vec4 vColor;
uniform bool invertColor;
uniform sampler2D tex;
in vec2 fTexCoords;

out vec4 fragmentColor;


void main() {
    vec4 mixedColor = texture(tex, fTexCoords) * vColor;

    if(invertColor){
        fragmentColor = vec4(1.0 - mixedColor.r, 1.0 - mixedColor.g, 1.0 - mixedColor.b, mixedColor.a);
    }else{
        fragmentColor = mixedColor;
    }
}