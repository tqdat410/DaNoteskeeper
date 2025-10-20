-- Update embedding column to 768 dimensions (for embeddinggemma model)
ALTER TABLE notes
ALTER COLUMN embedding TYPE VECTOR(768);
