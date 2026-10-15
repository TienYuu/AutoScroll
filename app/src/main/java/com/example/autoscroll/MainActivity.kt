package com.example.autoscroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnStart = Button(this).apply { text = "BẮT ĐẦU" }
        val btnPause = Button(this).apply { text = "TẠM NGỪNG" }

        btnStart.setOnClickListener {
            val svc = AutoScrollService.instance
            if (svc == null) {
                Toast.makeText(this, "Hãy bật dịch vụ trợ năng cho app", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                svc.startScroll()
                moveTaskToBack(true)
            }
        }
        btnPause.setOnClickListener { AutoScrollService.instance?.stopScroll() }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(btnStart)
            addView(btnPause)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("Tốc độ")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bar = SeekBar(this).apply {
            max = 1000
            progress = AutoScrollService.speedPxPerSec
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    AutoScrollService.speedPxPerSec = p.coerceAtLeast(50)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        AlertDialog.Builder(this)
            .setTitle("Tốc độ cuộn (px/giây)")
            .setView(bar)
            .setPositiveButton("OK", null)
            .show()
        return true
    }
}
