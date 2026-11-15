# CLAUDE.md — api-gateway

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P3 (roadmap #4). **Waits on:** `wallet-service` domain on `master` (a real route target).
Until P3 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/api-gateway.md` (if it exists) → `briefs/ROADMAP.md` (row #4).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `api-gateway/` only. Branch: `feat/gateway-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (the one allowed root-pom change, ask first), `GatewayApplication`, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.gateway` |
| Port | 8080 |
| Datastore | Redis 7 (rate-limit buckets) — compose block `redis`, port 6379 |
| Compose | `api-gateway` block (commented) — uncomment when the Dockerfile exists |

---

## 3. Responsibility

- Single entry point: routes external traffic to services. **No Eureka** — static routes locally, K8s DNS on EKS (locked decision).
- OAuth2 resource server: validates JWTs (RS256) before routing.
- Rate limiting: Redis token bucket, per user (and per IP for unauthenticated routes).
- No business logic. The gateway never reads or writes money.

---

## 4. Out of scope / premature

- Resilience4j circuit breakers on routes — P4 at the earliest, when there's more than one downstream.
- mTLS / Istio — P8.
- Routes to services that don't exist yet. Add a route when its service lands.

---

## 5. Dependencies

- **Downstream:** `wallet-service` (P3), then `transfer-service` (P4) and later services.
- **Contract gate G3:** JWT claims and how the user id becomes `ownerId` in `wallet-service`. Agree it in the thinking chat before coding; record it as an ADR.

---

## 6. Open decisions (resolve before coding)

- **Auth server placement.** The older architecture puts Spring Authorization Server inside the gateway. Auth server and gateway are different trust boundaries. Options: separate auth service, Keycloak in compose for local, or keep it embedded (cheapest — then it needs an ADR justifying it). Interviewers will ask.
- Whether the gateway is Spring Cloud Gateway (reactive or the MVC variant) — check SB4 compatibility of the current Spring Cloud release train before deciding.

---

## 7. Skill focus

- **Educative (P3):** Spring Security, OAuth2/JWT.
- **Interview themes:** JWT validation vs introspection, key rotation (JWKS), where authorization belongs (gateway vs service), token-bucket vs sliding-window rate limiting, what happens when Redis is down (fail open or closed?).
