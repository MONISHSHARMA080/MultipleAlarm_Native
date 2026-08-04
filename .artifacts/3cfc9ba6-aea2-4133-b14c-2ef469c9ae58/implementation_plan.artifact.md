# Remove Date List Card and integrate Date Selection into Settings Card

The goal is to simplify the UI by removing the horizontal date list and instead placing the date selection as a row within the settings card, right below the frequency setting. This row will be clickable and will open a calendar picker.

## Proposed Changes

### [Alarm Feature]

#### [MODIFY] [AlarmPickerScreen.kt](file:///home/monish/code/MultipleAlarmClock/app/src/main/java/MultipleAlarmClock/alarmFeature/ui/alarmFlow/alarmPicker/AlarmPickerScreen.kt)
- Remove the `DateList` component call.
- Add `showCalendar` state to manage the visibility of the date picker.
- Add a new `SettingRow` for the Date selection within the settings card, placed after `FrequencyRow`.
- The Date row will display the currently selected date formatted as "EEE, MMM d, yyyy" (e.g., "Wed, Aug 5, 2026").
- The Date row will use the `Icons.Rounded.CalendarToday` icon.
- Integrate `DatePickerModal` (from `listOfDates.kt`) to handle date selection.
- Adjust spacers to maintain a balanced layout after removing the large `DateList` component.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Run existing UI tests if available (none specified in context, but build check is primary).

### Manual Verification
- Deploy to an emulator/device.
- Verify that the horizontal date list is gone.
- Verify that the new "Date" row appears below "Frequency".
- Click on the "Date" row and verify that the calendar picker opens.
- Select a date and verify that it updates the displayed date in the row.
- Verify that the layout remains adaptive and balanced on different screen heights.
