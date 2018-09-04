package com.example.pong.ui

import android.os.Bundle
import com.example.pong.R
import android.preference.*


class SettingsActivity : PreferenceActivity() {

    // TODO change font for settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
        setContentView(R.layout.fragment_settings_container)
    }
}
