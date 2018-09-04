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

class MainMenuFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)

        // Inflate the layout for this fragment
        val startGameButton = view.findViewById<Button>(R.id.startGameButton)
        startGameButton?.setOnClickListener {
            startGameButton.isEnabled = false
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameModeSelectionFragment())
                .addToBackStack(null)
                .commit()
        }

        val showSettingsButton = view.findViewById<Button>(R.id.settingsButton)
        showSettingsButton?.setOnClickListener {
            showSettingsButton.isEnabled = false

            /*
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsActivity())
                    .addToBackStack(null)
                    .commit()
            */
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        val showHelpButton = view.findViewById<Button>(R.id.helpButton)
        showHelpButton?.setOnClickListener {
            showHelpButton.isEnabled = false

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HelpFragment())
                    .addToBackStack(null)
                    .commit()
        }

        val showAboutButton = view.findViewById<Button>(R.id.aboutButton)
        showAboutButton?.setOnClickListener {
            showAboutButton.isEnabled = false

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AboutFragment())
                    .addToBackStack(null)
                    .commit()
        }

        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)

        // set a custom retro font
        val customFont = Typeface.createFromAsset(activity.assets, MainActivity.FONT_NAME)

        startGameButton.typeface = customFont
        showSettingsButton.typeface = customFont
        showHelpButton.typeface = customFont
        showAboutButton.typeface = customFont
        nameTextView.typeface = customFont

        return view
    }
}
