package com.hermes.terminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.terminal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    currentBaseUrl: String,
    currentModel: String,
    currentHubUrl: String,
    currentNodeId: String,
    onSave: (apiKey: String, baseUrl: String, model: String, hubUrl: String, nodeId: String) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var baseUrl by remember { mutableStateOf(currentBaseUrl) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var hubUrl by remember { mutableStateOf(currentHubUrl) }
    var nodeId by remember { mutableStateOf(currentNodeId) }

    val popularModels = listOf(
        "deepseek/deepseek-r1",
        "deepseek/deepseek-chat",
        "anthropic/claude-3-7-sonnet",
        "anthropic/claude-3-5-sonnet",
        "google/gemini-2.0-flash",
        "openai/gpt-4o"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ HERMES CONFIGURATION", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface,
                    titleContentColor = NeonCyan
                )
            )
        },
        containerColor = TerminalBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "🛰️ 9ROUTER AI PROVIDER",
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("9Router API Key") },
                placeholder = { Text("sk-9router-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("9Router Base URL") },
                placeholder = { Text("https://api.9router.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Text("Selected AI Model:", color = TextMuted, fontSize = 12.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                popularModels.forEach { modelName ->
                    val isSelected = selectedModel == modelName
                    Button(
                        onClick = { selectedModel = modelName },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonPurple.copy(alpha = 0.3f) else TerminalSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modelName,
                                color = if (isSelected) NeonPurple else TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            if (isSelected) {
                                Text("✓ ACTIVE", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = TerminalBorder)

            Text(
                "🌐 HERMES CONTROL ROOM FLEET",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = hubUrl,
                onValueChange = { hubUrl = it },
                label = { Text("Hub URL (Control Room IP / Tunnel)") },
                placeholder = { Text("http://192.168.1.100:3500") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = nodeId,
                onValueChange = { nodeId = it },
                label = { Text("Node Identifier") },
                placeholder = { Text("hermes-android-phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(apiKey, baseUrl, selectedModel, hubUrl, nodeId)
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
