# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 3.3.5 (Java 17) REST backend for a veterinary appointment app ("Proyecto integrador del 311 de tecnología en sistemas"). Domain language, SQL, logs and error messages are in Spanish; method/endpoint names are a mix of English and Spanish.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` in Git Bash):

- Build: `./mvnw clean package` (add `-DskipTests` to skip tests)
- Run: `./mvnw spring-boot:run` (serves on port 8080)
- All tests: `./mvnw test`
- Single test: `./mvnw test -Dtest=ProyectoIntegradorApplicationTests` (or `-Dtest=Class#method`)

The local machine runs JDK 23, where javac no longer runs annotation processors by default; `pom.xml` sets `maven.compiler.proc=full` so Lombok still works. Don't remove it — without it the build fails with hundreds of "cannot find symbol" errors (`log`, getters/setters).

### Security (package `security/`)

- Stateless JWT auth (JJWT, HMAC-SHA256). `POST /integrador/login` returns `token` and `expiresIn` at the top level of `ResponseLogin` (next to `responseObj`). Every other endpoint except `login`/`register` requires `Authorization: Bearer <token>`; the SSE endpoint also accepts `?token=` because browser `EventSource` can't send headers.
- Endpoints still receive client ids in their DTOs, but controllers call `SecurityUtils.requireSameClient(...)` (and `requireSameUsername`) *before* their `try/catch` so the `AccessDeniedException` reaches `GlobalExceptionHandler` (403, code `6619`). Do the same in new endpoints that take a client id. Missing/invalid token → 401, code `6618` (written by `SecurityConfig`). Unlike other errors these use real HTTP status codes.
- Config: `jwt.secret` / `jwt.expiration-minutes` / `app.cors.allowed-origins` in `application.properties`, overridable by env vars `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `CORS_ALLOWED_ORIGINS`.
- `@WebMvcTest` tests must `@Import({SecurityConfig.class, JwtService.class})` and send a token from `jwtService.generateToken(...)`, otherwise Boot's default security applies.
- Roles (`security/Role`: ADMIN, VETERINARIO, CLIENTE) are resolved from `rol.descripcion` text (contains ADMIN / VETERIN / MEDIC), never by hardcoded `rol_id`; unknown or null → CLIENTE. The role is a `role` claim in the JWT, a `ROLE_*` authority, and `InfoUser.role` in the login response. `requireSameClient`/`requireSameUsername` let ADMIN and VETERINARIO act on any client; `requireOwnClient` is strict (used for the SSE channel). `@EnableMethodSecurity` is on, so admin-only endpoints use `@PreAuthorize("hasRole('ADMIN')")` (see `VeterinarianController`). A controller interface with `@PreAuthorize` must also carry `@RestController` (as all controller interfaces here do), otherwise the security proxy isn't detected as a handler and every route 404s.
- Veterinarians (VET-01): a vet is a `cliente` row (login, role VETERINARIO) + `personal` (linked via `personal.cliente_id`) + `medico` (`identificacion_medico` = `personal_id`); `medicoId` is what appointments use. Inactive staff (`personal.activo = false`) can't log in, aren't offered in the selector and can't be booked.
- Public `/register` always assigns the CLIENTE role (looked up with `getRoleId(Role.CLIENTE)`); staff accounts are created directly in the DB for now.

Passwords are hashed with BCrypt (`config/PasswordConfig`). Legacy plaintext passwords in `user_credentials.password_hash` are re-hashed on the user's next successful login (`ProyectoServiceImpl.passwordMatches`).

`ProyectoServiceImpl` has a hardcoded demo login (`demo` / `demo123`) marked `TODO: ELIMINAR` — intentional for demos, must be removed before any deployment.

`ProyectoIntegradorApplicationTests` is a `@SpringBootTest` context-load test and needs the database reachable (the app also won't start without it). The other tests (`@WebMvcTest` controller tests and Mockito service tests) run without a DB: `./mvnw test -Dtest='*ControllerImplTest,*ServiceImplTest'`.

## Database

PostgreSQL at `jdbc:postgresql://localhost:5432/postgres` (credentials in `src/main/resources/application.properties`). There is no full schema file and no JPA entities. Schema *changes* go in `sql/` (not `db/`, which is gitignored for local DB data) as hand-run SQL scripts named after the Jira issue (e.g. `sql/VET-01_gestion_veterinarios.sql`, which adds `personal.activo` and `personal.cliente_id`); code that depends on them requires the script to have been run. The ER diagram is Figura 2 of the thesis document. The rest of the schema (tables such as `cliente`, `cita`, `agenda_cita`, `estado_cita`, mascota/veterinario tables) lives only in the external database, and table/column names are known only from the SQL strings in the repository classes.

## Architecture

Layered: `controllers` → `services` → `repository`, with every layer split into an interface plus an `Impl/` subpackage class.

- **Controllers**: Routing annotations (`@RequestMapping`, `@GetMapping`, `@Valid`, etc.) are declared on the **interface** (`controllers/*Controller.java`); the `Impl` class only carries `@RestController` and implements the methods. Add new endpoints to the interface first. All routes are under `/integrador/...`. GET endpoints bind query params via `@ModelAttribute` DTOs; POST/PATCH use `@RequestBody`.
- **Response envelope**: Every endpoint returns `ResponseEntity<ResponseGeneric>` (`responseCode`, `responseDesc`, `responseObj`). Codes come from `enums/CodesResponse` (`"0000"` = OK). Errors are caught in the controller and returned with **HTTP 200** and an error code, with `ErrorMessages.clientMessage(e)` in `responseObj` — the client distinguishes success by `responseCode`, not HTTP status. Never put `e.getMessage()` in a response (RNF-05): only `BusinessException` messages reach the client; any other exception is logged and replaced by a generic message. So in services throw `BusinessException` for user-facing messages ("No hay disponibilidad de citas") and plain `RuntimeException` for internal failures. Uncaught exceptions are handled by the catch-all in `GlobalExceptionHandler` (code `6620`, keeps Spring's 4xx statuses, 500 otherwise). Follow this pattern and add new codes to `CodesResponse`. Bean Validation failures (`@Valid` on `@RequestBody`/`@ModelAttribute`, malformed JSON) happen before the controller and are turned into the same envelope with code `6617` by `exceptions/GlobalExceptionHandler`. In DTOs use `@NotBlank` only on `String`; numeric fields are `Integer` with `@NotNull`/`@Positive` (a primitive `int` defaults to 0 and `@NotBlank` on it throws at runtime).
- **Services**: Business validation is done by calling repository checks that return `Optional` and chaining `.orElseThrow(() -> new BusinessException("..."))`; that message becomes the client-facing error. Beware the `Optional<Boolean>` trap: `Optional.of(false)` does not trigger `orElseThrow` — return `Optional.empty()` for "not valid".
- **Repositories**: Plain `JdbcTemplate` with SQL text blocks and inline lambda row mappers (or `utils/ClienteRowMapper`). Methods return `Optional<...>`, catch exceptions internally, log them, and return `Optional.empty()` — so a DB error looks the same as "not found" to the service.
- **Models**: Lombok `@Data` POJOs/DTOs in `models/` (request DTOs are named `Data*DTO`); selector (dropdown) models in `models/selectors/`.
- **Dependency injection**: constructor injection via Lombok `@RequiredArgsConstructor` with `final` fields; logging via `@Slf4j`.

### Real-time notifications (SSE)

`GET /integrador/notifications/connection?client_id=...` opens a Server-Sent Events stream (`NotificationServiceimpl`). Emitters are kept in an in-memory `ConcurrentHashMap` keyed by client id (no timeout, single-instance only). On connect it sends a `conectado` event; `AgendaServiceImpl.scheduleAnAppointment` calls `notifyAppointmentCreation`, which sends a `cita-creada` event with a `DetailAppointment` payload to that client.

### Conventions worth knowing

- Appointment dates/times arrive as strings in `yyyyMMdd` and `HHmmss` formats and are parsed in the service.
