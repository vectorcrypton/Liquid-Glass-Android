package com.glasscc

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as WLP
import android.view.accessibility.AccessibilityEvent
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class GlassService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private var torchOn = false

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addHandle()
    }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        panel?.let { runCatching { wm.removeView(it) } }
        handle?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    // ---- Auslöser: kleiner Strich am rechten Rand, nach links ziehen ----
    private fun addHandle() {
        val v = View(this)
        val pill = GradientDrawable()
        pill.cornerRadius = dp(2).toFloat(); pill.setColor(0x99FFFFFF.toInt())
        v.background = InsetDrawable(pill, dp(24), dp(35), 0, dp(35))
        var x0 = 0f
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> x0 = e.rawX
                MotionEvent.ACTION_MOVE -> if (x0 - e.rawX > dp(36)) openPanel()
            }
            true
        }
        v.post { v.systemGestureExclusionRects = listOf(Rect(0, 0, v.width, v.height)) }
        val lp = WLP(dp(28), dp(110), WLP.TYPE_ACCESSIBILITY_OVERLAY,
            WLP.FLAG_NOT_FOCUSABLE or WLP.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        lp.y = -dp(120)
        wm.addView(v, lp); handle = v
    }

    // ---- Panel mit echtem Hintergrund-Blur ----
    private fun openPanel() {
        if (panel != null) return
        val content = buildContent()
        val root = FrameLayout(this)
        root.setOnClickListener { closePanel() }
        val clp = FrameLayout.LayoutParams(-1, -2, Gravity.TOP)
        clp.setMargins(dp(14), dp(56), dp(14), 0)
        root.addView(content, clp)

        val lp = WLP(-1, -1, WLP.TYPE_ACCESSIBILITY_OVERLAY,
            WLP.FLAG_BLUR_BEHIND or WLP.FLAG_DIM_BEHIND or WLP.FLAG_LAYOUT_IN_SCREEN or WLP.FLAG_LAYOUT_NO_LIMITS or WLP.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)
        lp.dimAmount = 0.2f
        lp.blurBehindRadius = 0
        wm.addView(root, lp); panel = root

        content.alpha = 0f; content.scaleX = .88f; content.scaleY = .88f; content.translationY = -dp(40).toFloat()
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setDuration(420).setInterpolator(OvershootInterpolator(1.2f)).start()

        val a = ValueAnimator.ofInt(0, 90).setDuration(320)
        a.addUpdateListener {
            if (panel === root && root.isAttachedToWindow) {
                lp.blurBehindRadius = it.animatedValue as Int
                wm.updateViewLayout(root, lp)
            }
        }
        a.start()
    }

    private fun closePanel() {
        val p = panel ?: return
        panel = null
        p.animate().alpha(0f).setDuration(180).withEndAction { runCatching { wm.removeView(p) } }.start()
    }

    // ---- Inhalt ----
    private fun cell(w: Float, h: Int): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, h, w)
        lp.setMargins(dp(5), dp(5), dp(5), dp(5))
        return lp
    }

    private fun hRow(h: Int, vararg tiles: View): LinearLayout {
        val row = LinearLayout(this)
        row.layoutParams = LinearLayout.LayoutParams(-1, -2)
        tiles.forEach { row.addView(it, cell(1f, h)) }
        return row
    }

    private fun tile(emoji: String, label: String, active: Boolean = false, onClick: (GlassCard) -> Unit): GlassCard {
        val card = GlassCard(this, dp(26).toFloat())
        card.active = active
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        val ic = TextView(this); ic.text = emoji; ic.textSize = 22f; ic.gravity = Gravity.CENTER
        val tx = TextView(this); tx.text = label; tx.textSize = 12f; tx.setTextColor(Color.WHITE); tx.gravity = Gravity.CENTER
        col.addView(ic); col.addView(tx)
        card.addView(col, FrameLayout.LayoutParams(-1, -1))
        card.setOnClickListener { onClick(card) }
        card.setOnTouchListener { v, e ->   // "flüssiges" Eindrücken
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.93f).scaleY(.93f).setDuration(120).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(3f)).setDuration(300).start()
            }
            false
        }
        return card
    }

    private fun buildContent(): View {
        val h = dp(78)
        val r = dp(26).toFloat()
        val am = getSystemService(AudioManager::class.java)
        val vol = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val br = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        grid.addView(hRow(h,
            tile("🔵", "Bluetooth") { go(Settings.ACTION_BLUETOOTH_SETTINGS) },
            tile("🔦", "Lampe", torchOn) { c ->
                torchOn = !torchOn
                runCatching { setTorch(torchOn) }
                c.active = torchOn
            }))
        grid.addView(hRow(h,
            tile("📸", "Screenshot") { act(GLOBAL_ACTION_TAKE_SCREENSHOT) },
            tile("🔒", "Sperren") { act(GLOBAL_ACTION_LOCK_SCREEN) }))

        val mid = LinearLayout(this)
        mid.layoutParams = LinearLayout.LayoutParams(-1, -2)
        mid.addView(grid, LinearLayout.LayoutParams(0, -2, 2f))
        mid.addView(GlassSlider(this, r, "☀️", br) { v ->
            if (Settings.System.canWrite(this@GlassService))
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, (v * 255).toInt().coerceAtLeast(1))
            else go(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
        }, cell(1f, 2 * h + dp(10)))
        mid.addView(GlassSlider(this, r, "🔊", vol) { v ->
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (v * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
        }, cell(1f, 2 * h + dp(10)))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.addView(hRow(h,
            tile("📶", "WLAN") { go(Settings.ACTION_WIFI_SETTINGS) },
            tile("↕️", "Internet") { go(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) }))
        root.addView(mid)
        root.addView(hRow(dp(64),
            tile("🔔", "Mitteilungen") { act(GLOBAL_ACTION_NOTIFICATIONS) },
            tile("⚙️", "Einstellungen") { go(Settings.ACTION_SETTINGS) },
            tile("🔋", "Energie") { go(Settings.ACTION_BATTERY_SAVER_SETTINGS) }))
        return root
    }

    // ---- Aktionen ----
    private fun go(action: String, uri: Uri? = null) {
        closePanel()
        startActivity(Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun act(a: Int) {
        closePanel()
        Handler(Looper.getMainLooper()).postDelayed({ performGlobalAction(a) }, 500)
    }

    private fun setTorch(on: Boolean) {
        val cm = getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        cm.setTorchMode(id, on)
    }
}
