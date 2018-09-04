package com.example.pong.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.pong.GameGLRenderer

/**
 * Created by lneumann on 01.08.17.
 */

internal class GameGLSurfaceView(context: Context, gameMode : String) : GLSurfaceView(context) {
    private val mRenderer: GameGLRenderer

    init {

        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3)

        this.mRenderer = GameGLRenderer(context, gameMode)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // pipe it to our renderer
        val result = mRenderer.handleUserInput(e)
        // redraw surface
        requestRender()
        return result
    }

    override fun onPause() {
        super.onPause()
        this.mRenderer.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        this.mRenderer.resumeGame()
    }

    fun shutdownGame() = this.mRenderer.shutdownGame()
}
