package com.kozaz.decoder

import android.content.Context
import android.graphics.Color

/** Theme colors derived from one hue. Change Theme.hue / Theme.mono to recolor the whole app. */
object Theme {
    var hue = 210f
    var mono = false      // true = white theme
    var version = 0       // bumps when the theme changes so screens know to rebuild

    private fun c(s: Float, v: Float): Int =
        Color.HSVToColor(floatArrayOf(hue, if (mono) s * 0.04f else s, v))

    val bg: Int get() = c(0.80f, 0.06f)
    val glow: Int get() = c(0.88f, 1.00f)
    val edge: Int get() = c(0.72f, 0.84f)
    val edgeStrong: Int get() = c(0.64f, 1.00f)
    val ice: Int get() = c(0.40f, 1.00f)
    val text: Int get() = c(0.19f, 1.00f)
    val muted: Int get() = c(0.35f, 0.65f)
    val fillTop: Int get() = c(0.78f, 0.32f)
    val fillBottom: Int get() = c(0.80f, 0.10f)

    fun set(h: Float, m: Boolean) {
        hue = h
        mono = m
        version++
    }

    fun sample(h: Float, m: Boolean): Int = Color.HSVToColor(floatArrayOf(h, if (m) 0f else 0.85f, 1f))
}

/** Saved settings. */
object Prefs {
    var alpha = 0.5f      // bubble opacity 0.1 .. 1.0
    var sizeDp = 56       // bubble size 40 .. 80
    var tuckSec = 3       // seconds before the bubble tucks into the edge, 0 = never
    var haptics = true
    var boot = false
    var side = 1          // 0 = left edge, 1 = right edge
    var y = -1            // last vertical position, -1 = default

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("kozaz", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        val s = sp(ctx)
        Theme.hue = s.getFloat("hue", 210f)
        Theme.mono = s.getBoolean("mono", false)
        alpha = s.getFloat("alpha", 0.5f).coerceIn(0.1f, 1f)
        sizeDp = s.getInt("size", 56).coerceIn(40, 80)
        tuckSec = s.getInt("tuck", 3).coerceIn(0, 10)
        haptics = s.getBoolean("haptics", true)
        boot = s.getBoolean("boot", false)
        side = s.getInt("side", 1)
        y = s.getInt("y", -1)
    }

    fun save(ctx: Context) {
        sp(ctx).edit()
            .putFloat("hue", Theme.hue)
            .putBoolean("mono", Theme.mono)
            .putFloat("alpha", alpha)
            .putInt("size", sizeDp)
            .putInt("tuck", tuckSec)
            .putBoolean("haptics", haptics)
            .putBoolean("boot", boot)
            .putInt("side", side)
            .putInt("y", y)
            .apply()
    }

    fun reset(ctx: Context) {
        alpha = 0.5f; sizeDp = 56; tuckSec = 3; haptics = true; boot = false
        Theme.set(210f, false)
        save(ctx)
    }
}
