import fetch from 'node-fetch';
import dotenv from 'dotenv';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

// Load environment variables from .env file in project root
const __dirname = dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: resolve(__dirname, '../.env') });

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-3-flash-preview';
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

if (!GEMINI_API_KEY) {
    throw new Error('GEMINI_API_KEY is not set in .env file');
}

async function testGemini() {
    console.log("🧪 Testing Gemini API directly...\n");

    const requestBody = {
        contents: [{
            parts: [{
                text: "You are a legal advisor. A user is being asked for their ID during a police encounter. Give brief tactical advice in under 15 words."
            }]
        }],
        generationConfig: {
            maxOutputTokens: 100,
            temperature: 0.4,
        }
    };

    try {
        const response = await fetch(GEMINI_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'x-goog-api-key': GEMINI_API_KEY
            },
            body: JSON.stringify(requestBody)
        });

        console.log("📥 Response status:", response.status);
        const data = await response.json();
        console.log("\n📄 Full response:");
        console.log(JSON.stringify(data, null, 2));

        if (data.candidates) {
            const advice = data.candidates[0]?.content?.parts?.[0]?.text;
            console.log("\n💡 Extracted advice:");
            console.log(advice);
        }
    } catch (error) {
        console.error("❌ Error:", error.message);
    }
}

testGemini();
