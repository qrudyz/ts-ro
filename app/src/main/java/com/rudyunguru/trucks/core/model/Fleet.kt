package com.rudyunguru.trucks.core.model

/** Catalog entry: a truck that can be bought from the dealer (original, fictional design). */
data class TruckModel(
    val id: String,
    val brand: String,
    val name: String,
    val truckClass: TruckClass,
    val priceEuro: Long,
    val powerHp: Int,
    val torqueNm: Int,
    val displacementL: Double,
    val gearbox: GearboxKind,
    val gearCount: Int,
    val driveConfig: DriveConfig,
    val curbWeightKg: Int,
    val maxGrossWeightKg: Int,
    val fuelTankL: Int,
    /** Litres / 100 km at 80 km/h with 50% load - the reference point of the consumption model. */
    val baseConsumptionL100: Double,
    val topSpeedKmh: Int,
    val cabin: CabinKind,
    val rating: Double,
    /** Procedural mesh variant used by the renderer, so every model looks different. */
    val meshVariant: Int,
    val paintOptions: List<String>,
    val description: String,
    val serviceCostEuro: Long,
    val resaleFactor: Double = 0.62,
) {
    val fullName: String get() = "$brand $name"

    /** Real payload available with a full tank, in tonnes. */
    val maxPayloadTons: Double get() = (maxGrossWeightKg - curbWeightKg) / 1000.0

    val hpPerTon: Double get() = powerHp / (maxGrossWeightKg / 1000.0)

    fun label(): String = "${fullName} (${truckClass.label})"
}

/** Catalog entry: a trailer that can be bought from the trailer dealer. */
data class TrailerModel(
    val id: String,
    val name: String,
    val kind: TrailerKind,
    val priceEuro: Long,
    /** Load capacity in tonnes. */
    val capacityTons: Double,
    val tareWeightKg: Int,
    val axleCount: Int,
    val allowedCargo: List<CargoKind>,
    val meshVariant: Int,
    val description: String,
    /** Higher value = cargo stays in perfect condition; refrigerated/tanker units are sensitive. */
    val maintenanceCostEuro: Long,
    val resaleFactor: Double = 0.58,
) {
    fun canHaul(cargo: CargoKind): Boolean = allowedCargo.contains(cargo)
}

/** Installed upgrade levels per truck. */
data class TruckUpgrades(val levels: Map<UpgradeKind, Int> = emptyMap()) {

    fun levelOf(kind: UpgradeKind): Int = (levels[kind] ?: 0).coerceIn(0, kind.maxLevel)

    fun isMaxed(kind: UpgradeKind): Boolean = levelOf(kind) >= kind.maxLevel

    fun priceForNextLevel(kind: UpgradeKind): Long {
        val level = levelOf(kind)
        if (level >= kind.maxLevel) return 0
        return (kind.basePriceEuro * (1.0 + level * 0.85)).toLong()
    }

    fun withLevel(kind: UpgradeKind, level: Int): TruckUpgrades {
        val clamped = level.coerceIn(0, kind.maxLevel)
        val next = HashMap(levels)
        if (clamped == 0) next.remove(kind) else next[kind] = clamped
        return TruckUpgrades(next)
    }

    fun totalInvestedEuro(): Long = levels.entries.sumOf { (kind, level) ->
        var sum = 0L
        var price = kind.basePriceEuro
        for (i in 0 until level.coerceAtMost(kind.maxLevel)) {
            sum += price
            price = (price * 1.85).toLong()
        }
        sum
    }

    /** Convenience accessors used by the HUD / showroom comparison table. */
    val engineLevel: Int get() = levelOf(UpgradeKind.ENGINE)
    val tankLevel: Int get() = levelOf(UpgradeKind.TANK)
    val brakeLevel: Int get() = levelOf(UpgradeKind.BRAKES)
    val gpsLevel: Int get() = levelOf(UpgradeKind.GPS)
    val lightLevel: Int get() = levelOf(UpgradeKind.LIGHTS)

    companion object {
        val NONE = TruckUpgrades()
    }
}

/** Wear channels, 0 = new, 100 = destroyed. */
data class TruckWear(
    val engine: Double = 0.0,
    val gearbox: Double = 0.0,
    val brakes: Double = 0.0,
    val tires: Double = 0.0,
    val body: Double = 0.0,
) {
    fun of(component: WearComponent): Double = when (component) {
        WearComponent.ENGINE -> engine
        WearComponent.GEARBOX -> gearbox
        WearComponent.BRAKES -> brakes
        WearComponent.TIRES -> tires
        WearComponent.BODY -> body
    }

    fun with(component: WearComponent, value: Double): TruckWear {
        val v = value.coerceIn(0.0, 100.0)
        return when (component) {
            WearComponent.ENGINE -> copy(engine = v)
            WearComponent.GEARBOX -> copy(gearbox = v)
            WearComponent.BRAKES -> copy(brakes = v)
            WearComponent.TIRES -> copy(tires = v)
            WearComponent.BODY -> copy(body = v)
        }
    }

    fun add(component: WearComponent, delta: Double): TruckWear = with(component, of(component) + delta)

    /** 0 = perfect, 1 = wrecked. */
    fun severity(): Double {
        val sum = engine * 1.2 + gearbox * 1.15 + brakes * 0.9 + tires * 0.95 + body * 0.8
        val weights = 1.2 + 1.15 + 0.9 + 0.95 + 0.8
        return (sum / weights / 100.0).coerceIn(0.0, 1.0)
    }

    /** 100 = mint condition (the "Stare" value shown in the dealer / garage screens). */
    fun conditionPercent(): Double = (100.0 - severity() * 100.0).coerceIn(0.0, 100.0)

    fun isBroken(): Boolean = engine >= 100.0 || gearbox >= 100.0

    fun worst(): WearComponent = WearComponent.entries.maxBy { of(it) }

    companion object {
        val NEW = TruckWear()
    }
}
