package com.rudyunguru.trucks.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rudyunguru.trucks.R
import com.rudyunguru.trucks.gl.GameGLView
import com.rudyunguru.trucks.gl.api.SceneMode
import com.rudyunguru.trucks.state.GameHolder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Driving in the Truckers of Europe 3 style: rotary steering wheel (bottom-left, drag to steer,
 * horn in the middle), brake/throttle pedals and a D/N/R style stick with +/- shifting
 * (bottom-right), speedometer bottom-center, job HUD top-left and pause button top-right.
 */
class DriveActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_placeholder)

        val session = GameHolder.sessionInternal(this)
        val state = GameHolder.stateInternal(this)

        val truck = state.trucks().firstOrNull()
        session.start(truck?.instanceId ?: "", null)
        session.setGearboxAutomatic(true)

        val gl = findViewById<GameGLView>(R.id.glView)
        gl.scene().setSceneMode(SceneMode.DRIVING)
        gl.scene().setStaticWorld(session.world.generate(1337, 20, state.settings))
        gl.scene().setEnvironment(session.environment())

        val hudSpeed = findViewById<TextView>(R.id.hudSpeed)
        val hudGear = findViewById<TextView>(R.id.hudGear)
        val hudFuel = findViewById<TextView>(R.id.hudFuel)
        val hudRemaining = findViewById<TextView>(R.id.hudRemaining)
        val wheel = findViewById<ImageView>(R.id.steeringWheel)

        // --- Steering wheel: drag around the hub; up = straight, sides = full lock -----------
        wheel.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val cx = wheel.width / 2f
                    val cy = wheel.height / 2f
                    val angle = Math.toDegrees(atan2((e.y - cy).toDouble(), (e.x - cx).toDouble()))
                    // angle: -180..180 with 0 = right; shift so straight-up = 0 steering
                    val steer = ((angle + 90.0) / 90.0).coerceIn(-1.0, 1.0)
                    session.setSteering(steer)
                    wheel.rotation = (steer * 180.0).toFloat()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    session.setSteering(0.0)
                    wheel.rotation = 0f
                    true
                }
                else -> true
            }
        }

        // --- Horn: press = sound, release = stop ---------------------------------------------
        val horn = findViewById<TextView>(R.id.btnHorn)
        horn.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { session.setHorn(true); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { session.setHorn(false); true }
                else -> true
            }.also { v.isPressed = it }
        }

        // --- Pedals: hold --------------------------------------------------------------------
        hold(findViewById(R.id.btnThrottle)) { session.setThrottle(it) }
        hold(findViewById(R.id.btnBrake)) { session.setBrake(it) }

        // --- Stick: +/- shifts, tap the gear display toggles auto/manual ----------------------
        findViewById<Button>(R.id.btnGearUp).setOnClickListener { session.shiftUp() }
        findViewById<Button>(R.id.btnGearDown).setOnClickListener { session.shiftDown() }
        hudGear.setOnClickListener {
            val wasAuto = session.telemetry.gearboxLabel != "M"
            session.setGearboxAutomatic(!wasAuto)
            hudGear.text = if (wasAuto) "M" else "D"
        }

        // --- Pause button ---------------------------------------------------------------------
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            session.end()
            GameHolder.persist(this)
            finish()
        }

        // --- Simulation loop -------------------------------------------------------------------
        running = true
        executor.execute {
            var last = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = TimeUnit.NANOSECONDS.toMicros(now - last) / 1_000_000.0
                last = now
                session.update(dt.coerceIn(0.001, 0.1))
                gl.scene().setPlayerVehicle(session.playerPose())

                val t = session.telemetry
                runOnUiThread {
                    if (!running) return@runOnUiThread
                    hudSpeed.text = "${t.speedKmh.roundToInt()} km/h"
                    hudGear.text = t.gearboxLabel + if (t.gearboxLabel == "M") t.gear.toString() else ""
                    hudFuel.text = "⛽ ${t.fuelPercent.roundToInt()}%"
                    hudRemaining.text =
                        if (t.remainingKm > 0) String.format("%.1f km", t.remainingKm) else "Liber"
                }
                Thread.sleep(16)
            }
        }
    }

    /** Turns a Button into a press-and-hold analog control (0..1 while pressed). */
    private fun hold(button: View, setter: (Double) -> Unit) {
        button.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; setter(1.0); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; setter(0.0); true }
                else -> true
            }
        }
    }

    override fun onDestroy() {
        running = false
        executor.shutdown()
        GameHolder.sessionInternal(this).end()
        super.onDestroy()
    }

    override fun onStop() {
        GameHolder.persist(this)
        super.onStop()
    }
}
