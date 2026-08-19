package com.hermes.terminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.terminal.ui.theme.*

data class ProviderPreset(
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val suggestedModels: List<String>
)

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
    var modelInput by remember { mutableStateOf(currentModel) }
    var hubUrl by remember { mutableStateOf(currentHubUrl) }
    var nodeId by remember { mutableStateOf(currentNodeId) }

    val presets = listOf(
        ProviderPreset(
            name = "9Router",
            baseUrl = "https://api.9router.com/v1",
            defaultModel = "deepseek/deepseek-r1",
            suggestedModels = listOf("deepseek/deepseek-r1", "anthropic/claude-3-7-sonnet", "google/gemini-2.0-flash", "openai/gpt-4o")
        ),
        ProviderPreset(
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "deepseek/deepseek-r1",
            suggestedModels = listOf("deepseek/deepseek-r1", "anthropic/claude-3.7-sonnet", "meta-llama/llama-3.3-70b-instruct", "google/gemini-2.0-flash-001")
        ),
        ProviderPreset(
            name = "DeepSeek Official",
            baseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-reasoner",
            suggestedModels = listOf("deepseek-reasoner", "deepseek-chat")
        ),
        ProviderPreset(
            name = "OpenAI Official",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o",
            suggestedModels = listOf("gpt-4o", "gpt-4o-mini", "o1", "o3-mini")
        ),
        ProviderPreset(
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            suggestedModels = listOf("llama-3.3-70b-versatile", "deepseek-r1-distill-llama-70b", "mixtral-8x7b-32768")
        ),
        ProviderPreset(
            name = "Ollama (Local / Termux)",
            baseUrl = "http://localhost:11434/v1",
            defaultModel = "qwen2.5-coder:7b",
            suggestedModels = listOf("qwen2.5-coder:7b", "llama3.2:3b", "deepseek-r1:8b", "mistral")
        )
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
            // Header Provider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonGreen)
                Text(
                    "UNIVERSAL AI PROVIDER (OPENAI-COMPATIBLE)",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                "Hermes bisa pakai provider AI apa saja! Pilih preset di bawah untuk auto-fill atau ketik Base URL & Model custom kamu sendiri:",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Preset Chips Carousel
            Text("⚡ Quick Provider Presets (Tap to Fill):", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isMatched = baseUrl.trimEnd('/') == preset.baseUrl.trimEnd('/')
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMatched) NeonPurple.copy(alpha = 0.25f) else TerminalSurface)
                            .border(1.dp, if (isMatched) NeonPurple else TerminalBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                baseUrl = preset.baseUrl
                                modelInput = preset.defaultModel
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            preset.name,
                            color = if (isMatched) NeonPurple else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Custom Base URL Field
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("AI Provider Base URL") },
                placeholder = { Text("https://api.9router.com/v1 atau https://openrouter.ai/api/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // API Key Field
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key (Kosongkan jika Ollama/Local)") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Model Name Field (100% Free Text Input!)
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model Name (Ketik nama model apa saja)") },
                placeholder = { Text("e.g. deepseek/deepseek-r1, gpt-4o, claude-3-7-sonnet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextCode,
                    unfocusedTextColor = TextCode
                )
            )

            // Suggested Models Chips
            val currentPreset = presets.find { baseUrl.trimEnd('/') == it.baseUrl.trimEnd('/') }
            val suggestions = currentPreset?.suggestedModels ?: listOf(
                "deepseek/deepseek-r1",
                "anthropic/claude-3-7-sonnet",
                "google/gemini-2.0-flash",
                "openai/gpt-4o",
                "deepseek-reasoner"
            )

            Text("💡 Model Suggestions (Tap to select):", color = TextMuted, fontSize = 11.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { sugModel ->
                    val isCur = modelInput.trim() == sugModel
                    Button(
                        onClick = { modelInput = sugModel },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCur) NeonGreen.copy(alpha = 0.2f) else TerminalSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            sugModel,
                            fontSize = 11.sp,
                            color = if (isCur) NeonGreen else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = TerminalBorder)

            // Hermes Control Room Section
            Text(
                "🌐 HERMES CONTROL ROOM FLEET",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = hubUrl,
                onValueChange = { hubUrl = it },
                label = { Text("Hub URL (Control Room IP / Cloudflare Tunnel)") },
                placeholder = { Text("https://07660cd3b7f968.lhr.life") },
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
                placeholder = { Text("hermes-android-native") },
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
                    onSave(apiKey, baseUrl, modelInput, hubUrl, nodeId)
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE & APPLY CONFIGURATION", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
