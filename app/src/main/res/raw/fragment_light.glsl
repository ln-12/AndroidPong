#version 300 es

precision mediump float;

uniform vec4 vColor;
uniform bool invertColor;
uniform vec4 lightPos[10];
uniform int ballCount;

in vec4 fPos;

out vec4 fragmentColor;

void main() {
    float minDistance = (lightPos[0][0]-fPos[0]) * (lightPos[0][0]-fPos[0]) + (lightPos[0][1]-fPos[1]) * (lightPos[0][1]-fPos[1]);
    for(int i = 1; i < ballCount; i++){
        float distance = (lightPos[i][0]-fPos[0]) * (lightPos[i][0]-fPos[0]) + (lightPos[i][1]-fPos[1]) * (lightPos[i][1]-fPos[1]);
        if(distance < minDistance){
            minDistance = distance;
        }
    }
    minDistance = 0.4 - minDistance;
    if(invertColor){
        vec4 c = vec4(minDistance * vColor.r, minDistance * vColor.g, minDistance * vColor.b, vColor.a);
        fragmentColor = vec4(1.0 - c[0], 1.0 - c[1], 1.0 - c[2], c[3]);
    }else{
        fragmentColor = vec4(minDistance * vColor.r, minDistance * vColor.g, minDistance * vColor.b, vColor.a);
    }
}