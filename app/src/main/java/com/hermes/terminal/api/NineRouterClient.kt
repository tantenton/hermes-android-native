package com.hermes.terminal.api

import com.hermes.terminal.model.ChatCompletionRequest
import com.hermes.terminal.model.ChatCompletionResponse
import com.hermes.terminal.model.ChatMessage
import com.hermes.terminal.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Universal OpenAI-Compatible Client for Hermes
 * Works seamlessly with 9Router, OpenRouter, DeepSeek, OpenAI, Groq, Ollama, LM Studio, etc.
 */
class NineRouterClient(
    private var apiKey: String,
    private var baseUrl: String = "https://api.9router.com/v1"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun updateConfig(newApiKey: String, newBaseUrl: String) {
        this.apiKey = newApiKey.trim()
        this.baseUrl = newBaseUrl.trim().trimEnd('/')
    }

    /**
     * Auto-Fetch available models dynamically from provider ($baseUrl/models)
     */
    suspend fun fetchAvailableModels(targetBaseUrl: String = baseUrl, targetApiKey: String = apiKey): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = targetBaseUrl.trim().trimEnd('/')
            val requestBuilder = Request.Builder()
                .url("$cleanUrl/models")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://hermes-agent.local")
                .addHeader("X-Title", "Hermes Android Native")

            if (targetApiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $targetApiKey")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: $body")
                    )
                }

                val modelIds = mutableListOf<String>()
                try {
                    val root = json.decodeFromString<JsonObject>(body)
                    
                    // 1. OpenAI / OpenRouter / 9Router format: {"data": [{"id": "model-id"}, ...]}
                    if (root.containsKey("data") && root["data"] is JsonArray) {
                        val dataArray = root["data"]!!.jsonArray
                        for (elem in dataArray) {
                            if (elem is JsonObject && elem.containsKey("id")) {
                                elem["id"]?.jsonPrimitive?.content?.let { modelIds.add(it) }
                            } else if (elem is JsonObject && elem.containsKey("name")) {
                                elem["name"]?.jsonPrimitive?.content?.let { modelIds.add(it) }
                            }
                        }
                    }
                    // 2. Ollama format: {"models": [{"name": "qwen2.5:7b"}, ...]}
                    else if (root.containsKey("models") && root["models"] is JsonArray) {
                        val modelsArray = root["models"]!!.jsonArray
                        for (elem in modelsArray) {
                            if (elem is JsonObject) {
                                val name = elem["name"]?.jsonPrimitive?.content 
                                    ?: elem["model"]?.jsonPrimitive?.content
                                    ?: elem["id"]?.jsonPrimitive?.content
                                name?.let { modelIds.add(it) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("JSON Parsing failed: ${e.message}"))
                }

                if (modelIds.isEmpty()) {
                    Result.failure(Exception("No models found in provider response"))
                } else {
                    Result.success(modelIds.distinct().sorted())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChatCompletion(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val reqPayload = ChatCompletionRequest(
                model = model.trim(),
                messages = messages,
                tools = tools,
                toolChoice = if (tools.isNullOrEmpty()) null else "auto",
                stream = false
            )

            val bodyStr = json.encodeToString(reqPayload)
            val requestBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://hermes-agent.local")
                .addHeader("X-Title", "Hermes Android Native")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val request = requestBuilder
                .post(bodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("AI Provider ($baseUrl) HTTP ${response.code}: $responseBody")
                    )
                }

                val chatResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
                val choice = chatResponse.choices.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Empty choices from provider: $responseBody"))

                val message = choice.message
                    ?: return@withContext Result.failure(Exception("Message payload is null"))

                Result.success(message)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
