import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY');
const GEMINI_MODEL = Deno.env.get('GEMINI_MODEL') || 'gemini-3-flash-preview';
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;
const EMBEDDING_MODEL = 'models/text-embedding-004';
const EMBEDDING_URL = `https://generativelanguage.googleapis.com/v1beta/${EMBEDDING_MODEL}:embedContent`;

if (!GEMINI_API_KEY) {
  throw new Error('GEMINI_API_KEY environment variable is required');
}

const BASE_SYSTEM_PROMPT = `You are a legal rights advisor. Provide ONE brief tactical instruction (under 200 words).`;

// Helper: Generate embedding for search query
async function generateQueryEmbedding(text: string): Promise<number[]> {
  const response = await fetch(EMBEDDING_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': GEMINI_API_KEY,
    },
    body: JSON.stringify({
      model: EMBEDDING_MODEL,
      content: { parts: [{ text }] }
    })
  });

  const data = await response.json();
  return data.embedding?.values || [];
}

// Helper: Search knowledge base
async function searchKnowledgeBase(queryText: string, limit: number = 3) {
  try {
    const embedding = await generateQueryEmbedding(queryText);

    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseKey);

    // Vector similarity search
    const { data, error } = await supabase.rpc('match_knowledge_chunks', {
      query_embedding: embedding,
      match_threshold: 0.7,
      match_count: limit
    });

    if (error) {
      console.error("❌ Knowledge base search error:", error);
      return [];
    }

    console.log(`📚 Found ${data?.length || 0} relevant knowledge chunks`);
    return data || [];
  } catch (error) {
    console.error("❌ RAG search failed:", error);
    return [];
  }
}

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
    console.log("📥 Received request");
    const formData = await req.formData();
    const audioFile = formData.get('audio');
    const contextStr = formData.get('context');

    if (!audioFile) {
      console.error("❌ No audio file in request");
      throw new Error("No audio file provided");
    }

    console.log("🎤 Audio file received:", audioFile.name, audioFile.size, "bytes");

    // Parse conversation context
    let conversationContext = [];
    if (contextStr) {
      try {
        conversationContext = JSON.parse(contextStr);
        console.log("💬 Conversation context:", conversationContext.length, "exchanges");
      } catch (e) {
        console.warn("⚠️ Failed to parse context:", e);
      }
    }

    // Step 1: Upload audio to Gemini File API
    const audioBuffer = await audioFile.arrayBuffer();
    const audioBytes = new Uint8Array(audioBuffer);

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

    console.log("📤 Uploading audio bytes...");
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

    // Step 2: RAG - Search knowledge base
    // Use recent conversation to create search query
    const recentContext = conversationContext.slice(-2);
    const searchQuery = recentContext.map(c => c.transcript || c.advice).join(' ');

    console.log("🔍 Searching knowledge base with query:", searchQuery.substring(0, 100));
    const knowledgeChunks = await searchKnowledgeBase(searchQuery || "legal rights during police interaction");

    // Build enhanced system prompt with RAG context
    let systemPrompt = BASE_SYSTEM_PROMPT;
    if (knowledgeChunks.length > 0) {
      const contextText = knowledgeChunks.map((chunk, i) =>
        `[Context ${i + 1}]: ${chunk.content}`
      ).join('\n\n');

      systemPrompt = `${BASE_SYSTEM_PROMPT}

RELEVANT LEGAL INFORMATION:
${contextText}

Based on the above context and the audio, provide specific tactical advice.`;

      console.log("✅ Enhanced prompt with", knowledgeChunks.length, "knowledge chunks");
    } else {
      console.log("ℹ️ No knowledge chunks found, using base prompt");
    }

    // Step 3: Generate content using the file URI
    const requestBody = {
      contents: [{
        parts: [
          { text: systemPrompt },
          { file_data: { mime_type: "audio/m4a", file_uri: fileInfo.file.uri } }
        ]
      }],
      generationConfig: {
        maxOutputTokens: 500,
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

    if (!response.ok) {
      console.error("Gemini Error:", data);
      throw new Error(`Gemini API Error: ${data.error?.message || 'Unknown error'}`);
    }

    const advice = data.candidates?.[0]?.content?.parts?.[0]?.text || "System Error. Remain Silent.";
    console.log("💡 Extracted advice:", advice);

    return new Response(
      JSON.stringify({
        advice: advice.trim(),
        rag_enabled: knowledgeChunks.length > 0,
        knowledge_chunks_used: knowledgeChunks.length
      }),
      { headers: { "Content-Type": "application/json", ...corsHeaders } }
    )

  } catch (error) {
    console.error(error)
    return new Response(
      JSON.stringify({ error: error.message, advice: "System Offline. Remain Silent." }),
      { status: 500, headers: { "Content-Type": "application/json", ...corsHeaders } }
    )
  }
})
