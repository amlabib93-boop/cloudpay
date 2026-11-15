# CLAUDE.md — shared-lib

> Module-level instructions. The root `CLAUDE.md` still applies in full; this file narrows scope for sessions working on `shared-lib`. If the two conflict, the root file wins — surface the conflict.

---

## 1. Why this module is special

Every service depends on `shared-lib`. A change here is a change to all of them. Shared code is coupling — keep this module **small, stable, and boring**.

- **Edit only `shared-lib/`.** Updating callers in services is a separate change, in that service's session.
- Every change here is its own PR (`feat/shared-<short-name>`), merged before any service PR that needs it.
- Breaking a public type or signature requires asking first and listing every caller.

---

## 2. What belongs here

| Allowed now | Lands later |
|---|---|
| `Money` record (root CLAUDE.md §4) | Avro schemas / event contracts — P5, with Kafka |
| Small, truly cross-cutting exceptions (e.g. a base `CloudPayException`) | Idempotency helpers — only once a second service needs one |

**Not allowed:** entities, repositories, Spring `@Configuration`, service-specific DTOs, HTTP clients for other services, anything used by only one service. "Might be reused later" is not a reason — move code here when the second caller exists.

---

## 3. Module facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.shared` |
| Packaging | plain jar — **no** `spring-boot-maven-plugin` |
| Dependencies | Plain Java preferred. Code here must not require a Spring context to use. |

Note: the root pom currently adds `spring-boot-starter` to every module, including this one. Don't rely on it here; if it becomes a problem, raise it as a root-pom change.

---

## 4. Money rules (from root §4, enforced here)

- `public record Money(BigDecimal amount, Currency currency)`.
- Compact constructor validates non-null and normalises scale explicitly with `RoundingMode.HALF_EVEN`: 4 for fiat, 8 for crypto. Never trust the incoming scale.
- `Currency` here means the CloudPay currency notion, which must also cover crypto (BTC, ETH, USDC, SOL) — `java.util.Currency` does not. Decide the type before writing `Money`; ask if unsure.
- Arithmetic (`add`, `subtract`, ...) rejects mismatched currencies. No implicit FX.
- Comparisons use `compareTo`; `equals`/`hashCode` must be scale-insensitive (record defaults are **not** — override them).
- Never `double`/`float`. Not even in tests or factory methods.

---

## 5. Testing and commands

- Plain JUnit 5 + AssertJ. No Spring context.
- 80%+ line coverage (JaCoCo gate in the parent pom enforces it on `verify`).

```bash
./mvnw -pl shared-lib verify
```

After every task: run it, paste the result, flag any deviation (root CLAUDE.md §9).
