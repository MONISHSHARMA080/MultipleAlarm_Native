# Multiple Alarm Clock — agent instructions

## Workflow rules
- Make components adaptive; activate the `adaptive` skill when working on UI.
- Don't take shortcuts or shoehorn solutions. don't write comments unless it's not obvious what the code does
- If your assumptions about the solution differ significantly (>65%), stop and ask before proceeding — unless the user wouldn't know either.
- Use the latest docs/APIs, not outdated ones.
- Follow modern app architecture: layered + adaptive, unidirectional data flow (UDF) in all layers, UI state holders, coroutines/flows, DI best practices.
- Never run git commands except to read files from older commits — no commits, pushes, or git config changes; the user handles those.

## Project layout
- Gradle modules: `:app` (the Android app) and `:baselineprofile` (macrobenchmark for baseline profiles). Root Gradle project is named "Trying_native".
- `backend/` is a separate Node/TypeScript Firebase Functions project (PostHog-cohort → FCM push cron). It is NOT part of the Gradle build and deploys independently of the app. Details in `backend/architect.md`.
- Toolchain: AGP 9.3.3, Kotlin 2.4.20, JDK 21, minSdk 33, targetSdk/compileSdk 37 (no instrumentation tests — all tests are Robolectric/JVM).

## Commands
- Build: `./gradlew :app:assembleDebug`
- Unit tests (Robolectric, no device required): `./gradlew test` for alarmController change only, dont run test for the ui changes just see if the code compiles
- `-PskipPostHog=true` blanks the PostHog build-config key (used for the CI baseline-profile job); PostHog is a no-op in non-release variants regardless.

## Codegen — don't hand-edit generated output
- Room: schemas export to `app/schemas/` (`AlarmDatabase`); migrations live in `data/local/migration.kt`. Hilt and Room both run via KSP only — no kapt.
- Protobuf: `app/src/main/proto/settings.proto` generates code at build time for the DataStore preferences. The build forces protobuf-kotlin-lite and excludes the `protobuf-lite` module from all configurations — keep that exclusion when touching dependencies.

## Architecture
- Layers under `app/src/main/java/com/coolApps/MultipleAlarmClock/`: `presentation/` (Compose screens + navigation), `domain/` (use cases, repository interfaces, models), `data/` (Room, DataStore, billing/RevenueCat, background), `di/AppModule.kt` (Hilt).
- Alarm scheduling core lives in `receiver/` (`AlarmReceiver`, `AlarmService`, `BootReceiver`, `ScheduledNotificationReceiver`, FCM service). These are manifest-registered; `BootReceiver` re-schedules alarms after reboot — preserve that when touching scheduling.
- Both `androidx.navigation` (compose) and the newer `androidx.navigation3` are on the classpath; check which one a screen actually uses before changing navigation.

Because of this, keep commit messages conventional (`feat:`, `fix:`) — the next release version depends on them (the user does the actual commits).
