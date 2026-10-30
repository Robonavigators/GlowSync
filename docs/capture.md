# Screen capture and lifecycle

GlowSync requests Android MediaProjection consent and starts a media-projection foreground service. On supported Android versions it requests capture of the default display. An active session may continue after the activity leaves the foreground while Android and the TV permit it. The user can stop from the app or foreground notification; sleep and projection revocation end the session. A fresh system prompt is needed for a new session, including after process death.

The service acquires the latest ImageReader image, samples a low-resolution image and closes it. It does not record video, save screenshots, or upload frames. Android may block DRM-protected media, secure windows and some HDMI inputs. A dark frame cannot reliably identify why capture is black; optional dark-frame behavior also affects genuinely dark scenes.

The service handles supported captured-content resize callbacks using the existing virtual display. Firmware behavior, revocation, sleep, rotation and background operation still need verification on each physical TV. See [privacy.md](privacy.md) and [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
