package com.rudyunguru.trucks.world

import com.rudyunguru.trucks.core.Rng
import com.rudyunguru.trucks.core.api.WorldApi
import com.rudyunguru.trucks.core.api.WorldTrigger
import com.rudyunguru.trucks.core.catalog.CityCatalog
import com.rudyunguru.trucks.core.model.*
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
        
        // 1. Create a large grass base
        ground.add(GroundPatch(GroundKind.GRASS, 0.0, 0.0, 20000.0, 20000.0, 0.0, "#2D5A27"))

        // 2. Place cities and connect them with roads
        val cities = CityCatalog.CITIES
        for (city in cities) {
            // City center patch
            ground.add(GroundPatch(GroundKind.CONCRETE, city.x, city.z, 400.0, 400.0, 0.0, "#444444"))
            
            // Some buildings
            val cityRng = Rng(city.id.hashCode() + seed)
            repeat(15) {
                buildings.add(BuildingBox(
                    city.x + cityRng.range(-150.0, 150.0), 0.0, city.z + cityRng.range(-150.0, 150.0),
                    cityRng.range(10.0, 25.0), cityRng.range(10.0, 25.0), cityRng.range(8.0, 30.0),
                    (cityRng.range(1.0, 8.0)).toInt(), BuildingKind.APARTMENT, "#888888"
                ))
            }
        }

        // 3. Connect cities (simplified: connect each to the next in catalog)
        for (i in cities.indices) {
            val a = cities[i]
            val b = cities[(i + 1) % cities.size]
            val points = floatArrayOf(a.x.toFloat(), a.z.toFloat(), b.x.toFloat(), b.z.toFloat())
            roads.add(RoadStrip(i, RoadClass.NATIONAL, points, 8.0, 100, 1))
        }

        val result = WorldGeometry(seed, -10000.0, -10000.0, 10000.0, 10000.0, 
            roads, buildings, props, ground, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
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
}
