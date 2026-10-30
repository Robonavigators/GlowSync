# Do and don't

## Do

- Use Android's system screen-capture prompt for every new projection session, and stop synchronization when requested.
- Test the physical strip with the marker and confirm layout, offset and direction before relying on ambient sync.
- Use a trusted network for the TV and WLED. Local HTTP and DDP are unencrypted.
- Document the exact devices, firmware, test conditions and measurement method when reporting results.
- Treat dark or black capture as ambiguous: it could be a dark scene, protected content, an inaccessible input or a device limitation.

## Don't

- Attempt to bypass DRM, secure windows or Android permission prompts.
- Treat a UDP send or `/json/info` response as confirmation of physical LED output.
- Upload captured screens, private content, credentials or private network details with issue reports.
- Present app-side timing or the direct-UDP response diagnostic as measured end-to-end screen-to-LED latency.
- Assume a profile's FPS target is a guarantee on every TV, network or controller.
