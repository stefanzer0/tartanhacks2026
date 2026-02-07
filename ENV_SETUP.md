# Environment Variables Setup Guide

This guide explains how to securely manage API keys and secrets in this repository.

## Quick Start

1. **Copy the example file**:
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual keys**:
   ```bash
   # Open .env and replace placeholder values with your real API keys
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```

3. **Never commit `.env`**:
   The `.gitignore` file is already configured to exclude `.env` from version control.

## Files Overview

### `.env`
- Contains your **actual secret keys**
- **NEVER** commit this file to GitHub
- Each team member needs their own `.env` file with their own keys
- Already listed in `.gitignore`

### `.env.example`
- Template showing which environment variables are needed
- **Safe to commit** to GitHub (contains no real secrets)
- Use this as a reference for what keys you need

### `.gitignore`
- Configured to exclude `.env` and other sensitive files
- Ensures secrets don't accidentally get committed

## For Supabase Edge Functions

Supabase Edge Functions (Deno runtime) don't use `.env` files directly. Instead:

1. **Set secrets using Supabase CLI**:
   ```bash
   supabase secrets set GEMINI_API_KEY=your_actual_key
   ```

2. **Access in your function**:
   ```typescript
   const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY');
   ```

3. **For local development**, create a `.env` file in your function directory and use:
   ```typescript
   import "https://deno.land/std@0.168.0/dotenv/load.ts";
   ```

## For Node.js Backend

If you're using Node.js (like `test-gemini.js`):

1. **Install dotenv**:
   ```bash
   npm install dotenv
   ```

2. **Use the config helper**:
   ```javascript
   import config from './config.js';
   
   const GEMINI_API_KEY = config.geminiApiKey;
   const GEMINI_URL = config.geminiUrl;
   ```

## Security Checklist

Before pushing to GitHub:

- [ ] `.env` is listed in `.gitignore`
- [ ] No hardcoded API keys in source code
- [ ] `.env.example` has placeholder values only
- [ ] All team members have their own `.env` file locally
- [ ] Supabase secrets are set via CLI for production

## Current Implementation Status

⚠️ **Action Required**: The following files still have hardcoded API keys that need to be removed:

1. `backend/supabase/functions/ask-advisor/index.ts` (line 4)
2. `backend/supabase/functions/generate-report/index.ts` (line 3)
3. `backend/test-gemini.js` (line 3)

These should be updated to use environment variables instead.
