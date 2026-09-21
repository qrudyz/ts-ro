package com.rudyunguru.trucks.state

import com.rudyunguru.trucks.core.api.*
import com.rudyunguru.trucks.core.catalog.TruckCatalog
import com.rudyunguru.trucks.core.model.*
import com.rudyunguru.trucks.gl.api.*
import com.rudyunguru.trucks.physics.*
import com.rudyunguru.trucks.world.WorldRuntime
import com.rudyunguru.trucks.core.model.MapScale

/**
 * Orchestrates the driving experience.
 */
class GameSession(
    private val state: GameState,
    private val audio: AudioApi
) : SessionApi {

    override val telemetry = Telemetry()
    override val world = WorldRuntime()
    override var isRunning = false
        private set
    override var truckInstanceId: String? = null
    override var activeJob: ActiveJob? = null

    private var physics: TruckPhysics? = null
    private val input = VehicleInput()
    private var cameraMode = CameraMode.EXTERIOR_BACK

    override fun start(truckInstanceId: String, job: ActiveJob?) {
        this.truckInstanceId = truckInstanceId
        this.activeJob = job
        
        val ownedTruck = state.ownedTruck(truckInstanceId) ?: return
        val model = TruckCatalog.byId(ownedTruck.modelId)
        
        val spec = VehicleSpec().apply {
            modelId = model.id
            curbMassKg = model.curbWeightKg.toDouble()
            maxGrossMassKg = model.maxGrossWeightKg.toDouble()
            powerHp = model.powerHp.toDouble()
            torqueNm = model.torqueNm.toDouble()
            fuelL = ownedTruck.fuelL
            tankCapacityL = model.fuelTankL.toDouble()
            baseConsumptionL100 = model.baseConsumptionL100
        }
        
        physics = TruckPhysics(spec).apply {
            val city = com.rudyunguru.trucks.core.catalog.CityCatalog.byId(ownedTruck.garageCityId)
            reset(city.x, 0.0, city.z, 0.0, 0.0)
        }
        
        world.generate(1337, 20, state.settings)
        isRunning = true
    }

    override fun end() {
        isRunning = false
    }

    override fun pause() { isRunning = false }
    override fun resume() { isRunning = true }

    override fun update(dtSeconds: Double) {
        if (!isRunning) return
        val p = physics ?: return
        
        p.update(input, dtSeconds, 1.0, 0.0)
        world.updateEnvironment(dtSeconds, dtSeconds / 60.0)
        world.updateTraffic(p.state().x, p.state().z, p.state().speedUnits, dtSeconds)
        
        // Update telemetry
        val ps = p.state()
                telemetry.apply {
            speedKmh = ps.speedUnits * 3.6
            rpm = ps.engineRpm
            gearboxLabel = p.gearLabel()
            fuelLiters = ps.fuelL
            fuelCapacityLiters = p.spec().tankCapacityL
            fuelPercent = if (p.spec().tankCapacityL > 0) (ps.fuelL / p.spec().tankCapacityL * 100.0).coerceIn(0.0, 100.0) else 0.0
            fuelRangeKm = if (ps.litersPer100Km > 0) ps.fuelL / ps.litersPer100Km * 100.0 else 0.0
            engineTemperatureC = ps.engineTemperatureC
            oilTemperatureC = ps.oilTemperatureC
            airPressureBar = ps.airPressureBar
            odometerKm = MapScale.unitsToKm(ps.x).takeIf { it > 0 } ?: 0.0
            consumptionL100 = ps.litersPer100Km
                        loadTons = p.spec().cargoMassKg / 1000.0
            totalMassTons = ps.totalMassKg / 1000.0
            positionX = ps.x
            positionZ = ps.z
            headingRad = ps.heading
            gear = ps.gear
            gearCount = 12
            trailerAttached = ps.trailerAttached
            trailerAngleRad = ps.trailerAngleRad
        }
    }

    override fun setThrottle(value: Double) { input.throttle = value }
    override fun setBrake(value: Double) { input.brake = value }
    override fun setSteering(value: Double) { input.steering = value }
    override fun setHandbrake(on: Boolean) { input.handbrake = on }
    override fun setRetarder(level: Int) { input.retarder = level }
    override fun toggleEngineBrake() { input.engineBrake = !input.engineBrake }
    override fun setCruiseControl(speedKmh: Double) { input.cruiseControlKmh = speedKmh }
    override fun shiftUp() { input.shiftUpRequested = true }
    override fun shiftDown() { input.shiftDownRequested = true }
    override fun setGearboxAutomatic(automatic: Boolean) { input.automaticGearbox = automatic }
    
    override fun toggleHeadlights() {}
    override fun toggleHighBeam() {}
    override fun toggleHazards() {}
    override fun setIndicator(left: Boolean, right: Boolean) {}
    override fun toggleWipers() {}
    override fun setHorn(on: Boolean) {}
    override fun cycleCamera() {
        cameraMode = cameraMode.next()
    }
    override fun setCamera(preset: CameraPreset) {
        cameraMode = CameraMode.fromName(preset.name)
    }

    override fun refuel(litres: Double): Long = 0
    override fun repair(): Long = 0
    override fun deliver(): DeliveryResult? = null
    override fun abandonJob() {}
    override fun fastTravelTo(cityId: String): Long = 0

    override fun playerPose(): VehiclePose {
        val ps = physics?.state() ?: VehicleState()
        return VehiclePose().apply {
            x = ps.x.toFloat()
            y = ps.y.toFloat()
            z = ps.z.toFloat()
            headingRad = ps.heading.toFloat()
            speedUnits = ps.speedUnits.toFloat()
            engineRpm = ps.engineRpm.toFloat()
            gear = ps.gear
        }
    }

    override fun environment(): EnvironmentState = world.updateEnvironment(0.0, 0.0)
    override fun routeProgress(): Double = 0.0
}
