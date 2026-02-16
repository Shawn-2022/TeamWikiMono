-- CORE: enforce case-insensitive uniqueness for tags
--
-- Why:
--  - Service layer uses existsByNameIgnoreCase, but the DB constraint was case-sensitive.
--  - This migration makes the database the final authority, preventing duplicates like 'DevOps' and 'devops'.

-- Drop the original (case-sensitive) unique constraint (also drops its backing index)
alter table if exists tags
drop constraint if exists uk_tag_name;

-- Case-insensitive unique index
create unique index if not exists uk_tag_name_ci
    on tags (lower(name));
