package com.kozaz.decoder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var bubbleBtn: GlassButton
    private var themeVer = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.load(this)
        themeVer = Theme.version
        window.statusBarColor = C_BG
        window.navigationBarColor = C_BG
        val shared = if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) ?: "" else ""

        val title = TextView(this).apply {
            text = "Kozaz Decoder"
            setTextColor(C_ICE)
            textSize = 30f
            letterSpacing = 0.02f
            gravity = Gravity.CENTER
            setShadowLayer(dp(10).toFloat(), 0f, 0f, withAlpha(C_GLOW, 200))
        }
        val me = this
        val gear = ImageView(this).apply {
            setImageDrawable(IconDrawable(Ic.GEAR) { C_ICE })
            setPadding(dp(9), dp(9), dp(9), dp(9))
            contentDescription = "Settings"
            setOnClickListener { startActivity(Intent(me, SettingsActivity::class.java)) }
        }
        val panel = KozazPanel(this, shared, onShare = { t -> share(t) })

        bubbleBtn = GlassButton(this, "Floating bubble", Ic.PHONE, 30, true) { toggleBubble() }
        updateBubbleLabel()

        val hint = TextView(this).apply {
            text = "در هر برنامه‌ای متن را انتخاب کن ← ⋮ ← Kozaz Decoder"
            setTextColor(C_MUTED)
            textSize = 13f
            gravity = Gravity.CENTER
        }

        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val header = FrameLayout(this).apply {
            addView(title, FrameLayout.LayoutParams(mp, wc, Gravity.CENTER))
            addView(gear, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.RIGHT or Gravity.CENTER_VERTICAL))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(14), dp(30), dp(14), dp(24))
            addView(header, LinearLayout.LayoutParams(mp, wc))
            addView(OrnamentView(me, 190), LinearLayout.LayoutParams(mp, wc).apply {
                topMargin = dp(8); bottomMargin = dp(22)
            })
            addView(panel, LinearLayout.LayoutParams(mp, wc))
            addView(bubbleBtn, LinearLayout.LayoutParams(mp, dp(60)).apply { topMargin = dp(12) })
            addView(hint, LinearLayout.LayoutParams(mp, wc).apply { topMargin = dp(18) })
            addView(OrnamentView(me, 130), LinearLayout.LayoutParams(mp, wc).apply { topMargin = dp(4) })
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(column)
        }
        val rootFrame = FrameLayout(this).apply {
            background = BackdropDrawable()
            addView(scroll, FrameLayout.LayoutParams(mp, mp))
        }
        setContentView(rootFrame)
    }

    override fun onResume() {
        super.onResume()
        if (Theme.version != themeVer) {
            recreate()
            return
        }
        updateBubbleLabel()
    }

    private fun updateBubbleLabel() {
        bubbleBtn.setLabel(if (BubbleService.running) "Stop floating bubble" else "Floating bubble")
    }

    private fun toggleBubble() {
        if (BubbleService.running) {
            startService(Intent(this, BubbleService::class.java).setAction(BubbleService.ACTION_STOP))
            bubbleBtn.setLabel("Floating bubble")
        } else if (!Settings.canDrawOverlays(this)) {
            toast("Allow \"Display over other apps\", then come back and tap again")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            val i = Intent(this, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            bubbleBtn.setLabel("Stop floating bubble")
            toast("Bubble on")
        }
    }

    private fun share(text: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(i, null))
    }
}
