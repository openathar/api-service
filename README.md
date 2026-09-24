# api-service

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/logo-dark.png" />
    <img src="docs/logo-light.png" alt="Athar — the word أثر" width="360" />
  </picture>
</p>

The public REST API for prayer times, Qibla, and Hijri calendar conversion —
free, rate-limited, built for developers to use. Part of the Athar platform
(Sadaqah Jariyah) — see [openathar](https://github.com/openathar).

## Status

Working V1 (Spring Boot 4.1.1, Java 25, hexagonal): prayer times, Qibla,
and Hijri endpoints, with Redis-backed rate limiting and aggressive HTTP
caching. API keys and content distribution are roadmap items.

## Endpoints

### Prayer times

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

### Qibla

```
GET /v1/qibla?lat={lat}&lon={lon}
```

Returns the Qibla bearing from true north in degrees
(`bearingDegrees`), computed for the Kaaba (21.4225241°N, 39.8261818°E).
Invalid input → `400` with an `error` message.

### Hijri

```
GET /v1/hijri?date={date}&locale={locale}
```

| Param | Required | Default | Notes |
|---|---|---|---|
| `date` | no | today | ISO `yyyy-MM-dd` |
| `locale` | no | `en` | `en` or `ar` — localized month name |

Returns the Hijri date (`day`, `month`, `year`, `monthName`,
`gregorianDate`). Invalid input → `400` with an `error` message.

## Rate limiting & caching

- **Rate limiting:** Redis-backed fixed window per client IP
  (`athar.ratelimit.enabled/limit/window-seconds`, env-overridable via
  `RATELIMIT_*`). Over the limit → `429` with a `Retry-After` header.
  Fails open when Redis is unreachable — the API never takes itself down
  because of the limiter.
- **Caching:** results for a given (lat, lon, date, method) are
  deterministic and valid forever, so all `/v1/*` 2xx/3xx responses carry
  `Cache-Control: public, max-age=31536000, immutable`.

## Quick start

```bash
mvn spring-boot:run
curl "http://localhost:8080/v1/prayer-times?lat=52.52&lon=13.405&date=2026-09-14&method=MWL&utcOffset=2"
curl "http://localhost:8080/v1/qibla?lat=52.52&lon=13.405"
curl "http://localhost:8080/v1/hijri?date=2026-09-14&locale=ar"
```

Swagger UI: `http://localhost:8080/swagger-ui.html` after startup.

## Design intent

A thin, hexagonal Spring Boot wrapper around `athan-core-java` (the single
source of truth for calculation logic — now consumed directly from Maven
Central). Deterministic inputs (lat, lon, date, method) mean
deterministic, cacheable outputs — aggressive HTTP caching and a
Redis-backed rate limiter do the heavy lifting, not clever backend logic.
No GraphQL, no user accounts required for these endpoints.