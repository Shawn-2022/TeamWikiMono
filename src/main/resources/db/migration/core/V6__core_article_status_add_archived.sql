-- CORE: align DB enum with code (ArticleStatus includes ARCHIVED)

-- PostgreSQL supports IF NOT EXISTS for ADD VALUE on modern versions.
-- If running on an older PostgreSQL, replace with a DO $$ BEGIN ... EXCEPTION ... END $$ block.
alter type article_status add value if not exists 'ARCHIVED';
