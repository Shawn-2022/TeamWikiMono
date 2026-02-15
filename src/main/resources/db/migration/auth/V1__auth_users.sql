-- AUTH: users + enums

create type user_role as enum ('ADMIN','EDITOR','VIEWER');

create table if not exists users (
    id bigserial primary key,
    username varchar(80) not null unique,
    password_hash varchar(255) not null,
    role user_role not null
);
