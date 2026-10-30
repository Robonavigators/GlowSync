# LED layout examples

These are illustrative mappings, not tested device reports and not importable configuration files. Enter counts and settings in GlowSync's calibration screen, then verify the physical strip with its marker test.

| Layout | Top | Right | Bottom | Left | Total |
|---|---:|---:|---:|---:|---:|
| Three sides | 30 | 18 | 0 | 18 | 66 |
| Four sides | 30 | 18 | 30 | 18 | 96 |

The default path starts at the top-left and travels clockwise across the top, down the right, across the bottom, and up the left. Physical installation direction and first LED differ: use reverse, starting corner and offset to align the strip. Test one LED at a time; an unacknowledged UDP send is not proof that the strip displayed it.

For verified device reports, use the [hardware report template](../docs/devices/REPORT_TEMPLATE.md). No physical TV/WLED compatibility reports are included yet.
