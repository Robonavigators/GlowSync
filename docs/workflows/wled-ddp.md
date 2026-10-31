# WLED, RGB mapping and DDP workflow

Read [WLED guide](../wled.md), [DDP specification](../specs/ddp.md), `network/Ddp.kt`, `network/WledDiscovery.kt`, `processing/EdgeSampler.kt` and related unit tests before changing transport or mapping.

1. Distinguish HTTP/mDNS discovery, verification, controller selection, RGB mapping and UDP streaming. WLED `/json/info` reachability is not proof of LED output.
2. Keep RGB24 payload/packet lengths, offset fields, sequence and final PUSH semantics consistent with the existing DDP codec. Test boundary sizes and multi-packet frames; avoid overrunning configured LED counts.
3. Ensure 3-sided/4-sided modes, direction and offsets remain correct. Test each edge and corner with known color patterns and preserve previous profiles.
4. Network work must not block capture/UI; serialize sends and prefer newest LED frame. Use bounded retries/backoff for HTTP discovery rather than retry storms.
5. DDP is unencrypted, UDP is not acknowledged, and network access should be confined to a trusted LAN. Never expose device addresses in public reports.
6. Run `UdpTest.kt`, `MappingTest.kt` and broader tests. On physical hardware, use diagnostics and visually confirm correct order, colors and LED count; report firmware/model/configuration.

**Stop:** when a requested compatibility or latency claim lacks actual measured or observed evidence.
