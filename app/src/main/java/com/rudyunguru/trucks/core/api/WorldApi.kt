package com.rudyunguru.trucks.core.api

import com.rudyunguru.trucks.core.model.City
import com.rudyunguru.trucks.core.model.GameSettings
import com.rudyunguru.trucks.core.model.MapPoi
import com.rudyunguru.trucks.core.model.RouteManeuver
import com.rudyunguru.trucks.core.model.RoutePlan
import com.rudyunguru.trucks.core.model.TriggerKind
import com.rudyunguru.trucks.core.model.WeatherKind
import com.rudyunguru.trucks.core.model.WorldGeometry
import com.rudyunguru.trucks.gl.api.EnvironmentState
import com.rudyunguru.trucks.gl.api.TrafficKind
import com.rudyunguru.trucks.gl.api.TrafficPose

/**
 * Runtime world: the generated static geometry, the road graph queries, the traffic AI, the weather
 * and the day/night cycle.
 *
 * Implemented by ``com.rudyunguru.trucks.world.WorldRuntime``. All query methods must be cheap enough
 * to be called every frame (they are used by the HUD, the police system and the traffic AI).
 */
interface WorldApi {

    /** Deterministic generation of the whole playable map. Called once per driving session. */
    fun generate(seed: Int, qualityTrafficMax: Int, settings: com.rudyunguru.trucks.core.model.GameSettings): WorldGeometry

    /** Cities present in the generated world, in catalog order. */
    fun cities(): List<City>

    /** Points of interest (dealers, fuel, service, depots, weigh stations...). */
    fun pois(): List<MapPoi>

    /** A* route between two cities; null when the cities are disconnected. */
    fun planRoute(fromCityId: String, toCityId: String): RoutePlan?

    /** Snaps arbitrary world coordinates to the nearest road point (used when spawning). */
    fun nearestRoadPoint(x: Double, z: Double): DoubleArray

    /** Posted speed limit at a position (km/h). */
    fun speedLimitAt(x: Double, z: Double): Int

    /** Nearest trigger volume, e.g. a fuel station or depot gate, or null when out on the road. */
    fun triggerAt(x: Double, z: Double, radiusUnits: Double): WorldTrigger?

    /** True when the traffic light controlling this position is red/yellow. */
    fun isTrafficLightBlocking(x: Double, z: Double, headingRad: Double): Boolean

    /** Current signal phase for the visual traffic light meshes. */
    fun trafficLightPhase(id: Int): Int

    /** Spawns / advances / recycles the AI traffic around the player. */
    fun updateTraffic(playerX: Double, playerZ: Double, playerSpeedUnits: Double, dtSeconds: Double)

    /** Live traffic snapshot for the renderer; the list is reused between frames. */
    fun trafficPoses(): List<TrafficPose>

    /** Traffic vehicles near the player that could be hit (used by the collision + police system). */
    fun nearestTrafficDistance(x: Double, z: Double, headingRad: Double): Double

    /** Advances weather + day/night. Returns the environment state for the renderer. */
    fun updateEnvironment(dtSeconds: Double, inGameMinutesDelta: Double): com.rudyunguru.trucks.gl.api.EnvironmentState

    /** Current weather, for the HUD and for grip calculations. */
    fun weather(): WeatherKind

    /** Forces weather (debug / settings screen preview). */
    fun forceWeather(kind: WeatherKind)

    /** Deterministic ambient traffic colour/variant helper shared by AI and renderer. */
    fun trafficVariantFor(kind: TrafficKind, seed: Int): Int

    /** Releases AI pools when the session ends. */
    fun release()
}

/** A trigger volume the truck can enter (fuel station, depot gate, weigh station...). */
class WorldTrigger(
    val id: String,
    val kind: TriggerKind,
    val cityId: String,
    val label: String,
    val x: Double,
    val z: Double,
    val radiusUnits: Double,
    val fuelPriceFactor: Double = 1.0,
)

/** Small helper the GPS uses to answer "what is the next instruction". */
class NavigationState {
    var plan: RoutePlan? = null
    var nextManeuver: RouteManeuver? = null
    var nextManeuverDistanceM: Double = 0.0
    var drivenUnits: Double = 0.0
    var offRoute: Boolean = false
    var arrived: Boolean = false
}
