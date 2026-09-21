package com.rudyunguru.trucks.render.math;

/**
 * Column-major 4x4 matrix, the layout OpenGL expects (`glUniformMatrix4fv` with transpose = false).
 *
 * Everything in the renderer uses metres and radians, matching the physics and world generation:
 * 1 world unit = 1 metre, x east, y up, z south.
 */
public final class Mat4 {

    public final float[] m = new float[16];

    public static Mat4 identity() {
        Mat4 r = new Mat4();
        r.m[0] = 1;
        r.m[5] = 1;
        r.m[10] = 1;
        r.m[15] = 1;
        return r;
    }

    public Mat4 set(Mat4 other) {
        System.arraycopy(other.m, 0, m, 0, 16);
        return this;
    }

    public Mat4 setIdentity() {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = 1;
        m[5] = 1;
        m[10] = 1;
        m[15] = 1;
        return this;
    }

    /** Right-handed perspective projection with a [0,1] depth range. */
    public static Mat4 perspective(float fovYRad, float aspect, float near, float far) {
        Mat4 r = new Mat4();
        float f = (float) (1.0 / Math.tan(fovYRad / 2.0));
        r.m[0] = f / aspect;
        r.m[5] = f;
        r.m[10] = (far + near) / (near - far);
        r.m[11] = -1f;
        r.m[14] = (2f * far * near) / (near - far);
        return r;
    }

    /** Right-handed look-at view matrix. */
    public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
        Vec3 f = Vec3.sub(target, eye).normalized();
        Vec3 s = Vec3.cross(f, up).normalized();
        Vec3 u = Vec3.cross(s, f);
        Mat4 r = new Mat4();
        r.m[0] = s.x; r.m[4] = s.y; r.m[8] = s.z;
        r.m[1] = u.x; r.m[5] = u.y; r.m[9] = u.z;
        r.m[2] = -f.x; r.m[6] = -f.y; r.m[10] = -f.z;
        r.m[12] = -Vec3.dot(s, eye);
        r.m[13] = -Vec3.dot(u, eye);
        r.m[14] = Vec3.dot(f, eye);
        r.m[15] = 1f;
        return r;
    }

    /** Orthographic projection used by the sun's shadow pass. */
    public static Mat4 ortho(float left, float right, float bottom, float top, float near, float far) {
        Mat4 r = new Mat4();
        r.m[0] = 2f / (right - left);
        r.m[5] = 2f / (top - bottom);
        r.m[10] = -2f / (far - near);
        r.m[12] = -(right + left) / (right - left);
        r.m[13] = -(top + bottom) / (top - bottom);
        r.m[14] = -(far + near) / (far - near);
        r.m[15] = 1f;
        return r;
    }

    public static Mat4 translation(float x, float y, float z) {
        Mat4 r = identity();
        r.m[12] = x;
        r.m[13] = y;
        r.m[14] = z;
        return r;
    }

    public static Mat4 scale(float s) {
        return scale(s, s, s);
    }

    public static Mat4 scale(float x, float y, float z) {
        Mat4 r = identity();
        r.m[0] = x;
        r.m[5] = y;
        r.m[10] = z;
        return r;
    }

    public static Mat4 rotationY(float angleRad) {
        Mat4 r = identity();
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        r.m[0] = c;
        r.m[2] = -s;
        r.m[8] = s;
        r.m[10] = c;
        return r;
    }

    public static Mat4 rotationX(float angleRad) {
        Mat4 r = identity();
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        r.m[5] = c;
        r.m[6] = s;
        r.m[9] = -s;
        r.m[10] = c;
        return r;
    }

    public static Mat4 rotationZ(float angleRad) {
        Mat4 r = identity();
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        r.m[0] = c;
        r.m[1] = s;
        r.m[4] = -s;
        r.m[5] = c;
        return r;
    }

    /** this = a * b (a is applied after b). */
    public Mat4 setMultiply(Mat4 a, Mat4 b) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += a.m[k * 4 + row] * b.m[col * 4 + k];
                }
                m[col * 4 + row] = sum;
            }
        }
        return this;
    }

    public static Mat4 multiply(Mat4 a, Mat4 b) {
        return new Mat4().setMultiply(a, b);
    }

    /** Transforms a point (w = 1) and writes xyz into [out]. */
    public void transformPoint(Vec3 p, Vec3 out) {
        float x = m[0] * p.x + m[4] * p.y + m[8] * p.z + m[12];
        float y = m[1] * p.x + m[5] * p.y + m[9] * p.z + m[13];
        float z = m[2] * p.x + m[6] * p.y + m[10] * p.z + m[14];
        out.set(x, y, z);
    }

    /** Upper-left 3x3 inverse-transpose, for normals under non-uniform scaling. */
    public float[] normalMatrix() {
        float[] n = new float[9];
        float a = m[0], b = m[4], c = m[8];
        float d = m[1], e = m[5], f = m[9];
        float g = m[2], h = m[6], i = m[10];
        float det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-8f) {
            n[0] = 1;
            n[4] = 1;
            n[8] = 1;
            return n;
        }
        float inv = 1f / det;
        n[0] = (e * i - f * h) * inv;
        n[1] = -(b * i - c * h) * inv;
        n[2] = (b * f - c * e) * inv;
        n[3] = -(d * i - f * g) * inv;
        n[4] = (a * i - c * g) * inv;
        n[5] = -(a * f - c * d) * inv;
        n[6] = (d * h - e * g) * inv;
        n[7] = -(a * h - b * g) * inv;
        n[8] = (a * e - b * d) * inv;
        return n;
    }

    /**
     * Builds a transform from position, heading (yaw around Y, 0 = +Z), roll and pitch. Shared by the
     * truck, trailer and traffic rendering so visuals and physics use one convention.
     */
    public static Mat4 fromPose(float x, float y, float z, float headingRad, float rollRad, float pitchRad) {
        Mat4 rot = multiply(rotationY(headingRad), multiply(rotationX(pitchRad), rotationZ(rollRad)));
        rot.m[12] = x;
        rot.m[13] = y;
        rot.m[14] = z;
        return rot;
    }

    public static Mat4 fromPose(float x, float y, float z, float headingRad) {
        return fromPose(x, y, z, headingRad, 0f, 0f);
    }
}
