# Preview and release checklist

A release is not validated just because an APK builds.

1. Freeze intended scope and document exact version, commit, supported/untested hardware, permissions, privacy and known limitations in README/release notes.
2. Run `python3 -m unittest discover -s tests -v` and `./gradlew testDebugUnitTest lintDebug assembleDebug`, then inspect [Android CI](../../.github/workflows/android.yml). Do not ignore failing checks.
3. With available hardware test installation, launch, remote controls, permission consent/denial, start/stop, DDP diagnostics, controller connection, mapping and failures.
4. Verify no signing keys, private configs, captured frames, tokens or network identifiers are committed or included in publicly distributed files.
5. Inspect APK metadata and artifacts. Ensure an actual downloadable file exists before advertising an APK download; distinguish debug artifacts from signed release builds.
6. Review screenshots and website/README claims. Retain [GlowSync website](https://robonavigators-io.vercel.app/GlowSync/) and [Robonavigators](https://robonavigators-io.vercel.app/) links.
7. Obtain maintainer review for security-sensitive changes and confirm the version tag/release points to intended commit.
8. Publish truthful release notes: tested environment, features, limitations, changes, install path and how to report bugs.

**No-go conditions:** broken core tests; unresolved capture/privacy regression; nonfunctional stop; fabricated physical validation; missing advertised APK; secret exposure. Early previews must be labeled early previews.
