# FieldRelay Sideload Test Plan

Use this for the first APK validation pass on a physical device or emulator.

## Build Artifact

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Variant: debug
- Package: `us.bensley.fieldrelay.debug`

## Preflight

1. Confirm the APK installs cleanly.
2. Confirm the app launches without crashing.
3. Confirm Settings opens from navigation.
4. Enter a valid Spotter Network Application ID and tap Save.
5. Set interval to 30 seconds for the first test pass.
6. Set beacon duration to Indefinite for the first test pass.
7. Enable Resume on boot.

## Permission Flow

1. From Home, tap Start beacon.
2. Grant fine/coarse location.
3. On Android 13+, grant notifications.
4. When prompted for background location, choose the strongest available background/all-the-time option.
5. If background permission is denied, verify the app continues foreground-only and explains that state.

Expected result: the service starts after required permissions are granted.

## Home Screen

1. Confirm Home shows Application ID configured.
2. Confirm the primary button changes to Stop beacon after start.
3. Wait for at least one location fix.
4. Confirm telemetry fields populate: latitude, longitude, speed, heading, elevation.
5. Confirm Last beacon update shows either OK or a clear failure reason.
6. Tap Stop beacon.

Expected result: reporting stops, telemetry clears on service stop, and no crash occurs.

## Duration Modes

1. In Settings, set Beacon duration to 1 hour.
2. Start the beacon from Home.
3. Confirm Home shows an expiry time.
4. Stop the beacon.
5. In Settings, set Beacon duration to Ask me each time.
6. Start the beacon from Home.
7. Confirm the runtime dialog shows only 1 h, 2 h, 4 h, 8 h, 24 h, and Indefinite.
8. Choose Indefinite.

Expected result: finite mode stores an expiry, Ask mode prompts, and Indefinite runs without an expiry.

## Widget

1. Add the FieldRelay widget to the launcher.
2. Confirm it displays latitude/longitude rows, SN/APRS status text, and a WEATHER action area.
3. Tap the left side.
4. Confirm the app opens to Home.
5. Tap the WEATHER side.
6. Confirm the app opens to the Weather tab.
7. Set duration to Ask me each time.
8. Start the beacon from Home and confirm the SN status turns green after state refresh.

Expected result: the widget opens Home from the left side, opens Weather from the right side, and reflects beacon/provider state.

## Notification And Background

1. Start the beacon.
2. Confirm a persistent FieldRelay notification appears.
3. Wait for a successful location report.
4. Confirm notification text includes OK or a clear failure reason.
5. Lock the screen for 5 minutes.
6. Unlock and confirm reporting continued.

Expected result: the foreground service remains active while the screen is off.

## Network Failure

1. Start the beacon.
2. Enable airplane mode.
3. Wait for the next report interval.
4. Confirm the app and notification show a failure reason.
5. Disable airplane mode.
6. Wait for the next report interval.

Expected result: failures are surfaced and the next interval retries without manual restart.

## Boot Resume

1. Start the beacon with duration set to Indefinite.
2. Reboot the device.
3. Wait up to 60 seconds after unlock/boot completion.
4. Confirm reporting resumes.
5. Repeat with a finite duration and verify the original deadline is honored.

Expected result: boot resume starts only when auto-resume is enabled, reporting was on, and the beacon has not expired.

## Logcat Checks

Filter for:

```text
FieldRelay
OkHttp
positions/update
LocationService
```

Expected network success pattern:

```text
POST https://www.spotternetwork.org/positions/update
200
```

## Pass/Fail Notes

Record:

- Device model:
- Android version:
- App version:
- APK SHA-256:
- Spotter Network API success observed: yes/no
- Widget tested: yes/no
- Reboot tested: yes/no
- Background/screen-off tested: yes/no
- Issues found:
