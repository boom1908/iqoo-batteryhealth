package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BatteryHealthData
import com.example.model.CoulombSession
import com.example.ui.components.CircularHealthMeter
import com.example.ui.components.CoulombCounterCard
import com.example.ui.components.PermissionStatusBanner
import com.example.ui.components.TelemetryGrid
import com.example.ui.theme.IqooCyan

@Composable
fun DashboardScreen(
    batteryData: BatteryHealthData,
    coulombSession: CoulombSession,
    funtouchVersion: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToElevate: () -> Unit,
    onResetCoulomb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "iQOO Battery Health",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = funtouchVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = IqooCyan
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = IqooCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Telemetry",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Circular Health Meter
        CircularHealthMeter(
            healthPercentage = batteryData.healthPercentage,
            diagnosticStatus = batteryData.diagnosticStatus,
            detectionMethod = batteryData.detectionMethod
        )

        // Permission Banner
        PermissionStatusBanner(
            batteryData = batteryData,
            onNavigateToElevate = onNavigateToElevate
        )

        // Real-time Telemetry Grid
        TelemetryGrid(
            batteryData = batteryData
        )

        // Active Coulomb Counter Card
        CoulombCounterCard(
            session = coulombSession,
            designCapacityMah = batteryData.designCapacityMah,
            onReset = onResetCoulomb
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
