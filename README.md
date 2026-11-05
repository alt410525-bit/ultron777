# Ultron — AI Voice Assistant

Ultron tumhara AI voice assistant hai — Gemini se powered, jo tumhari awaaz se baat karta hai aur Maya jaisa hi pura experience deta hai. Naam: **Ultron**.

## Features (A–Z)

- 🎙️ **Voice chat** — mic se bolo, Ultron jawab deta hai (Gemini + TTS)
- 🧠 **Gemini AI** — apni API key se real conversational replies
- 🗣️ **Text-to-Speech** — Ultron bolta hai (offline system TTS)
- 🔴 **Pulsing orb** — live status (listening/thinking/speaking)
- 👋 **Wake word** — "Hey Ultron" (foreground service)
- ⚙️ **Full settings hub** — Assistant, Personal, Messages, Connectors, Skills, Advanced, Battery, Backup, Secret, Interface, Optional, Privacy
- 🔐 **Permissions screen** — mic, calls, SMS, contacts, camera, location, storage, overlay, battery
- 🔒 **Secret settings** — Partner mode (Friendly / Romantic / Intense 18+)
- 🧠 **Memory ("Yaadein")** — on-device memory store
- 🔋 **Battery saver** — toggles for wake/orb/guardian
- 🚗 **Driving mode** — call reject + SMS auto-reply
- 💬 **WhatsApp auto-reply** — (requires notification access + service)
- 🌐 **Multi-language** — Hindi / English / Hinglish

## Build

Yeh project [GitHub Actions](.github/workflows/build-apk.yml) se **automatically APK build** karta hai:

1. Repo GitHub par push karo (`main` branch)
2. **Actions** tab → **Build APK** workflow run karo
3. Run complete hone pe **Artifacts** se `ultron-debug-apk` download karo

Manual build ke liye (Android Studio ya CI mein):
```
./gradlew assembleDebug
```

## Personalise karne ke liye

Sab settings ek hi jagah ghoomti hain:
- **`app/src/main/res/values/strings.xml`** — app ka text/naam
- **`app/src/main/java/com/ultron/android/settings/SettingsRegistry.kt`** — poora settings tree (yahan nayi setting add karo)
- **`app/src/main/java/com/ultron/android/data/SettingsStore.kt`** — settings storage
- **`app/src/main/java/com/ultron/android/chat/GeminiClient.kt`** — Ultron ka personality/system prompt
- **`app/src/main/java/com/ultron/android/theme/Theme.kt`** — colours

## Security

Tumhari **Gemini API key encrypted storage** (Android Keystore) mein rehti hai — device pe hi, kisi server pe upload nahi hoti.

> ⚠️ **Important:** Apna GitHub Personal Access Token kabhi public kisi wali jagah mat share karo. Is conversation mein jo token aaya tha usse **revoke/rotate karo**.
# Ultron-280-
