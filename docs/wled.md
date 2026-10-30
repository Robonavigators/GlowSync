# WLED connection

GlowSync can discover mDNS HTTP service candidates and checks `/json/info` before listing a controller as verified. Manual IP address or hostname entry is available when discovery is blocked. Multiple verified controllers can be saved; one controller is streamed to during a capture session. Change the destination by starting a new session.

The app sends RGB24 DDP over unicast UDP port 4048. Frames are divided into packets with byte offsets; the final packet has the PUSH flag. DDP and the controller's HTTP API are unencrypted, so use a trusted network. A successful HTTP check confirms that the information endpoint responded; a UDP send confirms only that the local send call completed, not that LEDs received or displayed the frame.

GlowSync checks controller HTTP reachability on a separate low-frequency control worker and retries local network errors with backoff. UDP cannot detect every unreachable or restarted controller. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for network checks.
