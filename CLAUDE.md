# CLAUDE.md — CloudPay

> **Purpose:** This file instructs Claude Code (and any other AI assistant) on how to generate, review, and modify code in this repository. Read this file in full at the start of every session. When in doubt about a rule, ask before generating.

---

## 1. What this project is

CloudPay is a cloud-native digital wallet and money transfer platform built as a portfolio project to demonstrate Senior Backend Engineer competence for EU fintech roles (Wise, Revolut, Zalando, Agoda, Booking, Mollie, Adyen tier).

**The audience for this code is a Senior/Staff interviewer at an EU fintech.** Every architectural choice should hold up to "why did you do it this way" under interview pressure. Cargo-culting is worse than simplicity.

Domain: fintech — wallet management, money transfers, fraud detection, notifications, remittance routing, crypto trading.

Target deployment: AWS EKS (planned, month 4+). Until then, local-only via docker-compose.

---

## 2. Stack and versions

- **Language:** Java 21 (use virtual threads, records, pattern matching, sealed classes, structured concurrency where it makes the code better — not for show)
- **Framework:** Spring Boot 4.x (latest stable in the 4.x line)
- **Build:** Maven (multi-module), Maven Wrapper checked in (`./mvnw`)
- **JVM target:** 21
- **Persistence:** PostgreSQL 16 (relational services), MongoDB 7 (document services), DynamoDB (audit/fraud), Redis 7 (caching, rate limiting, quote TTL)
- **Messaging:** Apache Kafka (Confluent Platform locally)
- **Migrations:** Liquibase for all PostgreSQL services
- **Testing:** JUnit 5, AssertJ, Mockito, Testcontainers, Spring Boot Test slices
- **AI integration:** Spring AI (in services that need it — see §4)
- **Observability:** Micrometer + OpenTelemetry + Prometheus + Grafana (month 3+)

---

## 3. Services — current state

The repo is a multi-module Maven project. Only `wallet-service` is being actively scaffolded right now. **Do not generate code that references services that don't yet exist in the repo.** Ask before creating cross-service contracts.

### Currently scaffolded

| Service | Status | Port | Primary DB |
|---|---|---|---|
| `wallet-service` | scaffolding in progress | 8081 | PostgreSQL |
| `shared-lib` | placeholder | — | — |

### Planned (not yet implemented — do not generate cross-service code)

| Service | Target month | Port | Primary DB | Responsibility |
|---|---|---|---|---|
| `api-gateway` | M2 | 8080 | Redis | OAuth2 resource server, JWT validation, rate limiting, routing |
| `transfer-service` | M2 | 8082 | PostgreSQL | Transfer domain, Saga orchestration, Outbox pattern |
| `fraud-service` | M3 | 8083 | DynamoDB + Redis | Fraud detection, Kafka consumer, Spring AI anomaly scoring |
| `notification-service` | M3 | 8084 | MongoDB + DynamoDB | Notifications, audit log, RAG chat, compliance reports |
| `remittance-service` | M4 | 8085 | PostgreSQL | DB-driven corridor routing, FX rates, provider Strategy |
| `crypto-service` | M5 | 8086 | MongoDB + Redis | Crypto buy/sell, quote TTL, price streaming via SSE |
| `profiler-service` | M5 | 8087 | — | Programmatic JFR via `jdk.jfr`, async-profiler FlameGraphs, Spring AI analysis |

---

## 4. Money handling — CRITICAL, NON-NEGOTIABLE

This is the one rule that, if violated, ships fintech bugs that look fine in tests and explode in production. There is no exception.

- **Never** use `double` or `float` for any monetary value, ever.
- **Always** use `BigDecimal` with `RoundingMode.HALF_EVEN` (banker's rounding).
- **Always** use the `Money` record from `shared-lib` for any cross-service or cross-layer monetary value:
  ```java
  public record Money(BigDecimal amount, Currency currency) { ... }
  ```
- **Scale:** minimum 4 decimal places for fiat currencies, 8 decimal places for crypto. Set scale explicitly — never trust the default.
- **Comparisons:** use `compareTo`, never `equals` (because `BigDecimal.equals` considers scale).
- **Persistence:** map `BigDecimal` columns with explicit precision and scale in JPA (`@Column(precision = 19, scale = 4)` for fiat).
- **JSON serialization:** `BigDecimal` must serialize as a string, not a number, to avoid JavaScript float precision loss on consumers.

If you (Claude Code) are about to generate code that touches money and uses `double`, `float`, or a primitive — **stop and ask.**

---

## 5. Coding conventions

### Hard rules — don't ask, just follow

- **No Lombok.** Anywhere. Write explicit getters, constructors, `equals`/`hashCode`. Use Java records for immutable data.
- **No field injection.** Constructor injection only. No `@Autowired` on fields.
- **No `@Component` scanning of arbitrary classes.** Be explicit with `@Service`, `@Repository`, `@RestController`, `@Configuration`.
- **No `System.out` / `System.err`.** Use SLF4J. Loggers are `private static final`.
- **No `e.printStackTrace()`.** Log with context or rethrow.
- **No checked exception swallowing.** If you catch, you handle or rethrow as unchecked with context.
- **No `Optional` as a method parameter or field.** Return type only.
- **Constants live in dedicated `*Constants` classes or as `static final` in the class that owns them.** No magic numbers, no string literals in business logic.

### Layered architecture — services DO have a service layer

CloudPay is portfolio code. Thin controllers, real service layer, repository as the data boundary. No skipping layers "because it's simple."

```
Controller  →  Service  →  Repository  →  DB
   (DTO)      (Domain)     (Entity)
```

- **Controllers**: handle HTTP only. Validate input DTOs (`@Valid`). Convert DTO ↔ domain. Return ResponseEntity. No business logic.
- **Services**: own the business logic. Transactional boundaries declared here (`@Transactional`). Domain objects (records or classes), not entities, cross this boundary outward.
- **Repositories**: Spring Data JPA interfaces or custom implementations. Return entities or projections. Never DTOs.
- **Entities**: JPA-mapped, package-private setters where possible, `@Version` on aggregate roots.

DTOs are records. Domain types are records or classes depending on behavior. Entities are classes.

### Package structure (per service)

```
com.cloudpay.<service>
├── <Service>Application.java
├── api/                  ← controllers, DTOs, exception handlers
│   ├── controller/
│   ├── dto/
│   └── exception/
├── domain/               ← business logic, service classes, domain types
│   ├── model/
│   └── service/
├── persistence/          ← entities, repositories, mappers
│   ├── entity/
│   ├── repository/
│   └── mapper/
├── config/               ← @Configuration classes
└── infrastructure/       ← external clients, Kafka producers/consumers, etc.
```

### Naming

- Classes: `WalletService`, `WalletController`, `WalletRepository`, `WalletEntity`, `WalletResponse`, `CreateWalletRequest`.
- Methods: verb-first (`createWallet`, `findByUserId`, `debit`, `credit`).
- Test methods: `should_<expected>_when_<condition>` (e.g., `should_throw_InsufficientFundsException_when_balance_below_amount`).

### Concurrency

- Use **virtual threads** for I/O-bound work (Spring Boot 4.0+ auto-configures with `spring.threads.virtual.enabled=true`).
- Use **structured concurrency** (`StructuredTaskScope`, preview in 21) for parallel validation/fan-out where appropriate — explicitly justify the choice in the ADR.
- **Never** use raw `Thread.start()`. Use `ExecutorService` or virtual threads.
- Optimistic locking via `@Version` on aggregate roots that have concurrent updates (e.g., `Wallet`).

---

## 6. Testing conventions

- **Smoke / context-load tests**: `@SpringBootTest` — one per service, asserts the app context loads.
- **Repository tests**: `@DataJpaTest` + Testcontainers PostgreSQL. Test real SQL behavior, not in-memory H2.
- **Controller tests**: `@WebMvcTest` + MockMvc. Mock the service layer.
- **Service tests**: plain JUnit + Mockito. No Spring context.
- **Integration tests**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers for full-stack flows. Tagged `@Tag("integration")` so they can be excluded from the fast loop.
- **Assertions**: AssertJ everywhere. No Hamcrest, no plain JUnit asserts.
- **Test data**: builder pattern or test fixtures in `src/test/java/com/cloudpay/<service>/testsupport/`. No "magic" test setup in `@BeforeEach`.

**Coverage target:** 80%+ line coverage on `domain/` packages. We don't chase coverage on controllers or config.

---

## 7. Git, commits, branches

- **Commit format:** Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
- **Branch naming:** `feat/<short-name>`, `fix/<short-name>`, `chore/<short-name>`.
- **PRs:** squash-merged to `master`. PR title becomes the commit message — use Conventional Commits there too.
- **No committing:** `.idea/`, `target/`, `*.iml`, `.env`, `logs/`. (`.gitignore` enforces this — fix the gitignore before fixing the symptom.)
- **One logical change per commit.** "WIP" commits get squashed before merging.

---

## 8. Architecture Decision Records (ADRs)

Every non-trivial architectural decision is recorded as an ADR in `docs/adr/` using MADR format. File naming: `NNNN-title-in-kebab-case.md` (e.g., `0001-multi-module-maven.md`).

**When to write an ADR:**
- Choosing between two viable patterns (Saga vs 2PC, REST vs gRPC, etc.).
- Adopting a non-obvious library.
- Setting a constraint that future-you might question (e.g., "no Lombok").
- Deciding *not* to do something (negative ADRs are valuable).

**ADRs are drafted in the CloudPay thinking chat first**, then committed to the repo. They are short (one page), opinionated, and dated.

---

## 9. AI-assisted development workflow

This project is built with Claude Code as the primary coding tool. The expectation is:

- **You (Claude Code) read this file at the start of every session.** When the user asks for something, check it against this file before generating.
- **When generating new code**: follow §5 conventions exactly. If a rule conflicts with what the user just asked, surface the conflict — don't silently violate.
- **When modifying existing code**: match the existing style in that file, even if it differs from this document. Surface the inconsistency at the end.
- **When you don't know**: ask. Don't invent file paths, class names, or imports from services that don't exist (see §3).
- **Multi-file changes**: list the files you intend to touch *before* touching them. Wait for confirmation on changes that span more than 3 files.
- **Test coverage**: every new public method in a service or controller gets a test in the same PR. No exceptions.
- **After completing a task**: run `./mvnw verify` in the affected module. Paste the result. Flag any deviation from the rules above.
- **Working loop**: code in small reviewable increments, then stop with a review package and learning notes. Follow `docs/ai-workflow.md` exactly.

---

## 10. Phased introduction — NOT premature additions

These technologies **will be added in their planned phase**. Do not introduce them speculatively. The note on each line tells you exactly when each one lands.

- ⏳ Authentication / authorization → P3 via `api-gateway` (OAuth2 server + JWT)
- ⏳ Kafka producers/consumers → P5 when `transfer-service` needs the Outbox pattern
- ⏳ Resilience4j → P4 when there's a real inter-service call to protect
- ⏳ AWS SDK / cloud anything → P6+
- ⏳ Spring AI calls → P11+, starting in `fraud-service`
- ⏳ Kubernetes manifests / Helm charts → P6+
- ⏳ Service mesh, Istio, mTLS → P8
- ⏳ Full CQRS with separate read DB → light CQRS lands in P2 (wallet-service, same DB, separate command/query services); full read-replica setup deferred

If a task seems to require any of the above, ask whether to introduce it now or defer.

---

## 11. Working agreements with the human

- **Ahmed is a Senior Backend Engineer** (Java/Spring/Kafka background, fintech domain, ~7 hrs/week on this project).
- **Time is the binding constraint.** Prefer the simple, correct solution over the clever one. Explicitly call out shortcuts so they can be revisited later.
- **Push back on bad ideas.** If a request violates a rule in this file or is likely to cause production-style problems (especially around money), say so — don't just comply.
- **No emoji in code or commit messages.** Plain text only.
- **Don't apologize.** Just fix it.