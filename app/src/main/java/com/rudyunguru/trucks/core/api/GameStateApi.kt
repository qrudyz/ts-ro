package com.rudyunguru.trucks.core.api

import com.rudyunguru.trucks.core.model.ActiveJob
import com.rudyunguru.trucks.core.model.Company
import com.rudyunguru.trucks.core.model.Contract
import com.rudyunguru.trucks.core.model.Driver
import com.rudyunguru.trucks.core.model.GameSettings
import com.rudyunguru.trucks.core.model.Garage
import com.rudyunguru.trucks.core.model.Loan
import com.rudyunguru.trucks.core.model.OwnedTrailer
import com.rudyunguru.trucks.core.model.OwnedTruck
import com.rudyunguru.trucks.core.model.PlayerProfile
import com.rudyunguru.trucks.core.model.SaveData
import com.rudyunguru.trucks.core.model.Statistics
import com.rudyunguru.trucks.core.model.UpgradeKind

/** Outcome of a buy action (truck, trailer, garage, upgrade...). */
class PurchaseResult(
    val ok: Boolean,
    val message: String,
    val costEuro: Long = 0,
    val newInstanceId: String? = null,
)

/** Progress report produced when the player comes back after some time away. */
class OfflineReport(
    val minutesElapsed: Long,
    val driverRevenueEuro: Long,
    val salariesPaidEuro: Long,
    val loanPaymentsEuro: Long,
    val contractsExpired: Int,
    val lines: List<String>,
)

/**
 * The persistent game: economy, fleet, staff, company, contracts and settings.
 *
 * Implemented by ``com.rudyunguru.trucks.state.GameState``; the pure rules live in
 * ``com.rudyunguru.trucks.sim``. Every mutation emits an event on
 * ``com.rudyunguru.trucks.core.GameBus`` so screens can refresh themselves.
 */
interface GameStateApi {

    val bus: com.rudyunguru.trucks.core.GameBus

    fun data(): SaveData

    val settings: GameSettings

    val player: PlayerProfile

    val company: Company

    val statistics: Statistics

    fun moneyEuro(): Long

    fun trucks(): List<OwnedTruck>

    fun trailers(): List<OwnedTrailer>

    fun drivers(): List<Driver>

    fun garages(): List<Garage>

    fun loans(): List<Loan>

    fun ownedTruck(instanceId: String): OwnedTruck?

    fun ownedTrailer(instanceId: String): OwnedTrailer?

    fun driver(driverId: String): Driver?

    fun garage(cityId: String): Garage?

    // ---------------------------------------------------------------- fleet

    fun buyTruck(modelId: String, paintHex: String, cityId: String, usedListingInstanceId: String? = null): PurchaseResult

    fun sellTruck(instanceId: String): PurchaseResult

    fun repairTruck(instanceId: String): PurchaseResult

    fun refuelTruck(instanceId: String, litres: Double): PurchaseResult

    fun upgradeTruck(instanceId: String, kind: UpgradeKind): PurchaseResult

    fun setTruckPaint(instanceId: String, colorHex: String)

    fun allocateDriver(truckInstanceId: String, driverId: String?): PurchaseResult

    fun allocateTrailer(truckInstanceId: String, trailerInstanceId: String?): PurchaseResult

    fun buyTrailer(modelId: String, cityId: String): PurchaseResult

    fun sellTrailer(instanceId: String): PurchaseResult

    // ---------------------------------------------------------------- staff

    /** Drivers currently looking for a job. */
    fun candidates(): List<Driver>

    fun refreshCandidates(): List<Driver>

    fun hireDriver(driverId: String): PurchaseResult

    fun fireDriver(driverId: String)

    // ---------------------------------------------------------------- contracts

    fun contracts(): List<Contract>

    fun refreshContracts(force: Boolean = false): List<Contract>

    fun acceptContract(contractId: String): ActiveJob?

    fun assignContractToDriver(contractId: String, driverId: String): PurchaseResult

    /** True when the player's fleet can legally haul this contract. */
    fun canHaul(contract: Contract, truckInstanceId: String? = null): Boolean

    /** Applies the result of the delivery to money, stats, contract log and reputation. */
    fun completeJob(result: DeliveryResult)

    // ---------------------------------------------------------------- company

    fun createCompany(name: String, logoId: Int, colorHex: String, hqCityId: String): PurchaseResult

    fun renameCompany(name: String)

    fun buyGarage(cityId: String): PurchaseResult

    fun upgradeGarage(cityId: String): PurchaseResult

    fun takeLoan(amountEuro: Long, termDays: Int): PurchaseResult

    fun repayLoan(loanId: String): PurchaseResult

    // ---------------------------------------------------------------- time

    /** Advances the in-game clock, paying salaries, progressing driver trips and loans. */
    fun advanceMinutes(minutes: Long, emitEvents: Boolean = true)

    /** Applies the time that passed while the game was closed. */
    fun applyOfflineProgress(nowEpochMs: Long): OfflineReport

    /** Fuel price for today (varies per day and per station factor). */
    fun fuelPricePerLitre(stationFactor: Double = 1.0): Double

    // ---------------------------------------------------------------- persistence & settings

    fun save()

    fun load(): Boolean

    fun resetGame(seed: Int? = null)

    fun exportSaveJson(): String

    fun importSaveJson(json: String): Boolean

    fun updateSettings(transform: (GameSettings) -> GameSettings)

    /** Notifications produced recently, newest first (HUD toasts read this). */
    fun notifications(limit: Int = 20): List<com.rudyunguru.trucks.core.model.GameNotification>
}
