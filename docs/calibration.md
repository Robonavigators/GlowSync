# LED layout and calibration

Count LEDs along the top, right, bottom and left of the screen. Set a missing side to zero. The default path starts near the top-left and proceeds clockwise across the top, down the right, across the bottom and up the left. Physical first LED and direction depend on the installation.

1. Verify the selected controller via HTTP and stop active sync.
2. Send the one-LED marker repeatedly and observe the strip. Each action sends a local UDP frame; receipt is not acknowledged.
3. Set counts, starting corner, reversal and offset to match the actual ordering. Save changes.
4. Start a permitted capture session. Adjust brightness, saturation, edge depth, smoothing, black-bar handling, white balance and per-side RGB correction. Setup offers the primary controls; the Calibration page offers the remaining adjustments.
5. Check a range of supported scenes, including dark scenes. Black-bar detection is conservative and can still be imperfect.

The LED-colour preview represents calculated output, not a full captured screen or confirmation that the LEDs changed. [Examples](../examples/README.md) are illustrative, not hardware reports.
