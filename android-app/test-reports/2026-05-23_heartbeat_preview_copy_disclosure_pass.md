# 2026-05-23 Heartbeat - Preview Copy Disclosure Pass

## Scope

- Gap checked: export tools generated raster/preview-based PDF copies, while UI copy could imply text-preserving PDF editing.
- Change made: Export page now states that compress, split, merge, and sign create preview PDF copies in this MVP.
- Change made: export operation labels and success messages now say "preview copy" for generated PDFs.

## Functional Verification

- Command: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease --stacktrace`
- Result: `BUILD SUCCESSFUL`
- Unit tests:
  - `FileSizeFormatTest`: 4 tests, 0 failures, 0 errors
  - `SignatureMarkTest`: 4 tests, 0 failures, 0 errors

## APK Verification

- Command: `apksigner verify --verbose app\build\outputs\apk\release\app-release.apk`
- Result: verified with APK Signature Scheme v2, 1 signer.
- Debug APK: `C:\Users\mobu\Desktop\Trae\Apps\PDFAPPs\android-app\app\build\outputs\apk\debug\app-debug.apk`
  - Size: 29,307,928 bytes
  - Last write: 2026-05-23 10:59:15
- Release APK: `C:\Users\mobu\Desktop\Trae\Apps\PDFAPPs\android-app\app\build\outputs\apk\release\app-release.apk`
  - Size: 17,265,797 bytes
  - Last write: 2026-05-23 10:59:27

## High-Fidelity UI Verification

- Device: `emulator-5554`
- Flow: install debug APK, cold launch `com.swiftpdf.app/.MainActivity`, open Export tab.
- Screenshot: `C:\Users\mobu\Desktop\Trae\Apps\PDFAPPs\android-app\test-reports\screenshots_smoke_2026-05-23_heartbeat\export_preview_copy_disclosure.png`
- UI dump: `C:\Users\mobu\Desktop\Trae\Apps\PDFAPPs\android-app\test-reports\screenshots_smoke_2026-05-23_heartbeat\export_preview_copy_disclosure.xml`
- Confirmed visible strings:
  - `Common tasks, no clutter. Preview edits save as new copies.`
  - `Preview-based copies`
  - `Preview copy`
  - `Preview merge`
- Crash scan: no `FATAL EXCEPTION` found for the app during the smoke run.

## Remaining Product Notes

- The release APK is installable and signed; without a production keystore it uses the configured debug-signing fallback.
- Camera capture still needs a physical-device pass for final store-level confidence.
- Text-preserving structural PDF editing remains outside the MVP engine and should use a production PDF SDK if required.
