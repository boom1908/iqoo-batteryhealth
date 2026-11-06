package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagnosticStatus
import com.example.model.HealthDetectionMethod
import com.example.ui.theme.IqooCyan
import com.example.ui.theme.IqooGreen
import com.example.ui.theme.IqooRed
import com.example.ui.theme.IqooYellow

@Composable
fun CircularHealthMeter(
    healthPercentage: Int?,
    diagnosticStatus: DiagnosticStatus,
    detectionMethod: HealthDetectionMethod,
    size: Dp = 240.dp,
    strokeWidth: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val targetPct = healthPercentage?.toFloat() ?: 0f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(targetPct) {
        animatedProgress.animateTo(
            targetValue = (targetPct / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    // Dynamic color coding: Green >85%, Yellow 80-84%, Red <80%
    val ringColor = when {
        healthPercentage == null -> IqooCyan
        healthPercentage >= 85 -> IqooGreen
        healthPercentage in 80..84 -> IqooYellow
        else -> IqooRed
    }

    val gradientBrush = Brush.sweepGradient(
        colors = listOf(
            ringColor.copy(alpha = 0.6f),
            ringColor,
            ringColor.copy(alpha = 0.9f)
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("circular_health_meter")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = this.size.minDimension - strokePx

            // Background inactive track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Dynamic progress arc
            val sweep = 270f * animatedProgress.value
            if (sweep > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "STATE OF HEALTH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (healthPercentage != null) {
                Text(
                    text = "$healthPercentage%",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = ringColor
                )
            } else {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Diagnostic Status Pill
            DiagnosticStatusPill(diagnosticStatus = diagnosticStatus)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = detectionMethod.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun DiagnosticStatusPill(diagnosticStatus: DiagnosticStatus) {
    val (bgColor, textColor, label) = when (diagnosticStatus) {
        DiagnosticStatus.OPTIMAL -> Triple(Color(0xFF003919), IqooGreen, "Optimal Condition")
        DiagnosticStatus.NORMAL -> Triple(Color(0xFF453500), IqooYellow, "Normal (Good)")
        DiagnosticStatus.SERVICE_RECOMMENDED -> Triple(Color(0xFF4C0A17), IqooRed, "Service Recommended")
        DiagnosticStatus.UNKNOWN -> Triple(Color(0xFF1E293B), Color(0xFF94A3B8), "Analyzing...")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("diagnostic_status_pill")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
