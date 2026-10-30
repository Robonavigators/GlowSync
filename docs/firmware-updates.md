# After an Android TV or WLED update

Firmware can change capture permissions, network discovery, TV input behavior, performance, or WLED real-time settings. After an update:

1. Open GlowSync and check that the saved WLED address responds to **Test connection**. Discovery may be blocked even when manual connection works.
2. Stop sync and send the LED marker; check LED count and physical ordering.
3. Start a new capture session and accept the Android system prompt. Confirm supported, non-protected content behaves as expected.
4. Review app processing and UDP rates, and test backgrounding and stopping on that particular TV. These app metrics do not measure visible LED latency.

GlowSync does not retain silent capture consent across process death or reboot. If the TV sleeps or projection is revoked, start again with fresh consent. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for network and capture checks.
