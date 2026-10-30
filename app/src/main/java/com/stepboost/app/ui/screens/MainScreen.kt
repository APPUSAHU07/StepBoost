package com.stepboost.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.StepsRecord
import com.stepboost.app.data.DailyStepSummary
import com.stepboost.app.data.HealthConnectStatus
import com.stepboost.app.ui.components.AutoBoosterCard
import com.stepboost.app.ui.components.CustomBoostSheet
import com.stepboost.app.ui.components.QuickBoostCard
import com.stepboost.app.ui.components.StepProgressGauge
import com.stepboost.app.ui.theme.DangerRed
import com.stepboost.app.ui.theme.GreenPrimary
import com.stepboost.app.ui.theme.WarningAmber
import com.stepboost.app.worker.StepBoosterWorker
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    healthStatus: HealthConnectStatus,
    hasPermissions: Boolean,
    stepSummary: DailyStepSummary,
    onRequestPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onQuickBoost: (Long) -> Unit,
    onRealisticBoost: (Long) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = remember { context.getSharedPreferences(StepBoosterWorker.PREFS_NAME, Context.MODE_PRIVATE) }
    var isAutoBoostEnabled by remember {
        mutableStateOf(prefs.getBoolean(StepBoosterWorker.KEY_AUTO_BOOST_ENABLED, false))
    }
    var dailyGoal by remember {
        mutableStateOf(prefs.getLong(StepBoosterWorker.KEY_DAILY_TARGET, 10000L))
    }

    var showCustomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "StepBoost",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Steps")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (hasPermissions) {
                FloatingActionButton(
                    onClick = { showCustomSheet = true },
                    containerColor = GreenPrimary,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Boost", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status or Permission Banner
            item {
                when {
                    healthStatus == HealthConnectStatus.UpdateRequired -> {
                        StatusBanner(
                            icon = Icons.Default.Warning,
                            iconColor = WarningAmber,
                            title = "Health Connect needs an update",
                            description = "Tap below to update or install Health Connect from Google Play Store.",
                            buttonText = "Open Play Store",
                            onAction = onInstallHealthConnect
                        )
                    }
                    healthStatus == HealthConnectStatus.NotSupported -> {
                        StatusBanner(
                            icon = Icons.Default.Warning,
                            iconColor = DangerRed,
                            title = "Health Connect Not Supported",
                            description = "Google Health Connect is not available on this device version (requires Android 9+).",
                            buttonText = null,
                            onAction = {}
                        )
                    }
                    !hasPermissions -> {
                        StatusBanner(
                            icon = Icons.Default.Warning,
                            iconColor = WarningAmber,
                            title = "Permissions Needed",
                            description = "StepBoost needs Health Connect read & write permissions to alter step count.",
                            buttonText = "Grant Permissions",
                            onAction = onRequestPermissions
                        )
                    }
                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected to Google Health Connect (No Root Required)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Step Progress Gauge
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StepProgressGauge(
                        currentSteps = stepSummary.totalSteps,
                        goalSteps = dailyGoal
                    )
                }
            }

            // Quick Booster Card
            if (hasPermissions) {
                item {
                    QuickBoostCard(
                        onQuickBoost = { steps ->
                            onQuickBoost(steps)
                            scope.launch {
                                snackbarHostState.showSnackbar("Added $steps steps to Health Connect!")
                            }
                        },
                        onTopUpToGoal = {
                            val remaining = (dailyGoal - stepSummary.totalSteps).coerceAtLeast(0L)
                            if (remaining > 0) {
                                onRealisticBoost(remaining)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Distributed $remaining steps across today!")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Today's goal of $dailyGoal steps is already reached!")
                                }
                            }
                        }
                    )
                }

                // Background Auto-Faker Card
                item {
                    AutoBoosterCard(
                        isEnabled = isAutoBoostEnabled,
                        onToggle = { enabled ->
                            isAutoBoostEnabled = enabled
                            prefs.edit().putBoolean(StepBoosterWorker.KEY_AUTO_BOOST_ENABLED, enabled).apply()
                            if (enabled) {
                                StepBoosterWorker.schedule(context)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Background step booster activated!")
                                }
                            } else {
                                StepBoosterWorker.cancel(context)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Background step booster paused.")
                                }
                            }
                        },
                        intervalHours = 1,
                        dailyGoal = dailyGoal
                    )
                }

                // Today's Injected Sessions Header
                item {
                    Text(
                        text = "Today's Step Sessions (${stepSummary.recordsCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // List of Sessions
                if (stepSummary.records.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No step records found for today yet. Use Quick Boost or the Custom Booster below.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(stepSummary.records) { record ->
                        StepRecordRow(
                            record = record,
                            onDelete = {
                                onDeleteRecord(record.metadata.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Record removed from Health Connect")
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showCustomSheet) {
        CustomBoostSheet(
            onDismiss = { showCustomSheet = false },
            onConfirm = { steps, distribute ->
                showCustomSheet = false
                if (distribute) {
                    onRealisticBoost(steps)
                } else {
                    onQuickBoost(steps)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Successfully inserted $steps steps!")
                }
            }
        )
    }
}

@Composable
fun StatusBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    buttonText: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}

@Composable
fun StepRecordRow(
    record: StepsRecord,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val zone = ZoneId.systemDefault()
    val startStr = record.startTime.atZone(zone).format(timeFormatter)
    val endStr = record.endTime.atZone(zone).format(timeFormatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "+${record.count} steps",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenPrimary
                )
                Text(
                    text = "$startStr – $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
