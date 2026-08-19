package com.hermes.terminal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = "auto",
    val temperature: Double = 0.2,
    val stream: Boolean = false
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionSpec
)

@Serializable
data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    val delta: ChatMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class NodeTelemetry(
    val id: String,
    val status: String = "ONLINE",
    val cpu: Int,
    val memory: Int,
    val battery: Int = 100,
    val tokensConsumed: Long = 0,
    val activeTask: String? = null
)

enum class AgentMode {
    AI_AGENT,
    DIRECT_TERMINAL,
    SPLIT_VIEW
}

data class AgentLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String, // USER, HERMES, SYSTEM, TOOL
    val message: String,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val isExecuting: Boolean = false
)
