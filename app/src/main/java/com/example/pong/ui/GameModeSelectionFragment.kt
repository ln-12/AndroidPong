package com.example.pong.ui

import android.app.Fragment
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.pong.R

class GameModeSelectionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // load layout
        val view = inflater.inflate(R.layout.fragment_game_mode_selection, container, false)

        // after touching a button we want to start the actual game in the specified mode

        // add listener for single player button
        val singlePlayerButton = view.findViewById<Button>(R.id.singlePlayerButton)
        singlePlayerButton?.setOnClickListener {
            singlePlayerButton.isEnabled = false

            // pop the game mode selection from the back stack to return to the main menu when leaving the game
            fragmentManager.popBackStack()

            val intent = Intent(activity, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "SINGLE_PLAYER")
            startActivity(intent)
        }

        // add listener for multi player button
        val multiPlayerButton = view.findViewById<Button>(R.id.multiPlayerButton)
        multiPlayerButton?.setOnClickListener {
            multiPlayerButton.isEnabled = false

            fragmentManager.popBackStack()

            val intent = Intent(activity, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "MULTI_PLAYER")
            startActivity(intent)
        }

        val gameModeTextView = view.findViewById<TextView>(R.id.gameModeTextView)

        // set a custom retro font
        val customFont = Typeface.createFromAsset(activity.assets, MainActivity.FONT_NAME)

        singlePlayerButton.typeface = customFont
        multiPlayerButton.typeface = customFont
        gameModeTextView.typeface = customFont

        return view
    }
}
