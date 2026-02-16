package com.whatsapp.autoresponder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.whatsapp.autoresponder.R
import com.whatsapp.autoresponder.data.LogEntry
import com.whatsapp.autoresponder.databinding.ItemContactBinding
import com.whatsapp.autoresponder.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.*

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

class MessageLogAdapter(
    private var logs: List<LogEntry>
) : RecyclerView.Adapter<MessageLogAdapter.LogViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

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
                tvResponse.text = log.response
                responseContainer.visibility = View.VISIBLE
            } else {
                responseContainer.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<LogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
