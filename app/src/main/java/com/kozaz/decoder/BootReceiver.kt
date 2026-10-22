package com.kozaz.decoder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Brings the bubble back after a restart, if "Start on boot" is on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Prefs.load(ctx)
        if (!Prefs.boot || !Settings.canDrawOverlays(ctx)) return
        try {
            val i = Intent(ctx, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        } catch (_: Exception) {
        }
    }
}
