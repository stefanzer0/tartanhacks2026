# RightGuard

**Your Rights. Your Safety. Your Control.**

An Android app that empowers citizens with real-time legal rights guidance, de-escalation techniques, encrypted recording, and panic lockdown during law enforcement encounters. Built for TartanHacks 2026.

## Features

- **Safeguard Mode** — One-tap encrypted video/audio recording with GPS tracking
- **AI Legal Guidance** — Real-time rights advice powered by Gemini, with voice interaction
- **Hands-Free Voice Control** — Speak naturally during encounters; AI responds through earbuds via TTS
- **Panic Mode** — Instant device lockdown disabling biometrics, requiring PIN to unlock
- **Trusted Contact Alerts** — Automatic SMS with GPS location to emergency contacts
- **Incident Reports** — Timeline of events, AI advice logs, and PDF export
- **Offline-First** — Recording, SMS alerts, and bundled legal knowledge work without internet
- **Encrypted Storage** — AES-256-GCM for recordings, SQLCipher for database, Android Keystore-backed

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Android | Kotlin, Jetpack Compose, SDK 36 (min 26) |
| DI | Hilt / Dagger |
| Database | Room + SQLCipher |
| Camera | CameraX VideoCapture |
| AI | Gemini 2.5 Flash via FastAPI backend |
| Voice | Android SpeechRecognizer + TextToSpeech |
| Encryption | Android Keystore, AES-256-GCM, EncryptedSharedPreferences |
| Backend | Python FastAPI |
| Widget | Jetpack Glance |

## Quick Start

### Backend
```bash
cd backend
pip install -r requirements.txt
cp .env.example .env  # Add your GEMINI_API_KEY
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Android
```bash
cd android
./gradlew assembleDebug
```
Install the APK on your device or emulator.

## Architecture

```
Clean Architecture: data -> domain -> ui

data/
  local/   -> Room + SQLCipher DB, Android Keystore crypto
  remote/  -> Retrofit API client
  repository/ -> Data access layer

domain/
  model/   -> Business entities
  usecase/ -> Business logic

ui/
  navigation/ -> Compose NavHost
  onboarding/ -> Setup wizard
  home/       -> Main dashboard
  safeguard/  -> Active encounter screen
  panic/      -> Lockdown screen
  incident/   -> History & reports
  settings/   -> Configuration
```

## Privacy

- **No telemetry** — Zero analytics, crash reporting, or tracking
- **Local-only storage** — All data stays on your device
- **Stateless backend** — Server processes requests without storing any user data
- **Self-hostable** — Configure your own backend URL
- **Encrypted everything** — Database and recordings are encrypted at rest

## License

Built for TartanHacks 2026.
