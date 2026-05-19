# Brief 02 — docker-compose minimal + Makefile

**Phase:** 1 · **Repo:** github.com/amlabib93-boop/cloudpay · **Est:** 30–45 min
**Read first:** `CLAUDE.md` at repo root.
**Prereq:** Brief 01 (parent pom) merged.

---

## Task

Replace the existing `docker-compose.yml` with a minimal version where only `postgres-wallet` is active. All future infrastructure and services stay in the file as commented blocks with `# ENABLE IN: Phase X` markers — they're visible but inert. Add a `Makefile` for common ops and a `.env` for local credentials.

---

## Why minimal

`docker compose up` must succeed on day 1 with one healthy container. The file you replace tried to build 8 service images that don't exist yet → fails immediately. Keep the future shape visible (commented) so you remember what's coming, but only run what works.

---

## Files to create

1. `docker-compose.yml` (replace existing)
2. `Makefile` (new, at repo root)
3. `.env.example` (new, at repo root — committed)
4. `.env` (new, at repo root — gitignored)

Update `.gitignore` to include `.env`.

---

## docker-compose.yml structure

**Active services (uncommented):**
- `postgres-wallet` — postgres:16-alpine, port 5432, health checked, named volume

**Commented blocks, in this order, each prefixed with a `# ENABLE IN: Phase X — <reason>` header:**

| Block | Enable phase | Notes |
|---|---|---|
| redis | P2 (end) — wallet caching | `requirepass` from `.env` |
| postgres-transfer | P4 — transfer-service | port 5433, named volume |
| kafka + zookeeper + schema-registry + kafka-ui | P5 — events | **Schema Registry on port 8085** (not 8081 — wallet uses 8081). kafka-ui on 9090. |
| mongodb | P10 — crypto + notification | port 27017, root creds from `.env` |
| postgres-remittance | P9 — remittance-service | port 5434, named volume |
| prometheus + grafana | P7 — observability | prometheus on 9091, grafana on 3000 |
| api-gateway, wallet-service, transfer-service, fraud-service, notification-service, remittance-service, crypto-service, profiler-service | P2+ each | All 8 service blocks commented out — they get uncommented when their Dockerfile exists |

**For every service block (commented or not):** strip the `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` env var entirely. CloudPay does not use Eureka — Spring Cloud LoadBalancer + K8s DNS replaces it in EKS, static config locally.

**Networks:** single `cloudpay-network` (bridge driver) — declare it even though only one service uses it right now.

**Volumes:** declare only `postgres_wallet_data`. Other named volumes get declared when their service is uncommented.

**Top-level `version:` key:** omit it. Compose v2+ ignores it and warns; the spec is implicit from the file syntax.

**Container names:** prefix everything with `cloudpay-` (e.g. `cloudpay-postgres-wallet`) so it's clear which containers belong to this project when other Docker stuff is running on the laptop.

---

## postgres-wallet — active block spec

```yaml
postgres-wallet:
  image: postgres:16-alpine
  container_name: cloudpay-postgres-wallet
  environment:
    POSTGRES_DB: wallet_db
    POSTGRES_USER: ${POSTGRES_USER}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  ports:
    - "5432:5432"
  volumes:
    - postgres_wallet_data:/var/lib/postgresql/data
  networks:
    - cloudpay-network
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d wallet_db"]
    interval: 10s
    timeout: 5s
    retries: 5
```

Note the `$${POSTGRES_USER}` double-dollar in the healthcheck — Compose escapes its own variable substitution this way so the shell inside the container sees `$POSTGRES_USER`.

---

## Makefile targets

Use real tabs (not spaces) for recipe indentation. Add a `.PHONY:` line for every target.

| Target | Action |
|---|---|
| `dev-up` | `docker compose up -d` |
| `dev-down` | `docker compose down` |
| `dev-logs` | `docker compose logs -f` (optionally takes `s=<service>` arg) |
| `dev-ps` | `docker compose ps` |
| `dev-psql` | `docker compose exec postgres-wallet psql -U $${POSTGRES_USER} -d wallet_db` |
| `dev-reset` | `docker compose down -v` — **destroys volumes**, confirm prompt |
| `help` | print all targets with one-line descriptions |

Default target (first one in file, or explicit `.DEFAULT_GOAL := help`) prints help.

**Load `.env`:** Makefile should `include .env` at the top with `-include .env` (the `-` prevents failure if `.env` doesn't exist) and `export` the variables so `psql` etc. can use them.

---

## .env.example (committed)

```
POSTGRES_USER=cloudpay
POSTGRES_PASSWORD=changeme_local
MONGO_USER=cloudpay
MONGO_PASSWORD=changeme_local
REDIS_PASSWORD=changeme_local
```

Include the credentials for the commented services too — when those blocks get uncommented, the env vars are already there.

## .env (gitignored)

Same keys, real local values. Add `.env` to `.gitignore` if not already.

---

## Pre-flight

1. `git status` clean
2. CLAUDE.md committed at repo root
3. Brief 01 merged (`./mvnw clean verify` passes)
4. Existing `docker-compose.yml` from the old Study Plan iteration is at repo root — we're replacing it

---

## Verification (DoD)

All must pass:

```bash
# Compose file is syntactically valid
docker compose config > /dev/null

# Start postgres
make dev-up

# Wait ~10s for healthcheck
sleep 15

# Container is healthy (not just running)
docker compose ps | grep cloudpay-postgres-wallet | grep -i "healthy"

# Can connect
make dev-psql -c "\l" 
# OR: docker compose exec postgres-wallet psql -U cloudpay -d wallet_db -c "\l"

# Stop
make dev-down

# Volume persists across restarts
make dev-up && sleep 15
# previous data still there — wallet_db exists
```

Visual checks:
- `docker compose config` output shows ONLY `postgres-wallet` as a service (commented blocks don't appear in resolved config)
- `make help` prints all targets
- `.env` is gitignored (`git status` doesn't show it)
- `.env.example` is tracked

---

## Return to me

1. Output of `docker compose ps` showing healthy postgres
2. Output of `make help`
3. `git diff HEAD~1 HEAD --stat` for the commit
4. One line on anything unexpected
5. Commit message used

**Suggested commit:**
```
chore(infra): minimal docker-compose + Makefile + .env scaffolding

- Active: postgres-wallet only (port 5432, named volume, healthcheck)
- All future services + infra commented with "ENABLE IN: Phase X" markers
- Eureka env vars stripped (CloudPay uses K8s DNS + static config)
- Schema Registry repositioned to 8085 (commented) to free 8081 for wallet
- Makefile: dev-up, dev-down, dev-logs, dev-psql, dev-reset, help
- .env (gitignored) + .env.example (committed) pattern established

Refs: Phase 1
```

---

## Out of scope (do not do)

- ❌ Create any `Dockerfile` — services have no Java code yet, Dockerfiles will fail
- ❌ Uncomment any commented block — those wait for their phase
- ❌ Add Eureka, Consul, or any service discovery container
- ❌ Add observability stack now — Phase 7
- ❌ Add real passwords to `.env.example` — only placeholders like `changeme_local`

---

## Stop if stuck

If `docker compose config` fails with a syntax error you can't resolve in 15 min, or the postgres healthcheck won't go green in 60s: stop, paste the error and what you tried in the CloudPay thinking chat.
