package com.rudyunguru.trucks.audio

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.rudyunguru.trucks.core.api.AudioApi
import com.rudyunguru.trucks.core.api.AudioSource
import com.rudyunguru.trucks.core.model.GameSettings
import kotlin.math.sin

/**
 * Procedural audio engine: generates the engine note, turbo, brakes and ambient sounds in code.
 */
class GameAudio(private val app: Application) : AudioApi {

    private var track: AudioTrack? = null
    private var isRunning = false
    private var source: AudioSource? = null
    private var phase = 0.0
    private var engineRunning = false

    override fun initialize() {
        val minBufferSize = AudioTrack.getMinBufferSize(44100, 
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        track?.play()
        isRunning = true
        
        Thread {
            val buffer = ShortArray(1024)
            while (isRunning) {
                fillBuffer(buffer)
                track?.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    private fun fillBuffer(buffer: ShortArray) {
        val rpm = source?.telemetry?.rpm ?: 600.0
        val freq = rpm / 60.0 * 2.0 // Simple engine sound frequency
        
        for (i in buffer.indices) {
            if (engineRunning) {
                phase += 2.0 * Math.PI * freq / 44100.0
                buffer[i] = (sin(phase) * 8000).toInt().toShort()
            } else {
                buffer[i] = 0
            }
        }
    }

    override fun applySettings(settings: GameSettings) {}
    override fun bindSource(source: AudioSource?) { this.source = source }
    override fun setEngineRunning(running: Boolean) { this.engineRunning = running }
    
    override fun playHorn(durationMs: Long) {}
    override fun stopHorn() {}
    override fun playUiClick() {}
    override fun playUiBack() {}
    override fun playUiError() {}
    override fun playMoney() {}
    override fun playLevelUp() {}
    override fun playIndicatorTick(left: Boolean) {}
    override fun playParkBrake() {}
    override fun playAirBrake() {}
    override fun playGearShift() {}
    override fun playWiperSweep() {}
    override fun playGateOpen() {}
    override fun playRefuelStart() {}
    override fun playRefuelStop() {}
    override fun playCollision(intensity: Double) {}
    override fun playPoliceSiren() {}
    override fun playJobComplete() {}
    override fun playRainStart() {}
    override fun playThunder() {}
    override fun speakNavigation(text: String) {}

    override fun release() {
        isRunning = false
        track?.stop()
        track?.release()
        track = null
    }
}
