package com.rudyunguru.trucks.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.rudyunguru.trucks.R

/**
 * Boot screen: shows the branded splash while the world/save bootstrap runs, then hands over to
 * [MainActivity]. The heavy work happens on a worker thread inside the state layer; this class only
 * owns the visual transition so the app never shows a frozen black screen.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed({ launchHome() }, BOOT_DELAY_MS)
    }

    private fun launchHome() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val BOOT_DELAY_MS = 1200L
    }
}
