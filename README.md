# Secure Content Management & User Management System

## Overview

A production-style **Spring Boot 3 REST API** that combines, in ONE integrated project, the topics of
experiments 2.1.1 - 2.3.2: REST APIs, exception handling, logging, correlation IDs, pagination, sorting,
the N+1 problem, caching, query optimisation, JWT, RBAC, AES encryption and access/refresh tokens.

**Real-world problem:** a content platform (blog / college portal) needs users who can sign in securely, write
posts in categories, and browse large amounts of content quickly - while admins manage users and categories and
private data (phone numbers, passwords, tokens) stays protected.

## Features

- Register / login with BCrypt-hashed passwords
- JWT **access token** (15 min) + rotating **refresh token** (7 days, stored only as SHA-256 hash) + logout
- Role-based access control (`ADMIN`, `USER`) using `@PreAuthorize`, plus ownership rules (users edit only their own posts)
- AES-256-GCM encryption of the phone number (`encryptedPhone`)
- Pagination (max page size 50) and validated sorting (`title`, `createdAt`, `updatedAt`)
- N+1 problem demonstrated and solved with `JOIN FETCH`; entity graphs; DTO projection
- Caffeine caching with `@Cacheable`, `@CachePut`, `@CacheEvict`
- Real benchmark endpoint (time + number of SQL statements, measured live)
- Global exception handling (`@RestControllerAdvice`) with one consistent JSON error format
- Request logging + correlation ID (`X-Correlation-ID` in MDC, logs and response header)
- Swagger UI with JWT "Authorize" button, CORS for React/Angular, 20+ demo posts, tests

## Technologies Used

Java 17+, Spring Boot 3.4, Spring Web, Spring Data JPA (Hibernate), Spring Security, jjwt 0.12, MySQL 8,
Lombok, Jakarta Validation, BCrypt, Spring Cache + Caffeine, SLF4J + Logback, springdoc-openapi (Swagger UI),
Maven, JUnit 5, Mockito, Spring Boot Test, H2 (tests only).

## Architecture

```
 Client (Swagger UI / Postman / React / Angular)
        |  HTTP + JSON  (Authorization: Bearer <JWT>)
        v
 +-----------------------------------------------------------+
 | Servlet filters                                           |
 |   CorrelationIdFilter -> RequestLoggingFilter             |
 |   Spring Security chain: CORS -> JwtAuthenticationFilter  |
 +-----------------------------------------------------------+
        v
 +------------------+   DTOs    +-------------------------+
 |   Controller     | --------> |  GlobalExceptionHandler |
 | (REST + @PreAuth)|           |  (@RestControllerAdvice)|
 +------------------+           +-------------------------+
        v
 +------------------+     +-----------------------------+
 |     Service      | <-> | Cache (Caffeine)            |
 | business rules   |     | EncryptionService (AES-GCM) |
 +------------------+     +-----------------------------+
        v
 +------------------+
 |   Repository     |   Spring Data JPA (JOIN FETCH, projections)
 +------------------+
        v
 +------------------+
 |   MySQL          |
 +------------------+
```

Package layout (`com.securecms`): `config, controller, dto, entity, repository, service, security, exception,
filter, encryption, cache, util, mapper, bootstrap`.

## Database Design

```
 roles                users                                   categories
 +----+-------+       +----+----------+-------+-----------+   +----+------+-------------+
 | id | name  |<------| id | username | email | password  |   | id | name | description |
 +----+-------+  N:1  | encrypted_phone | role_id |created_at|  +----+------+-------------+
                      | updated_at                          |        ^
                      +----+--------------------------------+        | 1:N
                           | 1:N                                      |
                           v                                          |
                      posts  (id, title, content, author_id --> users.id,
                              category_id --> categories.id, created_at, updated_at)

 refresh_tokens (id, token_hash UNIQUE, user_id --> users.id, expires_at, revoked, created_at)
```

Why each relationship exists:

| Relationship | Reason |
|---|---|
| User N:1 Role | Every user has exactly one role; roles are shared, so they live in their own table |
| User 1:N Post | A post must have an author (needed for "edit own post" and audit) |
| Category 1:N Post | Posts are grouped/filtered by category |
| RefreshToken N:1 User | Lets us revoke all sessions of a user |

All associations are `LAZY` on purpose - this is what allows the N+1 demonstration and forces explicit fetching.

## API Endpoints

| Method | URL | Who | Status codes | Description |
|---|---|---|---|---|
| POST | /api/auth/register | public | 201, 400, 409 | Register a USER account |
| POST | /api/auth/login | public | 200, 400, 401 | Get access + refresh token |
| POST | /api/auth/refresh | public | 200, 401 | Rotate refresh token, new access token |
| POST | /api/auth/logout | public | 204, 400 | Revoke refresh token |
| GET | /api/users/me | any logged-in | 200, 401 | My profile |
| GET | /api/users?page&size&sort | ADMIN | 200, 400, 403 | List users (paginated) |
| GET | /api/users/{id} | ADMIN or self | 200, 403, 404 | One user |
| POST | /api/users | ADMIN | 201, 400, 409 | Create user with role |
| PATCH | /api/users/{id}/role | ADMIN | 200, 404 | Change role |
| DELETE | /api/users/{id} | ADMIN | 204, 404 | Delete user (+ posts) |
| GET | /api/roles | ADMIN | 200 | List roles |
| GET | /api/categories | ADMIN, USER | 200 | List categories (cached) |
| GET | /api/categories/{id} | ADMIN, USER | 200, 404 | One category (cached) |
| POST | /api/categories | ADMIN | 201, 409 | Create category |
| PUT | /api/categories/{id} | ADMIN | 200, 404, 409 | Update category |
| DELETE | /api/categories/{id} | ADMIN | 204, 404, 409 | Delete category (409 if used) |
| GET | /api/posts?page&size&sort&categoryId | ADMIN, USER | 200, 400 | Paginated + sorted posts (JOIN FETCH) |
| GET | /api/posts/summaries | ADMIN, USER | 200 | Lightweight projection |
| GET | /api/posts/{id} | ADMIN, USER | 200, 404 | One post (cached) |
| POST | /api/posts | ADMIN, USER | 201, 400, 404 | Create post (author = me) |
| PUT | /api/posts/{id} | ADMIN / owner | 200, 403, 404 | Update post |
| DELETE | /api/posts/{id} | ADMIN / owner | 204, 403, 404 | Delete post |
| GET | /api/admin/benchmark/posts?size=20 | ADMIN | 200 | N+1 vs JOIN FETCH vs cache benchmark |
| GET | /api/admin/benchmark/cache-stats | ADMIN | 200 | Real cache hit/miss counters |

Success format: `{ "success": true, "message": "...", "data": {...}, "timestamp": "..." }`
Error format: `{ "success": false, "message": "...", "errorCode": "POST_NOT_FOUND", "timestamp": "...", "path": "/api/posts/10", "correlationId": "..." }`
(validation errors additionally contain `validationErrors: { field: message }`).

## Authentication

**Authentication = "Who are you?"** Login (`POST /api/auth/login`) checks username + BCrypt password through Spring's
`AuthenticationManager`. On success a JWT access token is issued with claims `sub` (username), `userId`, `role`, `iat`, `exp`.

For every later request `JwtAuthenticationFilter` (a `OncePerRequestFilter`) reads `Authorization: Bearer ...`,
validates signature and expiry, and stores the user in the `SecurityContext`. Invalid or expired tokens get a
`401` JSON response (`INVALID_TOKEN` / `TOKEN_EXPIRED`). Tokens are never logged.

## Authorization

**Authorization = "What are you allowed to do?"** Implemented with method security (`@EnableMethodSecurity`):

| Action | ADMIN | USER |
|---|---|---|
| View posts / categories | yes | yes |
| Create posts | yes | yes (author = himself) |
| Update / delete posts | any post | only own posts (else 403) |
| Create / update / delete categories | yes | no (403) |
| List / create / delete users, change roles | yes | no (403) |
| Read a single user | any | only himself |

## Pagination & Sorting

`GET /api/posts?page=0&size=10&sort=createdAt,desc` uses Spring Data `Pageable`. The maximum page size is 50
(`spring.data.web.pageable.max-page-size`): `size=1000` is silently reduced to 50. Allowed sort fields are
validated by `SortValidator` (`title`, `createdAt`, `updatedAt`); anything else returns `400 INVALID_SORT_FIELD`.

Response `data`: `{ "content": [], "page": 0, "size": 10, "totalElements": 24, "totalPages": 3, "first": true, "last": false }`

## Caching

Caffeine (in-memory, no Redis needed): max 1000 entries, expire 10 minutes after write.

| Concept | Meaning | Where |
|---|---|---|
| Cache **miss** | Not cached -> method body runs, database queried, result stored (log: `Cache MISS ...`) | first `GET /api/categories`, first `GET /api/posts/{id}` |
| Cache **hit** | Found in cache -> method body skipped, 0 SQL queries | second identical call |
| **Invalidation** | Writes remove/refresh stale entries | `@CacheEvict` on create/delete, `@CachePut` on update |

Annotations: `@Cacheable` (CategoryService.findAll/findById, PostService.findById), `@CachePut` (update category/post),
`@CacheEvict` (create category, delete category/post/user, update category evicts cached posts because they show the category name).
Watch it live: `GET /api/admin/benchmark/cache-stats`.

## N+1 Problem

*BAD* - `postRepository.findAll(pageable)` and then touching `post.getAuthor()` / `post.getCategory()` of every post:

```sql
select ... from posts order by created_at desc limit ?          -- 1 query
select ... from users      where id = ?                          -- once per distinct author
select ... from categories where id = ?                          -- once per distinct category
```

*GOOD* - `PostRepository.findAllWithDetails` uses `JOIN FETCH`:

```sql
select p.*, u.*, c.* from posts p
  join users u on u.id = p.author_id
  join categories c on c.id = p.category_id
order by p.created_at desc limit ?                               -- ONE query (+1 count query for pagination)
```

Fewer round trips to the database = less latency and less load. The benchmark endpoint measures the real statement
count (Hibernate statistics). To *see* every SQL statement set `SHOW_SQL=true` in `.env`.

> Honest note: inside one Hibernate session each distinct author/category is loaded only once, so the extra queries
> equal the number of *distinct* authors + categories on the page. With 20 posts, 4 authors and 5 categories that is
> about 9 extra queries; with thousands of different authors it would be thousands.

## Query Optimization

| Technique | Where | Why it helps |
|---|---|---|
| Pagination | every list endpoint | never loads the whole table |
| `JOIN FETCH` | `PostRepository.findAllWithDetails` etc. | removes N+1 |
| `@EntityGraph` | `UserRepository` | loads the role in the same query as the user |
| DTO projection | `PostRepository.findAllSummaries` | selects only 5 columns, skips the large `content` column |
| Caching | categories, single post | repeated reads cost 0 SQL |
| LAZY associations + `open-in-view=false` | entities / config | no hidden queries |

Indexes:

| Index | Why |
|---|---|
| `users.username` (unique) | login lookup and duplicate check |
| `users.email` (unique) | duplicate check on registration |
| `posts.title` | sorting/searching by title |
| `posts.category_id` | filtering posts by category and the JOIN |
| `posts.author_id` | JOIN with users, "posts of a user" |
| `posts.created_at` | default sort order (newest first) |
| `refresh_tokens.token_hash` (unique) | fast token lookup |

## Benchmarking (no fake numbers)

1. Login as admin in Swagger, authorize, then call `GET /api/admin/benchmark/posts?size=20`.
2. The response lists 4 scenarios with **measured** `durationMillis` and `sqlStatements`:
   N+1, JOIN FETCH, single post cache MISS, single post cache HIT.
3. Run it several times (and with `size=50`); times vary by machine, statement counts are exact.
4. Optional: compare with curl `-w "%{time_total}"` on `GET /api/posts/{id}` (first vs second call).

## Exception Handling

`GlobalExceptionHandler` is annotated `@RestControllerAdvice` (= `@ControllerAdvice` + `@ResponseBody`). Spring routes
**every** exception thrown from any controller or service to the matching `@ExceptionHandler` method, which builds the
standard `ErrorResponse`. Therefore there are no try-catch blocks in controllers/services: they simply `throw`.

| Exception | Status | errorCode |
|---|---|---|
| MethodArgumentNotValidException | 400 | VALIDATION_FAILED (+ field list) |
| BadRequestException | 400 | e.g. INVALID_SORT_FIELD |
| ResourceNotFoundException | 404 | POST_NOT_FOUND, USER_NOT_FOUND ... |
| UserAlreadyExistsException / ResourceConflictException | 409 | USER_ALREADY_EXISTS, CATEGORY_ALREADY_EXISTS ... |
| InvalidTokenException / UnauthorizedException / BadCredentialsException | 401 | INVALID_TOKEN, TOKEN_EXPIRED, INVALID_CREDENTIALS ... |
| AccessDeniedException | 403 | ACCESS_DENIED |
| Exception (anything else) | 500 | INTERNAL_SERVER_ERROR (no stack trace leaked) |

Security-layer errors (missing token, forbidden) use the same JSON via `JwtAuthenticationEntryPoint` and `CustomAccessDeniedHandler`.

## Logging

`RequestLoggingFilter` writes one line per request, e.g.
`POST /api/posts - user=vineet - status=201 - time=85ms`. It logs only method, path, user, status and duration -
never passwords, tokens, headers, bodies, query strings or keys. Logs go to the console and to `logs/secure-cms.log`
(rolling, 14 days) via Logback (`logback-spring.xml`).

## Correlation ID

`CorrelationIdFilter` reads `X-Correlation-ID` (only if it is a safe short value) or generates a UUID, puts it into the
SLF4J **MDC**, and returns it in the response header. The log pattern prints `correlationId=...` on every line:

```
2026-10-05 20:15:20.123 INFO  correlationId=7f4a9c12-... [http-nio-8080-exec-1] c.s.f.RequestLoggingFilter - POST /api/posts - user=vineet - status=201 - time=85ms
```

Why useful: when a user reports an error, give them the ID from the response header/error body and grep the log for
that one ID to see everything that happened in that request - essential when many requests run in parallel or when one
request travels through several services.

## AES Encryption

`EncryptionService` uses `AES/GCM/NoPadding` with a fresh random 12-byte IV per encryption (stored together with the
ciphertext, Base64). GCM also detects tampering. The 256-bit key comes from `AES_SECRET_KEY` (environment/.env) - never
from code, never logged, never returned by an API. API responses show the phone masked (`******3210`).

| | Password | Phone number |
|---|---|---|
| Technique | **BCrypt hashing** (one-way) | **AES encryption** (reversible) |
| Can the original be recovered? | No - only compare hashes | Yes, with the secret key |
| Why | We only need to *verify* a password | We must *show/use* the phone number later |

## Access & Refresh Tokens

| | Access token | Refresh token |
|---|---|---|
| Format | JWT (signed) | random 256-bit opaque string |
| Lifetime | 15 minutes | 7 days |
| Purpose | call the API | get a new access token |
| Stored | nowhere (stateless) | only its **SHA-256 hash** in `refresh_tokens` |

- `POST /api/auth/refresh`: validates the refresh token (exists, not revoked, not expired), revokes it (**rotation**) and returns a new pair.
- If an already-revoked token is presented again (possible theft) all sessions of that user are revoked.
- `POST /api/auth/logout`: revokes the refresh token. The access token simply expires within 15 minutes (stateless JWT trade-off).
- Changing a user's role or deleting a user revokes their refresh tokens. A nightly job deletes expired tokens.

## Swagger

Open **http://localhost:8080/swagger-ui.html**. APIs are grouped (Authentication, Users, Posts, Categories, Roles,
Performance). To test secured endpoints: `POST /api/auth/login` -> copy `data.accessToken` -> click **Authorize** ->
paste the token (without `Bearer`) -> Authorize. Raw OpenAPI JSON: `/v3/api-docs`.

## Testing

```bash
mvn test
```

Tests use an in-memory H2 database and throw-away secrets (profile `test`) - MySQL is not needed.

| Test class | Covers |
|---|---|
| `AuthControllerIntegrationTest` | registration validation, duplicate (409), login, wrong password (401), refresh rotation + reuse detection, logout, missing/garbage token (401), correlation ID |
| `PostControllerIntegrationTest` | post creation, validation errors, forbidden (403), RBAC, pagination, page-size cap, sorting, invalid sort field, projection, 404, ownership rules, caching, benchmark |
| `PostServiceTest` (Mockito) | not-found category, ownership check, admin override, delete |
| `JwtServiceTest` | claims, expired token, wrong key, garbage token |
| `EncryptionServiceTest` | round trip, random IV, tamper detection, key validation |

## How to Run

**Prerequisites:** JDK 17 or 21, Maven 3.9+ (or use the Maven bundled in VS Code/IntelliJ), MySQL 8 running locally.

1. Unzip the project and open the folder in VS Code (install the *Extension Pack for Java* and *Spring Boot Extension Pack*) or IntelliJ.
2. Copy `.env.example` to `.env` and fill in the values:
   - `DB_PASSWORD` = your MySQL root password (the database `secure_cms` is created automatically)
   - `JWT_SECRET`: `openssl rand -base64 48`
   - `AES_SECRET_KEY`: `openssl rand -base64 32`
   - No openssl? Python: `python -c "import secrets,base64;print(base64.b64encode(secrets.token_bytes(32)).decode())"`
     PowerShell: `$b=New-Object byte[] 32;[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b);[Convert]::ToBase64String($b)`
3. Run from the project root folder (the `.env` file is read from the working directory):
   ```bash
   mvn spring-boot:run
   ```
   or run `SecureCmsApplication` from your IDE (working directory = project root).
4. Open Swagger: http://localhost:8080/swagger-ui.html

Demo accounts (**development only**, created when `SEED_ENABLED=true`):

| Username | Password | Role |
|---|---|---|
| admin | Admin@12345 | ADMIN |
| vineet | User@12345 | USER |
| anita | User@12345 | USER |
| rohan | User@12345 | USER |

## Environment Variables

| Variable | Required | Meaning |
|---|---|---|
| `DB_PASSWORD` | yes | MySQL password |
| `JWT_SECRET` | yes | HMAC secret, at least 32 characters |
| `AES_SECRET_KEY` | yes | Base64 encoded 16/24/32-byte AES key |
| `DB_URL` | no | default `jdbc:mysql://localhost:3306/secure_cms?...` |
| `DB_USERNAME` | no | default `root` |
| `CORS_ALLOWED_ORIGINS` | no | default `http://localhost:3000,http://localhost:4200` |
| `SEED_ENABLED` | no | `true` loads demo data when the DB is empty (set `false` in production) |
| `SHOW_SQL` | no | `true` prints every SQL statement |

Variables can come from `.env` or from real OS environment variables. Never commit `.env`.

## Example API Requests

(Linux/macOS/Git Bash. On Windows PowerShell use `curl.exe` or just use Swagger.)

```bash
# 1. Login
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"vineet","password":"User@12345"}'
# -> copy data.accessToken into TOKEN
TOKEN=<paste accessToken here>

# 2. Paginated + sorted posts
curl -s "http://localhost:8080/api/posts?page=0&size=5&sort=title,asc" -H "Authorization: Bearer $TOKEN"

# 3. Create a post (category id 1 exists after seeding)
curl -s -X POST http://localhost:8080/api/posts -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"My first post","content":"Hello","categoryId":1}'

# 4. 403: a USER cannot create categories
curl -i -X POST http://localhost:8080/api/categories -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Nope","description":"x"}'

# 5. Refresh
curl -s -X POST http://localhost:8080/api/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken from login>"}'

# 6. Correlation ID: send your own and look at the response header
curl -i http://localhost:8080/api/posts -H "X-Correlation-ID: my-trace-001"
```

## Common Errors and Fixes

| Error | Cause / fix |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` (or DB_PASSWORD / AES_SECRET_KEY) | `.env` missing or app started from another folder. Create `.env` in the project root and start from there |
| `JWT_SECRET must be at least 32 characters` | use a longer secret (`openssl rand -base64 48`) |
| `AES_SECRET_KEY must be Base64 encoded` / `must decode to 16, 24 or 32 bytes` | generate with `openssl rand -base64 32` |
| `Access denied for user 'root'` / `Communications link failure` | wrong `DB_PASSWORD` or MySQL not running |
| `Port 8080 was already in use` | stop the other app or run with `--server.port=8081` |
| `Unable to find a ... role` / empty login | start with `SEED_ENABLED=true` on an empty DB |
| 401 right after login works | token pasted with the word `Bearer` in Swagger, or token expired (15 min) - login/refresh again |
| Lombok errors in IDE | install the Lombok plugin / enable annotation processing |
| Changed AES key: phone shows `null` | old data was encrypted with the old key; reset the DB or restore the key |

## Future Improvements

Flyway migrations, Redis cache for multi-instance deployments, rate limiting on login, account lockout, email verification,
password reset, refresh-token device list, Testcontainers with real MySQL, Docker Compose, Micrometer/Prometheus metrics.

## Experiment-to-Project Mapping

| Experiment | Concept | Where implemented |
|---|---|---|
| 2.1.1 | REST API | `controller/*` (GET/POST/PUT/PATCH/DELETE, status codes, `ApiResponse`) |
| 2.1.1 | Validation | DTO annotations + `GlobalExceptionHandler.handleValidation` |
| 2.1.2 | Exception handling | `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) + custom exceptions |
| 2.1.2 | Logging | `filter/RequestLoggingFilter`, `logback-spring.xml`, SLF4J `@Slf4j` |
| 2.1.2 | Correlation ID | `filter/CorrelationIdFilter` (MDC + response header) |
| 2.2.1 | Pagination | `Pageable`, `PageResponse`, `PostController.list` |
| 2.2.1 | Sorting | `sort=` param + `util/SortValidator` |
| 2.2.2 | N+1 problem | `PostRepository.findAllWithDetails` (JOIN FETCH) vs `BenchmarkService.naivePostList` |
| 2.2.2 | Caching | `cache/CacheConfig` (Caffeine), `@Cacheable/@CachePut/@CacheEvict` in `CategoryService`, `PostService` |
| 2.2.2 | Query optimisation | JOIN FETCH, `@EntityGraph`, projection, `@Index` on entities |
| 2.3.1 | JWT | `security/JwtService`, `security/JwtAuthenticationFilter` |
| 2.3.1 | RBAC | `@PreAuthorize` in controllers, ownership check in `PostService` |
| 2.3.2 | AES | `encryption/EncryptionService`, `User.encryptedPhone` |
| 2.3.2 | Access token | `JwtService.generateAccessToken` (15 min) |
| 2.3.2 | Refresh token | `AuthService.refresh/logout`, `RefreshTokenService`, `RefreshToken` entity |

See also `docs/VIVA_GUIDE.md` (viva questions + explanation for the teacher).
