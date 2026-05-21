# Android PDF Reader + Converter MVP PRD

## 1. Product Positioning

**Product name:** SwiftPDF

**Target users:** Students and office workers who need to open, read, sign, convert, and manage PDFs quickly on Android.

**MVP strategy:** Use fast PDF reading as the entry point, then convert high-intent actions into Pro upgrades through Image to PDF, signing, compression, merge/split, and PDF to Image.

**Competitive reference:** QR Code Scanner's `PDF Reader - PDF Viewer` shows strong demand for the "PDF reader + tool suite" pattern, but user reviews expose frustration around intrusive ads, forced subscription prompts, and slow opening. SwiftPDF should position itself as cleaner, faster, and less aggressive.

## 2. Goals

- Let users open recent PDFs within two taps.
- Let users complete Image to PDF in five steps or fewer.
- Let users sign a PDF and save a copy without leaving the reader.
- Monetize through clear Pro limits, not forced first-launch paywalls.
- Keep the first version scoped enough for a one- or two-person team.

## 3. MVP Features

### 3.1 File Home and Quick Reader

**User value:** Open and manage local PDFs quickly.

**Requirements:**
- Show recent PDF files on the home screen.
- Support search, sort, rename, share, delete, and recent-open states.
- Provide a clear empty state when no documents are found.
- Reader supports zoom, page jump, continuous scroll, night mode, bookmarks, and basic file actions.
- First launch should not show a forced subscription modal.

**Acceptance criteria:**
- App reaches home within 3 seconds and shows either recent files or an empty state.
- A user can open a PDF from the home list in two taps.
- Reader toolbar exposes search, bookmark, tools, and more actions.

### 3.2 Image to PDF

**User value:** Convert photos, screenshots, receipts, and notes into PDFs.

**Requirements:**
- Support selecting multiple images from gallery.
- Support camera import.
- Allow page reorder, crop, delete, and preview before export.
- Export to PDF and show a success screen with open/share actions.
- Free users get 3 exports per day; Pro users have unlimited exports.

**Acceptance criteria:**
- User can complete Image to PDF in five steps: choose tool, select images, reorder/crop, preview, export.
- Free-limit trigger shows a Pro prompt and allows returning to the workflow.

### 3.3 Signature and Basic Annotation

**User value:** Sign contracts, forms, assignments, or documents quickly.

**Requirements:**
- Create signature by drawing.
- Extract signature from an imported image.
- Place, resize, and move signature on a PDF page.
- Add highlight, freehand drawing, and text notes.
- Save as a copy by default to avoid overwriting the original.

**Acceptance criteria:**
- User can add a signature from the reader and save a signed copy.
- Signature controls include move, resize, confirm, and cancel.

### 3.4 PDF Toolbox

**User value:** Finish common PDF utility tasks without installing separate apps.

**Requirements:**
- Provide tools for compress, merge, split, PDF to Image, Image to PDF, and Sign.
- Batch processing and high-quality compression are Pro features.
- Basic single-file compression, single merge, and simple split remain free with daily limits.

**Acceptance criteria:**
- Tools are accessible from home and reader.
- Pro-only limits are explained at the moment of use.

### 3.5 PDF to Image

**User value:** Export selected PDF pages as JPG or PNG for sharing.

**Requirements:**
- Allow page selection.
- Allow output format: JPG or PNG.
- Standard resolution is free.
- HD export is Pro.

**Acceptance criteria:**
- User can select pages and export images.
- HD export triggers Pro prompt but standard export remains available.

## 4. Monetization

**Model:** Free base app + Pro subscription.

**Free:**
- PDF reading and file management.
- 3 Image to PDF exports per day.
- Basic signature.
- Standard PDF to Image export.
- Basic compress/merge/split limits.

**Pro:**
- No ads.
- Unlimited conversions.
- Batch processing.
- HD export.
- High-quality compression.
- Unlimited signatures and saved signature library.

**Paywall rules:**
- Do not show paywall on first launch.
- Show Pro prompts only after a user triggers a limit or taps a Pro feature.
- Paywall must have a clear close/back action.

## 5. Information Architecture

- **Home:** Recent files, search, quick tools, Pro entry.
- **Reader:** PDF viewport, top app bar, bottom page/tool controls.
- **Toolbox:** Grid of conversion and PDF utility tools.
- **Image to PDF flow:** Import, reorder/crop, preview, export result.
- **Sign flow:** Create/import signature, place on page, save copy.
- **Pro:** Benefits, price area, restore purchase, continue free.

## 6. Design Direction

- Platform: Android.
- Visual style: Material 3 inspired, clean productivity UI, light office tone.
- Primary color: teal-blue `#0F8B8D`.
- Accent color: blue `#2563EB`.
- Background: cool light gray `#F5F7FA`.
- Typography: Inter/Roboto style, compact and readable.
- Avoid heavy gradients, dark subscription-first visuals, and marketing-style hero screens.

## 7. Key Non-Goals for V1

- No PDF to Word.
- No OCR.
- No AI summary, translation, or read-aloud.
- No cloud sync.
- No collaborative editing.
- No iOS version.

## 8. Test Scenarios

- First launch with no documents: show empty state and scan/import actions.
- First launch with documents: show recent file list and quick tools.
- Open PDF: reader loads with toolbar, page count, bookmark action, and tools.
- Image to PDF: gallery import, reorder, preview, export success.
- Signature: draw signature, place on PDF, save as copy.
- Free limit: show Pro prompt, close prompt, continue free path.
- Permission: explain file access before requesting Android storage access.

