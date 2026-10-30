# Frequently asked questions

## Does GlowSync work with every streaming app or HDMI input?

No. Android can prevent capture of DRM-protected or secure content, and external inputs are often unavailable to ordinary applications. Dark scenes and blocked capture can look alike. GlowSync does not bypass these restrictions.

## Can it run after I leave the app?

An authorized MediaProjection session runs in a foreground service while Android and the TV permit it. It stops on revocation, explicit stop or screen sleep. A new session needs fresh system consent; it cannot silently restart after process death.

## Does it require cloud services or a subscription?

No. Screen analysis and configuration are local. WLED communication uses the local network and is unencrypted.

## Can I use several controllers?

You can save multiple verified controllers and select one. The current implementation streams to one controller per session.

## Does the diagnostic measure end-to-end latency?

No. Its direct UDP test bypasses capture. Film the screen and LEDs together or use photodiodes to measure physical response; for the complete sync path, show a pattern in a separate app while GlowSync is running. You can store an externally measured value in Settings.
