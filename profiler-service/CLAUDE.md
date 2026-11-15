# CLAUDE.md — profiler-service

> Module-level guide. The root `CLAUDE.md` applies in full; this file narrows scope. If they conflict, the root wins — surface the conflict.

**Status:** not started. **Phase:** P11 start, P12 real build (roadmap #14). **Waits on:** observability stack (roadmap #9) and several services running, so there is something to profile.
Until P11 starts, this folder holds only this file. No `pom.xml`, no entry in root `<modules>`.

---

## 1. Starting a session here

1. Read, in order: root `CLAUDE.md` → this file → `docs/ai-workflow.md` → `docs/interview/profiler-service.md` (if it exists) → `briefs/ROADMAP.md` (row #14).
2. Check the gate: is the phase started and is everything under "waits on" on `master`? If not, stop and tell Ahmed.
3. Need a brief from the thinking chat. If none was pasted, ask for it. Don't invent scope from this file.
4. Scope: `profiler-service/` only. Branch: `feat/profiler-<short-name>` off `master`.
5. First reply: restate the task in 3-5 lines, split it into increments (one commit each), list the files for the first increment, wait for "go".

First increment when the phase starts: module skeleton — `pom.xml`, add to root `<modules>` (ask first), `ProfilerServiceApplication`, context-load test.

---

## 2. Service facts

| Item | Value |
|---|---|
| Base package | `com.cloudpay.profiler` |
| Port | 8087 |
| Datastore | None in root `CLAUDE.md` §3. The older architecture says S3 for recordings — AWS, so P6+ is fine by then; confirm. |

---

## 3. Responsibility

- Start and stop JFR recordings programmatically (`jdk.jfr` API) against running CloudPay services.
- Produce FlameGraphs with async-profiler.
- Spring AI analysis of recordings: summarise hot paths, allocation pressure, lock contention, GC behaviour, and suggest where to look.
- It's a JVM-profiling assistant for engineers, not part of the money path.

### Resume-linked features (see `local/resume-map.md`, local only)

- **R13 memory/GC investigation:** reproduce a memory leak under a soak test, find it with JFR and a heap dump (Eclipse MAT), fix it, tune GC, and write up the before/after in `docs/performance/`.

---

## 4. Out of scope / premature

- Anything before observability (P7) exists — it needs metrics and traces to correlate with.
- Always-on continuous profiling in production — out of scope for a portfolio; on-demand recordings are enough.

---

## 5. Dependencies

- **Targets:** any running CloudPay service. How it reaches them (JMX, a sidecar, an actuator endpoint) is the first design question.
- No money contracts.

---

## 6. Open decisions (resolve before coding)

- How recordings are triggered on a remote JVM, and the security of that channel (JMX over the network is a real attack surface).
- Where recordings are stored (local volume vs S3).
- How much recording data goes to the LLM (size, sensitive data in stack traces and arguments).

---

## 7. Skill focus

- **Educative (P11):** LLM integration. JFR and async-profiler from the JDK docs and the async-profiler README.
- **Interview themes:** JFR overhead and event types, safepoint bias (why async-profiler exists), reading a FlameGraph, diagnosing virtual-thread pinning, allocation profiling vs CPU profiling.
