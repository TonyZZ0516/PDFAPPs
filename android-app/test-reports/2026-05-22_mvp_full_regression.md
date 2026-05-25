# SwiftPDF MVP full regression - 2026-05-22

## Environment

- Device: Android emulator `emulator-5554`, Pixel 7 / API 36
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- JDK: Android Studio JBR
- SDK: `%LOCALAPPDATA%/Android/Sdk`

## Build and unit tests

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
$env:TEMP="C:\tmp"
$env:TMP="C:\tmp"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --stacktrace
```

Result: PASS

- `:app:assembleDebug`: PASS
- `:app:testDebugUnitTest`: PASS
- Unit tests: 4 passed, 0 failed
- Test result: `app/build/test-results/testDebugUnitTest/TEST-com.swiftpdf.app.domain.document.FileSizeFormatTest.xml`

## UI screenshots

Screenshots are stored in:

```text
test-reports/screenshots_2026-05-22/
  home.png
  reader_scan_export.png
  scan.png
  export.png
  settings.png
```

## Functional regression

| Area | Case | Result | Evidence |
|---|---|---:|---|
| App launch | Cold start opens without crash | PASS | ADB start returned `Status: ok` |
| Home | Recent list displays generated/imported PDFs | PASS | `home.png` |
| Home | Clear history leads to empty state | PASS | ADB text showed `No recent files yet` |
| Reader | Opens generated scan PDF | PASS | `SwiftPDF_scan_20260522_150443.pdf`, `Page 1 of 3` |
| Reader | Zoom/page controls remain visible and usable | PASS | Existing reader controls regression plus current Reader check |
| Share | PDF share opens Android sharesheet | PASS | Previous reader/export share test |
| Scan | Image import through Android Photo Picker | PASS | Imported 3 images |
| Scan | Page queue displays imported images | PASS | ADB text showed `Pages: 3` |
| Scan | Rotate page | PASS | `Rotation: 90 deg` after tapping rotate |
| Scan | Reorder page down | PASS | Queue changed from `61.jpg` first to `62.jpg` first |
| Scan | Export image queue to PDF | PASS | Reader opened `SwiftPDF_scan_20260522_150443.pdf`, `Page 1 of 3` |
| Export | PDF to images | PASS | ADB text showed `Exported 2 image(s).` |
| Export | Share exported images | PASS | Android sharesheet showed `Sharing 2 images` |
| Export | Compress PDF | PASS | Reader opened `SwiftPDF_compressed_20260522_145534.pdf`, `Page 1 of 2` |
| Export | Extract first page | PASS | Reader opened `SwiftPDF_page_1_20260522_145932.pdf`, `Page 1 of 1` |
| Export | Merge two PDFs | PASS | Reader opened `SwiftPDF_merged_20260522_150224.pdf`, `Page 1 of 3` |
| Export | Merge one PDF validation | PASS | UI showed `Select at least two PDFs to merge.` |
| Settings | Clear history confirmation dialog | PASS | Dialog showed `Clear recent history?` |

## UI fidelity notes

Current UI is complete enough for MVP testing:

- Home, Reader, Scan, Export, Settings all have accessible main actions.
- Empty states and error states are present for missing document, export validation, and camera permission.
- Scan queue layout was changed to two rows to avoid crowded action icons on phone width.
- Settings now uses a confirmation dialog before clearing history.

Score estimate against the current HTML high-fidelity direction: 84/100.

- Layout and flow: good
- Typography and spacing: acceptable for MVP
- Visual polish: still needs final brand pass
- Advanced dialog/onboarding polish: partially present, not final

## Remaining non-blocking risks

- Camera live capture should still be checked on a physical Android device because emulator camera behavior is not representative.
- Compression, split, and merge currently rasterize PDFs. This is acceptable for MVP demos but should be replaced with true PDF-structure operations before production if text/selectability must be preserved.
- OCR, subscriptions, cloud sync, advanced annotations, and payment remain outside this MVP implementation.
