package com.kozaz.decoder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

// ---------- palette: all colors follow the current theme ----------
val C_BG: Int get() = Theme.bg
val C_ICE: Int get() = Theme.ice
val C_TEXT: Int get() = Theme.text
val C_MUTED: Int get() = Theme.muted
val C_EDGE: Int get() = Theme.edge
val C_EDGE_STRONG: Int get() = Theme.edgeStrong
val C_GLOW: Int get() = Theme.glow

fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

fun Context.dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Context.copyToClipboard(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("kozaz", text))
    toast("Copied")
}

// ---------- line icons drawn in code ----------
enum class Ic { DOC, SHARE, COPY, PHONE, PENCIL, STAR, SWAP, GEAR, BACK, CLOSE }

class IconDrawable(private val ic: Ic, private val color: () -> Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun draw(c: Canvas) {
        val b = bounds
        if (b.width() <= 0) return
        c.save()
        c.translate(b.left.toFloat(), b.top.toFloat())
        val s = b.width() / 24f
        c.scale(s, s)
        p.color = color()
        p.style = if (ic == Ic.STAR) Paint.Style.FILL else Paint.Style.STROKE
        when (ic) {
            Ic.DOC -> {
                val path = Path()
                path.moveTo(6f, 3f); path.lineTo(14f, 3f); path.lineTo(19f, 8f)
                path.lineTo(19f, 21f); path.lineTo(6f, 21f); path.close()
                c.drawPath(path, p)
                c.drawLine(14f, 3f, 14f, 8f, p)
                c.drawLine(14f, 8f, 19f, 8f, p)
                c.drawLine(9f, 13f, 16f, 13f, p)
                c.drawLine(9f, 16.5f, 16f, 16.5f, p)
            }
            Ic.SHARE -> {
                c.drawCircle(18f, 5.5f, 2.6f, p)
                c.drawCircle(6f, 12f, 2.6f, p)
                c.drawCircle(18f, 18.5f, 2.6f, p)
                c.drawLine(8.3f, 10.8f, 15.7f, 6.7f, p)
                c.drawLine(8.3f, 13.2f, 15.7f, 17.3f, p)
            }
            Ic.COPY -> {
                c.drawRoundRect(9f, 9f, 20f, 21f, 2f, 2f, p)
                val path = Path()
                path.moveTo(9f, 15f); path.lineTo(4f, 15f); path.lineTo(4f, 3f)
                path.lineTo(15f, 3f); path.lineTo(15f, 9f)
                c.drawPath(path, p)
            }
            Ic.PHONE -> {
                c.drawRoundRect(7f, 3f, 17f, 21f, 2.5f, 2.5f, p)
                c.drawLine(9.5f, 16.5f, 14.5f, 7.5f, p)
            }
            Ic.PENCIL -> {
                val path = Path()
                path.moveTo(4f, 20f); path.lineTo(5f, 15f); path.lineTo(16f, 4f)
                path.lineTo(20f, 8f); path.lineTo(9f, 19f); path.close()
                c.drawPath(path, p)
                c.drawLine(13.5f, 6.5f, 17.5f, 10.5f, p)
            }
            Ic.STAR -> {
                val path = Path()
                path.moveTo(12f, 2f)
                path.quadTo(12.8f, 11.2f, 22f, 12f)
                path.quadTo(12.8f, 12.8f, 12f, 22f)
                path.quadTo(11.2f, 12.8f, 2f, 12f)
                path.quadTo(11.2f, 11.2f, 12f, 2f)
                path.close()
                c.drawPath(path, p)
            }
            Ic.SWAP -> {
                c.drawLine(4f, 8f, 19f, 8f, p)
                val a = Path()
                a.moveTo(15f, 4f); a.lineTo(19f, 8f); a.lineTo(15f, 12f)
                c.drawPath(a, p)
                c.drawLine(20f, 16f, 5f, 16f, p)
                val d = Path()
                d.moveTo(9f, 12f); d.lineTo(5f, 16f); d.lineTo(9f, 20f)
                c.drawPath(d, p)
            }
            Ic.GEAR -> {
                c.drawCircle(12f, 12f, 3.2f, p)
                c.drawCircle(12f, 12f, 7f, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val ca = Math.cos(a).toFloat()
                    val sa = Math.sin(a).toFloat()
                    c.drawLine(12f + 7f * ca, 12f + 7f * sa, 12f + 10.2f * ca, 12f + 10.2f * sa, p)
                }
            }
            Ic.BACK -> {
                val path = Path()
                path.moveTo(15f, 5f); path.lineTo(8f, 12f); path.lineTo(15f, 19f)
                c.drawPath(path, p)
            }
            Ic.CLOSE -> {
                c.drawLine(6f, 6f, 18f, 18f, p)
                c.drawLine(18f, 6f, 6f, 18f, p)
            }
        }
        c.restore()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

// ---------- glowing glass panel (buttons, boxes, bubble) ----------
class GlassDrawable(
    private val radius: Float,
    private val glow: Float,
    private val fillAlpha: Int = 150,
    private val strongEdge: Boolean = false,
    private val glowAlpha: Int = 26
) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        rect.set(b.left + glow, b.top + glow, b.right - glow, b.bottom - glow)
        if (rect.width() <= 0f || rect.height() <= 0f) return
        val r = minOf(radius, rect.height() / 2f, rect.width() / 2f)

        val step = glow / 4f
        line.color = withAlpha(C_GLOW, glowAlpha)
        for (k in 4 downTo 1) {
            line.strokeWidth = 2f * step * k
            c.drawRoundRect(rect, r, r, line)
        }

        fill.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            withAlpha(Theme.fillTop, fillAlpha),
            withAlpha(Theme.fillBottom, fillAlpha),
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rect, r, r, fill)

        line.strokeWidth = maxOf(2f, glow / 4f)
        line.color = if (strongEdge) C_EDGE_STRONG else C_EDGE
        c.drawRoundRect(rect, r, r, line)
    }

    override fun getPadding(padding: Rect): Boolean {
        val g = glow.toInt()
        padding.set(g, g, g, g)
        return true
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

// ---------- app background: near-black with soft corner swirls ----------
class BackdropDrawable : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun halo(c: Canvas, cx: Float, cy: Float, r: Float) {
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r, withAlpha(C_GLOW, 46), withAlpha(C_GLOW, 0), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    private fun arc(c: Canvas, cx: Float, cy: Float, r: Float, a: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = withAlpha(C_EDGE_STRONG, a)
        c.drawCircle(cx, cy, r, p)
    }

    override fun draw(c: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = C_BG
        c.drawRect(bounds, p)
        halo(c, 0f, 0f, w * 0.9f)
        halo(c, w, h, w * 0.9f)
        arc(c, -w * 0.12f, -w * 0.10f, w * 0.50f, 90)
        arc(c, -w * 0.16f, -w * 0.14f, w * 0.56f, 50)
        arc(c, w * 1.12f, h + w * 0.10f, w * 0.50f, 90)
        arc(c, w * 1.16f, h + w * 0.14f, w * 0.56f, 50)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

// ---------- thin line with a sparkle in the middle ----------
class OrnamentView(ctx: Context, private val spanDp: Int = 200) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = IconDrawable(Ic.STAR) { withAlpha(C_ICE, 230) }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), context.dp(18))
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val half = context.dp(spanDp) / 2f
        val gap = context.dp(14).toFloat()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.shader = LinearGradient(cx - half, 0f, cx - gap, 0f,
            withAlpha(C_EDGE_STRONG, 0), withAlpha(C_EDGE_STRONG, 210), Shader.TileMode.CLAMP)
        canvas.drawLine(cx - half, cy, cx - gap, cy, p)
        p.shader = LinearGradient(cx + gap, 0f, cx + half, 0f,
            withAlpha(C_EDGE_STRONG, 210), withAlpha(C_EDGE_STRONG, 0), Shader.TileMode.CLAMP)
        canvas.drawLine(cx + gap, cy, cx + half, cy, p)
        p.shader = null
        val s = context.dp(14)
        star.setBounds((cx - s / 2f).toInt(), (cy - s / 2f).toInt(), (cx + s / 2f).toInt(), (cy + s / 2f).toInt())
        star.draw(canvas)
    }
}

// ---------- color dot for the theme picker ----------
class Swatch(ctx: Context, var hue: Float, var mono: Boolean, var selected: Boolean) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - context.dp(7)
        p.style = Paint.Style.FILL
        p.color = Theme.sample(hue, mono)
        canvas.drawCircle(cx, cy, r, p)
        if (selected) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = context.dp(2).toFloat()
            p.color = Color.WHITE
            canvas.drawCircle(cx, cy, r + context.dp(4), p)
        }
    }
}

// ---------- glass button: [icon] label ----------
class GlassButton(
    ctx: Context,
    label: String,
    icon: Ic?,
    radiusDp: Int = 14,
    strong: Boolean = false,
    onTap: () -> Unit
) : LinearLayout(ctx) {
    private val labelView = TextView(ctx)

    init {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        background = GlassDrawable(
            ctx.dp(radiusDp).toFloat(), ctx.dp(5).toFloat(),
            if (strong) 130 else 150, strong, if (strong) 38 else 26
        )
        if (icon != null) {
            val iv = ImageView(ctx)
            iv.setImageDrawable(IconDrawable(icon) { C_ICE })
            val lp = LinearLayout.LayoutParams(ctx.dp(20), ctx.dp(20))
            lp.rightMargin = ctx.dp(10)
            addView(iv, lp)
        }
        labelView.text = label
        labelView.setTextColor(C_TEXT)
        labelView.textSize = 15f
        addView(labelView, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        isClickable = true
        setOnClickListener { onTap() }
    }

    fun setLabel(t: String) {
        labelView.text = t
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        alpha = if (pressed) 0.6f else 1f
    }
}
