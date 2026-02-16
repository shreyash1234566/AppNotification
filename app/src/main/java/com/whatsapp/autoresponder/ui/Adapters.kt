package com.whatsapp.autoresponder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.whatsapp.autoresponder.databinding.ItemContactBinding

class ContactsAdapter(
    private var contacts: List<String>,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    class ContactViewHolder(val binding: ItemContactBinding) : 
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.contactName.text = contact
        holder.binding.btnRemove.setOnClickListener {
            onRemoveClick(contact)
        }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<String>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}

// MessageLogActivity.kt
package com.whatsapp.autoresponder.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.whatsapp.autoresponder.data.MessageLog
import com.whatsapp.autoresponder.databinding.ActivityMessageLogBinding

class MessageLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageLogBinding
    private lateinit var messageLog: MessageLog
    private lateinit var adapter: MessageLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Message Log"

        messageLog = MessageLog.getInstance(this)
        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = MessageLogAdapter(messageLog.getLogs())
        binding.recyclerViewLogs.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLogs.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnClearLogs.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Logs")
                .setMessage("Delete all message logs?")
                .setPositiveButton("Clear") { _, _ ->
                    messageLog.clearLogs()
                    adapter.updateLogs(emptyList())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// MessageLogAdapter.kt
package com.whatsapp.autoresponder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.whatsapp.autoresponder.R
import com.whatsapp.autoresponder.data.LogEntry
import com.whatsapp.autoresponder.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageLogAdapter(
    private var logs: List<LogEntry>
) : RecyclerView.Adapter<MessageLogAdapter.LogViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    class LogViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.binding.apply {
            tvSender.text = log.sender
            tvMessage.text = log.message
            tvTimestamp.text = dateFormat.format(Date(log.timestamp))
            tvStatus.text = log.status.replace("_", " ").uppercase()
            
            val statusColor = when (log.status) {
                "sent" -> R.color.status_success
                "failed", "error" -> R.color.status_error
                else -> R.color.status_warning
            }
            tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, statusColor))
            
            if (log.response != null) {
                tvResponse.text = "Response: ${log.response}"
                tvResponse.visibility = android.view.View.VISIBLE
            } else {
                tvResponse.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<LogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}

// SettingsActivity.kt
package com.whatsapp.autoresponder.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.whatsapp.autoresponder.data.PreferencesManager
import com.whatsapp.autoresponder.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        prefs = PreferencesManager.getInstance(this)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.etWebhookUrl.setText(prefs.getWebhookUrl())
    }

    private fun setupListeners() {
        binding.btnSaveSettings.setOnClickListener {
            val newUrl = binding.etWebhookUrl.text.toString().trim()
            if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) {
                prefs.setWebhookUrl(newUrl)
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// AutoResponderApplication.kt
package com.whatsapp.autoresponder

import android.app.Application
import android.util.Log

class AutoResponderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("AutoResponder", "Application started")
    }
}
