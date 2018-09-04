package com.example.pong.geometry

import android.content.Context
import android.opengl.GLES30

/**
 * Created by Lorenzo on 12.08.2017.
 */

class Particle (context: Context, x : Float, y : Float, radius : Float) : Shape(context, x, y, radius, radius, -1) {
    companion object {
        private var vertexBufferObject = intArrayOf(-1)

        fun resetVBOHandle(){
            vertexBufferObject[0] = -1
        }
    }

    // life time in seconds
    var life : Float = 1.0f

    init {
        val n = 10

        val alpha : Float  = 360.0f / n

        // allocate memory
        this.vertexData = FloatArray(12 * this.COORDS_PER_VERTEX)

        // fill in values
        this.vertexData[0] = 0.0f
        this.vertexData[1] = 0.0f
        this.vertexData[2] = 0.0f

        for (i in 1..n+1) {
            this.vertexData[i * 3 + 0] = (0.5f * Math.cos(alpha.toDouble() * (i-1) * 3.14 / 180.0f)).toFloat() * this.getWidth()
            this.vertexData[i * 3 + 1] = (0.5f * Math.sin(alpha.toDouble() * (i-1) * 3.14 / 180.0f)).toFloat() * this.getHeight()
            this.vertexData[i * 3 + 2] = 0.0f
        }

        this.drawMethod = GLES30.GL_TRIANGLE_FAN

        if(Particle.vertexBufferObject[0] <= 0){
            this.initVertexData()

            Particle.vertexBufferObject = this.vertexBufferObject
        }else{
            this.vertexBufferObject = Particle.vertexBufferObject
        }
    }
}