import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY');
const EMBEDDING_MODEL = 'models/text-embedding-004';
const EMBEDDING_URL = `https://generativelanguage.googleapis.com/v1beta/${EMBEDDING_MODEL}:embedContent`;

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

// Helper function to generate embedding for text
async function generateEmbedding(text: string): Promise<number[]> {
    const response = await fetch(EMBEDDING_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'x-goog-api-key': GEMINI_API_KEY,
        },
        body: JSON.stringify({
            model: EMBEDDING_MODEL,
            content: {
                parts: [{ text }]
            }
        })
    });

    const data = await response.json();
    return data.embedding?.values || [];
}

// Helper function to split text into chunks
function chunkText(text: string, chunkSize: number = 800): string[] {
    const sentences = text.match(/[^.!?]+[.!?]+/g) || [text];
    const chunks: string[] = [];
    let currentChunk = '';

    for (const sentence of sentences) {
        if ((currentChunk + sentence).length > chunkSize && currentChunk.length > 0) {
            chunks.push(currentChunk.trim());
            currentChunk = sentence;
        } else {
            currentChunk += ' ' + sentence;
        }
    }

    if (currentChunk.trim().length > 0) {
        chunks.push(currentChunk.trim());
    }

    return chunks;
}

serve(async (req) => {
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        console.log("📄 Processing knowledge base document...");

        const formData = await req.formData();
        const file = formData.get('document');

        if (!file) {
            throw new Error("No document provided");
        }

        console.log("📁 File received:", file.name, file.size, "bytes");

        // Read file content (supports .txt for now, .docx would need additional library)
        const text = await file.text();
        console.log("📖 Document length:", text.length, "characters");

        // Split into chunks
        const chunks = chunkText(text);
        console.log("✂️ Split into", chunks.length, "chunks");

        // Initialize Supabase client
        const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
        const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
        const supabase = createClient(supabaseUrl, supabaseKey);

        // Clear existing knowledge chunks
        console.log("🗑️ Clearing existing chunks...");
        await supabase.from('knowledge_chunks').delete().neq('id', '00000000-0000-0000-0000-000000000000');

        // Process each chunk
        let processedCount = 0;
        for (const [index, chunk] of chunks.entries()) {
            console.log(`Processing chunk ${index + 1}/${chunks.length}...`);

            // Generate embedding
            const embedding = await generateEmbedding(chunk);

            // Store in database
            const { error } = await supabase
                .from('knowledge_chunks')
                .insert({
                    content: chunk,
                    embedding: embedding,
                    metadata: {
                        chunk_index: index,
                        total_chunks: chunks.length,
                        source_file: file.name,
                        processed_at: new Date().toISOString()
                    }
                });

            if (error) {
                console.error(`Error storing chunk ${index}:`, error);
            } else {
                processedCount++;
            }

            // Small delay to avoid rate limiting
            if (index < chunks.length - 1) {
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }

        console.log(`✅ Successfully processed ${processedCount}/${chunks.length} chunks`);

        return new Response(
            JSON.stringify({
                success: true,
                chunks_processed: processedCount,
                total_chunks: chunks.length,
                message: "Knowledge base updated successfully"
            }),
            { headers: { "Content-Type": "application/json", ...corsHeaders } }
        )

    } catch (error) {
        console.error("❌ Error:", error);
        return new Response(
            JSON.stringify({ error: error.message }),
            { status: 500, headers: { "Content-Type": "application/json", ...corsHeaders } }
        )
    }
})
