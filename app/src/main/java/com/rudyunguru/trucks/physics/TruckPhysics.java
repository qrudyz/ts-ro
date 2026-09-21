package com.rudyunguru.trucks.physics;

/**
 * Longitudinal + lateral truck dynamics for one tractor (optionally pulling a semi-trailer).
 *
 * Everything is SI: metres, kilograms, seconds, newtons. World units in the renderer are metres, so
 * the truck is 16.5 m long and 90 km/h is 25 m/s. The *map* is compressed 20x in distance (see
 * MapScale), which is why displayed kilometres are `units * 20 / 1000`.
 *
 * The model implements what a truck simulator needs to feel right:
 *  - torque curve + power limit, 12-speed gearbox with torque interruption while shifting;
 *  - automatic/automated shifting with kick-down, manual shifting and a gear limiter;
 *  - service brakes with temperature fade, retarder stages and engine brake;
 *  - rolling resistance, aerodynamic drag and gradient force;
 *  - speed-sensitive steering, lateral grip limit with understeer and tyre slip;
 *  - semi-trailer articulation with a kinematic hitch, off-tracking and a jack-knife limit;
 *  - fuel burn as a function of load, speed, gradient, weather and wear;
 *  - engine/oil temperature and air pressure management.
 *
 * The class has no Android dependency at all, so the whole handling model is unit tested on the JVM.
 */
public final class TruckPhysics {

    private static final double G = 9.81;
    private static final double AIR_DENSITY = 1.225;
    /** Drag * area for a tractor + semi-trailer (m^2), i.e. a Cd of ~0.55 over 11.7 m^2. */
    private static final double DRAG_AREA = 6.6;
    private static final double ROLLING_COEFF = 0.0062;
    /** Maximum lateral acceleration a loaded truck can take before sliding (g). */
    private static final double MAX_LATERAL_G = 0.34;
    private static final double WHEELBASE_M = 3.75;

    private final VehicleSpec spec;
    private final VehicleState state = new VehicleState();

    /** Gear ratios, index 0 = reverse. */
    private final double[] gearRatios;

    private double trailerMassKg;
    private boolean trailerAttached;
    private double trailerLengthM = 13.6;

    private double engineTorqueSmoothed;
    private double shiftTimer;
    private int targetGear = 1;
    private double cruiseIntegrator;
    private double idleTimer;
    private double odometerUnits;
    private double fuelUsedL;
    private double throttleSmoothed;
    private double brakeSmoothed;
    private double steerSmoothed;
    private double lastSpeedForAccel;
    private double slipTimer;

    public TruckPhysics(VehicleSpec spec) {
        this.spec = spec;
        this.gearRatios = buildGearRatios(Math.max(2, spec.gearCount));
        reset(0, 0, 0, 0, 0);
    }

    /** Geometric ratio spread: reverse 4.2:1, 1st 6.4:1 down to 0.78:1 overdrive. */
    private static double[] buildGearRatios(int gears) {
        double[] ratios = new double[gears + 1];
        ratios[0] = -4.2; // reverse
        double first = 6.4;
        double last = 0.78;
        double factor = Math.pow(last / first, 1.0 / Math.max(1, gears - 1));
        double r = first;
        for (int i = 1; i <= gears; i++) {
            ratios[i] = r;
            r *= factor;
        }
        return ratios;
    }

    public VehicleSpec spec() {
        return spec;
    }

    public VehicleState state() {
        return state;
    }

    public double odometerUnits() {
        return odometerUnits;
    }

    public double fuelUsedL() {
        return fuelUsedL;
    }

    public void setFuel(double litres) {
        state.fuelL = Math.max(0, Math.min(spec.tankCapacityL, litres));
    }

    /** Attaches/detaches a semi-trailer; affects mass, articulation and jack-knife behaviour. */
    public void setTrailer(boolean attached, double massKg, double lengthM) {
        this.trailerAttached = attached;
        this.trailerMassKg = attached ? massKg : 0;
        this.trailerLengthM = lengthM;
        state.trailerAttached = attached;
        if (attached) {
            state.trailerX = state.x;
            state.trailerZ = state.z;
            state.trailerHeading = state.heading;
        }
    }

    public void setCargoMass(double cargoMassKg) {
        spec.cargoMassKg = Math.max(0, cargoMassKg);
    }

    /** Places the truck in the world, cancelling all motion. */
    public void reset(double x, double y, double z, double heading, double speedUnits) {
        state.x = x;
        state.y = y;
        state.z = z;
        state.heading = heading;
        state.speedUnits = speedUnits;
        state.longitudinalSpeedUnits = speedUnits;
        state.lateralSpeedUnits = 0;
        state.accelerationUnits = 0;
        state.yawRate = 0;
        state.bodyRoll = 0;
        state.bodyPitch = 0;
        state.steeringAngleRad = 0;
        state.wheelSpinRad = 0;
        state.slipRatio = 0;
        state.slipAngle01 = 0;
        state.engineRpm = spec.idleRpm;
        state.gear = 1;
        state.shifting = false;
        state.engineLoad01 = 0;
        state.torqueOutputNm = 0;
        state.brakeForceN = 0;
        state.parkingBrake = true;
        state.brakeTemperature01 = 0;
        state.fuelL = Math.min(state.fuelL <= 0 ? spec.tankCapacityL * 0.6 : state.fuelL, spec.tankCapacityL);
        state.litersPer100Km = 0;
        state.engineTemperatureC = 20;
        state.oilTemperatureC = 20;
        state.airPressureBar = 8.2;
        state.trailerX = x;
        state.trailerZ = z;
        state.trailerHeading = heading;
        state.trailerAngleRad = 0;
        state.jackknifed = false;
        targetGear = 1;
        shiftTimer = 0;
        engineTorqueSmoothed = 0;
        cruiseIntegrator = 0;
        throttleSmoothed = 0;
        brakeSmoothed = 0;
        steerSmoothed = 0;
        lastSpeedForAccel = speedUnits;
    }

    // ---------------------------------------------------------------- main integration step

    /**
     * Advances the truck by [dtSeconds] with the given input.
     *
     * @param gripMultiplier road surface grip (1 = dry asphalt, 0.8 = rain, 0.6 = grass/mud)
     * @param gradePercent    road gradient in percent (positive = uphill)
     */
    public void update(VehicleInput in, double dtSeconds, double gripMultiplier, double gradePercent) {
        if (dtSeconds <= 0) return;
        double dt = Math.min(0.05, dtSeconds);
        spec.trailerMassKg = trailerAttached ? trailerMassKg : 0;
        double mass = spec.totalMassKg();
        double grip = clamp(gripMultiplier, 0.35, 1.2) * clamp(spec.tireGrip, 0.5, 1.15);

        // --- 1. input smoothing (a real pedal/wheel never moves instantly) ---
        throttleSmoothed = approach(throttleSmoothed, clamp(in.throttle, 0, 1), 3.2 * dt);
        brakeSmoothed = approach(brakeSmoothed, clamp(in.brake, 0, 1), 5.0 * dt);
        steerSmoothed = approach(steerSmoothed, clamp(in.steering, -1, 1), 3.4 * dt);

        // --- 2. cruise control ---
        double throttle = throttleSmoothed;
        double brake = brakeSmoothed;
        double cruiseTarget = clamp(in.cruiseControlKmh, 0, spec.maxSpeedKmh);
        if (cruiseTarget > 1) {
            double error = cruiseTarget / 3.6 - state.speedUnits;
            if (error > 0.02) {
                cruiseIntegrator = clamp(cruiseIntegrator + error * 0.35 * dt, 0, 0.35);
                throttle = clamp(0.25 + error * 0.12 + cruiseIntegrator, 0, 1);
                brake = 0;
            } else if (error < -0.39) {
                throttle = 0;
                brake = clamp(-error * 0.28, 0, 0.55);
                cruiseIntegrator = clamp(cruiseIntegrator - 0.4 * dt, 0, 0.35);
            } else {
                throttle = clamp(0.14 + cruiseIntegrator, 0, 0.45);
                brake = 0;
            }
        } else {
            cruiseIntegrator = 0;
        }

        // --- 3. gearbox ---
        double ratio = gearRatioFor(state.gear);
        boolean clutchPressed = in.clutch && !in.automaticGearbox;
        if (shiftTimer > 0) {
            shiftTimer -= dt;
            state.shifting = shiftTimer > 0;
        } else {
            state.shifting = false;
            if (!clutchPressed) {
                if (in.shiftUpRequested) requestShift(state.gear + 1, in);
                if (in.shiftDownRequested) requestShift(state.gear - 1, in);
                if (in.automaticGearbox) automaticShift(in, throttle, brake);
            }
            ratio = gearRatioFor(state.gear);
        }
        if (clutchPressed) ratio = 0;

        updateEngine(dt, ratio, throttle);
        updateLongitudinalMotion(dt, mass, grip, ratio, throttle, brake, in, gradePercent);
        updateLateralMotion(dt, mass, grip);
        updateTrailer(dt);
        updateSystems(dt, mass, throttle, brake, in, speedAbs());
    }

    private double speedAbs() {
        return Math.abs(state.speedUnits);
    }

    // ---------------------------------------------------------------- engine & gearbox

    /** Torque curve: a broad plateau around [VehicleSpec.peakTorqueRpm] falling off at both ends. */
    private double torqueAtRpm(double rpm) {
        double peak = Math.max(600, spec.peakTorqueRpm);
        double maxTorque = spec.effectiveTorqueNm();
        if (rpm < spec.idleRpm) return maxTorque * 0.45;
        double x = (rpm - peak) / peak;
        double falloff = 1.0 - 1.15 * x * x;
        double torque = maxTorque * clamp(falloff, 0.25, 1.0);
        double omega = Math.max(1.0, rpm * 2 * Math.PI / 60.0);
        double maxByPower = spec.effectivePowerHp() * 745.7 / omega;
        return Math.min(torque, maxByPower);
    }

    private void updateEngine(double dt, double ratio, double throttle) {
        double wheelRpmFactor = 60.0 / (2 * Math.PI * spec.tireRadiusMeters);
        double drivenRatio = ratio * spec.finalDriveRatio;
        double gearedRpm = Math.abs(state.speedUnits) * drivenRatio * wheelRpmFactor;
        double targetRpm;
        if (Math.abs(drivenRatio) < 0.01) {
            targetRpm = spec.idleRpm + (spec.maxRpm - spec.idleRpm) * throttle * 0.85;
        } else if (state.speedUnits > 0.4 || !spec.automaticGearbox) {
            targetRpm = clamp(gearedRpm, spec.idleRpm, spec.maxRpm * 1.02);
        } else {
            targetRpm = spec.idleRpm + (spec.maxRpm - spec.idleRpm) * throttle * 0.55;
        }
        state.engineRpm = approach(state.engineRpm, targetRpm, 900 * dt);
        if (state.engineRpm < spec.idleRpm + 30 && throttle < 0.05) {
            state.engineRpm = approach(state.engineRpm, spec.idleRpm + 40, 450 * dt);
            idleTimer += dt;
        } else {
            idleTimer = 0;
        }
        state.stalled = false;
    }

    private double gearRatioFor(int gear) {
        int index = (int) clamp(gear, 0, gearRatios.length - 1);
        return gearRatios[index];
    }

    private void requestShift(int gear, VehicleInput in) {
        int maxGear = Math.min(gearRatios.length - 1, in.gearLimit > 0 ? in.gearLimit : gearRatios.length - 1);
        int target = (int) clamp(gear, 0, maxGear);
        if (target == state.gear) return;
        state.gear = target;
        shiftTimer = spec.shiftTimeSeconds * (1 - 0.15 * spec.wear01);
    }

    /**
     * Automatic/automated shifting: upshift when the engine revs past the economical window under
     * load, downshift when it drops below the torque plateau, kick-down on a full pedal.
     */
    private void automaticShift(VehicleInput in, double throttle, double brake) {
        double rpm = state.engineRpm;
        int maxGear = Math.min(gearRatios.length - 1, in.gearLimit > 0 ? in.gearLimit : gearRatios.length - 1);
        double upshiftRpm = throttle > 0.85 ? 1950 : (throttle > 0.35 ? 1750 : 1550);
        double downshiftRpm = throttle > 0.7 ? 1150 : 1000;
        if (state.gear < maxGear && rpm > upshiftRpm && throttle > 0.12) {
            state.gear++;
            shiftTimer = spec.shiftTimeSeconds;
        } else if (state.gear > 1 && rpm < downshiftRpm && brake < 0.4) {
            state.gear--;
            shiftTimer = spec.shiftTimeSeconds * 0.85;
        } else if (state.speedUnits < 0.6 && state.gear > 1 && throttle < 0.1) {
            state.gear = 1;
        }
    }

    // ---------------------------------------------------------------- longitudinal dynamics

    private void updateLongitudinalMotion(double dt, double mass, double grip, double ratio,
                                          double throttle, double brake, VehicleInput in,
                                          double gradePercent) {
        double torque = torqueAtRpm(state.engineRpm) * throttle;
        if (shiftTimer > 0 || Math.abs(ratio) < 0.01) torque = 0;
        double overheat = state.engineTemperatureC > 108
                ? 1 - clamp((state.engineTemperatureC - 108) / 12.0, 0, 0.6) : 1;
        torque *= overheat;
        engineTorqueSmoothed = approach(engineTorqueSmoothed, torque, 9 * dt);
        state.torqueOutputNm = engineTorqueSmoothed;
        state.engineLoad01 = clamp(engineTorqueSmoothed / Math.max(1, spec.effectiveTorqueNm()), 0, 1);

        double efficiency = 0.90;
        double drivenRatio = ratio * spec.finalDriveRatio;
        double engineForce = engineTorqueSmoothed * drivenRatio * efficiency / spec.tireRadiusMeters;
        state.engineForceN = engineForce;

        double speedAbs = Math.abs(state.speedUnits);
        double forwardSign = state.speedUnits >= 0 ? 1 : -1;
        double drag = 0.5 * AIR_DENSITY * DRAG_AREA * speedAbs * speedAbs;
        double rolling = speedAbs > 0.05 ? ROLLING_COEFF * mass * G : 0;
        double grade = mass * G * Math.sin(Math.atan(gradePercent / 100.0));
        state.dragForceN = drag;
        state.rollingResistanceN = rolling;
        state.gradeForceN = grade;

        double maxDecel = 5.4 * clamp(spec.brakeEfficiency, 0.4, 1.2)
                * airPressureFactor() * brakeTemperatureFactor();
        double pedalBrake = clamp(brake, 0, 1) * maxDecel * mass;
        double retarderBrake = clamp(in.retarder, 0, 3) * 0.34 * mass
                * clamp(spec.brakeEfficiency, 0.5, 1.0);
        double engineBrakeForce = in.engineBrake
                ? spec.engineBrakeTorqueNm * Math.abs(drivenRatio) * efficiency / spec.tireRadiusMeters
                : 0;
        double handbrakeForce = in.handbrake ? mass * G * 0.5 : 0;
        double brakeForce = pedalBrake + retarderBrake + engineBrakeForce + handbrakeForce;
        if (state.parkingBrake && speedAbs < 0.2 && throttle < 0.05) brakeForce += mass * G * 0.6;
        state.brakeForceN = brakeForce;
        state.parkingBrake = in.handbrake;

        double net = engineForce - drag * forwardSign - rolling * forwardSign - grade - brakeForce * forwardSign;
        double accel = net / mass;

        double drivenMass = mass * (spec.drivenAxles / (double) Math.max(1, spec.axleCount)) * 1.35;
        double maxTraction = drivenMass * G * grip;
        if (Math.abs(engineForce) > maxTraction) {
            double excess = Math.abs(engineForce) / maxTraction - 1;
            accel -= Math.signum(engineForce) * excess * 1.6;
            state.slipRatio = clamp(state.slipRatio + excess * dt * 2.2, 0, 1);
            slipTimer = 0.4;
        } else if (brakeForce > mass * G * grip * 0.75 && speedAbs > 1) {
            state.slipRatio = clamp(state.slipRatio + dt * 1.5, 0, 1);
            slipTimer = 0.3;
        } else {
            slipTimer -= dt;
            if (slipTimer <= 0) state.slipRatio = approach(state.slipRatio, 0, dt * 1.4);
        }

        double newSpeed = state.speedUnits + accel * dt;
        double maxForward = spec.maxSpeedUnits * (1 - 0.35 * spec.wear01);
        newSpeed = clamp(newSpeed, -maxForward * 0.28, maxForward);
        if (speedAbs < 0.12 && throttle < 0.05 && Math.abs(grade) < mass * G * 0.05) newSpeed = 0;
        state.accelerationUnits = (newSpeed - state.speedUnits) / dt;
        state.speedUnits = newSpeed;
        state.longitudinalSpeedUnits = newSpeed;
        state.totalMassKg = mass;
        lastSpeedForAccel = newSpeed;
    }

    // ---------------------------------------------------------------- lateral dynamics

    private void updateLateralMotion(double dt, double mass, double grip) {
        double speedAbs = Math.abs(state.speedUnits);
        // steering angle at the front wheels: 0.62 rad at standstill, ~0.075 rad at 90 km/h
        double maxSteer = clamp(0.62 - speedAbs * 0.0058, 0.075, 0.62);
        double steerAngle = steerSmoothed * maxSteer;
        state.steeringAngleRad = steerAngle;

        double yawRate = speedAbs < 0.05 ? 0 : state.speedUnits * Math.tan(steerAngle) / WHEELBASE_M;
        double latAccel = Math.abs(yawRate * state.speedUnits);
        double maxLat = MAX_LATERAL_G * G * grip;
        if (latAccel > maxLat && latAccel > 0.001) {
            double excess = latAccel / maxLat - 1;
            yawRate /= 1.0 + excess * 0.85;
            state.slipAngle01 = clamp(state.slipAngle01 + excess * dt * 1.4, 0, 1);
        } else {
            state.slipAngle01 = approach(state.slipAngle01, 0, dt * 1.2);
        }
        // a heavy trailer resists turning (off-tracking feel)
        if (trailerAttached) {
            yawRate *= 1.0 - clamp(Math.abs(state.trailerAngleRad) * 0.45, 0, 0.35);
        }
        state.yawRate = yawRate;
        state.heading = wrap(state.heading + yawRate * dt);
        state.lateralSpeedUnits = state.speedUnits * Math.sin(state.slipAngle01 * 0.25);

        double targetRoll = -Math.signum(yawRate * state.speedUnits) * clamp(latAccel / G * 0.14, 0, 0.09);
        state.bodyRoll = approach(state.bodyRoll, targetRoll, dt * 3.0);
        double targetPitch = clamp(-state.accelerationUnits / G * 0.055, -0.05, 0.05);
        state.bodyPitch = approach(state.bodyPitch, targetPitch, dt * 3.0);

        double fx = Math.sin(state.heading);
        double fz = Math.cos(state.heading);
        state.x += fx * state.speedUnits * dt;
        state.z += fz * state.speedUnits * dt;
        state.wheelSpinRad = wrap(state.wheelSpinRad
                + (state.speedUnits / spec.tireRadiusMeters) * dt * (1 + state.slipRatio * 2.2));
        odometerUnits += Math.abs(state.speedUnits) * dt;
    }

    // ---------------------------------------------------------------- semi-trailer

    /**
     * Kinematic hitch model: the trailer axle always trails behind the hitch point, which gives the
     * classic cut-in on tight turns, off-tracking in roundabouts and a jack-knife limit.
     */
    private void updateTrailer(double dt) {
        if (!trailerAttached) {
            state.trailerAttached = false;
            state.trailerAngleRad = approach(state.trailerAngleRad, 0, dt);
            return;
        }
        state.trailerAttached = true;
        double fx = Math.sin(state.heading);
        double fz = Math.cos(state.heading);
        double hitchX = state.x - fx * (spec.truckLengthUnits * 0.45);
        double hitchZ = state.z - fz * (spec.truckLengthUnits * 0.45);

        double targetHeading = Math.atan2(hitchX - state.trailerX, hitchZ - state.trailerZ);
        double lag = clamp(dt * (1.6 + 1.2 / (1 + Math.abs(state.speedUnits))), 0, 0.6);
        state.trailerHeading = wrap(state.trailerHeading + wrap(targetHeading - state.trailerHeading) * lag);
        state.trailerX = hitchX - Math.sin(state.trailerHeading) * trailerLengthM;
        state.trailerZ = hitchZ - Math.cos(state.trailerHeading) * trailerLengthM;
        state.trailerY = state.y;
        state.trailerAngleRad = wrap(state.heading - state.trailerHeading);
        state.jackknifed = Math.abs(state.trailerAngleRad) > Math.toRadians(78);
        if (state.jackknifed) {
            state.trailerAngleRad = Math.signum(state.trailerAngleRad) * Math.toRadians(78);
            state.trailerHeading = wrap(state.heading - state.trailerAngleRad);
            state.trailerX = hitchX - Math.sin(state.trailerHeading) * trailerLengthM;
            state.trailerZ = hitchZ - Math.cos(state.trailerHeading) * trailerLengthM;
        }
    }

    // ---------------------------------------------------------------- auxiliaries

    /** Fuel burn, temperatures and air pressure. Distances are converted to displayed kilometres. */
    private void updateSystems(double dt, double mass, double throttle, double brake, VehicleInput in,
                               double speedAbs) {
        double metresDriven = speedAbs * dt;
        // the map is compressed 20x, so displayed kilometres are 20x the driven metres
        double displayedKm = metresDriven * COMPRESSION / 1000.0;
        double speedKmh = speedAbs * 3.6;
        double gradePercent = state.gradeForceN / Math.max(1.0, mass * G) * 100.0;
        double consumption = consumptionL100(speedKmh, gradePercent, throttle, 1.0);
        double litres = consumption * displayedKm / 100.0;
        if (state.fuelL > 0) {
            state.fuelL = Math.max(0, state.fuelL - litres);
            fuelUsedL += litres;
        } else {
            // out of fuel: no torque, the truck coasts to a stop
            state.engineForceN = 0;
            state.torqueOutputNm = 0;
        }
        state.litersPer100Km = consumption;

        double speedCooling = Math.min(18.0, speedAbs * 0.55);
        double targetEngine = 88 + state.engineLoad01 * 15 - speedCooling;
        state.engineTemperatureC = approach(state.engineTemperatureC, targetEngine, dt * 0.09);
        state.oilTemperatureC = approach(state.oilTemperatureC, targetEngine + 7, dt * 0.07);

        if (brake > 0.08) {
            state.airPressureBar = Math.max(5.2, state.airPressureBar - brake * dt * 0.5);
        } else {
            state.airPressureBar = Math.min(8.4, state.airPressureBar + dt * 0.28);
        }

        double brakeLoad = clamp(brake * speedAbs / 25.0, 0, 1);
        state.brakeTemperature01 = clamp(
                state.brakeTemperature01 + brakeLoad * dt * 0.10
                        + clamp(in.retarder, 0, 3) * dt * 0.012 * clamp(speedAbs / 25.0, 0, 1)
                        - dt * 0.035 * (1 + speedAbs / 25.0),
                0, 1);
    }

    /**
     * Fuel consumption in litres / 100 displayed km.
     *
     * Base consumption is taken at 80 km/h with half load; the model scales it with load, speed,
     * gradient, accelerator pedal position, wear and the weather/grip factor.
     */
    public double consumptionL100(double speedKmh, double gradePercent, double throttle, double gripMultiplier) {
        double load = spec.loadFactor();
        double loadFactor = 0.62 + 0.78 * clamp(load, 0, 1.3);
        double speed = Math.max(0, speedKmh);
        double speedFactor;
        if (speed < 45) {
            speedFactor = 1.22 - speed * 0.002;
        } else if (speed < 80) {
            speedFactor = 1.0 + (speed - 60) * 0.0015;
        } else {
            speedFactor = 1.0 + (speed - 80) * 0.0075;
        }
        double gradeFactor = 1.0 + clamp(gradePercent, -6, 8) * 0.055;
        double pedalFactor = 0.88 + clamp(throttle, 0, 1) * 0.24;
        double wearFactor = 1.0 + spec.wear01 * 0.28;
        double gripFactor = 1.0 + (1.0 - clamp(gripMultiplier, 0.5, 1.1)) * 0.18;
        double idleBurn = speedKmh < 3 ? 1.35 : 0.0;
        return spec.baseConsumptionL100 * loadFactor * speedFactor * gradeFactor * pedalFactor
                * wearFactor * gripFactor + idleBurn;
    }

    /**
     * Applies a collision impulse.
     *
     * @param severity01 0 = a scratch, 1 = a full-speed impact
     * @return the damage fraction to add to the truck wear (0..1)
     */
    public double applyCollision(double severity01) {
        double severity = clamp(severity01, 0, 1);
        state.speedUnits *= 1.0 - 0.85 * severity;
        state.longitudinalSpeedUnits = state.speedUnits;
        state.engineRpm = Math.max(spec.idleRpm, state.engineRpm * (1 - 0.6 * severity));
        state.bodyRoll += (severity - 0.5) * 0.12;
        return 0.02 + 0.55 * severity;
    }

    /** Human readable gear for the HUD: R, N, 1..12. */
    public String gearLabel() {
        if (state.gear <= 0) return state.speedUnits < -0.3 ? "R" : "N";
        return Integer.toString(state.gear);
    }

    private double airPressureFactor() {
        return 1.0 - clamp((7.0 - state.airPressureBar) * 0.18, 0, 0.5);
    }

    private double brakeTemperatureFactor() {
        return 1.0 - clamp((state.brakeTemperature01 - 0.72) * 1.6, 0, 0.45);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static double approach(double current, double target, double maxDelta) {
        double d = target - current;
        if (Math.abs(d) <= maxDelta) return target;
        return current + Math.signum(d) * maxDelta;
    }

    private static double wrap(double angle) {
        double a = angle % (2 * Math.PI);
        if (a > Math.PI) a -= 2 * Math.PI;
        if (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }

    /** Real metres represented by one world unit in the distance compression of the map. */
    private static final double COMPRESSION = 20.0;
}

