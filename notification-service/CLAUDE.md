# CLAUDE.md — notification-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P5 (roadmap #7b) for notifications and audit log; P11 (roadmap #13) for RAG chat and compliance report. **Waits on:** Kafka infra and transfer events on `master` (roadmap #6).
Until P5 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/notification-service.md` (if it exists) → `briefs/ROADMAP.md` (rows #7b, #13).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `notification-service/` only. Branch: `feat/notification-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: resolve roadmap conflict #7 (parent pom), then the Quarkus module skeleton — `pom.xml` with its own Quarkus dependency management, add to root `<modules>` (ask first), a health check and a `@QuarkusTest` smoke test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.notification` |
| Port | 8084 |
| Datastores | MongoDB 7 (notifications, templates) + DynamoDB (immutable audit log) |
| Framework | Quarkus 3.x (not Spring Boot): CDI, `quarkus-rest`, SmallRye Reactive Messaging for Kafka, `Dockerfile.jvm` |
| Compose | `mongodb` block (27017, db `cloudpay_db`) and the Quarkus-style `notification-service` block, both marked Phase 5 |
| Messaging | Kafka consumer group on transfer (and later wallet/fraud) events |

---

## 3. Responsibility

- Turn domain events into user notifications (email/push stubs locally).
- Immutable audit log of every money-relevant event: append-only, never updated or deleted.
- Idempotent consumption: a replayed event never sends a second notification or writes a second audit entry.
- P11: RAG support chat over CloudPay docs/FAQ; compliance report generation via LLM function calling (framework per roadmap conflict #8).

### Resume-linked features (see `local/resume-map.md`, local only)

- **R8 tamper-evident audit log:** each entry stores the hash of the previous one (hash chain); a verify job detects edits, deletions and gaps.
- **R7 async documents:** receipts and monthly statements generated as PDFs from events, off the request path.
- **R20 external sync:** push completed transfers to a stub external accounting system in near real time from Kafka events, with retries and idempotent delivery.
- **R18:** this service is the Quarkus one (CDI, SmallRye Reactive Messaging, no Spring compat layer).

---

## 4. Out of scope / premature

- AI features (RAG, compliance report) — P11.
- SNS / Lambda fallback — AWS, P6+.
- Real email/SMS providers. Use a logging sender behind an interface until there's a reason.

---

## 5. Dependencies

- **Consumes:** transfer events (G5); later fraud decisions and wallet events.
- **No synchronous calls** to other services. If a notification needs data the event lacks, fix the event (G5), don't call back.

---

## 6. Open decisions (resolve before coding)

- **DynamoDB in P5 vs AWS in P6+ (§10)** — same question as `fraud-service`; decide once for both.
- **pgvector for RAG (P11).** The older architecture adds pgvector, a third datastore. Alternatives: MongoDB Atlas Vector Search (not available in local MongoDB 7 community), a Redis vector index, or pgvector in a dedicated Postgres. Decide in P11, not before.
- **Parent pom vs Quarkus (roadmap conflict #7).** The root pom imports the Spring Boot BOM and adds `spring-boot-starter` to every module. This module must not inherit those: give it its own parent or move the Spring deps out of the root pom's shared `<dependencies>`. Decide before the skeleton.
- **Spring AI on a Quarkus service (roadmap conflict #8).** RAG chat and compliance reports were planned with Spring AI, which needs Spring. Options: Quarkus LangChain4j in this service, or move the AI features to a Spring service. Decide before P11.

---

## 7. Skill focus

- **Educative (P5):** Kafka, event-driven architecture. **(P11):** LLM integration, RAG architecture.
- **Interview themes:** consumer-side idempotency (dedupe table vs natural keys), ordering guarantees per partition, append-only audit design and tamper evidence, why notifications must not block the money path, RAG chunking and grounding.
