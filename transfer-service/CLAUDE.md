# CLAUDE.md — transfer-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P4 (roadmap #5), outbox in P5 (roadmap #6). **Waits on:** `shared-lib` `Money`; `wallet-service` credit/debit with idempotency keys (roadmap #3).
Until P4 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/transfer-service.md` (if it exists) → `briefs/ROADMAP.md` (rows #5, #6).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `transfer-service/` only. Branch: `feat/transfer-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (ask first), `TransferServiceApplication`, Liquibase master changelog, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.transfer` |
| Port | 8082 |
| Database | PostgreSQL 16, `transfer_db` — compose block `postgres-transfer`, host port 5433 |
| Migrations | Liquibase, `src/main/resources/db/changelog/db.changelog-master.yaml` |
| Messaging | Kafka (P5) — `kafka:29092` in compose, `localhost:9092` from host |

---

## 3. Responsibility

- `Transfer` aggregate: source wallet, target wallet, `Money` amount, status state machine (e.g. `PENDING → DEBITED → COMPLETED`, or `→ COMPENSATED / FAILED`).
- Saga across wallet debit → wallet credit, with compensation when a later step fails.
- Outbox pattern (P5): transfer state change and its event are written in one DB transaction; a relay publishes to Kafka.
- Idempotent API: a client retry with the same idempotency key never creates a second transfer.
- Resilience4j around calls to `wallet-service`: timeouts, retry only on idempotent calls, circuit breaker.

### Resume-linked features (see `local/resume-map.md`, local only)

- **R14 reconciliation (P5):** a job compares completed transfers with wallet ledger entries and reports mismatches. It never auto-fixes money.
- **R21 reporting (P5):** daily transaction summary (volumes and totals per currency) built from the ledger.
- **R15 failure handling:** retries with backoff only on idempotent calls, timeouts, circuit breaker; failure rate measured with fault injection in P8.
- **R12:** batch inserts where volume is real (reconciliation results, outbox relay).

---

## 4. Out of scope / premature

- Fraud step in the saga — added in P5 once `fraud-service` exists (roadmap #7).
- FX / cross-currency transfers — P9 with `remittance-service`. Until then, source and target currencies must match.
- Redis distributed lock. Optimistic locking (`@Version`) plus idempotency keys plus the outbox already cover the cases. Add a lock only with a concrete race that those don't cover, and an ADR.
- "Kafka exactly-once" claims. The outbox gives at-least-once delivery; consumers must be idempotent. Say that accurately in code comments and ADRs.

---

## 5. Dependencies

- **Calls:** `wallet-service` credit/debit (sync HTTP, P4).
- **Publishes (P5):** transfer lifecycle events, consumed by `fraud-service` and `notification-service`.
- **Contract gate G4:** wallet credit/debit API as used by transfer, including idempotency-key semantics and error mapping (insufficient funds, version conflict).
- **Contract gate G5:** event schemas (Avro), topic names, partition key (transfer id or wallet id?).
- **Contract gate G6:** how the fraud decision reaches the saga.

---

## 6. Open decisions (resolve before coding)

- **Saga style.** Root `CLAUDE.md` §3 says orchestration; the older architecture said choreography. Orchestration is easier to reason about and explain for a two-step money flow. Decide in an ADR before the first saga increment.
- Where the saga state lives (on the `Transfer` row vs a separate saga table).
- Parallel pre-validation (wallets exist, currencies match, limits): plain blocking calls on virtual threads inside `StructuredTaskScope` vs `CompletableFuture`. Preview API in Java 21 — needs `--enable-preview` at compile and run time; ADR if chosen.

---

## 7. Skill focus

- **Educative (P4):** distributed transactions, Java concurrency. **(P5):** Kafka, event-driven architecture.
- **Interview themes:** saga vs 2PC, orchestration vs choreography, compensation that itself fails, outbox vs CDC (Debezium), at-least-once vs exactly-once, idempotency keys, retry storms and why only idempotent calls retry.
