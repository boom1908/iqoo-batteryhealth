package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryHealthData
import com.example.model.ChargingState
import com.example.model.CoulombSession
import com.example.ui.theme.IqooCyan
import com.example.ui.theme.IqooGreen
import com.example.ui.theme.IqooOrange
import com.example.ui.theme.IqooRed
import com.example.ui.theme.IqooYellow
import java.util.Locale

@Composable
fun PermissionStatusBanner(
    batteryData: BatteryHealthData,
    onNavigateToElevate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isElevated = batteryData.hasBatteryStatsPermission || batteryData.isShizukuPermissionGranted

    val cardBg = if (isElevated) Color(0xFF0F291E) else Color(0xFF261D0F)
    val borderColor = if (isElevated) IqooGreen else IqooYellow
    val icon = if (isElevated) Icons.Default.CheckCircle else Icons.Default.Warning
    val iconTint = if (isElevated) IqooGreen else IqooYellow

    val title = if (isElevated) {
        if (batteryData.isShizukuPermissionGranted) "Shizuku Privileged Shell Active"
        else "ADB BATTERY_STATS Granted"
    } else {
        "Standard Mode (Elevate Access Available)"
    }

    val subtitle = if (isElevated) {
        "Accessing hardware mSavedBatteryAsoc & cycle count"
    } else {
        "Tap to unlock 100% accurate Vivo/iQOO hardware health via ADB or Shizuku"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onNavigateToElevate() }
            .testTag("permission_status_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Status",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isElevated) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = borderColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "ELEVATE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = borderColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryGrid(
    batteryData: BatteryHealthData,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Row 1: Estimated Capacity & Cycle Count
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val formattedCap = String.format(Locale.US, "%.0f", batteryData.currentCapacityMah)
            val capValue = "$formattedCap / ${batteryData.designCapacityMah} mAh"
            val capSub = if (batteryData.isNominalOverridden) "Manual override" else "Factory rated"

            MetricCard(
                title = "CAPACITY",
                value = capValue,
                subtext = capSub,
                icon = Icons.Default.BatteryChargingFull,
                iconColor = IqooCyan,
                modifier = Modifier.weight(1f)
            )

            val cyclesText = batteryData.cycleCount?.let { "$it cycles" } ?: "Requires ADB/Shizuku"
            val cyclesSub = if (batteryData.usageRaw != null) "mSavedBatteryUsage" else "Charge cycles"

            MetricCard(
                title = "CYCLE COUNT",
                value = cyclesText,
                subtext = cyclesSub,
                icon = Icons.Default.Autorenew,
                iconColor = IqooYellow,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Real-time Wattage & Voltage
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val isCharging = batteryData.chargingState == ChargingState.CHARGING
            val currentPrefix = if (isCharging) "+" else if (batteryData.currentNowMa < 0) "" else "-"
            val wattageStr = String.format(Locale.US, "$currentPrefix%.2f W", batteryData.wattageW)
            val currentMaStr = String.format(Locale.US, "%.0f mA", batteryData.currentNowMa)

            MetricCard(
                title = "LIVE POWER",
                value = wattageStr,
                subtext = currentMaStr,
                icon = Icons.Default.Bolt,
                iconColor = if (isCharging) IqooGreen else IqooOrange,
                modifier = Modifier.weight(1f)
            )

            val voltageStr = String.format(Locale.US, "%.3f V", batteryData.voltageMv / 1000.0)
            MetricCard(
                title = "VOLTAGE",
                value = voltageStr,
                subtext = "${batteryData.voltageMv} mV",
                icon = Icons.Default.ElectricMeter,
                iconColor = IqooCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Temperature & Charging State
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val tempStr = String.format(Locale.US, "%.1f °C", batteryData.temperatureCelsius)
            val tempColor = when {
                batteryData.temperatureCelsius > 42.0 -> IqooRed
                batteryData.temperatureCelsius > 36.0 -> IqooOrange
                else -> IqooGreen
            }

            MetricCard(
                title = "TEMPERATURE",
                value = tempStr,
                subtext = if (batteryData.temperatureCelsius > 40.0) "Warm" else "Normal range",
                icon = Icons.Default.DeviceThermostat,
                iconColor = tempColor,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "STATUS",
                value = batteryData.chargingState.label,
                subtext = batteryData.pluggedSource,
                icon = Icons.Default.Speed,
                iconColor = IqooCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CoulombCounterCard(
    session: CoulombSession,
    designCapacityMah: Int,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (session.isTracking) IqooGreen else IqooOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Passive Coulomb Integration Session",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("reset_coulomb_button")
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Integrates live current during charging without needing ADB/Shizuku. Plug in and charge from low to high to evaluate capacity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Milliampere-hours Passed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val mahStr = String.format(Locale.US, "%.1f mAh", session.totalMilliampereHoursPassed)
                    Text(
                        text = mahStr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IqooCyan
                    )
                }

                Column {
                    Text(
                        text = "Session Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val mins = session.sessionDurationSeconds / 60
                    val secs = session.sessionDurationSeconds % 60
                    Text(
                        text = "${mins}m ${secs}s",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "Samples",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${session.samplesCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
