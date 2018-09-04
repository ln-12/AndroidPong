package com.example.pong.ui

import android.app.DialogFragment
import android.app.FragmentTransaction
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.pong.R

class EndGameDialogFragment() : DialogFragment() {
    private var continueCallback : () -> Unit = {}
    private var endCallback : () -> Unit = {}
    private var headingText : String = ""

    constructor(headingText: String) : this() {
        this.headingText = headingText
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_end_game_dialog, container, false)

        val headingTextView = view.findViewById<TextView>(R.id.headingTextView)
        val newGameButton = view.findViewById<Button>(R.id.newGameButton)
        val endButton = view.findViewById<Button>(R.id.endButton)


        newGameButton.setOnClickListener {
            this.continueCallback.invoke()
            this.dismiss()
        }

        endButton.setOnClickListener {
            this.endCallback.invoke()
            this.dismiss()
        }

        headingTextView.text = this.headingText

        // set a custom retro font
        val customFont = Typeface.createFromAsset(activity.assets, MainActivity.FONT_NAME)

        headingTextView.typeface = customFont
        newGameButton.typeface = customFont
        endButton.typeface = customFont

        return view
    }

    fun show(transaction: FragmentTransaction, tag: String, continueCallback : () -> Unit, endCallback : () -> Unit) {
        this.continueCallback = continueCallback
        this.endCallback = endCallback

        super.show(transaction, tag)
    }
}