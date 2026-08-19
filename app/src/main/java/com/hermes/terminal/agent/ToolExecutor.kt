package com.hermes.terminal.agent

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.hermes.terminal.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ToolExecutor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getToolDefinitions(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                function = com.hermes.terminal.model.FunctionSpec(
                    name = "run_shell",
                    description = "Menjalankan perintah bash/shell langsung di lingkungan sistem Android.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("command") {
                                put("type", "string")
                                put("description", "Perintah terminal yang ingin dieksekusi (e.g. 'ls -la', 'ps -A', 'uname -a')")
                            }
                        }
                        put("required", buildJsonArray { add(JsonPrimitive("command")) })
                    }
                )
            ),
            ToolDefinition(
                function = com.hermes.terminal.model.FunctionSpec(
                    name = "read_file",
                    description = "Membaca isi file teks dari file system Android.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("path") {
                                put("type", "string")
                                put("description", "Path lengkap ke file yang ingin dibaca.")
                            }
                        }
                        put("required", buildJsonArray { add(JsonPrimitive("path")) })
                    }
                )
            ),
            ToolDefinition(
                function = com.hermes.terminal.model.FunctionSpec(
                    name = "write_file",
                    description = "Menulis atau membuat file baru di storage Android.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("path") {
                                put("type", "string")
                                put("description", "Path file tujuan.")
                            }
                            putJsonObject("content") {
                                put("type", "string")
                                put("description", "Isi teks yang akan ditulis ke file.")
                            }
                        }
                        put("required", buildJsonArray {
                            add(JsonPrimitive("path"))
                            add(JsonPrimitive("content"))
                        })
                    }
                )
            ),
            ToolDefinition(
                function = com.hermes.terminal.model.FunctionSpec(
                    name = "device_status",
                    description = "Mengambil status baterai, RAM, CPU, dan metadata hardware Android.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("detail") {
                                put("type", "boolean")
                                put("description", "Set true untuk informasi lengkap.")
                            }
                        }
                    }
                )
            ),
            ToolDefinition(
                function = com.hermes.terminal.model.FunctionSpec(
                    name = "http_request",
                    description = "Mengirim HTTP GET request ke URL eksternal untuk mengambil data atau informasi web.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("url") {
                                put("type", "string")
                                put("description", "URL target (HTTP/HTTPS)")
                            }
                        }
                        put("required", buildJsonArray { add(JsonPrimitive("url")) })
                    }
                )
            )
        )
    }

    suspend fun execute(toolName: String, rawArguments: String): String = withContext(Dispatchers.IO) {
        try {
            val argsObj = try {
                json.decodeFromString<JsonObject>(rawArguments)
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }

            when (toolName) {
                "run_shell" -> {
                    val command = argsObj["command"]?.jsonPrimitive?.content ?: rawArguments
                    executeShellCommand(command)
                }
                "read_file" -> {
                    val path = argsObj["path"]?.jsonPrimitive?.content ?: ""
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        file.readText()
                    } else {
                        "Error: File tidak ditemukan di '$path'"
                    }
                }
                "write_file" -> {
                    val path = argsObj["path"]?.jsonPrimitive?.content ?: ""
                    val content = argsObj["content"]?.jsonPrimitive?.content ?: ""
                    val file = File(path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                    "Success: File berhasil disimpan ke $path (${content.length} bytes)"
                }
                "device_status" -> {
                    getDeviceStatus()
                }
                "http_request" -> {
                    val targetUrl = argsObj["url"]?.jsonPrimitive?.content ?: ""
                    fetchUrl(targetUrl)
                }
                else -> "Error: Unknown tool '$toolName'"
            }
        } catch (e: Exception) {
            "Tool Execution Error ($toolName): ${e.localizedMessage}"
        }
    }

    private fun executeShellCommand(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            process.waitFor()
            val result = output.toString().trim()
            if (result.isEmpty()) "Perintah berhasil dieksekusi (tanpa output)." else result
        } catch (e: Exception) {
            "Shell Error: ${e.message}"
        }
    }

    private fun getDeviceStatus(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging ?: false

        val runtime = Runtime.getRuntime()
        val totalRamMb = runtime.totalMemory() / (1024 * 1024)
        val freeRamMb = runtime.freeMemory() / (1024 * 1024)
        val usedRamMb = totalRamMb - freeRamMb

        return """
            [ANDROID HARDWARE STATUS]
            • Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
            • Battery: $batteryPct% (Charging: $isCharging)
            • JVM Memory: ${usedRamMb}MB used / ${totalRamMb}MB total
            • CPU Cores: ${runtime.availableProcessors()}
        """.trimIndent()
    }

    private fun fetchUrl(targetUrl: String): String {
        return try {
            val url = URL(targetUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "HTTP Fetch Failed: ${e.message}"
        }
    }
}
