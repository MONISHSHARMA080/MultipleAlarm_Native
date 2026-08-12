# Fix Stuck Alarm Picker near Midnight

This plan addresses the issue where the Alarm Picker screen gets stuck with a gray "Select" (OK) button, particularly around 11:42 PM or near midnight.

## Problem Analysis

The "stuck" behavior and gray button are caused by several interrelated bugs:

1.  **Faulty Enablement Logic**: The "OK" button in the `StartTime` step is disabled based on `isCandidateInvalid`, which compares the *default* end time with the *initial* start time in the ViewModel. It does not look at the time currently selected in the picker. If the defaults are invalid (e.g., `startTime == endTime`), the button is grayed out immediately, and changing the picker doesn't help because the logic isn't tied to the picker state.
2.  **Same-Day Force Logic**: `createDefaultAlarmObject` forces both start and end times to 23:59 if they cross into the next day. Near midnight (e.g., 11:59 PM), this results in `startTime` and `endTime` both being 23:59, making the range invalid (duration = 0).
3.  **Millisecond Pollution**: `Calendar.set(Calendar.SECOND, 0)` does not clear milliseconds. This can lead to `timeInMillis` comparisons failing even when hours, minutes, and seconds match.
4.  **Picker Desync**: `startTimePickerState` is initialized with the ViewModel's state but doesn't update if the ViewModel state changes (e.g., after `setInitialAlarmObject` runs in a `LaunchedEffect`) because the `remember` key is only tied to `currentProgress`.

## Proposed Changes

### [Component] Domain Model & Utils

#### [MODIFY] [AlarmObject.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/alarmFeature/domain/model/AlarmObject.kt)
- Add a helper to clear seconds and milliseconds from `Calendar`.
- Update `ifTimeIntervalPassedThenReturnRollOver` to roll over if *either* the start time or end time has passed, ensuring the alarm is always in the future.

### [Component] ViewModel

#### [MODIFY] [AlarmPickerViewModel.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/alarmFeature/ui/alarmFlow/alarmPicker/AlarmPickerViewModel.kt)
- Fix `createDefaultAlarmObject` to allow alarms to cross midnight. Instead of forcing 23:59, it should allow the next day.
- Ensure all `Calendar` instances have seconds and milliseconds cleared.

### [Component] UI

#### [MODIFY] [AlarmPickerScreen.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/alarmFeature/ui/alarmFlow/alarmPicker/AlarmPickerScreen.kt)
- Update `isCandidateInvalid` logic to be step-aware.
    - In `StartTime` step: The button should almost always be enabled (the app handles "passed" times by rolling them to tomorrow).
    - In `EndTime` step: Check the current `endTimePickerState` against the `startTime` already set in the ViewModel.
- Tie the `PrimaryActionButton`'s `enabled` state to the active picker's state where appropriate.
- Use a `LaunchedEffect` to sync `startTimePickerState` with `uiState.alarmObject.startTime` when it changes from the ViewModel (e.g., after initialization).

## Verification Plan

### Manual Verification
1.  **Midnight Test**: Set device time to 11:58 PM and open the app. Verify the "OK" button is enabled and the picker shows a valid range (e.g., 11:59 PM to 12:43 AM tomorrow).
2.  **Range Test**: In Step 1 (Start Time), pick 11:00 PM. In Step 2 (End Time), pick 10:00 PM. Verify the button grays out and shows an error message.
3.  **Roll Over Test**: Open the app at 11:42 PM. Wait until 11:43 PM without clicking anything. Verify the start time rolls over to 11:43 PM tomorrow (or today if end time allows).
