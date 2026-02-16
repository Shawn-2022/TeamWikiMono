-- AUDIT: add archive/unarchive event types for optional soft-delete workflow

alter type audit_event_type add value if not exists 'ARTICLE_ARCHIVED';
alter type audit_event_type add value if not exists 'ARTICLE_UNARCHIVED';
