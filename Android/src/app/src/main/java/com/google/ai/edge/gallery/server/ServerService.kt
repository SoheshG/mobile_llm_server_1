package com.google.ai.edge.gallery.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ServerService : Service() {
    
    @Inject
    lateinit var apiServer: ApiServer
    
    @Inject
    lateinit var inferenceService: InferenceService
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        const val CHANNEL_ID = "AIServerChannel"
        const val ACTION_START = "START_SERVER"
        const val ACTION_STOP = "STOP_SERVER"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }
    
    private fun startServer() {
        val notification = createNotification("Server running on port 8080")
        startForeground(1, notification)
        
        scope.launch {
            // Load model first
            val modelLoadResult = inferenceService.loadModel(/* your default model */)
            
            if (modelLoadResult.isSuccess) {
                apiServer.start(8080)
                updateNotification("Server active at ${apiServer.getLocalIpAddress()}:8080")
            } else {
                updateNotification("Failed to load model")
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mobile AI Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        apiServer.stop()
        scope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}