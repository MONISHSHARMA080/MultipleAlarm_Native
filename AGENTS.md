# Multiple Alarm Clock — agent instructions

## Workflow rules

- don't write comments unless it's not obvious what the code, if I can read and tell then I don't need comments
- If your assumptions about the solution differ significantly (>65%), stop and ask before proceeding — unless the user wouldn't know either.
- Use the latest docs/APIs, not outdated ones.
- Follow modern app architecture: layered + adaptive, unidirectional data flow (UDF) in all layers, UI state holders, coroutines/flows, DI best practices.
- Never run git commands except to read files from older commits — no commits, pushes, or git config changes; the user handles those.

## Architecture

- my app allows setting multiple/repeating alarms, it does that by, creating the first alarm and when it rings it checks if it need to set next alarm and the cycle continues, it can set an alarm from 5:00 -> 6:00 am every 1 min, also on every week days

- do not write or run tests unless I tell you to
