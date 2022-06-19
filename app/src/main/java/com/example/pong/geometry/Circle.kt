package com.example.pong.geometry

import android.content.Context

class Circle(context: Context, x : Float, y : Float, radius : Float) : Shape(context, x, y, radius, radius, -1) {
    init {
        val n = 20

        val alpha : Float  = 360.0f / n

        this.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        // allocate memory
        this.vertexData = FloatArray((n+2) * this.COORDS_PER_VERTEX)

        // fill in values
        this.vertexData[0] = 0.0f
        this.vertexData[1] = 0.0f
        this.vertexData[2] = 0.0f

        for (i in 1..n+1) {
            this.vertexData[i * 3 + 0] = (0.5f * Math.cos(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat() * this.getWidth()
            this.vertexData[i * 3 + 1] = (0.5f * Math.sin(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat() * this.getHeight()
            this.vertexData[i * 3 + 2] = 0.0f
        }

        this.initVertexData()
    }
}