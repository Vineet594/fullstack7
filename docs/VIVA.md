# Viva Guide

## Experiment → implementation mapping
| Experiment | Concept | Where implemented |
|---|---|---|
| 2.1.1 | REST API | `controller/*` (GET/POST/PUT/PATCH/DELETE, status codes 200/201/204/400/401/403/404/409/500, `ApiResponse`) |
| 2.1.2 | Exception handling | `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) + custom exceptions |
| 2.1.2 | Logging | `filter/RequestLoggingFilter` + SLF4J/Logback pattern in `application.properties` |
| 2.1.2 | Correlation ID | `filter/CorrelationIdFilter` (MDC, `X-Correlation-ID`) |
| 2.2.1 | Pagination | `PostController`/`UserController` + `util/PageableFactory` (`Pageable`, max size 50) |
| 2.2.1 | Sorting | `PageableFactory` (whitelisted fields title, createdAt, updatedAt) |
| 2.2.2 | N+1 problem | `PostRepository.findAllWithDetails` (JOIN FETCH) vs lazy `findAll`; `BenchmarkService` |
| 2.2.2 | Caching | `CategoryService`, `PostService` (`@Cacheable/@CachePut/@CacheEvict`, Caffeine) |
| 2.2.2 | Query optimisation | JOIN FETCH, `@EntityGraph`, indexes in `entity/Post`, `entity/User`, pagination |
| 2.3.1 | JWT | `security/JwtService`, `security/JwtAuthenticationFilter` |
| 2.3.1 | RBAC | `SecurityConfig` + `@PreAuthorize` + ownership check in `PostService` |
| 2.3.2 | AES | `encryption/EncryptionService` (AES-256-GCM) used for `User.encryptedPhone` |
| 2.3.2 | Access token | `JwtService.generateAccessToken` (15 min) |
| 2.3.2 | Refresh token | `RefreshTokenService`, `POST /api/auth/refresh` (rotation, hashed storage) |

## Viva questions and short answers
1. **What is @ControllerAdvice?** A class that handles exceptions for all controllers in one place, so there are no try/catch blocks everywhere.
2. **Why DTOs?** They hide internal fields (password hash), decouple the API from the DB schema, and let us validate input.
3. **What is the N+1 problem?** 1 query for the list plus 1 query per related row (author, category). Fix: `JOIN FETCH` loads everything in one query.
4. **Why does N+1 here equal the distinct authors/categories?** Hibernate re-uses entities already loaded in the same session.
5. **Authentication vs authorization?** Who you are vs what you may do.
6. **BCrypt vs AES?** BCrypt is one-way (passwords, cannot be decrypted). AES is reversible (data we must read back, e.g. phone).
7. **Why GCM?** It encrypts and detects tampering; each value uses a random IV.
8. **Why a short access token plus refresh token?** If an access token leaks it expires in 15 minutes; the refresh token is stored hashed, single-use and revocable.
9. **What is refresh-token rotation?** Each refresh invalidates the old token and issues a new one; reuse of an old one revokes all sessions.
10. **What is a correlation ID?** A unique id per request in every log line and the response header, so one request can be traced across logs.
11. **Cache hit / miss / invalidation?** Hit: served from memory. Miss: loaded from DB then stored. Invalidation: `@CacheEvict` removes stale data after changes.
12. **Why are indexes useful?** They let the DB find rows without scanning the entire table (login by username/email, sort by title/date, joins on foreign keys).
13. **Why limit page size?** To stop clients requesting thousands of rows and overloading the DB.
14. **Why CSRF disabled?** The API is stateless and uses an Authorization header, not cookies.
15. **Why validate sort fields?** Prevents sorting by sensitive/unindexed fields and bad input.

## Common errors and fixes
| Error | Fix |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | Set the env variables or run with `SPRING_PROFILES_ACTIVE=dev` |
| `JWT_SECRET must be at least 32 characters` | Use a longer random secret |
| `Communications link failure` (MySQL) | Start MySQL (`docker compose up -d`), check `DB_HOST/DB_PORT`, add `DB_PARAMS` from `.env.example` |
| `Public Key Retrieval is not allowed` | `DB_PARAMS=?allowPublicKeyRetrieval=true&useSSL=false` |
| CORS error in browser | Add your frontend origin to `CORS_ALLOWED_ORIGINS` (exact scheme + host, no trailing slash) |
| 401 in Swagger | Click *Authorize* and paste only the access token (no "Bearer ") |
| Render first request very slow | Free service was sleeping; wait ~1 minute |
| `Decryption failed` | `AES_SECRET_KEY` changed after data was stored |
| Lombok errors in IDE | Install the Lombok plugin and enable annotation processing |

## Explaining the project to your teacher (1 minute)
"This is one backend that combines all my experiments. Clients call REST controllers; every request first gets a correlation ID and is logged, then the JWT filter authenticates it and roles authorise it. Services hold the business rules and use caching; repositories use JOIN FETCH and indexes to avoid the N+1 problem. Errors from anywhere are converted into one JSON format by a single @ControllerAdvice. Passwords are BCrypt-hashed, phone numbers are AES-GCM encrypted, access tokens last 15 minutes and refresh tokens are hashed, single-use and revocable. You can try everything in Swagger UI, and the Performance tab / benchmark endpoint measures the real SQL count and time before and after optimisation."
