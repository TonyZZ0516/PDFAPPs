# SwiftPDF heartbeat pass - signed release APK - 2026-05-23 10:29 CST

## Progress this pass

- Added release signing support to `app/build.gradle.kts`.
- `assembleRelease` now outputs an installable signed APK at:

```text
app/build/outputs/apk/release/app-release.apk
```

- If `android-app/keystore.properties` exists, release builds use that production keystore.
- If no production keystore is configured, release builds fall back to Android debug signing for local demo/QA APK delivery.
- Added `.gitignore` entries for `keystore.properties`, `*.jks`, and `*.keystore`.
- Added `RELEASE_BUILD_CN.md` with local APK and production signing instructions.

## Automated verification

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
$env:TEMP="C:\tmp"
$env:TMP="C:\tmp"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease --stacktrace
```

Result: PASS

- `:app:assembleDebug`: PASS
- `:app:testDebugUnitTest`: PASS
- Unit tests: 7 passed, 0 failed
- `:app:validateSigningRelease`: PASS
- `:app:assembleRelease`: PASS

## Signature verification

```powershell
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Result: PASS

- APK verifies.
- Verified using APK Signature Scheme v2.
- Current signer: Android Debug certificate, because no production `keystore.properties` is configured.

## Emulator release APK smoke verification

- Device: `emulator-5554`
- Install: `adb install -r app/build/outputs/apk/release/app-release.apk`: PASS
- Cold launch: `am start -W -n com.swiftpdf.app/.MainActivity`: PASS
- Home UI visible after release launch: PASS
- Fatal crash scan: no `FATAL EXCEPTION` found after release launch.

Evidence:

```text
test-reports/screenshots_smoke_2026-05-23_heartbeat/
  release_launch.png
  release_launch.xml
```

## APK outputs

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Latest artifact sizes:

- Debug APK: 29,307,928 bytes
- Signed release APK: 17,265,797 bytes

## Remaining gaps before final product completion

- Production release still needs a real keystore in `keystore.properties`; current release APK is demo-signed with Android Debug.
- Physical-device camera capture is still not verified.
- Signature image import is still not implemented; freehand signing works.
- PDF operations are still raster-copy based, not text-preserving structural PDF edits.
