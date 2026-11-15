# AI-assisted workflow — how Claude Code sessions work on CloudPay

> Read by every Claude Code session at start (root `CLAUDE.md` §9 points here).
> Goal: Claude Code writes the code; Ahmed reviews, questions, and learns from every increment. The code ships *and* the skill sticks.

---

## 1. Session layout

- **One session per service**, each in its own git worktree, on its own branch (`feat/<service>-<short-name>`).
- Each session reads: root `CLAUDE.md` → `<module>/CLAUDE.md` → this file → `docs/interview/<service>.md` (if it exists).
- A session edits only its own module. Cross-module changes are separate PRs (see the module `CLAUDE.md`).
- To start a session: open the service's `<module>/CLAUDE.md` — its "Starting a session here" section is the checklist. Build order and dependencies: `briefs/ROADMAP.md`.

---

## 2. The loop

1. **Plan.** Restate the task in 3-5 lines. Split it into **increments** — each one small enough to review in ~15 minutes and land as one commit. List the files for the first increment. **Wait for "go".**
2. **Build one increment.** Code + tests for that increment only. Run `./mvnw -pl <module> -am verify`.
3. **Stop and present the review package** (§3). Do not start the next increment.
4. **Ahmed reviews and asks questions.** Answer them (§4). Change code only when asked, or when a question exposes a real defect — then say so plainly and fix it.
5. **On approval:** append to the learning notes (§5), commit (Conventional Commits, body says what was learned), then propose the next increment.

Time is the binding constraint (~7 hrs/week). Prefer fewer, meatier increments over many trivial ones. Never pad explanations.

---

## 3. Review package — after every increment

Use exactly these headings, in this order. Keep each section tight.

**What changed** — files touched, 3-5 bullets.

**Why this way** — the key design decision(s), the alternative(s) considered, and why they lost. If a rule from `CLAUDE.md` drove the choice, cite the section.

**Review guide** — 3-5 specific spots worth reading carefully, as `path:line`, each with *what to check* (e.g. "transaction boundary: is the version check inside it?", "scale set before compare?"). This is where Ahmed's review time should go.

**How it fails** — what breaks under concurrency, bad input, DB/network failure, or at scale, and what in the code handles it (or doesn't yet).

**Interview angle** — 2-3 questions a Senior interviewer at Wise/Revolut/Adyen would ask about this code, each with a short model answer (3-5 lines). Include at least one follow-up "what if…" probe.

**Go deeper** — one concept worth studying properly, with where to read (the phase's Educative topic, Spring/Hibernate reference docs, a JEP). Optionally one **exercise** Ahmed can do himself in ≤ 30 min (e.g. "write the test for the missing edge case X", "remove `@Version` and run the concurrency test — explain the result"). Optional means optional: skip it on a busy week.

**Shortcuts taken** — anything simplified or deferred, so it can be revisited.

**Verify** — the `./mvnw ... verify` summary (pass/fail, coverage), and any deviation from `CLAUDE.md` rules.

---

## 4. Answering Ahmed's questions

- Ahmed is a Senior Backend Engineer. Skip basics; go deep on *why*, failure modes, and production behaviour.
- Call out where Spring Boot 4 / Java 21 differ from Spring Boot 3 / Java 17 habits — that's where interview credibility is won or lost.
- "Why not X?" gets an honest trade-off. If X is actually better, say so and propose the change — don't defend the code for its own sake.
- If the answer is "I'm not sure", say that, and say how to verify it (a test, a doc link, a quick experiment).
- Answers don't change code unless Ahmed asks.

---

## 5. Learning notes — `docs/interview/<service>.md`

Appended after each approved increment and committed with it. Becomes interview prep material for Phase 14.

Per increment, add:

```markdown
## <increment title> — <YYYY-MM-DD>
**Concepts:** 2-4 bullets, one line each.
**Decision:** what we chose, what we rejected, why (one paragraph).
**Interview Q&A:** the questions from the review package, with Ahmed's final understanding (refined by his questions).
**Open questions / to revisit:** if any.
```

Keep it scannable — this is a revision sheet, not a blog post.
