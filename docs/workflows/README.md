# Coding-agent workflows

This directory contains actionable procedures for automated coding agents and human contributors. Begin with [AGENTS.md](../../AGENTS.md), which has precedence over these guides. Review current code and [README.md](../../README.md) rather than assuming the original [PROMPT.md](../../PROMPT.md) is implemented.

| Task | Workflow |
|---|---|
| A regression, crash or broken build | [Bug fixes](bug-fix.md) |
| A feature or architecture change | [Feature development](feature-development.md) |
| TV layout, glass UI, focus or remote | [Android TV interface](android-tv-ui.md) |
| Capture permissions, lifecycle or black frames | [MediaProjection](mediaprojection.md) |
| WLED discovery, mapping, UDP/DDP | [WLED and DDP](wled-ddp.md) |
| Memory, CPU, lag or frame drops | [Performance](performance.md) |
| Device compatibility claim | [Hardware validation](device-testing.md) |
| APK/version/release changes | [Release checklist](release-checklist.md) |

Every workflow: reproduce or specify acceptance criteria, inspect relevant sources, keep changes scoped, add regression tests where feasible, run what is available, document honestly, and report blockers. Stop instead of bypassing Android protections or publishing sensitive user data.
