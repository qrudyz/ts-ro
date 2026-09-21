package com.rudyunguru.trucks.core.model

/**
 * Root of the persisted game. Serialized by [com.rudyunguru.trucks.state.SaveManager] with the tiny
 * JSON writer in [com.rudyunguru.trucks.core.Json] so saving works identically on device and in JVM
 * unit tests.
 */
data class SaveData(
    val version: Int = CURRENT_VERSION,
    val seed: Int = 20240613,
    val createdEpochMs: Long = 0L,
    val lastPlayedEpochMs: Long = 0L,
    /** In-game minutes since day 0 (day 0 = the day the company was started). */
    val totalMinutes: Long = 8L * 60L,
    val player: PlayerProfile = PlayerProfile(),
    val company: Company = Company(),
    val trucks: List<OwnedTruck> = emptyList(),
    val trailers: List<OwnedTrailer> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val garages: List<Garage> = emptyList(),
    /** Contracts currently offered on the market. */
    val contracts: List<Contract> = emptyList(),
    val activeJob: ActiveJob? = null,
    val loans: List<Loan> = emptyList(),
    val usedTruckListings: List<UsedTruckListing> = emptyList(),
    val statistics: Statistics = Statistics(),
    val settings: GameSettings = GameSettings(),
    val notifications: List<GameNotification> = emptyList(),
    /** Fuel price varies from day to day; both values are persisted so prices stay stable. */
    val fuelPricePerLitre: Double = 1.62,
    val fuelPriceUpdatedDay: Int = 0,
    val unlockedCityIds: List<String> = listOf("bucuresti"),
    val firstRunComplete: Boolean = false,
    /** Last screen the player was on, used by "Continuă". */
    val lastScreen: String = "home",
) {
    fun day(): Int = (totalMinutes / 1440L).toInt()

    fun minutesOfDay(): Long = totalMinutes % 1440L

    companion object {
        const val CURRENT_VERSION = 1
        const val PREFS_NAME = "romania_truck_save"
        const val FILE_NAME = "savegame.json"
    }
}
