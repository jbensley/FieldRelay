#!/usr/bin/env python3
"""Generate an APRS-IS passcode for a callsign.

This utility is intentionally outside the Android app. FieldRelay requires users
to enter their callsign and APRS-IS passcode explicitly in app settings.
"""

from __future__ import annotations

import argparse


def aprs_is_passcode(callsign: str) -> int:
    base = callsign.split("-", 1)[0].strip().upper()
    code = 0x73E2
    for index, char in enumerate(base):
        value = ord(char)
        code ^= value << 8 if index % 2 == 0 else value
    return code & 0x7FFF


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Generate an APRS-IS passcode for a callsign.",
    )
    parser.add_argument("callsign", help="Amateur radio callsign, optionally with SSID")
    args = parser.parse_args()
    print(aprs_is_passcode(args.callsign))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
