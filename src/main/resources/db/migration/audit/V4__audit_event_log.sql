-- AUDIT: audit/activity stream + enums

create type audit_entity_type as enum ('SPACE','ARTICLE','VERSION','REVIEW_REQUEST','TAG','COMMENT');

create type audit_event_type as enum (
    'SPACE_CREATED',
    'ARTICLE_CREATED',
    'ARTICLE_TITLE_UPDATED',
    'VERSION_ADDED',
    'REVIEW_SUBMITTED',
    'REVIEW_APPROVED',
    'REVIEW_REJECTED',
    'TAG_ADDED_TO_ARTICLE',
    'TAG_REMOVED_FROM_ARTICLE',
    'COMMENT_ADDED'
);

create table if not exists audit_event_log (
    id bigserial primary key,
    event_type audit_event_type not null,
    entity_type audit_entity_type not null,
    entity_id bigint not null,
    space_key varchar(64),
    article_id bigint,
    actor varchar(80) not null,
    message varchar(300) not null,
    meta_json text,
    created_at timestamptz not null default now()
);

create index if not exists idx_audit_created_at on audit_event_log(created_at);
create index if not exists idx_audit_space_created on audit_event_log(space_key, created_at);
create index if not exists idx_audit_article_created on audit_event_log(article_id, created_at);
create index if not exists idx_audit_actor_created on audit_event_log(actor, created_at);
