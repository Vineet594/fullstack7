# Secure CMS - Content & User Management System

A production-style Spring Boot 3 REST API (Java 17) with a small web console. It combines REST design, centralised error handling, request logging with correlation IDs, pagination and sorting, caching, query optimisation (N+1 fix), JWT authentication, role-based access control, AES-256-GCM encryption and access/refresh token rotation in **one** project.

```
secure-cms/
├── backend/            Spring Boot API  -> deploy on Render (Docker)
├── frontend/           Static web console -> deploy on Netlify
├── docs/VIVA.md        Experiment mapping, viva Q&A, common errors
├── render.yaml         Render Blueprint (API + PostgreSQL)
├── netlify.toml        Netlify config
├── docker-compose.yml  Local MySQL
└── .env.example        Environment variable template (no real secrets)
```

## Features
REST CRUD for Users, Posts, Categories · JWT access + rotating refresh tokens · ADMIN/USER roles with `@PreAuthorize` and ownership checks · BCrypt passwords · AES-GCM encrypted phone numbers · validation · `@RestControllerAdvice` error handling · request logging + `X-Correlation-ID` in MDC · pagination (max 50) + whitelisted sorting · Caffeine caching with eviction · `JOIN FETCH` / `EntityGraph` + indexes · live N+1 benchmark endpoint · Swagger UI with Bearer auth · CORS for explicit origins · JUnit 5 + Mockito + MockMvc tests.

## Technologies
Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA (Hibernate 6), Spring Security 6, jjwt 0.12, MySQL (local) / PostgreSQL (Render) / H2 (dev + tests), Caffeine, Lombok, springdoc-openapi 2.6, Maven, Docker, vanilla HTML/CSS/JS frontend.

## Architecture
```
 Browser / Swagger / Postman
            │  HTTPS + JSON
            ▼
 ┌──────────────────────────────────────────────┐
 │ CorrelationIdFilter → RequestLoggingFilter   │  (filter package)
 │ Spring Security chain: JwtAuthenticationFilter│  (security package)
 └──────────────────────┬───────────────────────┘
                        ▼
   Controller  (HTTP, validation, @PreAuthorize)
        ▼
   Service     (business rules, ownership, @Cacheable/@CacheEvict, transactions)
        ▼
   Repository  (Spring Data JPA, JOIN FETCH)
        ▼
   MySQL / PostgreSQL
   GlobalExceptionHandler (@RestControllerAdvice) converts every exception to a uniform JSON error.
```

## Database design
```
 roles 1───* users 1───* posts *───1 categories
                │
                └───* refresh_tokens
```
* `User -> Role` (many-to-one): each user has exactly one role.
* `Post -> User` (author) and `Post -> Category`: many posts per author/category. Relations are LAZY and one-directional, so no huge collections are loaded by accident.
* `RefreshToken -> User`: lets us revoke sessions (logout) and detect token reuse. Only a SHA-256 hash is stored.

Indexes: unique `users.username`, `users.email` (login and duplicate checks), `posts.title` (search/sort by title), `posts.category_id` and `posts.author_id` (joins/filters), `posts.created_at` (newest-first sorting), unique `refresh_tokens.token_hash` (token lookup).

## API endpoints
| Method | Path | Access | Notes |
|---|---|---|---|
| POST | /api/auth/register | public | 201, 400, 409 |
| POST | /api/auth/login | public | returns access + refresh token |
| POST | /api/auth/refresh | public | rotates refresh token |
| POST | /api/auth/logout | public | revokes refresh token |
| GET | /api/users/me | any user | own profile |
| GET | /api/users?page&size&sort | ADMIN | paginated |
| GET / POST | /api/users, /api/users/{id} | ADMIN | |
| PATCH | /api/users/{id}/role | ADMIN | change role |
| DELETE | /api/users/{id} | ADMIN | 204, also deletes their posts |
| GET | /api/posts?page&size&sort | any user | JOIN FETCH, sort: title, createdAt, updatedAt |
| GET | /api/posts/{id} | any user | cached |
| POST | /api/posts | any user | 201 |
| PUT / DELETE | /api/posts/{id} | author or ADMIN | 403 otherwise |
| GET | /api/categories, /api/categories/{id} | any user | cached |
| POST / PUT / DELETE | /api/categories | ADMIN | evicts caches |
| GET | /api/benchmark/posts?size=20 | ADMIN | real SQL-count/time comparison |
| GET | /actuator/health, / | public | health check |

## Authentication and authorization
Authentication = *who are you?* (`/api/auth/login` verifies BCrypt hash, issues a JWT). Authorization = *what may you do?* (roles in the JWT + `@PreAuthorize`; ownership check in `PostService`). Access token: 15 minutes, claims `userId`, `username`, `role`. Missing/invalid token → JSON 401, wrong role → JSON 403.

## Access and refresh tokens
Refresh token = random 256-bit string, stored only as a SHA-256 hash, valid 7 days, **single use**. `/api/auth/refresh` revokes the old one and returns a new access + refresh token. Re-using an old refresh token revokes all sessions of that user. `/api/auth/logout` revokes the token.

## AES encryption
`EncryptionService` uses `AES/GCM/NoPadding`, random 12-byte IV per value (stored with the ciphertext), key derived from `AES_SECRET_KEY`. Passwords use BCrypt (one-way hash); phone numbers use AES (reversible, needed for display). The API only returns masked phones.

## Pagination, sorting, caching, N+1, query optimisation
* `GET /api/posts?page=0&size=10&sort=createdAt,desc`; size capped at 50; invalid sort field → 400.
* Caching: `@Cacheable` on categories and `posts/{id}`; `@CachePut` on update; `@CacheEvict` on create/delete. First call = cache **miss** (DB query, logged), repeated calls = **hit**.
* N+1: `findAll()` + lazy author/category = 1 + (distinct authors) + (distinct categories) queries. `findAllWithDetails()` uses `JOIN FETCH` = 1 data query (+1 count for paging). Run `GET /api/benchmark/posts` as ADMIN (or the "Performance" tab) to see **measured** numbers on your own database. Set `SHOW_SQL=true` to see the SQL.

## Exception handling, logging, correlation ID
Controllers/services just throw exceptions; `GlobalExceptionHandler` formats them: `{success:false, message, errorCode, timestamp, path, correlationId}`. Log line: `POST /api/posts - user=alice - status=201 - time=85ms`, prefixed with `correlationId=...`. Clients may send `X-Correlation-ID`; otherwise one is generated; it is returned in the response header. Passwords, tokens and keys are never logged.

## Run locally
**Fastest (no database):**
```bash
cd backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run        # Windows PowerShell: $env:SPRING_PROFILES_ACTIVE="dev"; mvn spring-boot:run
```
Open http://localhost:8080/swagger-ui.html. Demo users (dev only): `admin`, `alice`, `bob`, `charlie`, password `Demo@12345`.

**With MySQL:** `docker compose up -d`, copy `.env.example` to `.env`, export the variables, then `mvn spring-boot:run`.

**Frontend:** `cd frontend && npx serve -l 3000 .` (or open with VS Code Live Server). `config.js` points to `http://localhost:8080`.

**Tests:** `cd backend && mvn test`

## Environment variables
| Variable | Purpose |
|---|---|
| DB_TYPE, DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, DB_PARAMS | database (mysql or postgresql) |
| JWT_SECRET | JWT signing secret, ≥ 32 chars |
| AES_SECRET_KEY | AES key material, ≥ 32 chars (losing it makes stored phones unreadable) |
| CORS_ALLOWED_ORIGINS | comma-separated frontend origins |
| SEED_SAMPLE_DATA, SEED_DEMO_PASSWORD | demo data (demo only) |
| SHOW_SQL | print SQL for the N+1 demo |

## Deploy
**1. Backend on Render** - push this repo to GitHub → Render dashboard → *New → Blueprint* → pick the repo. `render.yaml` creates the PostgreSQL database and the Docker web service, generates `JWT_SECRET` and `AES_SECRET_KEY`, and asks for `SEED_DEMO_PASSWORD` and `CORS_ALLOWED_ORIGINS`. After deploy open `https://<your-service>.onrender.com/swagger-ui.html`.
Notes: free Render services sleep when idle (first request is slow) and free databases have limits/expiry, so check Render's current free-tier terms. For real production use a paid instance and database migrations (Flyway) instead of `ddl-auto=update`.

**2. Frontend on Netlify** - edit `frontend/config.js` and set `API_BASE_URL` to your Render URL → commit → Netlify *Add new site → Import from Git* (publish directory `frontend`, no build command; `netlify.toml` is included). Then set `CORS_ALLOWED_ORIGINS` on Render to your Netlify URL (e.g. `https://securecms.netlify.app`) and redeploy.

## Example requests
```bash
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Demo@12345"}'
curl localhost:8080/api/posts?page=0\&size=5\&sort=title,asc -H "Authorization: Bearer <accessToken>"
curl -X POST localhost:8080/api/auth/refresh -H "Content-Type: application/json" -d '{"refreshToken":"<refreshToken>"}'
curl -i localhost:8080/api/posts -H "X-Correlation-ID: demo-trace-12345" -H "Authorization: Bearer <accessToken>"
```

## Future improvements
Flyway migrations, Redis cache for multiple instances, rate limiting on login, email verification, httpOnly-cookie refresh tokens, scheduled cleanup of expired refresh tokens, Testcontainers tests, CI pipeline.
