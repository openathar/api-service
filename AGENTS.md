# AGENTS.md — api-service

Public REST API of the Athar platform (openathar). Spring Boot, hexagonal,
wraps `athan-core-java` (calculation engine) + content distribution.
Rate-limited (Redis token bucket), aggressively cached (results for
lat/lon/date/method are deterministic and valid forever).

## Links

- Architecture/roadmap: `../../AGENTS.md` (superproject `business/athar`)
- Repo conventions: `~/Development/harness/agents/business-repo.md`

## V1 scope

`GET /v1/prayer-times?lat&lon&date&method&utcOffset`,
`GET /v1/qibla?lat&lon` and `GET /v1/hijri?date&locale`. No GraphQL in V1.

## Current state

Working V1 endpoints: prayer times and Qibla, thin hexagonal wrappers
around `athan-core-java` (the single source of truth for calculation).
Spring Boot 4.1.1, Java 25, Lombok, MapStruct, springdoc 3.1.1. Structure
per the `spring-boot-conventions` skill: `domain/`, `port/in/`,
`application/`, `adapter/web/` — no persistence, no Modulith. ArchUnit
rules enforced in tests.

Implemented: Redis rate limiting (fixed window per client IP, fail-open
when Redis is unreachable, configurable via `athar.ratelimit.*`) and HTTP
caching headers (`Cache-Control: public, max-age=31536000, immutable` on
`/v1/*` 2xx/3xx responses — results are deterministic per lat/lon/date).
Not yet implemented (roadmap): API keys, content distribution.
`athan-core` is consumed from Maven Central (`org.openathar:athan-core:0.1.0`).

Build with `mvn test` (Java 25).

## APM (Agent Package Manager)

Project-local skills/agents/commands are managed via `apm.yaml`
(registry source: `~/Development/harness/registry/`).
- `apm install --local` — installs the packages listed in `apm.yaml`
- `apm status --local` — checks install state against the registry
