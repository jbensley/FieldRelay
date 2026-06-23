# Spotter Network Reporting API Plan

This is V2 planning for restoring the Report tab with real submission behavior.

## Source

Primary API blueprint:

- `https://jsapi.apiary.io/apis/spotternetwork.apib`
- Downloaded local reference: `docs/spotternetwork-api.apib`
- Blueprint metadata endpoint reported `lastUpdated=2026-04-30T02:35:11.687Z`.

Primary reporting criteria:

- `https://www.spotternetwork.org/pages/reporting-guidelines`

The JavaScript-rendered documentation page at `https://spotternetwork.docs.apiary.io/` points to the same API but is not directly readable without JavaScript.

## Authentication / Eligibility

`POST /login` accepts:

```json
{
  "username": "xxxxx",
  "password": "xxxxx"
}
```

Success response includes:

```json
{
  "success": true,
  "id": "APPLICATION-ID",
  "marker": "PUBLIC-ID",
  "CanReport": true
}
```

Implementation note: the app currently asks users to paste an Application ID directly. For report submission, we need either:

- Continue using the pasted Application ID and let the report endpoint accept/reject it.
- Add optional login/validation flow to confirm `CanReport=true` before enabling report submission.

Do not store Spotter Network username/password in v2 unless explicitly designed.

## Severe Report Submission

Endpoint:

```text
POST https://www.spotternetwork.org/report/severe
Content-Type: application/json
```

Documented request body:

```json
{
  "id": "APPLICATION-ID",
  "report_type": "S",
  "stamp": "YYYY-MM-DD HH:MM:SS",
  "tornado": 0,
  "funnelcloud": 0,
  "wallcloud": 0,
  "rotation": 0,
  "hail": 0,
  "wind": 0,
  "flood": 0,
  "flashflood": 0,
  "other": 1,
  "hailsize": 0,
  "windspeed": 0,
  "windmeasure": 0,
  "stamp_exact": 0,
  "damage": 0,
  "injury": 0,
  "narrative": "test test test",
  "lat": 39.763819,
  "lon": -105.2217393,
  "gps": 1,
  "nwschat": 1,
  "twitter": 1
}
```

Documented response:

```json
{}
```

Because the documented success body is empty, implementation should treat HTTP 2xx as success and surface non-2xx responses as failures, preserving response text where available.

## Severe Fields

Boolean flags use integer `0`/`1`:

- `tornado`
- `funnelcloud`
- `wallcloud`
- `rotation`
- `hail`
- `wind`
- `flood`
- `flashflood`
- `other`
- `stamp_exact`
- `damage`
- `injury`
- `gps`
- `nwschat`
- `twitter`

Numeric detail fields:

- `hailsize`: inches
- `windspeed`: miles per hour
- `windmeasure`: docs say `0 = estimated, 1 = exact`, but `/reports` examples include `2`; treat as an enum that needs live validation before final UI wording.

Text/location fields:

- `id`: Application ID
- `report_type`: `"S"` for severe
- `stamp`: UTC or server-expected timestamp format `YYYY-MM-DD HH:MM:SS`
- `narrative`: short narrative. The API accepts text, but the app should generate this from structured selections rather than exposing a free-text field.
- `lat`, `lon`: decimal degrees

## Spotter Network Reporting Criteria

The app should enforce or warn on Spotter Network's published reporting guidelines, not merely expose every API field.

Minimum severe reporting criteria:

- Tornado and rotating wall-cloud reports are allowed.
- All hail reports are allowed. There is no Spotter Network minimum hail size on the current guidelines page.
- Wind reports must be measured wind speeds and/or gusts greater than 50 mph.
- Hydro reports are allowed for severe hydro events such as flooding or flash flooding. Reports should include impacts and use a minimum water depth of 4 inches.
- Storm damage reports are allowed when notable, current-storm-generated, directly observed, and described with quantifiable measures.
- Tropical reports are allowed only when they have significant impact.

Explicitly not allowed:

- Test reports.
- Relay reports; reports must be first-hand.
- Reports delayed more than 20 minutes.
- Lightning reports.
- Power flashes as damage.
- Heat reports.
- Clear-sky reports.
- "Storm is getting stronger/weaker" reports.
- Fog reports.
- Radar-based reports.
- Reports heard on scanner, ham radio, TV, etc.
- Rain measurements unless they constitute flood conditions.
- General information such as a storm crossing a border.
- Winter reports as of spring 2022; Spotter Network directs users to CoCoRaHS or mPING for winter weather.

Implementation implications:

- Do not include a "test report" mode that submits to production.
- Do not offer winter report submission in the app.
- Do not allow lightning-only reports.
- Do not allow radar/relay report source choices.
- Require the user to confirm the report is first-hand and current.
- Reject or strongly block reports older than 20 minutes.
- For wind, require speed greater than 50 mph and a measured/observed gust workflow. The API has a `windmeasure` field, but the UI should not encourage estimated wind below or at the reporting threshold.
- For flood/flash flood, ask for water depth and generate a concise narrative. Block hydro reports below 4 inches in the first version.
- For damage/other/injury, generate a concise narrative from structured selections until more detailed criteria-specific controls are added.

## Recent Reports Read API

Endpoint:

```text
POST https://www.spotternetwork.org/reports
```

Request:

```json
{
  "id": "APPLICATION-ID"
}
```

Response contains `reports`, each with identity/location/time fields plus severe and winter flags. Severe fields mirror the submit request. Winter fields are returned but not documented as a submit request.

## Winter Reporting Gap / Do Not Implement

The blueprint says `report_type: "S"` means severe and `"W"` means winter, and `/reports` returns winter fields including:

- `snow`
- `sleet`
- `FreezingRain`
- `temp`
- `newsnowfall_w`
- `newsnowfall_f`
- `newsnowfall_past_hr`
- `totalsnowfall_w`
- `totalsnowfall_f`
- `snow_depth`
- `blowing`
- `drifting`
- `depth`
- `distance`
- `newice_w`
- `newice_f`
- `newice_past_hr`
- `totalice_w`
- `totalice_f`
- `icy`
- `slushy`
- `snow_covered`
- `power`
- `trees`

However, the downloaded blueprint does **not** document a `POST /report/winter` endpoint or winter submit request body.

The public reporting guidelines also state that winter reports are not allowed as of spring 2022 and direct users to CoCoRaHS or mPING instead.

Recommendation: implement severe reports only. Do not implement Spotter Network winter report submission unless Spotter Network publishes updated criteria allowing it and confirms the submit schema.

## Proposed App Components

Networking:

```text
data/api/SpotterNetworkApi.kt
data/model/SevereReportRequest.kt
data/model/ReportSubmitResponse.kt
data/model/RecentReportsRequest.kt
data/model/RecentReportsResponse.kt
```

UI:

```text
ui/report/ReportViewModel.kt
ui/screens/ReportScreen.kt
```

Pure helpers/tests:

```text
report/SevereReportDraft.kt
report/SevereReportValidator.kt
report/SevereReportFactory.kt
```

## UI Recommendation

Reintroduce the Report tab only when it can submit real severe reports.

Initial severe report form:

- Report types checklist: tornado, funnel cloud, wall cloud/rotation, hail, measured wind, flood, flash flood, notable damage, tropical impact, other.
- Conditional inputs:
  - Hail checked -> hail size in inches.
  - Wind checked -> measured gust/speed in mph. Must be greater than 50 mph.
  - Flood/flash flood checked -> water depth in inches.
- Damage/injury toggles.
- Time exact toggle; default exact when reporting "now".
- Generated narrative only; no user-entered free text in the first version.
- Location preview from latest GPS fix, with a warning if unavailable.
- Tornado/funnel/wall-cloud reports may use a structured offset from the reporter: preset distance plus cardinal/intercardinal direction. The app computes a report lat/lon from that offset using a simple short-distance approximation and generates the narrative.
- NWSChat/Twitter toggles default off until user explicitly opts in.
- Review/confirm dialog before submit.

Validation:

- Require Application ID.
- Require a current or manually accepted location.
- Require at least one phenomenon flag.
- Require first-hand confirmation.
- Require timestamp no more than 20 minutes old.
- Do not expose free-text narrative in the first version. Generate narrative from structured report details.
- Require positive hail size when `hail=1`; no minimum threshold because Spotter Network allows all hail reports.
- Require measured wind speed greater than 50 mph when `wind=1`.
- Block lightning-only, radar-only, relay, delayed, clear-sky, fog, heat, and generic storm-status reports.

## Testing Plan

Unit tests:

- Severe request serialization names and integer booleans.
- Timestamp formatting.
- Validation for missing App ID, missing location, no selected phenomenon, invalid hail/wind/flood details, stale report time, missing first-hand confirmation, and disallowed report categories.
- Factory fills `lat`, `lon`, `gps`, `stamp`, and default integer flags correctly.

Integration tests:

- Add a skipped-by-default live API test similar to `SpotterNetworkApiTest`.
- Require explicit `-PappId`, location, and an opt-in flag such as `-PsubmitReport=true` so accidental report submission is difficult.

Manual tests:

- Submit a harmless test only if Spotter Network has a documented testing practice or a verified non-public/sandbox path. Otherwise avoid live test submissions without operator approval.
