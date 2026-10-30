# Codex guidance for GlowSync

Read [AGENTS.md](AGENTS.md) before working in this repository. It is the shared source of project instructions for coding agents. Read [README.md](README.md) for the implemented behavior and limitations, and [docs/architecture.md](docs/architecture.md) for the current structure.

[PROMPT.md](PROMPT.md) is the original product request, not a statement that all features or hardware validation are complete. Preserve Android's capture-consent and protected-content restrictions; never invent connectivity, performance, or physical-device results. Keep frame data local and off disk.

For code changes, run the relevant checks described in [tests/README.md](tests/README.md), and report any emulator-only testing or unavailable hardware. Keep user changes and the existing MIT license intact. Update the README and supporting docs when behavior changes.
