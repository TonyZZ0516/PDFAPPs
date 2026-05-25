# SwiftPDF Functional Button Pass - 2026-05-23

## Build

- `./gradlew.bat :app:assembleDebug` passed.
- `./gradlew.bat :app:testDebugUnitTest` passed.
- Debug APK installed on `emulator-5554`.

## Fixes Made During Functional Test

- Stabilized app state by moving business ViewModels from plain Compose `remember` into Activity-scoped AndroidX `viewModel()` factories.
- Kept dependency construction keyed by stable `applicationContext`.
- Changed Image to PDF import from Photo Picker style `GetMultipleContents` to DocumentsUI `OpenMultipleDocuments(image/*)` so selected image URIs return reliably and persist read access.

## Tested Flows

- PDF import:
  - Imported `swiftpdf-two-page.pdf`.
  - Reader rendered `Page 1 of 2`.
  - App retained the imported PDF after Home/back-to-app resume.
  - DataStore persisted `files/datastore/document_history.preferences_pb`.

- Compress:
  - Tapped `Compress`.
  - Generated and opened `SwiftPDF_compressed_20260523_042029.pdf`.

- Split PDF:
  - Tapped `Split PDF`.
  - Generated and opened `SwiftPDF_page_1_20260523_042250.pdf`.

- Merge PDF:
  - Single-PDF selection correctly showed `Select at least two PDFs to merge.`
  - Multi-selected `swiftpdf-two-page.pdf` and `swiftpdf-test.pdf` from DocumentsUI.
  - Generated and opened `SwiftPDF_merged_20260523_043428.pdf`.

- PDF to Image:
  - Tapped `PDF to Image`.
  - Export status showed `Exported 1 image(s).`
  - `Exported Images` card appeared with `Share Images`.

- Sign PDF:
  - Opened `Sign PDF` dialog.
  - Entered signer name.
  - Dismissed Android emulator handwriting panel.
  - Tapped `Create signed copy`.
  - Generated and opened `SwiftPDF_signed_20260523_042632.pdf`.

- Image to PDF:
  - Opened DocumentsUI image picker.
  - Selected `swiftpdf-image-1.jpg`.
  - Scan screen advanced to `Step 2 of 4 - reorder pages`.
  - Tapped `Preview`.
  - Generated and opened `SwiftPDF_scan_20260523_043145.pdf`.

## Screenshot Evidence

- `test-reports/screenshots_functional_2026-05-23/reader_imported_stable.png`
- `test-reports/screenshots_functional_2026-05-23/export_active_document.png`
- `test-reports/screenshots_functional_2026-05-23/reader_after_compress.png`
- `test-reports/screenshots_functional_2026-05-23/reader_after_split.png`
- `test-reports/screenshots_functional_2026-05-23/reader_after_merge_success.png`
- `test-reports/screenshots_functional_2026-05-23/export_after_pdf_to_image.png`
- `test-reports/screenshots_functional_2026-05-23/sign_dialog_open.png`
- `test-reports/screenshots_functional_2026-05-23/reader_after_sign_confirm.png`
- `test-reports/screenshots_functional_2026-05-23/scan_after_image_doc_row.png`
- `test-reports/screenshots_functional_2026-05-23/reader_after_image_to_pdf.png`

## Notes

- Android emulator showed a system `Try out your stylus` handwriting panel when the signer-name field was focused. This was dismissed during the test; the app flow itself completed after that.
- DocumentsUI image picking is now preferred for this MVP because it behaves consistently with PDF import and supports persistable file access.
