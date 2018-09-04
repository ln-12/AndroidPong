package com.example.pong.geometry

import android.content.Context

/**
 * Created by vaterj on 28.09.17.
 */
class ShadowTrapeze (context : Context, x : Float, y : Float, vertexX1 : Float, vertexY1 : Float, vertexX2 : Float, vertexY2 : Float, vertexX3 : Float, vertexY3 : Float, vertexX4 : Float, vertexY4 : Float, textureIndex : Int) : Shape(context, x, y, 1.0f, 2.0f, textureIndex) {
    init {
        //this.fragmentShader = R.raw.fragment_texture
        //this.vertexShader = R.raw.vertex_texture
        this.color = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

        this.vertexData = floatArrayOf(
                vertexX1, vertexY1, 0.0f,
                vertexX2, vertexY2, 0.0f,
                vertexX3, vertexY3, 0.0f,
                vertexX4, vertexY4, 0.0f)

        //this.useTexture = true;
        //this.initOpenGLStuff()
    }
}