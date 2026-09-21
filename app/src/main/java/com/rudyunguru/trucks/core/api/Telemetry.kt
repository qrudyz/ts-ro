package com.rudyunguru.trucks.core.api

/**
 * Live readout of the player's truck, refreshed every frame by the driving session and read by the
 * HUD, the dashboard and the audio engine. Plain mutable class: one instance lives for the whole
 * session and is written in place (no allocation while driving).
 */
class Telemetry {
    /** Shown speed, already converted using [com.rudyunguru.trucks.core.model.MapScale]. */
    var speedKmh: Double = 0.0
    var rpm: Double = 0.0
    var gear: Int = 0
    var gearCount: Int = 12
    var gearboxLabel: String = "A"
    var fuelLiters: Double = 0.0
    var fuelCapacityLiters: Double = 0.0
    var fuelPercent: Double = 100.0
    var fuelRangeKm: Double = 0.0
    var engineTemperatureC: Double = 90.0
    var oilTemperatureC: Double = 95.0
    var airPressureBar: Double = 8.2
    var odometerKm: Double = 0.0
    var tripKm: Double = 0.0
    var consumptionL100: Double = 0.0
    var averageConsumptionL100: Double = 0.0
    var throttle01: Double = 0.0
    var brake01: Double = 0.0
    var retarderLevel: Int = 0
    var cruiseControlKmh: Double = 0.0
    var engineBrake: Boolean = false
    var handbrake: Boolean = false
    var wearPercent: Double = 100.0
    var damagePercent: Double = 0.0
    var loadTons: Double = 0.0
    var totalMassTons: Double = 0.0
    var positionX: Double = 0.0
    var positionY: Double = 0.0
    var positionZ: Double = 0.0
    var headingRad: Double = 0.0
    var currentSpeedLimitKmh: Int = 50
    var speeding: Boolean = false
    var speedingSeconds: Double = 0.0
    var headlights: Boolean = false
    var highBeam: Boolean = false
    var indicatorLeft: Boolean = false
    var indicatorRight: Boolean = false
    var hazards: Boolean = false
    var wipersOn: Boolean = false
    var hornActive: Boolean = false
    var brakeTemperature01: Double = 0.0
    var tireSlip01: Double = 0.0
    var trailerAttached: Boolean = false
    var trailerAngleRad: Double = 0.0
    /** 0..1 how much of the scheduled route is done (0 when free roaming). */
    var routeProgress: Double = 0.0
    var remainingKm: Double = 0.0
    var remainingMinutes: Long = 0
    var nextManeuverText: String = ""
    var nextManeuverDistanceM: Double = 0.0
    var statusLine: String = ""

    fun copyFrom(other: Telemetry) {
        speedKmh = other.speedKmh
        rpm = other.rpm
        gear = other.gear
        gearCount = other.gearCount
        gearboxLabel = other.gearboxLabel
        fuelLiters = other.fuelLiters
        fuelCapacityLiters = other.fuelCapacityLiters
        fuelPercent = other.fuelPercent
        fuelRangeKm = other.fuelRangeKm
        engineTemperatureC = other.engineTemperatureC
        oilTemperatureC = other.oilTemperatureC
        airPressureBar = other.airPressureBar
        odometerKm = other.odometerKm
        tripKm = other.tripKm
        consumptionL100 = other.consumptionL100
        averageConsumptionL100 = other.averageConsumptionL100
        throttle01 = other.throttle01
        brake01 = other.brake01
        retarderLevel = other.retarderLevel
        cruiseControlKmh = other.cruiseControlKmh
        engineBrake = other.engineBrake
        handbrake = other.handbrake
        wearPercent = other.wearPercent
        damagePercent = other.damagePercent
        loadTons = other.loadTons
        totalMassTons = other.totalMassTons
        positionX = other.positionX
        positionY = other.positionY
        positionZ = other.positionZ
        headingRad = other.headingRad
        currentSpeedLimitKmh = other.currentSpeedLimitKmh
        speeding = other.speeding
        speedingSeconds = other.speedingSeconds
        headlights = other.headlights
        highBeam = other.highBeam
        indicatorLeft = other.indicatorLeft
        indicatorRight = other.indicatorRight
        hazards = other.hazards
        wipersOn = other.wipersOn
        hornActive = other.hornActive
        brakeTemperature01 = other.brakeTemperature01
        tireSlip01 = other.tireSlip01
        trailerAttached = other.trailerAttached
        trailerAngleRad = other.trailerAngleRad
        routeProgress = other.routeProgress
        remainingKm = other.remainingKm
        remainingMinutes = other.remainingMinutes
        nextManeuverText = other.nextManeuverText
        nextManeuverDistanceM = other.nextManeuverDistanceM
        statusLine = other.statusLine
    }
}

/** What the audio engine needs to know; implemented by the session, consumed by [AudioApi]. */
interface AudioSource {
    val telemetry: Telemetry
    fun isBraking(): Boolean
    fun isSlipActive(): Boolean
    fun rainIntensity(): Double
    fun windIntensity(): Double
}
