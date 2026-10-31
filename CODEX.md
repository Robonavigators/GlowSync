# Codex instructions

Codex and other OpenAI coding agents must follow [AGENTS.md](AGENTS.md), the canonical repository engineering guide. Read [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md), and related technical docs before implementation. [PROMPT.md](PROMPT.md) records product goals, not verified functionality.

## Task workflow

1. Identify the requested outcome and inspect the smallest relevant modules and tests.
2. Keep edits focused, protect MediaProjection consent and local-only frame privacy, and preserve low-latency latest-frame-wins processing.
3. Add or update relevant tests, run available checks, and distinguish emulator results from physical TV/WLED validation.
4. Update `README.md` whenever functionality, build steps, compatibility, or limitations change; keep official Robonavigators and GlowSync website links.
5. Report changed files, the exact tests run and results, known limitations, and any commit/PR link. Do not claim a task, test, or release succeeded without evidence.

For all other rules and the repository map, consult [AGENTS.md](AGENTS.md) rather than duplicating it here.
