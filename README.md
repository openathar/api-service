# api-service

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/logo-dark.png" />
    <img src="docs/logo-light.png" alt="Athar — the word أثر" width="360" />
  </picture>
</p>

The public REST API for prayer times, Qibla, the Hijri calendar, Islamic
observances, the moon and city lookup — free, rate-limited, built for
developers to use. Part of the Athar platform
(Sadaqah Jariyah) — see [openathar](https://github.com/openathar).

## Status

Working V1 (Spring Boot 4.1.1, Java 25, hexagonal) with 15 endpoints: prayer
times (single day, date range, batch, current/next prayer, iCalendar feed),
the calculation-method catalogue, Qibla, Hijri conversion in both directions
plus month grids, Islamic observances with countdowns, moon phase, and city
lookup — with Redis-backed rate limiting, aggressive HTTP caching, and free
self-serve API keys for a higher rate limit. The one remaining roadmap item
is Qur'an and Adhkar content, which waits on a licence-clean text source.

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
window (`duhaStart`, `duhaEnd`, `duhaBest`). At high latitudes, when the sun
does not sink far enough for Fajr/Isha, both are placed by the
middle-of-the-night rule. Where an event does not occur at all (polar day or
night) its field is `null` — never a made-up time; `dhuhr` is always present.
Invalid input → `400` with an `{"error": "..."}` body (see *Errors*).

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
| `locale` | no | `en` | `en`, `ar` or `de` (same transliteration as `en`) — localized month name |

Returns the Hijri (Umm al-Qura) date (`day`, `month`, `year`, `monthName`,
`gregorianDate`). Without `date` it converts today (UTC). Supported range is
the JDK's Umm al-Qura table, 1882-11-12 to 2174-11-25; dates outside →
`400`. Invalid input → `400` with an `error` message.

### Prayer times: ranges, batches, the next prayer, a calendar feed

```
GET  /v1/prayer-times/range?lat&lon&from&to&method&utcOffset
GET  /v1/prayer-times/next?lat&lon&method&utcOffset&at
POST /v1/prayer-times/batch
GET  /v1/prayer-times.ics?lat&lon&from&days&method&utcOffset&lang&sunrise
GET  /v1/methods
```

- **`range`** — every day from `from` to `to` (inclusive, at most **62 days**) in
  one call; each entry has exactly the shape of the single-day response.
  A longer range or `to` before `from` → `400`.
- **`next`** — which prayer is current, which is next, and `secondsUntilNext`.
  Rolls over midnight (before Fajr the current prayer is yesterday's Isha,
  after Isha the next one is tomorrow's Fajr) and skips prayers that do not
  occur at the location. `at` (ISO-8601 instant, default now) answers for any
  moment.
- **`batch`** — body `{"date", "method"?, "locations": [{"label"?, "lat", "lon", "utcOffset"?}]}`,
  at most **50 locations**; results come back in request order with the
  label you sent. One invalid location rejects the whole call and the message
  names its index (`locations[3]: …`).
- **`prayer-times.ics`** — an iCalendar feed (`text/calendar`) to subscribe to
  from Apple Calendar, Google Calendar or Outlook: one short event per prayer
  (plus sunrise with `sunrise=true`), `days` days (default 30, max 62) from
  `from` (default today). Times are UTC, so they are right on every device.
  `lang` is `en`, `de` or `ar`.
- **`methods`** — the catalogue behind the `method` parameter: id, authority
  name, Fajr angle, Isha and Maghrib rule (angle or minutes), midnight rule.
  Generated from the `athan-core` enum, so it cannot drift from what is
  calculated.

### Hijri calendar and observances

```
GET /v1/calendar/hijri?year&month&locale       — one Hijri month, day by day
GET /v1/calendar/gregorian?year&month&day&locale — Hijri → Gregorian
GET /v1/events?from&count&locale               — upcoming observances with countdowns
```

`calendar/hijri` returns the month with the Gregorian date and weekday of
every day; `calendar/gregorian` is the reverse of `/v1/hijri` (same response
shape). Both cover 1300–1600 AH, the span of the Umm al-Qura table. `events`
lists the next `count` (1–20) of: Islamic New Year, Ashura, Mawlid, start of
Ramadan, Laylat al-Qadr, Eid al-Fitr and Eid al-Adha, with Hijri and
Gregorian date and `daysUntil`. These are calendar facts — a local moon
sighting can differ by a day. Mawlid is a date, not a call to observe it, and
the night of Laylat al-Qadr is stated as unknown (commonly the 27th).

### Moon

```
GET /v1/moon?at={instant}
```

Phase name, illuminated fraction, age in days, and the previous new moon,
next new moon and next full moon as UTC instants. Computed from the true
times of new and full moon (Meeus, *Astronomical Algorithms*, ch. 49 — good
to a couple of minutes; tests check USNO reference times). Astronomy, not a
moon sighting: a Hijri month begins with the observed crescent.

### Places

```
GET /v1/places?q={name}&limit={1..25}&date={date}
```

Resolves a city name (Latin or Arabic script, accent-insensitive; at least 2
characters) to latitude, longitude, IANA time zone and the **UTC offset on
`date`, daylight saving included** — exactly the inputs the prayer-time
endpoints need. Best match first (exact, prefix, contained; larger cities
first). Runs on a bundled extract of the GeoNames `cities15000` dump (cities
above 15,000 people), so no query is sent to a third-party geocoder. **Data:
[GeoNames](https://www.geonames.org/), CC BY 4.0** — every response names it
in `source`; see `src/main/resources/places/README.md`.

### API keys

```
POST /v1/api-keys                     — issue a free key
GET  /v1/api-keys/{apiKey}/usage      — look up label/created-at/request count
```

No email, no account: `POST` with an optional `{"label": "..."}` body
(max 64 characters, defaults to `unlabeled`) returns `{"apiKey", "label", "createdAt"}` — **the key is shown only in
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
- **Caching:** calculation results for a given input are deterministic and
  valid forever, so successful calculation responses carry
  `Cache-Control: public, max-age=31536000, immutable`. Exceptions, because
  their answer depends on the moment: `/v1/hijri` without a `date` ("today",
  5 minutes), `/v1/events` and `/v1/prayer-times.ics` without `from` (1 hour),
  `/v1/prayer-times/next` and `/v1/moon` without `at` (`no-store`).
  `/v1/methods` and `/v1/places` are cached for a day. The batch and the
  API-key endpoints (POST) are not cached. Cached responses never reach the API, so
  they count neither against the rate limit nor towards a key's usage.

## Errors

Every error on every endpoint has the same shape, `{"error": "..."}` —
missing or malformed parameters, out-of-range values, unknown methods,
unknown paths (`404`), wrong HTTP methods (`405`), wrong content types
(`415`), malformed JSON, rate limiting (`429`) and unexpected failures
(`500`, message deliberately generic). The one exception is a CORS
rejection (`403`), which is written before any controller runs; browsers
don't expose its body to scripts anyway.

## Quick start

```bash
mvn spring-boot:run
curl "http://localhost:8080/v1/prayer-times?lat=52.52&lon=13.405&date=2026-09-14&method=MWL&utcOffset=2"
curl "http://localhost:8080/v1/qibla?lat=52.52&lon=13.405"
curl "http://localhost:8080/v1/hijri?date=2026-09-14&locale=ar"
curl "http://localhost:8080/v1/prayer-times/range?lat=52.52&lon=13.405&from=2026-09-14&to=2026-09-20&utcOffset=2"
curl "http://localhost:8080/v1/prayer-times/next?lat=52.52&lon=13.405&utcOffset=2"
curl "http://localhost:8080/v1/methods"
curl "http://localhost:8080/v1/moon"
curl "http://localhost:8080/v1/events?count=3&locale=de"
curl "http://localhost:8080/v1/calendar/hijri?year=1448&month=4"
curl "http://localhost:8080/v1/places?q=berlin"
curl -o prayer.ics "http://localhost:8080/v1/prayer-times.ics?lat=52.52&lon=13.405&utcOffset=2"
curl -X POST http://localhost:8080/v1/prayer-times/batch -H "Content-Type: application/json" \
  -d '{"date":"2026-09-14","locations":[{"label":"berlin","lat":52.52,"lon":13.405,"utcOffset":2}]}'

# Optional: issue a free API key for a higher rate limit
KEY=$(curl -s -X POST http://localhost:8080/v1/api-keys \
  -H "Content-Type: application/json" -d '{"label":"my-app"}' | jq -r .apiKey)
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
City lookup uses a bundled dataset rather than an external geocoder, so a
query never leaves the server. No GraphQL, no mandatory user accounts — API keys are free-form tokens for
rate-limit tiering, not an identity system.

API keys are Redis-backed with no TTL — the pragmatic V1 choice given
Redis was already the only stateful dependency in this service. Once
Postgres (via CNPG) lands for user-sync data (Khatma/Tasbeeh), API keys
are the first candidate to move over for durability guarantees Redis
doesn't give (e.g. surviving a full data wipe, not just a restart).