# CLAUDE.md — wallet-service

> Module-level instructions. The root `CLAUDE.md` still applies in full; this file narrows scope for sessions working on `wallet-service`. If the two conflict, the root file wins — surface the conflict.

**Status:** active. **Phase:** P2 (roadmap #2, #3). **Waits on:** `shared-lib` `Money` for domain code only — skeleton, Liquibase and context-load test can start now.

---

## Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/wallet-service.md` (if it exists) → `briefs/ROADMAP.md` (rows #2, #3).
2. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
3. If `Money` is not on `master` yet: don't define a local money type. Plan the increments that don't need it first.
4. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

---

## 1. Scope of a wallet-service session

- **Edit only `wallet-service/`.** Anything outside it (root `pom.xml`, `shared-lib/`, `docker-compose.yml`, `CLAUDE.md`) is out of scope.
- If a task needs a change outside this module, **stop and say so**. That change goes in its own PR, on its own branch, before or after this one.
- Exception: adding a block to `docker-compose.yml` that this service needs *in its planned phase* (e.g. Redis at end of P2) — ask first.
- Do not create code, DTOs, clients or contracts for other services (`transfer-service`, `api-gateway`, ...). They don't exist yet.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.wallet` |
| HTTP port | 8081 |
| Database | PostgreSQL 16, `wallet_db` (container `cloudpay-postgres-wallet`, host port 5432) |
| Migrations | Liquibase, master changelog at `src/main/resources/db/changelog/db.changelog-master.yaml` |
| Credentials | From `.env` via `POSTGRES_USER` / `POSTGRES_PASSWORD` — never hardcode, never commit |
| Starters | `webmvc`, `data-jpa`, `validation`, `actuator`, `liquibase` (SB4 modular starters) |

---

## 3. Phase 2 target (current work)

- `Wallet` aggregate root: `id`, `ownerId`, `currency`, `balance` (`BigDecimal`, `precision = 19, scale = 4`), `@Version`.
- `Money` comes from `shared-lib` (see `shared-lib/CLAUDE.md`). Do not define a local money type.
- Light CQRS, same DB: `WalletCommandService` (create, credit, debit) and `WalletQueryService` (reads). No separate read store.
- Optimistic locking via `@Version`; `ConcurrentBalanceUpdateTest` proves it under contention (20 threads).
- Redis lands at the **end** of P2 for caching — not before the domain and tests are done.
- First real ADR (optimistic vs pessimistic locking) is drafted in the thinking chat, not here.

Not in this phase: auth/JWT (P3), Kafka/outbox (P5), Resilience4j (P4), Spring AI (P11+), AWS SDK (P6+).

### Resume-linked features (see `local/resume-map.md`, local only)

- **R12 / R17 (P2):** Redis read cache and index work driven by `EXPLAIN ANALYZE`; record before/after numbers in `docs/performance/`.
- **R16:** the optimistic-lock contention test is the concurrency story.
- **R7 onboarding (P5, not now):** a wallet starts `PENDING_KYC` and becomes `ACTIVE` on a KYC-approved event from `fraud-service`.
- **R14 (P5):** reconciliation needs a ledger row per credit/debit. Agree its shape in G4 before transfer starts.

---

## 4. Domain rules specific to wallets

- Balance never goes negative. Debit below balance throws `InsufficientFundsException`.
- Credit/debit amounts must be strictly positive (`compareTo(BigDecimal.ZERO) > 0`).
- Currency of the operation must match the wallet currency. No implicit FX — that is `remittance-service`'s job later.
- Every money operation goes through the service layer inside `@Transactional`. Controllers never touch entities.
- `OptimisticLockException` is not swallowed: either retry with a bounded, explicit policy or map to HTTP 409. Decide in the ADR.

---

## 5. Commands

```bash
make dev-up                                  # start postgres-wallet
./mvnw -pl wallet-service -am verify         # build this module (+ shared-lib) with quality gates
./mvnw -pl wallet-service -am verify -Pfast  # inner loop, gates skipped
cd wallet-service && ../mvnw spring-boot:run # run locally on 8081
```

After every task: run `./mvnw -pl wallet-service -am verify`, paste the result, flag any deviation (root CLAUDE.md §9).

---

## 6. Branches

Work on `feat/wallet-<short-name>` (e.g. `feat/wallet-domain`). One logical change per PR, squash-merged to `master`.

---

## 7. Dependencies and skill focus

- **Used by:** `api-gateway` (P3), `transfer-service` (P4, contract gate G4), `crypto-service` (P10, G8). Credit/debit must be idempotent before transfer starts (roadmap #3, gate G2).
- **Educative (P2):** JPA/Hibernate concurrency and locking, Spring Data JPA.
- **Interview themes:** optimistic vs pessimistic locking, lost updates, what `@Version` actually checks in the SQL, retry on conflict vs 409, CQRS without a separate read store, why `BigDecimal.equals` is a trap, Liquibase rollback strategy.
