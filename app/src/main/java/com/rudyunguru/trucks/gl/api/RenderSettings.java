package com.rudyunguru.trucks.gl.api;

/**
 * Renderer configuration derived from the player's graphics preset
 * (see {@code com.rudyunguru.trucks.core.model.GraphicsPreset}).
 *
 * Mutable on purpose: the settings screen updates it in place and the renderer picks the changes up
 * on the next frame, without recreating the GL context.
 */
public final class RenderSettings {

    public int qualityIndex = 1;
    public String qualityName = "MEDIUM";

    public int shadowMapSize = 1024;
    public boolean shadowsEnabled = true;
    public boolean reflectionsEnabled = false;
    public boolean msaaEnabled = false;

    public int maxTrafficVehicles = 16;
    public int drawDistanceMetres = 520;
    public int rainParticles = 350;
    public double vegetationDensity = 0.6;
    public double textureScale = 0.75;

    /** 30 or 60. The renderer caps its frame pacing to this value. */
    public int targetFps = 45;
    /** When true the renderer scales the render resolution automatically to hold the frame rate. */
    public boolean dynamicResolution = true;
    /** Current internal resolution scale (1.0 = native). Written back by the renderer. */
    public float resolutionScale = 1.0f;

    public boolean fogEnabled = true;
    public boolean vegetationShadows = false;
    public boolean headlightVolumetrics = false;
    public boolean wetRoadReflections = true;

    public RenderSettings copy() {
        RenderSettings c = new RenderSettings();
        c.qualityIndex = qualityIndex;
        c.qualityName = qualityName;
        c.shadowMapSize = shadowMapSize;
        c.shadowsEnabled = shadowsEnabled;
        c.reflectionsEnabled = reflectionsEnabled;
        c.msaaEnabled = msaaEnabled;
        c.maxTrafficVehicles = maxTrafficVehicles;
        c.drawDistanceMetres = drawDistanceMetres;
        c.rainParticles = rainParticles;
        c.vegetationDensity = vegetationDensity;
        c.textureScale = textureScale;
        c.targetFps = targetFps;
        c.dynamicResolution = dynamicResolution;
        c.resolutionScale = resolutionScale;
        c.fogEnabled = fogEnabled;
        c.vegetationShadows = vegetationShadows;
        c.headlightVolumetrics = headlightVolumetrics;
        c.wetRoadReflections = wetRoadReflections;
        return c;
    }
}
