# api-service

The public REST API for prayer times, Qibla, and Hijri calendar conversion —
free, rate-limited, built for developers to use. Part of the Athar platform
(Sadaqah Jariyah) — see [openathar](https://github.com/openathar).

## Status

Working V1 endpoint (Spring Boot 4.1.1, Java 25, hexagonal). Rate limiting,
caching headers, API keys, and the Qibla/Hijri endpoints are roadmap items.

## Endpoint

```
GET /v1/prayer-times?lat={lat}&lon={lon}&date={date}&method={method}&utcOffset={hours}
```

| Param | Required | Default | Notes |
|---|---|---|---|
| `lat` | yes | — | −90..90 |
| `lon` | yes | — | −180..180 |
| `date` | yes | — | ISO `yyyy-MM-dd` |
| `method` | no | `MWL` | `MWL`, `ISNA`, `EGYPT`, `MAKKAH`, `KARACHI`, `TEHRAN`, `JAFARI`, `FRANCE`, `RUSSIA`, `MALAYSIA`, `SINGAPORE` |
| `utcOffset` | no | `0` | Location's UTC offset in hours (−12..14), used to render local wall-clock times |

Returns all prayer times as local `HH:mm` strings, including the Duha
window (`duhaStart`, `duhaEnd`, `duhaBest`). Invalid input → `400` with an
`error` message.

## Quick start

```bash
# athan-core is not on Maven Central yet — install locally first
(cd ../core && mvn install -DskipTests)
mvn spring-boot:run
curl "http://localhost:8080/v1/prayer-times?lat=52.52&lon=13.405&date=2026-09-14&method=MWL&utcOffset=2"
```

Swagger UI: `http://localhost:8080/swagger-ui.html` after startup.

## Design intent

A thin, hexagonal Spring Boot wrapper around `athan-core-java` (the single
source of truth for calculation logic — see `docs/decisions/ADR-001` in
the superproject). Deterministic inputs (lat, lon, date, method) mean
deterministic, cacheable outputs — aggressive HTTP caching and a
Redis-backed rate limiter are planned to do the heavy lifting, not clever
backend logic. No GraphQL, no user accounts required for this endpoint.