package com.example.autoscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutoScrollService : AccessibilityService() {

    companion object {
        var instance: AutoScrollService? = null
        var speedPxPerSec = 300
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val tickMs = 400L

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            swipeUp()
            handler.postDelayed(this, tickMs)
        }
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopScroll()
        return super.onUnbind(intent)
    }

    fun startScroll() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stopScroll() {
        running = false
        handler.removeCallbacks(tick)
    }

    private fun swipeUp() {
        val dm = resources.displayMetrics
        val distance = speedPxPerSec * tickMs / 1000f
        val x = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.7f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, startY - distance)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, tickMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
