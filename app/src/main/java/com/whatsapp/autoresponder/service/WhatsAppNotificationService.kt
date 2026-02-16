package com.whatsapp.autoresponder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.whatsapp.autoresponder.R
import com.whatsapp.autoresponder.data.MessageLog
import com.whatsapp.autoresponder.data.PreferencesManager
import com.whatsapp.autoresponder.utils.NetworkUtils
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class WhatsAppNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: PreferencesManager
    private lateinit var messageLog: MessageLog
    
    // Singleton OkHttpClient for better performance
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS) // Longer for Render cold starts
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    companion object {
        private const val TAG = "WA_AutoResponder"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val CHANNEL_ID = "whatsapp_listener_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        
        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        messageLog = MessageLog.getInstance(this)
        startForegroundService()
        Log.d(TAG, "Service created and started in foreground")
    }

    private fun startForegroundService() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WhatsApp Auto-Responder Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the auto-responder active"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp Auto-Responder Active")
            .setContentText("Monitoring ${prefs.getAllowedContacts().size} contact(s)")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Filter WhatsApp notifications only
        if (sbn.packageName != WHATSAPP_PACKAGE) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract message details
        val sender = extractSender(extras) ?: return
        val message = extractMessage(extras) ?: return

        // Skip if service is paused
        if (prefs.isServicePaused()) {
            Log.d(TAG, "Service paused, skipping message from $sender")
            return
        }

        // Enhanced group detection
        if (isGroupMessage(extras, sender)) {
            Log.d(TAG, "Skipping group message: $sender")
            messageLog.logMessage(sender, message, "skipped_group", null)
            return
        }

        // Check if contact is allowed
        if (!isContactAllowed(sender)) {
            Log.d(TAG, "Skipping unauthorized contact: $sender")
            messageLog.logMessage(sender, message, "skipped_unauthorized", null)
            return
        }

        Log.d(TAG, "Processing message from $sender: $message")
        messageLog.logMessage(sender, message, "processing", null)

        // Process asynchronously with retry logic
        scope.launch {
            try {
                val aiResponse = getAIResponseWithRetry(sender, message)
                aiResponse?.let { reply ->
                    val success = sendReply(notification, reply, sender)
                    messageLog.logMessage(
                        sender, 
                        message, 
                        if (success) "sent" else "failed", 
                        reply
                    )
                } ?: run {
                    messageLog.logMessage(sender, message, "no_response", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                messageLog.logMessage(sender, message, "error", e.message)
            }
        }
    }

    private fun extractSender(extras: Bundle): String? {
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun extractMessage(extras: Bundle): String? {
        // Try EXTRA_TEXT first
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { 
            if (it.isNotEmpty()) return it 
        }
        
        // Try EXTRA_BIG_TEXT for longer messages
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let {
            if (it.isNotEmpty()) return it
        }
        
        return null
    }

    private fun isGroupMessage(extras: Bundle, sender: String): Boolean {
        // Method 1: Check explicit group flag
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) {
            return true
        }
        
        // Method 2: Check for message array (indicates group)
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.let {
            if (it.isNotEmpty()) return true
        }
        
        // Method 3: Check for group name patterns
        if (sender.contains(":", ignoreCase = false)) return true
        if (sender.contains("@", ignoreCase = false)) return true
        if (sender.matches(Regex(".*\\(\\d+\\)"))) return true // e.g., "Group (5)"
        
        return false
    }

    private fun isContactAllowed(sender: String): Boolean {
        val allowedContacts = prefs.getAllowedContacts()
        if (allowedContacts.isEmpty()) return false
        
        // Normalize sender name
        val normalizedSender = normalizeName(sender)
        
        return allowedContacts.any { contact ->
            val normalizedContact = normalizeName(contact)
            normalizedSender.equals(normalizedContact, ignoreCase = true) ||
            normalizedSender.contains(normalizedContact, ignoreCase = true) ||
            normalizedContact.contains(normalizedSender, ignoreCase = true)
        }
    }

    private fun normalizeName(name: String): String {
        // Remove emojis, special characters, and extra whitespace
        return name
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "") // Keep letters, numbers, spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private suspend fun getAIResponseWithRetry(sender: String, message: String): String? {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = getAIResponse(sender, message)
                if (response != null) {
                    return response
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed", e)
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                }
            }
        }
        return null
    }

    private suspend fun getAIResponse(sender: String, message: String): String? =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isNetworkAvailable(this@WhatsAppNotificationService)) {
                Log.e(TAG, "No network connection")
                return@withContext null
            }

            // ✅ FIXED: Correct JSON structure matching backend expectations
            val json = JSONObject().apply {
                val queryObject = JSONObject().apply {
                    put("sender", sender)
                    put("message", message)
                    put("isGroup", false)
                }
                put("query", queryObject)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val webhookUrl = prefs.getWebhookUrl()
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .addHeader("User-Agent", "WhatsApp-AutoResponder/2.0")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Response code: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Server error: ${response.code} - ${response.message}")
                        return@withContext null
                    }

                    val responseBody = response.body?.string() ?: run {
                        Log.e(TAG, "Empty response body")
                        return@withContext null
                    }

                    Log.d(TAG, "Response: $responseBody")

                    val jsonResponse = JSONObject(responseBody)
                    
                    // Handle different response formats
                    when {
                        jsonResponse.has("replies") -> {
                            val replies = jsonResponse.getJSONArray("replies")
                            if (replies.length() > 0) {
                                replies.getJSONObject(0).getString("message")
                            } else null
                        }
                        jsonResponse.has("message") -> {
                            jsonResponse.getString("message")
                        }
                        else -> {
                            Log.e(TAG, "Unexpected response format")
                            null
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error: ${e.message}")
                null
            }
        }

    private fun sendReply(notification: Notification, replyText: String, sender: String): Boolean {
        // Find reply action
        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.resultKey != null } == true
        } ?: run {
            Log.e(TAG, "No reply action found in notification")
            return false
        }

        val remoteInput = replyAction.remoteInputs?.firstOrNull() ?: run {
            Log.e(TAG, "No remote input found")
            return false
        }

        val intent = Intent().apply {
            val results = Bundle().apply {
                putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), this, results)
        }

        return try {
            replyAction.actionIntent.send(this, 0, intent)
            Log.d(TAG, "✅ Reply sent to $sender: $replyText")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "Failed to send reply", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending reply", e)
            false
        }
    }

    override fun onDestroy() {
        scope.cancel()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
    }
}
