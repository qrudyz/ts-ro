package com.rudyunguru.trucks.sim

import com.rudyunguru.trucks.core.Format
import com.rudyunguru.trucks.core.Mathx
import com.rudyunguru.trucks.core.Rng
import com.rudyunguru.trucks.core.catalog.CityCatalog
import com.rudyunguru.trucks.core.catalog.TruckCatalog
import com.rudyunguru.trucks.core.catalog.TrailerCatalog
import com.rudyunguru.trucks.core.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The complete rule set of the game: economy, freight market, staff, fleet operations, wear, fuel
 * prices and traffic fines.
 *
 * Deliberately pure Kotlin with no Android dependency and no I/O so that every rule can be unit
 * tested, and so that the same functions can run the live game or a fast-forward simulation of the
 * player's fleet while the game is closed. All money is `Long` euro, distances are `Double` real
 * kilometres (the ones the player sees) and durations are `Long` in-game minutes.
 */
object SimFacade {

    // ================================================================ economy

    /** Daily fuel price: a slow random walk around a national average, in euro per litre. */
    fun nextFuelPrice(day: Int, previous: Double, rng: Rng): Double {
        val seasonal = 0.06 * Math.sin(day / 21.0)
        val drift = rng.range(-0.035, 0.035)
        val target = 1.58 + seasonal
        val blended = previous + (target - previous) * 0.25 + drift
        return Mathx.round(blended.coerceIn(1.24, 2.15), 3)
    }

    /** Price at a specific station: city stations and motorway stations differ. */
    fun stationPrice(basePrice: Double, factor: Double): Double = Mathx.round(basePrice * factor, 3)

    fun refuelCost(litres: Double, pricePerLitre: Double): Long =
        max(0L, (litres * pricePerLitre).roundToLong())

    fun dailySalary(driver: Driver): Long = driver.dailySalary()

    /** Salaries owed for a period of in-game minutes. */
    fun salariesDue(drivers: List<Driver>, minutes: Long): Long {
        if (minutes <= 0) return 0
        var total = 0.0
        for (d in drivers) total += dailySalary(d) * (minutes.toDouble() / 1440.0)
        return total.roundToLong()
    }

    /** Adds one day of interest and pays as much of the instalment as the balance allows. */
    fun advanceLoan(loan: Loan, availableMoney: Long): Pair<Loan, Long> {
        if (loan.isPaidOff()) return loan to 0L
        val interest = (loan.remainingEuro * loan.dailyInterestRate).roundToLong()
        val owed = loan.dailyPaymentEuro + interest
        val paid = min(owed, max(0L, availableMoney))
        val remaining = (loan.remainingEuro + interest - paid).coerceAtLeast(0)
        return loan.copy(remainingEuro = remaining, daysPaid = loan.daysPaid + 1) to paid
    }

    /** Loan offer for a given amount; longer terms cost more interest. */
    fun loanOffer(amountEuro: Long, termDays: Int, day: Int): Loan {
        val annualRate = when {
            amountEuro <= 50_000 -> 0.089
            amountEuro <= 150_000 -> 0.098
            else -> 0.107
        }
        val dailyRate = annualRate / 365.0
        val total = amountEuro + amountEuro * dailyRate * termDays
        return Loan(
            id = "loan_${day}_$amountEuro",
            principalEuro = amountEuro,
            remainingEuro = amountEuro,
            dailyPaymentEuro = (total / termDays).roundToLong(),
            dailyInterestRate = dailyRate,
            takenDay = day,
            termDays = termDays,
            description = "Credit ${Format.money(amountEuro)} pe $termDays zile",
        )
    }

    fun xpForNextLevel(level: Int): Int = 900 * level

    /** Reputation delta of a delivery. */
    fun reputationDelta(perfect: Boolean, late: Boolean, payoutEuro: Long): Int {
        if (late) return -1
        val base = if (perfect) 2 else 1
        return base + if (payoutEuro > 4_000) 1 else 0
    }

    // ================================================================ freight market

    /**
     * Builds a fresh freight market for the player's reputation level.
     *
     * Offers are weighted towards the home region so the first jobs are reachable with the starter
     * MAN TGL, while heavy/oversize cargo only appears once the player has a reputation and a fleet.
     */
    fun generateContracts(
        rng: Rng,
        day: Int,
        count: Int,
        reputation: Int,
        homeCityId: String,
        unlockedCityIds: List<String> = CityCatalog.CITIES.map { it.id },
    ): List<Contract> {
        val home = CityCatalog.byId(homeCityId)
        val access = 0.35 + reputation / 100.0 * 0.65
        val offers = ArrayList<Contract>(count)
        var guard = 0
        while (offers.size < count && guard < count * 12) {
            guard++
            val cargo = rng.weighted(CargoKind.entries.toList(), CargoKind.entries.map { cargoWeight(it, access) })
            val from = pickShipper(rng, cargo, home, unlockedCityIds) ?: continue
            val to = pickReceiver(rng, cargo, from, unlockedCityIds) ?: continue
            if (from.id == to.id) continue
            val straightKm = from.distanceKmTo(to)
            if (straightKm < 40) continue

            val roadKm = straightKm * rng.range(1.12, 1.34)
            val lightJob = roadKm < 260 && rng.bool(0.55)
            val weight = if (lightJob) rng.range(cargo.minTons * 0.5, min(cargo.maxTons, 14.0))
            else rng.range(cargo.minTons, cargo.maxTons)
            val urgency = rng.range(0.15, 0.85)
            val payout = (roadKm * 1.55 * payoutMultiplier(cargo, weight, urgency)).roundToLong()
                .coerceAtLeast(220)

            val truckClass = when {
                roadKm < 260 && weight <= 13.5 -> TruckClass.STARTER
                weight > 26 || cargo == CargoKind.MACHINERY -> TruckClass.FLAGSHIP
                weight > 22 -> TruckClass.HEAVY
                else -> TruckClass.MEDIUM
            }
            val averageSpeed = when {
                roadKm < 120 -> 46.0
                roadKm < 300 -> 58.0
                else -> 68.0
            }
            val minimumMinutes = (roadKm / averageSpeed * 60).roundToLong()
            val deadline = (minimumMinutes * (1.35 + (1 - urgency) * 0.85)).roundToLong().coerceAtLeast(45)

            offers += Contract(
                id = "contract_${day}_${offers.size}_${rng.int(1000, 9999)}",
                cargo = cargo,
                fromCityId = from.id,
                toCityId = to.id,
                weightTons = Mathx.round(weight, 1),
                distanceKm = Mathx.round(roadKm, 1),
                payoutEuro = payout,
                deadlineMinutes = deadline,
                recommendedTruckClass = truckClass,
                requiredTrailerKind = TrailerCatalog.requiredKind(cargo),
                reputationRequirement = reputationRequirement(cargo, roadKm),
                urgency = Mathx.round(urgency, 2),
                postedDay = day,
                expiresDay = day + rng.int(1, 3),
                shipper = shipperName(rng, from),
                penaltyEuro = (payout * 0.22).roundToLong(),
            )
        }
        return offers.sortedByDescending { it.payoutEuro }
    }

    /**
     * Used trucks offered by the dealer. The starter model is always listed and affordable with the
     * starting balance, so a fresh player can buy a first truck without taking a loan.
     */
    fun generateUsedListings(rng: Rng, day: Int, count: Int, reputation: Int): List<UsedTruckListing> {
        val listings = ArrayList<UsedTruckListing>(count)
        val starter = TruckCatalog.byId(TruckCatalog.STARTER_ID)
        listings += UsedTruckListing(
            instanceId = "used_starter_$day",
            modelId = starter.id,
            priceEuro = rng.int(36_000, 43_000).toLong(),
            km = rng.range(180_000.0, 420_000.0),
            conditionPercent = rng.range(62.0, 78.0),
            plate = Format.plate(day * 31 + 7),
            paintHex = rng.pick(starter.paintOptions),
        )
        val affordable = TruckCatalog.TRUCKS.filter { it.truckClass != TruckClass.FLAGSHIP }
        repeat((count - 1).coerceAtLeast(0)) {
            val model = rng.weighted(
                affordable,
                affordable.map { if (it.truckClass == TruckClass.STARTER) 0.6 else 1.0 },
            )
            val condition = rng.range(48.0, 92.0)
            val ageFactor = 1.0 - condition / 100.0 * 0.55
            val premium = 1.0 + reputation / 100.0 * 0.08
            listings += UsedTruckListing(
                instanceId = "used_${day}_${listings.size}_${rng.int(100, 999)}",
                modelId = model.id,
                priceEuro = (model.priceEuro * ageFactor * premium).roundToLong().coerceAtLeast(8_000),
                km = rng.range(60_000.0, 240_000.0 + 900_000.0 * (1 - condition / 100.0)),
                conditionPercent = Mathx.round(condition, 1),
                plate = Format.plate(rng.int(1, 9_999)),
                paintHex = rng.pick(model.paintOptions),
            )
        }
        return listings.sortedBy { it.priceEuro }
    }

    private fun cargoWeight(cargo: CargoKind, access: Double): Double = when (cargo) {
        CargoKind.FOOD -> 1.15
        CargoKind.CONSTRUCTION -> 1.05
        CargoKind.CONTAINERS -> 0.95 + access * 0.4
        CargoKind.REFRIGERATED -> 0.85 + access * 0.35
        CargoKind.MACHINERY -> 0.5 + access * 0.8
        CargoKind.VEHICLES -> 0.9
        CargoKind.GRAIN -> 0.9
        CargoKind.TIMBER -> 0.85
    }

    private fun payoutMultiplier(cargo: CargoKind, tons: Double, urgency: Double): Double {
        val cargoFactor = when (cargo) {
            CargoKind.FOOD -> 0.98
            CargoKind.CONSTRUCTION -> 0.92
            CargoKind.CONTAINERS -> 1.05
            CargoKind.REFRIGERATED -> 1.22
            CargoKind.MACHINERY -> 1.35
            CargoKind.VEHICLES -> 1.18
            CargoKind.GRAIN -> 0.90
            CargoKind.TIMBER -> 1.02
        }
        return cargoFactor * (0.85 + tons / 40.0) * (0.92 + urgency * 0.28)
    }

    private fun reputationRequirement(cargo: CargoKind, roadKm: Double): Int = when {
        roadKm > 420 -> 20
        cargo == CargoKind.MACHINERY -> 35
        cargo == CargoKind.REFRIGERATED -> 15
        roadKm > 260 -> 8
        else -> 0
    }

    private fun pickShipper(rng: Rng, cargo: CargoKind, home: City, unlocked: List<String>): City? {
        val suppliers = CityCatalog.CITIES.filter { city ->
            unlocked.contains(city.id) && city.depots.any { it.cargo.contains(cargo) }
        }
        if (suppliers.isEmpty()) return null
        // roughly half of the offers start near the player's home city
        if (rng.bool(0.45)) {
            val nearby = suppliers.filter { it.id == home.id || it.distanceKmTo(home) < 180 }
            if (nearby.isNotEmpty()) return rng.pick(nearby)
        }
        return rng.pick(suppliers)
    }

    private fun pickReceiver(rng: Rng, cargo: CargoKind, from: City, unlocked: List<String>): City? {
        val candidates = CityCatalog.CITIES.filter { city ->
            unlocked.contains(city.id) && city.id != from.id
        }
        if (candidates.isEmpty()) return null
        val weights = candidates.map { city ->
            val buys = city.depots.count { it.cargo.contains(cargo) }
            val distancePenalty = 1.0 / (1.0 + city.distanceKmTo(from) / 900.0)
            (1.0 + buys * 1.2) * distancePenalty
        }
        return rng.weighted(candidates, weights)
    }

    private fun shipperName(rng: Rng, city: City): String {
        val prefix = rng.pick(
            listOf("Trans", "Rapid", "Cargo", "Logistic", "Frigo", "Agro", "Metal", "Danub", "Carp", "Rutier"),
        )
        val suffix = rng.pick(listOf("SRL", "SA", "Logistics", "Group", "Transport", "Com"))
        return "$prefix ${city.name.take(4)} $suffix"
    }

    // ================================================================ staff

    private val FIRST_NAMES = listOf(
        "Andrei", "Mihai", "Cristian", "Vlad", "Ion", "Alexandru", "Gabriel", "Florin", "Radu", "Bogdan",
        "Elena", "Ioana", "Maria", "Andreea", "Dana", "Roxana", "Cătălin", "Ștefan", "Marius", "Dumitru",
        "Lucian", "Nicolae", "Adrian", "Paul", "Sorin", "Victor", "Alina", "Bianca", "Cosmin", "Ovidiu",
    )
    private val LAST_NAMES = listOf(
        "Popescu", "Ionescu", "Popa", "Radu", "Dumitru", "Stoica", "Stan", "Gheorghe", "Constantin",
        "Marin", "Barbu", "Nistor", "Neagu", "Iliescu", "Sava", "Toma", "Vasile", "Voicu", "Zamfir",
        "Munteanu", "Moldovan", "Ardelean", "Bălan", "Cristea", "Lungu", "Oprea", "Petrescu", "Rusu",
    )

    /** Driver candidates looking for work; better candidates appear as the player's reputation grows. */
    fun generateCandidates(rng: Rng, count: Int, day: Int, reputation: Int): List<Driver> {
        val quality = reputation / 100.0
        return List(count) { index ->
            val levelRoll = rng.nextDouble() + quality * 0.85
            val level = when {
                levelRoll > 1.25 -> DriverLevel.EXPERIENCED
                levelRoll > 0.92 -> DriverLevel.DRIVER
                else -> DriverLevel.BEGINNER
            }
            Driver(
                id = "driver_${day}_${index}_${rng.int(1000, 9999)}",
                name = "${rng.pick(FIRST_NAMES)} ${rng.pick(LAST_NAMES)}",
                avatarIndex = rng.int(0, 11),
                age = rng.int(23, 61),
                cityId = rng.pick(CityCatalog.CITIES).id,
                level = level,
                xp = level.xpRequired + rng.int(0, 400),
                skill = (25 + level.ordinal * 12 + rng.int(0, 18)).coerceAtMost(100),
                baseSalaryPerDay = rng.int(78, 132).toLong() + level.ordinal * 26L,
                fuelEconomy = Mathx.round(1.14 - level.ordinal * 0.035 + rng.range(-0.04, 0.04), 3),
                accidentRate = Mathx.round(level.accidentRate * rng.range(0.75, 1.35), 4),
                profitability = Mathx.round(0.82 + level.ordinal * 0.035 + rng.range(0.0, 0.08), 3),
            )
        }
    }

    /** Plans a trip for an employed driver: duration, fuel cost and the payout they will bring in. */
    fun planDriverTrip(
        driver: Driver,
        contract: Contract,
        model: TruckModel,
        day: Int,
        minuteOfDay: Long,
    ): DriverRoute {
        val skillFactor = 1.0 - driver.effectiveSkill() / 400.0
        val averageSpeed = 62.0 * skillFactor
        val totalMinutes = ((contract.distanceKm / averageSpeed) * 60).roundToLong().coerceAtLeast(30)
        val consumption = model.baseConsumptionL100 * (0.9 + contract.weightTons / 40.0) * driver.fuelEconomy
        val litres = consumption * contract.distanceKm / 100.0
        return DriverRoute(
            contractId = contract.id,
            fromCityId = contract.fromCityId,
            toCityId = contract.toCityId,
            startedDay = day,
            startedMinutes = minuteOfDay,
            totalMinutes = totalMinutes,
            payoutEuro = (contract.payoutEuro * driver.profitability).roundToLong(),
            fuelCostEuro = refuelCost(litres, 1.62),
            cargo = contract.cargo,
            weightTons = contract.weightTons,
        )
    }

    /** Result of fast-forwarding the employed drivers. */
    class DriverTripReport(
        val drivers: List<Driver>,
        val completedRoutes: List<DriverRoute>,
        val revenueEuro: Long,
        val fuelCostEuro: Long,
        val incidents: List<String>,
        /** Instance id of the truck -> wear percent added in this period. */
        val truckWear: Map<String, Double>,
        /** Driver id -> XP gained. */
        val xpGains: Map<String, Int>,
    )

    /**
     * Advances every driver that is currently on the road. Returns the updated drivers plus the money
     * they earned and spent, the accidents that happened and the wear their trucks accumulated.
     */
    fun advanceDriverTrips(drivers: List<Driver>, minutes: Long, rng: Rng): DriverTripReport {
        if (minutes <= 0) {
            return DriverTripReport(drivers, emptyList(), 0, 0, emptyList(), emptyMap(), emptyMap())
        }
        val updated = ArrayList<Driver>(drivers.size)
        val completed = ArrayList<DriverRoute>()
        val incidents = ArrayList<String>()
        val wear = HashMap<String, Double>()
        val xpGains = HashMap<String, Int>()
        var revenue = 0L
        var fuelSpent = 0L

        for (driver in drivers) {
            val route = driver.currentRoute
            if (route == null || driver.status != DriverStatus.ON_ROUTE) {
                updated += driver
                continue
            }
            val elapsed = route.elapsedMinutes + minutes
            if (elapsed < route.totalMinutes) {
                updated += driver.copy(currentRoute = route.copy(elapsedMinutes = elapsed))
                continue
            }

            val xp = 90 + (route.totalMinutes / 6).toInt()
            val hadAccident = rng.nextDouble() < driver.accidentRate * (route.totalMinutes / 900.0)
            val truckId = driver.assignedTruckId ?: route.contractId
            val repairCost = if (hadAccident) {
                val damage = rng.range(2_400.0, 11_500.0)
                wear[truckId] = (wear[truckId] ?: 0.0) + rng.range(5.0, 16.0)
                incidents += "${driver.name} a avut un incident pe ruta " +
                    "${CityCatalog.name(route.fromCityId)} → ${CityCatalog.name(route.toCityId)}, " +
                    "reparații ${Format.money(damage.toLong())}"
                damage.roundToLong()
            } else {
                0L
            }

            revenue += route.payoutEuro - repairCost
            fuelSpent += route.fuelCostEuro
            xpGains[driver.id] = xp
            completed += route
            wear[truckId] = (wear[truckId] ?: 0.0) + 1.4
            updated += driver.copy(
                currentRoute = null,
                status = DriverStatus.IDLE,
                xp = driver.xp + xp,
                level = DriverLevel.forXp(driver.xp + xp),
                tripsCompleted = driver.tripsCompleted + 1,
                lifetimeRevenueEuro = driver.lifetimeRevenueEuro + route.payoutEuro,
                lifetimeCostEuro = driver.lifetimeCostEuro + route.fuelCostEuro + repairCost,
            )
        }
        return DriverTripReport(updated, completed, revenue, fuelSpent, incidents, wear, xpGains)
    }

    // ================================================================ fleet operations

    /** Full workshop repair of every wear channel, priced from the model's service cost. */
    fun repairCost(truck: OwnedTruck): Long {
        val model = TruckCatalog.byId(truck.modelId)
        var cost = 0.0
        for (component in WearComponent.entries) {
            cost += truck.wear.of(component) / 100.0 * model.serviceCostEuro * component.repairCostFactor * 6.5
        }
        return (cost * 1.24).roundToLong() // workshop labour and VAT
    }

    /** Dealer buy-back value: model value, upgrades and remaining condition. */
    fun sellValue(truck: OwnedTruck): Long {
        val model = TruckCatalog.byId(truck.modelId)
        val base = model.priceEuro * model.resaleFactor
        val upgrades = truck.upgrades.totalInvestedEuro() * 0.35
        val condition = truck.wear.conditionPercent() / 100.0
        return ((base + upgrades) * (0.55 + condition * 0.45)).roundToLong().coerceAtLeast(3_000)
    }

    fun sellTrailerValue(trailer: OwnedTrailer): Long {
        val model = TrailerCatalog.byId(trailer.modelId)
        val condition = trailer.conditionPercent() / 100.0
        return (model.priceEuro * model.resaleFactor * (0.55 + condition * 0.45)).roundToLong().coerceAtLeast(2_000)
    }

    fun upgradePrice(truck: OwnedTruck, kind: UpgradeKind): Long = truck.upgrades.priceForNextLevel(kind)

    /**
     * Wear added per 1000 displayed kilometres for one channel, taking load, road class, driver skill
     * and driving style (hard braking, wheel slip) into account.
     */
    fun wearPer1000Km(
        component: WearComponent,
        loadFactor: Double,
        roadRoughness: Double,
        driverSkill: Int,
        hardDriving01: Double,
    ): Double {
        val base = when (component) {
            WearComponent.ENGINE -> 0.55
            WearComponent.GEARBOX -> 0.42
            WearComponent.BRAKES -> 0.95
            WearComponent.TIRES -> 1.15
            WearComponent.BODY -> 0.18
        }
        val load = 0.75 + Mathx.clamp01(loadFactor) * 0.9
        val road = 1.0 + (roadRoughness - 1.0) * 0.8
        val skill = 1.18 - driverSkill / 100.0 * 0.3
        val style = 1.0 + hardDriving01 * 0.55
        return base * load * road * skill * style
    }

    /** Applies long term wear (used by the driver fast-forward and by the crash handling). */
    fun applyWear(
        truck: OwnedTruck,
        kilometers: Double,
        loadFactor: Double,
        roadRoughness: Double,
        driverSkill: Int,
        hardDriving01: Double,
    ): OwnedTruck {
        if (kilometers <= 0) return truck
        val per1000 = kilometers / 1000.0
        var wear = truck.wear
        for (component in WearComponent.entries) {
            val delta = wearPer1000Km(component, loadFactor, roadRoughness, driverSkill, hardDriving01) * per1000
            wear = wear.add(component, delta)
        }
        return truck.copy(
            wear = wear,
            totalKm = truck.totalKm + kilometers,
            kmSinceService = truck.kmSinceService + kilometers,
            brokenDown = wear.isBroken(),
            status = if (wear.isBroken()) TruckStatus.BROKEN else truck.status,
        )
    }

    /** True when the truck can be used for a contract of the given weight. */
    fun canHaul(contract: Contract, truck: OwnedTruck, trailer: OwnedTrailer?): Boolean {
        val model = TruckCatalog.byId(truck.modelId)
        val trailerModel = trailer?.let { TrailerCatalog.byId(it.modelId) }
        val required = contract.requiredTrailerKind
        if (required != null && trailerModel?.kind != required) return false
        if (trailerModel != null && !trailerModel.allowedCargo.contains(contract.cargo)) return false
        val totalTons = (model.curbWeightKg + (trailerModel?.tareWeightKg ?: 0)) / 1000.0 + contract.weightTons
        if (totalTons > model.maxGrossWeightKg / 1000.0 + 0.05) return false
        if (trailerModel != null && contract.weightTons > trailerModel.capacityTons) return false
        return !truck.brokenDown
    }

    // ================================================================ police & rules of the road

    /** Fine for speeding, scaled with how far over the limit the player is. */
    fun speedingFine(overKmh: Int, limitKmh: Int, inCity: Boolean): Long {
        if (overKmh <= 5) return 0
        val severity = overKmh / limitKmh.toDouble()
        val base = if (inCity) 320.0 else 240.0
        val factor = when {
            severity > 0.5 -> 4.2
            severity > 0.35 -> 3.0
            severity > 0.22 -> 2.0
            severity > 0.12 -> 1.35
            else -> 1.0
        }
        return (base * factor * (1 + limitKmh / 260.0)).roundToLong()
    }

    /** Chance that a patrol actually stops the player, per violation. */
    fun policeRisk(overKmh: Int, inCity: Boolean): Double = when {
        overKmh <= 5 -> 0.0
        overKmh <= 12 -> if (inCity) 0.05 else 0.03
        overKmh <= 25 -> if (inCity) 0.16 else 0.10
        overKmh <= 40 -> 0.33
        else -> 0.5
    }

    fun redLightFine(): Long = 780

    fun collisionFine(collisionSpeedKmh: Double): Long =
        (420 + collisionSpeedKmh * 26).roundToLong().coerceAtMost(4_800)

    /** Bonus for an undamaged, on-time delivery. */
    fun perfectDeliveryBonus(payoutEuro: Long): Long = (payoutEuro * 0.08).roundToLong()
}
