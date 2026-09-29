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
and Hijri endpoints, with Redis-backed rate limiting, aggressive HTTP
caching, and free self-serve API keys for a higher rate limit. Content
distribution is the remaining roadmap item.

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
`{"error": "..."}` body (same shape for every endpoint, including missing
or malformed parameters).

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

### API keys

```
POST /v1/api-keys                     — issue a free key
GET  /v1/api-keys/{apiKey}/usage      — look up label/created-at/request count
```

No email, no account: `POST` with an optional `{"label": "..."}` body
returns `{"apiKey", "label", "createdAt"}` — **the key is shown only in
this response**, store it yourself. Send it back as the `X-API-Key` header
on any `/v1/*` request for a higher rate limit than anonymous requests get
(see below). An unrecognized key is treated as anonymous, not rejected —
Athar never 401s a free API over a typo'd header.

## Rate limiting & caching

- **Rate limiting:** Redis-backed fixed window, keyed by client IP for
  anonymous requests or by API key (`X-API-Key` header) for a higher quota
  (`athar.ratelimit.enabled/limit/keyed-limit/window-seconds`,
  env-overridable via `RATELIMIT_*`; defaults: 60/min anonymous, 600/min
  keyed). Over the limit → `429` with a `Retry-After` header. Fails open
  when Redis is unreachable — the API never takes itself down because of
  the limiter.
- **Caching:** results for a given (lat, lon, date, method) are
  deterministic and valid forever, so all `/v1/*` 2xx/3xx responses carry
  `Cache-Control: public, max-age=31536000, immutable`.

## Quick start

```bash
mvn spring-boot:run
curl "http://localhost:8080/v1/prayer-times?lat=52.52&lon=13.405&date=2026-09-14&method=MWL&utcOffset=2"
curl "http://localhost:8080/v1/qibla?lat=52.52&lon=13.405"
curl "http://localhost:8080/v1/hijri?date=2026-09-14&locale=ar"

# Optional: issue a free API key for a higher rate limit
KEY=$(curl -s -X POST http://localhost:8080/v1/api-keys -d '{"label":"my-app"}' | jq -r .apiKey)
curl -H "X-API-Key: $KEY" "http://localhost:8080/v1/qibla?lat=52.52&lon=13.405"
curl "http://localhost:8080/v1/api-keys/$KEY/usage"
```

Interactive API docs (Swagger UI, branded with description/examples/error
schema): `http://localhost:8080/swagger-ui.html` after startup, or
`https://api.openathar.org/swagger-ui.html` in production. Raw OpenAPI 3.1
spec: `/v3/api-docs`.

## Design intent

A thin, hexagonal Spring Boot wrapper around `athan-core-java` (the single
source of truth for calculation logic — now consumed directly from Maven
Central). Deterministic inputs (lat, lon, date, method) mean
deterministic, cacheable outputs — aggressive HTTP caching and a
Redis-backed rate limiter do the heavy lifting, not clever backend logic.
No GraphQL, no mandatory user accounts — API keys are free-form tokens for
rate-limit tiering, not an identity system.

API keys are Redis-backed with no TTL — the pragmatic V1 choice given
Redis was already the only stateful dependency in this service. Once
Postgres (via CNPG) lands for user-sync data (Khatma/Tasbeeh), API keys
are the first candidate to move over for durability guarantees Redis
doesn't give (e.g. surviving a full data wipe, not just a restart).