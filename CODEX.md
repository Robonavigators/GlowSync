# Codex Cloud and Codex CLI instructions

Codex agents working on GlowSync **must read and follow [AGENTS.md](AGENTS.md)** before inspecting or changing code. That file is the canonical engineering, privacy, testing, and contributor policy; this file is only an entry point.

Read [README.md](README.md) to understand implemented features and known limitations, [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidance, and [PROMPT.md](PROMPT.md) only as the original product vision. Follow the task-specific playbook linked from `AGENTS.md` for the area you change.

For routine authorized maintenance, inspect the responsible module and its tests, implement the smallest safe fix, validate with applicable commands from `AGENTS.md`, and update README plus relevant documentation when behavior changes. Follow the repository owner's current instructions for whether to commit directly to `main` or use a pull request; do not bypass GitHub branch protections or required approvals.

Never bypass Android MediaProjection consent or secure-content restrictions; never log, upload, or store captured screen frames or secrets. Preserve latest-frame-wins processing, bounded resources, correct WLED DDP packet framing, and TV remote accessibility. Do not claim unperformed tests, real-device validation, or measured screen-to-light latency.

At completion, report changed files, the exact checks and outcomes, untested hardware limitations, and the resulting commit link. If blocked by missing permissions, tooling, or tests, state the blocker rather than claiming success.
