package com.example.pong.ui

import android.app.Activity
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.pong.R
import android.graphics.Typeface
import android.widget.TextView




class GameActivity : Activity() {
    // the view where the OpenGL content is drawn on
    private var mGLView: GameGLSurfaceView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get game mode from previous activity
        val gameMode: String
        gameMode = if (savedInstanceState == null) {
            val extras = intent.extras
            extras.getString("GAME_MODE")
        } else {
            savedInstanceState.getSerializable("GAME_MODE") as String
        }


        // set the content view
        val frame = layoutInflater.inflate(R.layout.activity_game, null) as androidx.constraintlayout.widget.ConstraintLayout
        setContentView(frame)

        // add our OpenGL view to our layout
        this.mGLView = GameGLSurfaceView(this, gameMode)

        // important!
        // keep the context when the user touches e.g the home button so we can go on after
        // switching back to the game
        this.mGLView?.preserveEGLContextOnPause = true
        frame.addView(this.mGLView, 0)


        // set a custom font
        val customFont = Typeface.createFromAsset(assets, MainActivity.FONT_NAME)
        val upperPlayerScoreTextView = findViewById<TextView>(R.id.upperPlayerScoreTextView) as TextView
        val lowerPlayerScoreTextView = findViewById<TextView>(R.id.lowerPlayerScoreTextView) as TextView

        upperPlayerScoreTextView.typeface = customFont
        lowerPlayerScoreTextView.typeface = customFont
    }

    override fun onResume() {
        val ft = fragmentManager.beginTransaction()
        /*val prev = fragmentManager.findFragmentByTag("pauseDialog")
        if (prev != null) {
            ft.remove(prev)
        }*/
        ft.addToBackStack(null)

        ft.commit()

        super.onResume()
    }

    override fun onPause() {
        // when the back button is pressed we want to pause our game
        if (this.mGLView != null) {
            this.mGLView?.onPause()
        }

        val ft = fragmentManager.beginTransaction()
        val prev = fragmentManager.findFragmentByTag("pauseDialog")
        if (prev == null) {
            ft.addToBackStack(null)

            // Create and show the dialog.
            val newFragment = PauseDialogFragment()
            newFragment.isCancelable = false
            newFragment.show(ft, "pauseDialog",
                    {
                        if (this.mGLView != null) {
                            this.mGLView?.onResume()
                        }
                    },
                    {
                        this.finish()
                    })
        }

        super.onPause()
    }

    override fun onDestroy() {
        // destroy the game so we can start fresh when the activity is reopened
        this.mGLView?.shutdownGame()

        super.onDestroy()
    }

    override fun onBackPressed() = this.onPause()
}
