package com.rudyunguru.trucks.gl.api;

import com.rudyunguru.trucks.core.model.WorldGeometry;

/**
 * The single entry point through which Kotlin (UI + simulation) talks to the OpenGL ES renderer.
 *
 * Threading contract:
 *  - every method may be called from any thread;
 *  - the implementation queues commands internally and applies them on the GL thread;
 *  - poses (``VehiclePose``, ``TrafficPose``, ``EnvironmentState``) are plain mutable objects, so a
 *    call transfers ownership of the values, not of the object.
 *
 * Implemented by ``com.rudyunguru.trucks.gl.RenderEngine`` and exposed to the UI by
 * ``com.rudyunguru.trucks.gl.GameGLView``.
 */
public interface SceneApi {

    // ---------------------------------------------------------------- scene lifecycle

    /** Switches what the renderer draws. Must be called before poses are pushed. */
    void setSceneMode(SceneMode mode);

    SceneMode sceneMode();

    /** Applies a new quality preset. Safe to call at runtime (no context recreation). */
    void setRenderSettings(RenderSettings settings);

    RenderSettings renderSettings();

    /** Uploads a generated world to the GPU. Returns immediately; poll ``StaticWorldRef.ready``. */
    StaticWorldRef setStaticWorld(WorldGeometry geometry);

    void clearStaticWorld();

    /** Draws the GPS ribbon for the active route (x,z pairs in world units). Pass null to clear. */
    void setRoutePolyline(float[] pointsXZ, int pointCount, String colorHex);

    /** Highlight ring for the next waypoint / active objective. */
    void setObjectiveMarker(float x, float y, float z, float radiusUnits, String colorHex, boolean visible);

    // ---------------------------------------------------------------- cameras

    void setCameraMode(CameraMode mode);

    CameraMode cameraMode();

    /** Orbit parameters for FREE camera mode (menus, showroom, cinematic pan). */
    void setCameraOrbit(float yawRad, float pitchRad, float distanceUnits);

    /** Offset from the vehicle for the CABIN/DASHBOARD cameras (e.g. seat adjustment). */
    void setCameraOffset(float x, float y, float z);

    /** Enables the cinematic garage camera used by the main menu backdrop. */
    void setCinematicCamera(boolean enabled);

    // ---------------------------------------------------------------- scene content

    void setEnvironment(EnvironmentState environment);

    /** Player truck pose. Pass null to hide the player vehicle. */
    void setPlayerVehicle(VehiclePose pose);

    /** Showroom/dealer vehicle: which procedural truck+trailer mesh and which paint. */
    void setShowroomVehicle(int truckMeshVariant, int trailerMeshVariant, String bodyColorHex,
                            String rimColorHex, boolean trailerVisible);

    /** Enables the detailed dashboard/steering wheel geometry for interior cameras. */
    void setInteriorVisible(boolean visible);

    /** Allocates the traffic pool once per driving session. */
    void setTrafficCapacity(int capacity);

    /** Number of traffic vehicles to draw this frame (``<= capacity``). */
    void setTrafficCount(int count);

    /** Writes one traffic slot; the index must be below the capacity set earlier. */
    void setTrafficPose(int index, TrafficPose pose);

    /** Manually forces one frame while the scene is paused (menus over a 3D backdrop). */
    void requestFrame();

    // ---------------------------------------------------------------- diagnostics

    FrameStats frameStats();

    void setDebugOverlay(boolean enabled);

    /** Frees every GL resource. Called from the view's ``onPause``/``onDestroy``. */
    void release();
}
