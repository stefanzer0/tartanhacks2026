-- Enable the pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Create table to store knowledge base chunks with embeddings
CREATE TABLE IF NOT EXISTS knowledge_chunks (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  content TEXT NOT NULL,
  embedding vector(768), -- Gemini embedding dimension
  metadata JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS knowledge_chunks_embedding_idx 
ON knowledge_chunks 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Create index for metadata searches
CREATE INDEX IF NOT EXISTS knowledge_chunks_metadata_idx 
ON knowledge_chunks 
USING gin (metadata);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to auto-update updated_at
CREATE TRIGGER update_knowledge_chunks_updated_at 
BEFORE UPDATE ON knowledge_chunks
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Comment on table
COMMENT ON TABLE knowledge_chunks IS 'Stores chunked legal rights document with vector embeddings for RAG';
