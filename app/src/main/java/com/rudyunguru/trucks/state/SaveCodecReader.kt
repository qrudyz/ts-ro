package com.rudyunguru.trucks.state

import com.rudyunguru.trucks.core.Json
import com.rudyunguru.trucks.core.Json.bool
import com.rudyunguru.trucks.core.Json.int
import com.rudyunguru.trucks.core.Json.long
import com.rudyunguru.trucks.core.Json.num
import com.rudyunguru.trucks.core.Json.obj
import com.rudyunguru.trucks.core.Json.objs
import com.rudyunguru.trucks.core.Json.str
import com.rudyunguru.trucks.core.Json.strings
import com.rudyunguru.trucks.core.model.CameraPreset
import com.rudyunguru.trucks.core.model.Company
import com.rudyunguru.trucks.core.model.ControlMode
import com.rudyunguru.trucks.core.model.GameSettings
import com.rudyunguru.trucks.core.model.GraphicsPreset
import com.rudyunguru.trucks.core.model.LanguageCode
import com.rudyunguru.trucks.core.model.PlayerProfile
import com.rudyunguru.trucks.core.model.SaveData
import com.rudyunguru.trucks.core.model.UnitsSystem

/**
 * Reader half of the save format (writer: [SaveCodec]). Every field is optional and falls back to the
 * data class default, so saves stay compatible when new fields are introduced.
 */
object SaveCodecReader {

    /** Returns null when the payload is not a usable save game. */
    fun decode(json: String): SaveData? {
        val root = Json.parseObject(json) ?: return null
        return runCatching { fromMap(root) }.getOrNull()
    }

    fun fromMap(m: Map<String, Any?>): SaveData = SaveData(
        version = m.int("version", SaveData.CURRENT_VERSION),
        seed = m.int("seed", 20240613),
        createdEpochMs = m.long("createdEpochMs", 0L),
        lastPlayedEpochMs = m.long("lastPlayedEpochMs", 0L),
        totalMinutes = m.long("totalMinutes", 8L * 60L),
        fuelPricePerLitre = m.num("fuelPricePerLitre", 1.62),
        fuelPriceUpdatedDay = m.int("fuelPriceUpdatedDay", 0),
        unlockedCityIds = m.strings("unlockedCityIds").ifEmpty { listOf("bucuresti") },
        firstRunComplete = m.bool("firstRunComplete", false),
        lastScreen = m.str("lastScreen", "home"),
        player = player(m.obj("player") ?: emptyMap()),
        company = company(m.obj("company") ?: emptyMap()),
        statistics = statistics(m.obj("statistics") ?: emptyMap()),
        settings = settings(m.obj("settings") ?: emptyMap()),
        trucks = m.objs("trucks").map { truck(it) },
        trailers = m.objs("trailers").map { trailer(it) },
        drivers = m.objs("drivers").map { driver(it) },
        garages = m.objs("garages").map { garage(it) },
        contracts = m.objs("contracts").map { contract(it) },
        activeJob = m.obj("activeJob")?.let { activeJob(it) },
        loans = m.objs("loans").map { loan(it) },
        usedTruckListings = m.objs("usedTruckListings").map { usedTruck(it) },
        notifications = m.objs("notifications").map { notification(it) },
    )

    // ---------------------------------------------------------------- nested readers

    private fun player(m: Map<String, Any?>): PlayerProfile = PlayerProfile(
        name = m.str("name", "Șofer"),
        avatarIndex = m.int("avatarIndex", 0),
        level = m.int("level", 1),
        xp = m.int("xp", 0),
        moneyEuro = m.long("moneyEuro", 75_000),
        reputation = m.int("reputation", 0),
        currentCityId = m.str("currentCityId", "bucuresti"),
    )

    private fun company(m: Map<String, Any?>): Company = Company(
        name = m.str("name", "Compania mea"),
        logoId = m.int("logoId", 0),
        colorHex = m.str("colorHex", "#FFD166"),
        hqCityId = m.str("hqCityId", "bucuresti"),
        created = m.bool("created", false),
        createdDay = m.int("createdDay", 0),
    )

    private fun statistics(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.Statistics =
        com.rudyunguru.trucks.core.model.Statistics(
            totalKm = m.num("totalKm", 0.0),
            totalJobs = m.int("totalJobs", 0),
            perfectDeliveries = m.int("perfectDeliveries", 0),
            lateDeliveries = m.int("lateDeliveries", 0),
            failedDeliveries = m.int("failedDeliveries", 0),
            accidents = m.int("accidents", 0),
            policeStops = m.int("policeStops", 0),
            totalRevenueEuro = m.long("totalRevenueEuro", 0),
            totalFuelCostEuro = m.long("totalFuelCostEuro", 0),
            totalRepairCostEuro = m.long("totalRepairCostEuro", 0),
            totalSalaryCostEuro = m.long("totalSalaryCostEuro", 0),
            totalFinesEuro = m.long("totalFinesEuro", 0),
            totalLoanPaymentsEuro = m.long("totalLoanPaymentsEuro", 0),
            litersUsed = m.num("litersUsed", 0.0),
            longestTripKm = m.num("longestTripKm", 0.0),
            bestContractEuro = m.long("bestContractEuro", 0),
            daysPlayed = m.int("daysPlayed", 0),
        )

    private fun settings(m: Map<String, Any?>): GameSettings = GameSettings(
        graphics = enumOf(m.str("graphics"), GraphicsPreset.MEDIUM),
        controlMode = enumOf(m.str("controlMode"), ControlMode.VIRTUAL_WHEEL),
        camera = enumOf(m.str("camera"), CameraPreset.DASHBOARD),
        hudVisible = m.bool("hudVisible", true),
        steeringSensitivity = m.num("steeringSensitivity", 1.0),
        steeringDeadZone = m.num("steeringDeadZone", 0.05),
        autoGearbox = m.bool("autoGearbox", true),
        cruiseControlAvailable = m.bool("cruiseControlAvailable", true),
        tiltInvert = m.bool("tiltInvert", false),
        masterVolume = m.num("masterVolume", 0.9),
        engineVolume = m.num("engineVolume", 0.95),
        sfxVolume = m.num("sfxVolume", 0.85),
        uiVolume = m.num("uiVolume", 0.7),
        voiceVolume = m.num("voiceVolume", 0.9),
        voiceGuidance = m.bool("voiceGuidance", true),
        vibration = m.bool("vibration", true),
        cameraShake = m.num("cameraShake", 0.6),
        adaptiveResolution = m.bool("adaptiveResolution", true),
        units = enumOf(m.str("units"), UnitsSystem.METRIC),
        language = enumOf(m.str("language"), LanguageCode.ROMANIAN),
        trafficDensity = m.num("trafficDensity", 0.7),
        trafficEnabled = m.bool("trafficEnabled", true),
        weatherEnabled = m.bool("weatherEnabled", true),
        dayNightCycle = m.bool("dayNightCycle", true),
        timeScale = m.num("timeScale", 1.0),
        policeEnabled = m.bool("policeEnabled", true),
        autoSave = m.bool("autoSave", true),
        speedLimitWarnings = m.bool("speedLimitWarnings", true),
        invertLook = m.bool("invertLook", false),
        leftHandedControls = m.bool("leftHandedControls", false),
        difficulty = m.num("difficulty", 1.0),
    )

    private fun upgrades(m: Map<String, Any?>?): com.rudyunguru.trucks.core.model.TruckUpgrades {
        if (m == null) return com.rudyunguru.trucks.core.model.TruckUpgrades.NONE
        val levels = HashMap<com.rudyunguru.trucks.core.model.UpgradeKind, Int>()
        for ((key, value) in m) {
            val kind = enumValues<com.rudyunguru.trucks.core.model.UpgradeKind>()
                .firstOrNull { it.name.equals(key, ignoreCase = true) } ?: continue
            val level = (value as? Number)?.toInt() ?: continue
            if (level > 0) levels[kind] = level
        }
        return com.rudyunguru.trucks.core.model.TruckUpgrades(levels)
    }

    private fun truck(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.OwnedTruck {
        val wear = m.obj("wear") ?: emptyMap()
        return com.rudyunguru.trucks.core.model.OwnedTruck(
            instanceId = m.str("instanceId", "truck_unknown"),
            modelId = m.str("modelId", com.rudyunguru.trucks.core.catalog.TruckCatalog.STARTER_ID),
            plate = m.str("plate", "B00 RTS"),
            paintHex = m.str("paintHex", "#D9051F"),
            garageCityId = m.str("garageCityId", "bucuresti"),
            purchasedDay = m.int("purchasedDay", 0),
            totalKm = m.num("totalKm", 0.0),
            kmSinceService = m.num("kmSinceService", 0.0),
            fuelL = m.num("fuelL", 0.0),
            wear = com.rudyunguru.trucks.core.model.TruckWear(
                engine = wear.num("engine", 0.0),
                gearbox = wear.num("gearbox", 0.0),
                brakes = wear.num("brakes", 0.0),
                tires = wear.num("tires", 0.0),
                body = wear.num("body", 0.0),
            ),
            upgrades = upgrades(m.obj("upgrades")),
            status = enumOf(m.str("status"), com.rudyunguru.trucks.core.model.TruckStatus.PARKED),
            trailerInstanceId = m.str("trailerInstanceId", "").ifEmpty { null },
            driverId = m.str("driverId", "").ifEmpty { null },
            lifetimeRevenueEuro = m.long("lifetimeRevenueEuro", 0),
            lifetimeFuelCostEuro = m.long("lifetimeFuelCostEuro", 0),
            lifetimeRepairCostEuro = m.long("lifetimeRepairCostEuro", 0),
            averageConsumptionL100 = m.num("averageConsumptionL100", 0.0),
            brokenDown = m.bool("brokenDown", false),
        )
    }

    private fun trailer(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.OwnedTrailer =
        com.rudyunguru.trucks.core.model.OwnedTrailer(
            instanceId = m.str("instanceId", "trailer_unknown"),
            modelId = m.str("modelId", com.rudyunguru.trucks.core.catalog.TrailerCatalog.TRAILERS.first().id),
            plate = m.str("plate", "B00 TRL"),
            garageCityId = m.str("garageCityId", "bucuresti"),
            purchasedDay = m.int("purchasedDay", 0),
            totalKm = m.num("totalKm", 0.0),
            wearTires = m.num("wearTires", 0.0),
            wearBody = m.num("wearBody", 0.0),
            allocatedTruckId = m.str("allocatedTruckId", "").ifEmpty { null },
        )

    private fun driverRoute(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.DriverRoute =
        com.rudyunguru.trucks.core.model.DriverRoute(
            contractId = m.str("contractId", ""),
            fromCityId = m.str("fromCityId", "bucuresti"),
            toCityId = m.str("toCityId", "bucuresti"),
            startedDay = m.int("startedDay", 0),
            startedMinutes = m.long("startedMinutes", 0),
            totalMinutes = m.long("totalMinutes", 60),
            elapsedMinutes = m.long("elapsedMinutes", 0),
            payoutEuro = m.long("payoutEuro", 0),
            fuelCostEuro = m.long("fuelCostEuro", 0),
            cargo = enumOf(m.str("cargo"), com.rudyunguru.trucks.core.model.CargoKind.FOOD),
            weightTons = m.num("weightTons", 0.0),
        )

    private fun driver(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.Driver =
        com.rudyunguru.trucks.core.model.Driver(
            id = m.str("id", "driver_unknown"),
            name = m.str("name", "Șofer"),
            avatarIndex = m.int("avatarIndex", 0),
            age = m.int("age", 35),
            cityId = m.str("cityId", "bucuresti"),
            level = enumOf(m.str("level"), com.rudyunguru.trucks.core.model.DriverLevel.BEGINNER),
            xp = m.int("xp", 0),
            skill = m.int("skill", 25),
            baseSalaryPerDay = m.long("baseSalaryPerDay", 92),
            fuelEconomy = m.num("fuelEconomy", 1.06),
            accidentRate = m.num("accidentRate", 0.06),
            profitability = m.num("profitability", 0.92),
            status = enumOf(m.str("status"), com.rudyunguru.trucks.core.model.DriverStatus.IDLE),
            hiredDay = m.int("hiredDay", 0),
            assignedTruckId = m.str("assignedTruckId", "").ifEmpty { null },
            currentRoute = m.obj("currentRoute")?.let { driverRoute(it) },
            lifetimeRevenueEuro = m.long("lifetimeRevenueEuro", 0),
            lifetimeCostEuro = m.long("lifetimeCostEuro", 0),
            tripsCompleted = m.int("tripsCompleted", 0),
        )

    private fun garage(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.Garage =
        com.rudyunguru.trucks.core.model.Garage(
            cityId = m.str("cityId", "bucuresti"),
            level = enumOf(m.str("level"), com.rudyunguru.trucks.core.model.GarageLevel.LEVEL_1),
            purchasedDay = m.int("purchasedDay", 0),
            truckInstanceIds = m.strings("truckInstanceIds"),
        )

    private fun loan(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.Loan =
        com.rudyunguru.trucks.core.model.Loan(
            id = m.str("id", "loan"),
            principalEuro = m.long("principalEuro", 0),
            remainingEuro = m.long("remainingEuro", 0),
            dailyPaymentEuro = m.long("dailyPaymentEuro", 0),
            dailyInterestRate = m.num("dailyInterestRate", 0.0003),
            takenDay = m.int("takenDay", 0),
            termDays = m.int("termDays", 90),
            daysPaid = m.int("daysPaid", 0),
            description = m.str("description", "Credit"),
        )

    private fun notification(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.GameNotification =
        com.rudyunguru.trucks.core.model.GameNotification(
            id = m.long("id", 0),
            kind = enumOf(m.str("kind"), com.rudyunguru.trucks.core.model.NotificationKind.INFO),
            title = m.str("title", ""),
            message = m.str("message", ""),
            day = m.int("day", 0),
            minutes = m.long("minutes", 0),
            moneyDeltaEuro = m.long("moneyDeltaEuro", 0),
            read = m.bool("read", false),
        )

    private fun usedTruck(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.UsedTruckListing =
        com.rudyunguru.trucks.core.model.UsedTruckListing(
            instanceId = m.str("instanceId", "used_unknown"),
            modelId = m.str("modelId", com.rudyunguru.trucks.core.catalog.TruckCatalog.STARTER_ID),
            priceEuro = m.long("priceEuro", 0),
            km = m.num("km", 0.0),
            conditionPercent = m.num("conditionPercent", 70.0),
            plate = m.str("plate", "B00 RTS"),
            paintHex = m.str("paintHex", "#D9051F"),
            upgrades = upgrades(m.obj("upgrades")),
        )

    private fun contract(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.Contract =
        com.rudyunguru.trucks.core.model.Contract(
            id = m.str("id", "contract"),
            cargo = enumOf(m.str("cargo"), com.rudyunguru.trucks.core.model.CargoKind.FOOD),
            fromCityId = m.str("fromCityId", "bucuresti"),
            toCityId = m.str("toCityId", "bucuresti"),
            weightTons = m.num("weightTons", 10.0),
            distanceKm = m.num("distanceKm", 100.0),
            payoutEuro = m.long("payoutEuro", 1000),
            deadlineMinutes = m.long("deadlineMinutes", 300),
            recommendedTruckClass = enumOf(
                m.str("recommendedTruckClass"),
                com.rudyunguru.trucks.core.model.TruckClass.STARTER,
            ),
            requiredTrailerKind = m.str("requiredTrailerKind", "").ifEmpty { null }?.let {
                enumOf(it, com.rudyunguru.trucks.core.model.TrailerKind.CURTAINSIDER)
            },
            reputationRequirement = m.int("reputationRequirement", 0),
            urgency = m.num("urgency", 0.3),
            postedDay = m.int("postedDay", 0),
            expiresDay = m.int("expiresDay", 3),
            shipper = m.str("shipper", "Transportator"),
            status = enumOf(m.str("status"), com.rudyunguru.trucks.core.model.ContractStatus.AVAILABLE),
            penaltyEuro = m.long("penaltyEuro", 0),
        )

    private fun activeJob(m: Map<String, Any?>): com.rudyunguru.trucks.core.model.ActiveJob =
        com.rudyunguru.trucks.core.model.ActiveJob(
            contract = contract(m.obj("contract") ?: emptyMap()),
            acceptedDay = m.int("acceptedDay", 0),
            acceptedMinutes = m.long("acceptedMinutes", 0),
            remainingMinutes = m.long("remainingMinutes", 0),
            drivenKm = m.num("drivenKm", 0.0),
            cargoDamage = m.num("cargoDamage", 0.0),
            finesEuro = m.long("finesEuro", 0),
            fuelCostEuro = m.long("fuelCostEuro", 0),
            routeProgress = m.num("routeProgress", 0.0),
            nextManeuverIndex = m.int("nextManeuverIndex", 0),
            deliveredPerfect = m.bool("deliveredPerfect", false),
        )

    /** Enum lookup that never throws: unknown names fall back to the default value. */
    internal inline fun <reified T : Enum<T>> enumOf(name: String?, fallback: T): T {
        if (name == null) return fallback
        return enumValues<T>().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: fallback
    }
}
