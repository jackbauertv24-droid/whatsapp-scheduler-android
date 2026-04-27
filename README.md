# WhatsApp Scheduler - Android App

A native Android application for scheduling WhatsApp messages. The app uses a pairing code authentication method and runs a background service to send messages at their scheduled times.

## Features

- Pairing code authentication (no QR code needed)
- View recent chats from WhatsApp
- Schedule messages for future delivery
- Cancel scheduled messages
- Background service for reliable message delivery
- Notifications for sent/failed messages

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐│
│  │ AuthActivity  │    │ MainActivity  │    │ QueueFragment ││
│  │               │    │               │    │               ││
│  │ - Phone input │    │ - ChatList    │    │ - View queue  ││
│  │ - Pairing code│    │ - ScheduleForm│    │ - Cancel msgs ││
│  └───────────────┘    └───────────────┘    └───────────────┘│
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   WebView Bridge                         │ │
│  │                                                         │ │
│  │  ┌─────────────────────────────────────────────────┐   │ │
│  │  │              Hidden WebView                       │   │ │
│  │  │                                                   │   │ │
│  │  │  [Baileys WhatsApp Client - JavaScript]          │   │ │
│  │  │                                                   │   │ │
│  │  │  - requestPairingCode()                          │   │ │
│  │  │  - sendMessage()                                 │   │ │
│  │  │  - getChats()                                    │   │ │
│  │  │                                                   │   │ │
│  │  └─────────────────────────────────────────────────┘   │ │
│  │                                                         │ │
│  │  JavaScript ↔ Kotlin Bridge                            │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              WhatsAppService (Foreground)               │ │
│  │                                                         │ │
│  │  - Keeps WhatsApp connection alive                     │ │
│  │  - Polls database for due messages (30s interval)      │ │
│  │  - Sends messages at scheduled time                    │ │
│  │  - Shows notifications                                 │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   Room Database                         │ │
│  │                                                         │ │
│  │  scheduled_messages:                                   │ │
│  │  - id, chat_jid, chat_name, content                    │ │
│  │  - scheduled_for, status, sent_at                     │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

- Android Studio (latest version)
- Node.js 18+ (for building JavaScript bundle)
- JDK 17
- Android SDK 26+ (Android 8.0+)

## Building the App

### Local Build

1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/whatsapp-scheduler-android.git
   cd whatsapp-scheduler-android
   ```

2. Build the JavaScript bundle:
   ```bash
   npm install
   npm run build
   ```

3. Open the project in Android Studio

4. Build the APK:
   - Debug: `./gradlew assembleDebug`
   - Release: `./gradlew assembleRelease`

### GitHub Actions Build

Push to the `main` or `master` branch to trigger the CI/CD workflow. The workflow will:
1. Build the JavaScript bundle
2. Build the Android APK (debug and release)
3. Upload APKs as artifacts
4. Create a release with the debug APK

Download APKs from:
- **Artifacts**: Go to Actions → Select workflow run → Download artifacts
- **Releases**: Go to Releases → Download the latest release

## Usage

### Authentication

1. Open the app
2. Enter your WhatsApp phone number (with country code)
3. Click "Continue"
4. A pairing code will be generated (e.g., `ABCD-1234`)
5. Enter the code you receive on your phone in WhatsApp, or enter the code WhatsApp sends you
6. Click "Verify"
7. Once connected, click "Start Using App"

### Scheduling a Message

1. Select a chat from the list
2. Type your message
3. Choose when to send:
   - **Specific time**: Pick a date and time
   - **Delay**: Set hours and minutes from now
4. Click "Schedule Message"

### Managing Scheduled Messages

1. Go to the Queue tab
2. View pending, sent, and failed messages
3. Cancel pending messages by clicking the "Cancel" button

## Permissions

The app requires the following permissions:
- `INTERNET` - Connect to WhatsApp servers
- `FOREGROUND_SERVICE` - Run background service for message sending
- `POST_NOTIFICATIONS` - Show status notifications
- `WAKE_LOCK` - Keep the service running

## Technical Details

### WebView Bridge

The app uses a hidden WebView to run the Baileys WhatsApp client. JavaScript calls are bridged to Kotlin via `@JavascriptInterface` annotated methods.

### Background Service

A foreground service (`WhatsAppService`) runs continuously to:
- Maintain WhatsApp connection
- Poll for due messages every 30 seconds
- Send messages using the WebView's JavaScript API
- Update message status in the database

### Data Storage

- **Scheduled messages**: SQLite via Room
- **WhatsApp auth**: EncryptedSharedPreferences

## Troubleshooting

### Connection Issues

If you see "Connection failed":
1. Check your internet connection
2. Make sure the phone number format is correct (e.g., `+1234567890`)
3. Try again - WhatsApp may have rate-limited your IP

### Messages Not Sending

If scheduled messages don't send:
1. Check that the foreground service is running (notification should be visible)
2. Make sure you're still connected to WhatsApp
3. Check the message status in the Queue tab

## Project Structure

```
whatsapp-scheduler-android/
├── .github/workflows/
│   └── build.yml              # CI/CD workflow
├── app/
│   ├── src/main/
│   │   ├── java/com/example/wascheduler/
│   │   │   ├── AuthActivity.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── bridge/
│   │   │   │   ├── JsBridge.kt
│   │   │   │   ├── WebViewManager.kt
│   │   │   │   └── WhatsAppClient.kt
│   │   │   ├── data/
│   │   │   │   ├── MessageDatabase.kt
│   │   │   │   ├── MessageDao.kt
│   │   │   │   └── ScheduledMessage.kt
│   │   │   ├── service/
│   │   │   │   └── WhatsAppService.kt
│   │   │   └── ui/
│   │   │       ├── ChatAdapter.kt
│   │   │       ├── ChatListFragment.kt
│   │   │       ├── MessageAdapter.kt
│   │   │       ├── QueueFragment.kt
│   │   │       └── ScheduleFragment.kt
│   │   ├── assets/whatsapp/
│   │   │   ├── index.html
│   │   │   └── baileys-bundle.js  # Generated by webpack
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   ├── drawable/
│   │   │   ├── menu/
│   │   │   └── navigation/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── js-src/
│   └── whatsapp-client.js     # Baileys wrapper
├── build.gradle
├── settings.gradle
├── webpack.config.js
├── package.json
└── README.md
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and build
5. Submit a pull request

## Notes

- This app uses the unofficial WhatsApp Web API via Baileys
- WhatsApp may block your number if you send too many messages
- Use responsibly and at your own risk
- This is for educational purposes only