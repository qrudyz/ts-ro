package com.rudyunguru.trucks.state

import com.rudyunguru.trucks.core.Json
import com.rudyunguru.trucks.core.model.*

/**
 * Manual, version tolerant serialization of [SaveData] on top of [Json].
 *
 * Written by hand instead of using reflection or an Android-only JSON API so that:
 *  - exactly the same code path runs on the device and in JVM unit tests (round-trip is tested);
 *  - every field has a documented default, so an older save keeps loading after new fields appear;
 *  - the file stays readable while debugging ([prettyPrint]).
 *
 * Reading lives in [SaveCodecReader].
 */
object SaveCodec {

    /** Set to true to pretty print the save file (useful while debugging). */
    var prettyPrint: Boolean = false

    fun encode(data: SaveData): String {
        val json = Json.stringify(toMap(data))
        return if (!prettyPrint) json else pretty(json)
    }

    fun decode(json: String): SaveData? = SaveCodecReader.decode(json)

    fun toMap(data: SaveData): Map<String, Any?> = linkedMapOf(
        "version" to data.version,
        "seed" to data.seed,
        "createdEpochMs" to data.createdEpochMs,
        "lastPlayedEpochMs" to data.lastPlayedEpochMs,
        "totalMinutes" to data.totalMinutes,
        "fuelPricePerLitre" to data.fuelPricePerLitre,
        "fuelPriceUpdatedDay" to data.fuelPriceUpdatedDay,
        "unlockedCityIds" to data.unlockedCityIds,
        "firstRunComplete" to data.firstRunComplete,
        "lastScreen" to data.lastScreen,
        "player" to player(data),
        "company" to company(data),
        "statistics" to statistics(data),
        "settings" to settings(data),
        "trucks" to data.trucks.map { truck(it) },
        "trailers" to data.trailers.map { trailer(it) },
        "drivers" to data.drivers.map { driver(it) },
        "garages" to data.garages.map { garage(it) },
        "contracts" to data.contracts.map { contract(it) },
        "activeJob" to data.activeJob?.let { activeJob(it) },
        "loans" to data.loans.map { loan(it) },
        "usedTruckListings" to data.usedTruckListings.map { usedTruck(it) },
        "notifications" to data.notifications.map { notification(it) },
    )

    private fun player(d: SaveData): Map<String, Any?> = with(d.player) {
        linkedMapOf(
            "name" to name, "avatarIndex" to avatarIndex, "level" to level, "xp" to xp,
            "moneyEuro" to moneyEuro, "reputation" to reputation, "currentCityId" to currentCityId,
        )
    }

    private fun company(d: SaveData): Map<String, Any?> = with(d.company) {
        linkedMapOf(
            "name" to name, "logoId" to logoId, "colorHex" to colorHex, "hqCityId" to hqCityId,
            "created" to created, "createdDay" to createdDay,
        )
    }

    private fun statistics(d: SaveData): Map<String, Any?> = with(d.statistics) {
        linkedMapOf(
            "totalKm" to totalKm, "totalJobs" to totalJobs, "perfectDeliveries" to perfectDeliveries,
            "lateDeliveries" to lateDeliveries, "failedDeliveries" to failedDeliveries,
            "accidents" to accidents, "policeStops" to policeStops,
            "totalRevenueEuro" to totalRevenueEuro, "totalFuelCostEuro" to totalFuelCostEuro,
            "totalRepairCostEuro" to totalRepairCostEuro, "totalSalaryCostEuro" to totalSalaryCostEuro,
            "totalFinesEuro" to totalFinesEuro, "totalLoanPaymentsEuro" to totalLoanPaymentsEuro,
            "litersUsed" to litersUsed, "longestTripKm" to longestTripKm,
            "bestContractEuro" to bestContractEuro, "daysPlayed" to daysPlayed,
        )
    }

    private fun settings(d: SaveData): Map<String, Any?> = with(d.settings) {
        linkedMapOf(
            "graphics" to graphics.name, "controlMode" to controlMode.name, "camera" to camera.name,
            "hudVisible" to hudVisible, "steeringSensitivity" to steeringSensitivity,
            "steeringDeadZone" to steeringDeadZone, "autoGearbox" to autoGearbox,
            "cruiseControlAvailable" to cruiseControlAvailable, "tiltInvert" to tiltInvert,
            "masterVolume" to masterVolume, "engineVolume" to engineVolume, "sfxVolume" to sfxVolume,
            "uiVolume" to uiVolume, "voiceVolume" to voiceVolume, "voiceGuidance" to voiceGuidance,
            "vibration" to vibration, "cameraShake" to cameraShake,
            "adaptiveResolution" to adaptiveResolution, "units" to units.name,
            "language" to language.name, "trafficDensity" to trafficDensity,
            "trafficEnabled" to trafficEnabled, "weatherEnabled" to weatherEnabled,
            "dayNightCycle" to dayNightCycle, "timeScale" to timeScale,
            "policeEnabled" to policeEnabled, "autoSave" to autoSave,
            "speedLimitWarnings" to speedLimitWarnings, "invertLook" to invertLook,
            "leftHandedControls" to leftHandedControls, "difficulty" to difficulty,
        )
    }

    private fun truck(t: com.rudyunguru.trucks.core.model.OwnedTruck): Map<String, Any?> = linkedMapOf(
        "instanceId" to t.instanceId, "modelId" to t.modelId, "plate" to t.plate,
        "paintHex" to t.paintHex, "garageCityId" to t.garageCityId, "purchasedDay" to t.purchasedDay,
        "totalKm" to t.totalKm, "kmSinceService" to t.kmSinceService, "fuelL" to t.fuelL,
        "wear" to linkedMapOf(
            "engine" to t.wear.engine, "gearbox" to t.wear.gearbox, "brakes" to t.wear.brakes,
            "tires" to t.wear.tires, "body" to t.wear.body,
        ),
        "upgrades" to t.upgrades.levels.entries.associate { it.key.name to it.value },
        "status" to t.status.name, "trailerInstanceId" to t.trailerInstanceId,
        "driverId" to t.driverId, "lifetimeRevenueEuro" to t.lifetimeRevenueEuro,
        "lifetimeFuelCostEuro" to t.lifetimeFuelCostEuro,
        "lifetimeRepairCostEuro" to t.lifetimeRepairCostEuro,
        "averageConsumptionL100" to t.averageConsumptionL100, "brokenDown" to t.brokenDown,
    )

    private fun trailer(t: com.rudyunguru.trucks.core.model.OwnedTrailer): Map<String, Any?> = linkedMapOf(
        "instanceId" to t.instanceId, "modelId" to t.modelId, "plate" to t.plate,
        "garageCityId" to t.garageCityId, "purchasedDay" to t.purchasedDay, "totalKm" to t.totalKm,
        "wearTires" to t.wearTires, "wearBody" to t.wearBody, "allocatedTruckId" to t.allocatedTruckId,
    )

    private fun driver(d: com.rudyunguru.trucks.core.model.Driver): Map<String, Any?> = linkedMapOf(
        "id" to d.id, "name" to d.name, "avatarIndex" to d.avatarIndex, "age" to d.age,
        "cityId" to d.cityId, "level" to d.level.name, "xp" to d.xp, "skill" to d.skill,
        "baseSalaryPerDay" to d.baseSalaryPerDay, "fuelEconomy" to d.fuelEconomy,
        "accidentRate" to d.accidentRate, "profitability" to d.profitability,
        "status" to d.status.name, "hiredDay" to d.hiredDay, "assignedTruckId" to d.assignedTruckId,
        "currentRoute" to d.currentRoute?.let { route(it) },
        "lifetimeRevenueEuro" to d.lifetimeRevenueEuro, "lifetimeCostEuro" to d.lifetimeCostEuro,
        "tripsCompleted" to d.tripsCompleted,
    )

    private fun route(r: com.rudyunguru.trucks.core.model.DriverRoute): Map<String, Any?> = linkedMapOf(
        "contractId" to r.contractId, "fromCityId" to r.fromCityId, "toCityId" to r.toCityId,
        "startedDay" to r.startedDay, "startedMinutes" to r.startedMinutes,
        "totalMinutes" to r.totalMinutes, "elapsedMinutes" to r.elapsedMinutes,
        "payoutEuro" to r.payoutEuro, "fuelCostEuro" to r.fuelCostEuro,
        "cargo" to r.cargo.name, "weightTons" to r.weightTons,
    )

    private fun garage(g: com.rudyunguru.trucks.core.model.Garage): Map<String, Any?> = linkedMapOf(
        "cityId" to g.cityId, "level" to g.level.name, "purchasedDay" to g.purchasedDay,
        "truckInstanceIds" to g.truckInstanceIds,
    )

    private fun loan(l: com.rudyunguru.trucks.core.model.Loan): Map<String, Any?> = linkedMapOf(
        "id" to l.id, "principalEuro" to l.principalEuro, "remainingEuro" to l.remainingEuro,
        "dailyPaymentEuro" to l.dailyPaymentEuro, "dailyInterestRate" to l.dailyInterestRate,
        "takenDay" to l.takenDay, "termDays" to l.termDays, "daysPaid" to l.daysPaid,
        "description" to l.description,
    )

    private fun notification(n: com.rudyunguru.trucks.core.model.GameNotification): Map<String, Any?> = linkedMapOf(
        "id" to n.id, "kind" to n.kind.name, "title" to n.title, "message" to n.message,
        "day" to n.day, "minutes" to n.minutes, "moneyDeltaEuro" to n.moneyDeltaEuro, "read" to n.read,
    )

    private fun usedTruck(u: UsedTruckListing): Map<String, Any?> = linkedMapOf(
        "instanceId" to u.instanceId, "modelId" to u.modelId, "priceEuro" to u.priceEuro,
        "km" to u.km, "conditionPercent" to u.conditionPercent, "plate" to u.plate,
        "paintHex" to u.paintHex,
        "upgrades" to u.upgrades.levels.entries.associate { it.key.name to it.value },
    )

    private fun contract(c: Contract): Map<String, Any?> = linkedMapOf(
        "id" to c.id, "cargo" to c.cargo.name, "fromCityId" to c.fromCityId, "toCityId" to c.toCityId,
        "weightTons" to c.weightTons, "distanceKm" to c.distanceKm, "payoutEuro" to c.payoutEuro,
        "deadlineMinutes" to c.deadlineMinutes, "recommendedTruckClass" to c.recommendedTruckClass.name,
        "requiredTrailerKind" to c.requiredTrailerKind?.name,
        "reputationRequirement" to c.reputationRequirement, "urgency" to c.urgency,
        "postedDay" to c.postedDay, "expiresDay" to c.expiresDay, "shipper" to c.shipper,
        "status" to c.status.name, "penaltyEuro" to c.penaltyEuro,
    )

    private fun activeJob(j: ActiveJob): Map<String, Any?> = linkedMapOf(
        "contract" to contract(j.contract),
        "acceptedDay" to j.acceptedDay, "acceptedMinutes" to j.acceptedMinutes,
        "remainingMinutes" to j.remainingMinutes, "drivenKm" to j.drivenKm,
        "cargoDamage" to j.cargoDamage, "finesEuro" to j.finesEuro, "fuelCostEuro" to j.fuelCostEuro,
        "routeProgress" to j.routeProgress, "nextManeuverIndex" to j.nextManeuverIndex,
        "deliveredPerfect" to j.deliveredPerfect,
    )

    /** Minimal pretty printer: the save file is small and readability beats cleverness here. */
    private fun pretty(json: String): String {
        val sb = StringBuilder(json.length + 256)
        var indent = 0
        var inString = false
        var escaped = false
        for (c in json) {
            when {
                inString -> {
                    sb.append(c)
                    when {
                        escaped -> escaped = false
                        c == '\\' -> escaped = true
                        c == '"' -> inString = false
                    }
                }
                c == '"' -> { inString = true; sb.append(c) }
                c == '{' || c == '[' -> {
                    sb.append(c).append('\n')
                    indent++
                    appendIndent(sb, indent)
                }
                c == '}' || c == ']' -> {
                    sb.append('\n')
                    indent--
                    appendIndent(sb, indent)
                    sb.append(c)
                }
                c == ',' -> { sb.append(c).append('\n'); appendIndent(sb, indent) }
                c == ':' -> sb.append(": ")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun appendIndent(sb: StringBuilder, level: Int) {
        repeat(level) { sb.append("  ") }
    }
}
