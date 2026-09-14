# AGENTS.md — api-service

Public REST API of the Athar platform (openathar). Spring Boot, hexagonal,
wraps `athan-core-java` (calculation engine) + content distribution.
Rate-limited (Redis token bucket), aggressively cached (results for
lat/lon/date/method are deterministic and valid forever).

## Links

- Architecture/roadmap: `../../AGENTS.md` (superproject `business/athar`)
- Repo conventions: `~/Development/harness/agents/business-repo.md`

## V1 scope

Only `GET /v1/prayer-times?lat&lon&date&method`. No GraphQL in V1.

## Current state

Working V1 endpoint: `GET /v1/prayer-times?lat&lon&date&method&utcOffset`,
a thin hexagonal wrapper around `athan-core-java` (the single source of
truth for calculation). Spring Boot 4.1.1, Java 25, Lombok, MapStruct,
springdoc 3.1.1. Structure per the `spring-boot-conventions` skill:
`domain/`, `port/in/`, `application/`, `adapter/web/` — no persistence, no
Modulith. ArchUnit rules enforced in tests.

Not yet implemented (roadmap, not scaffold): Redis rate limiting, HTTP
caching headers, API keys, content distribution, Qibla/Hijri endpoints.
`athan-core` must be installed locally first (`mvn install` in `core/`),
since it is not yet on Maven Central.

Build with `mvn test` (Java 25).

## APM (Agent Package Manager)

Projekt-lokale Skills/Agents/Commands werden über `apm.yaml` verwaltet
(Registry-Quelle: `~/Development/harness/registry/`).
- `apm install --local` — installiert die in `apm.yaml` gelisteten Packages
- `apm status --local` — prüft Installations-Stand gegen die Registry
