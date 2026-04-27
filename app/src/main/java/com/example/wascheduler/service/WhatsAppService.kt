package com.example.wascheduler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wascheduler.MainActivity
import com.example.wascheduler.R
import com.example.wascheduler.bridge.WhatsAppClient
import com.example.wascheduler.data.MessageDatabase
import com.example.wascheduler.data.ScheduledMessage
import kotlinx.coroutines.*

class WhatsAppService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private var whatsappClient: WhatsAppClient? = null
    private var messageDatabase: MessageDatabase? = null
    
    private val POLL_INTERVAL = 30_000L
    
    companion object {
        const val CHANNEL_ID = "whatsapp_scheduler_channel"
        const val NOTIFICATION_ID = 1
        
        const val ACTION_START = "com.example.wascheduler.action.START"
        const val ACTION_STOP = "com.example.wascheduler.action.STOP"
        const val ACTION_SEND_NOW = "com.example.wascheduler.action.SEND_NOW"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        
        fun start(context: Context) {
            val intent = Intent(context, WhatsAppService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, WhatsAppService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        fun sendNow(context: Context, messageId: Long) {
            val intent = Intent(context, WhatsAppService::class.java).apply {
                action = ACTION_SEND_NOW
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        whatsappClient = WhatsAppClient.getInstance(this)
        messageDatabase = MessageDatabase.getInstance(this)
        Log.d("WhatsAppService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startPolling()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SEND_NOW -> {
                val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1)
                if (messageId != -1) {
                    scope.launch {
                        sendMessageNow(messageId)
                    }
                }
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        Log.d("WhatsAppService", "Service destroyed")
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_content))
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_content)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startPolling() {
        handler.postDelayed(pollRunnable, POLL_INTERVAL)
    }
    
    private val pollRunnable = object : Runnable {
        override fun run() {
            scope.launch {
                processDueMessages()
            }
            handler.postDelayed(this, POLL_INTERVAL)
        }
    }
    
    private suspend fun processDueMessages() {
        val db = messageDatabase ?: return
        val client = whatsappClient ?: return
        
        if (!client.isConnected()) {
            Log.d("WhatsAppService", "Not connected to WhatsApp, skipping poll")
            return
        }
        
        val dueMessages = db.messageDao().getDueMessages(System.currentTimeMillis())
        Log.d("WhatsAppService", "Processing ${dueMessages.size} due messages")
        
        for (message in dueMessages) {
            sendMessage(message)
        }
    }
    
    private suspend fun sendMessage(message: ScheduledMessage) {
        val db = messageDatabase ?: return
        val client = whatsappClient ?: return
        
        Log.d("WhatsAppService", "Sending message ${message.id} to ${message.chatName}")
        
        try {
            client.sendMessage(message.chatJid, message.content) { success, messageId ->
                scope.launch {
                    if (success) {
                        db.messageDao().updateStatus(
                            message.id,
                            ScheduledMessage.STATUS_SENT,
                            System.currentTimeMillis(),
                            null
                        )
                        showSentNotification(message.chatName)
                        Log.d("WhatsAppService", "Message ${message.id} sent successfully")
                    } else {
                        db.messageDao().updateStatus(
                            message.id,
                            ScheduledMessage.STATUS_FAILED,
                            null,
                            "Failed to send"
                        )
                        showFailedNotification(message.chatName)
                        Log.e("WhatsAppService", "Message ${message.id} failed")
                    }
                }
            }
        } catch (e: Exception) {
            db.messageDao().updateStatus(
                message.id,
                ScheduledMessage.STATUS_FAILED,
                null,
                e.message
            )
            Log.e("WhatsAppService", "Error sending message: ${e.message}")
        }
    }
    
    private suspend fun sendMessageNow(messageId: Long) {
        val db = messageDatabase ?: return
        val message = db.messageDao().getById(messageId)
        
        if (message != null && message.status == ScheduledMessage.STATUS_PENDING) {
            sendMessage(message)
        }
    }
    
    private fun showSentNotification(chatName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_message_sent, chatName))
            .setSmallIcon(R.drawable.ic_check)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun showFailedNotification(chatName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_message_failed, chatName))
            .setSmallIcon(R.drawable.ic_queue)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}