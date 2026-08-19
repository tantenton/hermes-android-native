package com.hermes.terminal.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalSession(private val scope: CoroutineScope) {

    private val _terminalOutput = MutableStateFlow<String>("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private var process: Process? = null
    private var writer: BufferedWriter? = null

    init {
        startShell()
    }

    private fun startShell() {
        try {
            val processBuilder = ProcessBuilder("sh")
                .redirectErrorStream(true)
            
            // Set basic terminal env
            val env = processBuilder.environment()
            env["TERM"] = "xterm-256color"
            env["PS1"] = "hermes@android:$ "

            val proc = processBuilder.start()
            process = proc

            writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))

            _terminalOutput.value += "🛰️ HERMES INTERACTIVE SHELL READY\nType commands or switch to AI Agent tab.\n\n$ "

            scope.launch(Dispatchers.IO) {
                val buffer = CharArray(1024)
                var read: Int
                while (reader.read(buffer).also { read = it } != -1) {
                    val text = String(buffer, 0, read)
                    _terminalOutput.value += text
                }
            }
        } catch (e: Exception) {
            _terminalOutput.value += "\nShell init failed: ${e.message}\n"
        }
    }

    fun sendCommand(cmd: String) {
        scope.launch(Dispatchers.IO) {
            try {
                writer?.write(cmd + "\n")
                writer?.flush()
            } catch (e: Exception) {
                _terminalOutput.value += "\nWrite error: ${e.message}\n"
            }
        }
    }

    fun sendKey(key: String) {
        scope.launch(Dispatchers.IO) {
            try {
                writer?.write(key)
                writer?.flush()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun clear() {
        _terminalOutput.value = "$ "
    }
}
