package com.stepboost.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stepboost.app.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBoostSheet(
    onDismiss: () -> Unit,
    onConfirm: (steps: Long, distributeAcrossDay: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var stepSliderValue by remember { mutableFloatStateOf(6000f) }
    var stepTextValue by remember { mutableStateOf("6000") }
    var isRealisticDistribution by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Custom Step Generator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Generate and alter steps in Health Connect with customizable human walking patterns.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = stepTextValue,
                onValueChange = { input ->
                    stepTextValue = input
                    input.toLongOrNull()?.let {
                        stepSliderValue = it.toFloat().coerceIn(100f, 30000f)
                    }
                },
                label = { Text("Number of Steps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Slide to adjust: ${stepSliderValue.toInt()} steps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = stepSliderValue,
                onValueChange = { value ->
                    stepSliderValue = value
                    stepTextValue = value.toInt().toString()
                },
                valueRange = 500f..25000f,
                steps = 48
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Distribution Method:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isRealisticDistribution,
                    onClick = { isRealisticDistribution = true },
                    label = { Text("Natural Day Spacing (Recommended)") }
                )
                FilterChip(
                    selected = !isRealisticDistribution,
                    onClick = { isRealisticDistribution = false },
                    label = { Text("Single Recent Walk") }
                )
            }

            Text(
                text = if (isRealisticDistribution) {
                    "✨ Splits steps across 3–8 realistic walking sessions starting from 8:00 AM until now. Completely natural to fitness apps."
                } else {
                    "⏱️ Places all steps into a single continuous walk ending right now."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val count = stepTextValue.toLongOrNull() ?: stepSliderValue.toLong()
                    onConfirm(count, isRealisticDistribution)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                )
            ) {
                Text(
                    text = "Insert Steps into Health Connect",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.background
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
