# SwiftPDF Home Functional Pass - 2026-05-23

## Build

- `./gradlew.bat :app:assembleDebug` passed.
- `./gradlew.bat :app:testDebugUnitTest` passed.
- Debug APK installed and launched on `emulator-5554`.

## Fixes Made

- Added real behavior for the Home `Sort` button.
- `Sort` now opens a dialog with:
  - `Newest first`
  - `Oldest first`
  - `Cancel`

## Tested Home Functions

- Recent files list:
  - Home displayed `SwiftPDF_scan_20260523_075042.pdf` in Recent files.

- Search:
  - Entered a matching query.
  - Home switched from `Recent files` to `Search results`.
  - Matching recent PDF remained visible.
  - Non-matching query showed the empty result state.

- Sort:
  - Tapped `Sort`.
  - Verified `Sort recent files` dialog opened.
  - Verified `Oldest first` option applied and dialog dismissed.

- Recent file open:
  - Tapped the visible recent PDF row.
  - Reader opened the file and rendered `Page 1 of 2`.

- Recent file actions:
  - Tapped the row more/actions button.
  - Verified `File actions` dialog opened with `Tools`, `Remove`, and `Open`.
  - Tapped `Tools`.
  - Verified navigation to `PDF Toolbox` with the selected PDF active.

- Home quick tools:
  - Tapped `Image to PDF`.
  - Verified navigation to `Image to PDF` flow.
  - Tapped `Sign PDF`.
  - Verified navigation to `PDF Toolbox` with the selected PDF active.

## Unit Coverage

- `DocumentLibraryViewModelTest` covers recent file removal and clear-history behavior.
- I did not execute `Remove` in the emulator during this pass to avoid clearing the current test history.

## Screenshot Evidence

- `test-reports/screenshots_home_functional_2026-05-23/home_initial.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_sort_dialog.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_search_scan2.png`
- `test-reports/screenshots_home_functional_2026-05-23/reader_after_recent_row_tap.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_actions_after_more_tap.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_actions_tools_exact_result.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_quick_image_to_pdf_navigation.png`
- `test-reports/screenshots_home_functional_2026-05-23/home_quick_sign_navigation.png`
