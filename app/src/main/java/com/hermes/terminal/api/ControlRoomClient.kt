package com.hermes.terminal.api

import com.hermes.terminal.model.NodeTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ControlRoomClient(
    private var hubUrl: String,
    private var nodeId: String = "hermes-android-native"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun updateConfig(newHubUrl: String, newNodeId: String) {
        this.hubUrl = newHubUrl.trimEnd('/')
        this.nodeId = newNodeId
    }

    suspend fun sendHeartbeat(telemetry: NodeTelemetry): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(telemetry)
            val request = Request.Builder()
                .url("$hubUrl/api/nodes/heartbeat")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Hermes-Node-Id", nodeId)
                .post(bodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "OK")
                } else {
                    Result.failure(Exception("Hub returned ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
