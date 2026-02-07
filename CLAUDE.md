# RightGuard — CLAUDE.md

## Project Overview
RightGuard is an Android app that empowers citizens with real-time legal rights guidance, de-escalation techniques, encrypted recording, and panic lockdown during law enforcement encounters. Built for TartanHacks 2026.

## Tech Stack
- **Android**: Kotlin, Jetpack Compose, SDK 36 (min 26), Hilt DI, Room + SQLCipher, CameraX, Retrofit
- **Backend**: Python FastAPI, Gemini API (gemini-1.5-flash)
- **Encryption**: AES-256-GCM via Android Keystore, SQLCipher for DB

## Build & Run

### Android
```bash
cd android
./gradlew assembleDebug
```

### Backend
```bash
cd backend
pip install -r requirements.txt
cp .env.example .env  # Add your GEMINI_API_KEY
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Architecture
- Clean Architecture: data (local/remote) → domain (models/usecases) → ui (screens/viewmodels)
- Hilt for dependency injection
- Room + SQLCipher for encrypted local database
- CameraX for video recording
- Android SpeechRecognizer for voice input
- Retrofit for backend API calls

## Key Conventions
- All recordings encrypted with AES-256-GCM via Android Keystore
- DB passphrase stored in EncryptedSharedPreferences
- No analytics, telemetry, or server-side data storage
- Backend is stateless — no user data persisted
- Offline-first: bundled legal_knowledge_fallback.json for rights info
