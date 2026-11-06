package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryHealthData
import com.example.ui.theme.IqooCyan
import com.example.ui.theme.IqooGreen
import com.example.ui.theme.IqooYellow

@Composable
fun RawDumpScreen(
    batteryData: BatteryHealthData,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val rawContent = batteryData.rawDumpsysText ?: """
Current Battery Service state (Funtouch OS / OriginOS sample):
  AC powered: true
  USB powered: false
  Wireless powered: false
  Max charging current: 6000000
  Max charging voltage: 11000000
  Charge counter: ${String.format("%.0f", batteryData.currentCapacityMah * 1000)}
  status: 2
  health: 2
  present: true
  level: ${batteryData.batteryLevel}
  scale: 100
  voltage: ${batteryData.voltageMv}
  temperature: ${String.format("%.0f", batteryData.temperatureCelsius * 10)}
  technology: Li-poly
  mSavedBatteryAsoc: ${batteryData.healthPercentage ?: 96}
  mSavedBatteryUsage: ${batteryData.cycleCount ?: 182}
  mBatteryHealthState: 1
  mChargeFull: ${batteryData.designCapacityMah * 1000}
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Parsed Vendor Registers Summary
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Parsed Vivo / iQOO Kernel Registers",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                RegisterRow(
                    name = "mSavedBatteryAsoc",
                    parsedValue = batteryData.asocRaw?.let { "$it %" } ?: "Evaluating",
                    description = "Proprietary State of Capacity (ASOC) percentage tracked by Vivo HAL.",
                    highlightColor = IqooGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                RegisterRow(
                    name = "mSavedBatteryUsage",
                    parsedValue = batteryData.usageRaw?.let { "$it cycles" } ?: "Evaluating",
                    description = "Hardware battery charge cycles recorded in kernel power driver.",
                    highlightColor = IqooYellow
                )

                if (batteryData.kernelChargeFull != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RegisterRow(
                        name = "kernel charge_full",
                        parsedValue = "${batteryData.kernelChargeFull / 1000} mAh",
                        description = "Direct sysfs /sys/class/power_supply/battery/charge_full reading.",
                        highlightColor = IqooCyan
                    )
                }
            }
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Dumpsys Output", rawContent)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Dumpsys copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IqooCyan),
                modifier = Modifier
                    .weight(1f)
                    .testTag("copy_dumpsys_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Dumpsys", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_dumpsys_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-query")
            }
        }

        // Monospace Dumpsys Viewer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D14)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = IqooCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (batteryData.rawDumpsysText != null) "Live dumpsys battery output" else "Dumpsys Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .horizontalScroll(horizontalScroll)
                ) {
                    Text(
                        text = rawContent,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterRow(
    name: String,
    parsedValue: String,
    description: String,
    highlightColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = highlightColor
                )
                Text(
                    text = parsedValue,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = highlightColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
