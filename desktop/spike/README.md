# Step 0, the spike

docs/DESKTOP_APP.md §7 step 4; what it proves and its results are in desktop/docs/CHANGE_PLAN.md §12.
Runs only with `[spike]` in the commit message (`.github/workflows/desktop-spike.yml`). Throw-away code:
this folder and its workflow are deleted before the final full run.

- `port`: the reference's `ui/` and alarm composables, copied unchanged from `app/` at build time; every compile error is recorded.
- `room`, `roomplugin`: Room 2.8.5 on a Kotlin JVM module with the bundled SQLite driver.
- `probe`: Compose Desktop 1.12.2 APIs the plan relies on.
- `win`, `wix`, `ps`: a Windows prototype of the integrations, its MSI, and the checks run on windows-2022 and windows-2025.
