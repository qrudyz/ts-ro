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
        engine.trailerMesh = tr >= 0 ? new StaticMesh(VehicleModels.trailer(tr)) : null;
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
        StaticMesh worldMesh, playerMesh, trailerMesh;
        int program;
        float orbitYaw, orbitPitch, orbitDist = 15f;

        void init() {
            String v = "#version 300 es\n"
                    + "layout(location=0) in vec3 a_pos;\n"
                    + "layout(location=1) in vec3 a_norm;\n"
                    + "layout(location=2) in vec3 a_col;\n"
                    + "layout(location=3) in float a_flag;\n"
                    + "uniform mat4 u_mvp;\n"
                    + "out vec3 v_col;\n"
                    + "out vec3 v_norm;\n"
                    + "out float v_flag;\n"
                    + "void main(){ gl_Position=u_mvp*vec4(a_pos,1.0); v_col=a_col; v_norm=a_norm; v_flag=a_flag; }";
            String f = "#version 300 es\n"
                    + "precision mediump float;\n"
                    + "in vec3 v_col; in vec3 v_norm; in float v_flag;\n"
                    + "uniform vec3 u_sun;\n"
                    + "uniform float u_ambient;\n"
                    + "out vec4 frag;\n"
                    + "void main(){\n"
                    + "  float diff = max(dot(normalize(v_norm), normalize(u_sun)), 0.0);\n"
                    + "  vec3 c = v_col;\n"
                    + "  float shine = 0.0;\n"
                    + "  if (v_flag > 2.5) { c = mix(c, vec3(0.35,0.55,0.75), 0.55); }       // glass\n"
                    + "  else if (v_flag > 1.5) { c = c * 1.6; shine = 0.15; }              // emissive\n"
                    + "  else if (v_flag > 0.5) { shine = 0.35; }                            // metal\n"
                    + "  float lit = u_ambient + diff * (1.0 - u_ambient) + shine;\n"
                    + "  frag = vec4(c * min(lit, 1.6), 1.0);\n"
                    + "}";
            program = GLUtil.createProgram(v, f);
            playerMesh = new StaticMesh(VehicleModels.truck(0, MeshData.rgb("#D9051F", new float[3]), false));
            trailerMesh = new StaticMesh(VehicleModels.trailer(0));
        }

        void render(int w, int h) {
            GLES30.glClearColor(env.skyHorizonR, env.skyHorizonG, env.skyHorizonB, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glUseProgram(program);
            Mat4 proj = Mat4.perspective((float)Math.toRadians(60), (float)w/h, 0.1f, 4000f);
            Mat4 view;
            if (mode == SceneMode.DRIVING) {
                // Third-person chase camera: behind and above the truck, looking along its heading.
                float hd = playerPose.headingRad;
                float dx = (float)Math.sin(hd), dz = (float)Math.cos(hd);
                float dist = 16f, height = 6.5f;
                Vec3 eye = new Vec3(playerPose.x - dx * dist, playerPose.y + height, playerPose.z - dz * dist);
                Vec3 target = new Vec3(playerPose.x + dx * 10f, playerPose.y + 2.5f, playerPose.z + dz * 10f);
                view = Mat4.lookAt(eye, target, new Vec3(0, 1, 0));
            } else {
                // Garage / showroom orbit: slow turntable around the truck.
                long t = System.currentTimeMillis();
                float yaw = (t % 20000L) / 20000f * (float)(Math.PI * 2);
                float r = 11f;
                view = Mat4.lookAt(
                        new Vec3((float)Math.sin(yaw) * r, 3.6f, (float)Math.cos(yaw) * r),
                        new Vec3(0f, 1.6f, 0f), new Vec3(0, 1, 0));
                playerPose = new VehiclePose(); // Truck centered at the garage origin.
            }
            Mat4 mvp = Mat4.multiply(proj, view);
            int loc = GLES30.glGetUniformLocation(program, "u_mvp");
            GLES30.glUniformMatrix4fv(loc, 1, false, mvp.m, 0);
            int locSun = GLES30.glGetUniformLocation(program, "u_sun");
            float se = Math.max(env.sunElevationRad, 0.15f);
            GLES30.glUniform3f(locSun,
                    (float)Math.cos(env.sunAzimuthRad) * se,
                    se,
                    (float)Math.sin(env.sunAzimuthRad) * se);
            int locAmb = GLES30.glGetUniformLocation(program, "u_ambient");
            float amb = 0.30f + 0.15f * se;
            GLES30.glUniform1f(locAmb, amb);
            if (worldMesh != null) worldMesh.draw();
            // Player truck + attached trailer, articulated at the fifth wheel.
            Mat4 model = Mat4.fromPose(playerPose.x, playerPose.y, playerPose.z, playerPose.headingRad);
            GLES30.glUniformMatrix4fv(loc, 1, false, Mat4.multiply(mvp, model).m, 0);
            if (playerMesh != null) playerMesh.draw();
            if (mode == SceneMode.DRIVING && playerPose.trailerAttached) {
                Mat4 tr = Mat4.fromPose(
                        playerPose.x - (float)Math.sin(playerPose.headingRad) * 7.2f,
                        playerPose.y,
                        playerPose.z - (float)Math.cos(playerPose.headingRad) * 7.2f,
                        playerPose.headingRad);
                GLES30.glUniformMatrix4fv(loc, 1, false, Mat4.multiply(mvp, tr).m, 0);
                if (trailerMesh != null) trailerMesh.draw();
            }
        }
    }
}
