package com.rudyunguru.trucks.state

import android.app.Application
import android.content.Context
import com.rudyunguru.trucks.audio.GameAudio
import com.rudyunguru.trucks.core.GameBus
import com.rudyunguru.trucks.core.api.AudioApi
import com.rudyunguru.trucks.core.api.GameStateApi
import com.rudyunguru.trucks.core.api.SessionApi

/**
 * Process-wide access point for the three long-lived services of the game:
 * the persistent [GameStateApi], the driving [SessionApi] and the procedural [AudioApi].
 *
 * The heavy objects are created lazily on first use and kept alive for the whole process so switching
 * between menu, showroom and driving never reloads the save file or rebuilds the renderer. Screens
 * must only ever obtain them through this holder - that is what makes the whole UI testable and keeps
 * a single source of truth for money, fleet and settings.
 */
object GameHolder {

    @Volatile
    private var application: Application? = null

    @Volatile
    private var gameState: GameState? = null

    @Volatile
    private var gameSession: GameSession? = null

    @Volatile
    private var gameAudio: GameAudio? = null

    /** Called once from [com.rudyunguru.trucks.RomaniaTruckApp.onCreate]. */
    fun install(app: Application) {
        application = app
    }

    private fun app(context: Context): Application {
        val installed = application
        if (installed != null) return installed
        val resolved = context.applicationContext as Application
        application = resolved
        return resolved
    }

    fun state(context: Context): GameStateApi = stateInternal(context)

    fun session(context: Context): SessionApi = sessionInternal(context)

    fun audio(context: Context): AudioApi = audioInternal(context)

    fun bus(context: Context): GameBus = stateInternal(context).bus

    /** Typed access for the menu/driving code that needs the concrete classes (e.g. quality mapping). */
    fun stateInternal(context: Context): GameState {
        gameState?.let { return it }
        return synchronized(this) {
            gameState ?: GameState(app(context)).also { gameState = it }
        }
    }

    fun sessionInternal(context: Context): GameSession {
        gameSession?.let { return it }
        return synchronized(this) {
            gameSession ?: GameSession(
                state = stateInternal(context),
                audio = audioInternal(context),
            ).also { gameSession = it }
        }
    }

    fun audioInternal(context: Context): GameAudio {
        gameAudio?.let { return it }
        return synchronized(this) {
            gameAudio ?: GameAudio(app(context)).also { gameAudio = it }
        }
    }

    /** True once the save file has been read (the splash screen waits for this). */
    fun isReady(): Boolean = gameState?.isLoaded == true

    /** Persists the current progress. Called on every screen change and on lifecycle stop. */
    fun persist(context: Context) {
        gameState?.save()
    }

    /** Releases audio + session resources; called when the app is destroyed. */
    fun release() {
        runCatching { gameSession?.end() }
        runCatching { gameAudio?.release() }
        gameSession = null
    }
}
