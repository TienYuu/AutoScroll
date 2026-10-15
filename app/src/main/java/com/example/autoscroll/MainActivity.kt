package com.example.autoscroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnShow = Button(this).apply { text = "HIỆN NÚT NỔI" }
        val btnHide = Button(this).apply { text = "TẮT NÚT NỔI" }

        btnShow.setOnClickListener {
            val svc = AutoScrollService.instance
            if (svc == null) {
                Toast.makeText(this, "Hãy bật dịch vụ trợ năng cho app", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                svc.showOverlay()
                moveTaskToBack(true)
            }
        }
        btnHide.setOnClickListener { AutoScrollService.instance?.hideOverlay() }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(btnShow)
            addView(btnHide)
        })
    }
}
