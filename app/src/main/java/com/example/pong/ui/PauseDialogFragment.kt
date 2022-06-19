package com.example.pong.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction

import com.example.pong.R

class PauseDialogFragment : DialogFragment() {
    private var continueCallback : () -> Unit = {}
    private var endCallback : () -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pause_dialog, container, false)

        val headingTextView = view.findViewById<TextView>(R.id.headingTextView)
        val continueButton = view.findViewById<Button>(R.id.continueButton)
        val endButton = view.findViewById<Button>(R.id.endButton)


        continueButton.setOnClickListener {
            this.continueCallback.invoke()
            this.dismiss()
        }

        endButton.setOnClickListener {
            this.endCallback.invoke()
            this.dismiss()
        }

        // set a custom retro font
        val customFont = Typeface.createFromAsset(activity!!.assets, MainActivity.FONT_NAME)

        headingTextView.typeface = customFont
        continueButton.typeface = customFont
        endButton.typeface = customFont

        return view
    }

    fun show(transaction: FragmentTransaction, tag: String, continueCallback : () -> Unit, endCallback : () -> Unit) {
        this.continueCallback = continueCallback
        this.endCallback = endCallback

        super.show(transaction, tag)
    }
}
