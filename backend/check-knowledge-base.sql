-- Check if knowledge chunks were created
SELECT COUNT(*) as total_chunks FROM knowledge_chunks;

-- View sample chunks
SELECT 
  substring(content, 1, 100) as content_preview,
  created_at
FROM knowledge_chunks
ORDER BY created_at DESC
LIMIT 5;
