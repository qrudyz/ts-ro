package com.rudyunguru.trucks.core.model

/**
 * Geometry produced by the world generator and consumed by the OpenGL renderer.
 *
 * The generator (Kotlin) produces pure data - polylines, boxes, instances - and the renderer (Java)
 * turns it into batched GPU meshes. Keeping them apart means the whole world can be unit tested on
 * the JVM without an OpenGL context, and assets/materials can be swapped without touching gameplay.
 */
class WorldGeometry(
    val seed: Int,
    val minX: Double,
    val minZ: Double,
    val maxX: Double,
    val maxZ: Double,
    val roads: List<RoadStrip>,
    val buildings: List<BuildingBox>,
    val props: List<PropInstance>,
    val ground: List<GroundPatch>,
    val trafficLights: List<TrafficLightSpot>,
    val gates: List<GateTrigger>,
    val fuelStations: List<StationSpot>,
    val cityCenters: List<CityCenter>,
    val scenicRoutes: List<ScenicRoute>,
) {
    val sizeX: Double get() = maxX - minX
    val sizeZ: Double get() = maxZ - minZ

    /** Rough triangle estimate, used to decide mesh batching and to feed the quality presets. */
    fun estimatedTriangles(): Int {
        var tris = 0
        for (road in roads) tris += road.pointCount() * 4
        for (b in buildings) tris += 36 * b.floors.coerceAtMost(12)
        for (p in props) tris += 80
        for (g in ground) tris += 2
        return tris
    }
}

/** One drivable stretch of road: a polyline with width and surface properties. */
class RoadStrip(
    val id: Int,
    val roadClass: RoadClass,
    /** Interleaved x,z pairs in world units. */
    val points: FloatArray,
    val widthMetres: Double,
    val speedLimitKmh: Int,
    val lanesPerDirection: Int,
    val isTunnel: Boolean = false,
    val isBridge: Boolean = false,
    /** City id when the strip belongs to an urban layout. */
    val cityId: String? = null,
    val oneWay: Boolean = false,
) {
    fun pointCount(): Int = points.size / 2
    fun x(i: Int): Double = points[i * 2].toDouble()
    fun z(i: Int): Double = points[i * 2 + 1].toDouble()
}

/** One building footprint: axis aligned box, tinted per district. */
class BuildingBox(
    val x: Double,
    val y: Double,
    val z: Double,
    val width: Double,
    val depth: Double,
    val height: Double,
    val floors: Int,
    val kind: BuildingKind,
    val colorHex: String,
    val rotationRad: Double = 0.0,
    val hasLitWindows: Boolean = true,
    val roofKind: RoofKind = RoofKind.FLAT,
)

enum class BuildingKind(val label: String) {
    APARTMENT("Bloc"),
    HOUSE("Casă"),
    OFFICE("Birou"),
    SHOP("Magazin"),
    WAREHOUSE("Hală"),
    FACTORY("Fabrica"),
    FARM_BUILDING("Anexă fermă"),
    CHURCH("Biserică"),
    HOTEL("Hotel"),
    GARAGE("Garaj"),
}

enum class RoofKind { FLAT, GABLE, SHED, HIP }

/** Scattered decoration: trees, bushes, rocks, lamps, signs, fences. */
class PropInstance(
    val kind: PropKind,
    val x: Double,
    val z: Double,
    val y: Double,
    val scale: Double,
    val rotationRad: Double,
    val variant: Int,
    val colorHex: String,
)

enum class PropKind {
    TREE_DECIDUOUS, TREE_CONIFER, BUSH, ROCK, STREET_LAMP, HIGHWAY_LAMP, ROAD_SIGN, BILLBOARD,
    GUARDRAIL, FENCE, POWER_POLE, BENCH, HAY_BALE, SILO, WATER_TOWER, CHIMNEY, CRANE, PARKED_CAR,
}

/** Large ground patch (field, forest floor, grass verge, parking lot, concrete apron). */
class GroundPatch(
    val kind: GroundKind,
    val x: Double,
    val z: Double,
    val width: Double,
    val depth: Double,
    val rotationRad: Double = 0.0,
    val colorHex: String = "",
    val variant: Int = 0,
)

enum class GroundKind { GRASS, FIELD, FOREST_FLOOR, PLOWED, ASPHALT_LOT, CONCRETE, GRAVEL, SAND }

/** Traffic light head placement + lane binding. */
class TrafficLightSpot(
    val id: Int,
    val x: Double,
    val z: Double,
    val rotationRad: Double,
    /** Index of the road strip it controls (for AI queries). */
    val roadStripIndex: Int,
    val phasesSeconds: FloatArray,
)

/** Trigger volume at depots / gates / job pickup points. */
class GateTrigger(
    val id: String,
    val cityId: String,
    val kind: TriggerKind,
    val x: Double,
    val z: Double,
    val radiusUnits: Double,
    val label: String,
)

enum class TriggerKind { JOB_PICKUP, JOB_DROPOFF, FUEL, SERVICE, DEALER, GARAGE, PARKING, WEIGH_STATION, GATE, CITY_ENTRY }

/** Fuel station position + canopy orientation. */
class StationSpot(
    val id: String,
    val cityId: String,
    val x: Double,
    val z: Double,
    val rotationRad: Double,
    val fuelPriceFactor: Double,
    val label: String,
)

/** City center used by the map screen and by city-entry triggers. */
class CityCenter(
    val cityId: String,
    val x: Double,
    val z: Double,
    val radiusUnits: Double,
    val hasRingRoad: Boolean = false,
)

/** A scenic stretch (mountain pass, bridges, coast road) - the renderer adds extra props there. */
class ScenicRoute(
    val name: String,
    val kind: ScenicKind,
    val points: FloatArray,
)

enum class ScenicKind { MOUNTAIN_PASS, VALLEY, RIVER_BRIDGE, COAST, FOREST, PLAIN }
