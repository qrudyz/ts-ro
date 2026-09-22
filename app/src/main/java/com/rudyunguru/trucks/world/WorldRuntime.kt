package com.rudyunguru.trucks.world

import com.rudyunguru.trucks.core.Rng
import com.rudyunguru.trucks.core.api.WorldApi
import com.rudyunguru.trucks.core.api.WorldTrigger
import com.rudyunguru.trucks.core.catalog.CityCatalog
import com.rudyunguru.trucks.core.model.*
import com.rudyunguru.trucks.core.model.MapScale
import com.rudyunguru.trucks.gl.api.EnvironmentState
import com.rudyunguru.trucks.gl.api.TrafficKind
import com.rudyunguru.trucks.gl.api.TrafficPose

/**
 * Procedural Romania: road network, cities, scenery and traffic AI.
 */
class WorldRuntime : WorldApi {

    private var geo: WorldGeometry? = null
    private var traffic = ArrayList<TrafficPose>()
    private val environment = EnvironmentState()
    private var timeMinutes = 480.0

    override fun generate(seed: Int, qualityTrafficMax: Int, settings: GameSettings): WorldGeometry {
        val rng = Rng(seed)
        val roads = ArrayList<RoadStrip>()
        val buildings = ArrayList<BuildingBox>()
        val props = ArrayList<PropInstance>()
        val ground = ArrayList<GroundPatch>()

        // 1. Romania's real bounding box (projected, in world units).
        val (wMinX, wMinZ) = MapScale.project(48.30, 20.25)
        val (wMaxX, wMaxZ) = MapScale.project(43.60, 29.75)
        val cy = (wMinZ + wMaxZ) / 2.0

        // 2. Base terrain: plains green, over-painted with Carpathian forest and Wallachian field.
        ground.add(GroundPatch(GroundKind.GRASS, (wMinX + wMaxX) / 2.0, cy,
            wMaxX - wMinX, wMaxZ - wMinZ, 0.0, "#4C7A3F"))

        // Carpathian arc: dense forest band along the mountain arc.
        for (i in CARPATHIAN_ARC.indices step 2) {
            val (ax, az) = MapScale.project(CARPATHIAN_ARC[i], CARPATHIAN_ARC[i + 1])
            ground.add(GroundPatch(GroundKind.FOREST_FLOOR, ax, az, 9000.0, 9000.0, 0.0, "#2E5230"))
        }
        // Wallachian plain / farmland band.
        for (i in 0 until 6) {
            val (ax, az) = MapScale.project(44.3 + i * 0.6, 23.5 + (i % 3) * 1.8)
            ground.add(GroundPatch(GroundKind.PLOWED, ax, az, 7000.0, 7000.0, 0.0, "#7A6A3F"))
        }
        // Danube bank (south border) - sandy strip.
        for (i in 0 until 5) {
            val (ax, az) = MapScale.project(44.1 - i * 0.35, 22.4 + i * 1.4)
            ground.add(GroundPatch(GroundKind.SAND, ax, az, 3500.0, 1200.0, 0.3, "#B8A05A"))
        }
        // Black Sea coast strip.
        ground.add(GroundPatch(GroundKind.SAND, wMaxX - 900.0, wMaxZ - 2500.0, 1400.0, 5000.0, 0.0, "#C9B876"))

        // 3. The real road network (motorways + major national roads, real Romanian geometry).
        var roadId = 0
        for (hw in MOTORWAYS) {
            val pts = ArrayList<Float>()
            for (i in hw.points.indices step 2) {
                val (px, pz) = MapScale.project(hw.points[i], hw.points[i + 1])
                pts.add(px.toFloat()); pts.add(pz.toFloat())
            }
            roads.add(RoadStrip(roadId++, RoadClass.MOTORWAY, pts.toFloatArray(),
                11.5, 130, 2, isBridge = hw.bridges))
            // Lampposts along the motorways.
            for (i in 0 until pts.size / 2 step 6) {
                props.add(PropInstance(PropKind.HIGHWAY_LAMP, pts[i * 2].toDouble() + 8.0, 0.0,
                    pts[i * 2 + 1].toDouble(), 1.0, 0.0, 0, "#9E9E9E"))
            }
        }
        for (nat in NATIONAL_ROADS) {
            val pts = ArrayList<Float>()
            for (i in nat.points.indices step 2) {
                val (px, pz) = MapScale.project(nat.points[i], nat.points[i + 1])
                pts.add(px.toFloat()); pts.add(pz.toFloat())
            }
            roads.add(RoadStrip(roadId++, RoadClass.NATIONAL, pts.toFloatArray(),
                8.0, 100, 1))
            // Guardrails + trees along national roads.
            for (i in 0 until pts.size / 2 step 4) {
                props.add(PropInstance(PropKind.GUARDRAIL, pts[i * 2].toDouble() + 6.0, 0.0,
                    pts[i * 2 + 1].toDouble(), 1.0, 0.0, 0, "#8D9499"))
                if ((i / 4) % 2 == 0) {
                    props.add(PropInstance(PropKind.TREE_DECIDUOUS, pts[i * 2].toDouble() - 14.0, 0.0,
                        pts[i * 2 + 1].toDouble() - 14.0, rng.range(2.0, 4.5), rng.range(0.0, 3.14), 0, "#3D6B2F"))
                }
            }
        }

        // 4. Cities: real urban layouts - downtown grid of blocks + industrial ring.
        for (city in CityCatalog.CITIES) {
            val cityRng = Rng(city.id.hashCode() + seed)
            val size = when (city.tier) {
                CityTier.METROPOLIS -> 900.0
                CityTier.CITY -> 600.0
                CityTier.TOWN -> 420.0
            }
            ground.add(GroundPatch(GroundKind.ASPHALT_LOT, city.x, city.z, size * 2, size * 2, 0.0, "#3A3A3E"))

            val gridN = when (city.tier) {
                CityTier.METROPOLIS -> 7
                CityTier.CITY -> 5
                CityTier.TOWN -> 4
            }
            for (gx in 0 until gridN) {
                for (gz in 0 until gridN) {
                    if (cityRng.bool(0.25)) continue
                    val bx = city.x + (gx - gridN / 2.0) * (size / gridN) * 1.6 + cityRng.range(-20.0, 20.0)
                    val bz = city.z + (gz - gridN / 2.0) * (size / gridN) * 1.6 + cityRng.range(-20.0, 20.0)
                    val isDowntown = Math.abs(gx - gridN / 2) <= 1 && Math.abs(gz - gridN / 2) <= 1
                    val kind = when {
                        isDowntown && city.tier == CityTier.METROPOLIS -> BuildingKind.OFFICE
                        cityRng.bool(0.55) -> BuildingKind.APARTMENT
                        cityRng.bool(0.5) -> BuildingKind.HOUSE
                        else -> BuildingKind.SHOP
                    }
                    val floors = when {
                        isDowntown -> cityRng.range(4, 12)
                        kind == BuildingKind.HOUSE -> cityRng.range(1, 2)
                        else -> cityRng.range(2, 6)
                    }.toInt()
                    val color = when (kind) {
                        BuildingKind.OFFICE -> "#6E7B8B"
                        BuildingKind.APARTMENT -> cityRng.pick(listOf("#B0876A", "#9C8468", "#8B8B7A", "#A67B5B"))
                        BuildingKind.HOUSE -> cityRng.pick(listOf("#C19A6B", "#B5651D", "#7B6B4B"))
                        else -> "#7D8A97"
                    }
                    buildings.add(BuildingBox(bx, 0.0, bz,
                        cityRng.range(18.0, 45.0), cityRng.range(18.0, 45.0),
                        floors * 3.2, floors, kind, color,
                        rotationRad = cityRng.range(-0.2, 0.2)))
                }
            }

            // Industrial ring on the outskirts.
            repeat(if (city.tier == CityTier.METROPOLIS) 8 else 4) {
                val ang = cityRng.range(0.0, Math.PI * 2)
                val dist = size * 1.3
                buildings.add(BuildingBox(
                    city.x + Math.cos(ang) * dist, 0.0, city.z + Math.sin(ang) * dist,
                    cityRng.range(40.0, 90.0), cityRng.range(30.0, 60.0),
                    cityRng.range(8.0, 16.0), 1, BuildingKind.WAREHOUSE, "#5C6B73"))
            }

            // Urban street through the city, connecting to the road network.
            val streetPts = floatArrayOf(
                (city.x - size).toFloat(), city.z.toFloat(),
                (city.x + size).toFloat(), city.z.toFloat())
            roads.add(RoadStrip(roadId++, RoadClass.CITY, streetPts, 7.5, 50, 1, cityId = city.id))
        }

        // 5. Scenic highlights (renderer adds extra props along these).
        val scenic = ArrayList<ScenicRoute>()
        fun px(lat: Double, lon: Double) = MapScale.project(lat, lon).first.toFloat()
        fun pz(lat: Double, lon: Double) = MapScale.project(lat, lon).second.toFloat()
        scenic.add(ScenicRoute("Transfăgărășan", ScenicKind.MOUNTAIN_PASS,
            floatArrayOf(px(45.60, 24.62), pz(45.60, 24.62), px(45.48, 24.63), pz(45.48, 24.63))))
        scenic.add(ScenicRoute("Valea Prahovei", ScenicKind.VALLEY,
            floatArrayOf(px(45.35, 25.55), pz(45.35, 25.55), px(45.55, 25.58), pz(45.55, 25.58))))
        scenic.add(ScenicRoute("Litoral", ScenicKind.COAST,
            floatArrayOf(px(44.20, 28.65), pz(44.20, 28.65), px(43.75, 28.60), pz(43.75, 28.60))))
        scenic.add(ScenicRoute("Defileul Dunării", ScenicKind.RIVER_BRIDGE,
            floatArrayOf(px(44.63, 22.35), pz(44.63, 22.35), px(44.35, 22.55), pz(44.35, 22.55))))

        val result = WorldGeometry(seed, wMinX, wMinZ, wMaxX, wMaxZ,
            roads, buildings, props, ground, emptyList(), emptyList(), emptyList(),
            CityCatalog.CITIES.map { CityCenter(it.id, it.x, it.z, 500.0, hasRingRoad = true) }, scenic)
        this.geo = result
        return result
    }

    override fun cities(): List<City> = CityCatalog.CITIES
    override fun pois(): List<MapPoi> = emptyList()
    override fun planRoute(fromCityId: String, toCityId: String): RoutePlan? {
        val f = CityCatalog.byId(fromCityId)
        val t = CityCatalog.byId(toCityId)
        val pts = floatArrayOf(f.x.toFloat(), f.z.toFloat(), t.x.toFloat(), t.z.toFloat())
        return RoutePlan(fromCityId, toCityId, pts, floatArrayOf(0f, f.distanceKmTo(t).toFloat()), 
            f.distanceKmTo(t), emptyList(), listOf(RoadClass.NATIONAL), 60.0)
    }

    override fun nearestRoadPoint(x: Double, z: Double): DoubleArray = doubleArrayOf(x, z)
    override fun speedLimitAt(x: Double, z: Double): Int = 90
    override fun triggerAt(x: Double, z: Double, radiusUnits: Double): WorldTrigger? = null
    override fun isTrafficLightBlocking(x: Double, z: Double, headingRad: Double): Boolean = false
    override fun trafficLightPhase(id: Int): Int = 0
    override fun updateTraffic(playerX: Double, playerZ: Double, playerSpeedUnits: Double, dtSeconds: Double) {}
    override fun trafficPoses(): List<TrafficPose> = traffic
    override fun nearestTrafficDistance(x: Double, z: Double, headingRad: Double): Double = 1000.0

    override fun updateEnvironment(dtSeconds: Double, inGameMinutesDelta: Double): EnvironmentState {
        timeMinutes += inGameMinutesDelta
        val t = (timeMinutes % 1440.0) / 1440.0
        environment.timeOfDay01 = t.toFloat()
        // Sun elevation: peaks at noon (0.5), lowest at midnight (0, 1)
        environment.sunElevationRad = (Math.sin(t * Math.PI * 2 - Math.PI / 2) * 1.2).toFloat()
        return environment
    }

    override fun weather(): WeatherKind = WeatherKind.CLEAR
    override fun forceWeather(kind: WeatherKind) {}
    override fun trafficVariantFor(kind: TrafficKind, seed: Int): Int = 0
    override fun release() {}

    companion object {
        /** One real road: [lat, lon, lat, lon, ...] in degrees, real Romanian geometry. */
        class Road(val points: DoubleArray, val bridges: Boolean = false)

        // A1 Bucharest - Pitești - Sibiu - Deva - Timișoara / Arad (real route).
        private val A1 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.3500, 25.7000, 44.8565, 24.8692, 45.0300, 24.4500,
            45.7983, 24.1256, 45.8300, 23.5500, 45.9000, 23.0500, 46.0300, 22.5500,
            45.7489, 21.2087, 46.1866, 21.3123), bridges = true)

        // A2 Bucharest - Drajov - Fetești - Cernavodă - Constanța (the "Autostrada Soarelui").
        private val A2 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.3200, 26.7500, 44.2900, 27.4000, 44.3200, 28.0000,
            44.1598, 28.6348), bridges = true)

        // A3 Bucharest - Ploiești - Brașov (partial, real alignment).
        private val A3 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.9366, 26.0129, 45.3000, 25.7000, 45.6579, 25.6012))

        // A10 Sebeș - Turda (Transilvania).
        private val A10 = Road(doubleArrayOf(
            45.7983, 24.1256, 45.9500, 23.8500, 46.5800, 23.8000, 46.7712, 23.6236))

        // DN1 Bucharest - Ploiești - Brașov - Sibiu (Valea Prahovei).
        private val DN1 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.9366, 26.0129, 45.3550, 25.5550, 45.6579, 25.6012,
            45.8700, 25.2000, 45.7983, 24.1256))

        // DN7 Nădlac - Arad - Deva - Sibiu - Valea Oltului - București.
        private val DN7 = Road(doubleArrayOf(
            46.1500, 20.7500, 46.1866, 21.3123, 45.9500, 22.3500, 45.8800, 22.9000,
            45.7983, 24.1256, 45.4800, 24.4500, 45.1500, 24.8000, 44.8500, 25.2000,
            44.4268, 26.1025))

        // E85 / DN2 Bucharest - Buzău - Focșani - Bacău - Suceava.
        private val DN2 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.9300, 26.7000, 45.4200, 27.0000, 45.6900, 27.1900,
            46.5670, 26.9146, 47.2000, 26.6000, 47.6635, 26.2594))

        // DN1B / E584 Ploiești - Buzău - Galați.
        private val DNGALATI = Road(doubleArrayOf(
            44.9366, 26.0129, 45.1500, 26.8500, 45.4300, 27.7000, 45.4353, 28.0080))

        // E70 / DN6 București - Alexandria - Craiova - Timișoara.
        private val DN6 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.1000, 25.3500, 44.3302, 23.7949, 44.5500, 22.9000,
            45.3000, 22.1000, 45.7489, 21.2087))

        // E79 / DN1C Cluj - Oradea.
        private val DNCLOR = Road(doubleArrayOf(
            46.7712, 23.6236, 46.9500, 22.9000, 47.0465, 21.9189))

        // E81 / DN1C7 Cluj - Târgu Mureș - Iași corridor (DN15 + E583 segment).
        private val DNCLIAI = Road(doubleArrayOf(
            46.7712, 23.6236, 46.5386, 24.5573, 46.9000, 26.3000, 47.1585, 27.6014))

        // DN5 Bucharest - Giurgiu (Dunăre).
        private val DN5 = Road(doubleArrayOf(
            44.4268, 26.1025, 44.1500, 25.9800, 43.9000, 25.9700))

        // Carpathian mountain arc (Bend - Făgăraș - Parâng - Banat), for the forest band.
        private val CARPATHIAN_ARC = doubleArrayOf(
            45.3500, 25.9000, 45.5500, 25.3000, 45.6000, 24.9000, 45.6500, 24.6000,
            45.7500, 24.4000, 45.6000, 24.2000, 45.4500, 23.9000, 45.3500, 23.6000,
            45.2500, 23.3000, 45.2000, 23.0000, 45.3500, 22.6000, 45.2500, 22.3000)

        private val MOTORWAYS = listOf(A1, A2, A3, A10)
        private val NATIONAL_ROADS = listOf(DN1, DN7, DN2, DNGALATI, DN6, DNCLOR, DNCLIAI, DN5)
    }
}
