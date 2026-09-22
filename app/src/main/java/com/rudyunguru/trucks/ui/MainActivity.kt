package com.rudyunguru.trucks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rudyunguru.trucks.R
import com.rudyunguru.trucks.core.Format
import com.rudyunguru.trucks.core.catalog.CityCatalog
import com.rudyunguru.trucks.core.catalog.TruckCatalog
import com.rudyunguru.trucks.state.GameHolder

/**
 * TOE3-style dealer screen: the 3D garage with the truck on a rotating platform fills the whole
 * screen; the shop UI is a slim panel on the right side. The whole activity is landscape, like
 * the driving session, so the game never rotates.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_placeholder)

        val state = GameHolder.stateInternal(this)
        val d = state.data()

        findViewById<TextView>(R.id.titleCompany).text =
            d.company.name.ifEmpty { getString(R.string.app_name) }
        findViewById<TextView>(R.id.labelMoney).text = Format.money(d.player.moneyEuro)
        findViewById<TextView>(R.id.labelFleet).text =
            getString(R.string.label_trucks) + ": " + d.trucks.size

        // --- The truck standing in the garage -------------------------------------------------
        val gl = findViewById<com.rudyunguru.trucks.gl.GameGLView>(R.id.glView)
        val owned = state.trucks().firstOrNull()
        val shown = owned?.let { TruckCatalog.byId(it.modelId) } ?: TruckCatalog.TRUCKS.minBy { it.priceEuro }
        gl.scene().setSceneMode(com.rudyunguru.trucks.gl.api.SceneMode.GARAGE)
        gl.scene().setShowroomVehicle(
            shown.meshVariant, 0,
            owned?.paintHex ?: shown.paintOptions.firstOrNull() ?: "#D9051F", "", true,
        )

        val city = CityCatalog.byId(d.player.currentCityId)
        findViewById<TextView>(R.id.labelGarage).text = city.name

        // --- Actions ---------------------------------------------------------------------------
        findViewById<Button>(R.id.btnDrive).setOnClickListener {
            val truck = state.trucks().firstOrNull()
            if (truck == null) {
                Toast.makeText(this, "Cumpără mai întâi un camion", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, DriveActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnNewTruck).setOnClickListener {
            val cheapest = TruckCatalog.TRUCKS.minBy { it.priceEuro }
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
