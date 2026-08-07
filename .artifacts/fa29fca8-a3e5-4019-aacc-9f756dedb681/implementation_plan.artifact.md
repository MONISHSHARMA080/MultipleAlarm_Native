# Error Handling Strategy Improvement and Localization

This plan outlines the steps to localize error messages and improve them according to Apple's Human Interface Guidelines (HIG) for writing. The goal is to make errors concise, direct, and "perceivable at a glance" by removing polite but unnecessary words like "sorry" and "please", and focusing on the issue and solution.

## User Review Required

> [!IMPORTANT]
> The `Error` interface will be changed to use a new `UiText` wrapper instead of a raw `String`. This affects all classes implementing `Error`.
> Error messages will be moved from hardcoded Kotlin strings to `strings.xml`.

## Proposed Changes

### Core Infrastructure

#### [NEW] [UiText.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/utils/UiText.kt)
Create a `UiText` sealed class to handle both string resources and dynamic strings, allowing for easy localization and context-aware string resolution.

#### [MODIFY] [Result.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/utils/Result/Result.kt)
Update the `Error` interface to use `UiText` for `messageToDisplayUser`.

#### [MODIFY] [strings.xml](file:///home/monish/code/MultipleAlarmClock/app/src/main/res/values/strings.xml)
Add new, HIG-compliant error strings.

### Error Definitions

#### [MODIFY] [Errors.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/AlarmLogic/Errors.kt)
Refactor error classes to use `UiText.StringResource` with the new strings.

### Error Display

#### [MODIFY] [ErrorHandler.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/ErrorHandling/ErrorHandling.kt)
Update `ErrorHandler` to resolve `UiText` using `Context` and improve the default notification title.

### Implementation Call Sites

#### [MODIFY] [AlarmsController.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/AlarmLogic/AlarmsController.kt)
Update hardcoded error message instantiations to use `UiText`.

## Verification Plan

### Automated Tests
- Build the project to ensure all `Error` interface changes are correctly propagated.
- Run existing tests (if any) to verify logic remains sound.

### Manual Verification
- Trigger various error conditions (e.g., setting an alarm in the past, DB failure simulation) and verify the notifications show the new, localized, and HIG-compliant messages.
- Verify that the notification title is also improved and localized.
