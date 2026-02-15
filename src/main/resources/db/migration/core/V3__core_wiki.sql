-- CORE: wiki schema + enums

create type article_status as enum ('DRAFT','IN_REVIEW','PUBLISHED');

create type review_status as enum ('PENDING','APPROVED','REJECTED');

-- =========================
-- ARTICLES
-- =========================
create table if not exists articles (
    id bigserial primary key,
    space_id bigint not null references spaces(id),
    slug varchar(140) not null,
    title varchar(200) not null,
    status article_status not null default 'DRAFT',
    current_version_no int not null default 0,
    created_by varchar(80) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_space_slug unique (space_id, slug)
);

create index if not exists idx_articles_space on articles(space_id);
create index if not exists idx_articles_status on articles(status);
create index if not exists idx_articles_updated_at on articles(updated_at);

-- =========================
-- ARTICLE VERSIONS
-- =========================
create table if not exists article_versions (
    id bigserial primary key,
    article_id bigint not null references articles(id) on delete cascade,
    version_no int not null,
    content text not null,
    created_by varchar(80) not null,
    created_at timestamptz not null default now(),
    constraint uk_article_versionno unique(article_id, version_no)
);

create index if not exists idx_article_versions_article on article_versions(article_id);

-- =========================
-- REVIEW REQUESTS
-- =========================
create table if not exists review_requests (
    id bigserial primary key,
    article_id bigint not null references articles(id) on delete cascade,
    status review_status not null default 'PENDING',
    requested_by varchar(80) not null,
    requested_at timestamptz not null default now(),
    reviewed_by varchar(80),
    reviewed_at timestamptz,
    reason varchar(500)
);

create index if not exists idx_review_requests_article on review_requests(article_id);
create index if not exists idx_review_requests_status on review_requests(status);

-- =========================
-- TAGS + ARTICLE_TAGS
-- =========================
create table if not exists tags (
    id bigserial primary key,
    name varchar(80) not null,
    constraint uk_tag_name unique(name)
);

create table if not exists article_tags (
    id bigserial primary key,
    article_id bigint not null references articles(id) on delete cascade,
    tag_id bigint not null references tags(id) on delete cascade,
    constraint uk_article_tag unique(article_id, tag_id)
);

create index if not exists idx_article_tags_article on article_tags(article_id);
create index if not exists idx_article_tags_tag on article_tags(tag_id);

-- =========================
-- VERSION COMMENTS
-- =========================
create table if not exists version_comments (
    id bigserial primary key,
    article_id bigint not null references articles(id) on delete cascade,
    version_no int not null,
    body text not null,
    created_by varchar(80) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_comment_article_version on version_comments(article_id, version_no);
