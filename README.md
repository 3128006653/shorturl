# shorturl

A short link service built with Spring Boot 3, Spring Data JPA and MySQL.

## Features

- Generate short code from long URL (62-char alphabet, random 6 chars)
- Collision retry: check-then-insert, retry up to 3 times, unique index as final guard
- 302 redirect (not 301) so click stats and target changes stay possible
- Parameter validation for long URL

## Stack

Java 17 / Spring Boot 3 / Spring Data JPA / MySQL 9.7 / Maven

## Run
bash
mvn spring-boot:run
## API

| Method | Path | Description |
|---|---|---|
| POST | /api/shorten?longUrl=xxx | create short code |
| GET  | /{code} | 302 redirect to original URL |
| GET  | /api/expand?code=xxx | get original URL |

## Benchmark

Python multi-thread script `bench.py`, 50 concurrency, 2000 requests:

| Metric | Value |
|---|---|
| QPS | 3127 |
| avg | 11.5 ms |
| p50 | 9.3 ms |
| p95 | 27.3 ms |
| p99 | 41.7 ms |

Raised HikariCP max pool size from 10 to 50, QPS showed no significant
change (2957), which ruled out the connection pool as the bottleneck and
pointed to the per-request SQL round trip. Next step: Redis cache for hot codes.