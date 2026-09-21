package com.rudyunguru.trucks

import android.app.Application

/**
 * Application entry point. Keeps only process-wide, cheap state here: the global
 * [com.rudyunguru.trucks.state.GameState] lives in a holder that is lazily created so a cold
 * start stays fast on low-end phones.
 */
class RomaniaTruckApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
