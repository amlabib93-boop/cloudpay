# CloudPay

Cloud-native digital wallet and money transfer platform — 8 Java 21 microservices (Spring Boot 4, one on Quarkus) deployed on AWS EKS.

Built as a portfolio project demonstrating Senior Backend Engineer competence in fintech: distributed transactions, event-driven architecture, fraud detection with Spring AI, and production-grade observability.

---

## Services

| Service | Port | Stack | Responsibility |
|---|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway, Redis | OAuth2, JWT, rate limiting, routing, login lockout, device binding |
| `wallet-service` | 8081 | PostgreSQL, Redis | Wallet CRUD, balance management, CQRS |
| `transfer-service` | 8082 | PostgreSQL, Kafka | Transfer domain, Saga orchestration, Outbox pattern, reconciliation |
| `fraud-service` | 8083 | DynamoDB, Redis, Kafka, Spring AI | Fraud detection, async KYC checks over Kafka, anomaly scoring |
| `notification-service` | 8084 | Quarkus, MongoDB, DynamoDB, Spring AI | Notifications, hash-chained audit log, async receipts, RAG support chat |
| `remittance-service` | 8085 | PostgreSQL, Redis | DB-driven corridor rules, provider registry, FX quotes |
| `crypto-service` | 8086 | MongoDB, Redis, Kafka | Crypto and digital gold through one provider layer, quote TTL, SSE prices |
| `profiler-service` | 8087 | Spring AI | JFR profiling, FlameGraphs, AI performance analysis |

## Tech stack

**Core:** Java 21 · Spring Boot 4 · Maven multi-module · Liquibase

**Data:** PostgreSQL 16 · MongoDB 7 · DynamoDB · Redis 7

**Messaging:** Apache Kafka · Avro · Schema Registry · Outbox pattern

**AI:** Spring AI (fraud scoring, RAG chat, compliance reports, performance analysis)

**Infrastructure:** AWS EKS · ElastiCache · IAM · Secrets Manager · KMS · SNS/SQS · Lambda · Terraform · Argo CD · Helm · GitHub Actions CI/CD

**Security:** branch protection · Dependabot · CodeQL · secret scanning · Trivy · OWASP ZAP

**Observability:** OpenTelemetry · Prometheus · Grafana · Loki · Tempo

**Resilience:** Resilience4j · Istio mTLS · LitmusChaos · KEDA autoscaling

## Quick start (local)

```bash
# Prerequisites: Java 21, Docker, Maven

# Clone
git clone https://github.com/amlabib93-boop/cloudpay.git
cd cloudpay

# Copy environment variables
cp .env.example .env

# Start infrastructure
make dev-up

# Build all services
./mvnw clean verify

# Run wallet-service (first service available)
cd wallet-service && ../mvnw spring-boot:run
```

## Architecture decisions

All non-trivial architectural choices are documented as ADRs in [`docs/adr/`](docs/adr/):

<!-- ADRs will be listed here as they're written -->

## Project status

🚧 **Work in progress** — building incrementally, one service at a time.

Build order: [`briefs/ROADMAP.md`](briefs/ROADMAP.md).

<!-- ## Screenshots -->
<!-- Grafana SLO dashboard, Argo CD, FlameGraphs, RAG chat — added in later phases -->

<!-- ## Blog post -->
<!-- LinkedIn article link — added in Phase 13 -->

---

Built by [Ahmed Labib](https://github.com/amlabib93-boop) · Senior Backend Engineer · Java · Spring · Fintech