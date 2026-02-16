package com.whatsapp.autoresponder.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "AutoResponderPrefs"
        private const val KEY_ALLOWED_CONTACTS = "allowed_contacts"
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_SERVICE_PAUSED = "service_paused"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        
        private const val DEFAULT_WEBHOOK = "https://personalbot-kwev.onrender.com/webhook"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun getAllowedContacts(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED_CONTACTS, emptySet()) ?: emptySet()
    }
    
    fun setAllowedContacts(contacts: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWED_CONTACTS, contacts).apply()
    }
    
    fun addContact(contact: String): Boolean {
        val contacts = getAllowedContacts().toMutableSet()
        val added = contacts.add(contact)
        if (added) setAllowedContacts(contacts)
        return added
    }
    
    fun removeContact(contact: String): Boolean {
        val contacts = getAllowedContacts().toMutableSet()
        val removed = contacts.remove(contact)
        if (removed) setAllowedContacts(contacts)
        return removed
    }
    
    fun getWebhookUrl(): String {
        return prefs.getString(KEY_WEBHOOK_URL, DEFAULT_WEBHOOK) ?: DEFAULT_WEBHOOK
    }
    
    fun setWebhookUrl(url: String) {
        prefs.edit().putString(KEY_WEBHOOK_URL, url).apply()
    }
    
    fun isServicePaused(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_PAUSED, false)
    }
    
    fun setServicePaused(paused: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_PAUSED, paused).apply()
    }
    
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
}
