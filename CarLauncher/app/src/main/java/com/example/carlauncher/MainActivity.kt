package com.example.carlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {

    private data class Slot(val key: String, val label: String, val glyph: String, val c1: Int, val c2: Int, val def: () -> Intent)

    private val slots by lazy {
        listOf(
            Slot("movie", "Movie", "N", 0xFFE50914.toInt(), 0xFF8C0610.toInt()) { packageManager.getLaunchIntentForPackage("com.netflix.mediaclient") ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.netflix.mediaclient")) },
            Slot("show", "Show", "▶", 0xFFFF2D2D.toInt(), 0xFF8F0000.toInt()) { packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.youtube")) },
            Slot("carinfo", "Car Info", "🚙", 0xFF6FCF4A.toInt(), 0xFF2E7D32.toInt()) { Intent() },
            Slot("gallery", "Gallery", "▣", 0xFF5C6BC0.toInt(), 0xFF283593.toInt()) { Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY) },
            Slot("music", "Music", "♪", 0xFF9CCC65.toInt(), 0xFF33691E.toInt()) { Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC) }
        )
    }

    private val prefs by lazy { getSharedPreferences("slots", Context.MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* stay home */ }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                setGradientCenter(0.5f, 1.0f)
                gradientRadius = resources.displayMetrics.widthPixels * 0.8f
                colors = intArrayOf(0xFF1E78E6.toInt(), 0xFF0A2A66.toInt(), 0xFF030914.toInt())
            }
            setPadding(dp(32), dp(24), dp(32), 0)
        }

        // Clock + date
        val clockRow = LinearLayout(this).apply { gravity = Gravity.BOTTOM }
        clockRow.addView(TextClock(this).apply {
            format12Hour = "hh:mm"; format24Hour = "HH:mm"
            setTextColor(Color.WHITE); textSize = 40f; typeface = Typeface.DEFAULT_BOLD
        })
        clockRow.addView(TextClock(this).apply {
            format12Hour = " a"; format24Hour = ""
            setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 0, 0, dp(6))
        })
        root.addView(clockRow)
        root.addView(TextClock(this).apply {
            format12Hour = "yyyy/MM/dd"; format24Hour = "yyyy/MM/dd"
            setTextColor(Color.WHITE); textSize = 22f
        })

        // Icon row
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER; weightSum = slots.size.toFloat() }
        slots.forEach { row.addView(tile(it), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)) }
        root.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        root.addView(navBar(), LinearLayout.LayoutParams(MATCH_PARENT, dp(56)))
        return root
    }

    private fun tile(s: Slot): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val icon = TextView(this).apply {
            text = s.glyph; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(s.c1, s.c2)).apply {
                cornerRadius = dp(16).toFloat(); setStroke(dp(2), 0x88FFFFFF.toInt())
            }
        }
        col.addView(icon, LinearLayout.LayoutParams(dp(88), dp(88)))
        col.addView(View(this).apply { setBackgroundColor(0xFFE53935.toInt()) },
            LinearLayout.LayoutParams(dp(64), dp(2)).apply { topMargin = dp(8) })
        col.addView(TextView(this).apply {
            text = s.label; setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(4) })
        col.setOnClickListener { open(s) }
        col.setOnLongClickListener { pickApp("Assign app to ${s.label}") { pkg -> prefs.edit().putString(s.key, pkg).apply() }; true }
        return col
    }

    private fun navBar(): View {
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER; weightSum = 7f }
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fun btn(g: String, onClick: () -> Unit) = TextView(this).apply {
            text = g; textSize = 22f; setTextColor(0xFFB0BEC5.toInt()); gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }.also { bar.addView(it, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)) }
        btn("⌄") { pickApp("All apps") { launchPkg(it) } }
        btn("🔉") { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI) }
        btn("◀") { }
        btn("●") { recreate() }.setOnLongClickListener { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)); true }
        btn("■") { pickApp("All apps") { launchPkg(it) } }
        btn("🔊") { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI) }
        btn("📻") { pickApp("All apps") { launchPkg(it) } }
        return bar
    }

    private fun open(s: Slot) {
        if (s.key == "carinfo") { showCarInfo(); return }
        val pkg = prefs.getString(s.key, null)
        if (pkg != null) { launchPkg(pkg); return }
        try { startActivity(s.def()) } catch (e: Exception) {
            Toast.makeText(this, "No app found. Long-press to assign one.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchPkg(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    private fun pickApp(title: String, onPick: (String) -> Unit) {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .filter { it.second != packageName }
            .sortedBy { it.first.lowercase() }
        AlertDialog.Builder(this).setTitle(title)
            .setItems(apps.map { it.first }.toTypedArray()) { _, i -> onPick(apps[i].second) }
            .show()
    }

    private fun showCarInfo() {
        val d = android.app.Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen)
        val root = LinearLayout(this).apply { setBackgroundColor(0xFF0A1630.toInt()); setPadding(dp(24), dp(24), dp(24), dp(24)) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = "Mitsubishi Pajero Sport"; textSize = 24f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
        col.addView(TextView(this).apply {
            text = "Engine: 2.4L MIVEC turbo diesel\nPower: 133 kW\nTorque: 430 Nm\nGearbox: 8-speed automatic\nDrive: Super Select 4WD-II\nGround clearance: 218 mm\nWading depth: 700 mm\nTowing (braked): 3,100 kg\nFuel tank: 68 L"
            textSize = 16f; setTextColor(0xFFCFD8DC.toInt()); setLineSpacing(0f, 1.3f); setPadding(0, dp(16), 0, dp(16))
        })
        col.addView(Button(this).apply { text = "✕ Exit"; setOnClickListener { d.dismiss() } })
        val web = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = true; setBackgroundColor(0)
            loadUrl("file:///android_asset/car3d.html")
        }
        root.addView(col, LinearLayout.LayoutParams(0, MATCH_PARENT, 0.8f))
        root.addView(web, LinearLayout.LayoutParams(0, MATCH_PARENT, 1.2f))
        d.setContentView(root); d.show()
    }
}
