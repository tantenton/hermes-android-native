package com.hermes.terminal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.hermes.terminal.agent.HermesAgentEngine
import com.hermes.terminal.agent.ToolExecutor
import com.hermes.terminal.api.NineRouterClient
import com.hermes.terminal.service.HermesWorkerService
import com.hermes.terminal.terminal.TerminalSession
import com.hermes.terminal.ui.screens.SettingsScreen
import com.hermes.terminal.ui.screens.TerminalAgentScreen
import com.hermes.terminal.ui.theme.HermesTerminalTheme

class MainActivity : ComponentActivity() {

    private lateinit var apiClient: NineRouterClient
    private lateinit var toolExecutor: ToolExecutor
    private lateinit var agentEngine: HermesAgentEngine
    private lateinit var terminalSession: TerminalSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage access on launch
        requestStoragePermissions()

        val prefs = getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("9router_api_key", "") ?: ""
        val baseUrl = prefs.getString("9router_base_url", "https://api.9router.com/v1") ?: "https://api.9router.com/v1"
        val model = prefs.getString("hermes_model", "deepseek/deepseek-r1") ?: "deepseek/deepseek-r1"
        val hubUrl = prefs.getString("control_room_hub", "http://localhost:3500") ?: "http://localhost:3500"
        val nodeId = prefs.getString("hermes_node_id", "hermes-android-native") ?: "hermes-android-native"

        apiClient = NineRouterClient(apiKey = apiKey, baseUrl = baseUrl)
        toolExecutor = ToolExecutor(applicationContext)
        agentEngine = HermesAgentEngine(apiClient, toolExecutor)
        terminalSession = TerminalSession(lifecycleScope)

        // Start background worker service
        startFleetService(hubUrl, nodeId)

        setContent {
            HermesTerminalTheme {
                var currentScreen by remember { mutableStateOf("terminal") }
                var activeApiKey by remember { mutableStateOf(apiKey) }
                var activeBaseUrl by remember { mutableStateOf(baseUrl) }
                var activeModel by remember { mutableStateOf(model) }
                var activeHubUrl by remember { mutableStateOf(hubUrl) }
                var activeNodeId by remember { mutableStateOf(nodeId) }

                if (currentScreen == "terminal") {
                    TerminalAgentScreen(
                        agentEngine = agentEngine,
                        terminalSession = terminalSession,
                        activeModel = activeModel,
                        nodeId = activeNodeId,
                        onOpenSettings = { currentScreen = "settings" }
                    )
                } else {
                    SettingsScreen(
                        currentApiKey = activeApiKey,
                        currentBaseUrl = activeBaseUrl,
                        currentModel = activeModel,
                        currentHubUrl = activeHubUrl,
                        currentNodeId = activeNodeId,
                        onFetchModels = { targetUrl, targetKey ->
                            apiClient.fetchAvailableModels(targetUrl, targetKey)
                        },
                        onSave = { newKey, newUrl, newModel, newHub, newNode ->
                            activeApiKey = newKey
                            activeBaseUrl = newUrl
                            activeModel = newModel
                            activeHubUrl = newHub
                            activeNodeId = newNode

                            prefs.edit()
                                .putString("9router_api_key", newKey)
                                .putString("9router_base_url", newUrl)
                                .putString("hermes_model", newModel)
                                .putString("control_room_hub", newHub)
                                .putString("hermes_node_id", newNode)
                                .apply()

                            apiClient.updateConfig(newKey, newUrl)
                            startFleetService(newHub, newNode)
                        },
                        onBack = { currentScreen = "terminal" }
                    )
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
            }
        }
    }

    private fun startFleetService(hubUrl: String, nodeId: String) {
        val serviceIntent = Intent(this, HermesWorkerService::class.java).apply {
            putExtra("HUB_URL", hubUrl)
            putExtra("NODE_ID", nodeId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
