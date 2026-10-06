# Viva Guide

## 1. One-minute explanation for the teacher

"I built ONE Spring Boot application, a *Secure Content Management & User Management System*, instead of separate
demos. Users register and log in; the server returns a short-lived JWT access token and a long-lived refresh token.
A JWT filter authenticates every request, and `@PreAuthorize` decides what each role (ADMIN or USER) may do.
Posts are served with pagination and sorting, loaded with JOIN FETCH to avoid the N+1 problem, and frequently read
data is cached with Caffeine. Passwords are BCrypt-hashed, phone numbers are AES-GCM encrypted. All errors go through
one `@RestControllerAdvice`, every request is logged with a correlation ID, and everything can be tested in Swagger UI."

## 2. Request flow (explain with a diagram)

1. Request arrives -> `CorrelationIdFilter` (assign ID, put in MDC) -> `RequestLoggingFilter` (start timer)
2. Spring Security -> CORS -> `JwtAuthenticationFilter` (validate token, set user)
3. Controller (`@Valid` checks DTO, `@PreAuthorize` checks role)
4. Service (business rules, ownership, cache)
5. Repository -> MySQL
6. Response wrapped in `ApiResponse`; errors converted by `GlobalExceptionHandler`
7. `RequestLoggingFilter` logs `METHOD URL - user - status - time`

## 3. Viva questions and simple answers

**Q1. What is REST?** An architectural style where resources (users, posts) have URLs and are manipulated with standard HTTP methods: GET reads, POST creates, PUT replaces/updates, PATCH partially updates, DELETE removes. The server is stateless.

**Q2. Which status codes do you use?** 200 OK, 201 Created, 204 No Content (delete/logout), 400 validation errors, 401 not authenticated, 403 not allowed, 404 not found, 409 duplicate/conflict, 500 unexpected error.

**Q3. Why DTOs instead of entities?** To avoid leaking fields (password hash), to avoid lazy-loading/JSON recursion problems, to decouple the API from the database, and to validate input separately.

**Q4. How does `@ControllerAdvice` work?** Spring intercepts exceptions thrown by controllers/services and calls the matching `@ExceptionHandler` method in the advice class. One class builds all error responses, so there is no try-catch repetition and the JSON format is consistent. `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`.

**Q5. What is a correlation ID and why is it useful?** A unique ID per request, stored in the MDC so every log line has it and returned in the `X-Correlation-ID` header. It lets us find all logs of one request, which is essential for debugging concurrent requests and distributed systems.

**Q6. What is MDC?** Mapped Diagnostic Context: a thread-local map in SLF4J/Logback. Values put there can be printed in the log pattern with `%X{correlationId}`.

**Q7. What is the difference between filter and interceptor?** A filter belongs to the servlet container and runs before Spring MVC (can handle security and every request); an interceptor belongs to Spring MVC and runs around controller calls. I used filters because they also see security errors.

**Q8. How does pagination work?** `Pageable` (page, size, sort) is translated to SQL `LIMIT/OFFSET`; Spring returns a `Page` with content and metadata (totalElements, totalPages, first, last). I cap size at 50 so nobody can request thousands of rows.

**Q9. How do you make sorting safe?** I whitelist sortable fields (`title`, `createdAt`, `updatedAt`); anything else returns 400.

**Q10. What is the N+1 problem?** One query loads N posts, then lazy loading runs one extra query per author/category = N+1 queries. Fix: `JOIN FETCH` (or `@EntityGraph`) loads posts with their author and category in a single query.

**Q11. Why JOIN FETCH with pagination needs `countQuery`?** Spring Data cannot derive a count query from a fetch join, so I provide it explicitly.

**Q12. Explain cache hit, miss and invalidation.** Hit: data found in cache, no DB call. Miss: not cached, DB is queried and the result stored. Invalidation: removing/updating cached data when the source changes (`@CacheEvict`, `@CachePut`) so users never see stale data.

**Q13. `@Cacheable` vs `@CachePut` vs `@CacheEvict`?** `@Cacheable` returns the cached value if present, otherwise runs the method and caches the result. `@CachePut` always runs the method and updates the cache. `@CacheEvict` removes entries.

**Q14. Why Caffeine?** Fast in-memory cache, works locally without installing Redis. Limitation: not shared between several server instances.

**Q15. Which indexes did you add and why?** username/email (login, duplicate checks), post title (sorting/search), category_id and author_id (joins/filters), created_at (default sorting), refresh token hash (lookup).

**Q16. What is a JWT?** A signed token with three Base64 parts: header, payload (claims: username, userId, role, expiry) and signature. The server verifies the signature with its secret, so it needs no session storage. The payload is readable (not encrypted) - never put secrets in it.

**Q17. Authentication vs authorization?** Authentication = who are you (login/JWT). Authorization = what may you do (roles, `@PreAuthorize`).

**Q18. Why access AND refresh tokens?** The access token is short-lived (15 min) so a stolen one is dangerous only briefly. The refresh token lives longer and is used only to get new access tokens, so the user does not have to log in every 15 minutes.

**Q19. How is your refresh token system safe?** Random 256-bit token, only its SHA-256 hash is stored, rotation (each token works once), reuse detection revokes all sessions, logout revokes, expired tokens are cleaned up.

**Q20. Why does the access token still work after logout?** JWTs are stateless; it expires after at most 15 minutes. That is the trade-off for stateless authentication (a deny-list could be added).

**Q21. BCrypt vs AES?** BCrypt is one-way hashing with salt: we only verify passwords, never recover them. AES is reversible encryption with a key: used for data we must read again (phone number).

**Q22. Why AES/GCM?** GCM provides both confidentiality and integrity (tamper detection). A new random IV per encryption means identical inputs give different ciphertexts.

**Q23. Where are secrets stored?** In environment variables / `.env` (ignored by git), read via `${...}` placeholders. Nothing secret is hard-coded or logged.

**Q24. How does RBAC work in your code?** The JWT role becomes the authority `ROLE_ADMIN` / `ROLE_USER`; `@PreAuthorize("hasRole('ADMIN')")` guards methods. Ownership ("USER may edit only own posts") is checked in `PostService`.

**Q25. Why is CSRF disabled?** The API is stateless and uses an Authorization header, not cookies, so browsers do not send credentials automatically - CSRF attacks do not apply.

**Q26. How is CORS configured?** Only the origins in `CORS_ALLOWED_ORIGINS` (default localhost:3000/4200) may call the API from a browser; no wildcard.

**Q27. Why `spring.jpa.open-in-view=false`?** It stops lazy loading from happening silently in the controller/view layer, so hidden N+1 problems cannot hide.

**Q28. How did you test?** JUnit 5 + Mockito for services, Spring Boot Test + MockMvc for end-to-end API behaviour with an H2 database.

## 4. What to demo in the viva (5 minutes)

1. Swagger -> login as `vineet` -> Authorize.
2. `GET /api/posts?page=0&size=5&sort=title,asc` (pagination + sorting) -> try `size=1000` (capped at 50) and `sort=content,asc` (400).
3. `POST /api/categories` as vineet -> **403**; login as `admin` and repeat -> **201**.
4. `GET /api/posts/{id}` twice, watch the console: `Cache MISS` only the first time.
5. Admin: `GET /api/admin/benchmark/posts` -> show real SQL statement counts for N+1 vs JOIN FETCH vs cache.
6. Show the console log line with `correlationId=...` and the `X-Correlation-ID` response header.
7. `POST /api/auth/refresh` then reuse the old refresh token -> 401.
8. Open the `users` table in MySQL: `password` is a BCrypt hash, `encrypted_phone` is Base64 ciphertext.
