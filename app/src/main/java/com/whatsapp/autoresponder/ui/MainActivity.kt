package com.whatsapp.autoresponder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Toast.makeText(this, "Contacts permission granted", Toast.LENGTH_SHORT).show()
        updateUI()
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

        setupClickListeners()
        updateUI()
        checkBatteryOptimization()
    }

    private fun setupClickListeners() {
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
    }

    private fun requestContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == 
                PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Contacts permission already granted", Toast.LENGTH_SHORT).show()
            }
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
                binding.btnBatteryOptimization.visibility = View.VISIBLE
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

    private fun updateUI() {
        val isServiceEnabled = isNotificationServiceEnabled()
        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == 
            PackageManager.PERMISSION_GRANTED
        val isPaused = prefs.isServicePaused()
        val contactCount = prefs.getAllowedContacts().size
        val stats = messageLog.getStats()

        // Status text
        binding.statusText.text = when {
            isPaused -> "Paused"
            isServiceEnabled && hasContacts && contactCount > 0 -> "Active"
            isServiceEnabled && contactCount == 0 -> "Setup Contacts"
            isServiceEnabled -> "Needs Permission"
            else -> "Inactive"
        }

        // Status color
        val colorRes = when {
            isPaused -> R.color.status_warning
            isServiceEnabled && hasContacts && contactCount > 0 -> R.color.status_success
            else -> R.color.status_error
        }
        binding.statusText.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.serviceStatusCard.strokeColor = ContextCompat.getColor(this, colorRes)

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
