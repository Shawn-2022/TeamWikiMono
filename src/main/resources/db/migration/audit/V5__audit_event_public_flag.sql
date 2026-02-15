-- AUDIT: mark which audit events are safe to show to VIEWERs in the recent activity feed

alter table audit_event_log
    add column if not exists is_public boolean not null default false;

create index if not exists idx_audit_space_public_created
    on audit_event_log(space_key, is_public, created_at);

-- Best-effort backfill for existing data (if any):
-- REVIEW_APPROVED is the publish moment and is generally safe for a public feed.
update audit_event_log
set is_public = true
where event_type in ('SPACE_CREATED', 'REVIEW_APPROVED');
