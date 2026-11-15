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

`ProyectoServiceImpl` has a hardcoded demo login (`demo` / `demo123`) marked `TODO: ELIMINAR` — intentional for demos, must be removed before any deployment.

The only test is a `@SpringBootTest` context-load test, so it needs the database reachable.

## Database

PostgreSQL at `jdbc:postgresql://localhost:5432/postgres` (credentials in `src/main/resources/application.properties`). There is no schema/migration file in the repo and no JPA entities: the schema (tables such as `cliente`, `cita`, `agenda_cita`, `estado_cita`, mascota/veterinario tables) lives only in the external database, and table/column names are known only from the SQL strings in the repository classes.

## Architecture

Layered: `controllers` → `services` → `repository`, with every layer split into an interface plus an `Impl/` subpackage class.

- **Controllers**: Routing annotations (`@RequestMapping`, `@GetMapping`, `@Valid`, etc.) are declared on the **interface** (`controllers/*Controller.java`); the `Impl` class only carries `@RestController` and implements the methods. Add new endpoints to the interface first. All routes are under `/integrador/...`. GET endpoints bind query params via `@ModelAttribute` DTOs; POST/PATCH use `@RequestBody`.
- **Response envelope**: Every endpoint returns `ResponseEntity<ResponseGeneric>` (`responseCode`, `responseDesc`, `responseObj`). Codes come from `enums/CodesResponse` (`"0000"` = OK). Errors are caught in the controller and returned with **HTTP 200** and an error code, with the exception message in `responseObj` — the client distinguishes success by `responseCode`, not HTTP status. Follow this pattern and add new codes to `CodesResponse`.
- **Services**: Business validation is done by calling repository checks that return `Optional` and chaining `.orElseThrow(() -> new RuntimeException("..."))`; the exception message becomes the client-facing error.
- **Repositories**: Plain `JdbcTemplate` with SQL text blocks and inline lambda row mappers (or `utils/ClienteRowMapper`). Methods return `Optional<...>`, catch exceptions internally, log them, and return `Optional.empty()` — so a DB error looks the same as "not found" to the service.
- **Models**: Lombok `@Data` POJOs/DTOs in `models/` (request DTOs are named `Data*DTO`); selector (dropdown) models in `models/selectors/`.
- **Dependency injection**: constructor injection via Lombok `@RequiredArgsConstructor` with `final` fields; logging via `@Slf4j`.

### Real-time notifications (SSE)

`GET /integrador/notifications/connection?client_id=...` opens a Server-Sent Events stream (`NotificationServiceimpl`). Emitters are kept in an in-memory `ConcurrentHashMap` keyed by client id (no timeout, single-instance only). On connect it sends a `conectado` event; `AgendaServiceImpl.scheduleAnAppointment` calls `notifyAppointmentCreation`, which sends a `cita-creada` event with a `DetailAppointment` payload to that client.

### Conventions worth knowing

- Appointment dates/times arrive as strings in `yyyyMMdd` and `HHmmss` formats and are parsed in the service.
