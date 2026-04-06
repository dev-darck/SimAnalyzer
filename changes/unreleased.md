# Dev Release Notes

## Release status

This dev build expands the telemetry workflow with **Session Analysis**, a dedicated workspace for reviewing recorded
sessions in more detail.

The current build is usable for everyday review, but it is still part of the dev release track. Some scenarios still
need wider validation across more sessions, cars, and tracks.

## What is new in this build

### Session Analysis

- Added a dedicated **Session Analysis** screen for recorded telemetry sessions.
- Added a multi-pane desktop workspace built around the main review flow:
  - session header,
  - interactive track map,
  - lap and session navigator,
  - telemetry comparison graph,
  - inspector with analysis tabs.
- Added selected-lap versus reference-lap comparison across the workspace.
- Added interactive track review features such as focus mode, reference line display, corner overlays, diagnostic
  markers, and sector-aware context.
- Added inspector sections for line review, timing review, tyre review, setup advice, corner breakdown, and highlights.

### Analysis and coaching

- Added a higher-level analysis layer that combines telemetry, lap deltas, corner behaviour, tyre state, and consistency
  signals into one review flow.
- Added overview highlights for major telemetry events and obvious time-loss areas.
- Added corner diagnostics and corner-specific coaching cues.
- Added tyre temperature window analysis so overheating and under-temperature behaviour can be surfaced in review.
- Added setup-oriented diagnostic output that can distinguish between likely driving issues and likely car-balance
  issues.
- Added consistency analysis to surface repeatability issues instead of only showing single-lap pace.

### Track map and session data quality

- Added track-map merge logic so the analysis can use the best available geometry from recorded data, authored maps, and
  imported data.
- Added corner-zone detection and map-aware sector context to support review on tracks with incomplete source data.
- Added graceful fallback behaviour when imported track data or calibration data is unavailable.
- Added logging around degraded track-map and calibration loading paths to make failures diagnosable.

### Telemetry workflow

- **Live** screen continues to show real-time telemetry stream.
- **Session** screen continues to provide recorded session browsing with search, filtering, and sorting.
- **Session Details** continues to show lap-level and sector-level review for recorded sessions.
- **Session Analysis** now extends that workflow with a deeper comparison-oriented review screen.

## HUDs available in this dev build

### Input HUD

- Live input overlay with scrolling history for throttle, brake, clutch, and steering.
- Current pedal percentages and steering movement in real time.
- Distinct active and idle session states.
- Configurable display options for visibility, panel width, graph height, and history window.

### Fuel HUD

- Runtime phases for waiting in pits, warmup, predictive mode, and per-lap mode.
- Core values for fuel per lap, fuel left, laps left, last lap, and calculation basis.
- Fuel plan table with `Horizon` and `Time` estimates.
- Reset action that clears saved fuel calculations and current HUD state.

## Settings and recording

- `Settings` includes telemetry acquisition controls.
- Available configuration includes recording on or off, sampling rate, max recorded laps, storage folder, and current
  telemetry data size.
- Storage warnings are shown for high-risk configurations such as very high sampling rates or large lap limits.
- Recording remains temporary until the session is explicitly saved.
- If recording is stopped or the application is closed before saving, the temporary telemetry payload is removed.
- Game selection is available through `Auto detect` or manual selection for a supported title.

## Supported games in the current dev scope

- Current tested focus: **Assetto Corsa Competizione (ACC)** and **Assetto Corsa EVO (AC EVO)**.
- Manual game selection also includes **Assetto Corsa (AC)**.
- AC EVO still does not provide all telemetry data reliably from the game side.
- Because of this, part of sector tracking is reconstructed manually.
- Sector telemetry is published in integer milliseconds, so displayed sector times may differ by about 1 ms after
  rounding.
