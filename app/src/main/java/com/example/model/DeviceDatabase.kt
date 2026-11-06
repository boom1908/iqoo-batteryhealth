package com.example.model

import android.content.Context
import android.os.Build
import android.util.Log

data class DeviceProfile(
    val marketingName: String,
    val modelIdentifiers: List<String>,
    val designCapacityMah: Int,
    val maxChargingWattage: Int,
    val series: String
)

object DeviceDatabase {
    private const val TAG = "DeviceDatabase"
    private const val PREFS_NAME = "iqoo_battery_prefs"
    private const val KEY_OVERRIDE_CAPACITY = "manual_override_capacity_mah"

    val KNOWN_DEVICES = listOf(
        // iQOO Flagship Number Series
        DeviceProfile("iQOO 13", listOf("V2408A", "I2408", "V2408"), 6150, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 12 Pro", listOf("V2329A"), 5100, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 12", listOf("V2307A", "I2220", "V2307"), 5000, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 11S", listOf("V2304A"), 4700, 200, "iQOO Flagship"),
        DeviceProfile("iQOO 11", listOf("V2243A", "I2212", "V2243"), 5000, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 10 Pro", listOf("V2218A"), 4550, 200, "iQOO Flagship"),
        DeviceProfile("iQOO 10", listOf("V2217A"), 4700, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 9 Pro", listOf("V2172A", "I2022"), 4700, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 9T", listOf("I2201"), 4700, 120, "iQOO Flagship"),
        DeviceProfile("iQOO 9", listOf("V2171A", "I2021"), 4350, 120, "iQOO Flagship"),

        // iQOO Neo Series
        DeviceProfile("iQOO Neo 10 Pro", listOf("V2426A"), 6100, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 10", listOf("V2425A"), 6100, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 9 Pro", listOf("V2339A", "I2303"), 5160, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 9", listOf("V2338A"), 5160, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 8 Pro", listOf("V2302A"), 5000, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 8", listOf("V2301A"), 5000, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 7 Pro", listOf("I2217"), 5000, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 7", listOf("V2231A", "I2214"), 5000, 120, "iQOO Neo"),
        DeviceProfile("iQOO Neo 6", listOf("I2011", "V2154A"), 4700, 80, "iQOO Neo"),

        // iQOO Z Series
        DeviceProfile("iQOO Z9 Turbo+", listOf("V2417A"), 6400, 80, "iQOO Z Series"),
        DeviceProfile("iQOO Z9 Turbo", listOf("V2352A"), 6000, 80, "iQOO Z Series"),
        DeviceProfile("iQOO Z9 Pro", listOf("I2305"), 5500, 80, "iQOO Z Series"),
        DeviceProfile("iQOO Z9 5G", listOf("I2302", "V2343"), 5000, 44, "iQOO Z Series"),
        DeviceProfile("iQOO Z9x", listOf("I2219", "V2353A"), 6000, 44, "iQOO Z Series"),
        DeviceProfile("iQOO Z7 Pro 5G", listOf("I2301"), 4600, 66, "iQOO Z Series"),
        DeviceProfile("iQOO Z7 5G", listOf("I2207"), 4500, 44, "iQOO Z Series"),
        DeviceProfile("iQOO Z7s", listOf("I2223"), 4500, 44, "iQOO Z Series"),
        DeviceProfile("iQOO Z6 Pro", listOf("I2126"), 4700, 66, "iQOO Z Series"),
        DeviceProfile("iQOO Z6 5G", listOf("I2127"), 5000, 18, "iQOO Z Series"),
        DeviceProfile("iQOO Z6 Lite", listOf("I2208"), 5000, 18, "iQOO Z Series"),

        // Vivo X & V Series (Funtouch / OriginOS)
        DeviceProfile("Vivo X200 Pro", listOf("V2413A"), 6000, 90, "Vivo X Series"),
        DeviceProfile("Vivo X200", listOf("V2415A"), 5800, 90, "Vivo X Series"),
        DeviceProfile("Vivo X100 Ultra", listOf("V2366A"), 5500, 80, "Vivo X Series"),
        DeviceProfile("Vivo X100 Pro", listOf("V2324A", "V2334"), 5400, 100, "Vivo X Series"),
        DeviceProfile("Vivo X100", listOf("V2309A", "V2335"), 5000, 120, "Vivo X Series"),
        DeviceProfile("Vivo X90 Pro+", listOf("V2227A"), 4700, 80, "Vivo X Series"),
        DeviceProfile("Vivo X90 Pro", listOf("V2242A"), 4870, 120, "Vivo X Series"),
        DeviceProfile("Vivo X90", listOf("V2241A"), 4810, 120, "Vivo X Series"),
        DeviceProfile("Vivo V40 Pro", listOf("V2347"), 5500, 80, "Vivo V Series"),
        DeviceProfile("Vivo V40", listOf("V2348"), 5500, 80, "Vivo V Series"),
        DeviceProfile("Vivo V30 Pro", listOf("V2319"), 5000, 80, "Vivo V Series"),
        DeviceProfile("Vivo V30", listOf("V2318"), 5000, 80, "Vivo V Series"),
        DeviceProfile("Vivo T3 Pro", listOf("V2404"), 5500, 80, "Vivo T Series"),
        DeviceProfile("Vivo T3x", listOf("V2338"), 6000, 44, "Vivo T Series"),
        DeviceProfile("Vivo T3 5G", listOf("V2334"), 5000, 44, "Vivo T Series")
    )

    /**
     * Resolves the current device profile by matching Build.MODEL, Build.DEVICE, or Build.PRODUCT.
     */
    fun detectDeviceProfile(): DeviceProfile? {
        val model = Build.MODEL.trim()
        val device = Build.DEVICE.trim()
        val product = Build.PRODUCT.trim()

        return KNOWN_DEVICES.firstOrNull { profile ->
            profile.modelIdentifiers.any { id ->
                model.equals(id, ignoreCase = true) ||
                device.equals(id, ignoreCase = true) ||
                product.equals(id, ignoreCase = true) ||
                model.contains(id, ignoreCase = true)
            } || profile.marketingName.equals(model, ignoreCase = true)
        }
    }

    /**
     * Queries internal Android PowerProfile via reflection as a fallback.
     */
    fun queryInternalPowerProfileCapacity(context: Context): Double? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = method.invoke(powerProfile) as? Double
            if (capacity != null && capacity > 500.0) capacity else null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query PowerProfile reflection: ${e.message}")
            null
        }
    }

    /**
     * Retrieves effective factory design capacity, prioritizing:
     * 1. Manual user override
     * 2. Known device database
     * 3. Framework PowerProfile reflection
     * 4. Default 5000 mAh
     */
    fun getEffectiveDesignCapacity(context: Context): Pair<Int, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val override = prefs.getInt(KEY_OVERRIDE_CAPACITY, -1)
        if (override in 500..12000) {
            return Pair(override, true)
        }

        val matchedProfile = detectDeviceProfile()
        if (matchedProfile != null) {
            return Pair(matchedProfile.designCapacityMah, false)
        }

        val powerProfileCap = queryInternalPowerProfileCapacity(context)
        if (powerProfileCap != null) {
            return Pair(powerProfileCap.toInt(), false)
        }

        return Pair(5000, false)
    }

    fun saveManualOverrideCapacity(context: Context, capacityMah: Int?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (capacityMah == null || capacityMah <= 0) {
            prefs.edit().remove(KEY_OVERRIDE_CAPACITY).apply()
        } else {
            prefs.edit().putInt(KEY_OVERRIDE_CAPACITY, capacityMah).apply()
        }
    }
}
