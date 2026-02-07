-- Enable pgvector extension to work with embeddings
create extension if not exists vector;

-- Create a table to store legal knowledge chunks
create table if not exists legal_knowledge (
  id uuid primary key default gen_random_uuid(),
  content text not null,
  embedding vector(768) -- Dimension for Gemini embeddings
);

-- Create a function to search for legal knowledge
create or replace function match_legal_knowledge (
  query_embedding vector(768),
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  content text,
  similarity float
)
language plpgsql
as $$
begin
  return query
  select
    legal_knowledge.id,
    legal_knowledge.content,
    1 - (legal_knowledge.embedding <=> query_embedding) as similarity
  from legal_knowledge
  where 1 - (legal_knowledge.embedding <=> query_embedding) > match_threshold
  order by similarity desc
  limit match_count;
end;
$$;
