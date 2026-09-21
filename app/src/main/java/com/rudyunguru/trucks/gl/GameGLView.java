package com.rudyunguru.trucks.gl;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import com.rudyunguru.trucks.gl.api.*;
import com.rudyunguru.trucks.render.math.Mat4;
import com.rudyunguru.trucks.render.math.Vec3;
import com.rudyunguru.trucks.render.MeshData;
import com.rudyunguru.trucks.render.WorldMeshFactory;
import com.rudyunguru.trucks.render.models.VehicleModels;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Android View for the 3D game surface.
 */
public final class GameGLView extends GLSurfaceView implements GLSurfaceView.Renderer, SceneApi {

    private final RenderEngine engine = new RenderEngine();
    private int width, height;

    public GameGLView(Context context) { super(context); init(); }
    public GameGLView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public SceneApi scene() { return this; }
    public void startRendering() { setRenderMode(RENDERMODE_CONTINUOUSLY); }
    public void stopRendering() { setRenderMode(RENDERMODE_WHEN_DIRTY); }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) { engine.init(); }
    @Override public void onSurfaceChanged(GL10 gl, int w, int h) { this.width = w; this.height = h; GLES30.glViewport(0, 0, w, h); }
    @Override public void onDrawFrame(GL10 gl) { engine.render(width, height); }

    // --- SceneApi implementation ---
    @Override public void setSceneMode(SceneMode mode) { engine.mode = mode; }
    @Override public SceneMode sceneMode() { return engine.mode; }
    @Override public void setRenderSettings(RenderSettings s) { engine.settings = s; }
    @Override public RenderSettings renderSettings() { return engine.settings; }
    @Override public StaticWorldRef setStaticWorld(com.rudyunguru.trucks.core.model.WorldGeometry geo) { 
        engine.worldMesh = new StaticMesh(WorldMeshFactory.build(geo));
        return new StaticWorldRef(1, 0, 0, 0, 0, 0, 0, 0, 0); 
    }
    @Override public void clearStaticWorld() { if (engine.worldMesh != null) engine.worldMesh.release(); engine.worldMesh = null; }
    @Override public void setRoutePolyline(float[] pts, int cnt, String col) {}
    @Override public void setObjectiveMarker(float x, float y, float z, float r, String col, boolean v) {}
    @Override public void setCameraMode(CameraMode mode) { engine.cameraMode = mode; }
    @Override public CameraMode cameraMode() { return engine.cameraMode; }
    @Override public void setCameraOrbit(float y, float p, float d) { engine.orbitYaw = y; engine.orbitPitch = p; engine.orbitDist = d; }
    @Override public void setCameraOffset(float x, float y, float z) {}
    @Override public void setCinematicCamera(boolean e) {}
    @Override public void setEnvironment(EnvironmentState env) { engine.env = env; }
    @Override public void setPlayerVehicle(VehiclePose pose) { engine.playerPose = pose; }
    @Override public void setShowroomVehicle(int t, int tr, String b, String r, boolean v) {
        float[] paint = MeshData.rgb(b, new float[3]);
        engine.playerMesh = new StaticMesh(VehicleModels.truck(t, paint, true));
    }
    @Override public void setInteriorVisible(boolean v) {}
    @Override public void setTrafficCapacity(int c) {}
    @Override public void setTrafficCount(int c) {}
    @Override public void setTrafficPose(int i, TrafficPose p) {}
    @Override public void requestFrame() { requestRender(); }
    @Override public FrameStats frameStats() { return new FrameStats(); }
    @Override public void setDebugOverlay(boolean e) {}
    @Override public void release() { if (engine.worldMesh != null) engine.worldMesh.release(); }

    private static class RenderEngine {
        SceneMode mode = SceneMode.GARAGE;
        CameraMode cameraMode = CameraMode.EXTERIOR_BACK;
        RenderSettings settings = new RenderSettings();
        EnvironmentState env = new EnvironmentState();
        VehiclePose playerPose = new VehiclePose();
        StaticMesh worldMesh, playerMesh;
        int program;
        float orbitYaw, orbitPitch, orbitDist = 15f;

        void init() {
            String v = "#version 300 es\nlayout(location=0) in vec3 a_pos; layout(location=1) in vec3 a_norm; layout(location=2) in vec3 a_col; uniform mat4 u_mvp; out vec3 v_col; out vec3 v_norm; void main(){ gl_Position=u_mvp*vec4(a_pos,1.0); v_col=a_col; v_norm=a_norm; }";
            String f = "#version 300 es\nprecision mediump float; in vec3 v_col; in vec3 v_norm; out vec4 frag; void main(){ float d=max(dot(normalize(v_norm),normalize(vec3(1,2,1))),0.2); frag=vec4(v_col*d,1.0); }";
            program = GLUtil.createProgram(v, f);
            playerMesh = new StaticMesh(VehicleModels.truck(0, MeshData.rgb("#D9051F", new float[3]), false));
        }

        void render(int w, int h) {
            GLES30.glClearColor(env.skyTopR, env.skyTopG, env.skyTopB, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glUseProgram(program);
            Mat4 proj = Mat4.perspective((float)Math.toRadians(60), (float)w/h, 0.1f, 2000f);
            Mat4 view = mode == SceneMode.DRIVING ? Mat4.lookAt(new Vec3(playerPose.x, playerPose.y + 4f, playerPose.z - 10f), new Vec3(playerPose.x, playerPose.y + 2f, playerPose.z), new Vec3(0, 1, 0)) : Mat4.lookAt(new Vec3(10, 5, 10), new Vec3(0, 2, 0), new Vec3(0, 1, 0));
            Mat4 mvp = Mat4.multiply(proj, view);
            int loc = GLES30.glGetUniformLocation(program, "u_mvp");
            GLES30.glUniformMatrix4fv(loc, 1, false, mvp.m, 0);
            if (worldMesh != null) worldMesh.draw();
            Mat4 model = Mat4.fromPose(playerPose.x, playerPose.y, playerPose.z, playerPose.headingRad);
            GLES30.glUniformMatrix4fv(loc, 1, false, Mat4.multiply(mvp, model).m, 0);
            if (playerMesh != null) playerMesh.draw();
        }
    }
}
