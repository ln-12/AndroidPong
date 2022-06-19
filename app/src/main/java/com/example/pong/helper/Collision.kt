package com.example.pong.helper

import com.example.pong.geometry.Circle
import com.example.pong.geometry.PowerUp
import com.example.pong.geometry.Rectangle
import com.example.pong.geometry.Shape
import java.util.*

class Collision {
    data class Info(val didCollide : Boolean, var direction: Shape.Direction, val length : FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Info

            if (didCollide != other.didCollide) return false
            if (direction != other.direction) return false
            if (!Arrays.equals(length, other.length)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = didCollide.hashCode()
            result = 31 * result + direction.hashCode()
            result = 31 * result + Arrays.hashCode(length)
            return result
        }
    }

    companion object shape1 {
        fun eval(collision : Info, shapeToMove: Shape) = // react to collisions
                when {
                    collision.direction == Shape.Direction.DOWN -> {
                        // if we hit it from with the down side, set velocity to a positive number
                        shapeToMove.velocity.y = Math.abs(shapeToMove.velocity.y)
                        shapeToMove.moveBy(0.0f, 2 * (shapeToMove.getHeight() / 2 - Math.abs(collision.length[1])), 0.0f)
                    }
                    collision.direction == Shape.Direction.UP -> {
                        // if we hit it from with the up side, set velocity to a negative number
                        shapeToMove.velocity.y = -1 * Math.abs(shapeToMove.velocity.y)
                        shapeToMove.moveBy(0.0f, -2 * ((shapeToMove.getHeight() / 2) - Math.abs(collision.length[1])), 0.0f)

                    }
                    collision.direction == Shape.Direction.LEFT -> {
                        shapeToMove.velocity.x = Math.abs(shapeToMove.velocity.x)
                        shapeToMove.moveBy(2 * ((shapeToMove.getWidth() / 2) - Math.abs(collision.length[0])), 0.0f, 0.0f)

                    }
                    else -> {
                        shapeToMove.velocity.x = -1.0f * Math.abs(shapeToMove.velocity.x)
                        shapeToMove.moveBy(-2 * ((shapeToMove.getWidth() / 2) - Math.abs(collision.length[0])), 0.0f, 0.0f)
                    }
                }

        fun checkIntersectionBetween(shape1 : Shape, shape2 : Shape) : Info {
            // ball - paddle
            if(shape1 is Circle && shape2 is Rectangle){
                // check if a collision is possible with box model to reduce amount of calculations
                if(!(shape1.position.x + shape1.getWidth() / 2 >= shape2.position.x - shape2.getWidth() / 2 &&
                    shape2.position.x + shape2.getWidth() / 2 >= shape1.position.x - shape1.getWidth() / 2 &&
                    shape1.position.y + shape1.getHeight() / 2 >= shape2.position.y - shape2.getHeight() / 2 &&
                    shape2.position.y + shape2.getHeight() / 2 >= shape1.position.y - shape1.getHeight() / 2)){
                    return Info(false, Shape.Direction.NONE, floatArrayOf(0.0f))
                }

                // calculate distance between the two center points
                val positionDiff = Vector(
                        shape1.position.x - shape2.position.x,
                        shape1.position.y - shape2.position.y,
                        0.0f)

                // clamp these values to the boundaries of the rectangle
                val clamped = Vector(
                        Math.max(-shape2.getWidth() / 2.0f, Math.min(shape2.getWidth() / 2.0f, positionDiff.x)),
                        Math.max(-shape2.getHeight() / 2.0f, Math.min(shape2.getHeight() / 2.0f, positionDiff.y)),
                        0.0f)

                // now get the closest point inside the rectangle according to our ball
                val closest = Vector(
                        shape2.position.x + clamped.x,
                        shape2.position.y + clamped.y,
                        0.0f)

                // calculate the difference between the ball's position and the closest point
                val borderDiff = Vector(
                        closest.x - shape1.position.x,
                        closest.y - shape1.position.y,
                        0.0f)

                // if the distance is > the ball's radius we have a intersection, congrats!
                val isIntersecting = (Math.pow(borderDiff.x.toDouble(), 2.0) +
                                    Math.pow(borderDiff.y.toDouble(), 2.0) < Math.pow(shape1.getWidth() / 2.0, 2.0))

                // if we hit a rectangle we need to know bottom / top or left / right
                if(isIntersecting){
                    val directions : FloatArray = floatArrayOf(
                            0.0f, 1.0f,     // up
                            1.0f, 0.0f,     // right
                            0.0f, -1.0f,    // down
                            -1.0f, 0.0f     // left
                    )

                    var max = 0.0f
                    var bestMatch : Shape.Direction = Shape.Direction.NONE
                    for(i in 0 until 4){
                        val length = Vector.magnitude(floatArrayOf(borderDiff.x, borderDiff.y))
                        val dotProduct = Vector.dot(floatArrayOf(borderDiff.x / length, borderDiff.y / length),
                                floatArrayOf(directions[i * 2], directions[i * 2 + 1]))

                        if(dotProduct > max){
                            max = dotProduct
                            bestMatch = Shape.Direction.values()[i]
                        }
                    }

                    return Info(true, bestMatch, floatArrayOf(borderDiff.x, borderDiff.y))

                }

                return Info(false, Shape.Direction.NONE, floatArrayOf(0.0f))
            }

            // ball - powerUp
            if(shape1 is Circle && shape2 is PowerUp){
                val distance = Math.pow((shape1.position.x - shape2.position.x).toDouble(), 2.0) +
                                Math.pow((shape1.position.y - shape2.position.y).toDouble(), 2.0)

                if(distance < Math.pow((shape1.getWidth() / 2.0 + shape2.getWidth() / 2.0), 2.0)){
                    return Info(true, Shape.Direction.NONE, floatArrayOf(0.0f))
                }

                return Info(false, Shape.Direction.NONE, floatArrayOf(0.0f))
            }

            return Info(false, Shape.Direction.NONE, floatArrayOf(0.0f))
        }

        fun checkIfInside(shape : Shape, leftX : Float, bottomY : Float, rightX : Float, topY : Float) : Info = // check if the given shape is inside the bounds and return some info to handle it later
                when {
                    shape.position.x - shape.getWidth() / 2.0f < leftX -> Info(true, Shape.Direction.LEFT, floatArrayOf(shape.position.x - leftX, 0.0f))
                    shape.position.x + shape.getWidth() / 2.0f > rightX -> Info(true, Shape.Direction.RIGHT, floatArrayOf(shape.position.x - rightX, 0.0f))
                    shape.position.y + shape.getHeight() / 2.0f > topY -> Info(true, Shape.Direction.UP, floatArrayOf(0.0f, shape.position.y - topY))
                    shape.position.y - shape.getHeight() / 2.0f < bottomY -> Info(true, Shape.Direction.DOWN, floatArrayOf(0.0f, shape.position.y - bottomY))
                    else -> Info(false, Shape.Direction.NONE, floatArrayOf(0.0f, 0.0f))
                }

        fun resetToBounds(shape : Shape, leftX : Float, bottomY : Float, rightX : Float, topY : Float) {
            // if we exceeded the x limits, reset the shape's position to the given bounds
            if(shape.position.x - shape.getWidth() / 2.0f < leftX){
                shape.position.x = leftX + shape.getWidth() / 2.0f
            }else if(shape.position.x + shape.getWidth() / 2.0f > rightX){
                shape.position.x = rightX - shape.getWidth() / 2.0f
            }

            // same for y direction
            if(shape.position.y - shape.getHeight() / 2.0f < bottomY){
                shape.position.y = bottomY + shape.getHeight() / 2.0f
            }else if(shape.position.y + shape.getHeight() / 2.0f > topY){
                shape.position.y = topY - shape.getHeight() / 2.0f
            }
        }
    }
}