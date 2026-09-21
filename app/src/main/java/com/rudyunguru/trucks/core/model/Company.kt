package com.rudyunguru.trucks.core.model

/** The player's company (created after the first delivered contracts). */
data class Company(
    val name: String = "Compania mea",
    val logoId: Int = 0,
    val colorHex: String = "#FFD166",
    val hqCityId: String = "bucuresti",
    val created: Boolean = false,
    val createdDay: Int = 0,
) {
    val initials: String
        get() = name.split(' ').filter { it.isNotBlank() }.take(2).map { it.first().uppercaseChar() }.joinToString("")
}

/** Bank loan used to grow the fleet. */
data class Loan(
    val id: String,
    val principalEuro: Long,
    val remainingEuro: Long,
    val dailyPaymentEuro: Long,
    val dailyInterestRate: Double,
    val takenDay: Int,
    val termDays: Int,
    val daysPaid: Int = 0,
    val description: String,
) {
    fun progress(): Double = if (principalEuro <= 0) 1.0 else 1.0 - remainingEuro.toDouble() / principalEuro

    fun isPaidOff(): Boolean = remainingEuro <= 0

    fun remainingDays(): Int = (termDays - daysPaid).coerceAtLeast(0)
}

/** A garage in a city; its level decides how many trucks fit inside. */
data class Garage(
    val cityId: String,
    val level: GarageLevel = GarageLevel.LEVEL_1,
    val purchasedDay: Int = 0,
    val truckInstanceIds: List<String> = emptyList(),
) {
    fun capacity(): Int = level.capacity
    fun used(): Int = truckInstanceIds.size
    fun freeSlots(): Int = (capacity() - used()).coerceAtLeast(0)
    fun isFull(): Boolean = freeSlots() == 0
}

/** Player progression. */
data class PlayerProfile(
    val name: String = "Șofer",
    val avatarIndex: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    /** Enough for a used starter unit or a deposit on a loan-financed MAN TGL. */
    val moneyEuro: Long = 45_000,
    val reputation: Int = 0,
    val currentCityId: String = "bucuresti",
) {
    fun xpForNextLevel(): Int = level * 900

    fun xpProgress(): Double = (xp.toDouble() / xpForNextLevel().coerceAtLeast(1)).coerceIn(0.0, 1.0)

    fun reputationLabel(): String = when {
        reputation >= 90 -> "Legendar"
        reputation >= 70 -> "Expert"
        reputation >= 50 -> "Respectat"
        reputation >= 25 -> "Cunoscut"
        else -> "Începător"
    }

    /** Share of the market the player can access, driven by reputation. */
    fun marketAccess(): Double = (0.35 + reputation / 100.0 * 0.65).coerceIn(0.0, 1.0)
}

/** Lifetime statistics shown in the company dashboard. */
data class Statistics(
    val totalKm: Double = 0.0,
    val totalJobs: Int = 0,
    val perfectDeliveries: Int = 0,
    val lateDeliveries: Int = 0,
    val failedDeliveries: Int = 0,
    val accidents: Int = 0,
    val policeStops: Int = 0,
    val totalRevenueEuro: Long = 0,
    val totalFuelCostEuro: Long = 0,
    val totalRepairCostEuro: Long = 0,
    val totalSalaryCostEuro: Long = 0,
    val totalFinesEuro: Long = 0,
    val totalLoanPaymentsEuro: Long = 0,
    val litersUsed: Double = 0.0,
    val longestTripKm: Double = 0.0,
    val bestContractEuro: Long = 0,
    val daysPlayed: Int = 0,
) {
    fun netProfitEuro(): Long =
        totalRevenueEuro - totalFuelCostEuro - totalRepairCostEuro - totalSalaryCostEuro - totalFinesEuro - totalLoanPaymentsEuro

    fun averageConsumption(km: Double = totalKm): Double = if (km <= 0) 0.0 else litersUsed / km * 100.0
}

/** Toast / notification entry rendered by the UI layer. */
data class GameNotification(
    val id: Long,
    val kind: NotificationKind,
    val title: String,
    val message: String,
    val day: Int,
    val minutes: Long,
    val moneyDeltaEuro: Long = 0,
    val read: Boolean = false,
)
