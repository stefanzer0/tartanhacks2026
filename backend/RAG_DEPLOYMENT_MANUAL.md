# RAG Deployment Guide - Manual Approach

Since the Supabase CLI isn't installed, you can deploy everything through the Supabase Dashboard and direct API calls.

## Step 1: Run Database Migrations (Supabase Dashboard)

1. Go to your Supabase project dashboard: https://supabase.com/dashboard
2. Navigate to **SQL Editor**
3. Click **New Query**
4. Copy and paste the contents of each migration file:

### Migration 1: Create knowledge_chunks table
Copy the entire contents of:
`backend/supabase/migrations/20260207_create_knowledge_chunks.sql`

Run it in the SQL Editor.

### Migration 2: Create match function
Copy the entire contents of:
`backend/supabase/migrations/20260207_create_match_function.sql`

Run it in the SQL Editor.

## Step 2: Deploy Edge Functions (Supabase Dashboard)

### Deploy ask-advisor
1. Go to **Edge Functions** in your dashboard
2. Find `ask-advisor` function
3. Click **Edit**
4. Replace the code with contents from:
   `backend/supabase/functions/ask-advisor/index.ts`
5. Click **Deploy**

### Deploy process-knowledge-base
1. Click **Create a new function**
2. Name it: `process-knowledge-base`
3. Paste contents from:
   `backend/supabase/functions/process-knowledge-base/index.ts`
4. Click **Deploy**

## Step 3: Process Your Document

Since you have the document uploaded at `backend/knowledge-base/legal-rights-document.txt`, use this curl command:

```powershell
# Get your Supabase URL and Anon Key from dashboard
$SUPABASE_URL = "YOUR_PROJECT_URL"  # e.g., https://xxxxx.supabase.co
$ANON_KEY = "YOUR_ANON_KEY"

# Process the document
curl -X POST "$SUPABASE_URL/functions/v1/process-knowledge-base" `
  -H "Authorization: Bearer $ANON_KEY" `
  -F "document=@backend/knowledge-base/legal-rights-document.txt"
```

## Alternative: Use Postman or Similar

1. Create POST request to: `YOUR_PROJECT_URL/functions/v1/process-knowledge-base`
2. Add header: `Authorization: Bearer YOUR_ANON_KEY`
3. Body type: `form-data`
4. Add file field named `document` with your `.txt` file
5. Send request

## Verify Success

Check that chunks were created:
1. Go to **Table Editor** in dashboard
2. Find `knowledge_chunks` table
3. Should see rows with content and embeddings

## Where to Find Your Credentials

- **Project URL**: Dashboard → Settings → API → Project URL
- **Anon Key**: Dashboard → Settings → API → Project API keys → anon public

That's it! The RAG system will be live.
