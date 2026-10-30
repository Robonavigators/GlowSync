# Maintainer notes

Review pull requests for accurate behavior and test claims, Android screen-capture consent and lifecycle, local-only frame handling, and correct DDP framing. Verify that docs distinguish HTTP reachability, local UDP sends, and physical LED output. Preserve the MIT license. Hardware compatibility reports should identify the actual TV, Android build, WLED firmware and test conditions without publishing private frames or network details.

The Android workflow runs unit tests, lint and a debug build; instrumentation requires a compatible emulator or device and should be reported separately. There is no automated maintainer bot configured in this repository.
