package com.kozaz.decoder

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout

/** Popup that appears from the text-selection menu in other apps. */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.load(this)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val readOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        val replaceAction: (String) -> Unit = { out ->
            setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, out))
            finish()
        }
        val panel = KozazPanel(
            this, text,
            onClose = { finish() },
            onReplace = if (readOnly) null else replaceAction
        )
        panel.setPadding(dp(8), dp(8), dp(8), dp(8))
        val card = FrameLayout(this).apply {
            background = GlassDrawable(dp(20).toFloat(), dp(6).toFloat(), 245, true, 30)
            addView(panel)
        }
        setContentView(card)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
