# AGENTS.md — api-service

Public REST API der Athar-Plattform (openathar). Spring Boot, Hexagonal,
wrappt `athan-core-java` (Calculation Engine) + Content-Distribution.
Rate-limitiert (Redis Token-Bucket), aggressiv gecacht (Ergebnisse für
lat/lon/date/method sind deterministisch und ewig gültig).

## Verknuepfungen
- Architektur/Roadmap: `../../AGENTS.md` (Superproject `business/athar`)
- Repo-Regeln: `~/Development/harness/agents/business-repo.md`

## Sprint-1-Scope (MVP)
Nur `GET /v1/prayer-times?lat&lon&date&method`. Kein GraphQL in V1.
