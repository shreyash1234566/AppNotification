package com.whatsapp.autoresponder

import android.app.Application
import com.google.android.material.color.DynamicColors

class AutoResponderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic colors if available (Material 3)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
