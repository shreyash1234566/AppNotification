package com.whatsapp.autoresponder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.whatsapp.autoresponder.R
import com.whatsapp.autoresponder.data.MessageLog
import com.whatsapp.autoresponder.data.PreferencesManager
import com.whatsapp.autoresponder.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var messageLog: MessageLog
    private lateinit var adapter: ContactsAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { handleContactSelection(it) }
    }

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openContactPicker()
        else Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        messageLog = MessageLog.getInstance(this)

        setupRecyclerView()
        setupClickListeners()
        updateUI()
        checkBatteryOptimization()
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(prefs.getAllowedContacts().toList()) { contact ->
            confirmRemoveContact(contact)
        }
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabAddContact.setOnClickListener { 
            requestContactsPermission() 
        }

        binding.btnEnableNotifications.setOnClickListener { 
            requestNotificationAccess() 
        }

        binding.btnGrantContacts.setOnClickListener { 
            requestContactsPermission() 
        }

        binding.btnBatteryOptimization.setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        binding.btnViewLogs.setOnClickListener {
            startActivity(Intent(this, MessageLogActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.switchPauseService.setOnCheckedChangeListener { _, isChecked ->
            prefs.setServicePaused(isChecked)
            updateUI()
        }

        binding.btnTestWebhook.setOnClickListener {
            testWebhookConnection()
        }
    }

    private fun requestContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == 
                PackageManager.PERMISSION_GRANTED -> openContactPicker()
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Contacts Permission")
                    .setMessage("Access to contacts is needed to select who receives auto-responses.")
                    .setPositiveButton("Grant") { _, _ ->
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun openContactPicker() {
        try {
            contactPickerLauncher.launch(null)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open contacts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleContactSelection(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val contactName = it.getString(nameIndex)
                    addContact(contactName)
                }
            }
        }
    }

    private fun addContact(contactName: String) {
        if (prefs.addContact(contactName)) {
            adapter.updateContacts(prefs.getAllowedContacts().toList())
            Toast.makeText(this, "Added: $contactName", Toast.LENGTH_SHORT).show()
            updateUI()
        } else {
            Toast.makeText(this, "Contact already added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmRemoveContact(contact: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Contact")
            .setMessage("Remove $contact from auto-responder list?")
            .setPositiveButton("Remove") { _, _ ->
                if (prefs.removeContact(contact)) {
                    adapter.updateContacts(prefs.getAllowedContacts().toList())
                    Toast.makeText(this, "Removed: $contact", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        if (!isNotificationServiceEnabled()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Enable Notification Access")
                .setMessage("Grant notification access to monitor WhatsApp messages.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(this, "Notification access already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(PowerManager::class.java)
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                binding.btnBatteryOptimization.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Battery Optimization")
                .setMessage("Disable battery optimization to ensure the service runs continuously.")
                .setPositiveButton("Disable") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to open settings", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun testWebhookConnection() {
        binding.btnTestWebhook.isEnabled = false
        binding.btnTestWebhook.text = "Testing..."

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Simple connectivity test
                    try {
                        val url = java.net.URL(prefs.getWebhookUrl())
                        val connection = url.openConnection()
                        connection.connectTimeout = 5000
                        connection.connect()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (result) {
                    Toast.makeText(this@MainActivity, "✓ Webhook reachable", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "✗ Webhook unreachable", Toast.LENGTH_LONG).show()
                }
            } finally {
                binding.btnTestWebhook.isEnabled = true
                binding.btnTestWebhook.text = "Test Webhook"
            }
        }
    }

    private fun updateUI() {
        val isServiceEnabled = isNotificationServiceEnabled()
        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == 
            PackageManager.PERMISSION_GRANTED
        val isPaused = prefs.isServicePaused()
        val contactCount = prefs.getAllowedContacts().size
        val stats = messageLog.getStats()

        // Status text
        binding.statusText.text = when {
            isPaused -> "⏸ Service Paused"
            isServiceEnabled && hasContacts && contactCount > 0 -> 
                "✓ Active - Monitoring $contactCount contact(s)"
            isServiceEnabled && contactCount == 0 -> 
                "⚠ Active but no contacts added"
            isServiceEnabled -> 
                "⚠ Active but needs Contacts permission"
            else -> 
                "✗ Inactive - Enable notification access"
        }

        // Status card color
        val color = when {
            isPaused -> android.R.color.holo_orange_dark
            isServiceEnabled && hasContacts && contactCount > 0 -> android.R.color.holo_green_dark
            else -> android.R.color.holo_red_dark
        }
        binding.serviceStatusCard.strokeColor = ContextCompat.getColor(this, color)

        // Stats
        binding.tvTotalMessages.text = stats["total"].toString()
        binding.tvSentMessages.text = stats["sent"].toString()
        binding.tvFailedMessages.text = stats["failed"].toString()

        // Pause switch
        binding.switchPauseService.isChecked = isPaused
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
