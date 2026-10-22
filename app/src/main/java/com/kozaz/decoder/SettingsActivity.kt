package com.kozaz.decoder

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

class SettingsActivity : Activity() {
    private class Preset(val hue: Float, val mono: Boolean)

    private val presets = listOf(
        Preset(210f, false), Preset(140f, false), Preset(355f, false),
        Preset(270f, false), Preset(28f, false), Preset(320f, false), Preset(0f, true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.load(this)
        window.statusBarColor = C_BG
        window.navigationBarColor = C_BG
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT

        // header
        val back = ImageView(this).apply {
            setImageDrawable(IconDrawable(Ic.BACK) { C_ICE })
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = "Back"
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "Settings"
            setTextColor(C_ICE)
            textSize = 26f
            gravity = Gravity.CENTER
            setShadowLayer(dp(10).toFloat(), 0f, 0f, withAlpha(C_GLOW, 200))
        }
        val header = FrameLayout(this).apply {
            addView(title, FrameLayout.LayoutParams(mp, wc, Gravity.CENTER))
            addView(back, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.LEFT or Gravity.CENTER_VERTICAL))
        }

        // theme: preset dots + custom hue slider
        val swRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        for (pr in presets) {
            val sel = pr.mono == Theme.mono && (pr.mono || abs(pr.hue - Theme.hue) < 1f)
            val sw = Swatch(this, pr.hue, pr.mono, sel)
            sw.setOnClickListener {
                Theme.set(pr.hue, pr.mono)
                commitTheme()
            }
            swRow.addView(sw, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        val preview = Swatch(this, Theme.hue, Theme.mono, false)
        val hueSlider = slider(
            "Custom color", { "$it\u00B0" }, 360, Theme.hue.toInt(), true,
            { preview.hue = it.toFloat(); preview.mono = false; preview.invalidate() },
            { Theme.set(it.toFloat(), false); commitTheme() }
        )
        preview.layoutParams = LinearLayout.LayoutParams(mp, dp(34))
        val themeCard = card("Theme", swRow, hueSlider, preview)

        // bubble sliders
        val opacity = slider(
            "Opacity", { String.format(Locale.US, "%.2f", (it + 10) / 100f) }, 90,
            Math.round(Prefs.alpha * 100f) - 10, false,
            { Prefs.alpha = (it + 10) / 100f; BubbleService.instance?.applyAlpha() },
            { Prefs.alpha = (it + 10) / 100f; Prefs.save(this) }
        )
        val size = slider(
            "Size", { "${40 + it} dp" }, 40, Prefs.sizeDp - 40, false,
            { },
            { Prefs.sizeDp = 40 + it; Prefs.save(this); BubbleService.instance?.refresh() }
        )
        val tuck = slider(
            "Hide after", { if (it == 0) "Never" else "$it s" }, 10, Prefs.tuckSec, false,
            { },
            { Prefs.tuckSec = it; Prefs.save(this); BubbleService.instance?.refresh() }
        )
        val bubbleCard = card("Bubble", opacity, size, tuck)

        // general toggles
        val haptics = toggle("Haptic feedback", { Prefs.haptics }) { Prefs.haptics = it; Prefs.save(this) }
        val boot = toggle("Start on boot", { Prefs.boot }) { on ->
            if (on && !Settings.canDrawOverlays(this)) {
                toast("Turn the floating bubble on once first to allow overlays")
            } else {
                Prefs.boot = on
                Prefs.save(this)
            }
        }
        val generalCard = card("General", haptics, boot)

        val reset = GlassButton(this, "Reset to defaults", null) {
            Prefs.reset(this)
            BubbleService.instance?.refresh()
            recreate()
        }
        reset.layoutParams = LinearLayout.LayoutParams(mp, dp(54))

        fun gap() = LinearLayout.LayoutParams(mp, wc).apply { topMargin = dp(10) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(14), dp(30), dp(14), dp(24))
            addView(header, LinearLayout.LayoutParams(mp, wc))
            addView(themeCard, gap())
            addView(bubbleCard, gap())
            addView(generalCard, gap())
            addView(reset, LinearLayout.LayoutParams(mp, dp(54)).apply { topMargin = dp(16) })
        }
        val scroll = ScrollView(this).apply {
            id = 1001
            isFillViewport = true
            addView(column)
        }
        val rootFrame = FrameLayout(this).apply {
            background = BackdropDrawable()
            addView(scroll, FrameLayout.LayoutParams(mp, mp))
        }
        setContentView(rootFrame)
    }

    private fun commitTheme() {
        Prefs.save(this)
        BubbleService.instance?.refresh()
        recreate()
    }

    private fun card(title: String, vararg items: View): FrameLayout {
        val me = this
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(12), dp(10), dp(12), dp(12))
            addView(TextView(me).apply {
                text = title
                setTextColor(C_ICE)
                textSize = 17f
                setPadding(0, 0, 0, dp(8))
            })
            for (v in items) addView(v)
        }
        return FrameLayout(this).apply {
            background = GlassDrawable(dp(18).toFloat(), dp(5).toFloat(), 110, false, 22)
            addView(col)
        }
    }

    private fun slider(
        label: String,
        valueText: (Int) -> String,
        max: Int,
        start: Int,
        rainbow: Boolean,
        onLive: (Int) -> Unit,
        onDone: (Int) -> Unit
    ): LinearLayout {
        val me = this
        val name = TextView(this).apply { text = label; setTextColor(C_TEXT); textSize = 15f }
        val value = TextView(this).apply {
            text = valueText(start)
            setTextColor(C_ICE)
            textSize = 15f
            gravity = Gravity.RIGHT
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val bar = SeekBar(this).apply {
            this.max = max
            progress = start.coerceIn(0, max)
            thumbTintList = ColorStateList.valueOf(if (rainbow) Color.WHITE else C_ICE)
            if (rainbow) {
                progressTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                progressBackgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                val rb = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        0xFFFF3B3B.toInt(), 0xFFFFD43B.toInt(), 0xFF3BFF6A.toInt(),
                        0xFF3BF0FF.toInt(), 0xFF3B6AFF.toInt(), 0xFFD43BFF.toInt(), 0xFFFF3B3B.toInt()
                    )
                )
                rb.cornerRadius = dp(6).toFloat()
                background = InsetDrawable(rb, 0, dp(15), 0, dp(15))
            } else {
                progressTintList = ColorStateList.valueOf(C_EDGE_STRONG)
                progressBackgroundTintList = ColorStateList.valueOf(withAlpha(C_EDGE, 90))
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    value.text = valueText(p)
                    if (fromUser) onLive(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { onDone(s?.progress ?: 0) }
            })
        }
        return LinearLayout(me).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(6), 0, dp(6))
            addView(head)
            addView(bar)
        }
    }

    private fun toggle(label: String, get: () -> Boolean, set: (Boolean) -> Unit): GlassButton {
        lateinit var b: GlassButton
        fun text() = "$label: ${if (get()) "On" else "Off"}"
        b = GlassButton(this, text(), null) {
            set(!get())
            b.setLabel(text())
        }
        b.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
        return b
    }
}
