package com.kozaz.decoder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Floating bubble over other apps.
 * - drag it anywhere; on release it snaps to the nearest side edge
 * - after a delay it tucks into the edge (only a small tab stays visible)
 * - touch the tab to pull it back out; tap the bubble to open the converter window
 * - drag it onto the X at the bottom to turn it off
 */
class BubbleService : Service() {
    companion object {
        const val ACTION_STOP = "com.kozaz.decoder.STOP"
        @Volatile var running = false
        @Volatile var instance: BubbleService? = null
    }

    private lateinit var wm: WindowManager
    private val ui = Handler(Looper.getMainLooper())
    private var bubble: ImageView? = null
    private var blp: WindowManager.LayoutParams? = null
    private var trash: ImageView? = null
    private var panel: View? = null
    private var side = 1          // 0 = left, 1 = right
    private var tucked = false
    private var dragging = false
    private var armed = false
    private var anim: ValueAnimator? = null
    private val tuckRunnable = Runnable { tuck() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Prefs.load(this)
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        if (bubble == null) {
            running = true
            side = Prefs.side
            addBubble()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (bubble != null) {
            hidePanel()
            removeBubble()
            addBubble()
        }
    }

    // ---------- called from the settings screen ----------
    fun applyAlpha() {
        ui.post {
            bubble?.animate()?.cancel()
            bubble?.alpha = if (tucked) tuckAlpha() else Prefs.alpha
        }
    }

    fun refresh() {
        ui.post {
            if (bubble == null) return@post
            hidePanel()
            removeBubble()
            side = Prefs.side
            addBubble()
        }
    }

    // ---------- notification ----------
    private fun startAsForeground() {
        val channel = "kozaz_bubble"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channel, "Bubble", NotificationManager.IMPORTANCE_LOW))
        }
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channel) else Notification.Builder(this)
        val n = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kozaz Decoder")
            .setContentText("Bubble is on. Tap to turn it off.")
            .setContentIntent(stopPi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    // ---------- helpers ----------
    private fun themed(): Context = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    private fun size() = dp(Prefs.sizeDp)
    private fun peek() = dp(16)
    private fun screenW() = resources.displayMetrics.widthPixels
    private fun screenH() = resources.displayMetrics.heightPixels
    private fun dockX() = if (side == 0) 0 else screenW() - size()
    private fun tuckX() = if (side == 0) -(size() - peek()) else screenW() - peek()
    private fun tuckAlpha() = maxOf(0.1f, Prefs.alpha * 0.7f)

    private fun clampY(y: Int): Int {
        val lo = dp(24)
        val hi = maxOf(lo, screenH() - size() - dp(24))
        return y.coerceIn(lo, hi)
    }

    @Suppress("DEPRECATION")
    private fun tick(ms: Long = 12) {
        if (!Prefs.haptics) return
        try {
            val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vib.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }

    private fun push() {
        try {
            wm.updateViewLayout(bubble, blp)
        } catch (_: Exception) {
        }
    }

    private fun animateTo(x: Int, y: Int, ms: Long, interp: TimeInterpolator, end: (() -> Unit)? = null) {
        val lp = blp ?: return
        anim?.cancel()
        val x0 = lp.x
        val y0 = lp.y
        var canceled = false
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = ms
        a.interpolator = interp
        a.addUpdateListener {
            val f = it.animatedValue as Float
            lp.x = (x0 + (x - x0) * f).toInt()
            lp.y = (y0 + (y - y0) * f).toInt()
            push()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) { canceled = true }
            override fun onAnimationEnd(animation: Animator) { if (!canceled) end?.invoke() }
        })
        anim = a
        a.start()
    }

    // ---------- bubble ----------
    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val s = size()
        val v = ImageView(themed()).apply {
            setImageDrawable(IconDrawable(Ic.SWAP) { C_ICE })
            background = GlassDrawable(s / 2f, dp(4).toFloat(), 50, true, 30)
            val pad = (s * 0.28f).toInt()
            setPadding(pad, pad, pad, pad)
            alpha = Prefs.alpha
        }
        val lp = WindowManager.LayoutParams(
            s, s, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = dockX()
            y = clampY(if (Prefs.y < 0) dp(200) else Prefs.y)
        }
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var wasTucked = false
        val slop = dp(8)

        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    ui.removeCallbacks(tuckRunnable)
                    anim?.cancel()
                    wasTucked = tucked
                    dragging = false
                    tx = e.rawX
                    ty = e.rawY
                    v.animate().cancel()
                    v.animate().alpha(1f).setDuration(120).start()
                    if (tucked) {
                        tucked = false
                        tick(8)
                        animateTo(dockX(), lp.y, 220, OvershootInterpolator(1.1f))
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging) {
                        if (abs(e.rawX - tx) > slop || abs(e.rawY - ty) > slop) {
                            dragging = true
                            anim?.cancel()
                            sx = lp.x
                            sy = lp.y
                            tx = e.rawX
                            ty = e.rawY
                            v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).start()
                            tick(10)
                            showTrash()
                        }
                    } else {
                        lp.x = sx + (e.rawX - tx).toInt()
                        lp.y = sy + (e.rawY - ty).toInt()
                        push()
                        updateTrash()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        dragging = false
                        v.animate().scaleX(1f).scaleY(1f).alpha(Prefs.alpha).setDuration(140).start()
                        val wasArmed = armed
                        val c = trashCenter()
                        hideTrash()
                        if (wasArmed && c != null) closeIntoTrash(c[0], c[1]) else dockAfterDrag()
                    } else {
                        v.animate().alpha(Prefs.alpha).setDuration(150).start()
                        if (e.action == MotionEvent.ACTION_UP && !wasTucked) togglePanel() else scheduleTuck()
                    }
                }
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
        blp = lp
        tucked = false
        scheduleTuck()
    }

    private fun removeBubble() {
        ui.removeCallbacks(tuckRunnable)
        anim?.cancel()
        hideTrash()
        bubble?.let {
            it.animate().cancel()
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        bubble = null
        blp = null
        tucked = false
        dragging = false
    }

    private fun dockAfterDrag() {
        val lp = blp ?: return
        side = if (lp.x + size() / 2 < screenW() / 2) 0 else 1
        val y = clampY(lp.y)
        Prefs.side = side
        Prefs.y = y
        Prefs.save(this)
        tucked = false
        animateTo(dockX(), y, 260, DecelerateInterpolator()) {
            tick(8)
            scheduleTuck()
        }
    }

    private fun scheduleTuck() {
        ui.removeCallbacks(tuckRunnable)
        if (Prefs.tuckSec > 0 && panel == null && bubble != null && !dragging) {
            ui.postDelayed(tuckRunnable, Prefs.tuckSec * 1000L)
        }
    }

    private fun tuck() {
        val lp = blp ?: return
        if (panel != null || dragging || tucked) return
        tucked = true
        bubble?.animate()?.alpha(tuckAlpha())?.setDuration(250)?.start()
        animateTo(tuckX(), lp.y, 280, AccelerateDecelerateInterpolator())
    }

    // ---------- close zone ----------
    private fun showTrash() {
        if (trash != null) return
        val ts = dp(64)
        val t = ImageView(themed()).apply {
            setImageDrawable(IconDrawable(Ic.CLOSE) { C_ICE })
            background = GlassDrawable(ts / 2f, dp(4).toFloat(), 120, true, 40)
            val pad = dp(20)
            setPadding(pad, pad, pad, pad)
            alpha = 0f
            scaleX = 0.6f
            scaleY = 0.6f
        }
        val lp = WindowManager.LayoutParams(
            ts, ts, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(56)
        }
        wm.addView(t, lp)
        trash = t
        t.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start()
    }

    private fun trashCenter(): IntArray? {
        val t = trash ?: return null
        if (t.width == 0) return null
        val loc = IntArray(2)
        t.getLocationOnScreen(loc)
        return intArrayOf(loc[0] + t.width / 2, loc[1] + t.height / 2)
    }

    private fun updateTrash() {
        val t = trash ?: return
        val c = trashCenter() ?: return
        val lp = blp ?: return
        val bx = lp.x + size() / 2
        val by = lp.y + size() / 2
        val near = hypot((bx - c[0]).toDouble(), (by - c[1]).toDouble()) < dp(80)
        if (near != armed) {
            armed = near
            if (near) tick(20)
            val k = if (near) 1.3f else 1f
            t.animate().scaleX(k).scaleY(k).setDuration(120).start()
        }
    }

    private fun hideTrash() {
        val t = trash
        trash = null
        armed = false
        if (t != null) {
            t.animate().alpha(0f).setDuration(120).withEndAction {
                try {
                    wm.removeView(t)
                } catch (_: Exception) {
                }
            }.start()
        }
    }

    private fun closeIntoTrash(cx: Int, cy: Int) {
        val v = bubble ?: return
        tick(30)
        v.animate().scaleX(0.2f).scaleY(0.2f).alpha(0f).setDuration(180).start()
        animateTo(cx - size() / 2, cy - size() / 2, 180, DecelerateInterpolator()) { stopSelf() }
    }

    // ---------- converter window ----------
    private fun togglePanel() {
        if (panel != null) {
            hidePanel()
            return
        }
        ui.removeCallbacks(tuckRunnable)
        val ctx = themed()
        val kp = KozazPanel(ctx, onClose = { hidePanel() })
        kp.setPadding(dp(8), dp(8), dp(8), dp(8))
        val card = FrameLayout(ctx).apply {
            background = GlassDrawable(dp(20).toFloat(), dp(6).toFloat(), 235, true, 30)
            addView(kp)
        }
        val w = (resources.displayMetrics.widthPixels * 0.94f).toInt()
        val lp = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        wm.addView(card, lp)
        panel = card
        kp.input.requestFocus()
    }

    private fun hidePanel() {
        panel?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null
        scheduleTuck()
    }

    override fun onDestroy() {
        hidePanel()
        removeBubble()
        instance = null
        running = false
        super.onDestroy()
    }
}
