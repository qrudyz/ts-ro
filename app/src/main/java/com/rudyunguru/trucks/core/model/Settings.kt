package com.rudyunguru.trucks.core.model

/** Everything the settings screen can change. Persisted with the save file. */
data class GameSettings(
    val graphics: GraphicsPreset = GraphicsPreset.MEDIUM,
    val controlMode: ControlMode = ControlMode.VIRTUAL_WHEEL,
    val camera: CameraPreset = CameraPreset.CABIN,
    val hudVisible: Boolean = true,
    val steeringSensitivity: Double = 1.0,
    val steeringDeadZone: Double = 0.05,
    val autoGearbox: Boolean = true,
    val cruiseControlAvailable: Boolean = true,
    val tiltInvert: Boolean = false,
    val masterVolume: Double = 0.9,
    val engineVolume: Double = 0.95,
    val sfxVolume: Double = 0.85,
    val uiVolume: Double = 0.7,
    val voiceVolume: Double = 0.9,
    val voiceGuidance: Boolean = true,
    val vibration: Boolean = true,
    val cameraShake: Double = 0.6,
    val adaptiveResolution: Boolean = true,
    val units: UnitsSystem = UnitsSystem.METRIC,
    val language: LanguageCode = LanguageCode.ROMANIAN,
    val trafficDensity: Double = 0.7,
    val trafficEnabled: Boolean = true,
    val weatherEnabled: Boolean = true,
    val dayNightCycle: Boolean = true,
    /** In-game minutes per real minute while driving. */
    val timeScale: Double = 1.0,
    val policeEnabled: Boolean = true,
    val autoSave: Boolean = true,
    val speedLimitWarnings: Boolean = true,
    val invertLook: Boolean = false,
    val leftHandedControls: Boolean = false,
    val difficulty: Double = 1.0,
) {
    /** 30/60 fps cap requested by the current quality preset. */
    val targetFps: Int get() = graphics.targetFps
}
