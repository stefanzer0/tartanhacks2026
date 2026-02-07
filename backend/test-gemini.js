import fetch from 'node-fetch';

const GEMINI_API_KEY = "AIzaSyAUzVqYm8yJJThykoM3U8gSGJvOpLAiMug";
const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

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
