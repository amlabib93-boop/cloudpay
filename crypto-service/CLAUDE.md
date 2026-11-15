# CLAUDE.md — crypto-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P10 (roadmap #12), market insight via Spring AI in P11. **Waits on:** `shared-lib` `Money` supporting crypto (8-decimal scale); `wallet-service` idempotent credit/debit; Kafka infra (roadmap #6).
Until P10 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/crypto-service.md` (if it exists) → `briefs/ROADMAP.md` (row #12).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `crypto-service/` only. Branch: `feat/crypto-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (ask first), `CryptoServiceApplication`, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.crypto` |
| Port | 8086 |
| Datastores | MongoDB 7 (orders, holdings) + Redis 7 (quotes with TTL, price cache) |
| Messaging | Kafka for the price stream; SSE to clients |
| Assets | BTC, ETH, USDC, SOL |

---

## 3. Responsibility

- Buy/sell against a quote. Quote lives 30 s in Redis; accepting it is an **atomic** consume (e.g. `GETDEL` / Lua), so a quote can't be used twice.
- Slippage tolerance: reject execution if the price moved beyond the user's tolerance.
- Settlement with `wallet-service`: debit fiat, credit crypto (or the reverse), as a saga with compensation.
- Price streaming: prices published to Kafka, fanned out to clients over SSE.
- Crypto amounts: scale 8 minimum, `HALF_EVEN` (root `CLAUDE.md` §4).

---

## 4. Out of scope / premature

- Real exchange integrations — stub price feed first.
- Spring AI market insight — P11.
- Custody, blockchain nodes, on-chain transfers. CloudPay holds a ledger, not keys.

---

## 5. Dependencies

- **Calls:** `wallet-service` credit/debit (G8).
- **Contract gate G8:** how crypto holdings relate to wallets (separate crypto wallets in `wallet-service`, or holdings owned here), and the compensation path when the second leg fails.
- **Needs from shared-lib (G1):** `Money` that represents crypto currencies at 8 decimals.

---

## 6. Open decisions (resolve before coding)

- G8 above — the biggest design question for this service.
- MongoDB for money-bearing records: store amounts as `Decimal128`, never as double. Decide the mapping before the first entity.
- SSE vs WebSocket for the price stream (SSE is enough for one-way prices; WebSocket only if clients send data).

---

## 7. Skill focus

- **Educative (P10):** reuse P4-P5 material (sagas, Kafka); Redis atomic operations.
- **Interview themes:** quote TTL races, atomic check-and-consume in Redis, slippage, saga compensation when the market moved, decimal handling in MongoDB, backpressure on SSE streams.
