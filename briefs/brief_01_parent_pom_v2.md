# Brief 01 — Parent pom.xml hardening

**Phase:** 1 · **Repo:** github.com/amlabib93-boop/cloudpay · **Est:** 45–60 min
**Read first:** `CLAUDE.md` at repo root.

---

## Task

Harden the parent `pom.xml` for multi-module Maven. Add only what Phases 1–2 need (Spring Boot 4, Java 21, Liquibase, Testcontainers, JaCoCo, Spotless, Checkstyle, SpotBugs). Do not add Kafka, Spring AI, Avro, Resilience4j, AWS SDK, or Spring Cloud — those land in their phase.

---

## Current repo state

```
cloudpay/
├── .idea/                 (tracked by mistake — remove)
├── shared-lib/            (empty module, pom + Dockerfile)
├── wallet-service/        (empty module, pom + Dockerfile)
├── docker-compose.yml     (stub, separate brief)
└── pom.xml                (empty — this brief targets it)
```

---

## Constraints

1. **Spring Boot 4.0.6** (or latest 4.0.x — verify on Maven Central).
2. **Java 21** (`maven.compiler.release=21`). SB4's minimum is 17; we choose 21 for LTS + virtual threads.
3. **No Lombok** anywhere.
4. **All versions in `<properties>`.** No inline versions below the BOMs.
5. **Spring Boot Maven Plugin in `<pluginManagement>`** — applied per-service, NOT at parent (shared-lib must not get it).
6. **Maven Wrapper** at repo root (`./mvnw`). Generate with `mvn wrapper:wrapper` if missing.

---

## Parent pom structure

**GAV:** `com.cloudpay:cloudpay-parent:0.1.0-SNAPSHOT`, packaging `pom`.

**Modules:** `shared-lib`, `wallet-service`. Nothing else.

**Properties (verify all versions on Maven Central, do not guess):**
- `maven.compiler.release` = 21
- `project.build.sourceEncoding` = UTF-8
- `spring-boot.version` = latest 4.0.x
- `liquibase.version` = latest 4.x
- `testcontainers.version` = latest 1.20.x+
- `assertj.version` = latest
- `jacoco.version` = latest 0.8.x
- `spotless.version` = latest
- `checkstyle.plugin.version` = latest maven-checkstyle-plugin
- `checkstyle.version` = 10.x
- `spotbugs.plugin.version` = latest

**dependencyManagement (BOMs only):**
1. `org.springframework.boot:spring-boot-dependencies` `${spring-boot.version}` — type pom, scope import
2. `org.testcontainers:testcontainers-bom` `${testcontainers.version}` — type pom, scope import

Plus override `org.liquibase:liquibase-core` to `${liquibase.version}` (SB ships older).

**Root dependencies (inherited by every child):**
- `spring-boot-starter`
- `spring-boot-starter-test` (scope test)
- `assertj-core` (scope test)

**pluginManagement:**
- `maven-compiler-plugin` — release 21, `<parameters>true</parameters>`
- `spring-boot-maven-plugin` at `${spring-boot.version}` — managed only
- `maven-surefire-plugin` — latest
- `maven-failsafe-plugin` — latest (for `*IT.java` later)
- `jacoco-maven-plugin` — see below
- `spotless-maven-plugin` — `palantir-java-format`, runs on `verify`
- `maven-checkstyle-plugin` — uses `checkstyle.xml` at repo root, fails on errors, skip test sources
- `spotbugs-maven-plugin` — effort=Max, threshold=Medium, skip test sources

**JaCoCo:** three executions (`prepare-agent` → initialize; `report` → test; `check` → verify). Enforce 80% line coverage on bundle, exclude `**/*Application.class`, `**/config/**`, `**/dto/**`, `**/entity/**`.

**Profiles:**
- `default` (active): runs Spotless + Checkstyle + SpotBugs + JaCoCo check on verify
- `fast`: skips all the above (JaCoCo agent still attaches)

---

## Child pom updates

**`shared-lib/pom.xml`:**
- Parent → new parent
- Inherits root deps only
- Do NOT add `spring-boot-maven-plugin`

**`wallet-service/pom.xml`:**
- Parent → new parent
- Add: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `org.postgresql:postgresql`, `org.liquibase:liquibase-core`
- Apply `spring-boot-maven-plugin` in `<build><plugins>`

---

## checkstyle.xml (repo root)

Skip `sun_checks` — too noisy. Include:
- Line length 140
- No `System.out` / `System.err`
- No wildcard imports, no unused imports
- Naming: PascalCase classes, camelCase methods, UPPER_SNAKE constants
- No empty catch blocks

For "no `@Autowired` on fields" — if no standard Checkstyle module exists, add a TODO comment in `checkstyle.xml`. Don't block.

---

## Pre-flight

1. `git status` clean. If not, surface what's uncommitted and stop.
2. `.idea/` is tracked — add to `.gitignore` and `git rm -r --cached .idea/`.
3. `./mvnw` exists at root. If not, run `mvn wrapper:wrapper` and commit.
4. `CLAUDE.md` exists at root. If not — STOP and report.

---

## Verification (DoD)

All must pass:

```bash
./mvnw clean compile
./mvnw test
./mvnw verify
./mvnw verify -Pfast
./mvnw -version    # confirm Java 21
```

Then inspect:
- `target/site/jacoco/index.html` exists per module after verify
- No deprecation or `[WARNING] Some problems...` in build output
- `shared-lib/pom.xml` has NO `spring-boot-maven-plugin`
- `wallet-service/pom.xml` HAS the plugin + the four starters

---

## Return to me

1. Final `./mvnw clean verify` status block
2. `git diff HEAD~1 HEAD` of the commit
3. One line on anything unexpected
4. Commit message used

**Suggested commit:**
```
chore(build): harden parent pom with Spring Boot 4 BOM + quality gates

- Java 21 release, Spring Boot 4.0.x + Testcontainers BOMs
- JaCoCo 80% line coverage on verify (excludes entities/dto/config)
- Spotless (palantir) + Checkstyle + SpotBugs in default profile
- Fast profile for inner-loop dev
- .idea/ untracked and ignored
- shared-lib + wallet-service updated to new parent

Refs: Phase 1
```

---

## Stop if stuck

If anything is ambiguous or you've spent 90+ min: write the problem (what you tried, what failed) and bring it back to the CloudPay thinking chat. Don't improvise on the parent pom — it propagates to every service.