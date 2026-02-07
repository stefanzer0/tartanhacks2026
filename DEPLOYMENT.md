# Deployment Guide: Setting Secrets for Supabase Edge Functions

This guide explains how to deploy your Supabase Edge Functions with proper secret management.

## Prerequisites

- Supabase CLI installed (`npm install -g supabase`)
- Supabase project created
- Logged in to Supabase CLI (`supabase login`)

## Set Environment Variables for Production

### Option 1: Using Supabase CLI (Recommended)

Set your secrets using the Supabase CLI:

```bash
# Set the Gemini API key
supabase secrets set GEMINI_API_KEY=your_actual_gemini_api_key

# Set the model (optional, defaults to gemini-3-flash-preview)
supabase secrets set GEMINI_MODEL=gemini-3-flash-preview
```

### Option 2: Using Supabase Dashboard

1. Go to your Supabase project dashboard
2. Navigate to **Settings** → **Edge Functions**
3. Click on **Manage secrets**
4. Add the following secrets:
   - `GEMINI_API_KEY`: Your actual Gemini API key
   - `GEMINI_MODEL`: `gemini-3-flash-preview` (optional)

## Deploy Edge Functions

Deploy your functions to Supabase:

```bash
# Deploy all functions
supabase functions deploy

# Or deploy individual functions
supabase functions deploy ask-advisor
supabase functions deploy generate-report
```

## Local Development with Supabase Functions

For local testing of Edge Functions, you have two options:

### Option 1: Use inline environment variables

```bash
# Set env vars inline when running locally
GEMINI_API_KEY=your_key supabase functions serve
```

### Option 2: Create a local .env file

The Supabase CLI will automatically load environment variables from a `.env` file in your function directory or project root when using `supabase functions serve`.

1. Make sure `.env` exists in your project root with the API key
2. Run the local development server:

```bash
supabase functions serve
```

## Testing Your Setup

### Test the Node.js file locally

```bash
cd backend
npm test
```

This will run `test-gemini.js` which loads variables from `../.env`.

### Test Edge Functions locally

```bash
# Start local Supabase services
supabase functions serve ask-advisor

# In another terminal, test the function
curl -i --location --request POST 'http://localhost:54321/functions/v1/ask-advisor' \
  --header 'Content-Type: multipart/form-data' \
  --header 'Authorization: Bearer YOUR_ANON_KEY' \
  --form 'audio=@/path/to/test-audio.m4a'
```

## Verify Secrets Are Set

Check which secrets are set in your Supabase project:

```bash
supabase secrets list
```

## Security Best Practices

✅ **DO:**
- Use `supabase secrets set` for production secrets
- Keep `.env` in `.gitignore`
- Use `.env.example` to document required variables
- Never hardcode API keys in source code

❌ **DON'T:**
- Commit `.env` files to Git
- Share API keys in chat/email
- Use the same keys for local and production
- Expose keys in client-side code

## Troubleshooting

### "GEMINI_API_KEY environment variable is required" error

This means the environment variable isn't set. Solutions:

**For Edge Functions:**
```bash
supabase secrets set GEMINI_API_KEY=your_key
```

**For local Node.js:**
- Verify `.env` file exists in project root
- Check that `GEMINI_API_KEY=your_key` is in the file
- Verify no extra spaces or quotes around the value

### TypeScript "Cannot find name 'Deno'" errors

These are **expected** and can be ignored. VS Code doesn't understand the Deno runtime context used by Supabase Edge Functions. These errors won't affect deployment or runtime.

To suppress these warnings, you can create a `deno.jsonc` file in your function directories, but it's not required for the functions to work.

## Current Project Status

✅ **Completed:**
- Created `.env` and `.env.example` files
- Added `.gitignore` to protect secrets
- Updated all 3 files to use environment variables:
  - `backend/supabase/functions/ask-advisor/index.ts`
  - `backend/supabase/functions/generate-report/index.ts`
  - `backend/test-gemini.js`
- Installed required dependencies (dotenv, node-fetch)
- Configured ES module support

⚠️ **Next Steps:**
1. Set secrets in Supabase using `supabase secrets set GEMINI_API_KEY=your_key`
2. Deploy functions using `supabase functions deploy`
3. Test the deployed functions
