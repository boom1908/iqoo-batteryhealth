package com.example.model

enum class HealthDetectionMethod(val displayName: String) {
    ANDROID_14_API("Android 14+ StateOfHealth API"),
    SHIZUKU_DUMPSYS("Shizuku Dumpsys (mSavedBatteryAsoc)"),
    SYSFS_KERNEL("Kernel Power Supply Node"),
    COULOMB_ESTIMATE("Passive Coulomb Integration"),
    BASIC_BATTERY_MANAGER("Standard BatteryManager")
}

enum class DiagnosticStatus(val label: String) {
    OPTIMAL("Optimal"),
    NORMAL("Normal"),
    SERVICE_RECOMMENDED("Service Recommended"),
    UNKNOWN("Analyzing")
}

enum class ChargingState(val label: String) {
    CHARGING("Charging"),
    DISCHARGING("Discharging"),
    FULL("Full"),
    NOT_CHARGING("Idle / Not Charging"),
    UNKNOWN("Unknown")
}

data class BatteryHealthData(
    val healthPercentage: Int? = null,
    val detectionMethod: HealthDetectionMethod = HealthDetectionMethod.BASIC_BATTERY_MANAGER,
    val cycleCount: Int? = null,
    val currentCapacityMah: Double = 0.0,
    val designCapacityMah: Int = 5000,
    val isNominalOverridden: Boolean = false,
    val batteryLevel: Int = 0,
    val voltageMv: Int = 0,
    val temperatureCelsius: Double = 0.0,
    val currentNowMa: Double = 0.0,
    val wattageW: Double = 0.0,
    val chargingState: ChargingState = ChargingState.UNKNOWN,
    val pluggedSource: String = "Battery",
    val hasBatteryStatsPermission: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val diagnosticStatus: DiagnosticStatus = DiagnosticStatus.UNKNOWN,
    val rawDumpsysText: String? = null,
    val asocRaw: Int? = null,
    val usageRaw: Int? = null,
    val kernelChargeFull: Long? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

data class CoulombSession(
    val totalMilliampereHoursPassed: Double = 0.0,
    val sessionDurationSeconds: Long = 0,
    val isTracking: Boolean = false,
    val samplesCount: Int = 0
)
