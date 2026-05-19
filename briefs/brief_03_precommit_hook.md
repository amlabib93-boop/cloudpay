# Brief 03 — Pre-commit hook (Spotless + Checkstyle on staged files)

**Phase:** 1 · **Repo:** github.com/amlabib93-boop/cloudpay · **Est:** 30–45 min
**Read first:** `CLAUDE.md` at repo root.
**Prereq:** Briefs 01 + 02 merged.

---

## Task

Add a local git pre-commit hook that runs **Spotless format check + Checkstyle** on staged Java files before every commit. Goal: catch formatting and style violations *before* they're committed, not after CI fails.

This is **not** Husky-style npm tooling — we use a plain shell script at `.githooks/pre-commit` + a setup target in the Makefile so a fresh clone can install hooks with one command.

---

## Why this approach (not Husky / pre-commit framework)

- **No new dependencies.** Java project shouldn't pull npm/python tooling just for hooks.
- **Maven already has the plugins** from brief 01 (Spotless, Checkstyle). Hook just calls them.
- **Self-documenting.** Anyone cloning the repo runs `make hooks-install` and is protected.
- **Bypassable.** `git commit --no-verify` works when you genuinely need it (rare).

---

## Files to create

1. `.githooks/pre-commit` — shell script, executable
2. `.githooks/README.md` — explains what hooks exist and why
3. `Makefile` — add `hooks-install` target

---

## .githooks/pre-commit specification

Shell script (bash). Behavior:

1. **Detect staged Java files.** `git diff --cached --name-only --diff-filter=ACM | grep -E '\.java$'`. If none → exit 0 immediately (don't run Maven for non-Java commits).

2. **Run Spotless check on the whole repo.** `./mvnw spotless:check -q`. If it fails → print clear message: "Spotless found formatting violations. Run `./mvnw spotless:apply` to fix." Exit 1.

3. **Run Checkstyle on the whole repo.** `./mvnw checkstyle:check -q`. If it fails → print message pointing to where the report lives (`target/checkstyle-result.xml`). Exit 1.

4. **Skip SpotBugs in the hook.** It's slow (30-60s). Run only in CI / on `verify`. Hook should be fast (under 15s typically).

5. **On success:** exit 0 silently (no chatty output — only speak when something's wrong).

**Quality requirements for the script:**
- Bash-only (`#!/usr/bin/env bash`)
- `set -euo pipefail` at top
- Quote all variable references
- Print errors to stderr, not stdout
- Use `command -v ./mvnw` to verify wrapper exists before running

**Performance note:** running Spotless+Checkstyle on the *whole repo* (not just staged files) is the simple correct choice — easier than building a partial-file invocation, and Maven is fast enough on a project this size. Reconsider if it ever takes >30 seconds.

---

## Why hooks live in `.githooks/` (not `.git/hooks/`)

`.git/hooks/` is local-only — never committed. `.githooks/` is a directory we commit, then point git to it via `core.hooksPath`. That's how teammates (and future-you on another machine) get the hooks automatically after `make hooks-install`.

---

## Makefile additions

Add these targets to the existing Makefile from brief 02:

```makefile
hooks-install:    ## install git hooks (run once per clone)
	git config core.hooksPath .githooks
	chmod +x .githooks/pre-commit
	@echo "Git hooks installed. Pre-commit will run Spotless + Checkstyle on every commit."

hooks-uninstall:  ## remove git hooks
	git config --unset core.hooksPath
	@echo "Git hooks uninstalled. Default .git/hooks/ is back in use."

format:           ## auto-apply Spotless formatting to all Java files
	./mvnw spotless:apply

format-check:     ## check Spotless formatting without modifying
	./mvnw spotless:check

style-check:      ## run Checkstyle without modifying
	./mvnw checkstyle:check
```

Update the `help` target to include these (or `.DEFAULT_GOAL := help` should pick them up automatically if you used standard help-target patterns).

Add `hooks-install`, `hooks-uninstall`, `format`, `format-check`, `style-check` to `.PHONY:`.

---

## .githooks/README.md content

Short — what hooks exist, what they do, how to install/bypass:

```markdown
# Git hooks

## pre-commit

Runs on every `git commit`. Checks staged Java files against:

1. **Spotless** — formatting (palantir-java-format). Fix with `make format`.
2. **Checkstyle** — style rules from `checkstyle.xml`. Inspect violations in `target/checkstyle-result.xml`.

SpotBugs is NOT in the hook (too slow) — it runs in CI and on `make verify`.

## Installing

After cloning the repo:

```bash
make hooks-install
```

This points git to `.githooks/` and makes the hook executable.

## Bypassing (use sparingly)

If you need to commit without running hooks:

```bash
git commit --no-verify -m "..."
```

Don't make this a habit — hooks exist to catch problems before CI.
```

---

## Pre-flight

1. `git status` clean
2. Briefs 01 + 02 merged (`./mvnw verify` passes)
3. CLAUDE.md committed

---

## Verification (DoD)

**Test 1 — installation works:**
```bash
make hooks-install
git config --get core.hooksPath    # should print: .githooks
ls -l .githooks/pre-commit         # should show executable (x permission)
```

**Test 2 — hook runs on commit:**
```bash
# Make a deliberate formatting error
echo 'public class Test {  public  static void  main(String[]args){}}' > wallet-service/src/main/java/com/cloudpay/wallet/Test.java
git add wallet-service/src/main/java/com/cloudpay/wallet/Test.java
git commit -m "test: deliberate format violation"
# Expected: commit fails, message about Spotless violations
```

**Test 3 — clean commit passes:**
```bash
rm wallet-service/src/main/java/com/cloudpay/wallet/Test.java
git add -A
git commit -m "chore: remove test file"
# Expected: commit succeeds, hook runs silently
```

**Test 4 — bypass works when needed:**
```bash
# Re-create the bad file
echo 'class Bad{}' > wallet-service/src/main/java/com/cloudpay/wallet/Test.java
git add wallet-service/src/main/java/com/cloudpay/wallet/Test.java
git commit --no-verify -m "test: bypass hook"
# Expected: commits successfully despite violations
# Then clean up:
git reset --hard HEAD~1
```

**Test 5 — non-Java commits skip the hook fast:**
```bash
echo "test" >> README.md
git add README.md
time git commit -m "docs: test"
# Expected: <1s commit time (hook exits early on no Java changes)
git reset --hard HEAD~1
```

---

## Return to me

1. Output of `make hooks-install`
2. Output of Test 5 (`time git commit ...`) showing fast skip on non-Java change
3. `git log --oneline -3`
4. One line on anything unexpected
5. Commit message used

**Suggested commit:**
```
chore(dev): add pre-commit hook for Spotless + Checkstyle

- .githooks/pre-commit runs format + style checks on Java commits
- Skips automatically for non-Java commits (fast path)
- SpotBugs deliberately deferred to CI (too slow for hook)
- make hooks-install registers via core.hooksPath
- New Makefile targets: hooks-install, hooks-uninstall, format, format-check, style-check
- Bypassable with --no-verify when genuinely needed

Refs: Phase 1
```

---

## Out of scope (do not do)

- ❌ Don't add `pre-push` hook — overkill for now
- ❌ Don't add commit-msg hook for Conventional Commits enforcement — Phase 3+ if at all
- ❌ Don't add npm/Husky tooling
- ❌ Don't include SpotBugs in the hook (slow)
- ❌ Don't modify the pre-commit hook to auto-fix — explicit `make format` is clearer

---

## Stop if stuck

If `git config core.hooksPath .githooks` doesn't take effect, or Spotless takes >30s on this small repo: stop and bring the symptom back to the CloudPay thinking chat. Likely environment issue, not a brief flaw.
