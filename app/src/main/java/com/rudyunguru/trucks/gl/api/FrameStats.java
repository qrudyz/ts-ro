package com.rudyunguru.trucks.gl.api;

/** Per-frame renderer telemetry, shown in the debug overlay and used by the adaptive resolution. */
public final class FrameStats {
    public float fps;
    public float frameTimeMs;
    public float cpuTimeMs;
    public int drawCalls;
    public int triangles;
    public int activeTraffic;
    public float resolutionScale = 1f;
    public int surfaceWidth;
    public int surfaceHeight;
    public long totalFrames;
    public String glVersion = "";
    public String rendererName = "";

    public void reset() {
        drawCalls = 0;
        triangles = 0;
        activeTraffic = 0;
    }
}
