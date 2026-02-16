# Mini Team Wiki (Spring Boot 4 Monolith)

This repository implements the **Mini Team Wiki** as a **single monolith**, intentionally structured so it can later be split into **three microservices**:

- **Auth** – JWT login, users, roles
- **Core Wiki** – spaces, articles, versions, review/publish workflow, tags, comments, search
- **Audit** – audit log and recent activity feed

All API responses use standardized wrappers:

- `BaseResponse<T>`
- `BasePageResponse<T>`

---

## Package Layout (Split‑Ready)

```
com.wiki.monowiki.auth     → authentication, JWT, Spring Security
com.wiki.monowiki.wiki     → core wiki domain
com.wiki.monowiki.audit    → audit & activity
com.wiki.monowiki.common   → shared config & response wrappers
```

---

## Database (PostgreSQL + Flyway)

Flyway migrations are separated by module:

```
src/main/resources/db/migration/auth
src/main/resources/db/migration/core
src/main/resources/db/migration/audit
```

PostgreSQL **ENUM** types are used for:

- User roles
- Article status
- Review status
- Audit event and entity types

---

## Default Dev Users

Flyway seeds default users for local development.

| Username                        | Role   |
|---------------------------------|--------|
| admin1                          | ADMIN  |
| editor1, editor2                | EDITOR |
| viewer1, viewer2, viewer3       | VIEWER |

**Password (all users):** `ChangeMe123!`

---

## Run Locally

### 1) Start PostgreSQL (Docker)

```bash
docker run --name mono-wiki-db   -e POSTGRES_DB=moon   -e POSTGRES_USER=postgres   -e POSTGRES_PASSWORD=root   -p 5432:5432   -d postgres:16
```

> **Note:** `application.properties` currently points to  
> `jdbc:postgresql://localhost:5432/moon`

---

### 2) Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/moon
spring.datasource.username=postgres
spring.datasource.password=root

app.jwt.secret=<long-random-secret-at-least-32-chars>
```

---

### 3) Run Application

```bash
./mvnw spring-boot:run
```

---

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JWT Bearer authentication is configured globally
- All endpoints are secured **except** `/auth/login`

### Quick Swagger Flow

1. Call `POST /auth/login` with a seeded user
2. Copy `data.token` from the response
3. Click **Authorize** and paste:

```
Bearer <token>
```

---

## Core Workflow (High Level)

### Article Lifecycle

```
DRAFT → IN_REVIEW → PUBLISHED
        ↘ reject → DRAFT
```

- Creating an article starts in **DRAFT** (version #1 created)
- Versions can be added while in **DRAFT**
- Submitting moves the article to **IN_REVIEW**
- Approval publishes the article
- Rejection sends it back to **DRAFT**
- Optional soft delete: **ARCHIVED**

---

## Archive / Unarchive

### Archive

```
POST /articles/{id}/archive
Roles: ADMIN, EDITOR
```

- `DRAFT | PUBLISHED → ARCHIVED`
- Blocked when `IN_REVIEW` (returns **400**)

### Unarchive

```
POST /articles/{id}/unarchive
Roles: ADMIN, EDITOR
```

- `ARCHIVED → DRAFT`
- Review is required again before publishing

---

## Listing & Search Rules

- `includeArchived=true|false`
    - Allowed only for **ADMIN / EDITOR**
    - Default is `false`
- **VIEWER**
    - Sees only **PUBLISHED**
    - Archived and draft articles are hidden

---

## Audit

### Global Audit Feed

```
GET /audit
Roles: ADMIN, EDITOR
```

Filters:
- spaceKey
- articleId
- actorId
- actor
- eventType
- entityType
- entityId
- from / to (date range)

### Space Activity Feed

```
GET /spaces/{spaceKey}/activity
Roles: ALL
```

- VIEWER sees only public events

---

## Swagger Test Checklist

### 1) Swagger UI

- Open `GET /swagger-ui.html`
- Confirm tags:
  **Auth, Spaces, Articles, Versions, Review workflow, Tags, Comments, Search, Audit**

### 2) Public Login

- `POST /auth/login` must be **public**
- No auth lock icon in Swagger

### 3) Login & Authorize

- Username: `admin1`
- Password: `ChangeMe123!`
- Copy token → **Authorize** → `Bearer <token>`

### 4) Viewer Role Sanity

Login as `viewer1`:

- Space list works
- Article list/search returns only **PUBLISHED**
- Draft or in‑review slugs return **404**

### 5) Archive Flow (Editor)

As `editor1`:

1. Create article → `DRAFT`
2. Submit review → `IN_REVIEW`
3. Archive → **400**
4. Reject review → `DRAFT`
5. Archive → **200**, status `ARCHIVED`
6. List without `includeArchived` → hidden
7. List with `includeArchived=true` → visible
8. Unarchive → status `DRAFT`

### 6) Search includeArchived

- Editor:
    - `includeArchived=false` → archived hidden
    - `includeArchived=true` → archived visible
- Viewer:
    - Archived never visible

---

## Optional Next Improvements

- Swagger request/response examples
- Better paging & sorting parameter docs
- No behavior changes required
