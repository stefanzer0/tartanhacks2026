import fetch from 'node-fetch';
import FormData from 'form-data';
import fs from 'fs';

const SUPABASE_URL = 'https://edwcajihuqxpngcwutej.supabase.co';
const ANON_KEY = process.env.ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZnbHlvaGhjbmhtanFxeWxweXNzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA0MzIyNDEsImV4cCI6MjA4NjAwODI0MX0.wR2IPYX9IhpkzL0Y-dzYPoz8HdeFvWY7oQX6W-OwqII';

async function processKnowledgeBase() {
    console.log('📄 Processing knowledge base document...');

    const formData = new FormData();
    formData.append('document', fs.createReadStream('knowledge-base/legal-rights-document.txt'));

    try {
        const response = await fetch(`${SUPABASE_URL}/functions/v1/process-knowledge-base`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${ANON_KEY}`,
                ...formData.getHeaders()
            },
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ Success!');
            console.log(`📊 Processed: ${data.chunks_processed}/${data.total_chunks} chunks`);
            console.log(`💾 Message: ${data.message}`);
        } else {
            console.error('❌ Error:', data);
        }
    } catch (error) {
        console.error('❌ Failed:', error.message);
    }
}

processKnowledgeBase();
