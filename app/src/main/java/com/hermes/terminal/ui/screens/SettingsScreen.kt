package com.hermes.terminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch

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
    onFetchModels: suspend (baseUrl: String, apiKey: String) -> Result<List<String>>,
    onSave: (apiKey: String, baseUrl: String, model: String, hubUrl: String, nodeId: String) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var baseUrl by remember { mutableStateOf(currentBaseUrl) }
    var modelInput by remember { mutableStateOf(currentModel) }
    var hubUrl by remember { mutableStateOf(currentHubUrl) }
    var nodeId by remember { mutableStateOf(currentNodeId) }

    var isFetching by remember { mutableStateOf(false) }
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }
    var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelSearchQuery by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
                    "UNIVERSAL AI PROVIDER",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Presets
            Text("⚡ Quick Presets:", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                fetchStatusMessage = null
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
                label = { Text("Provider Base URL") },
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
                label = { Text("API Key (Opsional jika Local Ollama)") },
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

            // 🚀 Tombol AUTO FETCH MODELS!
            Button(
                onClick = {
                    if (baseUrl.isBlank()) return@Button
                    isFetching = true
                    fetchStatusMessage = "Mengambil daftar model dari $baseUrl/models..."
                    scope.launch {
                        val result = onFetchModels(baseUrl, apiKey)
                        isFetching = false
                        result.fold(
                            onSuccess = { models ->
                                fetchedModels = models
                                fetchStatusMessage = "✓ Berhasil memuat ${models.size} model dari provider!"
                                showModelDialog = true
                            },
                            onFailure = { error ->
                                fetchStatusMessage = "❌ Gagal fetch: ${error.message}"
                            }
                        )
                    }
                },
                enabled = !isFetching && baseUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FETCHING MODELS...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Fetch", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔄 FETCH MODELS DARI PROVIDER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (fetchStatusMessage != null) {
                Text(
                    text = fetchStatusMessage!!,
                    color = if (fetchStatusMessage!!.startsWith("✓")) NeonGreen else AlertRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Model Name Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("Active Model Name") },
                    placeholder = { Text("e.g. deepseek/deepseek-r1") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextCode,
                        unfocusedTextColor = TextCode
                    )
                )

                if (fetchedModels.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showModelDialog = true },
                        modifier = Modifier
                            .background(TerminalSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Browse Models", tint = NeonPurple)
                    }
                }
            }

            // Quick Suggestions Chips
            val displaySuggestions = if (fetchedModels.isNotEmpty()) {
                fetchedModels.take(8)
            } else {
                listOf("deepseek/deepseek-r1", "anthropic/claude-3-7-sonnet", "google/gemini-2.0-flash", "openai/gpt-4o", "deepseek-reasoner")
            }

            Text("💡 Model Suggestions (Tap to apply):", color = TextMuted, fontSize = 11.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displaySuggestions.forEach { sugModel ->
                    val isCur = modelInput.trim() == sugModel
                    Button(
                        onClick = { modelInput = sugModel },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCur) NeonGreen.copy(alpha = 0.25f) else TerminalSurface
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

    // Modal Dialog Daftar Model yang Di-fetch
    if (showModelDialog && fetchedModels.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            containerColor = TerminalSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Model (${fetchedModels.size})", color = NeonCyan, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                    IconButton(onClick = { showModelDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = modelSearchQuery,
                        onValueChange = { modelSearchQuery = it },
                        placeholder = { Text("Filter model...", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    val filteredList = fetchedModels.filter { it.contains(modelSearchQuery, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredList) { itemModel ->
                            val isSelected = modelInput.trim() == itemModel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else TerminalBackground)
                                    .clickable {
                                        modelInput = itemModel
                                        showModelDialog = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    itemModel,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) NeonGreen else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Text("✓", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("TUTUP", color = NeonCyan)
                }
            }
        )
    }
}
