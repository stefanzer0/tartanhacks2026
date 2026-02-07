# RAG System - Troubleshooting Document Processing

## Current Status
- ✅ Database migrations complete
- ✅ Edge functions deployed
- ✅ Match function created
- ❌ Document processing failing (timing out)

## Issue
Document processing times out when calling `process-knowledge-base` function.

## Possible Causes
1. **Timeout limits**: Supabase Edge Functions have a 150-second timeout by default
2. **Large document**: Your document is ~35KB with 967 lines - may need chunking
3. **Embedding API rate limits**: Gemini API may be rate-limiting requests
4. **CORS/Network issues**: Request may not be reaching the function

## Next Steps

### Option 1: Check Function Logs (Recommended)
1. Go to Supabase Dashboard
2. Click **Edge Functions** → `process-knowledge-base`
3. Click **Logs** tab
4. Look for error messages or timeout indicators

### Option 2: Test Manually in Dashboard
1. Go to **Edge Functions** → `process-knowledge-base`
2. Click **Invoke Function**
3. Upload `test-rights.txt` (small test file I created)
4. See the actual error message

### Option 3: Simplify the Processing
The issue might be that we're processing all chunks in one request. I can modify the function to:
- Process in smaller batches
- Add progress logging
- Handle rate limits better

### Option 4: Use Supabase Storage Instead
Alternative approach:
1. Upload document to Supabase Storage
2. Process it server-side (no upload timeout)
3. Better for large documents

## Quick Fix Available
I can modify `process-knowledge-base` to be more robust if you'd like. Let me know what you see in the function logs!
