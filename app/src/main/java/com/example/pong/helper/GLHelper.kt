package com.example.pong.helper

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.example.pong.GameGLRenderer

class GLHelper {
    class ProgramHandles {
        var programHandle: Int = -1
        var colorHandle: Int = -1
        var invertColorHandle: Int = -1
        var mvpMatrixHandle: Int = -1
        var lightPosHandle: Int = -1
        var ballCountHandle: Int = -1
    }

    companion object {
        var allShaders = HashMap<Pair<Int,Int>, ProgramHandles>()

        fun loadAllShaders(context : Context){
            for(shader in allShaders){
                loadProgram(context, shader.key.first, shader.key.second, shader.value)
                getUniformLocations(shader.value)
            }
        }

        private fun loadProgram(context : Context, fragmentShader : Int, vertexShader : Int, programHandles : ProgramHandles) : Int {
            var fileInputStream = context.resources.openRawResource(fragmentShader)
            val fragmentShaderCode = fileInputStream.bufferedReader().use { it.readText() }

            fileInputStream = context.resources.openRawResource(vertexShader)
            val vertexShaderCode = fileInputStream.bufferedReader().use { it.readText() }

            val vertexShaderHandle = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShaderHandle = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

            if(vertexShaderHandle == -1 || fragmentShaderHandle == -1){
                Log.e("loadProgram", "Failed to load shader")
                return -1
            }

            programHandles.programHandle = GLES30.glCreateProgram()             // create empty OpenGL ES Program
            GameGLRenderer.checkGLError("loadProgram", "glCreateProgram")

            GLES30.glAttachShader(programHandles.programHandle, vertexShaderHandle)   // add the vertex shader to programHandle
            GameGLRenderer.checkGLError("loadProgram", "glAttachShader vertexShader")

            GLES30.glAttachShader(programHandles.programHandle, fragmentShaderHandle) // add the fragment shader to programHandle
            GameGLRenderer.checkGLError("loadProgram", "glAttachShader fragmentShader")

            GLES30.glLinkProgram(programHandles.programHandle)
            GameGLRenderer.checkGLError("loadProgram", "glLinkProgram")

            // after linking we want to know if it was successful
            val linkStatus = IntArray(1)

            GLES30.glGetProgramiv(programHandles.programHandle, GLES30.GL_LINK_STATUS, linkStatus, 0)
            GameGLRenderer.checkGLError("loadProgram", "glGetProgramiv")

            if (linkStatus[0] != GLES30.GL_TRUE) {
                //Retrieve the error message:
                val errorMessage = GLES30.glGetProgramInfoLog(programHandles.programHandle)
                GameGLRenderer.checkGLError("loadProgram", "glGetProgramInfoLog")

                //Print it and fail:
                Log.e("loadProgram", errorMessage)
                return -1
            }

            // clean up
            GLES30.glDetachShader(programHandles.programHandle, vertexShaderHandle)
            GameGLRenderer.checkGLError("loadProgram", "glDetachShader vertexShader")

            GLES30.glDetachShader(programHandles.programHandle, fragmentShaderHandle)
            GameGLRenderer.checkGLError("loadProgram", "glDetachShader fragmentShader")

            GLES30.glDeleteShader(vertexShaderHandle)
            GameGLRenderer.checkGLError("loadProgram", "glDeleteShader vertexShader")

            GLES30.glDeleteShader(fragmentShaderHandle)
            GameGLRenderer.checkGLError("loadProgram", "glDeleteShader fragmentShader")

            // Add programHandle to OpenGL ES environment
            GLES30.glUseProgram(programHandles.programHandle)
            GameGLRenderer.checkGLError("loadProgram", "glUseProgram")

            return 0
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
            val shaderHandle = GLES30.glCreateShader(type)
            GameGLRenderer.checkGLError("loadShader", "glCreateShader " + type)

            // add the source code to the shader and compile it
            GLES30.glShaderSource(shaderHandle, shaderCode)
            GameGLRenderer.checkGLError("loadShader", "glShaderSource " + type)
            GLES30.glCompileShader(shaderHandle)
            GameGLRenderer.checkGLError("loadShader", "glCompileShader " + type)

            // after compilation check if it was successful
            val compileStatus = IntArray(1)

            GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            GameGLRenderer.checkGLError("loadShader", "glGetShaderiv")

            if (compileStatus[0] != GLES30.GL_TRUE) {
                // it failed so we want to know why
                val errorMessageRaw = GLES30.glGetShaderInfoLog(shaderHandle)
                GameGLRenderer.checkGLError("loadShader", "glGetShaderInfoLog")

                Log.e("loadShader", errorMessageRaw)

                return -1
            }

            return shaderHandle
        }

        private fun getUniformLocations(programHandles : ProgramHandles) {
            // get handle to fragment shader's vColor member
            programHandles.colorHandle = GLES30.glGetUniformLocation(programHandles.programHandle, "vColor")
            GameGLRenderer.checkGLError("getUniformLocations", "glGetUniformLocation vColor")

            // get handle to fragment shader's uMVPMatrix member
            programHandles.mvpMatrixHandle = GLES30.glGetUniformLocation(programHandles.programHandle, "uMVPMatrix")
            GameGLRenderer.checkGLError("getUniformLocations", "glGetUniformLocation uMVPMatrix")

            programHandles.invertColorHandle =  GLES30.glGetUniformLocation(programHandles.programHandle, "invertColor")
            GameGLRenderer.checkGLError("getUniformLocations", "glGetUniformLocation invertColor")

            programHandles.lightPosHandle = GLES30.glGetUniformLocation(programHandles.programHandle, "lightPos")
            GameGLRenderer.checkGLError("getUniformLocations", "glGetUniformLocation lightPos")

            programHandles.ballCountHandle = GLES30.glGetUniformLocation(programHandles.programHandle, "ballCount")
            GameGLRenderer.checkGLError("getUniformLocations", "glGetUniformLocation ballCount")
        }

        // init one texture
        fun initializeTexture(context : Context, textureID : Int) : Int {
            //Activate the texture unit:
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GameGLRenderer.checkGLError("createTexture", "Failed to activate texture unit")

            //Generate a texture handle:
            val textureHandle = IntArray(1)

            GLES30.glGenTextures(1, textureHandle, 0)
            GameGLRenderer.checkGLError("createTexture", "Failed to generate texture handle")

            //Bind our texture:
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])
            GameGLRenderer.checkGLError("createTexture", "Failed to bind texture")

            //Set wrapping mode:
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GameGLRenderer.checkGLError("createTexture", "Failed to set wrapping for s")

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GameGLRenderer.checkGLError("createTexture", "Failed to set wrapping for t")

            //Set min filter:
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GameGLRenderer.checkGLError("createTexture", "Failed to set texture minification filter")

            //Set mag filter:
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GameGLRenderer.checkGLError("createTexture", "Failed to set texture magnification filter")

            val bitmap = BitmapFactory.decodeResource(context.resources, textureID)

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            GameGLRenderer.checkGLError("createTexture", "load texture")

            bitmap.recycle()

            //Bind our texture:
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])
            GameGLRenderer.checkGLError("createTexture", "Failed to bind texture")

            //Create the textures:
            return textureHandle[0]
        }
    }
}