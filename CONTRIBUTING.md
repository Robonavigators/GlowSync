# Contributing to GlowSync

Thanks for helping improve GlowSync. Start with [README.md](README.md) for the current state and [AGENTS.md](AGENTS.md) for engineering constraints. The original [PROMPT.md](PROMPT.md) describes the product vision, including work that remains incomplete.

## Development

Use JDK 17 or newer and Android SDK 35. Run:

```sh
python3 -m unittest discover -s tests -v
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest  # with a compatible device or emulator
```

Keep changes focused and update documentation for behavior changes. Include meaningful tests for mapping, protocol, processing and lifecycle changes where practical. Do not claim physical hardware testing or latency results unless you performed them.

## Hardware reports

For compatibility reports, include the TV model, Android version, WLED version, LED count, network setup, steps to reproduce and observed result. Never include screenshots of private content, credentials, IP addresses you do not want public, or captured frames. Explain whether testing used an emulator, a physical TV, WLED hardware, or a combination.

## Pull requests

Describe the problem, the change and the checks run. For UI changes, include screenshots only if they do not contain private content. Do not bypass Android's capture consent or protected-content restrictions.
