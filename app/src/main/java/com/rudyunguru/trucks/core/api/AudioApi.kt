package com.rudyunguru.trucks.core.api

import com.rudyunguru.trucks.core.model.GameSettings

/**
 * Procedural audio engine (no shipped sound files): engine note follows RPM/load, plus turbo whistle,
 * brakes, tyre slip, rain, wind, horn, indicators, wipers, UI feedback and voice navigation.
 *
 * Implemented by ``com.rudyunguru.trucks.audio.GameAudio``.
 */
interface AudioApi {

    /** Creates the audio pipeline. Safe to call from the UI thread. */
    fun initialize()

    /** Applies volume levels / mute flags from the settings. */
    fun applySettings(settings: GameSettings)

    /** Binds a live source for the engine loop (called when the driving session starts). */
    fun bindSource(source: AudioSource?)

    /** Starts/stops the engine loop without tearing down the audio track. */
    fun setEngineRunning(running: Boolean)

    /** Momentary sounds, all optional and safe to spam (they are throttled internally). */
    fun playHorn(durationMs: Long = 600)
    fun stopHorn()
    fun playUiClick()
    fun playUiBack()
    fun playUiError()
    fun playMoney()
    fun playLevelUp()
    fun playIndicatorTick(left: Boolean)
    fun playParkBrake()
    fun playAirBrake()
    fun playGearShift()
    fun playWiperSweep()
    fun playGateOpen()
    fun playRefuelStart()
    fun playRefuelStop()
    fun playCollision(intensity: Double)
    fun playPoliceSiren()
    fun playJobComplete()
    fun playRainStart()
    fun playThunder()

    /** Short spoken navigation prompt, e.g. "În 500 de metri, virați la dreapta." */
    fun speakNavigation(text: String)

    /** Releases every audio resource; called when the app goes to the background. */
    fun release()
}
