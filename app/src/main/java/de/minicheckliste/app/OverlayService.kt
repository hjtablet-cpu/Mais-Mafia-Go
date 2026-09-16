package de.minicheckliste.app

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.*

class OverlayService : Service() {
    companion object {
        const val ACTION_REFRESH = "de.minicheckliste.app.REFRESH"
        private const val CHANNEL = "mmg"
        private const val NOTIFICATION_ID = 7
        private const val GAME_PACKAGE = "com.goodgamestudios.bigfarmmobileharvest.google"
    }

    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private lateinit var checks: android.content.SharedPreferences
    private lateinit var ui: android.content.SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val items = listOf(
        "Sonnenblumenmais",
        "Weizenbrot",
        "Karotten Zucker",
        "Milch",
        "Spargelsalat",
        "Zuckerrohr",
        "Eier",
        "Tomaten",
        "Dosenmais",
        "Chili-Eintopf",
        "Erdnüsse, Erdbeeren",
        "Bohnen + Dosenbohnen",
        "Abends Sonnenblumen"
    )

    private val watcher = object : Runnable {
        override fun run() {
            try {
                if (!ui.getBoolean("auto", false) || !Settings.canDrawOverlays(this@OverlayService)) {
                    hideOverlay()
                    handler.postDelayed(this, 1500)
                    return
                }

                val current = currentForegroundPackage()
                if (current == GAME_PACKAGE) {
                    if (root == null) showOverlay()
                } else {
                    hideOverlay()
                }
            } catch (_: Exception) {
                hideOverlay()
            }
            handler.postDelayed(this, 900)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        checks = getSharedPreferences("checks", 0)
        ui = getSharedPreferences("ui", 0)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        startForeground(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Mais Mafia Go aktiv")
                .setContentText("Checkliste erscheint automatisch nur in Big Farm")
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentIntent(pi)
                .build()
        )
        handler.post(watcher)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            hideOverlay()
        }
        if (!ui.getBoolean("auto", false)) stopSelf()
        return START_STICKY
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(CHANNEL, "Mais Mafia Go", NotificationManager.IMPORTANCE_LOW)
            )
    }

    private fun currentForegroundPackage(): String? {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10_000
        val events = usm.queryEvents(begin, end)
        val e = UsageEvents.Event()
        var pkg: String? = null
        var latest = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if ((e.eventType == UsageEvents.Event.ACTIVITY_RESUMED || e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) && e.timeStamp >= latest) {
                latest = e.timeStamp
                pkg = e.packageName
            }
        }
        return pkg
    }

    private fun showOverlay() {
        val d = resources.displayMetrics.density
        val width = ui.getInt("width", 165)
        val font = ui.getInt("font", 9).toFloat()
        val alpha = ui.getInt("alpha", 52)
        val a = (255 * alpha / 100f).toInt()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((3 * d).toInt(), (2 * d).toInt(), (3 * d).toInt(), (2 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.argb(a, 15, 18, 20))
                cornerRadius = 10 * d
            }
        }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "✓ Mais Mafia Go"
            setTextColor(Color.WHITE)
            textSize = font + 1
            setPadding(4, 0, 4, 0)
        }

        val reset = Button(this).apply {
            text = "↻"
            textSize = font
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(4, 0, 4, 0)
            setOnClickListener {
                checks.edit().clear().apply()
                rebuild()
            }
        }

        val close = Button(this).apply {
            text = "×"
            textSize = font
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(4, 0, 4, 0)
            setOnClickListener {
                ui.edit().putBoolean("auto", false).apply()
                stopSelf()
            }
        }

        head.addView(title, LinearLayout.LayoutParams(0, (23 * d).toInt(), 1f))
        head.addView(reset, LinearLayout.LayoutParams((28 * d).toInt(), (23 * d).toInt()))
        head.addView(close, LinearLayout.LayoutParams((28 * d).toInt(), (23 * d).toInt()))
        root!!.addView(head)

        val rowH = (font * 1.9f * d).toInt().coerceAtLeast((18 * d).toInt())
        items.forEachIndexed { i, s ->
            root!!.addView(
                CheckBox(this).apply {
                    text = "${i + 1}. $s"
                    textSize = font
                    setTextColor(Color.WHITE)
                    setPadding(0, 0, 0, 0)
                    minHeight = 0
                    minimumHeight = 0
                    isChecked = checks.getBoolean("c$i", false)
                    setOnCheckedChangeListener { _, v ->
                        checks.edit().putBoolean("c$i", v).apply()
                    }
                },
                LinearLayout.LayoutParams(-1, rowH)
            )
        }

        val p = WindowManager.LayoutParams(
            (width * d).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = ui.getInt("x", 0)
            y = ui.getInt("y", 0)
        }

        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        head.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x
                    sy = p.y
                    tx = e.rawX
                    ty = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = sx + (e.rawX - tx).toInt()
                    p.y = sy + (e.rawY - ty).toInt()
                    wm.updateViewLayout(root, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    ui.edit().putInt("x", p.x).putInt("y", p.y).apply()
                    true
                }
                else -> false
            }
        }

        wm.addView(root, p)
    }

    private fun rebuild() {
        hideOverlay()
        showOverlay()
    }

    private fun hideOverlay() {
        root?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        root = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(watcher)
        hideOverlay()
        super.onDestroy()
    }
}
