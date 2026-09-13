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

Scaffold only — no code, no endpoint yet, and it depends on
`athan-core-java` existing first (also currently a scaffold). See the
superproject's `docs/architecture.md` for the build order and reasoning.
