import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const GEMINI_API_KEY = "AIzaSyAUzVqYm8yJJThykoM3U8gSGJvOpLAiMug";
const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

serve(async (req) => {
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        // 1. In a real app, we would fetch session logs from DB here using a session_id
        // For MVP, we will use provided context logs or generate a generic mock if empty
        const { session_id, logs } = await req.json();

        let context = "";
        if (logs && logs.length > 0) {
            context = JSON.stringify(logs);
        } else {
            context = "User interacted with police. User queried about ID rights. User remained silent.";
        }

        const SYSTEM_PROMPT = `
    You are an expert Legal Observer. 
    Analyze the following interaction logs between a citizen and police.
    Generate a formatted INCIDENT REPORT.
    
    Structure:
    1. **Incident Summary**: Brief 1-sentence overview.
    2. **Rights Invoked**: Bullet points of rights the user exercised (Silence, Lawyer, etc).
    3. **Risk Analysis**: Assessment of the situation (Green/Yellow/Red).
    4. **Recommendations**: Post-incident legal steps.

    Keep it professional, factual, and concise (under 200 words).
    `;

        // 2. Call Gemini
        const requestBody = {
            contents: [{
                role: "user",
                parts: [{ text: `${SYSTEM_PROMPT}\n\nLOGS:\n${context}` }]
            }]
        };

        const response = await fetch(GEMINI_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'x-goog-api-key': GEMINI_API_KEY
            },
            body: JSON.stringify(requestBody)
        });


        const data = await response.json();
        console.log("📄 Report response:", JSON.stringify(data, null, 2));
        const report = data.candidates?.[0]?.content?.parts?.[0]?.text || "Report Generation Failed.";
        console.log("📋 Report length:", report.length);

        return new Response(
            JSON.stringify({ report: report }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        )

    } catch (error) {
        return new Response(
            JSON.stringify({ error: error.message }),
            { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        )
    }
})
