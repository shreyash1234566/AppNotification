package com.whatsapp.autoresponder.ui

import android.os.Bundle
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

        messageLog = MessageLog.getInstance(this)

        setupRecyclerView()
        setupButtons()
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val logs = messageLog.getLogs().reversed()
        adapter = MessageLogAdapter(logs)
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnClearLogs.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to delete all message logs?")
                .setPositiveButton("Clear") { _, _ ->
                    messageLog.clearLogs()
                    adapter.updateLogs(emptyList())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
