# CLAUDE.md — remittance-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P9 (roadmap #11). **Waits on:** `shared-lib` `Money`; `transfer-service` saga on `master` (roadmap #5).
Until P9 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/remittance-service.md` (if it exists) → `briefs/ROADMAP.md` (row #11).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `remittance-service/` only. Branch: `feat/remittance-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (ask first), `RemittanceServiceApplication`, Liquibase master changelog, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.remittance` |
| Port | 8085 — **clashes with Schema Registry's host port** (see open decisions) |
| Database | PostgreSQL 16, `remittance_db` — compose block `postgres-remittance`, host port 5434 |
| Cache | Redis 7 for quote locks (in README and older architecture; not in root `CLAUDE.md` §3 — confirm) |
| Migrations | Liquibase |

---

## 3. Responsibility

- **DB-driven corridor routing:** which provider handles a corridor (e.g. EUR→EGP) is data in tables (rules, priorities, limits), not `if` statements. Changing a route is a migration or an admin write, not a deploy.
- **FX providers behind a Strategy:** one interface, several provider implementations, selected by the routing engine.
- **Quotes:** a rate is quoted, locked for a short TTL, and honoured if the transfer executes inside it.
- All FX maths in `BigDecimal` with explicit scale and `HALF_EVEN`. Rates have their own scale (decide it — typically more than 4 decimals); converted amounts are rounded once, at the end.

---

## 4. Out of scope / premature

- Real FX provider APIs — stub providers with deterministic rates first.
- Crypto pairs — that's `crypto-service`.

---

## 5. Dependencies

- **Used by:** `transfer-service` for cross-currency transfers.
- **Contract gate G7:** quote request/response, quote id, TTL, what happens when a quote expires mid-saga.

---

## 6. Open decisions (resolve before coding)

- **Port clash 8085** with Schema Registry (roadmap conflict #1). Move one of them; Schema Registry is easier (only a host mapping).
- Redis for quote locks vs a `quotes` table with expiry in Postgres (simpler, one fewer store, fine at this scale).
- Rate scale and rounding policy — ADR; it's a classic interview question.

---

## 7. Skill focus

- **Educative (P9):** design patterns (Strategy, Interpreter).
- **Interview themes:** Strategy vs rules engine, data-driven configuration and its risks, FX rounding and where to round, quote expiry and race conditions, provider failover.
