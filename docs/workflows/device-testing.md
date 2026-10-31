# Hardware and device validation

Use [devices/REPORT_TEMPLATE.md](../devices/REPORT_TEMPLATE.md) and [devices/README.md](../devices/README.md). Identify whether each result is **unit test, emulator, physical Android TV, physical WLED controller or LED strip**.

1. Record TV vendor/model, Android TV/Google TV version, WLED firmware/controller, LED count/layout, Wi-Fi/LAN topology, app commit/build and tests executed. Exclude IPs, SSIDs, credentials and private frames.
2. Confirm installation and remote navigation, WLED discovery/manual address, verification and diagnostics separately.
3. Test MediaProjection grant/deny, stop/start, loss/reconnect, sleep/resume, rotation where supported, and a permitted moving test pattern.
4. Verify mapping: edge orientation, corners, starting LED, direction, count, colors/white balance and 3/4-sided modes.
5. Document whether tests exercised WLED HTTP, local UDP send, or **visually confirmed hardware output**. Never equate these.
6. For physical latency, use a documented external method (e.g., high-speed camera or photodiodes) and report measured variability.
7. Report failures, firmware restrictions, unknowns and what was not tested; propose targeted regressions for reproducible problems.

Do not assert protected streaming content, HDMI, all TV brands or background operation work universally. Never modify system packages or disable Android safeguards merely to pass tests.
