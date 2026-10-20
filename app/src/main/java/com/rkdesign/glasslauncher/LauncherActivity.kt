package com.rkdesign.glasslauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LauncherActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private var entries: List<AppEntry> = emptyList()
    private val bg = Color.rgb(7, 21, 43)
    private val card = Color.rgb(20, 43, 72)
    private val foreground = Color.rgb(241, 247, 255)
    private val muted = Color.rgb(176, 203, 229)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = bg
            window.navigationBarColor = bg
            window.decorView.systemUiVisibility = 0
            buildBasicScreen()
            loadApps()
        } catch (t: Throwable) {
            Log.e(TAG, "Launcher startup failure", t)
            showEmergencyScreen(t)
        }
    }

    private fun buildBasicScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(12))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val island = TextView(this).apply {
            text = "●  RK GLASS"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(foreground)
            background = rounded(Color.BLACK, 80)
            setPadding(dp(18), dp(10), dp(18), dp(10))
        }
        content.addView(island, lpWrap().apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(24) })
        val clock = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 48f
            setTextColor(foreground)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        content.addView(clock)
        content.addView(TextView(this).apply {
            text = SimpleDateFormat("EEEE, d MMMM", Locale("id", "ID")).format(Date())
            textSize = 14f; setTextColor(muted)
        }, lpWrap().apply { bottomMargin = dp(20) })
        search = EditText(this).apply {
            hint = "Cari aplikasi…"
            setSingleLine(true)
            textSize = 15f
            setTextColor(foreground)
            setHintTextColor(muted)
            background = rounded(card, 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addTextChangedListener(SimpleTextWatcher { renderApps(it) })
        }
        content.addView(search, lpMatch().apply { bottomMargin = dp(14) })
        content.addView(TextView(this).apply {
            text = "APLIKASI"
            textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(muted)
        }, lpWrap().apply { bottomMargin = dp(8) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list, lpMatch())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = TextView(this).apply {
            text = "Mode ringan • Android Oreo"
            textSize = 11f; gravity = Gravity.CENTER; setTextColor(muted)
            setPadding(0, dp(12), 0, dp(5))
            setOnLongClickListener { showLauncherHelp(); true }
        }
        root.addView(bottom, lpWrap())
        setContentView(root)
    }

    private fun loadApps() {
        list.removeAllViews()
        list.addView(label("Memuat aplikasi…"))
        worker.execute {
            val result = try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                packageManager.queryIntentActivities(intent, 0)
                    .filter { it.activityInfo?.packageName != packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .map { ri -> AppEntry(ri.loadLabel(packageManager)?.toString() ?: ri.activityInfo.packageName, ri.activityInfo.packageName, ri) }
                    .sortedBy { it.label.lowercase(Locale.getDefault()) }
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to enumerate launchable apps", t)
                emptyList()
            }
            main.post {
                if (isFinishing || isDestroyed) return@post
                entries = result
                renderApps(search.text?.toString().orEmpty())
            }
        }
    }

    private fun renderApps(query: String) {
        if (!::list.isInitialized) return
        try {
            list.removeAllViews()
            val filtered = entries.filter { it.label.contains(query.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) {
                list.addView(label(if (entries.isEmpty()) "Tidak ada aplikasi yang terbaca. Ketuk muat ulang." else "Aplikasi tidak ditemukan."))
                val retry = actionRow("Muat ulang daftar aplikasi", "↻") { loadApps() }
                list.addView(retry, lpMatch().apply { topMargin = dp(8) })
                return
            }
            filtered.forEach { app ->
                val row = actionRow(app.label, "›") { launch(app) }
                list.addView(row, lpMatch().apply { bottomMargin = dp(7) })
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Render app list failed", t)
            showEmergencyScreen(t)
        }
    }

    private fun launch(app: AppEntry) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.packageName, app.resolveInfo.activityInfo.name)
            startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Cannot launch ${app.packageName}", t)
            Toast.makeText(this, "Aplikasi tidak dapat dibuka", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionRow(title: String, symbol: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(card, 16)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
        val label = TextView(this@LauncherActivity).apply {
            text = title; textSize = 15f; setTextColor(foreground); maxLines = 2
        }
        addView(label, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(this@LauncherActivity).apply {
            text = symbol; textSize = 22f; setTextColor(muted); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(28), -2))
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 14f; setTextColor(muted); setPadding(dp(4), dp(14), dp(4), dp(14))
    }

    private fun showEmergencyScreen(t: Throwable) {
        try {
            val emergency = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(20)); setBackgroundColor(bg)
            }
            emergency.addView(TextView(this).apply {
                text = "RK Glass Launcher"; textSize = 24f; setTextColor(foreground); typeface = Typeface.DEFAULT_BOLD
            })
            emergency.addView(TextView(this).apply {
                text = "Mode pemulihan aktif. Error: ${t.javaClass.simpleName}\n${t.message.orEmpty().take(220)}"
                textSize = 14f; setTextColor(muted); setPadding(0, dp(12), 0, dp(20))
            })
            emergency.addView(actionRow("Buka pengaturan Android", "⚙") {
                try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Throwable) { }
            }, lpMatch())
            emergency.addView(actionRow("Coba buka ulang launcher", "↻") {
                try { buildBasicScreen(); loadApps() } catch (again: Throwable) { Log.e(TAG, "Retry failed", again) }
            }, lpMatch().apply { topMargin = dp(8) })
            setContentView(emergency)
        } catch (fallback: Throwable) {
            Log.e(TAG, "Emergency screen failed", fallback)
            finish()
        }
    }

    private fun showLauncherHelp() {
        AlertDialog.Builder(this)
            .setTitle("RK Glass Launcher")
            .setMessage("Jika aplikasi terus keluar, pilih launcher bawaan di Pengaturan Android. Versi ini memakai tampilan dasar agar kompatibel dengan Android 8.0/8.1.")
            .setPositiveButton("Pengaturan") { _, _ -> try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Throwable) { } }
            .setNegativeButton("Tutup", null).show()
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun lpWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun lpMatch() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    override fun onDestroy() { worker.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy() }

    private data class AppEntry(val label: String, val packageName: String, val resolveInfo: ResolveInfo)
    companion object { private const val TAG = "RKGlassSafe" }
}
