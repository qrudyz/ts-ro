package com.rudyunguru.trucks.gl.api;

/**
 * Reusable snapshot of one AI traffic vehicle. The pool is preallocated by the renderer
 * (``setTrafficCapacity``) and updated in place every frame, so no garbage is produced while driving.
 */
public final class TrafficPose {

    public int kindOrdinal;
    public float x;
    public float y;
    public float z;
    public float headingRad;
    public float speedUnits;
    public float wheelSpinRad;
    public float lengthMetres = 4.4f;
    public float heightMetres = 1.5f;

    public boolean brakeLights;
    public boolean headlights;
    public boolean indicatorLeft;
    public boolean indicatorRight;
    public boolean trailerAttached;

    public String bodyColorHex = "#B0B7C3";
    /** Mesh variety index so the pool does not look like clones. */
    public int variant;

    public void set(int kindOrdinal, float x, float y, float z, float headingRad, float speedUnits) {
        this.kindOrdinal = kindOrdinal;
        this.x = x;
        this.y = y;
        this.z = z;
        this.headingRad = headingRad;
        this.speedUnits = speedUnits;
    }

    public void copyFrom(TrafficPose other) {
        kindOrdinal = other.kindOrdinal;
        x = other.x;
        y = other.y;
        z = other.z;
        headingRad = other.headingRad;
        speedUnits = other.speedUnits;
        wheelSpinRad = other.wheelSpinRad;
        lengthMetres = other.lengthMetres;
        heightMetres = other.heightMetres;
        brakeLights = other.brakeLights;
        headlights = other.headlights;
        indicatorLeft = other.indicatorLeft;
        indicatorRight = other.indicatorRight;
        trailerAttached = other.trailerAttached;
        bodyColorHex = other.bodyColorHex;
        variant = other.variant;
    }
}
