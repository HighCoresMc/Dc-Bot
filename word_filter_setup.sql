-- Table structure creation if they do not exist (JPA handles this, but here for SQL safety)
CREATE TABLE IF NOT EXISTS word_filter (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(255) UNIQUE NOT NULL,
    strict BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS word_whitelist (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(255) UNIQUE NOT NULL
);

-- Add strict column if it does not exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name='word_filter' AND column_name='strict'
    ) THEN
        ALTER TABLE word_filter ADD COLUMN strict BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- Insert words if not already present
INSERT INTO word_filter (word, strict) VALUES
('http:', TRUE),
('https:', TRUE),
('184.57.51.245', TRUE),
('1190305586710073427', TRUE),
('زبوري', TRUE),
('نيك', TRUE),
('تناك', TRUE),
('منوك', TRUE),
('ينيك', TRUE),
('قحبة', TRUE),
('قحبه', TRUE),
('قاحيب', TRUE),
('شرموطه', TRUE),
('شرموطة', TRUE),
('شراميط', TRUE),
('عرص', TRUE),
('ديوث', TRUE),
('شاذ', TRUE),
('زب', TRUE),
('خنيث', TRUE),
('اير', TRUE),
('كس', TRUE),
('طيز', TRUE),
('خرا', TRUE),
('ابن الحرام', TRUE),
('عيال الحرام', TRUE),
('ابن ال', TRUE),
('طيرك', TRUE),
('طيري', TRUE),
('طير', TRUE)
ON CONFLICT (word) DO UPDATE SET strict = EXCLUDED.strict;
