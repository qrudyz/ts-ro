package com.rudyunguru.trucks.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rudyunguru.trucks.R

/**
 * Home shell. The 3D garage backdrop plus every card based menu (Home, Company, Garage, Trucks,
 * Trailers, Drivers, Contracts, Map, Settings) live here as fragments hosted by this activity.
 *
 * NOTE: initial scaffold placeholder, replaced by the full menu implementation.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_placeholder)
    }
}
