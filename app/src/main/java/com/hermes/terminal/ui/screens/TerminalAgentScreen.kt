package com.hermes.terminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.hermes.terminal.agent.HermesAgentEngine
import com.hermes.terminal.model.AgentLogEntry
import com.hermes.terminal.model.AgentMode
import com.hermes.terminal.terminal.TerminalSession
import com.hermes.terminal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalAgentScreen(
    agentEngine: HermesAgentEngine,
    terminalSession: TerminalSession,
    activeModel: String,
    nodeId: String,
    onOpenSettings: () -> Unit
) {
    val logs by agentEngine.logs.collectAsState()
    val isBusy by agentEngine.isBusy.collectAsState()
    val shellOutput by terminalSession.terminalOutput.collectAsState()

    var currentMode by remember { mutableStateOf(AgentMode.AI_AGENT) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(TerminalSurface)) {
                // Fleet Telemetry Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isBusy) NeonYellow else NeonGreen)
                        )
                        Text(
                            text = nodeId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "[$activeModel]",
                            fontFamily = FontFamily.Monospace,
                            color = NeonPurple,
                            fontSize = 10.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Mode Tabs
                TabRow(
                    selectedTabIndex = currentMode.ordinal,
                    containerColor = TerminalSurface,
                    contentColor = NeonGreen,
                    divider = { Divider(color = TerminalBorder) }
                ) {
                    Tab(
                        selected = currentMode == AgentMode.AI_AGENT,
                        onClick = { currentMode = AgentMode.AI_AGENT },
                        text = {
                            Text(
                                "🤖 HERMES AI",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMode == AgentMode.AI_AGENT) NeonGreen else TextMuted
                            )
                        }
                    )
                    Tab(
                        selected = currentMode == AgentMode.DIRECT_TERMINAL,
                        onClick = { currentMode = AgentMode.DIRECT_TERMINAL },
                        text = {
                            Text(
                                "💻 RAW SHELL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMode == AgentMode.DIRECT_TERMINAL) NeonCyan else TextMuted
                            )
                        }
                    )
                }
            }
        },
        containerColor = TerminalBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentMode == AgentMode.AI_AGENT) {
                    AgentLogListView(logs = logs, isBusy = isBusy, listState = listState)
                } else {
                    DirectTerminalView(output = shellOutput)
                }
            }

            // Quick Virtual Key Toolbar
            VirtualKeyToolbar(
                currentMode = currentMode,
                onKeyClick = { key ->
                    if (currentMode == AgentMode.DIRECT_TERMINAL) {
                        terminalSession.sendKey(key)
                    } else {
                        inputText += key
                    }
                },
                onQuickAction = { prompt ->
                    inputText = prompt
                    scope.launch {
                        agentEngine.executeUserGoal(prompt, activeModel)
                    }
                }
            )

            // Input Bar
            Surface(
                color = TerminalSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentMode == AgentMode.AI_AGENT) "hermes> " else "$ ",
                        color = if (currentMode == AgentMode.AI_AGENT) NeonGreen else NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (currentMode == AgentMode.AI_AGENT) "Perintahkan Hermes (e.g. 'Analisis storage HP')" else "Ketik perintah shell...",
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotEmpty()) {
                                inputText = ""
                                if (currentMode == AgentMode.AI_AGENT) {
                                    scope.launch {
                                        agentEngine.executeUserGoal(textToSend, activeModel)
                                    }
                                } else {
                                    terminalSession.sendCommand(textToSend)
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isBusy
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isBusy) NeonGreen else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentLogListView(
    logs: List<AgentLogEntry>,
    isBusy: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            AgentLogCard(log)
        }

        if (isBusy) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hermes is thinking & orchestrating...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonYellow
                    )
                }
            }
        }
    }
}

@Composable
fun AgentLogCard(log: AgentLogEntry) {
    var expanded by remember { mutableStateOf(false) }

    when (log.sender) {
        "USER" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurface)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("👤 USER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.message, color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        "HERMES" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurface)
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("🛰️ HERMES AGENT", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.message, color = TextCode, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        "TOOL" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F141C))
                    .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { expanded = !expanded }
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚡ [TOOL EXEC]: ${log.toolName ?: "Command"}",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (expanded) "▲ HIDE" else "▼ DETAILS",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (expanded || log.isExecuting) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!log.toolArgs.isNullOrBlank()) {
                            Text(
                                "Args: ${log.toolArgs}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            log.message,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        else -> {
            Text(
                "[SYSTEM]: ${log.message}",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun DirectTerminalView(output: String) {
    val scrollState = rememberScrollState()

    LaunchedEffect(output.length) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(Color.Black)
            .border(1.dp, TerminalBorder)
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = output,
            color = TextCode,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun VirtualKeyToolbar(
    currentMode: AgentMode,
    onKeyClick: (String) -> Unit,
    onQuickAction: (String) -> Unit
) {
    val keys = listOf("ESC", "TAB", "CTRL-C", "|", "/", "-", "~", "clear")
    val quickActions = listOf(
        "⚡ Status Hardware",
        "📂 List Files Storage",
        "🌐 Check Network IP"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (currentMode == AgentMode.DIRECT_TERMINAL) {
            keys.forEach { key ->
                Button(
                    onClick = {
                        if (key == "CTRL-C") onKeyClick("\u0003")
                        else if (key == "TAB") onKeyClick("\t")
                        else if (key == "ESC") onKeyClick("\u001b")
                        else onKeyClick(key)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBackground),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(key, fontSize = 10.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            quickActions.forEach { action ->
                Button(
                    onClick = {
                        val prompt = when (action) {
                            "⚡ Status Hardware" -> "Cek spesifikasi hardware, baterai, dan sisa RAM perangkat ini."
                            "📂 List Files Storage" -> "Jalankan shell untuk melihat file di direktori saat ini."
                            "🌐 Check Network IP" -> "Cek koneksi internet dan IP address perangkat ini."
                            else -> action
                        }
                        onQuickAction(prompt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBackground),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(action, fontSize = 10.sp, color = NeonGreen, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
