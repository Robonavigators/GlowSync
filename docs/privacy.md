# Privacy and networking

GlowSync analyzes permitted screen frames locally in memory. It does not save screen recordings or screenshots, upload frames, require a cloud account, or include analytics. Device and calibration settings are stored locally in app-private preferences; Settings can clear them.

Screen capture always uses Android's system consent flow. Protected content, secure windows and some inputs can be unavailable; GlowSync does not bypass those restrictions. The foreground service displays an active synchronization notification and stops when requested or when the display sleeps.

WLED discovery and controller information use local HTTP, and RGB output uses unencrypted DDP over UDP. Use a trusted network. A successful UDP send does not confirm receipt or LED output.
