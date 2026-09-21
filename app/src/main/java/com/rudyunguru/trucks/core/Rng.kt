package com.rudyunguru.trucks.core

/**
 * Deterministic random source. Every procedural element (world decoration, generated drivers,
 * generated contracts, traffic variety) derives from a seeded [Rng] so a save game rebuilds the
 * exact same world on every device.
 */
class Rng(seed: Int = 1337) {

    private var state: Int = if (seed == 0) 1337 else seed

    fun nextDouble(): Double {
        state += -0x61c88647
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t + ((t xor (t ushr 7)) * (t or 61))
        t = t xor (t ushr 14)
        return (t.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }

    fun range(min: Double, max: Double): Double = min + nextDouble() * (max - min)
    fun range(min: Float, max: Float): Float = (min + nextDouble() * (max - min)).toFloat()
    fun range(min: Int, max: Int): Int = min + (nextDouble() * (max - min + 1)).toInt().coerceAtMost(max - min)
    fun int(min: Int, max: Int): Int = if (max <= min) min else min + (nextDouble() * (max - min + 1)).toInt().coerceIn(0, max - min)

    fun bool(chance: Double = 0.5): Boolean = nextDouble() < chance

    fun <T> pick(items: List<T>): T = items[int(0, items.size - 1)]

    fun <T> pickOrNull(items: List<T>): T? = if (items.isEmpty()) null else pick(items)

    fun <T> shuffle(items: List<T>): List<T> {
        val arr = items.toMutableList()
        for (i in arr.size - 1 downTo 1) {
            val j = int(0, i)
            val tmp = arr[i]
            arr[i] = arr[j]
            arr[j] = tmp
        }
        return arr
    }

    fun <T> weighted(items: List<T>, weights: List<Double>): T {
        var total = 0.0
        for (w in weights) total += if (w > 0) w else 0.0
        var roll = nextDouble() * total
        for (i in items.indices) {
            roll -= if (weights[i] > 0) weights[i] else 0.0
            if (roll <= 0.0) return items[i]
        }
        return items.last()
    }

    fun fork(salt: Int = 0): Rng = Rng(int(0, 0xFFFFFF) xor (salt + 1) * -0x61c88647)

    fun stateSnapshot(): Int = state
    fun restore(state: Int) { this.state = if (state == 0) 1337 else state }
}
