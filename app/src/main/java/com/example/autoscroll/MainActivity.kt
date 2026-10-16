package com.example.autoscroll

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Hỏi thẳng hệ thống: dịch vụ Trợ năng của app có đang được bật không
    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, AutoScrollService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun showFloating() {
        if (!isServiceEnabled()) {
            Toast.makeText(this, "Hãy bật dịch vụ Tự cuộn trong Trợ năng", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        val svc = AutoScrollService.instance
        if (svc != null) {
            svc.showOverlay()
            moveTaskToBack(true)
            return
        }

        // Quyền đã bật nhưng dịch vụ chưa kết nối: chờ một lát rồi thử lại
        Toast.makeText(this, "Đang kết nối dịch vụ...", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            val s = AutoScrollService.instance
            if (s != null) {
                s.showOverlay()
                moveTaskToBack(true)
            } else {
                Toast.makeText(
                    this,
                    "Dịch vụ đang bị hệ thống tạm dừng. Hãy TẮT rồi BẬT lại Tự cuộn trong Trợ năng",
                    Toast.LENGTH_LONG
                ).show()
                openAccessibilitySettings()
            }
        }, 1500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnShow = Button(this).apply { text = "HIỆN NÚT NỔI" }
        val btnHide = Button(this).apply { text = "TẮT NÚT NỔI" }

        btnShow.setOnClickListener { showFloating() }
        btnHide.setOnClickListener { AutoScrollService.instance?.hideOverlay() }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(btnShow)
            addView(btnHide)
        })
    }
}