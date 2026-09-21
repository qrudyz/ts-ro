package com.rudyunguru.trucks.physics;

/**
 * Immutable performance envelope of a truck+trailer combination as the simulation sees it. Built by
 * the state layer from the catalog model, the installed upgrades and the current wear, so the physics
 * never needs to know about the game economy.
 *
 * All units are SI-ish and documented per field; world units (1 unit = 20 real metres, see
 * MapScale) are used for distances, so `maxSpeedUnits` is expressed in units per second.
 */
public final class VehicleSpec {

    /** Truck model id, for diagnostics only. */
    public String modelId = "unknown";

    // --- mass and size ---
    public double curbMassKg = 8000.0;
    public double maxGrossMassKg = 40000.0;
    public double cargoMassKg = 0.0;
    public double trailerMassKg = 0.0;
    /** Overall length used for collision and camera framing (world units). */
    public double truckLengthUnits = 6.2;
    public double trailerLengthUnits = 6.9;
    public double widthMeters = 2.55;
    public double heightMeters = 3.9;
    public int axleCount = 2;
    public int drivenAxles = 1;

    // --- powertrain ---
    public double powerHp = 450.0;
    public double torqueNm = 2200.0;
    /** rpm at which the torque curve peaks. */
    public double peakTorqueRpm = 1050.0;
    public double idleRpm = 520.0;
    public double maxRpm = 2100.0;
    public int gearCount = 12;
    /** true for AUTOMATIC/AUTOMATED gearboxes (no clutch handling needed). */
    public boolean automaticGearbox = true;
    /** automated/manual shifts take this long (seconds). */
    public double shiftTimeSeconds = 0.55;
    public double finalDriveRatio = 2.71;
    public double tireRadiusMeters = 0.52;

    // --- brakes / retarder ---
    /** 0.6 = worn, 1.0 = new brakes. */
    public double brakeEfficiency = 1.0;
    /** 0..3 retarder stages available. */
    public int retarderStages = 3;
    public double engineBrakeTorqueNm = 600.0;
    /** 0.6 = worn, 1.0 = new tyres (grip multiplier). */
    public double tireGrip = 1.0;

    // --- fuel ---
    public double tankCapacityL = 900.0;
    public double fuelL = 450.0;
    /** Reference consumption at 80 km/h with 50% load (litres / 100 km). */
    public double baseConsumptionL100 = 28.0;

    // --- wear ---
    /** 0 = pristine, 1 = wrecked; reduces power and increases consumption. */
    public double wear01 = 0.0;

    // --- limits ---
    public double maxSpeedKmh = 89.0;
    public double maxSpeedUnits = 25.0;
    public double comfortBonus = 0.0;

    /** Total moving mass in kg (truck + trailer + cargo). */
    public double totalMassKg() {
        return curbMassKg + trailerMassKg + cargoMassKg;
    }

    /** Payload ratio 0..1 used by the consumption model. */
    public double loadFactor() {
        double payload = Math.max(1.0, maxGrossMassKg - curbMassKg);
        return Math.min(1.3, (cargoMassKg + trailerMassKg) / payload);
    }

    /** Effective power after wear and damage. */
    public double effectivePowerHp() {
        return powerHp * (1.0 - 0.22 * wear01);
    }

    public double effectiveTorqueNm() {
        return torqueNm * (1.0 - 0.25 * wear01);
    }
}
