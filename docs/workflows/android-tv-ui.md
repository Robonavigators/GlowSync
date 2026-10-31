# Android TV UI, remote and accessibility

Inspect `app/src/main/java/com/robonavigators/glowsync/MainActivity.kt` and [REMOTE.md](../REMOTE.md) before editing Jetpack Compose UI.

1. Sketch focus order for every interactive screen: startup, setup, controller selection, calibration, diagnostics, settings, stop and recovery dialogs.
2. Keep each interactive action reachable through Up/Down/Left/Right/Select/Back without touchscreen or mouse. Preserve visible focus and safe Back behavior; never trap focus in a dialog.
3. Check long text, low-resolution panels, overscan, contrast and legibility at typical couch distance. Reduced-motion mode must avoid forced shimmer/continuous effects.
4. Ensure decorative animations, gradients and diagnostics do not block the foreground UI or interfere with the capture worker. Keep heavy processing off Compose recompositions.
5. Run unit/lint/build tests. With an emulator or physical TV, explicitly walk each focus boundary and stop flow using the remote/D-pad. Record which device was used.
6. Use sanitized screenshots for UI PRs; do not record protected/personal TV content. Update [README.md](../../README.md) for exposed behavior changes.

**Stop:** when UI requires ungranted Android access or suggests that secure/HDMI content can always be captured.
