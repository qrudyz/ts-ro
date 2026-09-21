package com.rudyunguru.trucks.core.model

/** A city on the simplified Romania map. Position is projected once, lazily. */
data class City(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val county: String,
    val tier: CityTier,
    val hasTruckDealer: Boolean,
    val hasTrailerDealer: Boolean,
    val hasService: Boolean,
    val hasGarageDealer: Boolean,
    val depots: List<DepotKind>,
    val population: Int,
) {
    /** World-space position (x east, z south). */
    val x: Double = MapScale.project(lat, lon).first
    val z: Double = MapScale.project(lat, lon).second

    fun distanceKmTo(other: City): Double = MapScale.geoDistanceKm(lat, lon, other.lat, other.lon)
}

enum class ManeuverKind { STRAIGHT, TURN_LEFT, TURN_RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, ROUNDABOUT, ARRIVE, MERGE, EXIT }

/** A navigation instruction produced by the GPS navigator. */
data class RouteManeuver(
    val kind: ManeuverKind,
    val distanceUnits: Double,
    val instructionRo: String,
    val instructionEn: String,
    val roundaboutExit: Int = 0,
    val speedLimitKmh: Int = 0,
) {
    /** Text used by the voice guidance, e.g. "În 500 de metri, virați la dreapta." */
    fun voicePrompt(showMetres: Boolean = true): String {
        if (kind == ManeuverKind.ARRIVE) return "Ați ajuns la destinație."
        val metres = MapScale.unitsToMetres(distanceUnits).toInt()
        val rounded = if (metres >= 1000) ((metres / 100).toInt() * 100) else (metres / 50).toInt() * 50
        val lead = when {
            !showMetres -> ""
            rounded >= 1000 -> "În ${"%.1f".format(rounded / 1000.0)} kilometri, "
            rounded in 1..49 -> "Acum, "
            else -> "În $rounded de metri, "
        }
        val action = when (kind) {
            ManeuverKind.STRAIGHT -> "continuați înainte"
            ManeuverKind.TURN_LEFT -> "virați la stânga"
            ManeuverKind.TURN_RIGHT -> "virați la dreapta"
            ManeuverKind.SLIGHT_LEFT -> "țineți ușor stânga"
            ManeuverKind.SLIGHT_RIGHT -> "țineți ușor dreapta"
            ManeuverKind.ROUNDABOUT -> "intrați în sensul giratoriu și ieșiți la ieșirea $roundaboutExit"
            ManeuverKind.MERGE -> "intrați pe autostradă"
            ManeuverKind.EXIT -> "ieșiți de pe autostradă"
            ManeuverKind.ARRIVE -> "ajungeți la destinație"
        }
        return "$lead$action."
    }
}

/** A planned route between two cities, produced by the GPS navigator. */
data class RoutePlan(
    val fromCityId: String,
    val toCityId: String,
    /** Polyline (x,z interleaved) in world units, from origin to destination. */
    val points: FloatArray,
    /** Cumulative distance (world units) at each point of [points]; same length as points/2. */
    val cumulativeUnits: FloatArray,
    val totalUnits: Double,
    val maneuvers: List<RouteManeuver>,
    val roadClasses: List<RoadClass>,
    val averageSpeedKmh: Double,
) {
    val totalKm: Double get() = MapScale.unitsToKm(totalUnits)

    fun remainingKm(drivenUnits: Double): Double = MapScale.unitsToKm((totalUnits - drivenUnits).coerceAtLeast(0.0))

    fun remainingMinutes(drivenUnits: Double, speedKmh: Double = averageSpeedKmh): Long =
        ((remainingKm(drivenUnits) / speedKmh.coerceAtLeast(20.0)) * 60.0).toLong()

    fun pointCount(): Int = points.size / 2

    fun pointX(index: Int): Double = points[index * 2].toDouble()
    fun pointZ(index: Int): Double = points[index * 2 + 1].toDouble()

    /** Nearest point on the route to the given position, plus its cumulative distance. */
    fun projectProgress(x: Double, z: Double): Pair<Int, Double> {
        var bestIndex = 0
        var bestDistSq = Double.MAX_VALUE
        for (i in 0 until pointCount()) {
            val dx = pointX(i) - x
            val dz = pointZ(i) - z
            val d = dx * dx + dz * dz
            if (d < bestDistSq) {
                bestDistSq = d
                bestIndex = i
            }
        }
        return bestIndex to cumulativeUnits[bestIndex].toDouble()
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/** Points of interest placed on the map by the world generator. */
data class MapPoi(
    val id: String,
    val kind: PoiKind,
    val cityId: String?,
    val x: Double,
    val z: Double,
    val name: String,
)

enum class PoiKind {
    TRUCK_DEALER, TRAILER_DEALER, GARAGE, SERVICE, FUEL, LOGISTICS_HUB, FACTORY, FARM, PORT, QUARRY, WEIGH_STATION, REST_AREA, TRAFFIC_LIGHT
}
