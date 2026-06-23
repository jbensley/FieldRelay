# FieldRelay Android

FieldRelay is an Android app for field location beaconing and structured weather reporting. It is currently focused on storm spotter workflows, with support for Spotter Network and APRS-IS position reporting.

The app is intentionally bare-bones: configure the services you want to use, start a beacon, and keep useful location/status information visible in the app and home-screen widget.

## Current Status

FieldRelay is in active local development and has working debug builds. It has been tested enough to confirm:

- Spotter Network position reporting is implemented.
- Spotter Network severe weather report submission is implemented with structured fields.
- APRS-IS position transmission is implemented.
- APRS-IS accepts an explicitly configured callsign and passcode pair.
- APRS packets have been observed on aprs.fi after beaconing.
- The app supports a home-screen widget with GPS and provider status.

Remaining validation is still needed across more devices, longer beacon sessions, reboot behavior, permission edge cases, and live Spotter Network weather report submission.

## App Identity

- App name: `FieldRelay`
- Android package / application ID: `us.bensley.fieldrelay`
- Debug application ID: `us.bensley.fieldrelay.debug`
- Minimum Android version: Android 8.0, API 26
- Target / compile SDK: 36

## Features

### Position Beaconing

FieldRelay uses a foreground location service to send GPS fixes to enabled providers at the configured interval.

Supported position providers:

- Spotter Network
- APRS-IS

The beacon can run for a selected duration or indefinitely. The app also supports an "Ask me each time" default so a duration can be chosen when starting the beacon.

Position beaconing and weather reporting are separate capabilities. APRS-IS can be used for position beaconing without enabling the Weather Report tab.

### Spotter Network

Spotter Network support includes:

- Application ID storage
- Position updates through `POST /positions/update`
- Structured severe weather reports through `POST /report/severe`

The Application ID is treated as a secret in the UI and is not displayed in plain text once saved.

### APRS-IS

APRS-IS support includes:

- Callsign / SSID configuration
- APRS-IS callsign and passcode configuration
- TCP transmission to APRS-IS
- Position packets with GPS location, course, speed, altitude, symbol, and comment
- Optional payload fields for listening frequency, CTCSS/DCS tone, repeater offset, range, and comment

Normal users should only need to enter their callsign and payload details. APRS server/port and symbol settings are hidden behind an advanced settings toggle and should usually be left at defaults.

Default APRS-IS server:

```text
rotate.aprs2.net:14580
```

### Weather Reporting

The Weather Report tab uses structured inputs instead of free text. Current report categories include tornado/funnel/wall cloud, hail, wind, flooding, damage, injury, and other severe impacts.

Report location defaults to the current GPS fix. A location offset can be enabled for nearby events, using distance and direction rather than free-form location text.

The Weather Report tab is only shown when at least one weather-reporting provider is configured. Today that means a saved Spotter Network Application ID. Future providers such as mPING should be added behind the same provider-capability check instead of being hard-coded directly into the UI.

### Home Screen

The Home screen shows:

- Beacon on/off status
- Configured provider status
- Last GPS position
- Decimal or arc/DMS coordinates
- Maidenhead grid locator
- Speed, heading, and elevation
- Beacon expiration time when applicable

Latitude/longitude display preferences and elevation units are persisted.

### Widget

The widget shows:

- Latest GPS position
- Provider status dots for Spotter Network and APRS
- A Home area that opens the app to the Home tab
- A Weather area that opens directly to Weather Report when a weather provider is configured

## Repository Notes

This repository is moving toward a normal GitHub pull request workflow. Current development has been local, with checkpoint commits on `master`.

Agent-facing working notes should live in `PLAN.md`. That file is intentionally ignored by Git and is meant to be recreated or updated locally during active development.

## Documentation

Additional planning and research documents:

- `docs/APRS_INTEGRATION_PLAN.md`
- `docs/SPOTTER_REPORTING_PLAN.md`
- `docs/SIDELOAD_TEST_PLAN.md`

These documents may contain implementation notes and historical context, but this README is the human-facing project overview.
