package com.example.pong.ui

import android.os.Bundle
import android.preference.PreferenceActivity
import com.example.pong.R

class SettingsActivity: PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
        setContentView(R.layout.fragment_settings_container)
    }
}
