package com.whatsapp.autoresponder.ui

import android.os.Bundle
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

        prefs = PreferencesManager.getInstance(this)

        binding.etWebhookUrl.setText(prefs.getWebhookUrl())

        binding.btnSaveSettings.setOnClickListener {
            val url = binding.etWebhookUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                prefs.setWebhookUrl(url)
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
