package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.service.BatteryReaderService
import com.example.ui.DashboardScreen
import com.example.ui.components.ElevateAccessScreen
import com.example.ui.components.ModelDatabaseScreen
import com.example.ui.components.RawDumpScreen
import com.example.ui.theme.IqooBatteryTheme
import com.example.ui.theme.IqooCyan
import com.example.viewmodel.BatteryViewModel
import com.example.viewmodel.MainTab
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels()

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == BatteryReaderService.SHIZUKU_REQUEST_CODE) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // Shizuku not present
        }

        setContent {
            IqooBatteryTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // Ignore
        }
    }
}

@Composable
fun MainAppScreen(viewModel: BatteryViewModel) {
    val currentTab by viewModel.selectedTab.collectAsState()
    val batteryData by viewModel.batteryData.collectAsState()
    val coulombSession by viewModel.coulombSession.collectAsState()
    val detectedProfile by viewModel.detectedProfile.collectAsState()
    val funtouchVersion by viewModel.funtouchOsVersion.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // BackHandler returns to the Dashboard if on another tab
    if (currentTab != MainTab.DASHBOARD) {
        BackHandler {
            viewModel.selectTab(MainTab.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { viewModel.selectTab(MainTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = "Inspector") },
                    label = { Text("Inspector") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = IqooCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_inspector")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.HARDWARE_SPECS,
                    onClick = { viewModel.selectTab(MainTab.HARDWARE_SPECS) },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Specs & Models") },
                    label = { Text("Models") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = IqooCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_models")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ADB_SHIZUKU,
                    onClick = { viewModel.selectTab(MainTab.ADB_SHIZUKU) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Elevate Access") },
                    label = { Text("Elevate") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = IqooCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_elevate")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.DIAGNOSTICS,
                    onClick = { viewModel.selectTab(MainTab.DIAGNOSTICS) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Raw Dump") },
                    label = { Text("Raw Dump") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = IqooCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_raw_dump")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        batteryData = batteryData,
                        coulombSession = coulombSession,
                        funtouchVersion = funtouchVersion,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onNavigateToElevate = { viewModel.selectTab(MainTab.ADB_SHIZUKU) },
                        onResetCoulomb = { viewModel.resetCoulombSession() }
                    )
                }
                MainTab.HARDWARE_SPECS -> {
                    ModelDatabaseScreen(
                        batteryData = batteryData,
                        detectedProfile = detectedProfile,
                        funtouchVersion = funtouchVersion,
                        onSaveOverride = { viewModel.setManualCapacityOverride(it) }
                    )
                }
                MainTab.ADB_SHIZUKU -> {
                    ElevateAccessScreen(
                        batteryData = batteryData,
                        onCopyAdbStats = { viewModel.copyAdbCommandToClipboard() },
                        onCopyAdbDump = { viewModel.copyAdbDumpCommandToClipboard() },
                        onRequestShizuku = { viewModel.requestShizukuPermission() },
                        onRefresh = { viewModel.refresh() }
                    )
                }
                MainTab.DIAGNOSTICS -> {
                    RawDumpScreen(
                        batteryData = batteryData,
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}
