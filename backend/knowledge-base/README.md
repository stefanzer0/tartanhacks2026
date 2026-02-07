# Knowledge Base

This directory contains the legal rights document used for RAG (Retrieval Augmented Generation).

## Upload Your Document Here

Please upload your Word document as:
- `legal-rights-document.docx`

## Processing

Once uploaded, run the `process-knowledge-base` Supabase function to:
1. Parse the document
2. Split into chunks
3. Generate embeddings
4. Store in vector database

## Supported Formats
- .docx (Microsoft Word)
- .txt (Plain text)
- .pdf (PDF documents)
