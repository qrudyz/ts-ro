package com.rudyunguru.trucks.gl.api;

/**
 * Everything the renderer needs to draw one vehicle this frame. Reused instance (no allocation in
 * the render loop): the simulation writes into it, the GL thread reads it.
 */
public final class VehiclePose {

    // --- transform (world units) ---
    public float x;
    public float y;
    public float z;
    /** Heading in radians, 0 = +Z axis, growing clockwise (matches the AI/path space). */
    public float headingRad;
    /** Chassis roll/pitch in radians, used for the "weight transfer" look. */
    public float bodyRoll;
    public float bodyPitch;

    // --- motion ---
    /** Forward speed in world units per second. */
    public float speedUnits;
    public float steeringAngleRad;
    public float wheelSpinRad;
    public float engineRpm;
    public int gear;
    public float retarderLevel;

    // --- trailer / articulation ---
    public boolean trailerAttached;
    public int trailerMeshVariant;
    public float trailerHitchAngleRad;
    /** Trailer yaw relative to the truck, for the visual hinge. */
    public float trailerYawRad;

    // --- lights and extras ---
    public boolean headlights;
    public boolean highBeam;
    public boolean brakeLights;
    public boolean reverseLights;
    public boolean indicatorLeft;
    public boolean indicatorRight;
    public boolean hazards;
    public boolean beaconLights;
    public float wiperAngleRad;
    public float cabinLightLevel;

    // --- damage / effects ---
    /** 0 = pristine, 1 = wreck: adds smoke, soot and scraped-paint shading. */
    public float damage01;
    public boolean engineSmoke;
    public float brakeTemperature01;
    public float tireSlip01;
    public String bodyColorHex = "#FFFFFF";

    public void copyFrom(VehiclePose other) {
        x = other.x;
        y = other.y;
        z = other.z;
        headingRad = other.headingRad;
        bodyRoll = other.bodyRoll;
        bodyPitch = other.bodyPitch;
        speedUnits = other.speedUnits;
        steeringAngleRad = other.steeringAngleRad;
        wheelSpinRad = other.wheelSpinRad;
        engineRpm = other.engineRpm;
        gear = other.gear;
        retarderLevel = other.retarderLevel;
        trailerAttached = other.trailerAttached;
        trailerMeshVariant = other.trailerMeshVariant;
        trailerHitchAngleRad = other.trailerHitchAngleRad;
        trailerYawRad = other.trailerYawRad;
        headlights = other.headlights;
        highBeam = other.highBeam;
        brakeLights = other.brakeLights;
        reverseLights = other.reverseLights;
        indicatorLeft = other.indicatorLeft;
        indicatorRight = other.indicatorRight;
        hazards = other.hazards;
        beaconLights = other.beaconLights;
        wiperAngleRad = other.wiperAngleRad;
        cabinLightLevel = other.cabinLightLevel;
        damage01 = other.damage01;
        engineSmoke = other.engineSmoke;
        brakeTemperature01 = other.brakeTemperature01;
        tireSlip01 = other.tireSlip01;
        bodyColorHex = other.bodyColorHex;
    }
}
