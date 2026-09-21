package com.rudyunguru.trucks.gl.api;

/**
 * Sky / weather / time-of-day state written by the weather system
 * (``com.rudyunguru.trucks.world.WeatherSystem``) and read by the renderer.
 */
public final class EnvironmentState {

    /** Sun elevation: radians, negative below the horizon. */
    public float sunElevationRad = 0.9f;
    /** Sun azimuth in radians (rotation around the world Y axis). */
    public float sunAzimuthRad = 0.6f;
    /** 0 = midnight, 0.5 = noon, 1 = midnight again. */
    public float timeOfDay01 = 0.5f;

    public float fogDensity = 0.15f;
    public float rainIntensity = 0f;
    public float snowIntensity = 0f;
    public float wetness = 0f;
    public float cloudCover = 0.25f;
    public float windSpeed = 2.5f;
    public float ambientTemperatureC = 18f;
    public float lightningFlash = 0f;

    public float skyTopR = 0.06f, skyTopG = 0.13f, skyTopB = 0.28f;
    public float skyHorizonR = 0.55f, skyHorizonG = 0.62f, skyHorizonB = 0.72f;
    public float sunColorR = 1.0f, sunColorG = 0.94f, sunColorB = 0.82f;
    public float exposure = 1.0f;

    /** True when headlights should be switched on automatically (night, storm, dense fog). */
    public boolean headlightsRecommended = false;

    public void copyFrom(EnvironmentState other) {
        sunElevationRad = other.sunElevationRad;
        sunAzimuthRad = other.sunAzimuthRad;
        timeOfDay01 = other.timeOfDay01;
        fogDensity = other.fogDensity;
        rainIntensity = other.rainIntensity;
        snowIntensity = other.snowIntensity;
        wetness = other.wetness;
        cloudCover = other.cloudCover;
        windSpeed = other.windSpeed;
        ambientTemperatureC = other.ambientTemperatureC;
        lightningFlash = other.lightningFlash;
        skyTopR = other.skyTopR;
        skyTopG = other.skyTopG;
        skyTopB = other.skyTopB;
        skyHorizonR = other.skyHorizonR;
        skyHorizonG = other.skyHorizonG;
        skyHorizonB = other.skyHorizonB;
        sunColorR = other.sunColorR;
        sunColorG = other.sunColorG;
        sunColorB = other.sunColorB;
        exposure = other.exposure;
        headlightsRecommended = other.headlightsRecommended;
    }
}
