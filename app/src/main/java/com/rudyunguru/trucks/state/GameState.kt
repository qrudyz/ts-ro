package com.rudyunguru.trucks.state

import android.app.Application
import com.rudyunguru.trucks.core.GameBus
import com.rudyunguru.trucks.core.Rng
import com.rudyunguru.trucks.core.api.DeliveryResult
import com.rudyunguru.trucks.core.api.GameStateApi
import com.rudyunguru.trucks.core.api.OfflineReport
import com.rudyunguru.trucks.core.api.PurchaseResult
import com.rudyunguru.trucks.core.catalog.TruckCatalog
import com.rudyunguru.trucks.core.model.*
import com.rudyunguru.trucks.sim.SimFacade

/**
 * Global game state: money, fleet, drivers, settings.
 */
class GameState(private val app: Application) : GameStateApi {

    override val bus = GameBus()
    private val saveManager = SaveManager(app)
    private var data = SaveData()
    var isLoaded = false
        private set

    init { load() }

    override fun data(): SaveData = data
    override val settings: GameSettings get() = data.settings
    override val player: PlayerProfile get() = data.player
    override val company: Company get() = data.company
    override val statistics: Statistics get() = data.statistics

    override fun moneyEuro(): Long = data.player.moneyEuro
    override fun trucks(): List<OwnedTruck> = data.trucks
    override fun trailers(): List<OwnedTrailer> = data.trailers
    override fun drivers(): List<Driver> = data.drivers
    override fun garages(): List<Garage> = data.garages
    override fun loans(): List<Loan> = data.loans

    override fun ownedTruck(instanceId: String) = data.trucks.find { it.instanceId == instanceId }
    override fun ownedTrailer(instanceId: String) = data.trailers.find { it.instanceId == instanceId }
    override fun driver(driverId: String) = data.drivers.find { it.id == driverId }
    override fun garage(cityId: String) = data.garages.find { it.cityId == cityId }

    override fun buyTruck(modelId: String, paintHex: String, cityId: String, usedListingInstanceId: String?): PurchaseResult {
        val model = TruckCatalog.byId(modelId)
        if (data.player.moneyEuro < model.priceEuro) return PurchaseResult(false, "Fonduri insuficiente")
        val newTruck = OwnedTruck(
            instanceId = "truck_${System.currentTimeMillis()}",
            modelId = modelId, plate = "B 01 RTS", paintHex = paintHex,
            garageCityId = cityId, fuelL = model.fuelTankL * 0.5
        )
        data = data.copy(
            player = data.player.copy(moneyEuro = data.player.moneyEuro - model.priceEuro),
            trucks = data.trucks + newTruck
        )
        save(); return PurchaseResult(true, "Camion cumpărat!", 0, newTruck.instanceId)
    }

    override fun sellTruck(instanceId: String): PurchaseResult = PurchaseResult(true, "Vândut")
    override fun repairTruck(instanceId: String): PurchaseResult = PurchaseResult(true, "Reparat")
    override fun refuelTruck(instanceId: String, litres: Double): PurchaseResult = PurchaseResult(true, "Alimentat")
    override fun upgradeTruck(instanceId: String, kind: UpgradeKind): PurchaseResult = PurchaseResult(true, "Upgrade")
    override fun setTruckPaint(instanceId: String, colorHex: String) {}
    override fun allocateDriver(truckInstanceId: String, driverId: String?): PurchaseResult = PurchaseResult(true, "Alocat")
    override fun allocateTrailer(truckInstanceId: String, trailerInstanceId: String?): PurchaseResult = PurchaseResult(true, "Alocat")
    override fun buyTrailer(modelId: String, cityId: String): PurchaseResult = PurchaseResult(true, "Cumpărat")
    override fun sellTrailer(instanceId: String): PurchaseResult = PurchaseResult(true, "Vândut")

    override fun candidates(): List<Driver> = SimFacade.generateCandidates(Rng(), 5, data.day(), data.player.reputation)
    override fun refreshCandidates(): List<Driver> = candidates()
    override fun hireDriver(driverId: String): PurchaseResult = PurchaseResult(true, "Angajat")
    override fun fireDriver(driverId: String) {}

    override fun contracts(): List<Contract> = SimFacade.generateContracts(Rng(), data.day(), 10, data.player.reputation, data.player.currentCityId)
    override fun refreshContracts(force: Boolean): List<Contract> = contracts()
    override fun acceptContract(contractId: String): ActiveJob? {
        val c = contracts().find { it.id == contractId } ?: return null
        return ActiveJob(c, data.day(), data.minutesOfDay(), c.deadlineMinutes)
    }
    override fun assignContractToDriver(contractId: String, driverId: String): PurchaseResult = PurchaseResult(true, "Asignat")
    override fun canHaul(contract: Contract, truckInstanceId: String?): Boolean = true
    override fun completeJob(result: DeliveryResult) {
        data = data.copy(player = data.player.copy(moneyEuro = data.player.moneyEuro + result.netEuro))
        save()
    }

    override fun createCompany(name: String, logoId: Int, colorHex: String, hqCityId: String): PurchaseResult = PurchaseResult(true, "Creată")
    override fun renameCompany(name: String) {}
    override fun buyGarage(cityId: String): PurchaseResult = PurchaseResult(true, "Cumpărat")
    override fun upgradeGarage(cityId: String): PurchaseResult = PurchaseResult(true, "Upgradat")
    override fun takeLoan(amountEuro: Long, termDays: Int): PurchaseResult = PurchaseResult(true, "Acordat")
    override fun repayLoan(loanId: String): PurchaseResult = PurchaseResult(true, "Rambursat")

    override fun advanceMinutes(minutes: Long, emitEvents: Boolean) { data = data.copy(totalMinutes = data.totalMinutes + minutes) }
    override fun applyOfflineProgress(nowEpochMs: Long): OfflineReport = OfflineReport(0, 0, 0, 0, 0, emptyList())
    override fun fuelPricePerLitre(stationFactor: Double): Double = data.fuelPricePerLitre * stationFactor

    override fun save() { saveManager.save(data) }
    override fun load(): Boolean {
        data = saveManager.load() ?: SaveData(seed = 1337)
        isLoaded = true
        return true
    }
    override fun resetGame(seed: Int?) { data = SaveData(seed = seed ?: 1337); save() }
    override fun exportSaveJson(): String = saveManager.exportJson(data)
    override fun importSaveJson(json: String): Boolean {
        data = saveManager.importJson(json) ?: return false
        save(); return true
    }
    override fun updateSettings(transform: (GameSettings) -> GameSettings) { data = data.copy(settings = transform(data.settings)); save() }
    override fun notifications(limit: Int): List<GameNotification> = data.notifications.take(limit)
}
