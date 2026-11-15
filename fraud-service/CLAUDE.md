# CLAUDE.md — fraud-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P5 (roadmap #7), Spring AI scoring in P11 (roadmap #13). **Waits on:** Kafka infra and transfer events on `master` (roadmap #6).
Until P5 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/fraud-service.md` (if it exists) → `briefs/ROADMAP.md` (rows #7, #13).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `fraud-service/` only. Branch: `feat/fraud-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (ask first), `FraudServiceApplication`, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.fraud` |
| Port | 8083 |
| Datastores | DynamoDB (fraud decisions, history) + Redis 7 (velocity counters, blacklist) |
| Messaging | Kafka consumer of transfer events; produces fraud decisions (shape per G6) |

---

## 3. Responsibility

- Consume transfer events; score each transfer; emit a decision (approve / reject / review).
- Checks run in parallel: velocity (Redis counters with TTL), blacklist (Redis set), and from P11 an AI anomaly score.
- Retry topics and a DLQ for poison messages. Consumer is idempotent (same event twice → same decision, no double counting).
- Every decision is stored with its inputs and reasons — explainability is a compliance requirement, not a nice-to-have.

---

## 4. Out of scope / premature

- Spring AI anomaly scoring — P11. P5 ships rule-based checks only.
- AWS SDK against real AWS — P6+ (see open decision on DynamoDB below).
- Any write to wallets or transfers. Fraud decides; `transfer-service` acts.

---

## 5. Dependencies

- **Consumes:** transfer events (G5).
- **Produces / answers:** fraud decisions consumed by the transfer saga (G6).
- **Contract gate G6:** event back to the saga vs a sync call from transfer. Async fits the outbox design; sync is simpler but couples availability. Decide before P5 coding.

---

## 6. Open decisions (resolve before coding)

- **DynamoDB in P5 vs AWS in P6+ (§10).** Options: DynamoDB Local container in compose (keeps the planned store, AWS SDK used against a local endpoint — needs a §10 exception), or Redis/PostgreSQL only in P5 and DynamoDB in P6. Roadmap conflict #3.
- Compose block for `fraud-service` says "Phase 6"; the roadmap says P5. Align when the phase starts.
- `StructuredTaskScope` for the parallel checks: preview in Java 21, needs `--enable-preview` at compile and run time. Justify in an ADR (root `CLAUDE.md` §5).
- Behaviour when a check times out or Redis is down: fail open (approve) or fail closed (review)? It's a business decision with a technical default.

---

## 7. Skill focus

- **Educative (P5):** Kafka, event-driven architecture. **(P11):** LLM integration.
- **Interview themes:** consumer groups and rebalancing, offset commit vs processing order, idempotent consumers, DLQ and retry-topic design, velocity counters with Redis TTLs, structured concurrency vs `CompletableFuture`, why an LLM score must never be the sole reject reason.
