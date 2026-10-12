# Copyright (c) 2026 iAlturki — GPL-3.0-only
from datetime import datetime, timezone
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parent
cases = [
    '1970-01-01T00:00:00Z', '1969-12-31T23:59:59.999Z',
    '2024-02-29T12:34:56.123Z', '2026-10-13T07:29:14.1Z',
    '2026-10-13T07:29:14.12Z', '2026-10-13T07:29:14.123Z',
    '2026-10-13T07:29:14.1234Z', '2026-10-13T07:29:14.12345Z',
    '2026-10-13T07:29:14.123456Z', '2026-10-13T07:29:14.1234567Z',
    '2026-10-13T07:29:14.12345678Z', '2026-10-13T07:29:14.123456789Z',
    '2026-10-13T10:29:14.123+03:00', '2026-10-13T03:29:14.123-04:00',
    '2026-10-13T07:29:14.000Z', '2000-01-01T00:00:00Z',
]
epoch = datetime(1970, 1, 1, tzinfo=timezone.utc)
for value in cases:
    delta = datetime.fromisoformat(value).astimezone(timezone.utc) - epoch
    expected = (delta.days * 86400 + delta.seconds) * 1000 + delta.microseconds // 1000
    result = subprocess.run([str(Path(__import__('sys').argv[1])), value], check=True, capture_output=True, text=True)
    actual = int(result.stdout.strip())
    assert actual == expected, (value, actual, expected)
for value in ['not-a-date', '2026-02-30T00:00:00Z', '2026-10-13T07:29:14.Z', '2026-10-13T07:29:14.1234567890Z']:
    result = subprocess.run([str(Path(__import__('sys').argv[1])), value], check=True, capture_output=True, text=True)
    assert int(result.stdout.strip()) == 0, value
print('PASS: 16 independent UTC/fraction/calendar/offset conversions; 4 malformed inputs fail closed.')
