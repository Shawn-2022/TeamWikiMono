-- AUDIT: add actor_id to support spec-style filtering (actorId) and future microservice split

alter table audit_event_log
    add column if not exists actor_id bigint;

create index if not exists idx_audit_actor_id_created
    on audit_event_log(actor_id, created_at);

-- Best-effort backfill for existing rows (monolith only, users table lives in the same DB)
update audit_event_log a
set actor_id = u.id
    from users u
where a.actor_id is null
  and a.actor = u.username;
