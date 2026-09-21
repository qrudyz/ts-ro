package com.rudyunguru.trucks.gl;

import android.opengl.GLES30;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Common GL helpers for the engine.
 */
public final class GLUtil {

    public static int createProgram(String vert, String frag) {
        int v = loadShader(GLES30.GL_VERTEX_SHADER, vert);
        int f = loadShader(GLES30.GL_FRAGMENT_SHADER, frag);
        if (v == 0 || f == 0) return 0;

        int p = GLES30.glCreateProgram();
        GLES30.glAttachShader(p, v);
        GLES30.glAttachShader(p, f);
        GLES30.glLinkProgram(p);

        int[] status = new int[1];
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e("GL", "Link failed: " + GLES30.glGetProgramInfoLog(p));
            GLES30.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private static int loadShader(int type, String source) {
        int s = GLES30.glCreateShader(type);
        GLES30.glShaderSource(s, source);
        GLES30.glCompileShader(s);

        int[] status = new int[1];
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e("GL", "Compile failed (" + type + "): " + GLES30.glGetShaderInfoLog(s));
            GLES30.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    public static FloatBuffer floatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    public static IntBuffer intBuffer(int[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer ib = bb.asIntBuffer();
        ib.put(data);
        ib.position(0);
        return ib;
    }

    public static void checkError(String op) {
        int err;
        while ((err = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e("GL", op + ": err 0x" + Integer.toHexString(err));
        }
    }
}
