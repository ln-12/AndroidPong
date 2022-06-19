package com.example.pong.geometry

import android.content.Context
import com.example.pong.R

class Rectangle (context: Context, x : Float, y : Float, width : Float, height : Float, striped : Boolean) : Shape(context, x, y, width, height, -1) {
    init {
        this.color = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)


        this.vertexData = floatArrayOf(
                -width / 2, height / 2, 0.0f, // top left
                -width / 2, -height / 2, 0.0f, // bottom left
                width / 2,  -height / 2, 0.0f, // bottom right
                width / 2,  height / 2, 0.0f) // top right)

        if(striped){
            this.fragmentShader = R.raw.fragment_stripe
            this.vertexShader = R.raw.vertex_stripe
        }

        this.initVertexData()
    }
}