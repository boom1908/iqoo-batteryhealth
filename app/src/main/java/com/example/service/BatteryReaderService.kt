package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.BatteryHealthData
import com.example.model.ChargingState
import com.example.model.CoulombSession
import com.example.model.DeviceDatabase
import com.example.model.DiagnosticStatus
import com.example.model.HealthDetectionMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.abs

/**
 * BatteryReaderService orchestrates the low-level battery health inspection
 * across three primary fallback layers:
 *
 * 1. Method A: Android 14+ standard BatteryManager API (BATTERY_PROPERTY_STATE_OF_HEALTH).
 * 2. Method B: Shizuku / Privileged Shell querying "dumpsys battery" and kernel sysfs nodes,
 *    targeting Vivo & iQOO's proprietary Funtouch OS fields:
 *      - mSavedBatteryAsoc: [0-100] (State of Capacity / Actual Health %)
 *      - mSavedBatteryUsage: Integer (Total battery charge cycles)
 * 3. Method C: Real-time passive Coulomb integration fallback via ACTION_BATTERY_CHANGED
 *    and BATTERY_PROPERTY_CURRENT_NOW integration over charge sessions.
 */
class BatteryReaderService(private val context: Context) {

    companion object {
        private const val TAG = "BatteryReaderService"
        const val SHIZUKU_REQUEST_CODE = 2001

        // Android 14 (API 34) introduced BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH = 7
        const val BATTERY_PROPERTY_STATE_OF_HEALTH = 7
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _batteryData = MutableStateFlow(BatteryHealthData())
    val batteryData: StateFlow<BatteryHealthData> = _batteryData.asStateFlow()

    private val _coulombSession = MutableStateFlow(CoulombSession())
    val coulombSession: StateFlow<CoulombSession> = _coulombSession.asStateFlow()

    private var lastCoulombSampleTimeMs = 0L
    private var initialChargeLevel = -1

    // BroadcastReceiver for instant battery changes (voltage, temperature, plug status)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                processBatteryChangedIntent(intent)
            }
        }
    }

    init {
        registerBatteryReceiver()
        refreshAll()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver not registered: ${e.message}")
        }
    }

    /**
     * Checks if the app has been granted android.permission.BATTERY_STATS via ADB.
     */
    fun hasBatteryStatsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            "android.permission.BATTERY_STATS"
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if Shizuku service is running and binder is reachable.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if this app has been granted Shizuku authorization.
     */
    fun isShizukuPermissionGranted(): Boolean {
        return try {
            isShizukuAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Requests Shizuku permission from the user.
     */
    fun requestShizukuPermission() {
        try {
            if (isShizukuAvailable()) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Main entry point to refresh all metrics across the hierarchy.
     */
    fun refreshAll() {
        serviceScope.launch {
            val hasStats = hasBatteryStatsPermission()
            val shizukuAvail = isShizukuAvailable()
            val shizukuGranted = isShizukuPermissionGranted()

            val (designCap, isOverridden) = DeviceDatabase.getEffectiveDesignCapacity(context)

            // Step 1: Baseline system battery properties
            val baseline = queryBaselineProperties(hasStats, shizukuAvail, shizukuGranted, designCap, isOverridden)

            // Step 2: Try Method A (Android 14+ StateOfHealth API)
            val android14Health = queryAndroid14Health()

            // Step 3: Try Method B (Shizuku / Shell Dumpsys & Kernel Sysfs)
            val shellResult = queryShizukuOrPrivilegedShell()

            // Step 4: Fallback to Kernel sysfs directly if readable
            val directKernelResult = queryDirectKernelNodes()

            // Step 5: Synthesize best data according to hierarchy
            val synthesized = resolveHighestAccuracyData(
                baseline = baseline,
                android14Health = android14Health,
                shellResult = shellResult,
                kernelResult = directKernelResult,
                designCapacity = designCap
            )

            _batteryData.value = synthesized
        }
    }

    /**
     * Reads baseline properties available via standard BatteryManager & cached intent.
     */
    private fun queryBaselineProperties(
        hasStats: Boolean,
        shizukuAvail: Boolean,
        shizukuGranted: Boolean,
        designCap: Int,
        isOverridden: Boolean
    ): BatteryHealthData {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempRaw / 10.0

        val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargingState = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
            else -> ChargingState.UNKNOWN
        }

        val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pluggedSource = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast Charger (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> "Battery Discharging"
        }

        // Current now in microamperes (µA) -> convert to milliamperes (mA)
        val currentNowMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentNowMa = if (currentNowMicro != Int.MIN_VALUE) currentNowMicro / 1000.0 else 0.0

        // Charge counter in microampere-hours (µAh) -> convert to mAh
        val chargeCounterUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val currentCapMah = if (chargeCounterUah > 0 && chargeCounterUah != Int.MIN_VALUE) {
            chargeCounterUah / 1000.0
        } else {
            // Rough estimation from level %
            (batteryPct / 100.0) * designCap
        }

        // Real-time power in Watts = Voltage (V) * Current (A)
        val wattage = abs((voltageMv / 1000.0) * (currentNowMa / 1000.0))

        return BatteryHealthData(
            batteryLevel = batteryPct,
            voltageMv = voltageMv,
            temperatureCelsius = tempC,
            currentNowMa = currentNowMa,
            wattageW = wattage,
            chargingState = chargingState,
            pluggedSource = pluggedSource,
            currentCapacityMah = currentCapMah,
            designCapacityMah = designCap,
            isNominalOverridden = isOverridden,
            hasBatteryStatsPermission = hasStats,
            isShizukuAvailable = shizukuAvail,
            isShizukuPermissionGranted = shizukuGranted,
            detectionMethod = HealthDetectionMethod.BASIC_BATTERY_MANAGER
        )
    }

    /**
     * Method A: Query Android 14+ BATTERY_PROPERTY_STATE_OF_HEALTH.
     */
    private fun queryAndroid14Health(): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return try {
                val health = batteryManager.getIntProperty(BATTERY_PROPERTY_STATE_OF_HEALTH)
                if (health in 1..100) health else null
            } catch (e: Exception) {
                Log.w(TAG, "Android 14 StateOfHealth query failed: ${e.message}")
                null
            }
        }
        return null
    }

    /**
     * Method B: Execute privileged dumpsys battery using Shizuku or local privileged process.
     */
    private suspend fun queryShizukuOrPrivilegedShell(): ShellDumpsysResult? = withContext(Dispatchers.IO) {
        val rawOutput = when {
            isShizukuPermissionGranted() -> executeViaShizuku("dumpsys battery")
            hasBatteryStatsPermission() -> executeLocalShell("dumpsys battery")
            else -> null
        } ?: return@withContext null

        parseDumpsysOutput(rawOutput)
    }

    /**
     * Parses the raw output of "dumpsys battery" specifically targeting Vivo & iQOO fields.
     */
    private fun parseDumpsysOutput(raw: String): ShellDumpsysResult {
        // Vivo / iQOO Funtouch OS & OriginOS proprietary regex patterns:
        // mSavedBatteryAsoc: 98 (ASOC = Actual State of Capacity in percentage)
        val asocMatch = Regex("""mSavedBatteryAsoc:\s*(\d+)""").find(raw)
        val asoc = asocMatch?.groupValues?.get(1)?.toIntOrNull()

        // mSavedBatteryUsage: 142 (Hardware charge cycle counter recorded by Vivo battery service)
        val usageMatch = Regex("""mSavedBatteryUsage:\s*(\d+)""").find(raw)
        val usageCycles = usageMatch?.groupValues?.get(1)?.toIntOrNull()

        // Standard Android battery dumpsys fields
        val chargeCounterMatch = Regex("""Charge counter:\s*(\d+)""").find(raw)
        val chargeCounterUah = chargeCounterMatch?.groupValues?.get(1)?.toLongOrNull()

        val cycleCountMatch = Regex("""cycle counter:\s*(\d+)""", RegexOption.IGNORE_CASE).find(raw)
        val generalCycles = cycleCountMatch?.groupValues?.get(1)?.toIntOrNull()

        val effectiveCycles = usageCycles ?: generalCycles

        return ShellDumpsysResult(
            rawText = raw,
            asocHealth = asoc,
            cycleCount = effectiveCycles,
            chargeCounterUah = chargeCounterUah
        )
    }

    /**
     * Attempts direct reading of kernel sysfs nodes in /sys/class/power_supply/battery/
     */
    private fun queryDirectKernelNodes(): KernelSysfsResult? {
        val paths = listOf(
            "/sys/class/power_supply/battery",
            "/sys/class/power_supply/bms"
        )

        for (base in paths) {
            try {
                val chargeFullFile = File("$base/charge_full")
                val cycleCountFile = File("$base/cycle_count")

                val chargeFull = if (chargeFullFile.exists() && chargeFullFile.canRead()) {
                    chargeFullFile.readText().trim().toLongOrNull()
                } else null

                val cycles = if (cycleCountFile.exists() && cycleCountFile.canRead()) {
                    cycleCountFile.readText().trim().toIntOrNull()
                } else null

                if (chargeFull != null || cycles != null) {
                    return KernelSysfsResult(chargeFullUah = chargeFull, cycles = cycles)
                }
            } catch (e: Exception) {
                // Ignore sysfs permission denials in sandbox
            }
        }
        return null
    }

    /**
     * Resolves the final BatteryHealthData, strictly prioritizing:
     * 1. Vivo/iQOO Shizuku/ADB dumpsys (mSavedBatteryAsoc & mSavedBatteryUsage)
     * 2. Android 14+ BATTERY_PROPERTY_STATE_OF_HEALTH
     * 3. Sysfs kernel nodes
     * 4. Coulomb measurement session estimate
     * 5. Standard BatteryManager
     */
    private fun resolveHighestAccuracyData(
        baseline: BatteryHealthData,
        android14Health: Int?,
        shellResult: ShellDumpsysResult?,
        kernelResult: KernelSysfsResult?,
        designCapacity: Int
    ): BatteryHealthData {
        var finalHealth: Int? = null
        var finalMethod: HealthDetectionMethod = HealthDetectionMethod.BASIC_BATTERY_MANAGER
        var finalCycles: Int? = null
        var calculatedCapacityMah: Double = baseline.currentCapacityMah

        // Priority 1: Shizuku / Dumpsys Funtouch ASOC
        if (shellResult?.asocHealth != null && shellResult.asocHealth in 1..100) {
            finalHealth = shellResult.asocHealth
            finalMethod = HealthDetectionMethod.SHIZUKU_DUMPSYS
            finalCycles = shellResult.cycleCount
            if (shellResult.chargeCounterUah != null && shellResult.chargeCounterUah > 0) {
                calculatedCapacityMah = shellResult.chargeCounterUah / 1000.0
            }
        }
        // Priority 2: Android 14+ API
        else if (android14Health != null && android14Health in 1..100) {
            finalHealth = android14Health
            finalMethod = HealthDetectionMethod.ANDROID_14_API
            finalCycles = shellResult?.cycleCount ?: kernelResult?.cycles
        }
        // Priority 3: Kernel sysfs
        else if (kernelResult?.chargeFullUah != null && kernelResult.chargeFullUah > 100000) {
            val fullMah = kernelResult.chargeFullUah / 1000.0
            val pct = ((fullMah / designCapacity) * 100).toInt()
            finalHealth = pct.coerceIn(10, 110)
            finalMethod = HealthDetectionMethod.SYSFS_KERNEL
            finalCycles = kernelResult.cycles
            calculatedCapacityMah = fullMah
        }
        // Priority 4: Coulomb estimation if valid session exists
        else if (_coulombSession.value.totalMilliampereHoursPassed > 500 && _coulombSession.value.samplesCount > 10) {
            val session = _coulombSession.value
            val projected = (session.totalMilliampereHoursPassed / designCapacity) * 100
            finalHealth = projected.toInt().coerceIn(30, 100)
            finalMethod = HealthDetectionMethod.COULOMB_ESTIMATE
        }

        // Determine diagnostic status
        val diagnosticStatus = when {
            finalHealth == null -> DiagnosticStatus.UNKNOWN
            finalHealth >= 85 -> DiagnosticStatus.OPTIMAL
            finalHealth in 80..84 -> DiagnosticStatus.NORMAL
            else -> DiagnosticStatus.SERVICE_RECOMMENDED
        }

        return baseline.copy(
            healthPercentage = finalHealth,
            detectionMethod = finalMethod,
            cycleCount = finalCycles,
            currentCapacityMah = calculatedCapacityMah,
            diagnosticStatus = diagnosticStatus,
            rawDumpsysText = shellResult?.rawText,
            asocRaw = shellResult?.asocHealth,
            usageRaw = shellResult?.cycleCount,
            kernelChargeFull = kernelResult?.chargeFullUah
        )
    }

    /**
     * Executes shell command via Shizuku privileged IPC using reflection to invoke newProcess.
     */
    private fun executeViaShizuku(command: String): String? {
        return try {
            val cmdArray = arrayOf("sh", "-c", command)
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            val remoteProcess = newProcessMethod.invoke(null, cmdArray, null, null) ?: return null
            val processClass = remoteProcess.javaClass

            val getInputStreamMethod = processClass.getMethod("getInputStream")
            val inputStream = getInputStreamMethod.invoke(remoteProcess) as java.io.InputStream

            val output = inputStream.bufferedReader().use { it.readText() }

            try {
                processClass.getMethod("destroy").invoke(remoteProcess)
            } catch (e: Exception) {
                // Ignore cleanup error
            }

            output.ifBlank { null }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku exec error for '$command': ${e.message}")
            null
        }
    }

    /**
     * Executes local shell command when granted android.permission.BATTERY_STATS / DUMP.
     */
    private fun executeLocalShell(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            output.ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "Local shell execution error: ${e.message}")
            null
        }
    }

    /**
     * Process real-time changes and update Coulomb measurement during charge.
     */
    private fun processBatteryChangedIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

        val now = SystemClock.elapsedRealtime()

        if (isCharging) {
            if (lastCoulombSampleTimeMs > 0) {
                val dtMs = now - lastCoulombSampleTimeMs
                if (dtMs in 500..30000) { // Valid sampling window
                    val currentNowMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val currentNowMa = if (currentNowMicro != Int.MIN_VALUE) abs(currentNowMicro / 1000.0) else 1500.0

                    val dtHours = dtMs / (1000.0 * 3600.0)
                    val passedMah = currentNowMa * dtHours

                    val currentSession = _coulombSession.value
                    _coulombSession.value = currentSession.copy(
                        totalMilliampereHoursPassed = currentSession.totalMilliampereHoursPassed + passedMah,
                        sessionDurationSeconds = currentSession.sessionDurationSeconds + (dtMs / 1000),
                        isTracking = true,
                        samplesCount = currentSession.samplesCount + 1
                    )
                }
            } else {
                initialChargeLevel = level
            }
            lastCoulombSampleTimeMs = now
        } else {
            lastCoulombSampleTimeMs = 0L
            if (_coulombSession.value.isTracking) {
                _coulombSession.value = _coulombSession.value.copy(isTracking = false)
            }
        }
    }

    fun resetCoulombSession() {
        _coulombSession.value = CoulombSession()
        lastCoulombSampleTimeMs = 0L
        initialChargeLevel = -1
    }

    private data class ShellDumpsysResult(
        val rawText: String,
        val asocHealth: Int?,
        val cycleCount: Int?,
        val chargeCounterUah: Long?
    )

    private data class KernelSysfsResult(
        val chargeFullUah: Long?,
        val cycles: Int?
    )
}
