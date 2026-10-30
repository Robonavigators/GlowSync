# GlowSync DDP output

GlowSync uses WLED's RGB24 DDP path on UDP port 4048. It encodes the 10-byte header with version 1, sequence values 1–15, RGB24 type `0x0b`, display destination `1`, a big-endian byte-channel offset, and a big-endian payload length. Payload chunks are at most 1440 bytes and RGB-aligned. The final packet has PUSH set; other packets in the frame do not. The sender serializes all packets for a frame.

The JVM tests cover header bytes, RGB ordering, large offsets, splitting, invalid inputs and loopback transmission. These tests do not validate physical WLED receipt or LED response. Refer to [WLED's DDP documentation](https://kno.wled.ge/interfaces/ddp/) and the WLED implementation when changing packet behavior.
