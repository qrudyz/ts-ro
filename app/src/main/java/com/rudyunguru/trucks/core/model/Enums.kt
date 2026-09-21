package com.rudyunguru.trucks.core.model

/** Graphics quality presets. Values are consumed by the OpenGL ES renderer. */
enum class GraphicsPreset(
    val label: String,
    val description: String,
    val shadowMapSize: Int,
    val shadowsEnabled: Boolean,
    val reflectionsEnabled: Boolean,
    val maxTrafficVehicles: Int,
    val drawDistanceMetres: Int,
    val rainParticles: Int,
    val vegetationDensity: Double,
    val textureScale: Double,
    val msaaEnabled: Boolean,
    val targetFps: Int,
    val dynamicResolution: Boolean,
) {
    LOW(
        "LOW", "Telefoane slabe. Umbre dezactivate, trafic redus.", 0, false, false,
        8, 320, 0, 0.35, 0.5, false, 30, true,
    ),
    MEDIUM(
        "MEDIUM", "Echilibrat. Umbre la rezoluție medie.", 1024, true, false,
        16, 520, 350, 0.6, 0.75, false, 45, true,
    ),
    HIGH(
        "HIGH", "Grafică ridicată, reflexii pe caroserie.", 2048, true, true,
        28, 720, 800, 0.85, 1.0, true, 60, false,
    ),
    ULTRA(
        "ULTRA", "Maxim. Pentru flagship-uri.", 4096, true, true,
        44, 950, 1500, 1.0, 1.0, true, 60, false,
    );

    companion object {
        fun fromName(name: String?): GraphicsPreset =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}

/** Steering input method. */
enum class ControlMode(val label: String) {
    VIRTUAL_WHEEL("Volan virtual"),
    BUTTONS("Butoane"),
    TILT("Înclinare / giroscop"),
}

/** Camera presets; mapped 1:1 to the OpenGL renderer camera modes. */
enum class CameraPreset(val label: String) {
    CABIN("Cabină"),
    DASHBOARD("Bord"),
    EXTERIOR_BACK("Exterior spate"),
    EXTERIOR_SIDE("Exterior lateral"),
    FREE("Cameră liberă");

    fun next(): CameraPreset = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): CameraPreset =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CABIN
    }
}

enum class GearboxKind(val label: String) {
    AUTOMATIC("Automată"),
    AUTOMATED("Automatizată"),
    MANUAL("Manuală"),
}

enum class DriveConfig(val label: String, val axles: Int, val drivenAxles: Int) {
    D4x2("4x2", 2, 1),
    D6x2("6x2", 3, 1),
    D6x4("6x4", 3, 2),
}

enum class CabinKind(val label: String, val comfortBonus: Double) {
    DAY_CAB("Cabină de zi", 0.0),
    SLEEPER("Cabină cu pat", 0.6),
    LUXURY_SLEEPER("Cabină premium", 1.0),
}

enum class TruckClass(val label: String) {
    STARTER("Începător"),
    MEDIUM("Mediu"),
    HEAVY("Grei"),
    FLAGSHIP("Flagship"),
}

enum class TrailerKind(val label: String) {
    CURTAINSIDER("Prelată"),
    REFRIGERATED("Frigorific"),
    TANKER("Cisternă"),
    FLATBED("Platformă"),
    CONTAINER("Container"),
    LOW_LOADER("Trailer coborât"),
}

enum class CargoKind(val label: String, val labelEn: String, val minTons: Double, val maxTons: Double) {
    FOOD("Alimente", "Food", 8.0, 22.0),
    GRAIN("Cereale", "Grain", 18.0, 26.0),
    TIMBER("Lemn", "Timber", 16.0, 24.0),
    VEHICLES("Mașini", "Vehicles", 6.0, 14.0),
    CONTAINERS("Containere", "Containers", 14.0, 26.0),
    CONSTRUCTION("Materiale construcții", "Construction", 20.0, 28.0),
    MACHINERY("Utilaje", "Machinery", 18.0, 32.0),
    REFRIGERATED("Marfă frigorifică", "Refrigerated", 10.0, 22.0),
}
