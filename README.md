# Digital Witness

**A mobile safety application designed to protect individuals during police encounters through AI-powered legal guidance, automated recording, and evidence preservation.**

## 🎯 What is Digital Witness?

Digital Witness is a React Native mobile app that helps users navigate police interactions safely by:
- 🎥 **Recording** audio and video evidence automatically
- 🤖 **AI Legal Advisor** providing real-time guidance via voice
- ☁️ **Cloud Backup** of all interaction data
- 📊 **Incident Reports** generated from the session
- 🛡️ **Rights Reminders** displayed during encounters

---

## ⚡ Re-Launching the App (Already Set Up?)

If you've already installed everything, just run this command:

```bash
cd digital-witness
npm start
```

**Then:**
1. A QR code will appear in your terminal
2. Open **Expo Go** app on your phone
3. Scan the QR code
4. Digital Witness will load on your device
5. You should see **"SYSTEM: ONLINE"** ✅

**Alternative options:**
- Press `a` in terminal to open Android emulator
- Press `i` in terminal to open iOS simulator (macOS only)

---

## 🚀 First Time Setup

### What You Need

Before installing, make sure you have:
- **Node.js** (v16 or higher) - [Download](https://nodejs.org/)
- **npm** (comes with Node.js)
- **Expo Go** app on your phone - [iOS](https://apps.apple.com/app/expo-go/id982107779) | [Android](https://play.google.com/store/apps/details?id=host.exp.exponent)

### Installation Steps

**1. Clone the repository**
```bash
git clone <repository-url>
cd tartanhacks2026
```

**2. Install mobile app dependencies**
```bash
cd digital-witness
npm install
```

**3. Start the app**
```bash
npm start
```

**4. Open on your phone**
- Open the **Expo Go** app
- Scan the QR code from your terminal
- Wait for the app to load
- You should see **"SYSTEM: ONLINE"** ✅

**That's it!** The backend is already deployed and ready to use.

---

### Optional: Backend Development Setup

**Only needed if you want to modify the AI backend functions.**

1. **Install additional dependencies**
   ```bash
   cd ../backend
   npm install
   ```

2. **Set up environment variables** (for local testing only)
   ```bash
   cp ../.env.example ../.env
   # Edit .env and add your Gemini API key
   ```

3. **Install Supabase CLI** (for deployment)
   - Windows: `npm install -g supabase`
   - macOS/Linux: See [Supabase CLI docs](https://supabase.com/docs/guides/cli)

---

## 🏗️ Project Structure

```
tartanhacks2026/
├── digital-witness/          # React Native mobile app (Expo)
│   ├── App.js                # Main application code
│   ├── package.json          # Mobile dependencies
│   └── assets/               # Images and media
├── backend/                  # Backend services
│   ├── supabase/functions/   # Supabase Edge Functions
│   │   ├── ask-advisor/      # AI voice advisor endpoint
│   │   └── generate-report/  # Report generation endpoint
│   ├── config.js             # Environment configuration
│   ├── test-gemini.js        # Test script for Gemini API
│   └── package.json          # Backend dependencies
├── .env                      # Environment variables (DO NOT COMMIT)
├── .env.example              # Example environment file
└── README.md                 # This file
```

---

## 💡 How to Use the App

### First Time Setup
1. Launch the app
2. Fill in your **Safety Profile**:
   - Full Name
   - Legal Status (e.g., "Citizen", "F-1 Visa")
   - Emergency Contact Number
3. Tap **SAVE PROFILE**

### During a Police Encounter
1. Tap the large **ARM** button to activate protection mode
2. The app will:
   - ✅ Start recording video and audio
   - ✅ Upload data to cloud storage in real-time
   - ✅ Display your rights on screen
   - ✅ Enable AI legal advisor

3. **Ask the AI for Advice**:
   - Hold the **"HOLD TO ASK"** button
   - Speak your question (e.g., "They're asking for my ID")
   - Release the button
   - The AI will respond with legal guidance via voice

4. **Other Features**:
   - **PLAY MSG**: Automatically announces "I am recording this interaction for my safety"
   - **LAWYER**: Connects to legal counsel

5. **End Session**:
   - Tap **STOP**
   - Enter PIN: `1234`
   - View session summary and generate incident report

---

## 🔒 Security & Privacy

- ✅ All recordings are encrypted and uploaded to secure cloud storage
- ✅ API keys are never hardcoded (stored in environment variables)
- ✅ PIN protection prevents unauthorized session termination
- ✅ `.env` file is excluded from version control via `.gitignore`

**Important**: Never commit your `.env` file or share API keys publicly.

---

## 🧪 Testing

### Test the Gemini API Integration
```bash
cd backend
npm test
```

### Test Deployed Edge Functions
```bash
curl -i --location --request POST 'https://YOUR-PROJECT-REF.supabase.co/functions/v1/ask-advisor' \
  --header 'Authorization: Bearer YOUR_ANON_KEY' \
  --header 'Content-Type: multipart/form-data' \
  --form 'audio=@/path/to/test-audio.m4a'
```

---

## 📚 Additional Documentation

- [**DEPLOYMENT.md**](./DEPLOYMENT.md) - Detailed deployment and secret management guide
- [**ENV_SETUP.md**](./ENV_SETUP.md) - Environment variable configuration guide

---

## 🛠️ Tech Stack

### Mobile App
- **React Native** - Cross-platform mobile framework
- **Expo** - Development toolchain
- **Expo Camera** - Video recording
- **Expo Audio** - Voice recording
- **Expo Speech** - Text-to-speech for AI responses
- **AsyncStorage** - Local data persistence

### Backend
- **Supabase Edge Functions** - Serverless API endpoints (Deno runtime)
- **Google Gemini API** - AI language model for legal guidance
- **Node.js** - Testing and configuration

---

## 🐛 Troubleshooting

### "SYSTEM: OFFLINE" in Dashboard
- Verify your Supabase functions are deployed
- Check that `SUPABASE_URL` and `SUPABASE_KEY` are correct in `App.js`
- Ensure Edge Functions have the `GEMINI_API_KEY` secret set

### Microphone/Camera Not Working
- Grant permissions when prompted
- On iOS: Settings → Digital Witness → Enable Camera & Microphone
- On Android: Settings → Apps → Digital Witness → Permissions

### "Failed to get upload URL" Error
- Check that `GEMINI_API_KEY` is set in Supabase:
  ```bash
  supabase secrets list
  ```
- Redeploy functions if needed:
  ```bash
  supabase functions deploy
  ```

### TypeScript "Cannot find name 'Deno'" Errors
- These are **expected** in VS Code for Edge Functions
- They don't affect deployment or runtime
- Can be safely ignored

---

## 📝 License

This project was created for TartanHacks 2026.

---

## 👥 Contributing

Contributions are welcome! Please ensure:
1. No API keys or secrets are committed
2. `.env` remains in `.gitignore`
3. All new environment variables are documented in `.env.example`

---

## 📞 Support

For issues or questions, please open an issue in the repository.

---

**Stay Safe. Stay Informed. Stay Protected.**