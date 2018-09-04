package com.example.pong.ui

import android.app.Fragment
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.pong.R

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        val textView = view.findViewById<TextView>(R.id.aboutText)

        val customFont = Typeface.createFromAsset(activity.assets, MainActivity.FONT_NAME)

        textView.typeface = customFont

        return view
    }
}