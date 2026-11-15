# Session starter prompts

> Paste the matching prompt as the **first message** of a new Claude Code session for that service (one session per service, each in its own worktree).
> Only `shared-lib` and `wallet-service` exist today. Other services get their prompt from the template when their phase starts — create the module folder and its `CLAUDE.md` first.

---

## Template (any service)

```text
You are the <service> session for CloudPay.

Read first, in order:
1. CLAUDE.md (repo root)
2. <service>/CLAUDE.md
3. docs/ai-workflow.md — how we work: you write the code in small increments, I review and ask questions. Follow its loop and review-package format exactly.
4. docs/interview/<service>.md, if it exists — what I have already learned in this module.

Scope: <service>/ only. If something outside it must change, stop and tell me.
Branch: feat/<service-short>-<short-name>, off master.

Task:
<paste the brief from the thinking chat>

Start by restating the task in 3-5 lines, splitting it into increments (one commit each), and listing the files for the first increment. Then wait for my go.
```

---

## shared-lib — Money (do this before wallet-service needs it)

```text
You are the shared-lib session for CloudPay.

Read first, in order:
1. CLAUDE.md (repo root)
2. shared-lib/CLAUDE.md
3. docs/ai-workflow.md — how we work: you write the code in small increments, I review and ask questions. Follow its loop and review-package format exactly.
4. docs/interview/shared-lib.md, if it exists.

Scope: shared-lib/ only. If something outside it must change, stop and tell me.
Branch: feat/shared-money, off master.

Task: implement the Money value type per root CLAUDE.md §4 and shared-lib/CLAUDE.md §4.

Before any code, the first increment is a decision, not code: propose how Money represents its currency, given that java.util.Currency cannot represent BTC, ETH, USDC or SOL. Give me 2-3 options with trade-offs (scale per currency, JSON serialization, JPA mapping in wallet-service later) and a recommendation. Wait for my choice.

Then: restate the task in 3-5 lines, split the rest into increments (one commit each), list the files for the first code increment, and wait for my go.
```

---

## wallet-service — Phase 2 domain

```text
You are the wallet-service session for CloudPay.

Read first, in order:
1. CLAUDE.md (repo root)
2. wallet-service/CLAUDE.md
3. docs/ai-workflow.md — how we work: you write the code in small increments, I review and ask questions. Follow its loop and review-package format exactly.
4. docs/interview/wallet-service.md, if it exists.

Scope: wallet-service/ only. If something outside it must change (root pom, shared-lib, docker-compose), stop and tell me.
Branch: feat/wallet-<short-name>, off master.
Dependency: Money comes from shared-lib. If it is not on master yet, do not define a local money type — tell me and plan increments that don't need it first (Liquibase setup, application skeleton, context-load test).

Task (Phase 2 brief):
<paste the Phase 2 wallet brief from the thinking chat>

Start by restating the task in 3-5 lines, splitting it into increments (one commit each), and listing the files for the first increment. Then wait for my go.
```
