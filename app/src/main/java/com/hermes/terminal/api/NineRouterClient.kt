package com.hermes.terminal.api

import com.hermes.terminal.model.ChatCompletionRequest
import com.hermes.terminal.model.ChatCompletionResponse
import com.hermes.terminal.model.ChatMessage
import com.hermes.terminal.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NineRouterClient(
    private var apiKey: String,
    private var baseUrl: String = "https://api.9router.com/v1"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun updateConfig(newApiKey: String, newBaseUrl: String) {
        this.apiKey = newApiKey
        this.baseUrl = newBaseUrl.trimEnd('/')
    }

    suspend fun createChatCompletion(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val reqPayload = ChatCompletionRequest(
                model = model,
                messages = messages,
                tools = tools,
                toolChoice = if (tools.isNullOrEmpty()) null else "auto",
                stream = false
            )

            val bodyStr = json.encodeToString(reqPayload)
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://hermes-agent.local")
                .addHeader("X-Title", "Hermes Android Native")
                .post(bodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("9Router HTTP ${response.code}: $responseBody")
                    )
                }

                val chatResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
                val choice = chatResponse.choices.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Empty choices in 9Router response"))

                val message = choice.message
                    ?: return@withContext Result.failure(Exception("Message is null in choice"))

                Result.success(message)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
