package com.rudyunguru.trucks.core.model

/** An owned truck instance (catalog model + everything that can change while playing). */
data class OwnedTruck(
    val instanceId: String,
    val modelId: String,
    val plate: String,
    val paintHex: String,
    val garageCityId: String,
    val purchasedDay: Int = 0,
    val totalKm: Double = 0.0,
    /** Distance driven since the last service - drives wear accumulation. */
    val kmSinceService: Double = 0.0,
    val fuelL: Double = 0.0,
    val wear: TruckWear = TruckWear.NEW,
    val upgrades: TruckUpgrades = TruckUpgrades.NONE,
    val status: TruckStatus = TruckStatus.PARKED,
    val trailerInstanceId: String? = null,
    val driverId: String? = null,
    /** Lifetime earnings of this truck (own driving + driver revenue). */
    val lifetimeRevenueEuro: Long = 0,
    val lifetimeFuelCostEuro: Long = 0,
    val lifetimeRepairCostEuro: Long = 0,
    /** Performance snapshot used for the garage list: litres/100km average. */
    val averageConsumptionL100: Double = 0.0,
    val brokenDown: Boolean = false,
) {
    fun hasDriver(): Boolean = driverId != null
    fun hasTrailer(): Boolean = trailerInstanceId != null

    fun odometerDisplay(): String = com.rudyunguru.trucks.core.Format.km(totalKm)
}

/** An owned trailer instance. */
data class OwnedTrailer(
    val instanceId: String,
    val modelId: String,
    val plate: String,
    val garageCityId: String,
    val purchasedDay: Int = 0,
    val totalKm: Double = 0.0,
    val wearTires: Double = 0.0,
    val wearBody: Double = 0.0,
    val allocatedTruckId: String? = null,
) {
    fun conditionPercent(): Double =
        (100.0 - ((wearTires * 0.5 + wearBody * 0.5)).coerceIn(0.0, 100.0)).coerceIn(0.0, 100.0)

    fun isAllocated(): Boolean = allocatedTruckId != null
}

/** A driver that the player can hire, fire and level up. */
data class Driver(
    val id: String,
    val name: String,
    val avatarIndex: Int,
    val age: Int,
    val cityId: String,
    val level: DriverLevel = DriverLevel.BEGINNER,
    val xp: Int = 0,
    /** 0..100 driving skill: affects fuel use, accidents and average speed. */
    val skill: Int = 25,
    val baseSalaryPerDay: Long = 92,
    /** Multiplier applied to ideal fuel consumption (lower is better). */
    val fuelEconomy: Double = 1.06,
    /** Probability of an incident per 1000 km. */
    val accidentRate: Double = 0.06,
    /** How much of the contract payout this driver collects (higher is better). */
    val profitability: Double = 0.92,
    val status: DriverStatus = DriverStatus.IDLE,
    val hiredDay: Int = 0,
    val assignedTruckId: String? = null,
    val currentRoute: DriverRoute? = null,
    val lifetimeRevenueEuro: Long = 0,
    val lifetimeCostEuro: Long = 0,
    val tripsCompleted: Int = 0,
) {
    fun dailySalary(): Long = (baseSalaryPerDay * level.salaryMultiplier).toLong()

    /** Effective skill capped by the current level. */
    fun effectiveSkill(): Int = skill.coerceIn(0, level.skillCap)

    fun isFree(): Boolean = status == DriverStatus.IDLE && assignedTruckId == null

    fun xpProgressToNextLevel(): Double {
        val next = level.next() ?: return 1.0
        val span = (next.xpRequired - level.xpRequired).coerceAtLeast(1)
        return ((xp - level.xpRequired).toDouble() / span).coerceIn(0.0, 1.0)
    }

    fun withXpGained(amount: Int): Driver {
        val newXp = xp + amount
        val newLevel = DriverLevel.forXp(newXp)
        // Skill grows slowly with every level-up and a little from experience points.
        val levelsGained = newLevel.ordinal - level.ordinal
        val skillGrowth = if (levelsGained > 0) 6 * levelsGained else if (amount >= 150) 1 else 0
        return copy(xp = newXp, level = newLevel, skill = (skill + skillGrowth).coerceAtMost(100))
    }
}

/** Route currently being driven by an employed driver (simplified, simulated over game time). */
data class DriverRoute(
    val contractId: String,
    val fromCityId: String,
    val toCityId: String,
    val startedDay: Int,
    val startedMinutes: Long,
    val totalMinutes: Long,
    val elapsedMinutes: Long = 0,
    val payoutEuro: Long,
    val fuelCostEuro: Long,
    val cargo: CargoKind,
    val weightTons: Double,
) {
    fun progress(): Double = if (totalMinutes <= 0) 1.0 else (elapsedMinutes.toDouble() / totalMinutes).coerceIn(0.0, 1.0)

    fun remainingMinutes(): Long = (totalMinutes - elapsedMinutes).coerceAtLeast(0)

    fun isFinished(): Boolean = elapsedMinutes >= totalMinutes
}
