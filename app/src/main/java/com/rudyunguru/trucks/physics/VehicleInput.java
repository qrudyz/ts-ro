package com.rudyunguru.trucks.physics;

/**
 * Per-frame driver input. Values are analog 0..1 (or -1..1 for steering) and are written by the
 * touch controls (virtual wheel, pedals, tilt) or by the keyboard/controller layer in the debug
 * build. The class is mutable and reused - the driving loop never allocates.
 */
public final class VehicleInput {

    /** 0..1 accelerator pedal travel. */
    public double throttle;
    /** 0..1 service brake pedal travel. */
    public double brake;
    /** -1 = full left, +1 = full right. */
    public double steering;
    /** Retarder stalk: 0 = off, 1..3 stages. */
    public int retarder;
    /** Engine brake (exhaust/compression brake) on/off. Already implied by retarder level > 0. */
    public boolean engineBrake;
    public boolean handbrake;
    /** Requested cruise speed in km/h, 0 = cruise control off. */
    public double cruiseControlKmh;
    /** Manual shift requests; consumed and reset by the gearbox each frame. */
    public boolean shiftUpRequested;
    public boolean shiftDownRequested;
    /** Manual gearbox: true when the clutch is pressed (blocks torque). */
    public boolean clutch;
    /** Automatic gearbox mode selection (false = manual). */
    public boolean automaticGearbox = true;
    /** Gear range limiter requested from the UI (0 = none). */
    public int gearLimit;

    public void reset() {
        throttle = 0;
        brake = 0;
        steering = 0;
        retarder = 0;
        engineBrake = false;
        handbrake = true;
        cruiseControlKmh = 0;
        shiftUpRequested = false;
        shiftDownRequested = false;
        clutch = false;
        gearLimit = 0;
    }

    /** Convenience for headless tests and for AI-driven vehicles. */
    public void set(double throttle, double brake, double steering) {
        this.throttle = clamp01(throttle);
        this.brake = clamp01(brake);
        this.steering = clamp(steering, -1, 1);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }
}
