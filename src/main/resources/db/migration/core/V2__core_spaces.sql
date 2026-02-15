-- CORE: spaces

create table if not exists spaces (
    id bigserial primary key,
    space_key varchar(64) not null unique,
    name varchar(120) not null,
    created_at timestamptz not null default now()
);
