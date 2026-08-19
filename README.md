# 📱 Hermes Terminal (Android Native Edition)

**Hermes Terminal** adalah aplikasi Android Native berbasis **Kotlin + Jetpack Compose** yang menggabungkan kemampuan **Terminal Emulator (mirip Termux)** dengan **Autonomous AI Agent Core (Hermes)** yang ditenagai oleh **9Router API**.

---

## 🌟 Fitur Utama

1. **🤖 Hermes Autonomous AI Engine**
   - Autonomous ReAct loop (berpikir, memilih tool, mengeksekusi di OS Android, menganalisis hasil, dan menyelesaikan target).
   - Tool Calling: `run_shell` (eksekusi perintah Linux/Android), `read_file`, `write_file`, `device_status`, `http_request`.

2. **⚡ 9Router Multi-Model Support**
   - Kompatibel dengan semua model AI via 9Router:
     - `deepseek/deepseek-r1` & `deepseek/deepseek-chat`
     - `anthropic/claude-3-7-sonnet` & `anthropic/claude-3-5-sonnet`
     - `google/gemini-2.0-flash`
     - `openai/gpt-4o`

3. **💻 Raw Interactive Shell**
   - Terminal PTY langsung ke sistem Android (`sh` / `toybox`).
   - Virtual Key Toolbar (ESC, TAB, CTRL-C, `|`, `/`, `-`, `~`, `clear`).

4. **🛰️ Hermes Control Room Fleet Mesh**
   - Foreground background service (`HermesWorkerService`) yang terus mengirim heartbeat, telemetri baterai, RAM, dan CPU ke Hermes Control Room.

---

## 🏗️ Struktur Project

```
hermes-android-native/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/hermes/terminal/
│   │   │   ├── agent/             # ReAct Engine & Tool Executor
│   │   │   ├── api/               # 9Router Client & Control Room Client
│   │   │   ├── model/             # Data Models & Payloads
│   │   │   ├── service/           # 24/7 Foreground Service
│   │   │   ├── terminal/          # Terminal Process & IO Stream
│   │   │   ├── ui/                # Jetpack Compose Screens & Theme
│   │   │   ├── HermesApp.kt
│   │   │   └── MainActivity.kt
│   │   └── res/                   # Themes, strings, XML config
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Cara Build Jadi APK

1. **Buka di Android Studio**:
   - Clone / copy folder `hermes-android-native` ke laptop / PC kamu.
   - Buka Android Studio -> **Open Project** -> pilih folder `hermes-android-native`.
   - Klik **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

2. **Build via CLI (Gradle)**:
   ```bash
   cd hermes-android-native
   ./gradlew assembleDebug
   ```
   File APK akan tergenerate di:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Install di HP Android**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Setup 9Router di Aplikasi

1. Buka aplikasi **Hermes Terminal** di HP Android.
2. Tap tombol **⚙️ (Settings)** di pojok kanan atas.
3. Masukkan **9Router API Key** dan **9Router Base URL** (default: `https://api.9router.com/v1`).
4. Pilih model AI yang ingin digunakan (misal: `deepseek/deepseek-r1` atau `anthropic/claude-3-7-sonnet`).
5. Masukkan URL **Hermes Control Room** kamu.
6. Tap **SAVE SETTINGS**. Hermes siap menjalankan tugas autonomous di Android!
