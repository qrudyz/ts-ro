package com.rudyunguru.trucks.core.model

import com.rudyunguru.trucks.core.Mathx
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Map data contracts shared by the world generator, the GPS and the map screen.
 *
 * The world is built in "world units". One unit represents [COMPRESSION] real metres so the whole
 * country fits in a mobile-friendly scene, while every distance shown to the player (km, fuel,
 * odometer, contract payouts) is converted back to real kilometres. Physical speeds stay realistic:
 * 25 units/s equals 90 km/h of displayed speed.
 */
object MapScale {
    /** Real metres represented by one world unit. */
    const val COMPRESSION: Double = 20.0

    /** Local map origin: geographical centre of Romania. */
    const val ORIGIN_LAT: Double = 45.94
    const val ORIGIN_LON: Double = 24.96

    private const val METRES_PER_DEG_LAT = 111_320.0

    fun metresToUnits(metres: Double): Double = metres / COMPRESSION
    fun unitsToMetres(units: Double): Double = units * COMPRESSION
    fun unitsToKm(units: Double): Double = units * COMPRESSION / 1000.0
    fun kmToUnits(km: Double): Double = km * 1000.0 / COMPRESSION

    /**
     * Equirectangular projection around Romania's centre, then compression to world units.
     * Result: x grows east, z grows south.
     */
    fun project(lat: Double, lon: Double): Pair<Double, Double> {
        val metresPerDegLon = METRES_PER_DEG_LAT * cos(ORIGIN_LAT * Mathx.DEG2RAD)
        val east = (lon - ORIGIN_LON) * metresPerDegLon
        val south = (ORIGIN_LAT - lat) * METRES_PER_DEG_LAT
        return metresToUnits(east) to metresToUnits(south)
    }

    /** Straight-line real distance (km) between two geographic points. */
    fun geoDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val metresPerDegLon = METRES_PER_DEG_LAT * cos(ORIGIN_LAT * Mathx.DEG2RAD)
        val dx = (lon2 - lon1) * metresPerDegLon
        val dz = (lat2 - lat1) * METRES_PER_DEG_LAT
        return sqrt(dx * dx + dz * dz) / 1000.0
    }
}

enum class RoadClass(
    val label: String,
    val defaultSpeedLimitKmh: Int,
    val lanesPerDirection: Int,
    val roadWidthMetres: Double,
) {
    MOTORWAY("Autostradă", 130, 2, 11.5),
    EXPRESS("Drum expres", 120, 2, 10.5),
    NATIONAL("Drum național", 100, 1, 8.0),
    SECONDARY("Drum județean", 90, 1, 7.0),
    LOCAL("Drum local", 60, 1, 6.0),
    CITY("Stradă urbană", 50, 1, 7.5),
    SERVICE("Drum de serviciu", 30, 1, 5.0),
}

/** Industrial / logistics sites that generate contracts. */
enum class DepotKind(val label: String, val cargo: List<CargoKind>) {
    LOGISTICS_HUB("Hub logistic", listOf(CargoKind.CONTAINERS, CargoKind.FOOD, CargoKind.VEHICLES)),
    WAREHOUSE("Depozit", listOf(CargoKind.FOOD, CargoKind.CONSTRUCTION, CargoKind.CONTAINERS)),
    FACTORY("Fabrica", listOf(CargoKind.MACHINERY, CargoKind.VEHICLES, CargoKind.CONSTRUCTION)),
    FARM("Fermă", listOf(CargoKind.GRAIN, CargoKind.FOOD)),
    QUARRY("Carieră", listOf(CargoKind.CONSTRUCTION)),
    PORT("Port", listOf(CargoKind.CONTAINERS, CargoKind.MACHINERY)),
    TERMINAL("Terminal frigorific", listOf(CargoKind.REFRIGERATED)),
    SAWMILL("Fabrica de cherestea", listOf(CargoKind.TIMBER)),
}

enum class CityTier { METROPOLIS, CITY, TOWN }
