#version 300 es

precision mediump float;

uniform vec4 vColor;
uniform bool invertColor;

in vec4 position;
out vec4 fragmentColor;


void main() {
    vec4 newColor = vColor;
    int compare = 0;

    // we need to switch at the y axis to avoid a mirrored line
    if(position.x < 0.0){
        compare = 1;
    }

    // switch alpha every 0.1 units
    if(int(abs(position.x) * 10.0) % 2 == compare){
        newColor.a = 0.0;
    }else{
        newColor.a = 1.0;
    }

    if(invertColor){
        fragmentColor = vec4(1.0 - newColor.r, 1.0 - newColor.g, 1.0 - newColor.b, newColor.a);
    }else{
        fragmentColor = newColor;
    }
}