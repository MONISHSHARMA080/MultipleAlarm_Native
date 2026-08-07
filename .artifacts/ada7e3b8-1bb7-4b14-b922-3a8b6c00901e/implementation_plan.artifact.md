# Error Strategy and Localization Implementation Plan

This plan outlines the refactoring of the app's error strategy to use localized string resources instead of hardcoded strings, following modern Android best practices.

## User Review Required

> [!IMPORTANT]
> The `Error` interface and all its implementations will change from using `String` to a `UiText` wrapper. This will affect how errors are created throughout the app.

## Proposed Changes

### [Component] Core Utilities & Base Error Strategy

#### [NEW] [UiText.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/com/coolApps/MultipleAlarmClock/utils/UiText.kt)
Create a `UiText` sealed class to handle both hardcoded strings (for developer-facing/internal errors) and localized string resources (for user-facing errors).

#### [MODIFY] [Result.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/com/coolApps/MultipleAlarmClock/utils/Result/Result.kt)
Update the `Error` interface to use `UiText` instead of `String`.

---

### [Component] Localization

#### [MODIFY] [strings.xml](file:///home/monish/code/MultipleAlarmClock/app/src/main/res/values/strings.xml)
Add comprehensive error strings with better wording.

---

### [Component] Error Definitions & Handling

#### [MODIFY] [Errors.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/com/coolApps/MultipleAlarmClock/AlarmLogic/Errors.kt)
Update all sealed classes to use the new `UiText` and reference string resources instead of hardcoded strings.

#### [MODIFY] [ErrorHandler.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/com/coolApps/MultipleAlarmClock/ErrorHandling/ErrorHandling.kt)
Update the handler to resolve `UiText` into actual strings using the Android `Context` before displaying notifications or logging to analytics.

---

### [Component] Business Logic Update

#### [MODIFY] [AlarmsController.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/com/coolApps/MultipleAlarmClock/AlarmLogic/AlarmsController.kt)
Update calls to error constructors to match the new `UiText` signature.

## Verification Plan

### Manual Verification
1.  Deploy the app to an emulator or device.
2.  Trigger a known error path (e.g., trying to set an alarm in the past if not handled by UI validation).
3.  Verify that the notification shows the localized string from `strings.xml`.
4.  Change device language and verify the notification language changes (if translations are added later).
