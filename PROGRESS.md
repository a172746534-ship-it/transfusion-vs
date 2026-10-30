# PROGRESS — design overhaul

Read this first after any restart, then docs/DESIGN_OVERHAUL.md and docs/TEST_LOOP.md.

## Done
- Branch `design-overhaul` created from main (5d32b55, v0.4.0).
- docs/DESIGN_OVERHAUL.md and docs/TEST_LOOP.md saved verbatim.

## In progress
- Phase 1: test lab, audit screenshots, tap table, AUDIT.md, baseline SCORECARD.md.

## Next
- Phase 2: three creative directions in a PR. STOP there and wait for the pick.

## Decisions
- CSV export / stats do not exist: the maintainer removed them on 2026-10-28 (Cerner is the
  record) and they are not being re-added. The "export" check in TEST_LOOP is N/A, recorded
  as such in SCORECARD.md and in the final report.
- No local Android SDK: GitHub Actions is the compiler and the emulator lab.
