package com.stepboost.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepboost.app.ui.theme.EmeraldGradientEnd
import com.stepboost.app.ui.theme.EmeraldGradientStart
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StepProgressGauge(
    currentSteps: Long,
    goalSteps: Long,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 16.dp
) {
    val progress = if (goalSteps > 0) {
        (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StepGaugeProgress"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val formattedCurrent = NumberFormat.getNumberInstance(Locale.getDefault()).format(currentSteps)
    val formattedGoal = NumberFormat.getNumberInstance(Locale.getDefault()).format(goalSteps)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()

            // Background Track
            drawArc(
                color = surfaceColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active Progress with Gradient
            val gradient = Brush.sweepGradient(
                colors = listOf(EmeraldGradientStart, EmeraldGradientEnd, EmeraldGradientStart)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    brush = gradient,
                    startAngle = 140f,
                    sweepAngle = 260f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TODAY'S STEPS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Text(
                text = formattedCurrent,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Goal: $formattedGoal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
