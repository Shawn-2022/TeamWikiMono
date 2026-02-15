# Mini Team Wiki (Spring Boot Monolith)

This repository is a **monolith** designed to be split later into 3 microservices:

- **auth** (JWT login, users/roles)
- **core wiki** (spaces, articles, versions, review/publish workflow, tags, comments, search)
- **audit** (audit log + recent activity feed)

## Packages

- `com.wiki.monowiki.auth` – auth + security
- `com.wiki.monowiki.wiki` – core wiki
- `com.wiki.monowiki.audit` – audit/activity
- `com.wiki.monowiki.common` – shared config + response wrappers

## Database

PostgreSQL + Flyway.

Flyway migrations are separated by module:

- `src/main/resources/db/migration/auth`
- `src/main/resources/db/migration/core`
- `src/main/resources/db/migration/audit`

> PostgreSQL ENUM types are used for roles/status/audit event types.

## Run locally

1) Start PostgreSQL and create a DB (default: `mono`).
2) Update `src/main/resources/application.properties` (URL/user/pass + `app.jwt.secret`).
3) Run the application.

### Swagger

- Swagger UI: `/swagger-ui.html`
- JWT is configured as **Bearer** auth (use the **Authorize** button).

### Create a user (dev)

There is no public registration endpoint by design.

For local/dev, you can either:

- Insert a user row manually into `users` (with a bcrypt password hash), **or**
- Uncomment `com.wiki.monowiki.auth.service.UserSeeder` to seed `admin/editor/viewer` users on startup.
