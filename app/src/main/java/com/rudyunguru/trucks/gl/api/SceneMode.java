package com.rudyunguru.trucks.gl.api;

/**
 * What the renderer is currently drawing.
 *
 * GARAGE   - main menu backdrop: the player's truck parked in a procedurally lit garage with slowly
 *            moving cinematic cameras and menu-friendly framing.
 * SHOWROOM - dealer viewer: turntable podium, orbit/zoom, exterior and interior inspection.
 * DRIVING  - the actual game world with traffic, weather and the full HUD overlay drawn by the UI.
 */
public enum SceneMode {
    GARAGE,
    SHOWROOM,
    DRIVING
}
