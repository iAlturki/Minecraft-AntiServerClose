# Copyright (c) 2026 iAlturki — GPL-3.0-only
"""Compare the C++ serializer with independent Python wire encoding."""
import struct
import subprocess
import sys

result = subprocess.run([sys.argv[1]], capture_output=True, text=True, check=True)
frames = [bytes.fromhex(line.split()[1]) for line in result.stdout.splitlines() if line.startswith('ACK_HEX ')]
expected = [(120.25, 63, -75.5, 45, 10), (121.75, 70, -77.5, 60, -10), (-100, 75, 100, -90, 0)]
assert len(frames) == len(expected)
for i, (frame, values) in enumerate(zip(frames, expected), 1):
    assert frame[:2] == bytes([0, i]), ('packet/teleport id', frame.hex())
    assert frame[2:] == struct.pack('>dddff', *values), ('coordinates/rotations', i)
print('PASS: three protocol-777 wire replies and rejection of an unknown initial relative position.')
