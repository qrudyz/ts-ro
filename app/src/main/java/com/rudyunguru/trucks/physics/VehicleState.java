package com.rudyunguru.trucks.physics;

/**
 * Everything the physics exposes to the rest of the game: the pose of the truck, the pose of the
 * trailer (when attached) and all the numbers the HUD/dashboard shows. Written in place every frame.
 *
 * Axes: x = east, y = up, z = south (see MapScale), heading 0 = +Z, growing clockwise.
 */
public final class VehicleState {

    // --- truck pose ---
    public double x;
    public double y;
    public double z;
    /** Yaw in radians. */
    public double heading;
    /** Roll of the body (weight transfer look), radians. */
    public double bodyRoll;
    /** Pitch of the body, radians (positive = nose up). */
    public double bodyPitch;

    // --- motion ---
    /** Forward speed in world units per second (25 u/s == 90 km/h). */
    public double speedUnits;
    /** Sign of the motion along the heading (negative when reversing). */
    public double longitudinalSpeedUnits;
    public double lateralSpeedUnits;
    public double accelerationUnits;
    /** Yaw rate / turn rate, radians per second. */
    public double yawRate;
    public double steeringAngleRad;
    public double wheelSpinRad;
    /** Longitudinal slip 0..1 (1 = wheels spinning or locked). */
    public double slipRatio;
    /** Lateral slip 0..1 (1 = sliding sideways). */
    public double slipAngle01;

    // --- powertrain ---
    public double engineRpm;
    public int gear;
    public boolean shifting;
    public double engineLoad01;
    public double torqueOutputNm;
    public boolean stalled;

    // --- brakes ---
    public double brakeForceN;
    public boolean parkingBrake;
    public double brakeTemperature01;

    // --- fuel / temperatures ---
    public double fuelL;
    public double litersPer100Km;
    public double engineTemperatureC;
    public double oilTemperatureC;
    public double airPressureBar;

    // --- trailer ---
    public boolean trailerAttached;
    public double trailerX;
    public double trailerY;
    public double trailerZ;
    public double trailerHeading;
    /** Hinge angle between truck and trailer, radians (0 = straight). */
    public double trailerAngleRad;
    public boolean jackknifed;

    // --- forces (for the debug overlay and tests) ---
    public double engineForceN;
    public double dragForceN;
    public double rollingResistanceN;
    public double gradeForceN;
    public double totalMassKg;

    public void copyFrom(VehicleState other) {
        x = other.x; y = other.y; z = other.z; heading = other.heading;
        bodyRoll = other.bodyRoll; bodyPitch = other.bodyPitch;
        speedUnits = other.speedUnits;
        longitudinalSpeedUnits = other.longitudinalSpeedUnits;
        lateralSpeedUnits = other.lateralSpeedUnits;
        accelerationUnits = other.accelerationUnits;
        yawRate = other.yawRate;
        steeringAngleRad = other.steeringAngleRad;
        wheelSpinRad = other.wheelSpinRad;
        slipRatio = other.slipRatio;
        slipAngle01 = other.slipAngle01;
        engineRpm = other.engineRpm;
        gear = other.gear;
        shifting = other.shifting;
        engineLoad01 = other.engineLoad01;
        torqueOutputNm = other.torqueOutputNm;
        stalled = other.stalled;
        brakeForceN = other.brakeForceN;
        parkingBrake = other.parkingBrake;
        brakeTemperature01 = other.brakeTemperature01;
        fuelL = other.fuelL;
        litersPer100Km = other.litersPer100Km;
        engineTemperatureC = other.engineTemperatureC;
        oilTemperatureC = other.oilTemperatureC;
        airPressureBar = other.airPressureBar;
        trailerAttached = other.trailerAttached;
        trailerX = other.trailerX; trailerY = other.trailerY; trailerZ = other.trailerZ;
        trailerHeading = other.trailerHeading;
        trailerAngleRad = other.trailerAngleRad;
        jackknifed = other.jackknifed;
        engineForceN = other.engineForceN;
        dragForceN = other.dragForceN;
        rollingResistanceN = other.rollingResistanceN;
        gradeForceN = other.gradeForceN;
        totalMassKg = other.totalMassKg;
    }
}
