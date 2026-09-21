package com.rudyunguru.trucks.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

/** Small numeric helpers shared by the whole simulation. No Android dependencies on purpose. */
object Mathx {

    const val DEG2RAD: Double = PI / 180.0
    const val RAD2DEG: Double = 180.0 / PI

    fun clamp(v: Double, min: Double, max: Double): Double = if (v < min) min else if (v > max) max else v
    fun clamp(v: Float, min: Float, max: Float): Float = if (v < min) min else if (v > max) max else v
    fun clamp(v: Int, min: Int, max: Int): Int = if (v < min) min else if (v > max) max else v
    fun clamp01(v: Double): Double = clamp(v, 0.0, 1.0)
    fun clamp01(v: Float): Float = clamp(v, 0f, 1f)

    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun invLerp(a: Double, b: Double, v: Double): Double = if (b == a) 0.0 else (v - a) / (b - a)

    fun remap(v: Double, fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double =
        lerp(toMin, toMax, clamp01(invLerp(fromMin, fromMax, v)))

    /** Frame-rate independent exponential smoothing. */
    fun damp(current: Double, target: Double, lambda: Double, dtSeconds: Double): Double =
        lerp(current, target, 1.0 - exp(-lambda * dtSeconds))

    fun damp(current: Float, target: Float, lambda: Float, dtSeconds: Float): Float =
        lerp(current, target, 1f - exp(-lambda * dtSeconds))

    fun moveTowards(current: Double, target: Double, maxDelta: Double): Double {
        val d = target - current
        return if (abs(d) <= maxDelta) target else current + sign(d) * maxDelta
    }

    fun smoothStep(t: Double): Double {
        val x = clamp01(t)
        return x * x * (3 - 2 * x)
    }

    /** Wrap an angle into (-PI, PI]. */
    fun wrapAngle(a: Double): Double {
        var x = a % (2 * PI)
        if (x > PI) x -= 2 * PI
        if (x <= -PI) x += 2 * PI
        return x
    }

    fun angleDelta(from: Double, to: Double): Double = wrapAngle(to - from)

    fun angleLerp(from: Double, to: Double, t: Double): Double = from + angleDelta(from, to) * clamp01(t)

    fun moveAngleTowards(current: Double, target: Double, maxDelta: Double): Double {
        val d = angleDelta(current, target)
        return if (abs(d) <= maxDelta) wrapAngle(target) else wrapAngle(current + sign(d) * maxDelta)
    }

    fun distance(ax: Double, az: Double, bx: Double, bz: Double): Double = hypot(bx - ax, bz - az)

    fun distanceSq(ax: Double, az: Double, bx: Double, bz: Double): Double {
        val dx = bx - ax
        val dz = bz - az
        return dx * dx + dz * dz
    }

    /** Heading of the vector (a -> b) in the XZ plane, 0 = +Z, growing clockwise. */
    fun headingBetween(ax: Double, az: Double, bx: Double, bz: Double): Double = atan2(bx - ax, bz - az)

    fun headingToVector(heading: Double): Pair<Double, Double> = sin(heading) to cos(heading)

    fun kmhToUnitsPerSecond(kmh: Double, unitsPerMetre: Double): Double = (kmh / 3.6) * unitsPerMetre
    fun unitsPerSecondToKmh(u: Double, unitsPerMetre: Double): Double = (u / unitsPerMetre) * 3.6

    fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0
    fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0

    fun round(value: Double, decimals: Int = 2): Double {
        val f = Math.pow(10.0, decimals.toDouble())
        return Math.round(value * f) / f
    }

    fun signOf(v: Double): Double = sign(v)
    fun signOf(v: Float): Float = sign(v)
    fun signOf(v: Int): Int = if (v > 0) 1 else if (v < 0) -1 else 0

    /** Deterministic hash in [0,1) - used for stable procedural decoration. */
    fun hashRandom(seed: Int): Double {
        var x = seed + -0x61c88647
        x = (x xor (x ushr 16)) * -0x7a143595
        x = (x xor (x ushr 13)) * -0x3d4d51cb
        x = x xor (x ushr 16)
        return (x.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }

    fun interpolateCatmullRom(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)
    }

    fun cosDeg(deg: Double): Double = cos(deg * DEG2RAD)
    fun sinDeg(deg: Double): Double = sin(deg * DEG2RAD)
}
