# AppNotification — WhatsApp Auto-Responder

An Android app that listens to WhatsApp notifications and automatically replies to selected contacts using an AI-powered webhook backend.

---

## How It Works

1. The app runs a foreground **NotificationListenerService** that intercepts incoming WhatsApp messages.
2. When a message arrives from an allowed contact, it sends the message to a configured webhook URL.
3. The backend returns an AI-generated reply.
4. The app uses WhatsApp's notification reply action to send the response automatically.

---

## Requirements

- Android 8.0+ (API 26) — targets API 34
- WhatsApp installed on the device
- Android Studio Hedgehog or later
- Kotlin 1.9+, Gradle 8.0+

---

## Setup

### 1. Clone and Open

```bash
git clone https://github.com/shreyash1234566/AppNotification.git
```

Open the project in Android Studio (`File → Open`).

### 2. Build

```bash
./gradlew assembleDebug
```

### 3. Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions Required

| Permission | Purpose |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read incoming WhatsApp notifications |
| `READ_CONTACTS` | Pick allowed contacts from your phonebook |
| `FOREGROUND_SERVICE` | Keep the service alive in the background |
| `INTERNET` | Send messages to the webhook |
| `RECEIVE_BOOT_COMPLETED` | Restart the service after a reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the system from killing the service |

---

## First-Time Configuration

1. **Launch the app.**
2. Tap **Enable Notification Access** and grant the permission.
3. Tap **Grant Contacts Permission** and allow access.
4. Tap **Disable Battery Optimization** (recommended).
5. Tap **+** to add contacts that should receive auto-replies.
6. Tap **Test Webhook Connection** to verify your backend is reachable.

---

## Webhook

### Default URL

```
https://personalbot-kwev.onrender.com/webhook
```

Change it anytime in **Settings → Webhook URL**.

### Request Format (sent by the app)

```json
{
  "query": {
    "sender": "Contact Name",
    "message": "Incoming message text",
    "isGroup": false
  }
}
```

### Expected Response Format

```json
{ "replies": [{ "message": "AI reply here" }] }
```

or

```json
{ "message": "AI reply here" }
```

---

## Project Structure

```
app/src/main/
├── java/com/whatsapp/autoresponder/
│   ├── service/
│   │   ├── WhatsAppNotificationService.kt  # Intercepts notifications & sends replies
│   │   └── BootReceiver.kt                 # Restarts service on device boot
│   ├── ui/
│   │   ├── MainActivity.kt                 # Dashboard
│   │   ├── MessageLogActivity.kt           # Message history
│   │   ├── SettingsActivity.kt             # Webhook & preferences
│   │   └── Adapters.kt                     # RecyclerView adapters
│   ├── data/
│   │   ├── PreferencesManager.kt           # Persists settings & contact list
│   │   └── MessageLog.kt                   # In-memory message log
│   ├── utils/
│   │   └── Utils.kt                        # Network helpers
│   └── AutoResponderApplication.kt
├── res/
│   ├── layout/                             # XML layouts
│   └── values/                             # Colors, strings, themes
└── AndroidManifest.xml
```

---

## Debugging

```bash
# Stream relevant logs
adb logcat | grep "WA_AutoResponder\|AutoResponder\|BootReceiver"
```

**Service keeps stopping?** Disable battery optimization and confirm notification access is still enabled.

**Messages not being replied to?** Check that the contact is in the allowed list, the webhook URL is correct, and the backend is online.

---

## Tech Stack

| Component | Library |
|---|---|
| Language | Kotlin 1.9 |
| Async | Kotlin Coroutines 1.7.3 |
| HTTP | OkHttp 4.12.0 |
| UI | Material Design 3 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## License

For educational purposes only. Respect [WhatsApp's Terms of Service](https://www.whatsapp.com/legal/terms-of-service).
