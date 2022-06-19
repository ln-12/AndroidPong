package com.example.pong.geometry

import android.content.Context
import android.opengl.GLES30
import com.example.pong.R

class LightPolygon (context : Context, x : Float, y : Float, lightX : Float, lightY : Float, vertexX1 : Float, vertexY1 : Float, vertexX2 : Float, vertexY2 : Float, vertexX3 : Float, vertexY3 : Float,  vertexX4 : Float, vertexY4 : Float, lowerShadowTrapeze : ShadowTrapeze, upperShadowTrapeze: ShadowTrapeze, textureIndex : Int) : Shape(context, x, y, 1.0f, 2.0f, textureIndex) {
    init {
        this.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        this.drawMethod = GLES30.GL_TRIANGLE_STRIP

        this.vertexData = floatArrayOf(
                lightX, lightY, 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[6], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[7], 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[9], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[10], 0.0f,
                lightX, lightY, 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[9], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[10], 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[0], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[1], 0.0f,
                lightX, lightY, 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[0], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[1], 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[3], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[4], 0.0f,
                lightX, lightY, 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[3], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[4], 0.0f,
                vertexX1, vertexY1, 0.0f,
                lightX, lightY, 0.0f,
                vertexX1, vertexY1, 0.0f,
                vertexX2, vertexY2, 0.0f,
                lightX, lightY, 0.0f,
                vertexX2, vertexY2, 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[6], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[7], 0.0f,
                lightX, lightY, 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[6], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[7], 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[9], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[10], 0.0f,
                lightX, lightY, 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[9], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[10], 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[0], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[1], 0.0f,
                lightX, lightY, 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[0], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[1], 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[3], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[4], 0.0f,
                lightX, lightY, 0.0f,
                upperShadowTrapeze.position.x + upperShadowTrapeze.vertexData[3], upperShadowTrapeze.position.y + upperShadowTrapeze.vertexData[4], 0.0f,
                vertexX3, vertexY3, 0.0f,
                lightX, lightY, 0.0f,
                vertexX3, vertexY3, 0.0f,
                vertexX4, vertexY4, 0.0f,
                lightX, lightY, 0.0f,
                vertexX4, vertexY4, 0.0f,
                lowerShadowTrapeze.position.x + lowerShadowTrapeze.vertexData[6], lowerShadowTrapeze.position.y + lowerShadowTrapeze.vertexData[7], 0.0f
                )

        this.fragmentShader = R.raw.fragment_light
        this.vertexShader = R.raw.vertex_light

        this.initVertexData()
    }
}