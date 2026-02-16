package com.whatsapp.autoresponder.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: Long,
    val sender: String,
    val message: String,
    val status: String, // "sent", "failed", "skipped_group", "skipped_unauthorized", "processing", "no_response", "error"
    val response: String?
)

class MessageLog private constructor(private val context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val PREFS_NAME = "MessageLogs"
        private const val KEY_LOGS = "message_logs"
        private const val MAX_LOGS = 100
        
        @Volatile
        private var instance: MessageLog? = null
        
        fun getInstance(context: Context): MessageLog {
            return instance ?: synchronized(this) {
                instance ?: MessageLog(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun logMessage(sender: String, message: String, status: String, response: String?) {
        val logs = getLogs().toMutableList()
        logs.add(0, LogEntry(System.currentTimeMillis(), sender, message, status, response))
        
        // Keep only recent logs
        if (logs.size > MAX_LOGS) {
            logs.subList(MAX_LOGS, logs.size).clear()
        }
        
        saveLogs(logs)
    }
    
    fun getLogs(): List<LogEntry> {
        val jsonString = prefs.getString(KEY_LOGS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val logs = mutableListOf<LogEntry>()
        
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                logs.add(
                    LogEntry(
                        timestamp = obj.getLong("timestamp"),
                        sender = obj.getString("sender"),
                        message = obj.getString("message"),
                        status = obj.getString("status"),
                        response = obj.optString("response", null)
                    )
                )
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }
        
        return logs
    }
    
    fun clearLogs() {
        prefs.edit().remove(KEY_LOGS).apply()
    }
    
    fun getStats(): Map<String, Int> {
        val logs = getLogs()
        return mapOf(
            "total" to logs.size,
            "sent" to logs.count { it.status == "sent" },
            "failed" to logs.count { it.status == "failed" },
            "skipped" to logs.count { it.status.startsWith("skipped") }
        )
    }
    
    private fun saveLogs(logs: List<LogEntry>) {
        val jsonArray = JSONArray()
        logs.forEach { log ->
            jsonArray.put(JSONObject().apply {
                put("timestamp", log.timestamp)
                put("sender", log.sender)
                put("message", log.message)
                put("status", log.status)
                put("response", log.response ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString(KEY_LOGS, jsonArray.toString()).apply()
    }
}
