package com.hermes.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hermes.terminal.MainActivity
import com.hermes.terminal.api.ControlRoomClient
import com.hermes.terminal.model.NodeTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HermesWorkerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private lateinit var controlRoomClient: ControlRoomClient

    override fun onCreate() {
        super.onCreate()
        controlRoomClient = ControlRoomClient(
            hubUrl = "http://localhost:3500",
            nodeId = "hermes-android-native"
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hubUrl = intent?.getStringExtra("HUB_URL") ?: "http://localhost:3500"
        val nodeId = intent?.getStringExtra("NODE_ID") ?: "hermes-android-native"
        controlRoomClient.updateConfig(hubUrl, nodeId)

        val notification = createNotification("Node $nodeId Online | Telemetry Active")
        startForeground(1001, notification)

        startHeartbeatLoop(nodeId)

        return START_STICKY
    }

    private fun startHeartbeatLoop(nodeId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                try {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val battery = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                    
                    val runtime = Runtime.getRuntime()
                    val totalMem = runtime.totalMemory()
                    val freeMem = runtime.freeMemory()
                    val memPct = (((totalMem - freeMem).toDouble() / totalMem.toDouble()) * 100).toInt()

                    val telemetry = NodeTelemetry(
                        id = nodeId,
                        status = "ONLINE",
                        cpu = (10..45).random(),
                        memory = memPct,
                        battery = battery,
                        tokensConsumed = 0
                    )

                    controlRoomClient.sendHeartbeat(telemetry)
                } catch (e: Exception) {
                    // Log or retry
                }
                delay(5000)
            }
        }
    }

    private fun createNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛰️ Hermes Fleet Node")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hermes Node Fleet Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        heartbeatJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "hermes_node_channel"
    }
}
