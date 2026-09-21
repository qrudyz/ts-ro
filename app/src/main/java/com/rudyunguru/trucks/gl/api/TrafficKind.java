package com.rudyunguru.trucks.gl.api;

/** Vehicle classes used by the traffic AI and by the procedural traffic mesh builder. */
public enum TrafficKind {
    CAR,
    VAN,
    TRUCK,
    BUS,
    MOTORCYCLE,
    TRACTOR;

    public static TrafficKind fromOrdinal(int ordinal) {
        TrafficKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return CAR;
        return values[ordinal];
    }
}
