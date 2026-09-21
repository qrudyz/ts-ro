package com.rudyunguru.trucks.gl.api;

/** The five camera presets required by the design plus the free inspection camera. */
public enum CameraMode {
    /** Inside the cab, at the driver's eye position; the functional interior camera. */
    CABIN,
    /** Behind the windshield: shows the dashboard, steering wheel and gauges. */
    DASHBOARD,
    /** Chase camera behind the truck (default for casual driving). */
    EXTERIOR_BACK,
    /** Camera looking at the truck's side, handy for tight manoeuvres. */
    EXTERIOR_SIDE,
    /** Free orbit camera used in menus, showroom and for cinematic shots. */
    FREE;

    public static CameraMode fromName(String name) {
        if (name == null) return CABIN;
        for (CameraMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) return mode;
        }
        return CABIN;
    }

    public CameraMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
