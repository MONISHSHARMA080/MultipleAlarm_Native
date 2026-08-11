# Fix Lag/Flicker when Editing Existing Alarms

The user is experiencing a "slight lag" where clicking an existing alarm briefly shows the "Select Start Time" screen (intended for new alarms) before switching to the full editor. This is caused by a race condition between the initial composition of the `AlarmPickerScreen` (which defaults to "New Alarm" state) and the `LaunchedEffect` that initializes the `ViewModel` with the existing alarm data.

## Proposed Changes

### [Component] Alarm Flow & Picker

#### [MODIFY] [AlarmFlowScreen.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/alarmFeature/ui/alarmFlow/AlarmFlowScreen.kt)
- Collect `uiState` from `AlarmPickerViewModel`.
- Implement an `isSyncing` check to verify if the ViewModel's state matches the provided `alarmData`.
- Suppress rendering of the navigation flow until the synchronization is complete to avoid the flicker.
- Trigger `setInitialAlarmObject` earlier during composition using `remember` to speed up the transition.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Click on an existing alarm from the main list.
- Verify that the full editor appears immediately without any flicker or brief showing of the time picker.
- Click the "+" button to create a new alarm.
- Verify that it still correctly takes you to the "Select Start Time" screen first.
