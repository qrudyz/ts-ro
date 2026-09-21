package com.rudyunguru.trucks.render.math;

import java.util.Locale;

/** Minimal mutable 3-component vector used by the renderer and camera code. */
public final class Vec3 {

    public float x;
    public float y;
    public float z;

    public Vec3() {
    }

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3 set(Vec3 other) {
        return set(other.x, other.y, other.z);
    }

    public static Vec3 add(Vec3 a, Vec3 b) {
        return new Vec3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vec3 sub(Vec3 a, Vec3 b) {
        return new Vec3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vec3 mul(Vec3 a, float s) {
        return new Vec3(a.x * s, a.y * s, a.z * s);
    }

    public static float dot(Vec3 a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    public static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalized() {
        float len = length();
        if (len < 1e-6f) return new Vec3(0, 1, 0);
        return new Vec3(x / len, y / len, z / len);
    }

    /** Linear interpolation, used for smooth camera movement. */
    public static Vec3 lerp(Vec3 a, Vec3 b, float t) {
        return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }

    /** Forward direction of a heading (0 = +Z, growing clockwise around Y). */
    public static Vec3 forward(float headingRad) {
        return new Vec3((float) Math.sin(headingRad), 0f, (float) Math.cos(headingRad));
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "(%.2f, %.2f, %.2f)", x, y, z);
    }
}
