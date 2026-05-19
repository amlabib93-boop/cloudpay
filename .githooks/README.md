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
