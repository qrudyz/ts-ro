package com.rudyunguru.trucks.core.model

enum class DriverLevel(
    val label: String,
    val xpRequired: Int,
    val salaryMultiplier: Double,
    val skillCap: Int,
    val accidentRate: Double,
) {
    BEGINNER("Începător", 0, 1.00, 40, 0.090),
    DRIVER("Șofer", 1200, 1.12, 55, 0.070),
    EXPERIENCED("Experimentat", 3600, 1.28, 70, 0.048),
    PROFESSIONAL("Profesionist", 8200, 1.46, 85, 0.030),
    EXPERT("Expert", 16000, 1.68, 100, 0.016);

    fun next(): DriverLevel? = entries.getOrNull(ordinal + 1)

    companion object {
        fun forXp(xp: Int): DriverLevel = entries.last { xp >= it.xpRequired }
    }
}

enum class DriverStatus(val label: String) {
    IDLE("Disponibil"),
    ON_ROUTE("În cursă"),
    RESTING("Odihnă"),
}

enum class TruckStatus(val label: String) {
    PARKED("În garaj"),
    DRIVING("Condus de tine"),
    ON_ROUTE("În cursă cu șofer"),
    BROKEN("Avariat"),
}

enum class GarageLevel(val label: String, val capacity: Int, val upgradeCostEuro: Long) {
    LEVEL_1("Level 1", 2, 78_000),
    LEVEL_2("Level 2", 5, 165_000),
    LEVEL_3("Level 3", 9, 320_000),
    PREMIUM("Premium", 15, 690_000);

    fun next(): GarageLevel? = entries.getOrNull(ordinal + 1)
}

enum class WeatherKind(val label: String, val grip: Double, val visibilityMetres: Int) {
    CLEAR("Senin", 1.00, 900),
    CLOUDY("Noros", 0.98, 780),
    RAIN("Ploaie", 0.80, 420),
    STORM("Furtună", 0.68, 300),
    FOG("Ceață", 0.92, 140),
}

enum class UnitsSystem(val label: String) {
    METRIC("km/h, °C"),
    IMPERIAL("mph, °F"),
}

enum class LanguageCode(val label: String, val code: String) {
    ROMANIAN("Română", "ro"),
    ENGLISH("English", "en"),
}

enum class NotificationKind { INFO, SUCCESS, WARNING, MONEY, LEVEL_UP, JOB }

/** Individual wear channels of an owned truck. */
enum class WearComponent(val label: String, val repairCostFactor: Double) {
    ENGINE("Motor", 1.35),
    GEARBOX("Transmisie", 1.20),
    BRAKES("Frâne", 0.75),
    TIRES("Anvelope", 0.60),
    BODY("Caroserie", 1.00),
}

/** Upgrade lines available in the garage / tuning screen. */
enum class UpgradeKind(
    val label: String,
    val maxLevel: Int,
    val basePriceEuro: Long,
    val effect: String,
    val icon: String,
) {
    ENGINE("Motor", 3, 42_000, "+12% putere și cuplu / nivel", "engine"),
    TRANSMISSION("Transmisie", 3, 26_000, "schimbări mai rapide, retarder mai puternic", "gearbox"),
    TANK("Rezervor", 3, 9_500, "+180 L capacitate / nivel", "tank"),
    BRAKES("Frâne", 3, 7_400, "distanță de frânare mai mică", "brake"),
    SUSPENSION("Suspensie", 2, 12_800, "stabilitate și confort mai bun", "suspension"),
    TIRES("Anvelope", 3, 4_200, "aderență mai bună, durată de viață mai mică", "tire"),
    GPS("GPS", 2, 3_100, "trasee optime, ETA mai precis", "gps"),
    LIGHTS("Lumini", 2, 5_600, "faruri mai puternice în ceață", "light"),
    INTERIOR("Interior", 2, 8_900, "aspect interior premium", "interior"),
    SEATS("Scaune", 2, 6_700, "confort: oboseală mai mică", "seat"),
    STEERING("Volan", 2, 4_800, "direcție mai precisă", "steering"),
    PAINT("Vopsea", 1, 1_200, "culoare personalizată", "paint"),
    ACCESSORIES("Accesorii", 3, 3_400, "bare de lumină, spoilere, deflectoare", "accessory");

    companion object {
        fun byIcon(icon: String): UpgradeKind? = entries.firstOrNull { it.icon == icon }
    }
}
