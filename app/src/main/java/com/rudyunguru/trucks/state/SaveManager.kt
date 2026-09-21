package com.rudyunguru.trucks.state

import android.content.Context
import com.rudyunguru.trucks.core.Json
import com.rudyunguru.trucks.core.Json.long
import com.rudyunguru.trucks.core.model.SaveData
import java.io.File

/**
 * Persistence for the save game.
 *
 * Two copies are written: the JSON file inside the app's private storage (portable, exportable,
 * debuggable) and a copy in SharedPreferences so a corrupted file can always be recovered from the
 * last known good state. The format is plain JSON produced by [com.rudyunguru.trucks.core.Json].
 */
class SaveManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(SaveData.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val file: File
        get() = File(context.filesDir, SaveData.FILE_NAME)

    fun exists(): Boolean = file.exists() || prefs.contains(KEY_BACKUP)

    /** Loads the save, preferring the newest usable copy. Returns null on a fresh install. */
    fun load(): SaveData? {
        val fromFile = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
        val decoded = fromFile?.let { SaveCodec.decode(it) }
        if (decoded != null) return decoded
        val fromPrefs = prefs.getString(KEY_BACKUP, null)
        return fromPrefs?.let { SaveCodec.decode(it) }
    }

    /** Writes the save to disk. Never throws: a failed save must not crash the game. */
    fun save(data: SaveData): Boolean {
        val json = SaveCodec.encode(data)
        prefs.edit().putString(KEY_BACKUP, json).apply()
        return runCatching {
            val tmp = File(context.filesDir, "${SaveData.FILE_NAME}.tmp")
            tmp.writeText(json)
            if (file.exists()) file.delete()
            tmp.renameTo(file)
            true
        }.getOrDefault(false)
    }

    fun delete() {
        runCatching { file.delete() }
        prefs.edit().remove(KEY_BACKUP).apply()
    }

    /** Export/import used by the settings screen ("copiază salvarea"). */
    fun exportJson(data: SaveData): String = SaveCodec.encode(data)

    fun importJson(json: String): SaveData? = SaveCodec.decode(json)

    fun lastPlayedEpochMs(): Long = runCatching {
        Json.parseObject(file.readText())?.long("lastPlayedEpochMs", 0L) ?: 0L
    }.getOrDefault(0L)

    private companion object {
        const val KEY_BACKUP = "save_backup_json"
    }
}
