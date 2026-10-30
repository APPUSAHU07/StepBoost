package com.stepboost.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.stepboost.app.data.DailyStepSummary
import com.stepboost.app.data.HealthConnectManager
import com.stepboost.app.data.HealthConnectStatus
import com.stepboost.app.ui.screens.MainScreen
import com.stepboost.app.ui.theme.StepBoostTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>

    private var healthStatus by mutableStateOf<HealthConnectStatus>(HealthConnectStatus.Available)
    private var hasPermissions by mutableStateOf(false)
    private var stepSummary by mutableStateOf(DailyStepSummary())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectManager = HealthConnectManager(this)

        // Health Connect Permissions Contract
        val requestPermissionContract = PermissionController.createRequestPermissionResultContract()
        requestPermissionLauncher = registerForActivityResult(requestPermissionContract) { granted ->
            lifecycleScope.launch {
                hasPermissions = healthConnectManager.hasAllPermissions()
                if (hasPermissions) {
                    refreshStepData()
                }
            }
        }

        checkHealthConnectState()

        setContent {
            StepBoostTheme {
                MainScreen(
                    healthStatus = healthStatus,
                    hasPermissions = hasPermissions,
                    stepSummary = stepSummary,
                    onRequestPermissions = { requestPermissions() },
                    onInstallHealthConnect = { openHealthConnectInPlayStore() },
                    onQuickBoost = { steps ->
                        lifecycleScope.launch {
                            healthConnectManager.addQuickWalk(steps)
                            refreshStepData()
                        }
                    },
                    onRealisticBoost = { steps ->
                        lifecycleScope.launch {
                            healthConnectManager.boostRealisticSteps(steps)
                            refreshStepData()
                        }
                    },
                    onDeleteRecord = { recordId ->
                        lifecycleScope.launch {
                            healthConnectManager.deleteRecords(listOf(recordId))
                            refreshStepData()
                        }
                    },
                    onRefresh = {
                        refreshStepData()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkHealthConnectState()
    }

    private fun checkHealthConnectState() {
        healthStatus = healthConnectManager.checkAvailability()
        lifecycleScope.launch {
            hasPermissions = healthConnectManager.hasAllPermissions()
            if (hasPermissions) {
                refreshStepData()
            }
        }
    }

    private fun refreshStepData() {
        lifecycleScope.launch {
            stepSummary = healthConnectManager.getTodayStepSummary()
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(healthConnectManager.permissions)
    }

    private fun openHealthConnectInPlayStore() {
        val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", packageName)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser URL
            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }
}
