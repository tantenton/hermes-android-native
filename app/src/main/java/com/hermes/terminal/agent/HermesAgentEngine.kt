package com.hermes.terminal.agent

import com.hermes.terminal.api.NineRouterClient
import com.hermes.terminal.model.AgentLogEntry
import com.hermes.terminal.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class HermesAgentEngine(
    private val apiClient: NineRouterClient,
    private val toolExecutor: ToolExecutor
) {
    private val _logs = MutableStateFlow<List<AgentLogEntry>>(emptyList())
    val logs: StateFlow<List<AgentLogEntry>> = _logs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()
    private val maxIterations = 8

    init {
        val systemPrompt = """
            You are Hermes Mobile Agent, an autonomous AI operating directly inside an Android terminal environment.
            You are powered by 9Router and connected to the distributed Hermes Fleet mesh.
            
            Your Capabilities:
            1. You can execute shell commands on this Android device using `run_shell`.
            2. You can read, write, and inspect local files using `read_file` and `write_file`.
            3. You can inspect device telemetry (battery, RAM, CPU) using `device_status`.
            4. You can query remote web resources using `http_request`.
            
            Guidelines:
            - Think step-by-step before invoking tools.
            - When the user asks you to automate a task, analyze what shell command or file operations are required, invoke them, inspect the output, and continue until the goal is fully accomplished.
            - Provide clear, concise status updates in Indonesian or English matching the user's prompt.
        """.trimIndent()

        conversationHistory.add(ChatMessage(role = "system", content = systemPrompt))
        appendLog("SYSTEM", "🛰️ Hermes Android Native initialized. Ready for commands via 9Router.")
    }

    suspend fun executeUserGoal(userGoal: String, model: String) {
        if (_isBusy.value) return
        _isBusy.value = true

        appendLog("USER", userGoal)
        conversationHistory.add(ChatMessage(role = "user", content = userGoal))

        var iteration = 0
        var shouldContinue = true

        try {
            while (shouldContinue && iteration < maxIterations) {
                iteration++
                val toolDefs = toolExecutor.getToolDefinitions()

                val result = apiClient.createChatCompletion(
                    model = model,
                    messages = conversationHistory,
                    tools = toolDefs
                )

                result.fold(
                    onSuccess = { assistantMsg ->
                        conversationHistory.add(assistantMsg)

                        if (!assistantMsg.content.isNullOrBlank()) {
                            appendLog("HERMES", assistantMsg.content)
                        }

                        val toolCalls = assistantMsg.toolCalls
                        if (!toolCalls.isNullOrEmpty()) {
                            for (call in toolCalls) {
                                val funcName = call.function.name
                                val funcArgs = call.function.arguments

                                appendLog(
                                    sender = "TOOL",
                                    message = "Executing: $funcName",
                                    toolName = funcName,
                                    toolArgs = funcArgs,
                                    isExecuting = true
                                )

                                val toolResult = toolExecutor.execute(funcName, funcArgs)

                                appendLog(
                                    sender = "TOOL",
                                    message = toolResult,
                                    toolName = funcName,
                                    toolArgs = funcArgs,
                                    isExecuting = false
                                )

                                conversationHistory.add(
                                    ChatMessage(
                                        role = "tool",
                                        name = funcName,
                                        content = toolResult,
                                        toolCallId = call.id
                                    )
                                )
                            }
                        } else {
                            // Selesai jika tidak ada tool yang dipanggil lagi
                            shouldContinue = false
                        }
                    },
                    onFailure = { error ->
                        appendLog("SYSTEM", "❌ 9Router Error: ${error.message}")
                        shouldContinue = false
                    }
                )
            }

            if (iteration >= maxIterations) {
                appendLog("SYSTEM", "⚠️ Max autonomous iterations reached.")
            }
        } finally {
            _isBusy.value = false
        }
    }

    fun appendLog(
        sender: String,
        message: String,
        toolName: String? = null,
        toolArgs: String? = null,
        isExecuting: Boolean = false
    ) {
        val entry = AgentLogEntry(
            id = UUID.randomUUID().toString(),
            sender = sender,
            message = message,
            toolName = toolName,
            toolArgs = toolArgs,
            isExecuting = isExecuting
        )
        _logs.value = _logs.value + entry
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
