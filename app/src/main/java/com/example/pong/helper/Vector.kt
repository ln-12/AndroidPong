package com.example.pong.helper

class Vector(var x : Float, var y : Float, var z : Float) {
    operator fun plus(vec2 : Vector) : Vector =
            Vector(this.x + vec2.x, this.y + vec2.y, this.y + vec2.y)

    operator fun plus(value : Float) : Vector =
            Vector(this.x + value, this.y + value, this.y + value)

    operator fun times(value : Float) : Vector =
            Vector(this.x * value, this.y * value, this.y * value)

    companion object {
        fun dot(v1: FloatArray, v2: FloatArray): Float = v1.indices
                .map { v1[it] * v2[it] }
                .sum()

        fun magnitude(vector: FloatArray): Float {
            val res = vector.indices.sumByDouble { (vector[it] * vector[it]).toDouble() }
            return Math.sqrt(res).toFloat()
        }
    }

}