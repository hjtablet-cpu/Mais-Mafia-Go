package de.minicheckliste.app

import android.app.*
import android.app.AppOpsManager
import android.os.Bundle
import android.os.Process
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("ui", 0)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 45, 40, 30)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        box.addView(TextView(this).apply {
            text = "Mais Mafia Go"
            textSize = 26f
            setTextColor(Color.BLACK)
        })

        box.addView(TextView(this).apply {
            text = "Automatik: Die Checkliste erscheint nur in Big Farm: Mobile Harvest und wird in WhatsApp oder anderen Apps automatisch ausgeblendet."
            textSize = 16f
            setPadding(0, 18, 0, 18)
        })

        addSlider(box, "Breite", "width", 145, 240, 165)
        addSlider(box, "Schriftgröße", "font", 8, 15, 9)
        addSlider(box, "Transparenz", "alpha", 30, 90, 52)

        box.addView(Button(this).apply {
            text = "AUTOMATIK AKTIVIEREN"
            setOnClickListener {
                prefs.edit().putBoolean("auto", true).apply()
                ensurePermissionsAndStart()
            }
        })

        box.addView(Button(this).apply {
            text = "AUTOMATIK STOPPEN"
            setOnClickListener {
                prefs.edit().putBoolean("auto", false).apply()
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "Automatik gestoppt", Toast.LENGTH_SHORT).show()
            }
        })

        box.addView(TextView(this).apply {
            text = "Beim ersten Einrichten braucht die App zwei Freigaben: 'Über anderen Apps anzeigen' und 'Nutzungsdatenzugriff'. Danach startet die Überwachung automatisch und auch nach einem Neustart des Handys."
            textSize = 13f
            setPadding(0, 18, 0, 0)
        })

        setContentView(box)
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("auto", false) && Settings.canDrawOverlays(this) && hasUsageAccess()) {
            startWatcher()
        }
    }

    private fun addSlider(box: LinearLayout, label: String, key: String, min: Int, max: Int, def: Int) {
        val t = TextView(this).apply {
            text = "$label: ${prefs.getInt(key, def)}"
            textSize = 16f
            setPadding(0, 12, 0, 0)
        }
        box.addView(t)

        val s = SeekBar(this).apply {
            this.max = max - min
            progress = prefs.getInt(key, def) - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar, p: Int, f: Boolean) {
                    val n = p + min
                    t.text = "$label: $n"
                    prefs.edit().putInt(key, n).apply()
                }
                override fun onStartTrackingTouch(v: SeekBar) {}
                override fun onStopTrackingTouch(v: SeekBar) {
                    if (prefs.getBoolean("auto", false) && Settings.canDrawOverlays(this@MainActivity) && hasUsageAccess()) {
                        startWatcher()
                    }
                }
            })
        }
        box.addView(s, LinearLayout.LayoutParams(-1, -2))
    }

    private fun ensurePermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Bitte 'Über anderen Apps anzeigen' erlauben und danach zurückgehen.", Toast.LENGTH_LONG).show()
            return
        }

        if (!hasUsageAccess()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Bitte für 'Mais Mafia Go' den Nutzungsdatenzugriff einschalten und danach zurückgehen.", Toast.LENGTH_LONG).show()
            return
        }

        startWatcher()
        Toast.makeText(this, "Automatik aktiv – die Liste erscheint nur in Big Farm.", Toast.LENGTH_LONG).show()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startWatcher() {
        val i = Intent(this, OverlayService::class.java).apply { action = OverlayService.ACTION_REFRESH }
        startForegroundService(i)
    }
}
