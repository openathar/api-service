# api-service

> **Status: scaffold.** README and contributor notes only — no code, no
> endpoint yet. See
> [the architecture doc](https://github.com/openathar/athar/blob/main/docs/architecture.md)
> for why this comes after `athan-core-java`, not before it.

The public REST API for prayer times, Qibla, and Hijri calendar conversion —
free, rate-limited, built for developers to use (similar in spirit to the
Aladhan API, which Athar's own web frontend currently leans on as a
placeholder). Part of the Athar platform (Sadaqah Jariyah) — see
[openathar](https://github.com/openathar).

## Design intent

A thin, Spring Boot wrapper around `athan-core-java` (once that exists),
plus content distribution. Deterministic inputs (lat, lon, date, method)
mean deterministic, cacheable outputs — aggressive HTTP caching and a
Redis-backed rate limiter do most of the heavy lifting, not clever backend
logic.

First and only planned endpoint for V1:

```
GET /v1/prayer-times?lat={lat}&lon={lon}&date={date}&method={method}
```

No GraphQL, no user accounts required for this endpoint.
