# Feature-development workflow

1. Translate the request into acceptance criteria and identify whether it is implemented, partial or only mentioned in [PROMPT.md](../../PROMPT.md). Do not mark aspirational features as shipped.
2. Find the right home via [architecture.md](../architecture.md): capture, processing, network, performance, diagnostics, local controller data or UI.
3. Check ownership and interactions: foreground service/session state, CPU worker, serialized UDP sender, Compose state, stored controller data and test seams. Prefer a small extension to existing abstractions.
4. Preserve local-only screen processing, permission lifecycle, bounded allocations and TV remote usability. Avoid new dependencies or background services without a compelling documented reason.
5. Add deterministic tests for packet offsets, LED direction/offset, fallback behaviors, lifecycle or preferences where relevant. Add instrumentation for real lifecycle behavior where feasible.
6. Run helper tests, JVM unit tests, lint and assemble. Test on physical hardware before making hardware-specific promises.
7. Update README, changelog/release notes if present, and technical guides in the same change. Provide a precise behavior and limitations summary.

**Do not:** introduce unimplemented UI controls, break existing mapping presets, silently increase traffic/storage, or claim measured latency from a theoretical FPS target.
