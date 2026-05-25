# SwiftPDF APK 构建说明

## 本地可安装 APK

当前项目可以直接生成 debug APK 和 signed release APK：

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug :app:assembleRelease
```

输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

如果没有配置生产签名，`release` 会使用 Android debug signing 生成一个可安装的演示 APK。这个 APK 适合本地验收和演示，不适合上架。

## 生产签名

正式发布前，在 `android-app/keystore.properties` 中配置真实 keystore。该文件已被 `.gitignore` 忽略，不应提交到仓库。

```properties
storeFile=path/to/release-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

配置后重新运行：

```powershell
.\gradlew.bat :app:assembleRelease
```

`app/build/outputs/apk/release/app-release.apk` 将使用真实 release key 签名。
