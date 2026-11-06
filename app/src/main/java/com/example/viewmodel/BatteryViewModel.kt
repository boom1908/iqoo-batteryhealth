package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BatteryHealthData
import com.example.model.CoulombSession
import com.example.model.DeviceDatabase
import com.example.model.DeviceProfile
import com.example.service.BatteryReaderService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    DASHBOARD("Inspector"),
    HARDWARE_SPECS("Specs & Models"),
    ADB_SHIZUKU("Elevate Access"),
    DIAGNOSTICS("Raw Dump")
}

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val batteryReader = BatteryReaderService(application)

    val batteryData: StateFlow<BatteryHealthData> = batteryReader.batteryData
    val coulombSession: StateFlow<CoulombSession> = batteryReader.coulombSession

    private val _selectedTab = MutableStateFlow(MainTab.DASHBOARD)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _detectedProfile = MutableStateFlow<DeviceProfile?>(null)
    val detectedProfile: StateFlow<DeviceProfile?> = _detectedProfile.asStateFlow()

    private val _funtouchOsVersion = MutableStateFlow<String>("Funtouch OS / OriginOS")
    val funtouchOsVersion: StateFlow<String> = _funtouchOsVersion.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        _detectedProfile.value = DeviceDatabase.detectDeviceProfile()
        detectVivoOsVersion()

        // Auto-periodic refresh every 3 seconds for live wattage, voltage, and current
        viewModelScope.launch {
            while (isActive) {
                batteryReader.refreshAll()
                delay(3000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryReader.unregister()
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            batteryReader.refreshAll()
            _detectedProfile.value = DeviceDatabase.detectDeviceProfile()
            delay(500)
            _isRefreshing.value = false
        }
    }

    fun requestShizukuPermission() {
        batteryReader.requestShizukuPermission()
        viewModelScope.launch {
            delay(1000)
            refresh()
        }
    }

    fun setManualCapacityOverride(capacityMah: Int?) {
        DeviceDatabase.saveManualOverrideCapacity(getApplication(), capacityMah)
        refresh()
    }

    fun resetCoulombSession() {
        batteryReader.resetCoulombSession()
    }

    fun copyAdbCommandToClipboard() {
        val appId = getApplication<Application>().packageName
        val command = "adb shell pm grant $appId android.permission.BATTERY_STATS"
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Command", command)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "ADB command copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun copyAdbDumpCommandToClipboard() {
        val appId = getApplication<Application>().packageName
        val command = "adb shell pm grant $appId android.permission.DUMP"
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Dump Command", command)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "ADB DUMP command copied!", Toast.LENGTH_SHORT).show()
    }

    private fun detectVivoOsVersion() {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)

            val vivoVersion = getMethod.invoke(null, "ro.vivo.os.build.display.id", "") as String
            val originVersion = getMethod.invoke(null, "ro.vivo.os.version", "") as String
            val funtouchVersion = getMethod.invoke(null, "ro.funtouch.os.version", "") as String

            val version = listOf(vivoVersion, originVersion, funtouchVersion)
                .firstOrNull { it.isNotBlank() }

            if (!version.isNullOrBlank()) {
                _funtouchOsVersion.value = version
            } else {
                _funtouchOsVersion.value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            }
        } catch (e: Exception) {
            _funtouchOsVersion.value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        }
    }
}
