package com.kozaz.decoder

import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Shared converter UI: used by the main screen, the selection popup and the floating bubble window. */
class KozazPanel(
    ctx: Context,
    initial: String = "",
    onClose: (() -> Unit)? = null,
    onReplace: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null
) : LinearLayout(ctx) {

    val input = EditText(ctx)
    private val output = TextView(ctx)
    private var mode = Mode.AUTO
    private val modeBtn: GlassButton

    init {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val g = ctx.dp(5)
        val pad = g + ctx.dp(14)

        // input box with pencil icon
        input.hint = "متن را بنویس یا Paste کن"
        input.setHintTextColor(C_MUTED)
        input.setTextColor(C_TEXT)
        input.textSize = 17f
        input.minLines = 3
        input.gravity = Gravity.TOP
        input.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        input.background = GlassDrawable(ctx.dp(16).toFloat(), g.toFloat(), 110, true, 34)
        input.setPadding(pad, pad, pad, pad + ctx.dp(18))
        input.setText(initial)

        val inputFrame = FrameLayout(ctx)
        inputFrame.addView(input, FrameLayout.LayoutParams(mp, wc))
        val pen = ImageView(ctx)
        pen.setImageDrawable(IconDrawable(Ic.PENCIL) { withAlpha(C_ICE, 170) })
        val penLp = FrameLayout.LayoutParams(ctx.dp(18), ctx.dp(18), Gravity.BOTTOM or Gravity.LEFT)
        penLp.setMargins(pad, 0, 0, g + ctx.dp(12))
        inputFrame.addView(pen, penLp)
        addView(inputFrame, LinearLayout.LayoutParams(mp, wc))

        // mode + paste
        modeBtn = GlassButton(ctx, mode.label, Ic.SWAP) { mode = mode.next(); refresh() }
        val pasteBtn = GlassButton(ctx, "Paste", Ic.DOC) { paste() }
        addView(row(ctx, modeBtn, pasteBtn))

        // output box
        output.setTextColor(C_ICE)
        output.textSize = 18f
        output.minLines = 4
        output.setTextIsSelectable(true)
        output.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        output.background = GlassDrawable(ctx.dp(16).toFloat(), g.toFloat(), 70, false, 18)
        output.setPadding(pad, pad, pad, pad)
        addView(output, LinearLayout.LayoutParams(mp, wc))

        // copy / share / replace / close
        val buttons = mutableListOf(GlassButton(ctx, "Copy", Ic.COPY) { ctx.copyToClipboard(output.text.toString()) })
        onShare?.let { f -> buttons.add(GlassButton(ctx, "Share", Ic.SHARE) { f(output.text.toString()) }) }
        onReplace?.let { f -> buttons.add(GlassButton(ctx, "Replace", Ic.PENCIL) { f(output.text.toString()) }) }
        onClose?.let { f -> buttons.add(GlassButton(ctx, "Close", null) { f() }) }
        addView(row(ctx, *buttons.toTypedArray()))

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { refresh() }
        })
        refresh()
    }

    private fun row(ctx: Context, vararg btns: GlassButton): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        for (b in btns) addView(b, LinearLayout.LayoutParams(0, ctx.dp(54), 1f))
    }

    private fun refresh() {
        output.text = Decoder.convert(input.text.toString(), mode)
        modeBtn.setLabel(mode.label)
    }

    private fun paste() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val t = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else ""
        if (t.isEmpty()) context.toast("Clipboard is empty or access was denied") else input.setText(t)
    }
}
