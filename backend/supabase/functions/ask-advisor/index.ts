import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY');
const GEMINI_MODEL = Deno.env.get('GEMINI_MODEL') || 'gemini-3-flash-preview';
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

if (!GEMINI_API_KEY) {
  throw new Error('GEMINI_API_KEY environment variable is required');
}

const SYSTEM_PROMPT = `You are a legal rights advisor. Listen to the user's audio and provide ONE brief tactical instruction (under 12 words). Examples: "Ask: Am I free to go?", "Say: I invoke my right to silence.", "Do not consent to searches."`;

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

serve(async (req) => {
  // 1. Handle CORS (Preflight)
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log("📥 Received request");
    const formData = await req.formData();
    const audioFile = formData.get('audio');

    if (!audioFile) {
      console.error("❌ No audio file in request");
      throw new Error("No audio file provided");
    }

    console.log("🎤 Audio file received:", audioFile.name, audioFile.size, "bytes");

    // Step 1: Upload audio to Gemini File API
    const audioBuffer = await audioFile.arrayBuffer();
    const audioBytes = new Uint8Array(audioBuffer);

    // Initial resumable request
    console.log("📤 Starting file upload...");
    const uploadInitResponse = await fetch("https://generativelanguage.googleapis.com/upload/v1beta/files", {
      method: 'POST',
      headers: {
        'x-goog-api-key': GEMINI_API_KEY,
        'X-Goog-Upload-Protocol': 'resumable',
        'X-Goog-Upload-Command': 'start',
        'X-Goog-Upload-Header-Content-Length': audioFile.size.toString(),
        'X-Goog-Upload-Header-Content-Type': 'audio/m4a',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ file: { display_name: 'voice_input' } })
    });

    const uploadUrl = uploadInitResponse.headers.get('x-goog-upload-url');
    if (!uploadUrl) {
      console.error("❌ No upload URL in response");
      throw new Error("Failed to get upload URL");
    }

    console.log("� Uploading audio bytes...");
    // Upload the actual bytes
    const uploadResponse = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'Content-Length': audioFile.size.toString(),
        'X-Goog-Upload-Offset': '0',
        'X-Goog-Upload-Command': 'upload, finalize'
      },
      body: audioBytes
    });

    const fileInfo = await uploadResponse.json();
    console.log("📁 File uploaded:", fileInfo.file?.uri);

    if (!fileInfo.file?.uri) {
      console.error("❌ No file URI in response:", fileInfo);
      throw new Error("Failed to upload audio file");
    }

    // Step 2: Generate content using the file URI
    const requestBody = {
      contents: [{
        parts: [
          { text: SYSTEM_PROMPT },
          { file_data: { mime_type: "audio/m4a", file_uri: fileInfo.file.uri } }
        ]
      }],
      generationConfig: {
        maxOutputTokens: 50,
        temperature: 0.3,
      }
    };

    console.log("🤖 Calling Gemini API with file URI...");
    const response = await fetch(GEMINI_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': GEMINI_API_KEY
      },
      body: JSON.stringify(requestBody)
    });

    const data = await response.json();
    console.log("📥 Gemini response status:", response.status);
    console.log("📄 Full Gemini response:", JSON.stringify(data, null, 2));

    if (!response.ok) {
      console.error("Gemini Error:", data);
      throw new Error(`Gemini API Error: ${data.error?.message || 'Unknown error'}`);
    }

    const advice = data.candidates?.[0]?.content?.parts?.[0]?.text || "System Error. Remain Silent.";
    console.log("💡 Extracted advice:", advice);

    return new Response(
      JSON.stringify({ advice: advice.trim() }),
      { headers: { "Content-Type": "application/json", 'Access-Control-Allow-Origin': '*' } }
    )

  } catch (error) {
    console.error(error)
    return new Response(
      JSON.stringify({ error: error.message, advice: "System Offline. Remain Silent." }),
      { status: 500, headers: { "Content-Type": "application/json", 'Access-Control-Allow-Origin': '*' } }
    )
  }
})
