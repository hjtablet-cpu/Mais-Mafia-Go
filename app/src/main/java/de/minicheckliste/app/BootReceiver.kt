package de.minicheckliste.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("ui", 0)
        if (!prefs.getBoolean("auto", false)) return
        if (!Settings.canDrawOverlays(context)) return
        context.startForegroundService(Intent(context, OverlayService::class.java))
    }
}
