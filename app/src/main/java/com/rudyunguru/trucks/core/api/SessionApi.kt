package com.rudyunguru.trucks.core.api

import com.rudyunguru.trucks.core.model.ActiveJob
import com.rudyunguru.trucks.core.model.CameraPreset
import com.rudyunguru.trucks.gl.api.EnvironmentState
import com.rudyunguru.trucks.gl.api.VehiclePose

/** Result of a delivery, produced when the truck reaches the destination gate. */
class DeliveryResult(
    val contractId: String,
    val cargo: String,
    val payoutEuro: Long,
    val damagePenaltyEuro: Long,
    val latePenaltyEuro: Long,
    val finesEuro: Long,
    val xpGained: Int,
    val reputationGained: Int,
    val perfect: Boolean,
    val kilometers: Double,
) {
    val netEuro: Long get() = (payoutEuro - damagePenaltyEuro - latePenaltyEuro - finesEuro).coerceAtLeast(0)

    fun summaryRo(): String = buildString {
        append("Livrare finalizată: +${com.rudyunguru.trucks.core.Format.money(netEuro)}")
        if (perfect) append(" • livrare perfectă")
        if (damagePenaltyEuro > 0) append(" • marfă deteriorată -${com.rudyunguru.trucks.core.Format.money(damagePenaltyEuro)}")
        if (latePenaltyEuro > 0) append(" • întârziere -${com.rudyunguru.trucks.core.Format.money(latePenaltyEuro)}")
        if (finesEuro > 0) append(" • amenzi -${com.rudyunguru.trucks.core.Format.money(finesEuro)}")
    }
}

/**
 * One driving session: physics in, visuals + HUD telemetry out.
 *
 * Implemented by ``com.rudyunguru.trucks.state.GameSession``. The UI drives it:
 *
 * ```
 *   session.start(truckId, job)
 *   session.setThrottle(0.6)          // from the touch controls
 *   session.update(dtSeconds)          // from the GL frame callback
 *   hud.render(session.telemetry)
 * ```
 */
interface SessionApi {

    val telemetry: Telemetry

    /** Live world queries (GPS, speed limits, weather, traffic). */
    val world: WorldApi

    /** Session is running: physics advancing and the environment updating. */
    val isRunning: Boolean

    /** Truck instance currently being driven, or null in free-roam-without-truck previews. */
    val truckInstanceId: String?

    /** Contract being driven, or null when free roaming. */
    val activeJob: ActiveJob?

    fun start(truckInstanceId: String, job: ActiveJob?)

    fun end()

    fun pause()

    fun resume()

    /** Advances physics + world by one frame. Called by the renderer's frame callback. */
    fun update(dtSeconds: Double)

    // ---------------------------------------------------------------- controls

    fun setThrottle(value: Double)
    fun setBrake(value: Double)
    /** -1 = full left, +1 = full right. The session applies the settings sensitivity/dead zone. */
    fun setSteering(value: Double)
    fun setHandbrake(on: Boolean)
    fun setRetarder(level: Int)
    fun toggleEngineBrake()
    fun setCruiseControl(speedKmh: Double)
    fun shiftUp()
    fun shiftDown()
    fun setGearboxAutomatic(automatic: Boolean)
    fun toggleHeadlights()
    fun toggleHighBeam()
    fun toggleHazards()
    fun setIndicator(left: Boolean, right: Boolean)
    fun toggleWipers()
    fun setHorn(on: Boolean)
    fun cycleCamera()
    fun setCamera(preset: CameraPreset)

    // ---------------------------------------------------------------- interactions

    /** Cost of the last refuel in euro; 0 when nothing was bought. */
    fun refuel(litres: Double): Long

    fun repair(): Long

    fun deliver(): DeliveryResult?

    fun abandonJob()

    /** Puts the truck back in a garage and charges a fee (relocation / tow). */
    fun fastTravelTo(cityId: String): Long

    // ---------------------------------------------------------------- renderer bridge

    fun playerPose(): VehiclePose

    fun environment(): EnvironmentState

    /** Current route progress 0..1 (0 when free roaming). */
    fun routeProgress(): Double
}
