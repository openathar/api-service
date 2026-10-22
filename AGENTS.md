# AGENTS.md — api-service

Public REST API of the Athar platform (openathar). Spring Boot, hexagonal,
wraps `athan-core-java` (calculation engine) + content distribution.
Rate-limited (Redis token bucket), aggressively cached (results for
lat/lon/date/method are deterministic and valid forever).

## Links

- Architecture/roadmap: `../../AGENTS.md` (superproject `business/athar`)
- Repo conventions: `~/Development/harness/agents/business-repo.md`

## V1 scope

15 endpoints, all under `/v1`:

- Prayer times: `GET /prayer-times`, `GET /prayer-times/range` (from/to, max
  62 days), `GET /prayer-times/next` (current/next prayer; `no-store` without
  `at`), `POST /prayer-times/batch` (max 50 locations), `GET /prayer-times.ics`
  (iCalendar feed), `GET /methods` (catalogue, built from the athan-core enum).
- `GET /qibla`, `GET /hijri`, `GET /calendar/hijri` (month grid),
  `GET /calendar/gregorian` (reverse conversion), `GET /events` (observances
  with countdowns), `GET /moon` (Meeus true new/full moon).
- `GET /places` — city lookup over a bundled GeoNames `cities15000` extract
  (CC BY 4.0, attribution in every response, see
  `src/main/resources/places/README.md`; no external geocoder).
- `POST /api-keys`, `GET /api-keys/{key}/usage`.

Still open: Qur'an/Adhkar content (needs a licence-clean text source — a
product decision, not an implementation detail). No GraphQL in V1.

## Current state

Thin hexagonal wrappers around `athan-core-java` (the single source of truth
for calculation); moon, events, places and the ICS feed live in
`application/` as pure services with unit tests (USNO reference times for the
moon). Spring Boot 4.1.1, Java 25, Lombok, MapStruct, springdoc 3.1.1.
Structure per the `spring-boot-conventions` skill: `domain/`, `port/in/`,
`application/`, `adapter/web/` — no persistence, no Modulith. ArchUnit rules
enforced in tests.

Implemented: Redis rate limiting (fixed window per client IP, fail-open
when Redis is unreachable, configurable via `athar.ratelimit.*`) and HTTP
caching headers (`Cache-Control: public, max-age=31536000, immutable` on
deterministic `/v1/*` responses; answers that mean "now" — `next`, `moon`
without `at`, `events`/`.ics` without `from` — are `no-store` or 1 hour, see
the README).
API keys are implemented (Redis-backed). Not yet implemented: content distribution.
`athan-core` is consumed from Maven Central (`org.openathar:athan-core:0.1.0`).

Build with `mvn test` (Java 25).

## APM (Agent Package Manager)

Project-local skills/agents/commands are managed via `apm.yaml`
(registry source: `~/Development/harness/registry/`).
- `apm install --local` — installs the packages listed in `apm.yaml`
- `apm status --local` — checks install state against the registry
