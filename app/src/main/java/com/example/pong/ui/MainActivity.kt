package com.example.pong.ui

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.FragmentActivity
import com.example.pong.R

// this class is the main class of our app
// here we start by loading the main menu layout (an empty frame layout) and adding the actual menu layout

class MainActivity : FragmentActivity() {
    companion object {
        val FONT_NAME = "fonts/wendy.ttf"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // load empty layout
        setContentView(R.layout.activity_main_menu)

        // we need to set default values for our settings here to use them in the game
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // load fragment into container
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, MainMenuFragment())
            .commit()
    }
}