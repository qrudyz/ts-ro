package com.rudyunguru.trucks.core.model

enum class ContractStatus { AVAILABLE, IN_PROGRESS, ASSIGNED_TO_DRIVER, COMPLETED, FAILED, EXPIRED }

/** A freight contract offered on the market. */
data class Contract(
    val id: String,
    val cargo: CargoKind,
    val fromCityId: String,
    val toCityId: String,
    val weightTons: Double,
    val distanceKm: Double,
    val payoutEuro: Long,
    /** In-game minutes available from acceptance to delivery. */
    val deadlineMinutes: Long,
    val recommendedTruckClass: TruckClass,
    /** Some cargo can only be hauled with a specific trailer. */
    val requiredTrailerKind: TrailerKind?,
    val reputationRequirement: Int,
    /** 0 = relaxed, 1 = very tight. Shown in the contract card. */
    val urgency: Double,
    val postedDay: Int,
    val expiresDay: Int,
    val shipper: String,
    val status: ContractStatus = ContractStatus.AVAILABLE,
    /** Money lost when the deadline is missed. */
    val penaltyEuro: Long = 0,
) {
    val routeLabel: String get() = "${fromCityId.upperFirst()} → ${toCityId.upperFirst()}"

    fun isUrgent(): Boolean = urgency >= 0.66

    fun payoutPerKm(): Double = if (distanceKm <= 0) 0.0 else payoutEuro / distanceKm

    companion object {
        private fun String.upperFirst(): String =
            replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/** State of the contract the player is currently driving themselves. */
data class ActiveJob(
    val contract: Contract,
    val acceptedDay: Int,
    val acceptedMinutes: Long,
    /** In-game minutes remaining before the deadline. */
    val remainingMinutes: Long,
    val drivenKm: Double = 0.0,
    /** 0..100 - increases on collisions and hard braking. */
    val cargoDamage: Double = 0.0,
    val finesEuro: Long = 0,
    val fuelCostEuro: Long = 0,
    /** 0..1 progress along the planned route. */
    val routeProgress: Double = 0.0,
    val nextManeuverIndex: Int = 0,
    val deliveredPerfect: Boolean = false,
) {
    fun deadlineProgress(): Double {
        val total = contract.deadlineMinutes.coerceAtLeast(1)
        val spent = (total - remainingMinutes).coerceAtLeast(0)
        return (spent.toDouble() / total).coerceIn(0.0, 1.0)
    }

    fun isLate(): Boolean = remainingMinutes <= 0

    fun cargoConditionPercent(): Double = (100.0 - cargoDamage).coerceIn(0.0, 100.0)

    /** Final payout after damage and fines are taken into account. */
    fun projectedPayoutEuro(): Long {
        val damagePenalty = (contract.payoutEuro * (cargoDamage / 100.0) * 0.9).toLong()
        val latePenalty = if (isLate()) contract.penaltyEuro else 0L
        return (contract.payoutEuro - damagePenalty - latePenalty - finesEuro).coerceAtLeast(0)
    }
}

/** A used truck listed by the dealer (adds the second-hand market to the showroom). */
data class UsedTruckListing(
    val instanceId: String,
    val modelId: String,
    val priceEuro: Long,
    val km: Double,
    val conditionPercent: Double,
    val plate: String,
    val paintHex: String,
    val upgrades: TruckUpgrades = TruckUpgrades.NONE,
) {
    fun discountVsNew(newPriceEuro: Long): Double =
        if (newPriceEuro <= 0) 0.0 else ((newPriceEuro - priceEuro).toDouble() / newPriceEuro).coerceIn(0.0, 1.0)
}
