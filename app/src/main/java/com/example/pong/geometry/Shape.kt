package com.example.pong.geometry

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.example.pong.GameGLRenderer
import com.example.pong.R
import com.example.pong.helper.GLHelper
import com.example.pong.helper.Vector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * This class is used as base for all object which are drawn on the screen.
 *
 * @param context the android app context
 * @param x the horizontal position of the object
 * @param y the vertical position of the object
 * @param width the width of the object
 * @param height the height of the object
 * @param textureIndex the index of the texture array
 * @property color the color of the object on the screen
 * @constructor creates a shape with the given properties
*/
abstract class Shape (private val context: Context, x : Float, y : Float, width : Float, height : Float, private val textureIndex : Int) {
    enum class Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT,
        NONE
    }

    private val ATTRIB_POSITION = 0
    private val ATTRIB_TEXTURE_COORD = 1

    var fragmentShader = R.raw.fragment_default
    var vertexShader = R.raw.vertex_default

    private val width : Float
    private val height : Float

    var scaleX = 1.0f
    var scaleY = 1.0f

    fun getWidth() : Float{
        return this.width * this.scaleX
    }

    fun getHeight() : Float {
        return this.height * this.scaleY
    }

    fun multiplyScaleXwith(factor : Float){
        if(Math.abs(this.scaleX) > 0.25f){
            this.scaleX *= factor
        }
    }

    fun multiplyScaleYwith(factor : Float){
        if(Math.abs(this.scaleY) > 0.25f){
            this.scaleY *= factor
        }
    }

    private var vertexBuffer: FloatBuffer = FloatBuffer.allocate(0)
    var vertexData: FloatArray = FloatArray(0)
    val COORDS_PER_VERTEX = 3
    private var vertexCount : Int = 0
        get() = if(this.useTexture){this.vertexData.size / (this.COORDS_PER_VERTEX + 2)} else {this.vertexData.size / this.COORDS_PER_VERTEX}

    private var vertexStride = 0
        get() = if(this.useTexture) {(this.COORDS_PER_VERTEX + 2) * 4} else {this.COORDS_PER_VERTEX * 4}

    var vertexBufferObject = IntArray(1)

    var useTexture : Boolean = false

    var color : FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

    private var lightPos : FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)

    private var ballCount : Int = 0

    var drawMethod : Int = GLES30.GL_TRIANGLE_FAN

    var position: Vector = Vector(0.0f, 0.0f, 0.0f)
    var velocity: Vector = Vector(0.0f, 0.0f, 0.0f)
    private var rotation : Float = 0.0f

    init{
        this.position.x = x
        this.position.y = y

        this.width = width
        this.height = height

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun moveBy(deltaX: Float, deltaY: Float, deltaZ: Float) {
        position.x += deltaX
        position.y += deltaY
        position.z += deltaZ
    }

    fun moveTo(x: Float, y: Float, z: Float) {
        position.x = x
        position.y = y
        position.z = z
    }

    fun draw(mvpMatrix: FloatArray, invertColor : Boolean, lightPosition : Array<Vector>, _ballCount : Int) {
        this.ballCount = _ballCount

        // setting light positions
        for (i in 0 until this.ballCount){
            this.lightPos[i * 4] = lightPosition[i].x
            this.lightPos[i * 4 + 1] = lightPosition[i].y
            this.lightPos[i * 4 + 2] = lightPosition[i].z
        }

        // get our program handles
        // if it fails, we return to avoid errors
        val programHandles = GLHelper.allShaders.get(Pair(this.fragmentShader, this.vertexShader))?.let { it as? GLHelper.ProgramHandles } ?: return

        GLES30.glUseProgram(programHandles.programHandle)

        // ### PROJECTION MATRIX MANIPULATION
        // we need to clone our matrix cause otherwise the changes are applied to all shapes an the screen
        val newMVPMatrix : FloatArray = mvpMatrix.clone()

        // moveBy the shape in our scene
        Matrix.translateM(newMVPMatrix, 0, position.x, position.y, position.z)

        // rotate the shape
        val rotationMatrix = FloatArray(16)
        Matrix.setRotateM(rotationMatrix, 0, rotation, 0f, 0f, -1.0f)
        Matrix.multiplyMM(newMVPMatrix, 0, newMVPMatrix, 0, rotationMatrix, 0)

        // scale the shape
        Matrix.scaleM(newMVPMatrix, 0, this.scaleX, this.scaleY, 0.0f)


        // ### UNIFORMS ###
        // Set color for drawing the triangle
        GLES30.glUniform4fv(programHandles.colorHandle, 1, this.color, 0)
        GameGLRenderer.checkGLError("draw", "glGetUniformLocation")

        // Apply the projection and view transformation
        GLES30.glUniformMatrix4fv(programHandles.mvpMatrixHandle, 1, false, newMVPMatrix, 0)
        GameGLRenderer.checkGLError("draw", "glUniformMatrix4fv")

        // turn on/off invert color effect
        GLES30.glUniform1i(programHandles.invertColorHandle, if(invertColor) 1 else 0 )
        GameGLRenderer.checkGLError("draw", "glUniformMatrix1i")

        // positions of all light sources
        GLES30.glUniform4fv(programHandles.lightPosHandle, 10, this.lightPos, 0)
        GameGLRenderer.checkGLError("draw", "glGetUniformLocation")

        // number of balls
        GLES30.glUniform1i(programHandles.ballCountHandle, ballCount)
        GameGLRenderer.checkGLError("draw", "glUniformMatrix1i")


        // ### POSITION DATA ###
        // bind buffer to use the correct data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, this.vertexBufferObject[0])

        // specify attribute location
        GLES30.glVertexAttribPointer(this.ATTRIB_POSITION, 3, GLES30.GL_FLOAT, false, this.vertexStride, 0)
        GameGLRenderer.checkGLError("draw", "Failed to specify position attribute")

        GLES30.glEnableVertexAttribArray(this.ATTRIB_POSITION)
        GameGLRenderer.checkGLError("INIT", "Failed to enable position attribute")


        if(this.useTexture) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, this.textureIndex)

            GLES30.glVertexAttribPointer(this.ATTRIB_TEXTURE_COORD, 2, GLES30.GL_FLOAT, false, this.vertexStride, 3 * 4)
            GameGLRenderer.checkGLError("draw", "Failed to specify tex coords attribute")

            GLES30.glEnableVertexAttribArray(this.ATTRIB_TEXTURE_COORD)
            GameGLRenderer.checkGLError("INIT", "Failed to enable position attribute")

            // ### ACTUAL DRAWING ###
            // draw the object on the screen
            GLES30.glDrawArrays(this.drawMethod, 0, this.vertexCount)
            GameGLRenderer.checkGLError("draw", "glDrawArrays")
        }else{
            // ### ACTUAL DRAWING ###
            // draw the object on the screen
            GLES30.glDrawArrays(this.drawMethod, 0, this.vertexCount)
            GameGLRenderer.checkGLError("draw", "glDrawArrays")
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        if(_ballCount > 1){
            this.ballCount = _ballCount
        }
    }

    fun rotate(rotation : Float){
        this.rotation = rotation
    }

    protected fun initVertexData(){
        //Generate a VBO:
        GLES30.glGenBuffers(1, this.vertexBufferObject, 0)
        GameGLRenderer.checkGLError("INIT", "Failed to generate VBO")

        //Bind it:
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, this.vertexBufferObject[0])
        GameGLRenderer.checkGLError("INIT", "Failed to bind VBO")

        // fill our vertex buffer
        val vertexByteBuffer = ByteBuffer.allocateDirect(this.vertexData.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        this.vertexBuffer = vertexByteBuffer.asFloatBuffer()
        this.vertexBuffer.put(this.vertexData)
        this.vertexBuffer.position(0)

        //Upload it:
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, this.vertexData.size * 4, vertexByteBuffer, GLES30.GL_STATIC_DRAW)
        GameGLRenderer.checkGLError("INIT", "Failed to buffer vertex data")

        // specify attribute location
        GLES30.glVertexAttribPointer(this.ATTRIB_POSITION, 3, GLES30.GL_FLOAT, false, this.vertexStride, 0)
        GameGLRenderer.checkGLError("draw", "Failed to specify position attribute")

        GLES30.glEnableVertexAttribArray(this.ATTRIB_POSITION)
        GameGLRenderer.checkGLError("INIT", "Failed to enable position attribute")


        if(this.useTexture) {
            GLES30.glVertexAttribPointer(this.ATTRIB_TEXTURE_COORD, 2, GLES30.GL_FLOAT, false, this.vertexStride, 3 * 4)
            GameGLRenderer.checkGLError("draw", "Failed to specify tex coords attribute")

            GLES30.glEnableVertexAttribArray(this.ATTRIB_TEXTURE_COORD)
            GameGLRenderer.checkGLError("INIT", "Failed to enable position attribute")
        }
    }
}