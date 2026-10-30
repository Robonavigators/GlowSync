# Performance and measurement

GlowSync samples low-resolution ImageReader frames and keeps only the latest pending LED frame. The adaptive rate controller reduces its cap under sustained processing or send pressure and slowly recovers; analysis resolution changes conservatively. Eco, Balanced, Fast and Ultra are targets, not promises.

The dashboard separates processing time, DDP encoding time, UDP call time, and age since acquisition. None measures physical LED output, and UDP has no acknowledgement. No physical screen-to-LED benchmark has been completed for this project.

To measure visible response, film the TV and LEDs with a high-speed camera, or compare photodiodes on an oscilloscope. Settings provides a direct-UDP pattern that bypasses capture; compare its screen and LED transitions externally. For full sync latency, run GlowSync and display a test pattern in another permitted app. Record device model, frame rate, network, strip length and method alongside any result.
