# 📱 AppNotification — WhatsApp Auto-Responder

An Android app that listens to incoming WhatsApp notifications and automatically replies using an AI backend. When a whitelisted contact messages you, this app intercepts the notification, forwards it to [PersonalBot](https://github.com/shreyash1234566/PersonalBot), receives an AI-generated reply, and sends it back — all without you touching your phone.

> **This is the Android frontend of a two-repo system.**
> The AI brain lives in 👉 [shreyash1234566/PersonalBot](https://github.com/shreyash1234566/PersonalBot)

---

## 🔗 How the Two Repos Work Together

```
┌─────────────────────────────────────────────────────────────────┐
│                        Full System Flow                         │
└─────────────────────────────────────────────────────────────────┘

  Someone messages you on WhatsApp
           │
           ▼
  ┌─────────────────────┐        POST /webhook         ┌──────────────────────┐
  │   AppNotification   │ ─────────────────────────►  │     PersonalBot      │
  │   (Android / Kotlin)│                              │   (Python / FastAPI) │
  │                     │   {"query": {                │                      │
  │  NotificationListener    "sender": "Alice",        │  RAG + ChromaDB      │
  │  intercepts reply    │   "message": "Hey!",        │  Groq / Gemini /     │
  │  action from WhatsApp│   "isGroup": false}}        │  Together AI         │
  │                     │                              │  MongoDB history     │
  │                     │ ◄─────────────────────────  │                      │
  │                     │   {"replies": [              └──────────────────────┘
  │   Fires reply action │    {"message": "Hey! ..."}
  │   back into WhatsApp │   ]}
  └─────────────────────┘
           │
           ▼
  WhatsApp sends the AI reply to the contact
```

| Repo | Role | Language | Deployed On |
|---|---|---|---|
| [AppNotification](https://github.com/shreyash1234566/AppNotification) | Android app — intercepts notifications, fires replies | Kotlin | Your phone |
| [PersonalBot](https://github.com/shreyash1234566/PersonalBot) | AI backend — RAG + LLM response generation | Python | Render.com |

---

## ✨ Features

- **Notification listener** — hooks into Android's `NotificationListenerService` to catch every WhatsApp message in real time
- **AI-powered auto-reply** — forwards messages to PersonalBot and sends the response back through WhatsApp's native reply action
- **Contact allowlist** — only contacts you explicitly add will receive auto-replies; everyone else is ignored
- **Pause / Resume** — one-tap switch to disable auto-responses without uninstalling
- **Message log** — timestamped history of every processed message with sent/failed status
- **Statistics dashboard** — total messages, successfully sent, and failed counts at a glance
- **Webhook tester** — verify the backend is reachable before going live
- **Custom webhook URL** — change the PersonalBot endpoint from the Settings screen without rebuilding
- **Retry logic** — exponential backoff with 3 attempts handles Render cold starts gracefully
- **Boot receiver** — service auto-restarts when the phone reboots
- **Material Design 3** — clean dark-theme UI built with modern Android components

---

## 📋 Prerequisites

- Android **8.0+** (API 26+)
- WhatsApp installed on the same device
- [PersonalBot](https://github.com/shreyash1234566/PersonalBot) backend running (local or deployed on Render)
- Android Studio **Hedgehog** or later
- Kotlin **1.9+**
- Gradle **8.0+**

---

## 🚀 Setup

### 1. Clone the repo

```bash
git clone https://github.com/shreyash1234566/AppNotification.git
cd AppNotification
```

### 2. Open in Android Studio

```
File → Open → Select the AppNotification folder
```

Let Gradle sync complete before making any changes.

### 3. Configure the Webhook URL

The app points to the PersonalBot backend by default. If you're running PersonalBot locally or on a custom domain, open `app/src/main/java/.../data/PreferencesManager.kt` and update the default, **or** just change it from the Settings screen at runtime:

```
Default: https://personalbot-kwev.onrender.com/webhook
```

### 4. Update dependencies (if needed)

In `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // View Binding
    implementation("androidx.activity:activity-ktx:1.8.2")
}
```

Also make sure View Binding is enabled:

```kotlin
android {
    buildFeatures {
        viewBinding = true
    }
}
```

### 5. Build and install

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 First-Time Setup on Device

After installing, complete these steps in order — the app will prompt you through each one:

1. **Enable Notification Access** — Settings → Notification Access → toggle on *WhatsApp Auto-Responder*
2. **Grant Contacts Permission** — required for matching sender names against your allowlist
3. **Disable Battery Optimization** — critical; without this Android will kill the service after a few minutes
4. **Add contacts** — tap the **+** button and select which contacts should receive auto-replies
5. **Test the connection** — tap *Test Webhook Connection* to confirm PersonalBot is reachable
6. **Start the service** — the dashboard will show *Active* when everything is running

---

## 📂 Project Structure

```
AppNotification/
├── app/
│   └── src/main/
│       ├── java/com/whatsapp/autoresponder/
│       │   ├── service/
│       │   │   ├── WhatsAppNotificationService.kt  # Core — intercepts notifications & fires replies
│       │   │   └── BootReceiver.kt                 # Restarts service on device reboot
│       │   ├── ui/
│       │   │   ├── MainActivity.kt                 # Dashboard — status, stats, contact list
│       │   │   ├── MessageLogActivity.kt           # Per-message history with timestamps
│       │   │   ├── SettingsActivity.kt             # Webhook URL and other config
│       │   │   └── Adapters.kt                     # RecyclerView adapters
│       │   ├── data/
│       │   │   ├── PreferencesManager.kt           # SharedPreferences wrapper
│       │   │   └── MessageLog.kt                   # In-memory + persisted message log
│       │   ├── utils/
│       │   │   └── Utils.kt                        # OkHttp client, network helpers
│       │   └── AutoResponderApplication.kt
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml
│       │   │   ├── activity_message_log.xml
│       │   │   ├── activity_settings.xml
│       │   │   ├── item_contact.xml
│       │   │   └── item_log.xml
│       │   └── values/
│       │       ├── colors.xml                      # Dark theme palette
│       │       ├── strings.xml
│       │       └── themes.xml
│       └── AndroidManifest.xml
├── QUICK_START.md
├── ISSUES_ANALYSIS.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🔄 API Contract with PersonalBot

### Request (AppNotification → PersonalBot)

```json
POST /webhook

{
  "query": {
    "sender": "Alice",
    "message": "Hey, are you free tonight?",
    "isGroup": false
  }
}
```

### Response (PersonalBot → AppNotification)

```json
{
  "replies": [
    { "message": "Hey! I'm a bit busy right now, can we talk later?" }
  ]
}
```

The app also accepts the flat format:

```json
{ "message": "Hey! I'm a bit busy right now..." }
```

---

## 🐛 Debugging

### Live logcat

```bash
adb logcat | grep -E "WA_AutoResponder|AutoResponder|BootReceiver"
```

### Common issues

| Problem | Likely Cause | Fix |
|---|---|---|
| Service stops after a few minutes | Battery optimization is on | Disable it in Settings |
| Messages not getting a reply | Contact not in the allowlist | Add them via the + button |
| `Network error` in logs | Backend cold-starting on Render | Wait 30s, the retry logic will handle it |
| Reply fires but WhatsApp shows nothing | Notification reply action unavailable | Ensure WhatsApp notifications are not snoozed or grouped |
| Service doesn't start on reboot | `BootReceiver` not triggered | Check that `RECEIVE_BOOT_COMPLETED` permission is granted |

---

## 🔐 Security & Privacy

- The contact allowlist is stored **locally** in SharedPreferences — never uploaded anywhere
- The only outbound network call is the POST to your configured webhook URL (your own PersonalBot instance)
- No third-party analytics or tracking SDKs
- Set `AUTORESPONDER_SHARED_SECRET` on the PersonalBot side and include it in headers to lock down your endpoint

---

## ⚡ Performance Notes

- Uses a **singleton OkHttpClient** for connection pooling — no new socket per message
- All network calls run on **Kotlin Coroutines** (non-blocking, no ANR risk)
- Message log is capped at **100 entries** to keep memory usage flat
- Runs as a **foreground service** with a persistent notification so Android won't kill it

---

## 🛠 Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI | Material Design 3, View Binding |
| Async | Kotlin Coroutines |
| HTTP | OkHttp 4 |
| Storage | SharedPreferences |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |

---

## ✅ Pre-Launch Checklist

Before going live, verify every item:

- [ ] Notification access granted in Android settings
- [ ] Battery optimization disabled for this app
- [ ] At least one contact added to the allowlist
- [ ] Webhook test passes (green status in the app)
- [ ] PersonalBot backend is running and reachable
- [ ] Sent a real WhatsApp test message and checked the message log
- [ ] Confirmed the reply appeared in the WhatsApp chat

---

## 📄 License

For educational purposes only. Use responsibly and in accordance with WhatsApp's Terms of Service.

---

**Version:** 2.0 Enhanced
**Minimum Android:** 8.0 (API 26)
**Target Android:** 14 (API 34)
**Backend:** [shreyash1234566/PersonalBot](https://github.com/shreyash1234566/PersonalBot)
