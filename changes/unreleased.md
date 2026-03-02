# Dev Release Notes

## Release status

This build starts the **dev release track** from functionality that is already in a usable state.

Important: this is still a **dev build**. Some scenarios are not fully validated yet, so specific behaviors may still be inconsistent.

## Supported games (current dev scope)

- Current tested focus: **Assetto Corsa Competizione (ACC)** and **Assetto Corsa EVO (AC EVO)**.
- Manual game selection also includes **Assetto Corsa (AC)**.
- AC EVO still does not provide all telemetry data reliably from the game side.
- Because of this, part of sector tracking is reconstructed manually.
- Sector telemetry is published in integer milliseconds, so displayed sector times may differ by about 1 ms (sub-millisecond drift can exist before rounding).

## Telemetry: recording and storage

- `Settings` includes a **Telemetry acquisition** section.
- Available configuration:
  - recording on/off,
  - sampling rate (Hz),
  - max recorded laps,
  - telemetry storage folder,
  - current telemetry data size.
- Storage warnings are shown for high-risk configurations (high Hz and/or high lap limits).
- Recording is treated as temporary until the session is explicitly saved.
- If recording is stopped or the app is closed before saving, collected telemetry is removed.
- `Settings` includes game selection (`Auto detect` or manual game lock to a supported title).

## Telemetry: viewing and analysis

- **Live** screen shows real-time telemetry stream.
- **Session** screen shows recorded sessions with search, filtering, and sorting.
- Session actions include save, delete, and open details.
- **Session Details** screen shows lap-level and sector-level data (S1/S2/S3), total time, delta, incidents, and lap status.

## HUDs in this dev build

### Input HUD

- Live input overlay with scrolling history for throttle, brake, clutch, and steering.
- Current pedal percentages and steering movement in real time.
- Distinct active/idle session states.
- Configurable display options: line visibility, header/legend, panel width, graph height, and history window.

### Fuel HUD

- Runtime phases: waiting in pits, warmup, predictive, and per-lap.
- Core values: fuel per lap, fuel left, laps left, last lap, and basis.
- Fuel plan table (`Horizon • Time`) with `Now/Max` estimates.
- Reset action clears saved fuel calculations and current HUD state.

## Core user-facing screens in this release note

- **Live**
- **Session**
- **Session Details**
- **Settings**
- **HUD Settings** (from Settings)
- File/folder chooser dialogs used by telemetry storage settings
