package com.rudyunguru.trucks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rudyunguru.trucks.R
import com.rudyunguru.trucks.core.Format
import com.rudyunguru.trucks.state.GameHolder

/**
 * Home shell: company status, quick actions and the entry point into driving.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_placeholder)

        val state = GameHolder.stateInternal(this)
        val d = state.data()

        findViewById<TextView>(R.id.titleCompany).text =
            d.company.name.ifEmpty { getString(R.string.app_name) }
        findViewById<TextView>(R.id.labelMoney).text =
            getString(R.string.label_balance) + ": " + Format.money(d.player.moneyEuro)
        findViewById<TextView>(R.id.labelFleet).text =
            getString(R.string.label_trucks) + ": " + d.trucks.size + "  •  " +
                getString(R.string.label_drivers) + ": " + d.drivers.size

        findViewById<Button>(R.id.btnDrive).setOnClickListener {
            val truck = state.trucks().firstOrNull()
            if (truck == null) {
                Toast.makeText(this, "Cumpără mai întâi un camion", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, DriveActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnNewTruck).setOnClickListener {
            val cheapest = com.rudyunguru.trucks.core.catalog.TruckCatalog.TRUCKS.minBy { it.priceEuro }
            val res = state.buyTruck(cheapest.id, "#D9051F", d.player.currentCityId, null)
            Toast.makeText(this, res.message, Toast.LENGTH_SHORT).show()
            recreate()
        }

        findViewById<Button>(R.id.btnContracts).setOnClickListener {
            val c = state.contracts().firstOrNull()
            if (c == null) {
                Toast.makeText(this, "Nicio cursă disponibilă", Toast.LENGTH_SHORT).show()
            } else {
                state.acceptContract(c.id)
                Toast.makeText(this, "Cursă acceptată", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            state.save()
            Toast.makeText(this, "Salvat", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        GameHolder.persist(this)
    }
}
