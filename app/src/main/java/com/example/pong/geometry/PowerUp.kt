package com.example.pong.geometry

import android.content.Context
import com.example.pong.helper.PowerUpKind
import com.example.pong.R

class PowerUp(context: Context, x : Float, y : Float, radius : Float, val powerUpKind : PowerUpKind, textureIndex : Int) : Shape(context, x, y, radius, radius, textureIndex) {
    // life time in seconds
    var life : Float = 7.5f

    val r = radius

    init {
        this.fragmentShader = R.raw.fragment_texture
        this.vertexShader = R.raw.vertex_texture

        this.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        val n = 20

        val alpha : Float  = 360.0f / n

        // allocate memory
        this.vertexData = FloatArray((n+2) * this.COORDS_PER_VERTEX + (n+2) * 2)

        // fill in values
        this.vertexData[0] = 0.0f
        this.vertexData[1] = 0.0f
        this.vertexData[2] = 0.0f
        this.vertexData[3] = 0.5f
        this.vertexData[4] = 0.5f

        for (i in 1..n+1) {
            this.vertexData[i * 5 + 0] = (0.5f * Math.cos(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat() * this.getWidth()
            this.vertexData[i * 5 + 1] = (0.5f * Math.sin(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat() * this.getHeight()
            this.vertexData[i * 5 + 2] = 0.0f
            this.vertexData[i * 5 + 3] = ((Math.cos(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat()   + 1) / 2.0f
            this.vertexData[i * 5 + 4] = ((Math.sin(alpha.toDouble() * (i-1) * Math.PI / 180.0f)).toFloat()   + 1) / 2.0f
        }

        this.useTexture = true

        this.initVertexData()
    }
}