package com.example.autoscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AutoScrollService : AccessibilityService() {

    companion object {
        var instance: AutoScrollService? = null
    }

    private var speedPxPerSec = 300
    private var intervalSec = 8
    private var modeInterval = false

    private var running = false
    private var blocked = false
    private val tickMs = 400L
    private val handler = Handler(Looper.getMainLooper())

    private var homePkgs: Set<String> = emptySet()
    private var wm: WindowManager? = null
    private var panel: View? = null
    private var btnPlay: Button? = null
    private var btnMode: Button? = null
    private var tvValue: TextView? = null

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            stopScroll()
        }
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            if (!blocked) {
                if (modeInterval) swipe(0.75f, 0.25f, 200) else swipeSlow()
            }
            handler.postDelayed(this, if (modeInterval) intervalSec * 1000L else tickMs)
        }
    }

    override fun onServiceConnected() {
        instance = this
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        homePkgs = packageManager.queryIntentActivities(home, 0)
            .map { it.activityInfo.packageName }.toSet()
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg.contains("inputmethod") || pkg.contains("keyboard")) return
        blocked = pkg == "com.android.systemui" || pkg in homePkgs
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        hideOverlay()
        try { unregisterReceiver(screenOff) } catch (_: Exception) {}
        instance = null
        return super.onUnbind(intent)
    }

    // ---------- Điều khiển cuộn ----------
    private fun startScroll() {
        if (running) return
        running = true
        handler.postDelayed(loop, if (modeInterval) intervalSec * 1000L else 0L)
        updateUi()
    }

    fun stopScroll() {
        running = false
        handler.removeCallbacks(loop)
        updateUi()
    }

    private fun restartIfRunning() {
        if (running) {
            handler.removeCallbacks(loop)
            handler.postDelayed(loop, if (modeInterval) intervalSec * 1000L else 0L)
        }
    }

    private fun swipeSlow() {
        val dm = resources.displayMetrics
        val dist = speedPxPerSec * tickMs / 1000f
        val startY = dm.heightPixels * 0.7f
        swipePath(dm.widthPixels / 2f, startY, startY - dist, tickMs)
    }

    private fun swipe(fromRatio: Float, toRatio: Float, duration: Long) {
        val dm = resources.displayMetrics
        swipePath(
            dm.widthPixels / 2f,
            dm.heightPixels * fromRatio,
            dm.heightPixels * toRatio,
            duration
        )
    }

    private fun swipePath(x: Float, y1: Float, y2: Float, duration: Long) {
        val path = Path().apply { moveTo(x, y1); lineTo(x, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    // ---------- Nút nổi ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun smallBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        setPadding(dp(10), dp(4), dp(10), dp(4))
        setOnClickListener { onClick() }
    }

    fun showOverlay() {
        if (panel != null) return
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val handle = TextView(this).apply {
            text = "✥ kéo"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val close = smallBtn("✕") { hideOverlay() }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(handle, LinearLayout.LayoutParams(0, -2, 1f))
            addView(close)
        }

        btnPlay = smallBtn("▶") {
            if (running) stopScroll() else startScroll()
        }
        btnMode = smallBtn("Liên tục") {
            modeInterval = !modeInterval
            restartIfRunning()
            updateUi()
        }
        val minus = smallBtn("−") { adjust(-1) }
        val plus = smallBtn("+") { adjust(+1) }
        tvValue = TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            minWidth = dp(64)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(minus); addView(tvValue); addView(plus)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(200, 20, 20, 20))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            addView(top)
            addView(btnPlay)
            addView(btnMode)
            addView(row)
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dp(170)
            y = dp(160)
        }

        // Kéo di chuyển bằng phần "✥ kéo"
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = ev.rawX; ty = ev.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = sx + (ev.rawX - tx).toInt()
                    lp.y = sy + (ev.rawY - ty).toInt()
                    wm?.updateViewLayout(box, lp)
                }
            }
            true
        }

        wm?.addView(box, lp)
        panel = box
        updateUi()
    }

    fun hideOverlay() {
        stopScroll()
        panel?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun adjust(dir: Int) {
        if (modeInterval) {
            intervalSec = (intervalSec + dir).coerceIn(1, 120)
        } else {
            speedPxPerSec = (speedPxPerSec + dir * 50).coerceIn(50, 1500)
        }
        restartIfRunning()
        updateUi()
    }

    private fun updateUi() {
        btnPlay?.text = if (running) "⏸" else "▶"
        btnMode?.text = if (modeInterval) "Mỗi XX giây" else "Liên tục"
        tvValue?.text = if (modeInterval) "${intervalSec}s" else "${speedPxPerSec}px/s"
    }
}
